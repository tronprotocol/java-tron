/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.utils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.spongycastle.util.encoders.Hex;

public class ByteUtil {

  public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
  public static final byte[] ZERO_BYTE_ARRAY = new byte[]{0};

  /**
   * The regular {@link java.math.BigInteger#toByteArray()} method isn't quite what we often need:
   * it appends a leading zero to indicate that the number is positive and may need padding.
   *
   * @param b the integer to format into a byte array
   * @param numBytes the desired size of the resulting byte array
   * @return numBytes byte long array.
   */
  public static byte[] bigIntegerToBytes(BigInteger b, int numBytes) {
    if (b == null) {
      return null;
    }
    byte[] bytes = new byte[numBytes];
    byte[] biBytes = b.toByteArray();
    int start = (biBytes.length == numBytes + 1) ? 1 : 0;
    int length = Math.min(biBytes.length, numBytes);
    System.arraycopy(biBytes, start, bytes, numBytes - length, length);
    return bytes;
  }

  /**
   * Omitting sign indication byte. <br><br> Instead of {@link org.spongycastle.util.BigIntegers#asUnsignedByteArray(BigInteger)}
   * <br>we use this custom method to avoid an empty array in case of BigInteger.ZERO
   *
   * @param value - any big integer number. A <code>null</code>-value will return <code>null</code>
   * @return A byte array without a leading zero byte if present in the signed encoding.
   * BigInteger.ZERO will return an array with length 1 and byte-value 0.
   */
  public static byte[] bigIntegerToBytes(BigInteger value) {
    if (value == null) {
      return null;
    }

    byte[] data = value.toByteArray();

    if (data.length != 1 && data[0] == 0) {
      byte[] tmp = new byte[data.length - 1];
      System.arraycopy(data, 1, tmp, 0, tmp.length);
      data = tmp;
    }
    return data;
  }

  /**
   * @param arrays - arrays to merge
   * @return - merged array
   */
  public static byte[] merge(byte[]... arrays) {
    int arrCount = 0;
    int count = 0;
    for (byte[] array : arrays) {
      arrCount++;
      count += array.length;
    }

    // Create new array and copy all array contents
    byte[] mergedArray = new byte[count];
    int start = 0;
    for (byte[] array : arrays) {
      System.arraycopy(array, 0, mergedArray, start, array.length);
      start += array.length;
    }
    return mergedArray;
  }

  /**
   * Creates a copy of bytes and appends b to the end of it
   */
  public static byte[] appendByte(byte[] bytes, byte b) {
    byte[] result = Arrays.copyOf(bytes, bytes.length + 1);
    result[result.length - 1] = b;
    return result;
  }

  /**
   * Turn nibbles to a pretty looking output string <p> Example. [ 1, 2, 3, 4, 5 ] becomes
   * '\x11\x23\x45'
   *
   * @param nibbles - getting byte of data [ 04 ] and turning it to a '\x04' representation
   * @return pretty string of nibbles
   */
  public static String nibblesToPrettyString(byte[] nibbles) {
    StringBuilder builder = new StringBuilder();
    for (byte nibble : nibbles) {
      final String nibbleString = oneByteToHexString(nibble);
      builder.append("\\x").append(nibbleString);
    }
    return builder.toString();
  }

  public static String oneByteToHexString(byte value) {
    String retVal = Integer.toString(value & 0xFF, 16);
    if (retVal.length() == 1) {
      retVal = "0" + retVal;
    }
    return retVal;
  }

  /**
   * Convert a byte-array into a hex String.<br> Works similar to {@link Hex#toHexString} but allows
   * for <code>null</code>
   *
   * @param data - byte-array to convert to a hex-string
   * @return hex representation of the data.<br> Returns an empty String if the input is
   * <code>null</code>
   * @see Hex#toHexString
   */
  public static String toHexString(byte[] data) {
    return data == null ? "" : Hex.toHexString(data);
  }

  /**
   * Cast hex encoded value from byte[] to int <p> Limited to Integer.MAX_VALUE: 2^32-1 (4 bytes)
   *
   * @param b array contains the values
   * @return unsigned positive int value.
   */
  public static int byteArrayToInt(byte[] b) {
    if (b == null || b.length == 0) {
      return 0;
    }
    return new BigInteger(1, b).intValue();
  }

  public static boolean isNullOrZeroArray(byte[] array) {
    return (array == null) || (array.length == 0);
  }

  public static boolean isSingleZero(byte[] array) {
    return (array.length == 1 && array[0] == 0);
  }

  /**
   * Converts a int value into a byte array.
   *
   * @param val - int value to convert
   * @return value with leading byte that are zeroes striped
   */
  public static byte[] intToBytesNoLeadZeroes(int val) {

    if (val == 0) {
      return EMPTY_BYTE_ARRAY;
    }

    int lenght = 0;

    int tmpVal = val;
    while (tmpVal != 0) {
      tmpVal = tmpVal >>> 8;
      ++lenght;
    }

    byte[] result = new byte[lenght];

    int index = result.length - 1;
    while (val != 0) {

      result[index] = (byte) (val & 0xFF);
      val = val >>> 8;
      index -= 1;
    }

    return result;
  }

  /**
   * Converts int value into a byte array.
   *
   * @param val - int value to convert
   * @return <code>byte[]</code> of length 4, representing the int value
   */
  public static byte[] intToBytes(int val) {
    return ByteBuffer.allocate(4).putInt(val).array();
  }

}