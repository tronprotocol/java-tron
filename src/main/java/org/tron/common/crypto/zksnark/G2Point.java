package org.tron.common.crypto.zksnark;

import java.math.BigInteger;

public class G2Point {

  private BigInteger[] x;
  private BigInteger[] y;

  public G2Point(BigInteger x1, BigInteger x2, BigInteger y1, BigInteger y2) {
    x[0] = x1;
    x[1] = x2;
    y[0] = y1;
    y[1] = y2;
  }

  public BN128G2 toBN128G2() {
    return BN128G2
        .create(x[0].toByteArray(), x[1].toByteArray(), y[0].toByteArray(), y[1].toByteArray());
  }

  public static G2Point P2() {
    return new G2Point(new BigInteger(
        "11559732032986387107991004021392285783925812861821192530917403151452391805634"),
        new BigInteger(
            "10857046999023057135944570762232829481370756359578518086990519993285655852781"),
        new BigInteger(
            "4082367875863433681332203403145435568316851327593401208105741076214120093531"),
        new BigInteger(
            "8495653923123431417604973247489272438418190587263600148770280649306958101930"));
  }
}
