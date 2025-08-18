package org.tron.core.services.jsonrpc.filters;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.bloom.Bloom;
import org.tron.common.crypto.Hash;
import org.tron.core.exception.jsonrpc.JsonRpcTooManyResultException;
import org.tron.core.store.SectionBloomStore;

/**
 * query possible block list by logFilterWrapper
 * warning: must not use bitSet.set(0, bitSet.length()) !
 */
@Slf4j(topic = "API")
public class LogBlockQuery {

  public static final int MAX_RESULT = 10000;
  private final LogFilterWrapper logFilterWrapper;
  private final SectionBloomStore sectionBloomStore;
  private final ExecutorService sectionExecutor;

  private final int minSection;
  private final int maxSection;
  private final long minBlock;
  private long maxBlock;
  private final long currentMaxBlockNum;

  public LogBlockQuery(LogFilterWrapper logFilterWrapper, SectionBloomStore sectionBloomStore,
      long currentMaxBlockNum, ExecutorService executor) {
    this.logFilterWrapper = logFilterWrapper;
    this.sectionBloomStore = sectionBloomStore;
    this.sectionExecutor = executor;
    this.currentMaxBlockNum = currentMaxBlockNum;

    if (logFilterWrapper.getFromBlock() == Long.MAX_VALUE) {
      minBlock = currentMaxBlockNum;
    } else {
      minBlock = logFilterWrapper.getFromBlock();
    }
    minSection = (int) (minBlock / Bloom.BLOOM_BIT_SIZE);

    if (logFilterWrapper.getToBlock() == Long.MAX_VALUE) {
      maxBlock = currentMaxBlockNum;
    } else {
      maxBlock = logFilterWrapper.getToBlock();
      if (maxBlock > currentMaxBlockNum) {
        maxBlock = currentMaxBlockNum;
      }
    }
    maxSection = (int) (maxBlock / Bloom.BLOOM_BIT_SIZE);
  }

  public List<Long> getPossibleBlock() throws ExecutionException, InterruptedException,
      JsonRpcTooManyResultException {
    List<Long> blockNumList = new ArrayList<>();
    if (minBlock > currentMaxBlockNum) {
      return blockNumList;
    }

    int[][][] allConditionsIndex = getConditions();

    int capacity = (maxSection - minSection + 1) * SectionBloomStore.BLOCK_PER_SECTION;
    BitSet blockNumBitSet = new BitSet(capacity);
    blockNumBitSet.set(0, capacity);

    // works serial
    for (int[][] conditionsIndex : allConditionsIndex) {
      BitSet bitSet = subMatch(conditionsIndex);
      blockNumBitSet.and(bitSet);
    }

    for (int i = blockNumBitSet.nextSetBit(0); i >= 0; i = blockNumBitSet.nextSetBit(i + 1)) {
      // operate on index i here
      if (i == Integer.MAX_VALUE) {
        break; // or (i+1) would overflow
      }
      long blockNum = (long) minSection * SectionBloomStore.BLOCK_PER_SECTION + i;
      if (minBlock <= blockNum && blockNum <= maxBlock) {
        blockNumList.add(blockNum);
      }
    }

    if (blockNumList.size() >= MAX_RESULT) {
      throw new JsonRpcTooManyResultException(
          "query returned more than " + MAX_RESULT + " results");
    }

    return blockNumList;
  }

  /**
   * address -> subMatch0,
   * topic1 -> subMatch1, topic2 -> subMatch2, topic3 -> subMatch3, topic4 -> subMatch4
   * works serial, return a BitSet with capacity of section num * blockPerSection
   */
  private BitSet subMatch(int[][] bitIndexes) throws ExecutionException, InterruptedException {

    int capacity = (maxSection - minSection + 1) * SectionBloomStore.BLOCK_PER_SECTION;
    BitSet subBitSet = new BitSet(capacity);

    for (int section = minSection; section <= maxSection; section++) {
      BitSet partialBitSet = partialMatch(bitIndexes, section);

      for (int i = partialBitSet.nextSetBit(0); i >= 0; i = partialBitSet.nextSetBit(i + 1)) {
        // operate on index i here
        if (i == Integer.MAX_VALUE) {
          break; // or (i+1) would overflow
        }
        int offset = (section - minSection) * SectionBloomStore.BLOCK_PER_SECTION + i;
        subBitSet.set(offset);
      }
    }

    return subBitSet;
  }

