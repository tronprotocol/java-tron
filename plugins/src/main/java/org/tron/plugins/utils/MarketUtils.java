package org.tron.plugins.utils;

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


}
