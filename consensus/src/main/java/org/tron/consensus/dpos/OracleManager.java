package org.tron.consensus.dpos;

import static org.tron.core.config.Parameter.ChainSymbol.TRX_SYMBOL;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.entity.Dec;
import org.tron.common.utils.ByteArray;
import org.tron.consensus.ConsensusDelegate;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.DecOracleRewardCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.service.MortgageService;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.StableMarketStore;
import org.tron.protos.Protocol;


@Slf4j(topic = "consensus")
@Component
public class OracleManager {

  @Autowired
  private DynamicPropertiesStore dynamicPropertiesStore;

  @Autowired
  private StableMarketStore stableMarketStore;

  @Autowired
  private MortgageService mortgageService;

  static class ExchangeRateData implements Comparable<ExchangeRateData> {
    private final ByteString srAddress;
    private long vote;
    private Dec exchangeRate;

    public ExchangeRateData(ByteString srAddress, long vote, Dec exchangeRate) {
      this.srAddress = srAddress;
      this.vote = vote;
      this.exchangeRate = exchangeRate;
    }

    @Override
    public int compareTo(ExchangeRateData e) {
      return Long.compare(vote, e.vote);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ExchangeRateData that = (ExchangeRateData) o;
      return vote == that.vote && srAddress.equals(that.srAddress)
          && Objects.equals(exchangeRate, that.exchangeRate);
    }

    @Override
    public int hashCode() {
      return Objects.hash(srAddress, vote, exchangeRate);
    }
  }

  // Claim is an interface that directs its rewards to sr account.
  static class Claim {
    private final long vote;
    private long weight;
    private long winCount;

    public Claim(long vote) {
      this.weight = 0;
      this.vote = vote;
      this.winCount = 0;
    }
  }

  @Autowired
  private ConsensusDelegate consensusDelegate;

  public void applyBlock(BlockCapsule blockCapsule) {
    final long votePeriod = consensusDelegate.getDynamicPropertiesStore().getOracleVotePeriod();
    final long blockNum = blockCapsule.getNum();

    if (votePeriod == 0) {
      return;
    }
    //check period last block
    if ((blockNum + 1) % votePeriod == 0) {
      // Build claim map over all srs in active set
      Map<ByteString, Claim> srMap = new HashMap<>();
      long totalVote = 0;
      for (WitnessCapsule witness : consensusDelegate.getAllWitnesses()) {
        ByteString sr = witness.getAddress();
        srMap.put(sr, new Claim(witness.getVoteCount()));
        totalVote += witness.getVoteCount();
      }

      // 1. clear old exchange rates
      stableMarketStore.clearAllOracleExchangeRates();

      Map<String, Dec> supportAssets = stableMarketStore.getAllTobinTax();
      if (supportAssets != null) {
        // 2, sorting ballots
        Map<String, List<ExchangeRateData>> assetVotes =
            organizeBallotByAsset(srMap, supportAssets);

        // 3. pick reference Asset
        final long thresholdVotes = totalVote / 100
            * consensusDelegate.getDynamicPropertiesStore().getOracleVoteThreshold();
        String referenceAsset = pickReferenceAsset(assetVotes, thresholdVotes, supportAssets);
        logger.info("pick reference asset [{}]", referenceAsset);
        // 4. calculate cross exchange rates
        if (!referenceAsset.isEmpty()) {
          // make voteMap of Reference Asset to calculate cross exchange rates
          List<ExchangeRateData> voteReferenceList = assetVotes.get(referenceAsset);
          // save reference asset exchange rate
          Dec exchangeRateReference = getWeightedMedian(voteReferenceList);
          stableMarketStore.setOracleExchangeRate(
              ByteArray.fromString(referenceAsset), exchangeRateReference);

          // save other assets exchange rate
          Map<ByteString, Dec> voteReferenceMap = new HashMap<>();
          voteReferenceList.forEach(vote ->
              voteReferenceMap.put(vote.srAddress, vote.exchangeRate));
          assetVotes.forEach((asset, voteList) -> {
            if (!asset.equals(referenceAsset)) {
              // Convert vote to cross exchange rates
              toCrossRate(voteList, voteReferenceMap);
              Dec exchangeRate = tally(voteList, srMap);
              exchangeRate = exchangeRateReference.quo(exchangeRate);
              stableMarketStore.setOracleExchangeRate(
                  ByteArray.fromString(asset), exchangeRate);
            }
          });
        }
        // 5. Do miss counting & slashing
        int supportAssetsSize = assetVotes.size();
        srMap.forEach((sr, claim) -> {
          if (claim.winCount < supportAssetsSize) {
            // Increase miss counter
            long missCount = stableMarketStore.getWitnessMissCount(sr.toByteArray());
            stableMarketStore.setWitnessMissCount(sr.toByteArray(), missCount + 1);
          }
        });

        // 6. payout sr rewards
        long rewardDistributionWindow = dynamicPropertiesStore.getOracleRewardDistributionWindow();
        rewardBallotWinners(votePeriod, rewardDistributionWindow, supportAssets, srMap);
      }

      // 7. post-processing, clear vote info, update tobin tax
      stableMarketStore.clearPrevoteAndVotes(blockNum, votePeriod);
      stableMarketStore.updateTobinTax(supportAssets);
    }
  }

