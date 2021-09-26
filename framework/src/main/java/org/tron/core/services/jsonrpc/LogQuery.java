package org.tron.core.services.jsonrpc;

import java.util.BitSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import org.tron.common.logsfilter.Bloom;
import org.tron.core.store.SectionBloomStore;

public class LogQuery {

  private LogFilterWrapper logFilterWrapper;
  private SectionBloomStore sectionBloomStore;
  private ExecutorService executor;

  public LogQuery(LogFilterWrapper logFilterWrapper, SectionBloomStore sectionBloomStore,
      ExecutorService executor) {
    this.logFilterWrapper = logFilterWrapper;
    this.sectionBloomStore = sectionBloomStore;
    this.executor = executor;
  }

  private List<Long> possibleBlock() {

    return null;
  }

  //address -> subMatch0,
  //topic1 -> subMatch1, topic2 -> subMatch2, topic3 -> subMatch3, topic4 -> subMatch4
  //works serial
  private BitSet subMatch(int[][] bitIndexes) {
    int minSection = (int) (logFilterWrapper.fromBlock / Bloom.bloom_bit_size);
    int maxSection = (int) (logFilterWrapper.toBlock / Bloom.bloom_bit_size);

    for (int section = minSection; section < maxSection; section++) {
      BitSet bitSet = partialMatch(bitIndexes, section);
    }
    return null;
  }

  //every section has a section, works parallel
  // and condition in second dimension, or condition in first dimension
  private BitSet partialMatch(int[][] bitIndexes, int section) {
    for (int i = 0; i < bitIndexes.length; i++) {
      for (int j = 0; j <= bitIndexes[i].length; j++) {

      }
    }
    return null;
  }

  class Task implements Callable<BitSet> {

    private int section;
    private int bitIndex;
    private SectionBloomStore sectionBloomStore;

    public Task(int section, int bitIndex, SectionBloomStore sectionBloomStore) {
      this.section = section;
      this.bitIndex = bitIndex;
      this.sectionBloomStore = sectionBloomStore;
    }

    @Override
    public BitSet call() throws Exception {
      //bitset size is blockPerSection
      return sectionBloomStore.get(section, bitIndex);
    }
  }
}
