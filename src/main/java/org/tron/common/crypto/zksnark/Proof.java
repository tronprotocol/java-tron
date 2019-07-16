package org.tron.common.crypto.zksnark;

public class Proof {

  public G1Point getA() {
    return A;
  }

  public G1Point getA_p() {
    return A_p;
  }

  public G2Point getB() {
    return B;
  }

  public G1Point getB_p() {
    return B_p;
  }

  public G1Point getC() {
    return C;
  }

  public G1Point getC_p() {
    return C_p;
  }

  public G1Point getK() {
    return K;
  }

  public G1Point getH() {
    return H;
  }

  private G1Point A;
  private G1Point A_p;
  private G2Point B;
  private G1Point B_p;
  private G1Point C;
  private G1Point C_p;
  private G1Point K;
  private G1Point H;

  public Proof(G1Point A, G1Point A_p, G2Point B, G1Point B_p, G1Point C, G1Point C_p, G1Point H,
      G1Point K) {
    this.A = A;
    this.A_p = A_p;
    this.B = B;
    this.B_p = B_p;
    this.C = C;
    this.C_p = C_p;
    this.K = K;
    this.H = H;
  }

}
