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
import org.tron.protos.Contract.BN128G1;
import org.tron.protos.Contract.BN128G2;
import org.tron.protos.Contract.zkv0proof;

public class ZksnarkUtils {

  public static byte[] computeHSig(org.tron.protos.Contract.ZksnarkV0TransferContract zkContract) {
    byte[] message = ByteUtil
        .merge(zkContract.getRandomSeed().toByteArray(), zkContract.getNf1().toByteArray(),
            zkContract.getNf2().toByteArray(), zkContract.getPksig().toByteArray());
    return Blake2b.hash(message);
  }

  public static byte[] computeZkSignInput(
      org.tron.protos.Contract.ZksnarkV0TransferContract zkContract) {
    byte[] hSig = computeHSig(zkContract);
    org.tron.protos.Contract.ZksnarkV0TransferContract.Builder builder = zkContract.toBuilder();
    builder.setRandomSeed(ByteString.EMPTY);
    builder.setPksig(ByteString.copyFrom(hSig));
    return builder.build().toByteArray();
  }

  public static EdDSAPublicKey byte2PublicKey(byte[] pk) {
    EdDSANamedCurveSpec curveSpec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);
    EdDSAPublicKeySpec spec = new EdDSAPublicKeySpec(pk, curveSpec);
    EdDSAPublicKey publicKey = new EdDSAPublicKey(spec);
    return publicKey;
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
    BigInteger[] result = new BigInteger[9];
    result[0] = new BigInteger(rt);
    result[1] = new BigInteger(h_sig);
    result[2] = new BigInteger(nf1);
    result[3] = new BigInteger(h1);
    result[4] = new BigInteger(nf2);
    result[5] = new BigInteger(h2);
    result[6] = new BigInteger(cm1);
    result[7] = new BigInteger(cm2);

    byte[] vold = ByteArray.fromLong(vpub_old);
    byte[] vnew = ByteArray.fromLong(vpub_new);
    byte[] temp = new byte[32];
    System.arraycopy(temp, 24 - vold.length, vold, 0, vold.length);
    System.arraycopy(temp, 32 - vnew.length, vnew, 0, vnew.length);
    result[8] = new BigInteger(temp);
    return result;
  }

  public static boolean isEmpty(BN128G1 g1) {
    if (g1 == null || g1 == BN128G1.getDefaultInstance()) {
      return true;
    }
    if (g1.getX().isEmpty()) {
      return true;
    }
    if (g1.getY().isEmpty()) {
      return true;
    }
    return false;
  }

  public static boolean isEmpty(BN128G2 g2) {
    if (g2 == null || g2 == BN128G2.getDefaultInstance()) {
      return true;
    }
    if (g2.getX1().isEmpty()) {
      return true;
    }
    if (g2.getX2().isEmpty()) {
      return true;
    }
    if (g2.getY1().isEmpty()) {
      return true;
    }
    if (g2.getY2().isEmpty()) {
      return true;
    }
    return false;
  }

  public static Proof zkproof2Proof(zkv0proof proof) {
    if (isEmpty(proof.getA())) {
      return null;
    }
    G1Point A = new G1Point(proof.getA().getX().toByteArray(), proof.getA().getY().toByteArray());

    if (isEmpty(proof.getAP())) {
      return null;
    }
    G1Point A_p = new G1Point(proof.getA().getX().toByteArray(), proof.getA().getY().toByteArray());

    if (isEmpty(proof.getB())) {
      return null;
    }
    G2Point B = new G2Point(proof.getB().getX1().toByteArray(), proof.getB().getX2().toByteArray(),
        proof.getB().getY1().toByteArray(), proof.getB().getY2().toByteArray());

    if (isEmpty(proof.getBP())) {
      return null;
    }
    G1Point B_p = new G1Point(proof.getBP().getX().toByteArray(),
        proof.getBP().getY().toByteArray());

    if (isEmpty(proof.getC())) {
      return null;
    }
    G1Point C = new G1Point(proof.getC().getX().toByteArray(), proof.getC().getY().toByteArray());

    if (isEmpty(proof.getCP())) {
      return null;
    }
    G1Point C_p = new G1Point(proof.getCP().getX().toByteArray(),
        proof.getCP().getY().toByteArray());

    if (isEmpty(proof.getK())) {
      return null;
    }
    G1Point K = new G1Point(proof.getK().getX().toByteArray(), proof.getK().getY().toByteArray());

    if (isEmpty(proof.getH())) {
      return null;
    }
    G1Point H = new G1Point(proof.getH().getX().toByteArray(), proof.getH().getY().toByteArray());

    return new Proof(A, A_p, B, B_p, C, C_p, H, K);
  }

  public static zkv0proof proof2Zkproof(Proof proof) {
    return null;
  }
}
