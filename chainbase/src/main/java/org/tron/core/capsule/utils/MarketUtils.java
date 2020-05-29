/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.capsule.utils;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.MarketAccountOrderCapsule;
import org.tron.core.capsule.MarketOrderCapsule;
import org.tron.core.capsule.MarketPriceCapsule;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.MarketAccountStore;
import org.tron.protos.Protocol.MarketOrder.State;
import org.tron.protos.Protocol.MarketOrderPair;
import org.tron.protos.Protocol.MarketPrice;

public class MarketUtils {

  public static final int TOKEN_ID_LENGTH = ByteArray
      .fromString(Long.toString(Long.MAX_VALUE)).length; // 19

  public static byte[] calculateOrderId(ByteString address, byte[] sellTokenId,
      byte[] buyTokenId, long count) {

    byte[] addressByteArray = address.toByteArray();
    byte[] countByteArray = ByteArray.fromLong(count);

    byte[] result = new byte[addressByteArray.length + TOKEN_ID_LENGTH
        + TOKEN_ID_LENGTH + countByteArray.length];

    System.arraycopy(addressByteArray, 0, result, 0, addressByteArray.length);
    System.arraycopy(sellTokenId, 0, result, addressByteArray.length, sellTokenId.length);
    System.arraycopy(buyTokenId, 0, result, addressByteArray.length + TOKEN_ID_LENGTH,
        buyTokenId.length);
    System.arraycopy(countByteArray, 0, result, addressByteArray.length
        + TOKEN_ID_LENGTH + TOKEN_ID_LENGTH, countByteArray.length);

    return Hash.sha3(result);
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

  public static byte[] createPairPriceKeyNoGCD(byte[] sellTokenId, byte[] buyTokenId,
      long sellTokenQuantity, long buyTokenQuantity) {

    byte[] sellTokenQuantityBytes = ByteArray.fromLong(sellTokenQuantity);
    byte[] buyTokenQuantityBytes = ByteArray.fromLong(buyTokenQuantity);

    return doCreatePairPriceKey(sellTokenId, buyTokenId,
        sellTokenQuantityBytes, buyTokenQuantityBytes);
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

  /**
   * The first price key of one token
   * Because using the price compare, we can set the smallest price as the first one.
   * */
  public static byte[] getPairPriceHeadKey(byte[] sellTokenId, byte[] buyTokenId) {
    return createPairPriceKey(sellTokenId, buyTokenId, 0L, 0L);
  }

  public static byte[] expandTokenIdToPriceArray(byte[] tokenId) {
    byte[] result = new byte[TOKEN_ID_LENGTH];
    System.arraycopy(tokenId, 0, result, 0, tokenId.length);
    return result;
  }

  /**
   * 0...18: sellTokenId
   * 19...37: buyTokenId
   * 38...45: sellTokenQuantity
   * 46...53: buyTokenQuantity
   *
   * return sellTokenQuantity, buyTokenQuantity
   * */
  public static MarketPrice decodeKeyToMarketPrice(byte[] key) {
    byte[] sellTokenQuantity = new byte[8];
    byte[] buyTokenQuantity = new byte[8];

    System.arraycopy(key, 38, sellTokenQuantity, 0, 8);
    System.arraycopy(key, 46, buyTokenQuantity, 0, 8);

    return new MarketPriceCapsule(ByteArray.toLong(sellTokenQuantity),
        ByteArray.toLong(buyTokenQuantity)).getInstance();
  }

  /**
   * input key can be pairKey or pairPriceKey
   * */
  public static MarketOrderPair decodeKeyToMarketPair(byte[] key) {
    byte[] sellTokenId = new byte[TOKEN_ID_LENGTH];
    byte[] buyTokenId = new byte[TOKEN_ID_LENGTH];

    System.arraycopy(key, 0, sellTokenId, 0, TOKEN_ID_LENGTH);
    System.arraycopy(key, TOKEN_ID_LENGTH, buyTokenId, 0, TOKEN_ID_LENGTH);

    MarketOrderPair.Builder builder = MarketOrderPair.newBuilder();
    builder.setSellTokenId(ByteString.copyFrom(sellTokenId))
        .setBuyTokenId(ByteString.copyFrom(buyTokenId));

    return builder.build();
  }

  public static byte[] trim(byte[] bytes) {
    int i = bytes.length - 1;
    while (i >= 0 && bytes[i] == 0)
    {
      --i;
    }

    return Arrays.copyOf(bytes, i + 1);
  }

  /**
   * It's almost the same as decodeKeyToMarketPair, except remove useless 0
   * */
  public static MarketOrderPair decodeKeyToMarketPairHuman(byte[] key) {
    byte[] sellTokenId = new byte[TOKEN_ID_LENGTH];
    byte[] buyTokenId = new byte[TOKEN_ID_LENGTH];

    System.arraycopy(key, 0, sellTokenId, 0, TOKEN_ID_LENGTH);
    System.arraycopy(key, TOKEN_ID_LENGTH, buyTokenId, 0, TOKEN_ID_LENGTH);

    sellTokenId = trim(sellTokenId);
    buyTokenId = trim(buyTokenId);

    MarketOrderPair.Builder builder = MarketOrderPair.newBuilder();
    builder.setSellTokenId(ByteString.copyFrom(sellTokenId))
        .setBuyTokenId(ByteString.copyFrom(buyTokenId));

    return builder.build();
  }

  public static boolean pairKeyIsEqual(byte[] key1,byte[] key2) {
    byte[] bytes1 = decodeKeyToMarketPairKey(key1);
    byte[] bytes2 = decodeKeyToMarketPairKey(key2);
    return ByteUtil.equals(bytes1, bytes2);
  }

  public static byte[] decodeKeyToMarketPairKey(byte[] key) {
    byte[] pairKey = new byte[TOKEN_ID_LENGTH * 2];
    System.arraycopy(key, 0, pairKey, 0, TOKEN_ID_LENGTH * 2);
    return pairKey;
  }

  public static byte[] createPairKey(byte[] sellTokenId, byte[] buyTokenId) {
    byte[] result = new byte[TOKEN_ID_LENGTH * 2];
    System.arraycopy(sellTokenId, 0, result, 0, sellTokenId.length);
    System.arraycopy(buyTokenId, 0, result, TOKEN_ID_LENGTH, buyTokenId.length);
    return result;
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

  /**
   * ex.
   * for sellToken is A, buyToken is TRX.
   * price_A_maker * sellQuantity_maker = Price_TRX * buyQuantity_maker
   * ==> price_A_maker = Price_TRX * buyQuantity_maker/sellQuantity_maker
   *
   * price_A_maker_1 < price_A_maker_2
   * ==> buyQuantity_maker_1/sellQuantity_maker_1 < buyQuantity_maker_2/sellQuantity_maker_2
   * ==> buyQuantity_maker_1*sellQuantity_maker_2 < buyQuantity_maker_2 * sellQuantity_maker_1
   */
  public static int comparePrice(MarketPrice price1, MarketPrice price2) {
    return comparePrice(price1.getSellTokenQuantity(), price1.getBuyTokenQuantity(),
        price2.getSellTokenQuantity(), price2.getBuyTokenQuantity());
  }

  public static boolean isLowerPrice(MarketPrice price1, MarketPrice price2) {
    return comparePrice(price1, price2) == -1;
  }

  /**
   * if takerPrice >= makerPrice, return True
   * note: here are two different token pairs
   * firstly, we should change the token pair of taker to be the same with maker
   */
  public static boolean priceMatch(MarketPrice takerPrice, MarketPrice makerPrice) {
    // for takerPrice, buyToken is A,sellToken is TRX.
    // price_A_taker * buyQuantity_taker = Price_TRX * sellQuantity_taker
    // ==> price_A_taker = Price_TRX * sellQuantity_taker/buyQuantity_taker

    // price_A_taker must be greater or equal to price_A_maker
    // price_A_taker / price_A_maker >= 1
    // ==> Price_TRX * sellQuantity_taker/buyQuantity_taker >= Price_TRX * buyQuantity_maker/sellQuantity_maker
    // ==> sellQuantity_taker * sellQuantity_maker > buyQuantity_taker * buyQuantity_maker

    return comparePrice(takerPrice.getBuyTokenQuantity(), takerPrice.getSellTokenQuantity(),
        makerPrice.getSellTokenQuantity(), makerPrice.getBuyTokenQuantity()) >= 0;
  }

  public static void updateOrderState(MarketOrderCapsule orderCapsule,
      State state, MarketAccountStore marketAccountStore) throws ItemNotFoundException {
    orderCapsule.setState(state);

    // remove from account order list
    if (state == State.INACTIVE || state == State.CANCELED) {
      MarketAccountOrderCapsule accountOrderCapsule = marketAccountStore
          .get(orderCapsule.getOwnerAddress().toByteArray());
      accountOrderCapsule.removeOrder(orderCapsule.getID());
      marketAccountStore.put(accountOrderCapsule.createDbKey(), accountOrderCapsule);
    }
  }

  public static long multiplyAndDivide(long a, long b, long c) {
    try {
      long tmp = Math.multiplyExact(a, b);
      return Math.floorDiv(tmp, c);
    } catch (ArithmeticException ex) {
      // do nothing here, because we will use BigInteger to compute again
    }

    BigInteger aBig = BigInteger.valueOf(a);
    BigInteger bBig = BigInteger.valueOf(b);
    BigInteger cBig = BigInteger.valueOf(c);

    return aBig.multiply(bBig).divide(cBig).longValue();
  }

  // for taker
  public static void returnSellTokenRemain(MarketOrderCapsule orderCapsule,
      AccountCapsule accountCapsule,
      DynamicPropertiesStore dynamicStore,
      AssetIssueStore assetIssueStore) {
    byte[] sellTokenId = orderCapsule.getSellTokenId();
    long sellTokenQuantityRemain = orderCapsule.getSellTokenQuantityRemain();
    if (Arrays.equals(sellTokenId, "_".getBytes())) {
      accountCapsule.setBalance(Math.addExact(
          accountCapsule.getBalance(), sellTokenQuantityRemain));
    } else {
      accountCapsule
          .addAssetAmountV2(sellTokenId, sellTokenQuantityRemain, dynamicStore, assetIssueStore);
    }
    orderCapsule.setSellTokenQuantityRemain(0L);
  }

  public static int comparePriceKey(byte[] o1, byte[] o2) {
    //compare pair
    byte[] pair1 = new byte[TOKEN_ID_LENGTH * 2];
    byte[] pair2 = new byte[TOKEN_ID_LENGTH * 2];

    System.arraycopy(o1, 0, pair1, 0, TOKEN_ID_LENGTH * 2);
    System.arraycopy(o2, 0, pair2, 0, TOKEN_ID_LENGTH * 2);

    int pairResult = org.spongycastle.util.Arrays.compareUnsigned(pair1, pair2);
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

  public static boolean greaterOrEquals(byte[] bytes1, byte[] bytes2) {
    return comparePriceKey(bytes1, bytes2) >= 0;
  }

  public static boolean checkTokenValid(byte[] tokenId) {
    if (!Arrays.equals("_".getBytes(), tokenId) && !TransactionUtil.isNumber(tokenId)) {
      return false;
    }

    return true;
  }

  public static void checkPairValid(byte[] sellTokenId, byte[] buyTokenId)
      throws BadItemException {
    if (!checkTokenValid(sellTokenId)) {
      throw new BadItemException("sellTokenId is not a valid number");
    }

    if (!checkTokenValid(buyTokenId)) {
      throw new BadItemException("buyTokenId is not a valid number");
    }
  }
}