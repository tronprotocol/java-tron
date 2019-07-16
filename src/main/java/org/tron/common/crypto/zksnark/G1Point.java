package org.tron.common.crypto.zksnark;

import java.math.BigInteger;
import org.tron.common.utils.BIUtil;

public class G1Point {

  private BigInteger x;
  private BigInteger y;

  public G1Point(long x, long y) {
    this.x = BigInteger.valueOf(x);
    this.y = BigInteger.valueOf(y);
  }

  public G1Point(BigInteger x, BigInteger y) {
    this.x = x;
    this.y = y;
  }

  public G1Point(String x, String y) {
    this.x = new BigInteger(x, 10);
    this.y = new BigInteger(y, 10);
  }

  public G1Point(byte[] x, byte[] y) {
    this.x = new BigInteger(x);
    this.y = new BigInteger(y);
  }

  public BigInteger getX() {
    return x;
  }

  public BigInteger getY() {
    return y;
  }

  public byte[] getXBytes() {
    return x.toByteArray();
  }

  public byte[] getYBytes() {
    return y.toByteArray();
  }

  public static G1Point P1() {
    return new G1Point(1, 2);
  }

  public BN128<Fp> toBN128() {
    return BN128Fp.create(x.toByteArray(), y.toByteArray());
  }

  public static G1Point fromBN128(BN128<Fp> p) {
    return new G1Point(p.x.bytes(), p.y.bytes());
  }

  public BN128G1 toBN128G1() {
    return BN128G1.create(x.toByteArray(), y.toByteArray());
  }

  public G1Point negate() {
    BigInteger q = new BigInteger(
        "21888242871839275222246405745257275088696311157297823662689037894645226208583");
    if (getX() == BigInteger.valueOf(0) && getY() == BigInteger.valueOf(0)) {
      return new G1Point(0, 0);
    }
    return new G1Point(getX(), q.subtract(getY().mod(q)));
  }

  public G1Point add(G1Point q) {
    if (q == null) {
      return null;
    }

    BN128<Fp> p1 = BN128Fp.create(getXBytes(), getYBytes());
    if (p1 == null) {
      return null;
    }

    BN128<Fp> p2 = BN128Fp.create(q.getXBytes(), q.getYBytes());
    if (p2 == null) {
      return null;
    }

    BN128<Fp> res = p1.add(p2).toEthNotation();
    return new G1Point(res.x.bytes(), res.y.bytes());
  }

  public G1Point mul(BigInteger s) {
    if (s == null) {
      return null;
    }

    BN128<Fp> p = this.toBN128();
    if (p == null) {
      return null;
    }
    BN128<Fp> res = p.mul(BIUtil.toBI(s.toByteArray())).toEthNotation();
    return fromBN128(res);
  }
}