  /**
   * Match blocks using optimized bloom filter operations. This method reduces database queries
   * and BitSet operations by handling duplicate bit indexes and skipping invalid groups.
   *
   * @param bitIndexes A 2D array where:
   *                   - First dimension represents different topic/address (OR)
   *                   - Second dimension contains bit indexes within each topic/address (AND)
   *                   Example: [[1,2,3], [4,5,6]] means (1 AND 2 AND 3) OR (4 AND 5 AND 6)
   * @param section The section number in the bloom filter store to query
   * @return A BitSet representing the matching blocks in this section
   * @throws ExecutionException If there's an error in concurrent execution
   * @throws InterruptedException If the concurrent execution is interrupted
   */
  private BitSet partialMatch(final int[][] bitIndexes, int section)
      throws ExecutionException, InterruptedException {
    // 1. Collect all unique bitIndexes
    Set<Integer> uniqueBitIndexes = new HashSet<>();
    for (int[] index : bitIndexes) {
      for (int bitIndex : index) { //normally 3, but could be less due to hash collisions
        uniqueBitIndexes.add(bitIndex);
      }
    }

    // 2. Submit concurrent requests for all unique bitIndexes
    Map<Integer, Future<BitSet>> bitIndexResults = new HashMap<>();
    for (int bitIndex : uniqueBitIndexes) {
      Future<BitSet> future
          = sectionExecutor.submit(() -> sectionBloomStore.get(section, bitIndex));
      bitIndexResults.put(bitIndex, future);
    }

    // 3. Wait for all results and cache them
    Map<Integer, BitSet> resultCache = new HashMap<>();
    for (Map.Entry<Integer, Future<BitSet>> entry : bitIndexResults.entrySet()) {
      BitSet result = entry.getValue().get();
      if (result != null) {
        resultCache.put(entry.getKey(), result);
      }
    }


    // 4. Process valid groups with reused BitSet objects
    BitSet finalResult = new BitSet(SectionBloomStore.BLOCK_PER_SECTION);
    BitSet tempBitSet = new BitSet(SectionBloomStore.BLOCK_PER_SECTION);

    for (int[] index : bitIndexes) {

      // init tempBitSet with all 1
      tempBitSet.set(0, SectionBloomStore.BLOCK_PER_SECTION);

      // and condition in second dimension
      for (int bitIndex : index) {
        BitSet cached = resultCache.get(bitIndex);
        if (cached == null) { //match nothing
          tempBitSet.clear();
          break;
        }
        // "and" condition in second dimension
        tempBitSet.and(cached);
        if (tempBitSet.isEmpty()) {
          break;
        }
      }

      // "or" condition in first dimension
      if (!tempBitSet.isEmpty()) {
        finalResult.or(tempBitSet);
      }
    }

    return finalResult;
  }

  /**
   * convert LogFilter to the condition as 3 dimension array
   */
  public int[][][] getConditions() {

    LogFilter logFilter = logFilterWrapper.getLogFilter();
    List<byte[][]> conditions = new ArrayList<>();
    if (ArrayUtils.isNotEmpty(logFilter.getContractAddresses())) {
      conditions.add(logFilter.getContractAddresses());
    }
    for (byte[][] topicList : logFilter.getTopics()) {
      if (ArrayUtils.isNotEmpty(topicList)) {
        conditions.add(topicList);
      }
    }

    int[][][] allConditionsIndex = new int[conditions.size()][][];

    for (int k = 0; k < conditions.size(); k++) {
      byte[][] conditionByte = conditions.get(k);

      int[][] bitIndexes = new int[conditionByte.length][];
      for (int j = 0; j < conditionByte.length; j++) {

        byte[] hash = Hash.sha3(conditionByte[j]);
        Bloom bloom = Bloom.create(hash);
        BitSet bs = BitSet.valueOf(bloom.getData());

        //number of nonZero positions may be equal or less than number(3) of hash function in Bloom
        List<Integer> bitIndexList = new ArrayList<>();
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
          // operate on index i here
          if (i == Integer.MAX_VALUE) {
            break; // or (i+1) would overflow
          }
          bitIndexList.add(i);
        }

        bitIndexes[j] = bitIndexList.stream().mapToInt(Integer::intValue).toArray();
      }
      allConditionsIndex[k] = bitIndexes;
    }

    return allConditionsIndex;
  }
}
