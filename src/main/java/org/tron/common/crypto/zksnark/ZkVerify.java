package org.tron.common.crypto.zksnark;

import java.math.BigInteger;
import org.apache.commons.lang3.ArrayUtils;

public class ZkVerify {

  public boolean pairing(G1Point[] g1, G2Point[] g2) {
    if (ArrayUtils.isEmpty(g1) || ArrayUtils.isEmpty(g2)) {
      return false;
    }
    if (g1.length != g2.length) {
      return false;
    }

    PairingCheck check = PairingCheck.create();
    for (int i = 0; i < g1.length; i++) {
      check.addPair(g1[i].toBN128G1(), g2[i].toBN128G2());
    }
    check.run();
    int result = check.result();

    if (result == 0) {
      return false;
    }
    return true;
  }

  public boolean pairingProd2(G1Point a1, G2Point a2, G1Point b1, G2Point b2) {
    G1Point[] p1 = new G1Point[2];
    G2Point[] p2 = new G2Point[2];

    p1[0] = a1;
    p1[1] = b1;
    p2[0] = a2;
    p2[1] = b2;

    return pairing(p1, p2);
  }

  public boolean pairingProd3(G1Point a1, G2Point a2, G1Point b1, G2Point b2, G1Point c1,
      G2Point c2) {
    G1Point[] p1 = new G1Point[3];
    G2Point[] p2 = new G2Point[3];

    p1[0] = a1;
    p1[1] = b1;
    p1[2] = c1;
    p2[0] = a2;
    p2[1] = b2;
    p2[2] = c2;

    return pairing(p1, p2);
  }

  public boolean pairingProd4(G1Point a1, G2Point a2, G1Point b1, G2Point b2, G1Point c1,
      G2Point c2, G1Point d1, G2Point d2) {
    G1Point[] p1 = new G1Point[4];
    G2Point[] p2 = new G2Point[4];

    p1[0] = a1;
    p1[1] = b1;
    p1[2] = c1;
    p1[3] = d1;
    p2[0] = a2;
    p2[1] = b2;
    p2[2] = c2;
    p2[3] = d2;

    return pairing(p1, p2);
  }

  public int verify(VerifyingKey vk, BigInteger[] input, Proof proof) {
    for (int i = 0; i < input.length; i++) {
      System.out.println(input[i].toString(10));
      System.out.println(input[i].toString(16));
    }
    System.out.println(proof.getK().getX().toString(10));
    System.out.println(proof.getK().getX().toString(16));
    System.out.println(proof.getK().getY().toString(10));
    System.out.println(proof.getK().getY().toString(16));
    if (input.length + 1 != vk.getIC().length) {
      return -1;
    }

    G1Point vk_x = new G1Point(0, 0);
    for (int i = 0; i < input.length; i++) {
      vk_x = vk_x.add(vk.getIC()[i + 1].mul(input[i]));
    }
    vk_x = vk_x.add(vk.getIC()[0]);
    G2Point p2 = G2Point.P2();
    if (!pairingProd2(proof.getA(), vk.getA(), proof.getA_p().negate(), p2)) {
      return 1;
    }
    if (!pairingProd2(vk.getB(), proof.getB(), proof.getB_p().negate(), p2)) {
      return 2;
    }
    if (!pairingProd2(proof.getC(), vk.getC(), proof.getC_p().negate(), p2)) {
      return 3;
    }
    if (!pairingProd3(proof.getK(), vk.getGamma(),
        (vk_x.add(proof.getA().add(proof.getC()))).negate(), vk.getGammaBeta2(),
        vk.getGammaBeta1().negate(), proof.getB())) {
      return 4;
    }
    if (!pairingProd3(vk_x.add(proof.getA()), proof.getB(), proof.getH().negate(), vk.getZ(),
        proof.getC().negate(), p2)) {
      return 5;
    }
    return 0;
  }
}
