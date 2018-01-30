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
import org.spongycastle.util.encoders.Hex;

public class ByteArray {

  public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

  public static String toHexString(byte[] data) {
    return data == null ? "" : Hex.toHexString(data);
  }

  public static byte[] fromHexString(String data) {
    if (data == null) {
      return EMPTY_BYTE_ARRAY;
    }
    if (data.startsWith("0x")) {
      data = data.substring(2);
    }
    if (data.length() % 2 == 1) {
      data = "0" + data;
    }
    return Hex.decode(data);
  }

  public static long toLong(byte[] b) {
    if (b == null || b.length == 0) {
      return 0;
    }
    return new BigInteger(1, b).longValue();
  }

  public static byte[] fromString(String str) {
    if (str == null) {
      return null;
    }

    return str.getBytes();
  }

  public static String toStr(byte[] byteArray) {
    if (byteArray == null) {
      return null;
    }

    return new String(byteArray);
  }

  public static byte[] fromLong(long val) {
    return ByteBuffer.allocate(8).putLong(val).array();
  }
}
