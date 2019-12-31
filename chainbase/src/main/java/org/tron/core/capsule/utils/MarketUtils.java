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
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol.MarketPrice;

public class MarketUtils {


  public static byte[] calculateOrderId(ByteString address, byte[] sellTokenId,
      byte[] buyTokenId, long count) {

    byte[] addressByteArray = address.toByteArray();
    byte[] countByteArray = ByteArray.fromLong(count);

    byte[] result = new byte[addressByteArray.length + sellTokenId.length
        + buyTokenId.length + countByteArray.length];

    System.arraycopy(addressByteArray, 0, result, 0, addressByteArray.length);
    System.arraycopy(sellTokenId, 0, result, addressByteArray.length, sellTokenId.length);
    System.arraycopy(buyTokenId, 0, result, addressByteArray.length + sellTokenId.length,
        buyTokenId.length);
    System.arraycopy(countByteArray, 0, result, addressByteArray.length
        + sellTokenId.length + buyTokenId.length, countByteArray.length);

//    return Hash.sha3(result);
    return result;
  }


  public static byte[] createPairPriceKey(byte[] sellTokenId, byte[] buyTokenId,
      long sellTokenQuantity, long buyTokenQuantity) {
    byte[] sellTokenQuantityBytes = ByteArray.fromLong(sellTokenQuantity);
    byte[] buyTokenQuantityBytes = ByteArray.fromLong(buyTokenQuantity);
    byte[] result = new byte[sellTokenId.length + buyTokenId.length
        + sellTokenQuantityBytes.length + buyTokenQuantityBytes.length];

    System.arraycopy(sellTokenId, 0, result, 0, sellTokenId.length);
    System.arraycopy(buyTokenId, 0, result, sellTokenId.length, buyTokenId.length);
    System.arraycopy(sellTokenQuantityBytes, 0, result,
        sellTokenId.length + buyTokenId.length,
        sellTokenQuantityBytes.length);
    System.arraycopy(buyTokenQuantityBytes, 0, result,
        sellTokenId.length + buyTokenId.length + sellTokenQuantityBytes.length,
        buyTokenQuantityBytes.length);
    return result;
  }

  public static byte[] createPairKey(byte[] sellTokenId, byte[] buyTokenId) {
    byte[] result = new byte[sellTokenId.length + buyTokenId.length];
    System.arraycopy(sellTokenId, 0, result, 0, sellTokenId.length);
    System.arraycopy(buyTokenId, 0, result, sellTokenId.length, buyTokenId.length);
    return result;
  }

  public static boolean isLowerPrice(MarketPrice price1, MarketPrice price2) {
    // ex.
    // for sellToken is A,buyToken is TRX.
    // price_A_maker * sellQuantity_maker = Price_TRX * buyQuantity_maker
    // ==> price_A_maker = Price_TRX * buyQuantity_maker/sellQuantity_maker

    // price_A_maker_1 < price_A_maker_2
    // ==> buyQuantity_maker_1/sellQuantity_maker_1 < buyQuantity_maker_2/sellQuantity_maker_2
    // ==> buyQuantity_maker_1*sellQuantity_maker_2 < buyQuantity_maker_2 * sellQuantity_maker_1
    return Math.multiplyExact(price1.getBuyTokenQuantity(), price2.getSellTokenQuantity())
        < Math.multiplyExact(price2.getBuyTokenQuantity(), price1.getSellTokenQuantity());
  }

  public static boolean isSamePrice(MarketPrice price1, MarketPrice price2) {
    return Math.multiplyExact(price1.getBuyTokenQuantity(), price2.getSellTokenQuantity())
        == Math.multiplyExact(price2.getBuyTokenQuantity(), price1.getSellTokenQuantity());
  }


}