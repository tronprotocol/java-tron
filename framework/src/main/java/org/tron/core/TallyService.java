package org.tron.core;

import static org.tron.core.config.Parameter.ChainSymbol.TRX_SYMBOL;

import com.google.protobuf.ByteString;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.entity.Dec;
import org.tron.core.capsule.DecOracleRewardCapsule;
import org.tron.core.service.MortgageService;
import org.tron.core.store.DelegationStore;
import org.tron.core.store.DynamicPropertiesStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;


/**
 * @author kiven.miao
 * @date 2022/5/16 11:35 上午
 */
@Component
public class TallyService {

  @Autowired
  private DelegationStore delegationStore;

  @Autowired
  private MortgageService mortgageService;

  @Autowired
  private DynamicPropertiesStore dynamicPropertiesStore;

  private Dec tally(ArrayList<ExchangeRate> sortedVoteList, HashMap<ByteString, Claim> srClaim) {
    if (CollectionUtils.isEmpty(sortedVoteList)) {
      return Dec.zeroDec();
    }
    Dec rewardBand = Dec.newDecWithPrec(dynamicPropertiesStore.getOracleRewardBand(), 6);

    Dec weightedMedian = getWeightedMedianWithAssertion(sortedVoteList);
    if (weightedMedian.eq(Dec.zeroDec())) {
      return Dec.zeroDec();
    }

    Dec standardDeviation = getStandardDeviation(sortedVoteList, weightedMedian);
    if (standardDeviation.eq(Dec.zeroDec())) {
      return Dec.zeroDec();
    }
    Dec rewardSpread = weightedMedian.mul(rewardBand.quo(2));

    if (standardDeviation.gt(rewardSpread)) {
      rewardSpread = standardDeviation;
    }

    for (ExchangeRate exchange : sortedVoteList) {
      // Filter ballot winners & abstain voters
      if (exchange.exchangeRate.gte(weightedMedian.sub(rewardSpread))
          && exchange.exchangeRate.lte(weightedMedian.add(rewardSpread)) || !exchange.exchangeRate.isPositive()) {
        Claim claim = srClaim.get(exchange.srAddress);
        claim.weight += exchange.vote;
        claim.winCount++;
        srClaim.put(exchange.srAddress, claim);
      }
    }

    return weightedMedian;
  }

