package org.tron.common.zksnark;

import com.sun.jna.IntegerType;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

public class Libsodium {

  public static final int crypto_generichash_blake2b_PERSONALBYTES = 16;
  public static final int crypto_aead_chacha20poly1305_ietf_NPUBBYTES = 12;
  public static final int crypto_aead_chacha20poly1305_IETF_NPUBBYTES = crypto_aead_chacha20poly1305_ietf_NPUBBYTES;
  private static ILibsodium INSTANCE;

  static {
    INSTANCE = (ILibsodium) Native
        .loadLibrary(JLibrustzcash.getLibraryByName("libsodium"), ILibsodium.class);
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

  public interface ILibsodium extends Library {

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

    class crypto_generichash_blake2b_state extends Structure {

      public long h[] = new long[8];
      public long t[] = new long[2];
      public long f[] = new long[2];
      public byte buf[] = new byte[2 * 128];
      public sizeT buflen = new sizeT();
      public byte last_node;

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
    }
  }
}