  // NOTE: **Make abstain votes to have zero vote**
  private Map<String, List<ExchangeRateData>> organizeBallotByAsset(
      Map<ByteString, Claim> srMap, Map<String, Dec> whiteList) {
    Map<String, List<ExchangeRateData>> tokenVotes = new HashMap<>();
    // Organize aggregate votes
    for (Map.Entry<ByteString, Claim> entry : srMap.entrySet()) {
      ByteString sr = entry.getKey();
      Claim srInfo = entry.getValue();
      Protocol.OracleVote srVote = stableMarketStore.getVote(sr.toByteArray());
      if (srVote == null) {
        continue;
      }
      String exchangeRateStr = srVote.getExchangeRates();

      // check all assets are in the vote whitelist
      Map<String, Dec> exchangeRateMap = StableMarketStore.parseExchangeRateTuples(exchangeRateStr);
      exchangeRateMap.forEach((asset, exchangeRate) -> {
        if (whiteList.get(asset) != null) {
          List<ExchangeRateData> tokenRateList =
              tokenVotes.computeIfAbsent(asset, k -> new ArrayList<>());

          long tmpVote = srInfo.vote;
          if (!exchangeRate.isPositive()) {
            tmpVote = 0;
          }
          tokenRateList.add(new ExchangeRateData(sr, tmpVote, exchangeRate));
        }
      });
    }

    tokenVotes.forEach((key, voteList) -> Collections.sort(voteList));
    return tokenVotes;
  }

