package org.tron.common.zksnark.zen.utils;

import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;
import org.tron.common.zksnark.zen.Librustzcash;

public class PRF {

  public static byte[] prfAsk(byte[] sk) {
    byte[] ask = new byte[32];
    byte t = 0x00;
    byte[] tmp = prfExpand(sk, t);
    Librustzcash.librustzcashToScalar(tmp, ask);
    return ask;
  }

  public static byte[] prfNsk(byte[] sk) {
    byte[] nsk = new byte[32];
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
    Sodium sodium = NaCl.sodium();
    System.arraycopy(sk, 0, blob, 0, 32);
    blob[32] = t;

    byte[] state = new byte[Sodium.crypto_generichash_statebytes()];
    byte[] key = new byte[Sodium.crypto_generichash_keybytes()];

    // Sodium.crypto_generichash_blake2b_init(state, key, 0, 32);
    Sodium.crypto_generichash_blake2b_update(state, blob, 33);
    Sodium.crypto_generichash_blake2b_final(state, res, 64);

    return res;
  }
}
