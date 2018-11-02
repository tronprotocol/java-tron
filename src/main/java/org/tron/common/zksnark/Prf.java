package org.tron.common.zksnark;


import org.tron.common.utils.Sha256Hash;

public class Prf {

  public static byte[] prfAddrAPk(final byte[] a_sk) {
    byte t = 0x00;
    return prfAddr(a_sk, t);
  }


  public static byte[] prfAddrSkEnc(final byte[] a_sk) {
    byte t = 0x01;
    return prfAddr(a_sk, t);
  }

  private static byte[] prf(boolean a, boolean b, boolean c, boolean d,
      final byte[] x,
      final byte[] y) {
    byte[] res;
    byte[] blob = new byte[64];

    System.arraycopy(x, 0, blob, 0, 32);
    System.arraycopy(y, 0, blob, 32, 32);

    blob[0] &= 0x0F;
    blob[0] |= (a ? 1 << 7 : 0) | (b ? 1 << 6 : 0) | (c ? 1 << 5 : 0) | (d ? 1 << 4 : 0);

    res = Sha256Hash.hash(blob);

    return res;
  }

  private static byte[] prfAddr(final byte[] a_sk, byte t) {
    byte[] y = new byte[32];
    y[0] = t;
    return prf(true, true, false, false, a_sk, y);
  }


}