  private String pickReferenceAsset(
      Map<String, List<ExchangeRateData>> assetVotes, long thresholdVotes,
      Map<String, Dec> supportAssets) {
    long largestVote = 0;
    String referenceAsset = "";

    Iterator<Map.Entry<String, List<ExchangeRateData>>> it = assetVotes.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, List<ExchangeRateData>> assetVote = it.next();
      long tokenVoteCounts = assetVote.getValue().stream().mapToLong(vote -> vote.vote).sum();
      String asset = assetVote.getKey();

      // check token vote count
      if (tokenVoteCounts < thresholdVotes) {
        it.remove();
        supportAssets.remove(asset);
        logger.info("remove asset {} with insufficient votes ", asset);
        continue;
      }

      if (tokenVoteCounts > largestVote) {
        referenceAsset = asset;
        largestVote = tokenVoteCounts;
      } else if (largestVote == tokenVoteCounts && referenceAsset.compareTo(asset) > 0) {
        referenceAsset = asset;
      }
    }
    return referenceAsset;
  }

  private Dec getWeightedMedian(List<ExchangeRateData> voteReferenceList) {
    if (!voteReferenceList.isEmpty()) {
      long tokenVoteCounts = voteReferenceList.stream().mapToLong(vote -> vote.vote).sum();
      long currentVote = 0;
      for (ExchangeRateData vote : voteReferenceList) {
        currentVote += vote.vote;
        if (currentVote >= (tokenVoteCounts / 2)) {
          return vote.exchangeRate;
        }
      }
    }
    return Dec.zeroDec();
  }

  private void toCrossRate(
      List<ExchangeRateData> voteList, Map<ByteString, Dec> referenceVote) {
    voteList.forEach(vote -> {
      Dec srReferenceRate = referenceVote.get(vote.srAddress);
      if (srReferenceRate.isPositive()) {
        vote.exchangeRate = srReferenceRate.quo(vote.exchangeRate);
      } else {
        vote.exchangeRate = Dec.zeroDec();
        vote.vote = 0;
      }
    });
    Collections.sort(voteList);
  }

  private Dec tally(List<ExchangeRateData> voteList, Map<ByteString, Claim> srClaim) {
    if (CollectionUtils.isEmpty(voteList)) {
      return Dec.zeroDec();
    }
    Dec rewardBand = Dec.newDecWithPrec(dynamicPropertiesStore.getOracleRewardBand(), 6);

    Dec weightedMedian = getWeightedMedian(voteList);
    if (weightedMedian.eq(Dec.zeroDec())) {
      return weightedMedian;
    }

    Dec standardDeviation = getStandardDeviation(voteList, weightedMedian);
    if (standardDeviation.eq(Dec.zeroDec())) {
      return weightedMedian;
    }
    Dec rewardSpread = weightedMedian.mul(rewardBand.quo(2));

    if (standardDeviation.gt(rewardSpread)) {
      rewardSpread = standardDeviation;
    }

    for (ExchangeRateData exchange : voteList) {
      // Filter ballot winners & abstain voters
      if (exchange.exchangeRate.gte(weightedMedian.sub(rewardSpread))
          && exchange.exchangeRate.lte(weightedMedian.add(rewardSpread))
          || !exchange.exchangeRate.isPositive()) {
        Claim claim = srClaim.get(exchange.srAddress);
        claim.weight += exchange.vote;
        claim.winCount++;
        srClaim.put(exchange.srAddress, claim);
      }
    }

    return weightedMedian;
  }

  private void rewardBallotWinners(long votePeriod, long rewardDistributionWindow,
                                   Map<String, Dec> voteTargets,
                                   Map<ByteString, Claim> ballotWinners) {
    List<String> rewardDenoms = new ArrayList<>();
    rewardDenoms.add(TRX_SYMBOL);
    voteTargets.forEach((name, dec) -> rewardDenoms.add(name));

    AtomicLong ballotPowerSum = new AtomicLong(0L);
    ballotWinners.forEach((srAddress, claim) -> ballotPowerSum.addAndGet(claim.weight));

    if (0L == ballotPowerSum.get()) {
      return;
    }

    Dec distributionRatio = Dec.newDec(votePeriod).quo(rewardDistributionWindow);

    List<DecCoin> periodRewards = new ArrayList<>();
    DecOracleRewardCapsule decOracleRewardCapsule = stableMarketStore.getOracleRewardPool();
    rewardDenoms.forEach(denom -> {
      Dec rewardPool = getRewardPool(denom, decOracleRewardCapsule);
      if (rewardPool.isZero()) {
        return;
      }
      periodRewards.add(new DecCoin(denom, rewardPool.mul(distributionRatio)));
    });

    ballotWinners.forEach((srAddress, claim) -> {
      //check validator status TODO
      byte[] receiverVal = srAddress.toByteArray();

      // Reflects contribution
      List<DecCoin> decCoins =
          mulDec(periodRewards, Dec.newDec(claim.weight).quo(ballotPowerSum.get()));
      List<Coin> rewardCoins = new ArrayList<>();
      List<DecCoin> changeCoins = new ArrayList<>();
      truncateDecimal(decCoins, rewardCoins, changeCoins);

      // In case absence of the validator, we just skip distribution
      if (null != receiverVal && !rewardCoins.isEmpty()) {
        DecOracleRewardCapsule srOracleReward = oracleRewardFromCoins(rewardCoins);
        mortgageService.payOracleReward(receiverVal, srOracleReward);
        stableMarketStore.addOracleRewardPool(srOracleReward);
      }
    });
  }

  private Dec getRewardPool(String denom, DecOracleRewardCapsule decOracleRewardCapsule) {
    if (TRX_SYMBOL.equals(denom)) {
      return decOracleRewardCapsule.getBalance();
    }
    if (!decOracleRewardCapsule.getAsset().containsKey(denom)) {
      return Dec.zeroDec();
    }
    return decOracleRewardCapsule.getAsset().get(denom);
  }

  private DecOracleRewardCapsule oracleRewardFromCoins(List<Coin> rewardCoins) {
    BigInteger balance = BigInteger.ZERO;
    Map<String, BigInteger> asset = new HashMap<>();
    for (Coin coin : rewardCoins) {
      if (TRX_SYMBOL.equals(coin.denom)) {
        balance = balance.add(coin.amount);
        continue;
      }
      asset.put(coin.denom, coin.amount);
    }
    return new DecOracleRewardCapsule(balance, asset);
  }

  private List<DecCoin> mulDec(List<DecCoin> periodRewards, Dec d) {
    List<DecCoin> result = new ArrayList<>();
    periodRewards.forEach(newDecCoin -> {
      DecCoin decCoin = new DecCoin(newDecCoin.decDenom, newDecCoin.decAmount.mul(d));
      if (!decCoin.isZero()) {
        result.add(decCoin);
      }
    });
    return result;
  }

  private void truncateDecimal(List<DecCoin> decCoins,
                               List<Coin> truncatedCoins, List<DecCoin> changeCoins) {
    decCoins.forEach(decCoin -> {
      BigInteger truncated = decCoin.decAmount.truncateBigInt();
      Dec change = decCoin.decAmount.sub(Dec.newDec(truncated));
      Coin truncatedCoin = new Coin(decCoin.decDenom, truncated);
      DecCoin changeCoin = new DecCoin(decCoin.decDenom, change);
      if (!truncatedCoin.isZero()) {
        truncatedCoins.add(truncatedCoin);
      }
      if (!changeCoin.isZero()) {
        changeCoins.add(changeCoin);
      }
    });
  }

  private Dec getStandardDeviation(List<ExchangeRateData> sortedVoteList, Dec weightedMedian) {
    if (CollectionUtils.isEmpty(sortedVoteList)) {
      return Dec.zeroDec();
    }

    Dec sum = Dec.zeroDec();
    for (ExchangeRateData exchangeRate : sortedVoteList) {
      Dec deviation = exchangeRate.exchangeRate.sub(weightedMedian);
      sum = sum.add(deviation.mul(deviation));
    }

    Dec variance = sum.quo(sortedVoteList.size());

    double floatNum = variance.parseDouble();
    double sqrtNum = Math.sqrt(floatNum);
    return Dec.newDec(String.valueOf(sqrtNum));
  }

  private static class DecCoin {
    private String decDenom;
    private Dec decAmount;

    private boolean isZero() {
      return this.decAmount.isZero();
    }

    public DecCoin(String decDenom, Dec decAmount) {
      this.decDenom = decDenom;
      this.decAmount = decAmount;
    }
  }

  private static class Coin {
    private String denom;
    private BigInteger amount;

    private boolean isZero() {
      return amount.equals(BigInteger.ZERO);
    }

    public Coin(String denom, BigInteger amount) {
      this.denom = denom;
      this.amount = amount;
    }
  }

}
