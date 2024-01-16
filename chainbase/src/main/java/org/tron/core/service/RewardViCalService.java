package org.tron.core.service;

import static org.tron.core.store.DelegationStore.DECIMAL_OF_VI_REWARD;
import static org.tron.core.store.DelegationStore.REMARK;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Streams;
import com.google.common.primitives.Bytes;
import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import javax.annotation.PreDestroy;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.error.TronDBException;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Pair;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.utils.MerkleTree;
import org.tron.core.db.common.iterator.DBIterator;
import org.tron.core.db2.common.DB;
import org.tron.core.store.DelegationStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.RewardViStore;
import org.tron.core.store.WitnessStore;

@Component
@Slf4j(topic = "rewardViCalService")
public class RewardViCalService {

  private final DB<byte[], byte[]> propertiesStore;
  private final DB<byte[], byte[]> delegationStore;
  private final DB<byte[], byte[]> witnessStore;

  @Autowired
  private RewardViStore rewardViStore;

  private static final byte[] IS_DONE_KEY = new byte[]{0x00};
  private static final byte[] IS_DONE_VALUE = new byte[]{0x01};

  private long newRewardCalStartCycle = Long.MAX_VALUE;

  private volatile long lastBlockNumber = -1;

  @VisibleForTesting
  @Setter
  private Sha256Hash rewardViRoot = Sha256Hash.wrap(
      ByteString.fromHex("9debcb9924055500aaae98cdee10501c5c39d4daa75800a996f4bdda73dbccd8"));

  private final CountDownLatch lock = new CountDownLatch(1);

  private final ScheduledExecutorService es = ExecutorServiceManager
      .newSingleThreadScheduledExecutor("rewardViCalService");


  @Autowired
  public RewardViCalService(@Autowired  DynamicPropertiesStore propertiesStore,
                            @Autowired DelegationStore delegationStore, @Autowired WitnessStore witnessStore) {
    this.propertiesStore = propertiesStore.getDb();
    this.delegationStore = delegationStore.getDb();
    this.witnessStore = witnessStore.getDb();
  }

  public void init() {
    // after init, we can get the latest block header number from db
    this.newRewardCalStartCycle = this.getNewRewardAlgorithmEffectiveCycle();
    boolean ret = this.newRewardCalStartCycle != Long.MAX_VALUE;
    if (ret) {
      // checkpoint is flushed to db, we can start rewardViCalService immediately
      lastBlockNumber = Long.MAX_VALUE;
    }
    es.scheduleWithFixedDelay(this::maybeRun, 0, 3, TimeUnit.SECONDS);
  }

  private boolean enableNewRewardAlgorithm() {
    this.newRewardCalStartCycle = this.getNewRewardAlgorithmEffectiveCycle();
    boolean ret = this.newRewardCalStartCycle != Long.MAX_VALUE;
    if (ret && lastBlockNumber == -1) {
      lastBlockNumber = this.getLatestBlockHeaderNumber();
    }
    return ret;
  }

  private boolean isDone() {
    return rewardViStore.has(IS_DONE_KEY);
  }

  private void maybeRun() {
    if (enableNewRewardAlgorithm()) {
      if (this.newRewardCalStartCycle > 1) {
        if (isDone()) {
          this.clearUp(true);
          logger.info("rewardViCalService is already done");
        } else {
          if (lastBlockNumber ==  Long.MAX_VALUE // start rewardViCalService immediately
              || this.getLatestBlockHeaderNumber() > lastBlockNumber) {
            // checkpoint is flushed to db, so we can start rewardViCalService
            startRewardCal();
            clearUp(true);
          } else {
            logger.info("startRewardCal will run after checkpoint is flushed to db");
          }
        }
      } else {
        clearUp(false);
        logger.info("rewardViCalService is no need to run");
      }
    }
  }

  private void clearUp(boolean isDone) {
    lock.countDown();
    if (isDone) {
      calcMerkleRoot();
    }
    es.shutdown();
  }

  @PreDestroy
  private void destroy() {
    es.shutdownNow();
  }


  public long getNewRewardAlgorithmReward(long beginCycle, long endCycle,
                                          List<Pair<byte[], Long>> votes) {
    if (!rewardViStore.has(IS_DONE_KEY)) {
      logger.warn("rewardViCalService is not done, wait for it");
      try {
        lock.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new TronDBException(e);
      }
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
    return reward;

  }

  private void calcMerkleRoot() {
    logger.info("calcMerkleRoot start");
    DBIterator iterator = rewardViStore.iterator();
    iterator.seekToFirst();
    ArrayList<Sha256Hash> ids = Streams.stream(iterator)
        .map(this::getHash)
        .collect(Collectors.toCollection(ArrayList::new));

    Sha256Hash rewardViRootLocal = MerkleTree.getInstance().createTree(ids).getRoot().getHash();
    if (!Objects.equals(rewardViRoot, rewardViRootLocal)) {
      logger.warn("merkle root mismatch, expect: {}, actual: {}",
          rewardViRoot, rewardViRootLocal);
    }
    logger.info("calcMerkleRoot: {}", rewardViRootLocal);
  }

  private Sha256Hash getHash(Map.Entry<byte[], byte[]> entry) {
    return Sha256Hash.of(CommonParameter.getInstance().isECKeyCryptoEngine(),
        Bytes.concat(entry.getKey(), entry.getValue()));
  }

  private void startRewardCal() {
    logger.info("rewardViCalService start");
    rewardViStore.reset();
    DBIterator iterator = (DBIterator) witnessStore.iterator();
    iterator.seekToFirst();
    iterator.forEachRemaining(e -> accumulateWitnessReward(e.getKey()));
    rewardViStore.put(IS_DONE_KEY, IS_DONE_VALUE);
    logger.info("rewardViCalService is done");

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

  private long getLatestBlockHeaderNumber() {
    byte[] value =  this.propertiesStore.get("latest_block_header_number".getBytes());
    return value == null ? 1 : ByteArray.toLong(value);
  }
}

