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

package org.tron.common.zksnark;

import com.google.protobuf.ByteString;
import org.tron.common.utils.ByteArray;

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

//    return Hash.sha3(result);
    return result;
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
    return result;
  }

  public static byte[] createPairKey(byte[] sellTokenId, byte[] buyTokenId) {
    byte[] result = new byte[TOKEN_ID_LENGTH * 2];
    System.arraycopy(sellTokenId, 0, result, 0, sellTokenId.length);
    System.arraycopy(buyTokenId, 0, result, TOKEN_ID_LENGTH, buyTokenId.length);
    return result;
  }

}