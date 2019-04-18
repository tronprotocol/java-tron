package org.tron.common.zksnark.zen.utils;

import org.tron.common.zksnark.zen.Librustzcash;
import org.tron.common.zksnark.zen.Libsodium;
import org.tron.common.zksnark.zen.Libsodium.ILibsodium.crypto_generichash_blake2b_state;

public class PRF {

  public static byte[] prfAsk(byte[] sk) {
    byte[] ask = new byte[32];
    byte t = 0x00;
    byte[] tmp = prfExpand(sk, t);
    Librustzcash.librustzcashToScalar(tmp, ask);
    return ask;
  }

  public static byte[] prfNsk(byte[] sk) {
    byte[] nsk = null;
    byte t = 0x01;
    byte[] tmp = prfExpand(sk, t);
    Librustzcash.librustzcashToScalar(tmp, nsk);
    return nsk;
  }

  public static byte[] prfOvk(byte[] sk) {
    byte[] ovk = new byte[32];
    byte t = 0x02;
    byte[] tmp = prfExpand(sk, t);
    System.arraycopy(tmp, 0, ovk, 0, 32);
    return ovk;
  }

  private static byte[] prfExpand(byte[] sk, byte t) {
    byte[] res = new byte[64];
    byte[] blob = new byte[33];

    System.arraycopy(sk, 0, blob, 0, 32);
    blob[32] = t;
    crypto_generichash_blake2b_state.ByReference state = new crypto_generichash_blake2b_state.ByReference();
    Libsodium.cryptoGenerichashBlake2bUpdate(state, blob, 33);
    Libsodium.cryptoGenerichashBlake2bFinal(state, res, 64);
    return res;
  }
}