  private void rewardBallotWinners(long votePeriod, long rewardDistributionWindow, Map<String, Dec> voteTargets,
                                   Map<ByteString, Claim> ballotWinners) {
    List<String> rewardDenoms = new ArrayList<>();
    rewardDenoms.add(TRX_SYMBOL);
    voteTargets.forEach((name, dec) -> {
      rewardDenoms.add(name);
    });

    AtomicLong ballotPowerSum = new AtomicLong(0L);
    ballotWinners.forEach((srAddress, claim) -> {
      ballotPowerSum.addAndGet(claim.weight);
    });

    if (0L == ballotPowerSum.get()) {
      return;
    }

    Dec distributionRatio = Dec.newDec(votePeriod).quo(rewardDistributionWindow);

    List<DecCoin> periodRewards = new ArrayList<>();
    DecOracleRewardCapsule decOracleRewardCapsule = delegationStore.getOracleRewardPool();
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
      List<DecCoin> decCoins = mulDec(periodRewards, Dec.newDec(claim.weight).quo(ballotPowerSum.get()));
      List<Coin> rewardCoins = new ArrayList<>();
      List<DecCoin> changeCoins = new ArrayList<>();
      truncateDecimal(decCoins, rewardCoins, changeCoins);

      // In case absence of the validator, we just skip distribution
      if (null != receiverVal && !rewardCoins.isEmpty()) {
        DecOracleRewardCapsule srOracleReward = oracleRewardFromCoins(rewardCoins);
        mortgageService.payOracleReward(receiverVal, srOracleReward);
        delegationStore.addOracleRewardPool(srOracleReward);
      }
    });
  }

  private DecOracleRewardCapsule oracleRewardFromCoins(List<Coin> rewardCoins) {
    Dec balance = Dec.zeroDec();
    Map<String, Dec> asset = new HashMap<>();
    for (Coin coin : rewardCoins) {
      if (TRX_SYMBOL.equals(coin.denom)) {
        balance = balance.add(Dec.newDec(coin.amount));
        continue;
      }
      asset.put(coin.denom, Dec.newDec(coin.amount));
    }
    return new DecOracleRewardCapsule(balance, asset);
  }

  private List<DecCoin> mulDec(List<DecCoin> periodRewards, Dec d) {
    List<DecCoin> result = new ArrayList<>();
    periodRewards.forEach(newDecCoin -> {
      DecCoin decCoin = new DecCoin(newDecCoin.denom, newDecCoin.amount.mul(d));
      if (!decCoin.isZero()) {
        result.add(decCoin);
      }
    });
    return result;
  }

  private void truncateDecimal(List<DecCoin> decCoins, List<Coin> truncatedCoins, List<DecCoin> changeCoins) {
    decCoins.forEach(decCoin -> {
      int truncated = decCoin.amount.truncateInt();
      Dec change = decCoin.amount.sub(Dec.newDec(truncated));
      Coin truncatedCoin = new Coin(decCoin.denom, truncated);
      DecCoin changeCoin = new DecCoin(decCoin.denom, change);
      if (!truncatedCoin.isZero()) {
        truncatedCoins.add(truncatedCoin);
      }
      if (!changeCoin.isZero()) {
        changeCoins.add(changeCoin);
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

  private Dec getWeightedMedianWithAssertion(List<ExchangeRate> sortedVoteList) {
    if (CollectionUtils.isEmpty(sortedVoteList)) {
      return Dec.zeroDec();
    }
    Long totalPower = getTotalPower(sortedVoteList);
    long currentPower = 0L;
    for (ExchangeRate exchangeRate : sortedVoteList) {
      currentPower += exchangeRate.vote;
      if (currentPower >= totalPower / 2) {
        return exchangeRate.exchangeRate;
      }
    }
    return Dec.zeroDec();
  }

  private Dec getStandardDeviation(List<ExchangeRate> sortedVoteList, Dec weightedMedian) {
    if (CollectionUtils.isEmpty(sortedVoteList)) {
      return Dec.zeroDec();
    }

    Dec sum = Dec.zeroDec();
    for (ExchangeRate exchangeRate : sortedVoteList) {
      Dec deviation = exchangeRate.exchangeRate.sub(weightedMedian);
      sum = sum.add(deviation.mul(deviation));
    }

    Dec variance = sum.quo(sortedVoteList.size());

    double floatNum = variance.parseDouble();
    double sqrtNum = Math.sqrt(floatNum);
    return Dec.newDec(String.valueOf(sqrtNum));
  }

  private Long getTotalPower(List<ExchangeRate> sortedVoteList) {
    AtomicReference<Long> totalPower = new AtomicReference<>(0L);
    sortedVoteList.forEach(exchangeRate -> totalPower.set(totalPower.get() + exchangeRate.vote));
    return totalPower.get();
  }

  private static class ExchangeRate implements Comparable<ExchangeRate> {
    ByteString srAddress;
    String asset;
    long vote;
    Dec exchangeRate;

    public ExchangeRate(ByteString srAddress, long vote, String asset, Dec exchangeRate) {
      this.srAddress = srAddress;
      this.asset = asset;
      this.vote = vote;
      this.exchangeRate = exchangeRate;
    }

    @Override
    public int compareTo(ExchangeRate e) {
      if (this.vote - e.vote > 0) {
        return 1;
      } else if (this.vote - e.vote < 0) {
        return -1;
      }
      return 0;
    }
  }

  // Claim is an interface that directs its rewards to sr account.
  private static class Claim {
    long vote;
    long weight;
    long winCount;
    byte[] srAddress;

    public Claim(byte[] srAddress, long vote) {
      this.srAddress = srAddress;
      this.weight = 0;
      this.vote = vote;
      this.winCount = 0;
    }
  }

  private static class Coin {
    String denom;
    long amount;

    private boolean isZero() {
      return 0L == this.amount;
    }

    public Coin(String denom, long amount) {
      this.denom = denom;
      this.amount = amount;
    }
  }

  private static class DecCoin {
    String denom;
    Dec amount;

    private boolean isZero() {
      return this.amount.isZero();
    }

    public DecCoin(String denom, Dec amount) {
      this.denom = denom;
      this.amount = amount;
    }
  }


}
