package org.tron.core.service;

import com.google.common.primitives.Bytes;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.InvalidProtocolBufferException;
import io.prometheus.client.Histogram;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
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

  private final AtomicBoolean doing = new AtomicBoolean(false);


  private final ExecutorService es = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("rewardCalService").build());

  @PostConstruct
  private void init() throws IOException {
    newRewardCalStartCycle = propertiesStore.getNewRewardAlgorithmEffectiveCycle();
    if (newRewardCalStartCycle != Long.MAX_VALUE) {
      isDoneKey = ByteArray.fromLong(newRewardCalStartCycle);
      if (rewardCacheStore.has(isDoneKey)) {
        logger.info("RewardCalService is already done");
        return;
      }
      accountIterator = (DBIterator) accountStore.getDb().iterator();
      calReward();
    }
  }

  @PreDestroy
  private void destroy() {
    es.shutdownNow();
  }

  public void calReward() throws IOException {
    initLastAccount();
    es.submit(this::startRewardCal);
  }

  public void calRewardForTest() throws IOException {
    newRewardCalStartCycle = propertiesStore.getNewRewardAlgorithmEffectiveCycle();
    isDoneKey = ByteArray.fromLong(newRewardCalStartCycle);
    if (rewardCacheStore.has(isDoneKey)) {
      logger.info("RewardCalService is already done");
      return;
    }
    accountIterator = (DBIterator) accountStore.getDb().iterator();
    initLastAccount();
    startRewardCal();
  }

  private void initLastAccount() throws IOException {
    try (DBIterator iterator = rewardCacheStore.iterator()) {
      iterator.seekToLast();
      if (iterator.valid()) {
        byte[] key  = iterator.getKey();
        System.arraycopy(key, 0, lastAccount, 0, ADDRESS_SIZE);
      }
    }
  }


  private void startRewardCal() {
    if (!doing.compareAndSet(false, true)) {
      logger.info("RewardCalService is doing");
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
    if (endCycle >= newRewardCalStartCycle) {
      return;
    }
    //skip the last cycle reward
    boolean skipLastCycle = beginCycle + 1 == endCycle;
    if (skipLastCycle) {
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
    this.putReward(address, beginCycle, endCycle, reward, skipLastCycle);
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
    long reward = 0;
    for (Protocol.Vote vote : account.getVotesList()) {
      byte[] srAddress = vote.getVoteAddress().toByteArray();
      long totalReward = delegationStore.getReward(cycle, srAddress);
      if (totalReward <= 0) {
        continue;
      }
      long totalVote = delegationStore.getWitnessVote(cycle, srAddress);
      if (totalVote <= 0) {
        continue;
      }
      long userVote = vote.getVoteCount();
      double voteRate = (double) userVote / totalVote;
      reward += voteRate * totalReward;
    }
    return reward;
  }

  public long getReward(byte[] address, long cycle) {
    return rewardCacheStore.getReward(buildKey(address, cycle));
  }

  private void putReward(byte[] address, long start, long end, long reward, boolean skipLastCycle) {
    long startCycle = delegationStore.getBeginCycle(address);
    long endCycle = delegationStore.getEndCycle(address);
    //skip the last cycle reward
    if (skipLastCycle) {
      startCycle += 1;
    }
    // check if the delegation is still valid
    if (startCycle == start && endCycle == end) {
      rewardCacheStore.putReward(buildKey(address, start), reward);
    }
  }

  private byte[] buildKey(byte[] address, long beginCycle) {
    return Bytes.concat(address, ByteArray.fromLong(beginCycle));
  }
}
