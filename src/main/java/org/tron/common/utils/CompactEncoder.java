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

import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;
import static org.spongycastle.util.Arrays.concatenate;
import static org.spongycastle.util.encoders.Hex.encode;
import static org.tron.common.utils.ByteUtil.appendByte;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class CompactEncoder {

  private final static byte TERMINATOR = 16;
  private final static Map<Character, Byte> hexMap = new HashMap<>();

  static {
    hexMap.put('0', (byte) 0x0);
    hexMap.put('1', (byte) 0x1);
    hexMap.put('2', (byte) 0x2);
    hexMap.put('3', (byte) 0x3);
    hexMap.put('4', (byte) 0x4);
    hexMap.put('5', (byte) 0x5);
    hexMap.put('6', (byte) 0x6);
    hexMap.put('7', (byte) 0x7);
    hexMap.put('8', (byte) 0x8);
    hexMap.put('9', (byte) 0x9);
    hexMap.put('a', (byte) 0xa);
    hexMap.put('b', (byte) 0xb);
    hexMap.put('c', (byte) 0xc);
    hexMap.put('d', (byte) 0xd);
    hexMap.put('e', (byte) 0xe);
    hexMap.put('f', (byte) 0xf);
  }

  /**
   * Pack nibbles to binary
   *
   * @param nibbles sequence. may have a terminator
   * @return hex-encoded byte array
   */
  public static byte[] packNibbles(byte[] nibbles) {
    int terminator = 0;

    if (nibbles[nibbles.length - 1] == TERMINATOR) {
      terminator = 1;
      nibbles = copyOf(nibbles, nibbles.length - 1);
    }
    int oddlen = nibbles.length % 2;
    int flag = 2 * terminator + oddlen;
    if (oddlen != 0) {
      byte[] flags = new byte[]{(byte) flag};
      nibbles = concatenate(flags, nibbles);
    } else {
      byte[] flags = new byte[]{(byte) flag, 0};
      nibbles = concatenate(flags, nibbles);
    }
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    for (int i = 0; i < nibbles.length; i += 2) {
      buffer.write(16 * nibbles[i] + nibbles[i + 1]);
    }
    return buffer.toByteArray();
  }

  public static boolean hasTerminator(byte[] packedKey) {
    return ((packedKey[0] >> 4) & 2) != 0;
  }

  /**
   * Unpack a binary string to its nibbles equivalent
   *
   * @param str of binary data
   * @return array of nibbles in byte-format
   */
  public static byte[] unpackToNibbles(byte[] str) {
    byte[] base = binToNibbles(str);
    base = copyOf(base, base.length - 1);
    if (base[0] >= 2) {
      base = appendByte(base, TERMINATOR);
    }
    if (base[0] % 2 != 0) {
      base = copyOfRange(base, 1, base.length);
    } else {
      base = copyOfRange(base, 2, base.length);
    }
    return base;
  }

  /**
   * Transforms a binary array to hexadecimal format + terminator
   *
   * @param str byte[]
   * @return array with each individual nibble adding a terminator at the end
   */
  public static byte[] binToNibbles(byte[] str) {

    byte[] hexEncoded = encode(str);
    byte[] hexEncodedTerminated = Arrays.copyOf(hexEncoded, hexEncoded.length + 1);

    for (int i = 0; i < hexEncoded.length; ++i) {
      byte b = hexEncodedTerminated[i];
      hexEncodedTerminated[i] = hexMap.get((char) b);
    }

    hexEncodedTerminated[hexEncodedTerminated.length - 1] = TERMINATOR;
    return hexEncodedTerminated;
  }


  public static byte[] binToNibblesNoTerminator(byte[] str) {

    byte[] hexEncoded = encode(str);

    for (int i = 0; i < hexEncoded.length; ++i) {
      byte b = hexEncoded[i];
      hexEncoded[i] = hexMap.get((char) b);
    }

    return hexEncoded;
  }
}
