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

package org.tron.common.zksnark.sapling.utils;

import java.io.ByteArrayOutputStream;
import org.tron.common.zksnark.sapling.ZkChainParams;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.core.Bech32;
import org.tron.common.zksnark.sapling.core.Bech32.Bech32Data;

public class KeyIo {

  public static int ConvertedSaplingPaymentAddressSize = ((32 + 11) * 8 + 4) / 5;

  public static PaymentAddress decodePaymentAddress(String str) {
    byte[] data;
    Bech32Data bech = Bech32.decode(str);
    if (bech.hrp.equals(ZkChainParams.APLING_PAYMENT_ADDRESS)
        && bech.data.length == ConvertedSaplingPaymentAddressSize) {
      // Bech32 decoding
      //      data = new byte[((bech.data.length * 5) / 8)];
      data = convertBits(bech.data, 0, ConvertedSaplingPaymentAddressSize, 5, 8, false);

      return PaymentAddress.decode(data);
    }

    return null;
  }

  // todo:  base58
  public static String EncodePaymentAddress(PaymentAddress zaddr) {
    //    byte[] seraddr = zaddr.encode();
    //    byte[] data = new byte[(43 * 8 + 4) / 5];
    //
    //    ConvertBits< 8, 5, true > ([ &](unsigned char c){
    //      data.push_back(c);
    //    },seraddr.begin(), seraddr.end());
    //    return bech32::Encode (m_params.Bech32HRP(CChainParams::SAPLING_PAYMENT_ADDRESS), data);

    return "";
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
    ByteArrayOutputStream out = new ByteArrayOutputStream(64); // todo:size
    final int maxv = (1 << toBits) - 1;
    final int max_acc = (1 << (fromBits + toBits - 1)) - 1;
    for (int i = 0; i < inLen; i++) {
      int value = in[i + inStart] & 0xff;
      if ((value >>> fromBits) != 0) {
        throw new IllegalArgumentException(
            String.format("Input value '%X' exceeds '%d' bit size", value, fromBits));
      }
      acc = ((acc << fromBits) | value) & max_acc;
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
}
