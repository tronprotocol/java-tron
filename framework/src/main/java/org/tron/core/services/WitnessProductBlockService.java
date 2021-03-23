package org.tron.core.services;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.BlockCapsule;

@Slf4j(topic = "witness")
@Service
public class WitnessProductBlockService {

  private Cache<Long, BlockCapsule> historyBlockCapsuleCache = CacheBuilder.newBuilder()
      .initialCapacity(200).maximumSize(200).build();

  private Map<String, CheatWitnessInfo> cheatWitnessInfoMap = new HashMap<>();

  public void validWitnessProductTwoBlock(BlockCapsule block) {
    try {
      BlockCapsule blockCapsule = historyBlockCapsuleCache.getIfPresent(block.getNum());
      if (blockCapsule != null && Arrays.equals(blockCapsule.getWitnessAddress().toByteArray(),
          block.getWitnessAddress().toByteArray()) && !Arrays.equals(block.getBlockId().getBytes(),
          blockCapsule.getBlockId().getBytes())) {
        String key = ByteArray.toHexString(block.getWitnessAddress().toByteArray());
        if (!cheatWitnessInfoMap.containsKey(key)) {
          CheatWitnessInfo cheatWitnessInfo = new CheatWitnessInfo();
          cheatWitnessInfoMap.put(key, cheatWitnessInfo);
        }
        cheatWitnessInfoMap.get(key).clear().setTime(System.currentTimeMillis())
            .setLatestBlockNum(block.getNum()).add(block).add(blockCapsule).increment();
      } else {
        historyBlockCapsuleCache.put(block.getNum(), new BlockCapsule(block.getInstance()));
      }
    } catch (Exception e) {
      logger.error("valid witness same time product two block fail! blockNum: {}, blockHash: {}",
          block.getNum(), block.getBlockId().toString(), e);
    }
  }

  public Map<String, CheatWitnessInfo> queryCheatWitnessInfo() {
    return cheatWitnessInfoMap;
  }

  public static class CheatWitnessInfo {

    private AtomicInteger times = new AtomicInteger(0);
    private long latestBlockNum;
    private Set<BlockCapsule> blockCapsuleSet = new HashSet<>();
    private long time;

    public CheatWitnessInfo increment() {
      times.incrementAndGet();
      return this;
    }

    public AtomicInteger getTimes() {
      return times;
    }

    public CheatWitnessInfo setTimes(AtomicInteger times) {
      this.times = times;
      return this;
    }

    public long getLatestBlockNum() {
      return latestBlockNum;
    }

    public CheatWitnessInfo setLatestBlockNum(long latestBlockNum) {
      this.latestBlockNum = latestBlockNum;
      return this;
    }

    public Set<BlockCapsule> getBlockCapsuleSet() {
      return new HashSet<>(blockCapsuleSet);
    }

    public CheatWitnessInfo setBlockCapsuleSet(Set<BlockCapsule> blockCapsuleSet) {
      this.blockCapsuleSet = new HashSet<>(blockCapsuleSet);
      return this;
    }

    public CheatWitnessInfo clear() {
      blockCapsuleSet.clear();
      return this;
    }

    public CheatWitnessInfo add(BlockCapsule blockCapsule) {
      blockCapsuleSet.add(blockCapsule);
      return this;
    }

    public long getTime() {
      return time;
    }

    public CheatWitnessInfo setTime(long time) {
      this.time = time;
      return this;
    }

    @Override
    public String toString() {
      return "{"
          + "times=" + times.get()
          + ", time=" + time
          + ", latestBlockNum=" + latestBlockNum
          + ", blockCapsuleSet=" + blockCapsuleSet
          + '}';
    }
  }
}
