package org.tron.common.utils;

import java.math.BigInteger;

public class MarketComparator {

  public static final int TOKEN_ID_LENGTH = Long.toString(Long.MAX_VALUE).getBytes().length; // 19


  public static int comparePriceKey(byte[] o1, byte[] o2) {
    //compare pair
    byte[] pair1 = new byte[TOKEN_ID_LENGTH * 2];
    byte[] pair2 = new byte[TOKEN_ID_LENGTH * 2];

    System.arraycopy(o1, 0, pair1, 0, TOKEN_ID_LENGTH * 2);
    System.arraycopy(o2, 0, pair2, 0, TOKEN_ID_LENGTH * 2);

    int pairResult = compareUnsigned(pair1, pair2);
    if (pairResult != 0) {
      return pairResult;
    }

    //compare price
    byte[] getSellTokenQuantity1 = new byte[8];
    byte[] getBuyTokenQuantity1 = new byte[8];

    byte[] getSellTokenQuantity2 = new byte[8];
    byte[] getBuyTokenQuantity2 = new byte[8];

    int longByteNum = 8;

    System.arraycopy(o1, TOKEN_ID_LENGTH + TOKEN_ID_LENGTH,
        getSellTokenQuantity1, 0, longByteNum);
    System.arraycopy(o1, TOKEN_ID_LENGTH + TOKEN_ID_LENGTH + longByteNum,
        getBuyTokenQuantity1, 0, longByteNum);

    System.arraycopy(o2, TOKEN_ID_LENGTH + TOKEN_ID_LENGTH,
        getSellTokenQuantity2, 0, longByteNum);
    System.arraycopy(o2, TOKEN_ID_LENGTH + TOKEN_ID_LENGTH + longByteNum,
        getBuyTokenQuantity2, 0, longByteNum);

    long sellTokenQuantity1 = toLong(getSellTokenQuantity1);
    long buyTokenQuantity1 = toLong(getBuyTokenQuantity1);
    long sellTokenQuantity2 = toLong(getSellTokenQuantity2);
    long buyTokenQuantity2 = toLong(getBuyTokenQuantity2);

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

    return comparePrice(sellTokenQuantity1, buyTokenQuantity1,
        sellTokenQuantity2, buyTokenQuantity2);

  }

  /**
   * Note: the params should be the same token pair, or you should change the order.
   * All the quantity should be bigger than 0.
   * */
  public static int comparePrice(long price1SellQuantity, long price1BuyQuantity,
                                 long price2SellQuantity, long price2BuyQuantity) {
    try {
      return Long.compare(StrictMath.multiplyExact(price1BuyQuantity, price2SellQuantity),
          StrictMath.multiplyExact(price2BuyQuantity, price1SellQuantity));

    } catch (ArithmeticException ex) {
      // do nothing here, because we will use BigInteger to compute again
    }

    BigInteger price1BuyQuantityBI = BigInteger.valueOf(price1BuyQuantity);
    BigInteger price1SellQuantityBI = BigInteger.valueOf(price1SellQuantity);
    BigInteger price2BuyQuantityBI = BigInteger.valueOf(price2BuyQuantity);
    BigInteger price2SellQuantityBI = BigInteger.valueOf(price2SellQuantity);

    return price1BuyQuantityBI.multiply(price2SellQuantityBI)
        .compareTo(price2BuyQuantityBI.multiply(price1SellQuantityBI));
  }

  /**
   * copy from org.bouncycastle.util.Arrays.compareUnsigned
   */
  private static int compareUnsigned(byte[] a, byte[] b) {
    if (a == b) {
      return 0;
    }
    if (a == null) {
      return -1;
    }
    if (b == null) {
      return 1;
    }
    int minLen = StrictMath.min(a.length, b.length);
    for (int i = 0; i < minLen; ++i) {
      int aVal = a[i] & 0xFF;
      int bVal = b[i] & 0xFF;
      if (aVal < bVal) {
        return -1;
      }
      if (aVal > bVal) {
        return 1;
      }
    }
    if (a.length < b.length) {
      return -1;
    }
    if (a.length > b.length) {
      return 1;
    }
    return 0;
  }

  public static long toLong(byte[] b) {
    return (b == null || b.length == 0) ? 0 : new BigInteger(1, b).longValue();
  }
}
