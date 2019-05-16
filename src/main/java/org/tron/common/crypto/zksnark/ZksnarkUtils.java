/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.crypto.zksnark;


import com.google.protobuf.ByteString;
import java.math.BigInteger;
import org.tron.common.crypto.blake2b.Blake2b;
import org.tron.common.crypto.eddsa.EdDSAPublicKey;
import org.tron.common.crypto.eddsa.spec.EdDSANamedCurveSpec;
import org.tron.common.crypto.eddsa.spec.EdDSANamedCurveTable;
import org.tron.common.crypto.eddsa.spec.EdDSAPublicKeySpec;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;

public class ZksnarkUtils {


  public static EdDSAPublicKey byte2PublicKey(byte[] pk) {
    EdDSANamedCurveSpec curveSpec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);
    EdDSAPublicKeySpec spec = new EdDSAPublicKeySpec(pk, curveSpec);
    EdDSAPublicKey publicKey = new EdDSAPublicKey(spec);
    return publicKey;
  }


  public static void sort(byte[] bytes) {
    int len = bytes.length / 2;
    for (int i = 0; i < len; i++) {
      byte b = bytes[i];
      bytes[i] = bytes[bytes.length - i - 1];
      bytes[bytes.length - i - 1] = b;
    }
  }

  private static void to253Bits(byte[] src, byte[] ret, int offset) {
    for (int i = 0; i < ret.length; i++) {
      ret[i] = 0;
    }
    for (int i = 0; i < 253; i++, offset++) {
      if ((src[offset / 8] & (1 << (7 - (offset % 8)))) != 0) {
        ret[i / 8] |= 1 << (i % 8);
      }
    }
    sort(ret);
  }

  private static void to152Bits(byte[] src, byte[] ret, int offset) {
    for (int i = 0; i < ret.length; i++) {
      ret[i] = 0;
    }
    for (int i = 0; i < 152; i++, offset++) {
      if ((src[offset / 8] & (1 << (7 - (offset % 8)))) != 0) {
        ret[i / 8] |= 1 << (i % 8);
      }
    }
    sort(ret);
  }

  public static BigInteger[] witnessMap(
      byte[] rt,
      byte[] h_sig,
      byte[] h1,
      byte[] h2,
      byte[] nf1,
      byte[] nf2,
      byte[] cm1,
      byte[] cm2,
      long vpub_old,
      long vpub_new
  ) {
    System.out.println(ByteArray.toHexString(h_sig));
    System.out.println(ByteArray.toHexString(rt));
    System.out.println(ByteArray.toHexString(h1));
    System.out.println(ByteArray.toHexString(h2));
    System.out.println(ByteArray.toHexString(nf1));
    System.out.println(ByteArray.toHexString(nf2));
    byte[] vold = ByteArray.fromLong(vpub_old);
    sort(vold);
    byte[] vnew = ByteArray.fromLong(vpub_new);
    sort(vnew);
    byte[] temp = new byte[21];

    byte[] witness = ByteUtil.merge(rt, h_sig, nf1, h1, nf2, h2, cm1, cm2, vold, vnew);

    int offset = 0;
    to253Bits(witness, rt, offset);
    offset += 253;
    to253Bits(witness, h_sig, offset);
    offset += 253;
    to253Bits(witness, nf1, offset);
    offset += 253;
    to253Bits(witness, h1, offset);
    offset += 253;
    to253Bits(witness, nf2, offset);
    offset += 253;
    to253Bits(witness, h2, offset);
    offset += 253;
    to253Bits(witness, cm1, offset);
    offset += 253;
    to253Bits(witness, cm2, offset);
    offset += 253;
    to152Bits(witness, temp, offset);

    BigInteger[] result = new BigInteger[9];
    result[0] = new BigInteger(rt);
    result[1] = new BigInteger(h_sig);
    result[2] = new BigInteger(nf1);
    result[3] = new BigInteger(h1);
    result[4] = new BigInteger(nf2);
    result[5] = new BigInteger(h2);
    result[6] = new BigInteger(cm1);
    result[7] = new BigInteger(cm2);
    result[8] = new BigInteger(temp);
    return result;
  }

  public static void main(String[] args) {
    byte[] rt = ByteArray
        .fromHexString("2549f0f69104f687e926819ce20226226157627e3bc426c264389f5708b7b5a1");
    byte[] hsig = ByteArray
        .fromHexString("c8d7e310080ec1f830dc98f42eb45c4022f55faeaedfaeee95b6108b56664fa2");
    byte[] h1 = ByteArray
        .fromHexString("764f5da7cbbf3178f4e4bf7da9002b9cb8b70b1d00977b081491925c985bf022");
    byte[] h2 = ByteArray
        .fromHexString("a633708072886bcac55e9ec8a2be34c7f611e9b5ac37f0aead55183995fdba93");
    byte[] nf1 = ByteArray
        .fromHexString("7d047228554bc6ffcb0747838285e72b73484b4095a44eb36f38d5359a18d6df");
    byte[] nf2 = ByteArray
        .fromHexString("8c964eaefbf8b07fe8e8165582626e2f2312e4c61f423b210aaf98da871869f0");
    byte[] cm1 = ByteArray
        .fromHexString("2a58c84eb33b0eed82986f10b27ba46ecf605f482345e19bc3265eff647c27b7");
    byte[] cm2 = ByteArray
        .fromHexString("7f6b9552b380d9cd27c4c4f21ddf95f01a0c63a02f9e8060efc044c4975d7f53");
    long old = 1000000000;
    long newP = 0;

    BigInteger[] witnessMap = witnessMap(rt, hsig, h1, h2, nf1, nf2, cm1, cm2, old, newP);
    for (int i = 0; i < witnessMap.length; i++) {
      BigInteger integer = witnessMap[i];
      System.out.println(integer.toString(10));
      System.out.println(integer.toString(16));
    }

//    String input = "00100101 01001001 11110000 11110110 10010001 00000100 11110110 10000111 11101001 00100110 10000001 10011100 11100010 00000010 00100110 00100010 01100001 01010111 01100010 01111110 00111011 11000100 00100110 11000010 01100100 00111000 10011111 01010111 00001000 10110111 10110101 10100001";
//    int i = 0;
//    int j = 0;
//    byte[] b = new byte[32];
//
//    while (i < 253) {
//      char c = input.charAt(j);
//      if (c == '1') {
//        b[i / 8] |= 1 << (i % 8);
//        i++;
//      } else if (c == '0') {
//        i++;
//      }
//      j++;
//    }
//
//    System.out.println(ByteArray.toHexString(b));
//    VerifyingKey.sort(b);
//    BigInteger integer = new BigInteger(b);
//    System.out.println(integer.toString(10));
  }
}
