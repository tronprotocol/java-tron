/*
 * Copyright by the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tron.core.zen.address;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.tron.common.utils.Bech32;
import org.tron.common.utils.Bech32.Bech32Data;


public class KeyIo {

  private static int CONVERTED_SAPLING_PAYMENT_ADDRESS_SIZE = ((32 + 11) * 8 + 4) / 5;
  private static String SAPLING_PAYMENT_ADDRESS = "ztron";

  public static PaymentAddress decodePaymentAddress(String str) {
    byte[] data;
    Bech32Data bech = Bech32.decode(str);
    if (bech.hrp.equals(SAPLING_PAYMENT_ADDRESS)
        && bech.data.length == CONVERTED_SAPLING_PAYMENT_ADDRESS_SIZE) {
      data = convertBits(bech.data, 0, CONVERTED_SAPLING_PAYMENT_ADDRESS_SIZE, 5, 8, false);
      return PaymentAddress.decode(data);
    }
    return null;
  }

  public static String encodePaymentAddress(PaymentAddress zaddr) {
    byte[] serAddr = zaddr.encode();
    List<Byte> progBytes = new ArrayList<Byte>();
    for (int i = 0; i < serAddr.length; i++) {
      progBytes.add(serAddr[i]);
    }
    byte[] prog = convertBits(progBytes, 8, 5, true);
    return Bech32.encode(SAPLING_PAYMENT_ADDRESS, prog);
  }

  /**
   * Helper for re-arranging bits into groups.
   */
  private static byte[] convertBits(
      final byte[] in,
      final int inStart,
      final int inLen,
      final int fromBits,
      final int toBits,
      final boolean pad)
      throws IllegalArgumentException {
    int acc = 0;
    int bits = 0;

    // int size = 64;
    int size = inLen;
    ByteArrayOutputStream out = new ByteArrayOutputStream(size); // todo:size

    final int maxv = (1 << toBits) - 1;
    final int maxAcc = (1 << (fromBits + toBits - 1)) - 1;
    for (int i = 0; i < inLen; i++) {
      int value = in[i + inStart] & 0xff;
      if ((value >>> fromBits) != 0) {
        throw new IllegalArgumentException(
            String.format("Input value '%X' exceeds '%d' bit size", value, fromBits));
      }
      acc = ((acc << fromBits) | value) & maxAcc;
      bits += fromBits;
      while (bits >= toBits) {
        bits -= toBits;
        out.write((acc >>> bits) & maxv);
      }
    }
    if (pad) {
      if (bits > 0) {
        out.write((acc << (toBits - bits)) & maxv);
      }
    } else if (bits >= fromBits || ((acc << (toBits - bits)) & maxv) != 0) {
      throw new IllegalArgumentException("Could not convert bits, invalid padding");
    }
    return out.toByteArray();
  }

  private static byte[] convertBits(List<Byte> data, int fromBits, int toBits, boolean pad) {
    int acc = 0;
    int bits = 0;
    int maxv = (1 << toBits) - 1;
    List<Byte> ret = new ArrayList<Byte>();

    for (Byte value : data) {
      short b = (short) (value.byteValue() & 0xff);
      if (b < 0) {
        throw new IllegalArgumentException();
      } else if ((b >> fromBits) > 0) {
        throw new IllegalArgumentException();
      }

      acc = (acc << fromBits) | b;
      bits += fromBits;
      while (bits >= toBits) {
        bits -= toBits;
        ret.add((byte) ((acc >> bits) & maxv));
      }
    }

    if (pad && (bits > 0)) {
      ret.add((byte) ((acc << (toBits - bits)) & maxv));
    } else if (bits >= fromBits || (byte) ((acc << (toBits - bits)) & maxv) != 0) {
      return null;
    }

    byte[] buf = new byte[ret.size()];
    for (int i = 0; i < ret.size(); i++) {
      buf[i] = ret.get(i);
    }

    return buf;
  }
}