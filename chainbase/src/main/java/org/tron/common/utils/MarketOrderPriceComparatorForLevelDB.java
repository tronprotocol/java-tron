package org.tron.common.utils;

import org.spongycastle.util.Arrays;
import org.tron.core.capsule.utils.MarketUtils;
import org.tron.protos.Protocol.MarketPrice;

public class MarketOrderPriceComparatorForLevelDB implements org.iq80.leveldb.DBComparator {

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

  /**
   * Note, for this compare, we should ensure token and quantity are all bigger than 0. Otherwise,
   * when quantity is 0, the result of compare this key with others will be 0, but actually the
   * result should be -1.
   */
  @Override
  public int compare(byte[] o1, byte[] o2) {
    return MarketUtils.comparePriceKey(o1, o2);
  }

}
