package org.tron.p2p.utils;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;


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
@Slf4j(topic = "net")
public class ByteArray {

  public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
  public static final byte[] ZERO_BYTE_ARRAY = new byte[] {0};
  public static final int WORD_SIZE = 32;

  public static String toHexString(byte[] data) {
    return data == null ? "" : Hex.toHexString(data);
  }

  /**
   * get bytes data from hex string data.
   */
  public static byte[] fromHexString(String data) {
    if (data == null) {
      return EMPTY_BYTE_ARRAY;
    }
    if (data.startsWith("0x")) {
      data = data.substring(2);
    }
    if (data.length() % 2 != 0) {
      data = "0" + data;
    }
    return Hex.decode(data);
  }

  /**
   * get long data from bytes data.
   */
  public static long toLong(byte[] b) {
    return ArrayUtils.isEmpty(b) ? 0 : new BigInteger(1, b).longValue();
  }

  /**
   * get int data from bytes data.
   */
  public static int toInt(byte[] b) {
    return ArrayUtils.isEmpty(b) ? 0 : new BigInteger(1, b).intValue();
  }

  /**
   * get bytes data from string data.
   */
  public static byte[] fromString(String s) {
    return StringUtils.isBlank(s) ? null : s.getBytes();
  }

  /**
   * get string data from bytes data.
   */
  public static String toStr(byte[] b) {
    return ArrayUtils.isEmpty(b) ? null : new String(b);
  }

  public static byte[] fromLong(long val) {
    return Longs.toByteArray(val);
  }

  public static byte[] fromInt(int val) {
    return Ints.toByteArray(val);
  }

  /**
   * get bytes data from object data.
   */
  public static byte[] fromObject(Object obj) {
    byte[] bytes = null;
    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
      objectOutputStream.writeObject(obj);
      objectOutputStream.flush();
      bytes = byteArrayOutputStream.toByteArray();
    } catch (IOException e) {
      log.error("Method objectToByteArray failed.", e);
    }
    return bytes;
  }

  /**
   * Stringify byte[] x
   * null for null
   * null for empty []
   */
  public static String toJsonHex(byte[] x) {
    return x == null || x.length == 0 ? "0x" : "0x" + Hex.toHexString(x);
  }


  public static String toJsonHex(Long x) {
    return x == null ? null : "0x" + Long.toHexString(x);
  }

  public static String toJsonHex(int x) {
    return toJsonHex((long) x);
  }

  public static String toJsonHex(String x) {
    return "0x" + x;
  }

  public static BigInteger hexToBigInteger(String input) {
    if (input.startsWith("0x")) {
      return new BigInteger(input.substring(2), 16);
    } else {
      return new BigInteger(input, 10);
    }
  }


  public static int jsonHexToInt(String x) throws Exception {
    if (!x.startsWith("0x")) {
      throw new Exception("Incorrect hex syntax");
    }
    x = x.substring(2);
    return Integer.parseInt(x, 16);
  }

  /**
   * Generate a subarray of a given byte array.
   *
   * @param input the input byte array
   * @param start the start index
   * @param end the end index
   * @return a subarray of <tt>input</tt>, ranging from <tt>start</tt> (inclusively) to <tt>end</tt>
   * (exclusively)
   */
  public static byte[] subArray(byte[] input, int start, int end) {
    byte[] result = new byte[end - start];
    System.arraycopy(input, start, result, 0, end - start);
    return result;
  }

  public static boolean isEmpty(byte[] input) {
    return input == null || input.length == 0;
  }

  public static boolean matrixContains(List<byte[]> source, byte[] obj) {
    for (byte[] sobj : source) {
      if (Arrays.equals(sobj, obj)) {
        return true;
      }
    }
    return false;
  }

  public static String fromHex(String x) {
    if (x.startsWith("0x")) {
      x = x.substring(2);
    }
    if (x.length() % 2 != 0) {
      x = "0" + x;
    }
    return x;
  }

  public static int byte2int(byte b) {
    return b & 0xFF;
  }
}
