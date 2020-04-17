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

  @Override
  public int compare(byte[] o1, byte[] o2) {

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

    MarketPrice p1 = MarketPrice.newBuilder().setSellTokenQuantity(sellTokenQuantity1)
        .setBuyTokenQuantity(buyTokenQuantity1).build();
    MarketPrice p2 = MarketPrice.newBuilder().setSellTokenQuantity(sellTokenQuantity2)
        .setBuyTokenQuantity(buyTokenQuantity2).build();

    return MarketUtils.comparePrice(p1, p2);

  }

}
