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

import com.google.common.primitives.Longs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

@Slf4j
public class TypeConversion {

  public static byte[] longToBytes(long x) {
    return Longs.toByteArray(x);
  }

  public static long bytesToLong(byte[] bytes) {
    return Longs.fromByteArray(bytes);
  }

  public static String bytesToHexString(byte[] src) {
    return Hex.encodeHexString(src);
  }

  public static byte[] hexStringToBytes(String hexString) {
    try {
      return Hex.decodeHex(hexString);
    } catch (DecoderException e) {
      logger.debug(e.getMessage(), e);
      return null;
    }
  }

  public static boolean increment(byte[] bytes) {
    final int startIndex = 0;
    int i;
    for (i = bytes.length - 1; i >= startIndex; i--) {
      bytes[i]++;
      if (bytes[i] != 0) {
        break;
      }
    }

    return (i >= startIndex || bytes[startIndex] != 0);
  }
}
