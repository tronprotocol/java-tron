package org.tron.core.service;

import static org.tron.core.store.DelegationStore.REMARK;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.LongStream;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.db.common.iterator.DBIterator;
import org.tron.core.db2.common.DB;
import org.tron.core.store.DelegationStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.RewardCacheStore;
import org.tron.core.store.WitnessStore;

@Component
@Slf4j(topic = "rewardCalService")
public class RewardCalService {

  private final DB<byte[], byte[]> propertiesStore;
  private final DB<byte[], byte[]> delegationStore;
  private final DB<byte[], byte[]> witnessStore;

  @Autowired
  private RewardCacheStore rewardCacheStore;


  private static final byte[] IS_DONE_KEY = new byte[]{0x00};
  private static final byte[] IS_DONE_VALUE = new byte[]{0x01};

  private volatile boolean isDone = false;

  private long newRewardCalStartCycle = Long.MAX_VALUE;

  private final ExecutorService es = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("rewardCalService").build());


  @Autowired
  public RewardCalService(@Autowired  DynamicPropertiesStore propertiesStore,
      @Autowired DelegationStore delegationStore, @Autowired WitnessStore witnessStore) {
    this.propertiesStore = propertiesStore.getDb();
    this.delegationStore = delegationStore.getDb();
    this.witnessStore = witnessStore.getDb();
  }

  @PostConstruct
  private void init() throws IOException {
    this.newRewardCalStartCycle = this.getNewRewardAlgorithmEffectiveCycle();
    if (newRewardCalStartCycle != Long.MAX_VALUE) {
      isDone = rewardCacheStore.has(IS_DONE_KEY);
      if (isDone) {
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
    es.submit(this::startRewardCal);
  }

  public void calRewardForTest() throws IOException {
    this.newRewardCalStartCycle = this.getNewRewardAlgorithmEffectiveCycle();
    isDone = rewardCacheStore.has(IS_DONE_KEY);
    if (isDone) {
      logger.info("RewardCalService is already done");
      return;
    }
    startRewardCal();
  }

  private void startRewardCal() {
    logger.info("RewardCalService start");
    rewardCacheStore.reset();
    DBIterator iterator = (DBIterator) witnessStore.iterator();
    iterator.seekToFirst();
    iterator.forEachRemaining(e -> doRewardCache(e.getKey()));
    rewardCacheStore.put(IS_DONE_KEY, IS_DONE_VALUE);
    isDone = true;
    logger.info("RewardCalService is done");
  }

  private void doRewardCache(byte[] witness) {
    long startCycle = 1;
    LongStream.range(startCycle, newRewardCalStartCycle)
        .forEach(cycle -> cacheRewardAndVote(cycle, witness));
  }

  private void cacheRewardAndVote(long cycle, byte[] witness) {
    long totalReward = this.getReward(cycle, witness);
    long totalVote = this.getWitnessVote(cycle, witness);
    this.putWitnessCache(cycle, witness, totalReward, totalVote);
  }

  public long getRewardCache(byte[] address, long cycle) {
    if (isDone) {
      byte[] v = rewardCacheStore.get(generateKey(cycle, address, "reward"));
      return v == null ? 0 : ByteArray.toLong(v);
    }
    isDone = rewardCacheStore.has(IS_DONE_KEY);
    return REMARK;
  }

  public long getVoteCache(byte[] address, long cycle) {
    if (isDone) {
      byte[] v = rewardCacheStore.get(generateKey(cycle, address, "vote"));
      return v == null ? REMARK : ByteArray.toLong(v);
    }
    isDone = rewardCacheStore.has(IS_DONE_KEY);
    return REMARK;
  }

  private void putWitnessCache(long cycle, byte[] address, long reward, long vote) {
    if (reward <= 0 || vote <= 0) {
      return;
    }
    rewardCacheStore.put(generateKey(cycle, address, "reward"), ByteArray.fromLong(reward));
    rewardCacheStore.put(generateKey(cycle, address, "vote"), ByteArray.fromLong(vote));
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

