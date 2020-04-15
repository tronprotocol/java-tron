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
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.MarketAccountOrderCapsule;
import org.tron.core.capsule.MarketOrderCapsule;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.MarketAccountStore;
import org.tron.protos.Protocol.MarketOrder.State;
import org.tron.protos.Protocol.MarketPrice;

public class MarketUtils {

  private static final int TOKEN_ID_LENGTH = ByteArray
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


  public static byte[] createPairPriceKey(byte[] sellTokenId, byte[] buyTokenId,
      long sellTokenQuantity, long buyTokenQuantity) {
    byte[] sellTokenQuantityBytes = ByteArray.fromLong(sellTokenQuantity);
    byte[] buyTokenQuantityBytes = ByteArray.fromLong(buyTokenQuantity);
    byte[] result = new byte[TOKEN_ID_LENGTH + TOKEN_ID_LENGTH
        + sellTokenQuantityBytes.length + buyTokenQuantityBytes.length];

    System.arraycopy(sellTokenId, 0, result, 0, sellTokenId.length);
    System.arraycopy(buyTokenId, 0, result, TOKEN_ID_LENGTH, buyTokenId.length);
    System.arraycopy(sellTokenQuantityBytes, 0, result,
        TOKEN_ID_LENGTH + TOKEN_ID_LENGTH,
        sellTokenQuantityBytes.length);
    System.arraycopy(buyTokenQuantityBytes, 0, result,
        TOKEN_ID_LENGTH + TOKEN_ID_LENGTH + sellTokenQuantityBytes.length,
        buyTokenQuantityBytes.length);

    return Hash.sha3(result);
  }

  public static byte[] createPairKey(byte[] sellTokenId, byte[] buyTokenId) {
    byte[] result = new byte[TOKEN_ID_LENGTH * 2];
    System.arraycopy(sellTokenId, 0, result, 0, sellTokenId.length);
    System.arraycopy(buyTokenId, 0, result, TOKEN_ID_LENGTH, buyTokenId.length);
    return result;
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
    try {
      long price1BuyQuantity = price1.getBuyTokenQuantity();
      long price1SellQuantity = price1.getSellTokenQuantity();
      long price2BuyQuantity = price2.getBuyTokenQuantity();
      long price2SellQuantity = price2.getSellTokenQuantity();

      return Long.compare(Math.multiplyExact(price1BuyQuantity, price2SellQuantity),
          Math.multiplyExact(price2BuyQuantity, price1SellQuantity));

    } catch (ArithmeticException ex) {
      // do nothing here, because we will use BigInteger to compute again
    }

    BigInteger price1BuyQuantity = BigInteger.valueOf(price1.getBuyTokenQuantity());
    BigInteger price1SellQuantity = BigInteger.valueOf(price1.getSellTokenQuantity());
    BigInteger price2BuyQuantity = BigInteger.valueOf(price2.getBuyTokenQuantity());
    BigInteger price2SellQuantity = BigInteger.valueOf(price2.getSellTokenQuantity());

    return price1BuyQuantity.multiply(price2SellQuantity).compareTo(price2BuyQuantity
        .multiply(price1SellQuantity));
  }

  public static boolean isLowerPrice(MarketPrice price1, MarketPrice price2) {
    return comparePrice(price1, price2) == -1;
  }


  /**
   * if takerPrice >= makerPrice, return True
   * note: here are two different token pairs
   */
  public static boolean priceMatch(MarketPrice takerPrice, MarketPrice makerPrice) {
    // for takerPrice, buyToken is A,sellToken is TRX.
    // price_A_taker * buyQuantity_taker = Price_TRX * sellQuantity_taker
    // ==> price_A_taker = Price_TRX * sellQuantity_taker/buyQuantity_taker

    // price_A_taker must be greater or equal to price_A_maker
    // price_A_taker / price_A_maker >= 1
    // ==> Price_TRX * sellQuantity_taker/buyQuantity_taker >= Price_TRX * buyQuantity_maker/sellQuantity_maker
    // ==> sellQuantity_taker * sellQuantity_maker > buyQuantity_taker * buyQuantity_maker

    try {
      return
          Math.multiplyExact(takerPrice.getSellTokenQuantity(), makerPrice.getSellTokenQuantity())
              >= Math
              .multiplyExact(takerPrice.getBuyTokenQuantity(), makerPrice.getBuyTokenQuantity());
    } catch (ArithmeticException ex) {
      // do nothing here, because we will use BigInteger to compute again
    }

    BigInteger takerBuyQuantity = BigInteger.valueOf(takerPrice.getBuyTokenQuantity());
    BigInteger takerSellQuantity = BigInteger.valueOf(takerPrice.getSellTokenQuantity());
    BigInteger makerPriceBuyQuantity = BigInteger.valueOf(makerPrice.getBuyTokenQuantity());
    BigInteger makerPriceSellQuantity = BigInteger.valueOf(makerPrice.getSellTokenQuantity());

    return takerSellQuantity.multiply(makerPriceSellQuantity)
        .compareTo(takerBuyQuantity.multiply(makerPriceBuyQuantity)) >= 0;
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

    }

    BigInteger aBig = BigInteger.valueOf(a);
    BigInteger bBig = BigInteger.valueOf(b);
    BigInteger cBig = BigInteger.valueOf(c);

    return aBig.multiply(bBig).divide(cBig).longValue();

  }

  // for taker
  public static void returnSellTokenRemain(MarketOrderCapsule orderCapsule, AccountCapsule accountCapsule,
      DynamicPropertiesStore dynamicStore, AssetIssueStore assetIssueStore) {
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

}