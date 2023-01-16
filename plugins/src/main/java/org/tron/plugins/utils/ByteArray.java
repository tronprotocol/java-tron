package org.tron.plugins.utils;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.math.BigInteger;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;

public class ByteArray {

  public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
  public static final byte[] ZERO_BYTE_ARRAY = new byte[]{0};
  public static final int WORD_SIZE = 32;

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

  /**
   * get long data from bytes data.
   */
  public static long toLong(byte[] b) {
    return ArrayUtils.isEmpty(b) ? 0 : new BigInteger(1, b).longValue();
  }

  public static byte[] fromInt(int val) {
    return Ints.toByteArray(val);
  }

  /**
   * get int data from bytes data.
   */
  public static int toInt(byte[] b) {
    return ArrayUtils.isEmpty(b) ? 0 : new BigInteger(1, b).intValue();
  }

  public static int compareUnsigned(byte[] a, byte[] b) {
    if (a == b) {
      return 0;
    }
    if (a == null) {
      return -1;
    }
    if (b == null) {
      return 1;
    }
    int minLen = Math.min(a.length, b.length);
    for (int i = 0; i < minLen; ++i) {
      int aVal = a[i] & 0xFF;
      int bVal = b[i] & 0xFF;
      if (aVal < bVal) {
        return -1;
      }
      if (aVal > bVal) {
        return 1;
      }
    }
    if (a.length < b.length) {
      return -1;
    }
    if (a.length > b.length) {
      return 1;
    }
    return 0;
  }

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
}
