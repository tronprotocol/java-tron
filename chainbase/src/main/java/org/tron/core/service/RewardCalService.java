package org.tron.core.service;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.InvalidProtocolBufferException;
import io.prometheus.client.Histogram;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.LongStream;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.Metrics;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.db.common.iterator.DBIterator;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DelegationStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.RewardCacheStore;
import org.tron.protos.Protocol;

@Component
@Slf4j(topic = "rewardCalService")
public class RewardCalService {
  @Autowired
  private DynamicPropertiesStore propertiesStore;
  @Autowired
  private DelegationStore delegationStore;

  @Autowired
  private AccountStore accountStore;

  @Autowired
  private RewardCacheStore rewardCacheStore;

  private  DBIterator accountIterator;

  private  byte[] isDoneKey;
  private static final byte[] IS_DONE_VALUE = new byte[]{0x01};

  private long newRewardCalStartCycle = Long.MAX_VALUE;
  private static final int ADDRESS_SIZE = 21;
  private byte[] lastAccount = new byte[ADDRESS_SIZE];


  private final ExecutorService es = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("rewardCalService").build());

  @PostConstruct
  private void init() throws IOException {
    newRewardCalStartCycle = propertiesStore.getNewRewardAlgorithmEffectiveCycle();
    if (newRewardCalStartCycle != Long.MAX_VALUE) {
      isDoneKey = ByteArray.fromLong(newRewardCalStartCycle);
      accountIterator = (DBIterator) accountStore.getDb().iterator();
      calReward();
    }
  }

  @PreDestroy
  private void destroy() {
    es.shutdownNow();
    rewardCacheStore.close();
  }

  public void calReward() throws IOException {
    try (DBIterator iterator = rewardCacheStore.iterator()) {
      iterator.seekToLast();
      if (iterator.hasNext()) {
        byte[] key  = iterator.next().getKey();
        System.arraycopy(key, 0, lastAccount, 0, ADDRESS_SIZE);
      }
    }
    es.submit(this::startRewardCal);
  }


  private void startRewardCal() {
    if (rewardCacheStore.has(isDoneKey)) {
      logger.info("RewardCalService is done");
      return;
    }
    logger.info("RewardCalService start from lastAccount: {}", ByteArray.toHexString(lastAccount));
    ((DBIterator) delegationStore.getDb().iterator()).prefixQueryAfterThat(
        new byte[]{Constant.ADD_PRE_FIX_BYTE_MAINNET}, lastAccount).forEachRemaining(e -> {
          try {
            doRewardCal(e.getKey(), ByteArray.toLong(e.getValue()));
          } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
          }
        });
    rewardCacheStore.put(this.isDoneKey, IS_DONE_VALUE);
    logger.info("RewardCalService is done");
  }

  private void doRewardCal(byte[] address, long beginCycle) throws InterruptedException {
    if (beginCycle >= newRewardCalStartCycle) {
      return;
    }
    long endCycle = delegationStore.getEndCycle(address);
    //skip the last cycle reward
    if (beginCycle + 1 == endCycle) {
      beginCycle += 1;
    }
    if (beginCycle >= newRewardCalStartCycle) {
      return;
    }

    Protocol.Account account = getAccount(address);
    if (account == null || account.getVotesList().isEmpty()) {
      return;
    }
    Histogram.Timer requestTimer = Metrics.histogramStartTimer(
        MetricKeys.Histogram.DO_REWARD_CAL_DELAY,
        (newRewardCalStartCycle - beginCycle) / 100 + "");
    long reward = LongStream.range(beginCycle, newRewardCalStartCycle)
        .map(i -> computeReward(i, account))
        .sum();
    rewardCacheStore.putReward(Bytes.concat(address, Longs.toByteArray(beginCycle)), reward);
    Metrics.histogramObserve(requestTimer);
  }

  private Protocol.Account getAccount(byte[] address) {
    try {
      accountIterator.seek(address);
      if (accountIterator.hasNext()) {
        byte[] account = accountIterator.next().getValue();
        return Protocol.Account.parseFrom(account);
      }
    } catch (InvalidProtocolBufferException e) {
      logger.error("getAccount error: {}", e.getMessage());
    }
    return null;
  }

  long computeReward(long cycle, Protocol.Account account) {
    return account.getVotesList().stream().mapToLong(vote -> {
      byte[] srAddress = vote.getVoteAddress().toByteArray();
      long totalReward = delegationStore.getReward(cycle, srAddress);
      if (totalReward <= 0) {
        return 0;
      }
      long totalVote = delegationStore.getWitnessVote(cycle, srAddress);
      if (totalVote <= 0) {
        return 0;
      }
      long userVote = vote.getVoteCount();
      double voteRate = (double) userVote / totalVote;
      return (long) (voteRate * totalReward);
    }).sum();
  }

  public long getReward(byte[] address, long cycle) {
    return rewardCacheStore.getReward(Bytes.concat(address, Longs.toByteArray(cycle)));
  }
}
