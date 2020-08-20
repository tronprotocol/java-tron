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

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ByteUtil {

  public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
  public static final byte[] ZERO_BYTE_ARRAY = new byte[]{0};
  public static final int WORD_SIZE = 32;


  /**
   * return a cloned byte array. return null if parameter data is null
   */
  public static byte[] cloneBytes(byte[] data) {
    if (data == null) {
      return null;
    }

    int length = data.length;
    byte[] rc = new byte[length];
    if (length > 0) {
      System.arraycopy(data, 0, rc, 0, length);
    }
    return rc;
  }

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
   * merge arrays.
   *
   * @param arrays - arrays to merge
   * @return - merged array
   */
  public static byte[] merge(byte[]... arrays) {
    int count = 0;
    for (byte[] array : arrays) {
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
   * Creates a copy of bytes and appends b to the end of it.
   */
  public static byte[] appendByte(byte[] bytes, byte b) {
    byte[] result = Arrays.copyOf(bytes, bytes.length + 1);
    result[result.length - 1] = b;
    return result;
  }

  /**
   * Turn nibbles to a pretty looking output string Example. [ 1, 2, 3, 4, 5 ] becomes
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

  /**
   * get hex string data from byte data.
   */
  public static String oneByteToHexString(byte value) {
    String retVal = Integer.toString(value & 0xFF, 16);
    if (retVal.length() == 1) {
      retVal = "0" + retVal;
    }
    return retVal;
  }

  /**
   * Cast hex encoded value from byte[] to int Limited to Integer.MAX_VALUE: 2^32-1 (4 bytes)
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

  /**
   * Cast hex encoded value from byte[] to BigInteger null is parsed like byte[0]
   *
   * @param bb byte array contains the values
   * @return unsigned positive BigInteger value.
   */
  public static BigInteger bytesToBigInteger(byte[] bb) {
    return (bb == null || bb.length == 0) ? BigInteger.ZERO : new BigInteger(1, bb);
  }

  /**
   * Cast hex encoded value from byte[] to long null is parsed like byte[0]
   *
   * Limited to Long.MAX_VALUE: 2<sup>63</sup>-1 (8 bytes)
   *
   * @param b array contains the values
   * @return unsigned positive long value.
   */
  public static long byteArrayToLong(byte[] b) {
    if (b == null || b.length == 0) {
      return 0;
    }
    return new BigInteger(1, b).longValueExact();
  }

  public static int firstNonZeroByte(byte[] data) {
    for (int i = 0; i < data.length; ++i) {
      if (data[i] != 0) {
        return i;
      }
    }
    return -1;
  }

  public static int lastNonZeroByte(byte[] data) {
    for (int i = data.length - 1; i >= 0; --i) {
      if (data[i] != 0) {
        return i;
      }
    }
    return -1;
  }

  public static byte[] stripLeadingZeroes(byte[] data) {

    if (data == null) {
      return null;
    }

    final int firstNonZero = firstNonZeroByte(data);
    switch (firstNonZero) {
      case -1:
        return ZERO_BYTE_ARRAY;

      case 0:
        return data;

      default:
        byte[] result = new byte[data.length - firstNonZero];
        System.arraycopy(data, firstNonZero, result, 0, data.length - firstNonZero);

        return result;
    }
  }

  public static byte[] stripEndingZeroes(byte[] data) {

    if (data == null) {
      return null;
    }
    if (data[data.length - 1] != 0) {
      return data;
    }
    final int lastNonZeroByte = lastNonZeroByte(data);
    switch (lastNonZeroByte) {
      case -1:
        return ZERO_BYTE_ARRAY;
      default:
        byte[] result = new byte[lastNonZeroByte + 1];
        System.arraycopy(data, 0, result, 0, lastNonZeroByte + 1);

        return result;
    }
  }

  /**
   * Utility function to copy a byte array into a new byte array with given size. If the src length
   * is smaller than the given size, the result will be left-padded with zeros.
   *
   * @param value - a BigInteger with a maximum value of 2^256-1
   * @return Byte array of given size with a copy of the <code>src</code>
   */
  public static byte[] copyToArray(BigInteger value) {
    byte[] dest = ByteBuffer.allocate(WORD_SIZE).array();
    byte[] src = ByteUtil.bigIntegerToBytes(value);
    if (src != null) {
      System.arraycopy(src, 0, dest, dest.length - src.length, src.length);
    }
    return dest;
  }

  /**
   * Returns a number of zero bits preceding the highest-order ("leftmost") one-bit interpreting
   * input array as a big-endian integer value
   */
  public static int numberOfLeadingZeros(byte[] bytes) {

    int i = firstNonZeroByte(bytes);

    if (i == -1) {
      return bytes.length * 8;
    } else {
      int byteLeadingZeros = Integer.numberOfLeadingZeros((int) bytes[i] & 0xff) - 24;
      return i * 8 + byteLeadingZeros;
    }
  }

  /**
   * Parses fixed number of bytes starting from {@code offset} in {@code input} array. If {@code
   * input} has not enough bytes return array will be right padded with zero bytes. I.e. if {@code
   * offset} is higher than {@code input.length} then zero byte array of length {@code len} will be
   * returned
   */
  public static byte[] parseBytes(byte[] input, int offset, int len) {

    if (offset >= input.length || len == 0) {
      return EMPTY_BYTE_ARRAY;
    }

    byte[] bytes = new byte[len];
    System.arraycopy(input, offset, bytes, 0, Math.min(input.length - offset, len));
    return bytes;
  }

  /**
   * Parses 32-bytes word from given input. Uses {@link #parseBytes(byte[], int, int)} method, thus,
   * result will be right-padded with zero bytes if there is not enough bytes in {@code input}
   *
   * @param idx an index of the word starting from {@code 0}
   */
  public static byte[] parseWord(byte[] input, int idx) {
    return parseBytes(input, WORD_SIZE * idx, WORD_SIZE);
  }

  /**
   * Parses 32-bytes word from given input. Uses {@link #parseBytes(byte[], int, int)} method, thus,
   * result will be right-padded with zero bytes if there is not enough bytes in {@code input}
   *
   * @param idx an index of the word starting from {@code 0}
   * @param offset an offset in {@code input} array to start parsing from
   */
  public static byte[] parseWord(byte[] input, int offset, int idx) {
    return parseBytes(input, offset + WORD_SIZE * idx, WORD_SIZE);
  }

  public static boolean greater(byte[] bytes1, byte[] bytes2) {
    return compare(bytes1, bytes2) > 0;
  }

  public static boolean greaterOrEquals(byte[] bytes1, byte[] bytes2) {
    return compare(bytes1, bytes2) >= 0;
  }

  public static boolean less(byte[] bytes1, byte[] bytes2) {
    return compare(bytes1, bytes2) < 0;
  }

  public static boolean lessOrEquals(byte[] bytes1, byte[] bytes2) {
    return compare(bytes1, bytes2) <= 0;
  }

  public static boolean equals(byte[] bytes1, byte[] bytes2) {
    return compare(bytes1, bytes2) == 0;
  }

  public static boolean isNullOrZeroArray(byte[] array) {
    return (array == null) || (array.length == 0);
  }

  // lexicographical order
  public static int compare(byte[] bytes1, byte[] bytes2) {
    Preconditions.checkNotNull(bytes1);
    Preconditions.checkNotNull(bytes2);
    Preconditions.checkArgument(bytes1.length == bytes2.length);
    int length = bytes1.length;
    for (int i = 0; i < length; ++i) {
      int ret = UnsignedBytes.compare(bytes1[i], bytes2[i]);
      if (ret != 0) {
        return ret;
      }
    }

    return 0;
  }

  public static byte[] hexToBytes(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
          + Character.digit(s.charAt(i + 1), 16));
    }
    return data;
  }


  public static List<Boolean> convertBytesVectorToVector(final byte[] bytes) {
    List<Boolean> ret = new ArrayList<>();

    byte c;
    for (int i = 0; i < bytes.length; i++) {
      c = bytes[i];
      for (int j = 0; j < 8; j++) {
        ret.add(((c >> (7 - j)) & 1) == 1);
      }
    }

    return ret;
  }

  public static void reverse(byte[] bytes) {
    int len = bytes.length / 2;
    for (int i = 0; i < len; i++) {
      byte b = bytes[i];
      bytes[i] = bytes[bytes.length - i - 1];
      bytes[bytes.length - i - 1] = b;
    }
  }

  public static byte[] longTo32Bytes(long value) {
    byte[] longBytes = ByteArray.fromLong(value);
    byte[] zeroBytes = new byte[24];
    return ByteUtil.merge(zeroBytes, longBytes);
  }

}
