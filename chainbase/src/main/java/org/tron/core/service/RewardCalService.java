package org.tron.core.service;

import static org.tron.core.store.DelegationStore.DECIMAL_OF_VI_REWARD;
import static org.tron.core.store.DelegationStore.REMARK;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.math.BigInteger;
import java.util.List;
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
import org.tron.common.utils.Pair;
import org.tron.core.db.common.iterator.DBIterator;
import org.tron.core.db2.common.DB;
import org.tron.core.store.DelegationStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.RewardViStore;
import org.tron.core.store.WitnessStore;

@Component
@Slf4j(topic = "rewardCalService")
public class RewardCalService {

  private final DB<byte[], byte[]> propertiesStore;
  private final DB<byte[], byte[]> delegationStore;
  private final DB<byte[], byte[]> witnessStore;

  @Autowired
  private RewardViStore rewardViStore;


  private static final byte[] IS_DONE_KEY = new byte[]{0x00};
  private static final byte[] IS_DONE_VALUE = new byte[]{0x01};

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
  private void init() {
    this.newRewardCalStartCycle = this.getNewRewardAlgorithmEffectiveCycle();
    if (newRewardCalStartCycle != Long.MAX_VALUE) {
      if (rewardViStore.has(IS_DONE_KEY)) {
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

  public void calReward() {
    es.submit(this::startRewardCal);
  }

  public void calRewardForTest() {
    this.newRewardCalStartCycle = this.getNewRewardAlgorithmEffectiveCycle();
    if (rewardViStore.has(IS_DONE_KEY)) {
      logger.info("RewardCalService is already done");
      return;
    }
    startRewardCal();
  }


  public long getNewRewardAlgorithmReward(long beginCycle, long endCycle,
                                          List<Pair<byte[], Long>> votes) {
    if (!rewardViStore.has(IS_DONE_KEY)) {
      return -1;
    }
    long reward = 0;
    if (beginCycle < endCycle) {
      for (Pair<byte[], Long> vote : votes) {
        byte[] srAddress = vote.getKey();
        BigInteger beginVi = getWitnessVi(beginCycle - 1, srAddress);
        BigInteger endVi = getWitnessVi(endCycle - 1, srAddress);
        BigInteger deltaVi = endVi.subtract(beginVi);
        if (deltaVi.signum() <= 0) {
          continue;
        }
        long userVote = vote.getValue();
        reward += deltaVi.multiply(BigInteger.valueOf(userVote))
            .divide(DelegationStore.DECIMAL_OF_VI_REWARD).longValue();
      }
    }
    // TODO: remove this code after old reward algorithm optimization
    // TODO: ADD PROPOSAL NEED TO CHECK IS_DONE
    reward = -1;
    return reward;
  }

  private void startRewardCal() {
    logger.info("RewardCalService start");
    rewardViStore.reset();
    DBIterator iterator = (DBIterator) witnessStore.iterator();
    iterator.seekToFirst();
    iterator.forEachRemaining(e -> accumulateWitnessReward(e.getKey()));
    rewardViStore.put(IS_DONE_KEY, IS_DONE_VALUE);
    logger.info("RewardCalService is done");
  }

  private void accumulateWitnessReward(byte[] witness) {
    long startCycle = 1;
    LongStream.range(startCycle, newRewardCalStartCycle)
        .forEach(cycle -> accumulateWitnessVi(cycle, witness));
  }

  private void accumulateWitnessVi(long cycle, byte[] address) {
    BigInteger preVi = getWitnessVi(cycle - 1, address);
    long voteCount = getWitnessVote(cycle, address);
    long reward = getReward(cycle, address);
    if (reward == 0 || voteCount == 0) { // Just forward pre vi
      if (!BigInteger.ZERO.equals(preVi)) { // Zero vi will not be record
        setWitnessVi(cycle, address, preVi);
      }
    } else { // Accumulate delta vi
      BigInteger deltaVi = BigInteger.valueOf(reward)
          .multiply(DECIMAL_OF_VI_REWARD)
          .divide(BigInteger.valueOf(voteCount));
      setWitnessVi(cycle, address, preVi.add(deltaVi));
    }
  }

  private void setWitnessVi(long cycle, byte[] address, BigInteger value) {
    byte[] k = buildViKey(cycle, address);
    byte[] v = value.toByteArray();
    rewardViStore.put(k, v);
  }

  private BigInteger getWitnessVi(long cycle, byte[] address) {

    byte[] v = rewardViStore.get(buildViKey(cycle, address));
    if (v == null) {
      return BigInteger.ZERO;
    } else {
      return new BigInteger(v);
    }
  }

  private byte[] buildViKey(long cycle, byte[] address) {
    return generateKey(cycle, address, "vi");
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

