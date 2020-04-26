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
   * Note, for this compare, we should ensure token and quantity are all bigger than 0.
   * Otherwise, when quantity is 0, the result of compare this key with others will be 0, but
   * actually the result should be -1.
   * */
  @Override
  public int compare(byte[] o1, byte[] o2) {
    return compareIn(o1, o2);
  }

  public static int compareIn(byte[] o1, byte[] o2) {
    //compare pair
    byte[] pair1 = new byte[MarketUtils.TOKEN_ID_LENGTH * 2];
    byte[] pair2 = new byte[MarketUtils.TOKEN_ID_LENGTH * 2];

    System.arraycopy(o1, 0, pair1, 0, MarketUtils.TOKEN_ID_LENGTH * 2);
    System.arraycopy(o2, 0, pair2, 0, MarketUtils.TOKEN_ID_LENGTH * 2);

    int pairResult = Arrays.compareUnsigned(pair1, pair2);
    if (pairResult != 0) {
      return pairResult;
    }

    //compare price
    byte[] getSellTokenQuantity1 = new byte[8];
    byte[] getBuyTokenQuantity1 = new byte[8];

    byte[] getSellTokenQuantity2 = new byte[8];
    byte[] getBuyTokenQuantity2 = new byte[8];

    int longByteNum = 8;

    System.arraycopy(o1, MarketUtils.TOKEN_ID_LENGTH + MarketUtils.TOKEN_ID_LENGTH,
        getSellTokenQuantity1, 0, longByteNum);
    System.arraycopy(o1, MarketUtils.TOKEN_ID_LENGTH + MarketUtils.TOKEN_ID_LENGTH + longByteNum,
        getBuyTokenQuantity1, 0, longByteNum);

    System.arraycopy(o2, MarketUtils.TOKEN_ID_LENGTH + MarketUtils.TOKEN_ID_LENGTH,
        getSellTokenQuantity2, 0, longByteNum);
    System.arraycopy(o2, MarketUtils.TOKEN_ID_LENGTH + MarketUtils.TOKEN_ID_LENGTH + longByteNum,
        getBuyTokenQuantity2, 0, longByteNum);

    long sellTokenQuantity1 = ByteArray.toLong(getSellTokenQuantity1);
    long buyTokenQuantity1 = ByteArray.toLong(getBuyTokenQuantity1);
    long sellTokenQuantity2 = ByteArray.toLong(getSellTokenQuantity2);
    long buyTokenQuantity2 = ByteArray.toLong(getBuyTokenQuantity2);

    if ((sellTokenQuantity1 == 0 || buyTokenQuantity1 == 0)
        && (sellTokenQuantity2 == 0 || buyTokenQuantity2 == 0)) {
      return 0;
    }

    if (sellTokenQuantity1 == 0 || buyTokenQuantity1 == 0) {
      return -1;
    }

    if (sellTokenQuantity2 == 0 || buyTokenQuantity2 == 0) {
      return 1;
    }

    return MarketUtils.comparePrice(sellTokenQuantity1, buyTokenQuantity1,
        sellTokenQuantity2, buyTokenQuantity2);

  }

  public boolean greaterOrEquals(byte[] bytes1, byte[] bytes2) {
    return compare(bytes1, bytes2) >= 0;
  }
}
