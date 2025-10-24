package org.tron.common.utils;

import java.nio.ByteBuffer;
import org.rocksdb.AbstractComparator;
import org.rocksdb.ComparatorOptions;

public class MarketOrderPriceComparatorForRocksDB extends AbstractComparator {

  public MarketOrderPriceComparatorForRocksDB(final ComparatorOptions copt) {
    super(copt);
  }

  @Override
  public String name() {
    return "MarketOrderPriceComparator";
  }

  @Override
  public int compare(final ByteBuffer a, final ByteBuffer b) {
    return MarketComparator.comparePriceKey(convertDataToBytes(a), convertDataToBytes(b));
  }

  /**
   * DirectSlice.data().array will throw UnsupportedOperationException.
   * */
  public byte[] convertDataToBytes(ByteBuffer buf) {
    byte[] bytes = new byte[buf.remaining()];
    buf.get(bytes);
    return bytes;
  }

}
