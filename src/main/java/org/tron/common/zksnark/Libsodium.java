package org.tron.common.zksnark;

import com.sun.jna.IntegerType;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

public class Libsodium {

  private static ILibsodium INSTANCE;

  public static final int crypto_generichash_blake2b_PERSONALBYTES = 16;
  public static final int crypto_aead_chacha20poly1305_ietf_NPUBBYTES = 12;
  public static final int crypto_aead_chacha20poly1305_IETF_NPUBBYTES = crypto_aead_chacha20poly1305_ietf_NPUBBYTES;

  static {
    INSTANCE = (ILibsodium) Native
        .loadLibrary(Librustzcash.getLibraryByName("libsodium"), ILibsodium.class);
  }


  public interface ILibsodium extends Library {

    class crypto_generichash_blake2b_state extends Structure {

      public static class ByReference extends crypto_generichash_blake2b_state implements
          Structure.ByReference {

      }

      public static class ByValue extends crypto_generichash_blake2b_state implements
          Structure.ByValue {

      }

      public static class sizeT extends IntegerType {

        public sizeT() {
          this(0);
        }

        public sizeT(long value) {
          super(Native.POINTER_SIZE, value);
        }
      }

      public long h[] = new long[8];
      public long t[] = new long[2];
      public long f[] = new long[2];
      public byte buf[] = new byte[2 * 128];
      public sizeT buflen = new sizeT();
      public byte last_node;
    }

    int crypto_generichash_blake2b_init(crypto_generichash_blake2b_state.ByReference state,
        byte[] key, int kenlen, int outlen);

    int crypto_generichash_blake2b_init_salt_personal(
        crypto_generichash_blake2b_state.ByReference state, byte[] key, int keylen, int outlen,
        byte[] salt, byte[] personal);

    int crypto_generichash_blake2b_update(crypto_generichash_blake2b_state.ByReference state,
        byte[] in, long inlen);

    int crypto_generichash_blake2b_final(crypto_generichash_blake2b_state.ByReference state,
        byte[] out, int outlen);

    int crypto_generichash_blake2b_salt_personal(byte[] out, int outlen, byte[] in, long inlen,
        byte[] key, int keylen, byte[] salt, byte[] personal);

    int crypto_aead_chacha20poly1305_ietf_decrypt(byte[] m, long[] mlen_p, byte[] nsec, byte[] c,
        long clen, byte[] ad, long adlen, byte[] npub, byte[] k);

    int crypto_aead_chacha20poly1305_ietf_encrypt(byte[] c, long[] clen_p, byte[] m,
        long mlen, byte[] ad, long adlen, byte[] nsec, byte[] npub, byte[] k);
  }

  public static int cryptoGenerichashBlake2bInitSaltPersonal(
      ILibsodium.crypto_generichash_blake2b_state.ByReference state,
      byte[] key, int keylen, int outlen, byte[] salt, byte[] personal) {
    return INSTANCE
        .crypto_generichash_blake2b_init_salt_personal(state, key, keylen, outlen, salt, personal);
  }

  public static int cryptoGenerichashBlake2bUpdate(
      ILibsodium.crypto_generichash_blake2b_state.ByReference state,
      byte[] in, long inlen) {
    return INSTANCE.crypto_generichash_blake2b_update(state, in, inlen);
  }

  public static int cryptoGenerichashBlake2bFinal(
      ILibsodium.crypto_generichash_blake2b_state.ByReference state,
      byte[] out, int outlen) {
    return INSTANCE.crypto_generichash_blake2b_final(state, out, outlen);
  }

  public static int cryptoGenerichashBlack2bSaltPersonal(byte[] out, int outlen, byte[] in,
      long inlen, byte[] key, int keylen, byte[] salt, byte[] personal) {
    return INSTANCE
        .crypto_generichash_blake2b_salt_personal(out, outlen, in, inlen, key, keylen, salt,
            personal);
  }

  public static int cryptoAeadChacha20poly1305IetfDecrypt(byte[] m, long[] mlen_p, byte[] nsec,
      byte[] c, long clen, byte[] ad, long adlen, byte[] npub, byte[] k) {
    return INSTANCE
        .crypto_aead_chacha20poly1305_ietf_decrypt(m, mlen_p, nsec, c, clen, ad, adlen, npub, k);
  }

  public static int cryptoAeadChacha20Poly1305IetfEncrypt(byte[] c, long[] clen_p, byte[] m,
      long mlen, byte[] ad, long adlen, byte[] nsec, byte[] npub, byte[] k) {
    return INSTANCE
        .crypto_aead_chacha20poly1305_ietf_encrypt(c, clen_p, m, mlen, ad, adlen, nsec, npub, k);
  }

  public static void test(byte[] K, byte[] ovk, byte[] cv, byte[] cm, byte[] epk) {
    byte[] block = new byte[128];

    System.arraycopy(ovk, 0, block, 0, 32);
    System.arraycopy(cv, 0, block, 32, 32);
    System.arraycopy(cm, 0, block, 64, 32);
    System.arraycopy(epk, 0, block, 96, 32);

    byte[] personalization = new byte[32];
    byte[] aa = "Zcash_Derive_ock".getBytes();
    System.arraycopy(aa, 0, personalization, 0, aa.length);
    if (Libsodium.cryptoGenerichashBlack2bSaltPersonal(K, 32,
        block, 128,
        null, 0, // No key.
        null,    // No salt.
        personalization
    ) != 0) {
      System.out.println("cryptoGenerichashBlack2bSaltPersonal return pok...");
      //throw new RuntimeException("hash function failure");
    } else {
      System.out.println("cryptoGenerichashBlack2bSaltPersonal return ok....");
      for (int i = 0; i < personalization.length; i++) {
        System.out.print(personalization[i] + " ");
      }
      System.out.println();
    }

    byte[] cipher_nonce = new byte[crypto_aead_chacha20poly1305_IETF_NPUBBYTES];

    if (Libsodium.cryptoAeadChacha20poly1305IetfDecrypt(
        new byte[1024], null,
        null,
        new byte[1024], 1024,
        null,
        0,
        cipher_nonce, K) != 0) {
      System.out.println("cryptoAeadChacha20poly1305IetfDecrypt return true.");
    } else {
      System.out.println("cryptoAeadChacha20poly1305IetfDecrypt return false.");
    }

    return;
  }

  public static void main(String[] args) {
    byte[] K = new byte[32];
    byte[] ovk = new byte[32];
    byte[] cv = new byte[32];
    byte[] cm = new byte[32];
    byte[] epk = new byte[32];

    test(K, ovk, cv, cm, epk);
  }
}
