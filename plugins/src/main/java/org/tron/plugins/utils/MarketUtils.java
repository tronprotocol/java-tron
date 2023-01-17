package org.tron.plugins.utils;

import java.math.BigInteger;
import org.tron.plugins.utils.ByteArray;

public class MarketUtils {

  public static final int TOKEN_ID_LENGTH = ByteArray
      .fromString(Long.toString(Long.MAX_VALUE)).length; // 19



  /**
   * In order to avoid the difference between the data of same key stored and fetched by hashMap and
   * levelDB, when creating the price key, we will find the GCD (Greatest Common Divisor) of
   * sellTokenQuantity and buyTokenQuantity.
   */
  public static byte[] createPairPriceKey(byte[] sellTokenId, byte[] buyTokenId,
                                          long sellTokenQuantity, long buyTokenQuantity) {

    byte[] sellTokenQuantityBytes;
    byte[] buyTokenQuantityBytes;

    // cal the GCD
    long gcd = findGCD(sellTokenQuantity, buyTokenQuantity);
    if (gcd == 0) {
      sellTokenQuantityBytes = ByteArray.fromLong(sellTokenQuantity);
      buyTokenQuantityBytes = ByteArray.fromLong(buyTokenQuantity);
    } else {
      sellTokenQuantityBytes = ByteArray.fromLong(sellTokenQuantity / gcd);
      buyTokenQuantityBytes = ByteArray.fromLong(buyTokenQuantity / gcd);
    }

    return doCreatePairPriceKey(sellTokenId, buyTokenId,
        sellTokenQuantityBytes, buyTokenQuantityBytes);
  }

  public static long findGCD(long number1, long number2) {
    if (number1 == 0 || number2 == 0) {
      return 0;
    }
    return calGCD(number1, number2);
  }

  private static long calGCD(long number1, long number2) {
    if (number2 == 0) {
      return number1;
    }
    return calGCD(number2, number1 % number2);
  }


  private static byte[] doCreatePairPriceKey(byte[] sellTokenId, byte[] buyTokenId,
                                             byte[] sellTokenQuantity, byte[] buyTokenQuantity) {
    byte[] result = new byte[TOKEN_ID_LENGTH + TOKEN_ID_LENGTH
        + sellTokenQuantity.length + buyTokenQuantity.length];

    System.arraycopy(sellTokenId, 0, result, 0, sellTokenId.length);
    System.arraycopy(buyTokenId, 0, result, TOKEN_ID_LENGTH, buyTokenId.length);
    System.arraycopy(sellTokenQuantity, 0, result,
        TOKEN_ID_LENGTH + TOKEN_ID_LENGTH,
        sellTokenQuantity.length);
    System.arraycopy(buyTokenQuantity, 0, result,
        TOKEN_ID_LENGTH + TOKEN_ID_LENGTH + buyTokenQuantity.length,
        buyTokenQuantity.length);

    return result;
  }


  public static int comparePriceKey(byte[] o1, byte[] o2) {
    //compare pair
    byte[] pair1 = new byte[TOKEN_ID_LENGTH * 2];
    byte[] pair2 = new byte[TOKEN_ID_LENGTH * 2];

    System.arraycopy(o1, 0, pair1, 0, TOKEN_ID_LENGTH * 2);
    System.arraycopy(o2, 0, pair2, 0, TOKEN_ID_LENGTH * 2);

    int pairResult = ByteArray.compareUnsigned(pair1, pair2);
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
      return Long.compare(Math.multiplyExact(price1BuyQuantity, price2SellQuantity),
          Math.multiplyExact(price2BuyQuantity, price1SellQuantity));

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
}
