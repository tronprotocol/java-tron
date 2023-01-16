package org.tron.plugins.comparator;

import org.iq80.leveldb.DBComparator;
import org.tron.plugins.utils.MarketUtils;

public class MarketOrderPriceComparatorForLevelDB implements DBComparator {

  @Override
  public String name() {
    return "MarketOrderPriceComparator";
  }

  @Override
  public byte[] findShortestSeparator(byte[] start, byte[] limit) {
    return new byte[0];
  }

  @Override
  public byte[] findShortSuccessor(byte[] key) {
    return new byte[0];
  }

  @Override
  public int compare(byte[] o1, byte[] o2) {
    return MarketUtils.comparePriceKey(o1, o2);
  }

}