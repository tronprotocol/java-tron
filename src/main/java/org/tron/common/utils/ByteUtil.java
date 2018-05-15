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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.spongycastle.util.encoders.Hex;
import org.tron.core.db.ByteArrayWrapper;

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
   * Omitting sign indication byte. <br><br> Instead of
   * {@link org.spongycastle.util.BigIntegers#asUnsignedByteArray(BigInteger)}
   * <br>we use this custom method to avoid an empty array in case of BigInteger.ZERO
   *
   * @param value - any big integer number. A <code>null</code>-value will return <code>null</code>
   * @return A byte array without a leading zero byte if present in the signed encoding.
   *     BigInteger.ZERO will return an array with length 1 and byte-value 0.
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

  public static byte[] bigIntegerToBytesSigned(BigInteger b, int numBytes) {
    if (b == null)
      return null;
    byte[] bytes = new byte[numBytes];
    Arrays.fill(bytes, b.signum() < 0 ? (byte) 0xFF : 0x00);
    byte[] biBytes = b.toByteArray();
    int start = (biBytes.length == numBytes + 1) ? 1 : 0;
    int length = Math.min(biBytes.length, numBytes);
    System.arraycopy(biBytes, start, bytes, numBytes - length, length);
    return bytes;
  }

  /**
   * Cast hex encoded value from byte[] to BigInteger
   * null is parsed like byte[0]
   *
   * @param bb byte array contains the values
   * @return unsigned positive BigInteger value.
   */
  public static BigInteger bytesToBigInteger(byte[] bb) {
    return (bb == null || bb.length == 0) ? BigInteger.ZERO : new BigInteger(1, bb);
  }

  /**
   * Returns the amount of nibbles that match each other from 0 ...
   * amount will never be larger than smallest input
   *
   * @param a - first input
   * @param b - second input
   * @return Number of bytes that match
   */
  public static int matchingNibbleLength(byte[] a, byte[] b) {
    int i = 0;
    int length = a.length < b.length ? a.length : b.length;
    while (i < length) {
      if (a[i] != b[i])
        return i;
      i++;
    }
    return i;
  }

  /**
   * Converts a long value into a byte array.
   *
   * @param val - long value to convert
   * @return <code>byte[]</code> of length 8, representing the long value
   */
  public static byte[] longToBytes(long val) {
    return ByteBuffer.allocate(Long.BYTES).putLong(val).array();
  }

  /**
   * Converts a long value into a byte array.
   *
   * @param val - long value to convert
   * @return decimal value with leading byte that are zeroes striped
   */
  public static byte[] longToBytesNoLeadZeroes(long val) {

    // todo: improve performance by while strip numbers until (long >> 8 == 0)
    if (val == 0) return EMPTY_BYTE_ARRAY;

    byte[] data = ByteBuffer.allocate(Long.BYTES).putLong(val).array();

    return stripLeadingZeroes(data);
  }

  /**
   * Calculate packet length
   *
   * @param msg byte[]
   * @return byte-array with 4 elements
   */
  public static byte[] calcPacketLength(byte[] msg) {
    int msgLen = msg.length;
    return new byte[]{
            (byte) ((msgLen >> 24) & 0xFF),
            (byte) ((msgLen >> 16) & 0xFF),
            (byte) ((msgLen >> 8) & 0xFF),
            (byte) ((msgLen) & 0xFF)};
  }

  /**
   * Cast hex encoded value from byte[] to long
   * null is parsed like byte[0]
   *
   * Limited to Long.MAX_VALUE: 2<sup>63</sup>-1 (8 bytes)
   *
   * @param b array contains the values
   * @return unsigned positive long value.
   */
  public static long byteArrayToLong(byte[] b) {
    if (b == null || b.length == 0)
      return 0;
    return new BigInteger(1, b).longValue();
  }

  /**
   * Calculate the number of bytes need
   * to encode the number
   *
   * @param val - number
   * @return number of min bytes used to encode the number
   */
  public static int numBytes(String val) {

    BigInteger bInt = new BigInteger(val);
    int bytes = 0;

    while (!bInt.equals(BigInteger.ZERO)) {
      bInt = bInt.shiftRight(8);
      ++bytes;
    }
    if (bytes == 0) ++bytes;
    return bytes;
  }

  /**
   * @param arg - not more that 32 bits
   * @return - bytes of the value pad with complete to 32 zeroes
   */
  public static byte[] encodeValFor32Bits(Object arg) {

    byte[] data;

    // check if the string is numeric
    if (arg.toString().trim().matches("-?\\d+(\\.\\d+)?"))
      data = new BigInteger(arg.toString().trim()).toByteArray();
      // check if it's hex number
    else if (arg.toString().trim().matches("0[xX][0-9a-fA-F]+"))
      data = new BigInteger(arg.toString().trim().substring(2), 16).toByteArray();
    else
      data = arg.toString().trim().getBytes();


    if (data.length > 32)
      throw new RuntimeException("values can't be more than 32 byte");

    byte[] val = new byte[32];

    int j = 0;
    for (int i = data.length; i > 0; --i) {
      val[31 - j] = data[i - 1];
      ++j;
    }
    return val;
  }

  /**
   * encode the values and concatenate together
   *
   * @param args Object
   * @return byte[]
   */
  public static byte[] encodeDataList(Object... args) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    for (Object arg : args) {
      byte[] val = encodeValFor32Bits(arg);
      try {
        baos.write(val);
      } catch (IOException e) {
        throw new Error("Happen something that should never happen ", e);
      }
    }
    return baos.toByteArray();
  }

  public static int firstNonZeroByte(byte[] data) {
    for (int i = 0; i < data.length; ++i) {
      if (data[i] != 0) {
        return i;
      }
    }
    return -1;
  }

  public static byte[] stripLeadingZeroes(byte[] data) {

    if (data == null)
      return null;

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

  /**
   * increment byte array as a number until max is reached
   *
   * @param bytes byte[]
   * @return boolean
   */
  public static boolean increment(byte[] bytes) {
    final int startIndex = 0;
    int i;
    for (i = bytes.length - 1; i >= startIndex; i--) {
      bytes[i]++;
      if (bytes[i] != 0)
        break;
    }
    // we return false when all bytes are 0 again
    return (i >= startIndex || bytes[startIndex] != 0);
  }

  /**
   * Utility function to copy a byte array into a new byte array with given size.
   * If the src length is smaller than the given size, the result will be left-padded
   * with zeros.
   *
   * @param value - a BigInteger with a maximum value of 2^256-1
   * @return Byte array of given size with a copy of the <code>src</code>
   */
  public static byte[] copyToArray(BigInteger value) {
    byte[] src = ByteUtil.bigIntegerToBytes(value);
    byte[] dest = ByteBuffer.allocate(32).array();
    System.arraycopy(src, 0, dest, dest.length - src.length, src.length);
    return dest;
  }


  public static ByteArrayWrapper wrap(byte[] data) {
    return new ByteArrayWrapper(data);
  }

  public static byte[] setBit(byte[] data, int pos, int val) {

    if ((data.length * 8) - 1 < pos)
      throw new Error("outside byte array limit, pos: " + pos);

    int posByte = data.length - 1 - (pos) / 8;
    int posBit = (pos) % 8;
    byte setter = (byte) (1 << (posBit));
    byte toBeSet = data[posByte];
    byte result;
    if (val == 1)
      result = (byte) (toBeSet | setter);
    else
      result = (byte) (toBeSet & ~setter);

    data[posByte] = result;
    return data;
  }

  public static int getBit(byte[] data, int pos) {

    if ((data.length * 8) - 1 < pos)
      throw new Error("outside byte array limit, pos: " + pos);

    int posByte = data.length - 1 - pos / 8;
    int posBit = pos % 8;
    byte dataByte = data[posByte];
    return Math.min(1, (dataByte & (1 << (posBit))));
  }

  public static byte[] and(byte[] b1, byte[] b2) {
    if (b1.length != b2.length) throw new RuntimeException("Array sizes differ");
    byte[] ret = new byte[b1.length];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = (byte) (b1[i] & b2[i]);
    }
    return ret;
  }

  public static byte[] or(byte[] b1, byte[] b2) {
    if (b1.length != b2.length) throw new RuntimeException("Array sizes differ");
    byte[] ret = new byte[b1.length];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = (byte) (b1[i] | b2[i]);
    }
    return ret;
  }

  public static byte[] xor(byte[] b1, byte[] b2) {
    if (b1.length != b2.length) throw new RuntimeException("Array sizes differ");
    byte[] ret = new byte[b1.length];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = (byte) (b1[i] ^ b2[i]);
    }
    return ret;
  }

  /**
   * XORs byte arrays of different lengths by aligning length of the shortest via adding zeros at beginning
   */
  public static byte[] xorAlignRight(byte[] b1, byte[] b2) {
    if (b1.length > b2.length) {
      byte[] b2_ = new byte[b1.length];
      System.arraycopy(b2, 0, b2_, b1.length - b2.length, b2.length);
      b2 = b2_;
    } else if (b2.length > b1.length) {
      byte[] b1_ = new byte[b2.length];
      System.arraycopy(b1, 0, b1_, b2.length - b1.length, b1.length);
      b1 = b1_;
    }

    return xor(b1, b2);
  }

  public static Set<byte[]> difference(Set<byte[]> setA, Set<byte[]> setB){

    Set<byte[]> result = new HashSet<>();

    for (byte[] elementA : setA){
      boolean found = false;
      for (byte[] elementB : setB){

        if (Arrays.equals(elementA, elementB)){
          found = true;
          break;
        }
      }
      if (!found) result.add(elementA);
    }

    return result;
  }

  public static int length(byte[]... bytes) {
    int result = 0;
    for (byte[] array : bytes) {
      result += (array == null) ? 0 : array.length;
    }
    return result;
  }

  public static byte[] intsToBytes(int[] arr, boolean bigEndian) {
    byte[] ret = new byte[arr.length * 4];
    intsToBytes(arr, ret, bigEndian);
    return ret;
  }

  public static int[] bytesToInts(byte[] arr, boolean bigEndian) {
    int[] ret = new int[arr.length / 4];
    bytesToInts(arr, ret, bigEndian);
    return ret;
  }

  public static void bytesToInts(byte[] b, int[] arr, boolean bigEndian) {
    if (!bigEndian) {
      int off = 0;
      for (int i = 0; i < arr.length; i++) {
        int ii = b[off++] & 0x000000FF;
        ii |= (b[off++] << 8) & 0x0000FF00;
        ii |= (b[off++] << 16) & 0x00FF0000;
        ii |= (b[off++] << 24);
        arr[i] = ii;
      }
    } else {
      int off = 0;
      for (int i = 0; i < arr.length; i++) {
        int ii = b[off++] << 24;
        ii |= (b[off++] << 16) & 0x00FF0000;
        ii |= (b[off++] << 8) & 0x0000FF00;
        ii |= b[off++] & 0x000000FF;
        arr[i] = ii;
      }
    }
  }

  public static void intsToBytes(int[] arr, byte[] b, boolean bigEndian) {
    if (!bigEndian) {
      int off = 0;
      for (int i = 0; i < arr.length; i++) {
        int ii = arr[i];
        b[off++] = (byte) (ii & 0xFF);
        b[off++] = (byte) ((ii >> 8) & 0xFF);
        b[off++] = (byte) ((ii >> 16) & 0xFF);
        b[off++] = (byte) ((ii >> 24) & 0xFF);
      }
    } else {
      int off = 0;
      for (int i = 0; i < arr.length; i++) {
        int ii = arr[i];
        b[off++] = (byte) ((ii >> 24) & 0xFF);
        b[off++] = (byte) ((ii >> 16) & 0xFF);
        b[off++] = (byte) ((ii >> 8) & 0xFF);
        b[off++] = (byte) (ii & 0xFF);
      }
    }
  }

  public static short bigEndianToShort(byte[] bs) {
    return bigEndianToShort(bs, 0);
  }

  public static short bigEndianToShort(byte[] bs, int off) {
    int n = bs[off] << 8;
    ++off;
    n |= bs[off] & 0xFF;
    return (short) n;
  }

  public static byte[] shortToBytes(short n) {
    return ByteBuffer.allocate(2).putShort(n).array();
  }

  /**
   * Converts string hex representation to data bytes
   * Accepts following hex:
   *  - with or without 0x prefix
   *  - with no leading 0, like 0xabc -> 0x0abc
   * @param data  String like '0xa5e..' or just 'a5e..'
   * @return  decoded bytes array
   */
  public static byte[] hexStringToBytes(String data) {
    if (data == null) return EMPTY_BYTE_ARRAY;
    if (data.startsWith("0x")) data = data.substring(2);
    if (data.length() % 2 == 1) data = "0" + data;
    return Hex.decode(data);
  }

  /**
   * Converts string representation of host/ip to 4-bytes byte[] IPv4
   */
  public static byte[] hostToBytes(String ip) {
    byte[] bytesIp;
    try {
      bytesIp = InetAddress.getByName(ip).getAddress();
    } catch (UnknownHostException e) {
      bytesIp = new byte[4];  // fall back to invalid 0.0.0.0 address
    }

    return bytesIp;
  }

  /**
   * Converts 4 bytes IPv4 IP to String representation
   */
  public static String bytesToIp(byte[] bytesIp) {

    StringBuilder sb = new StringBuilder();
    sb.append(bytesIp[0] & 0xFF);
    sb.append(".");
    sb.append(bytesIp[1] & 0xFF);
    sb.append(".");
    sb.append(bytesIp[2] & 0xFF);
    sb.append(".");
    sb.append(bytesIp[3] & 0xFF);

    String ip = sb.toString();
    return ip;
  }

  /**
   * Returns a number of zero bits preceding the highest-order ("leftmost") one-bit
   * interpreting input array as a big-endian integer value
   */
  public static int numberOfLeadingZeros(byte[] bytes) {

    int i = firstNonZeroByte(bytes);

    if (i == -1) {
      return bytes.length * 8;
    } else {
      int byteLeadingZeros = Integer.numberOfLeadingZeros((int)bytes[i] & 0xff) - 24;
      return i * 8 + byteLeadingZeros;
    }
  }

  /**
   * Parses fixed number of bytes starting from {@code offset} in {@code input} array.
   * If {@code input} has not enough bytes return array will be right padded with zero bytes.
   * I.e. if {@code offset} is higher than {@code input.length} then zero byte array of length {@code len} will be returned
   */
  public static byte[] parseBytes(byte[] input, int offset, int len) {

    if (offset >= input.length || len == 0)
      return EMPTY_BYTE_ARRAY;

    byte[] bytes = new byte[len];
    System.arraycopy(input, offset, bytes, 0, Math.min(input.length - offset, len));
    return bytes;
  }

  /**
   * Parses 32-bytes word from given input.
   * Uses {@link #parseBytes(byte[], int, int)} method,
   * thus, result will be right-padded with zero bytes if there is not enough bytes in {@code input}
   *
   * @param idx an index of the word starting from {@code 0}
   */
  public static byte[] parseWord(byte[] input, int idx) {
    return parseBytes(input, 32 * idx, 32);
  }

  /**
   * Parses 32-bytes word from given input.
   * Uses {@link #parseBytes(byte[], int, int)} method,
   * thus, result will be right-padded with zero bytes if there is not enough bytes in {@code input}
   *
   * @param idx an index of the word starting from {@code 0}
   * @param offset an offset in {@code input} array to start parsing from
   */
  public static byte[] parseWord(byte[] input, int offset, int idx) {
    return parseBytes(input, offset + 32 * idx, 32);
  }

}