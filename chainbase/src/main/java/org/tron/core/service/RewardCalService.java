package org.tron.core.service;

import static org.tron.core.store.DelegationStore.REMARK;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.prometheus.client.Histogram;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.LongStream;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.Metrics;
import org.tron.common.utils.ByteArray;
import org.tron.core.db.common.iterator.DBIterator;
import org.tron.core.db2.common.DB;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DelegationStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.RewardCacheStore;
import org.tron.protos.Protocol;

@Component
@Slf4j(topic = "rewardCalService")
public class RewardCalService {

  private final DB<byte[], byte[]> propertiesStore;
  private final DB<byte[], byte[]> delegationStore;
  private final DB<byte[], byte[]> accountStore;

  @Autowired
  private RewardCacheStore rewardCacheStore;


  private  byte[] isDoneKey;
  private static final byte[] IS_DONE_VALUE = new byte[]{0x01};

  private long newRewardCalStartCycle = Long.MAX_VALUE;
  private static final int ADDRESS_SIZE = 21;
  private byte[] lastAccount = new byte[ADDRESS_SIZE];

  private final AtomicBoolean doing = new AtomicBoolean(false);


  private final ExecutorService es = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("rewardCalService").build());


  @Autowired
  public RewardCalService(@Autowired  DynamicPropertiesStore propertiesStore,
      @Autowired DelegationStore delegationStore, @Autowired AccountStore accountStore) {
    this.propertiesStore = propertiesStore.getDb();
    this.delegationStore = delegationStore.getDb();
    this.accountStore = accountStore.getDb();
  }

  @PostConstruct
  private void init() throws IOException {
    this.newRewardCalStartCycle = this.getNewRewardAlgorithmEffectiveCycle();
    if (newRewardCalStartCycle != Long.MAX_VALUE) {
      isDoneKey = ByteArray.fromLong(newRewardCalStartCycle);
      if (rewardCacheStore.has(isDoneKey)) {
        logger.info("RewardCalService is already done");
        return;
      }
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
    this.newRewardCalStartCycle = this.getNewRewardAlgorithmEffectiveCycle();
    isDoneKey = ByteArray.fromLong(newRewardCalStartCycle);
    if (rewardCacheStore.has(isDoneKey)) {
      logger.info("RewardCalService is already done");
      return;
    }
    initLastAccount();
    startRewardCal();
  }

  private void initLastAccount() {
    byte[] value = rewardCacheStore.get("lastAccount".getBytes());
    if (value != null) {
      lastAccount = value;
    }
  }

  private void updateLastAccount(byte[] address) {
    rewardCacheStore.put("lastAccount".getBytes(), address);
  }


  private void startRewardCal() {
    if (!doing.compareAndSet(false, true)) {
      logger.info("RewardCalService is doing");
      return;
    }
    logger.info("RewardCalService start from lastAccount: {}", ByteArray.toHexString(lastAccount));
    DBIterator iterator = (DBIterator) accountStore.iterator();
    iterator.seek(lastAccount);
    iterator.forEachRemaining(e -> {
      try {
        doRewardCal(e.getKey(), e.getValue());
        updateLastAccount(e.getKey());
      } catch (InterruptedException error) {
        Thread.currentThread().interrupt();
      }
    });
    rewardCacheStore.put(this.isDoneKey, IS_DONE_VALUE);
    logger.info("RewardCalService is done");
  }

  private void doRewardCal(byte[] address, byte[] account) throws InterruptedException {
    List<Protocol.Vote> votesList = this.parseVotesList(account);
    if (votesList.isEmpty()) {
      return;
    }
    long beginCycle = this.getBeginCycle(address);
    if (beginCycle >= newRewardCalStartCycle) {
      return;
    }
    long endCycle = this.getEndCycle(address);
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
    Histogram.Timer requestTimer = Metrics.histogramStartTimer(
        MetricKeys.Histogram.DO_REWARD_CAL_DELAY,
        (newRewardCalStartCycle - beginCycle) / 100 + "");
    LongStream.range(beginCycle, newRewardCalStartCycle).forEach(i -> computeReward(i, votesList));
    Metrics.histogramObserve(requestTimer);
  }

  private List<Protocol.Vote> parseVotesList(byte[] account) {
    try {
      return Protocol.Account.parseFrom(account).getVotesList();
    } catch (Exception e) {
      logger.error("parse account error: {}", e.getMessage());
    }
    return new ArrayList<>();
  }

  private void computeReward(long cycle, List<Protocol.Vote> votesList) {
    for (Protocol.Vote vote : votesList) {
      byte[] srAddress = vote.getVoteAddress().toByteArray();
      long totalReward = this.getReward(cycle, srAddress);
      long totalVote = this.getWitnessVote(cycle, srAddress);
      this.putWitnessCache(cycle, srAddress, totalReward, totalVote);
    }
  }

  public long getRewardCache(byte[] address, long cycle) {
    byte[] v = rewardCacheStore.get(generateKey(cycle, address, "reward"));
    return v == null ? REMARK : ByteArray.toLong(v);
  }

  public long getVoteCache(byte[] address, long cycle) {
    byte[] v = rewardCacheStore.get(generateKey(cycle, address, "vote"));
    return v == null ? REMARK : ByteArray.toLong(v);
  }

  private void putWitnessCache(long cycle, byte[] address, long reward, long vote) {
    if (getRewardCache(address, cycle) != REMARK && getVoteCache(address, cycle) != REMARK) {
      return;
    }
    Map<byte[], byte[]> rows = new HashMap<>();
    rows.put(generateKey(cycle, address, "reward"), ByteArray.fromLong(reward));
    rows.put(generateKey(cycle, address, "vote"), ByteArray.fromLong(vote));
    rewardCacheStore.updateByBatch(rows);
  }

  private long getBeginCycle(byte[] address) {
    byte[] value = this.delegationStore.get(address);
    return value == null ? 0 : ByteArray.toLong(value);
  }

  private long getEndCycle(byte[] address) {
    byte[] value = this.delegationStore.get(generateKey("end", address, null));
    return value == null ? REMARK : ByteArray.toLong(value);
  }

  private long getReward(long cycle, byte[] address) {
    byte[] value = this.delegationStore.get(generateKey(cycle, address, "reward"));
    return value == null ? 0 : ByteArray.toLong(value);
  }

  private long getWitnessVote(long cycle, byte[] address) {
    byte[] value = this.delegationStore.get(generateKey(cycle, address, "vote"));
    return value == null ? REMARK : ByteArray.toLong(value);
  }

  private byte[] generateKey(long cycle, byte[] address, String suffix) {
    return generateKey(cycle + "", address, suffix);
  }

  private byte[] generateKey(String prefix, byte[] address, String suffix) {
    StringBuilder sb = new StringBuilder();
    if (prefix != null) {
      sb.append(prefix).append("-");
    }
    sb.append(Hex.toHexString(address));
    if (suffix != null) {
      sb.append("-").append(suffix);
    }
    return sb.toString().getBytes();
  }

  private long getNewRewardAlgorithmEffectiveCycle() {
    byte[] value =  this.propertiesStore.get("NEW_REWARD_ALGORITHM_EFFECTIVE_CYCLE".getBytes());
    return value == null ? Long.MAX_VALUE : ByteArray.toLong(value);
  }
}

