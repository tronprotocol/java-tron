package org.tron.common.zksnark.sapling.note;

import static org.tron.common.zksnark.sapling.Libsodium.crypto_aead_chacha20poly1305_IETF_NPUBBYTES;
import static org.tron.common.zksnark.sapling.ZkChainParams.ZC_SAPLING_ENCCIPHERTEXT_SIZE;
import static org.tron.common.zksnark.sapling.ZkChainParams.ZC_SAPLING_ENCPLAINTEXT_SIZE;
import static org.tron.common.zksnark.sapling.ZkChainParams.ZC_SAPLING_OUTCIPHERTEXT_SIZE;
import static org.tron.common.zksnark.sapling.ZkChainParams.ZC_SAPLING_OUTPLAINTEXT_SIZE;

import java.util.Optional;
import org.tron.common.zksnark.sapling.Librustzcash;
import org.tron.common.zksnark.sapling.Libsodium;

public class NoteEncryption {
  public static final int NOTEENCRYPTION_CIPHER_KEYSIZE = 32;

  public static class EncCiphertext {

    public byte[] data; // ZC_SAPLING_ENCCIPHERTEXT_SIZE
  }

  public static class EncPlaintext {

    public byte[] data; // ZC_SAPLING_ENCPLAINTEXT_SIZE
  }

  public static class OutCiphertext {

    public byte[] data; // ZC_SAPLING_OUTCIPHERTEXT_SIZE
  }

  public static class OutPlaintext {

    public byte[] data; // ZC_SAPLING_OUTPLAINTEXT_SIZE
  }

  public static void PRFOck(byte[] K, byte[] ovk, byte[] cv, byte[] cm, byte[] epk) {
    byte[] block = new byte[128];

    System.arraycopy(ovk, 0,  block, 0, 32);
    System.arraycopy(cv, 0,  block, 32, 32);
    System.arraycopy(cm, 0,  block, 64, 32);
    System.arraycopy(epk, 0,  block, 96, 32);

    byte[] personalization = new byte[Libsodium.crypto_generichash_blake2b_PERSONALBYTES];
    byte[] temp = "Zcash_Derive_ock".getBytes();
    System.arraycopy(temp, 0,  personalization, 0, temp.length);
    if (Libsodium.cryptoGenerichashBlack2bSaltPersonal(K, NOTEENCRYPTION_CIPHER_KEYSIZE,
            block, 128,
            null, 0, // No key.
            null,    // No salt.
            personalization
    ) != 0)
    {
      throw new RuntimeException("hash function failure");
    }

    return;
  }

  public static void KDFSapling(byte[] K, byte[] dhsecret, byte[] epk) {
    byte[] block = new byte[64];

    System.arraycopy(dhsecret, 0 ,block, 0, 32);
    System.arraycopy(epk, 0 ,block, 32, 32);

    byte[] personalization = new byte[Libsodium.crypto_generichash_blake2b_PERSONALBYTES];
    byte[] temp =  "Zcash_SaplingKDF".getBytes();
    System.arraycopy(temp, 0, personalization, 0, temp.length);

    if (Libsodium.cryptoGenerichashBlack2bSaltPersonal(K, NOTEENCRYPTION_CIPHER_KEYSIZE,
            block, 64,
            null, 0, // No key.
            null,    // No salt.
            personalization
    ) != 0)
    {
      throw new RuntimeException(("hash function failure"));
    }

    return;
  }


  public static Optional<EncPlaintext> AttemptSaplingEncDecryption(
      byte[] ciphertext, byte[] ivk, byte[] epk) {
    byte[] dhsecret = new byte[32];

    if(!Librustzcash.librustzcashSaplingKaAgree(epk, ivk, dhsecret)) {
      return Optional.empty();
    }

    byte[] K = new byte[NOTEENCRYPTION_CIPHER_KEYSIZE];
    KDFSapling(K, dhsecret, epk);

    byte[] cipher_nonce = new byte[crypto_aead_chacha20poly1305_IETF_NPUBBYTES];

    EncPlaintext plaintext = new EncPlaintext();
    plaintext.data = new byte[ZC_SAPLING_ENCPLAINTEXT_SIZE];

    if (Libsodium.cryptoAeadChacha20poly1305IetfDecrypt(
            plaintext.data, null,
            null,
        ciphertext, ZC_SAPLING_ENCCIPHERTEXT_SIZE,
            null,
            0,
            cipher_nonce, K) != 0)
    {
      return Optional.empty();
    }

    return Optional.of(plaintext);
  }

  public static Optional<EncPlaintext> AttemptSaplingEncDecryption(
      EncCiphertext ciphertext, byte[] epk, byte[] esk, byte[] pk_d) {
    byte[] dhsecret = new byte[32];

    if(!Librustzcash.librustzcashSaplingKaAgree(pk_d, esk, dhsecret)) {
      return Optional.empty();
    }

    byte[] K = new byte[NOTEENCRYPTION_CIPHER_KEYSIZE];
    KDFSapling(K, dhsecret, epk);

    byte[] cipher_nonce = new byte[crypto_aead_chacha20poly1305_IETF_NPUBBYTES];

    EncPlaintext plaintext = new EncPlaintext();
    plaintext.data = new byte[ZC_SAPLING_ENCPLAINTEXT_SIZE];

    if (Libsodium.cryptoAeadChacha20poly1305IetfDecrypt(
            plaintext.data, null,
            null,
            ciphertext.data, ZC_SAPLING_ENCCIPHERTEXT_SIZE,
            null,
            0,
            cipher_nonce, K) != 0)
    {
      return Optional.empty();
    }

    return Optional.of(plaintext);
  }

  public static Optional<OutPlaintext> AttemptSaplingOutDecryption(
      OutCiphertext ciphertext, byte[] ovk, byte[] cv, byte[] cm, byte[] epk) {

    byte[] K = new byte[NOTEENCRYPTION_CIPHER_KEYSIZE];
    PRFOck(K, ovk, cv, cm, epk);

    byte[] cipher_nonce = new byte[crypto_aead_chacha20poly1305_IETF_NPUBBYTES];
    OutPlaintext plaintext = new OutPlaintext();
    plaintext.data = new byte[ZC_SAPLING_OUTPLAINTEXT_SIZE];

    if (Libsodium.cryptoAeadChacha20poly1305IetfDecrypt(plaintext.data, null,
            null,
            ciphertext.data, ZC_SAPLING_OUTCIPHERTEXT_SIZE,
            null,
            0,
            cipher_nonce, K) != 0)
    {
      return Optional.empty();
    }

    return Optional.of(plaintext);
  }

  public static void main(String args[]) {
    byte[] ciphertext = {90, -65, 119, -9, -116, -57, -22, -105, 84, -80, -28, -124, -12, 43, 39,
        -43, 9, -96, 50, 11, 57, -96, -119, 65, 7, -3, -47, -72, -23, 71, -93, -56, 87, -100, -103,
        61, -55, 109, 80, 43, -79, -3, -26, -34, 102, -106, -105, 37, 91, 72, -109, 67, -45, 78,
        -119, 80, -92, 54, 61, -38, 21, 78, 118, 85, -16, 94, 110, -39, 3, -40, 92, -83, -31, -114,
        3, 30, -115, -68, 57, 86, -18, -52, -124, -35, 84, -36, 117, -50, 3, 60, 50, -20, -113, 117,
        74, 107, -69, -91, 81, 105, -108, 85, -62, -93, 65, -20, -14, 32, -86, 27, 90, 84, 92, -104,
        104, 100, -94, -98, -117, -61, 74, -2, -65, -39, -95, -47, -55, 19, -6, 108, 7, -75, 106,
        55, 29, 90, 62, -67, -88, 19, -9, -33, 27, 71, -10, 50, 88, -61, -41, 118, 61, 27, -24, 74,
        35, -79, 17, -39, -95, -1, -37, 104, 46, 66, -17, 90, -74, 108, -51, -4, 13, 105, -116, -26,
        61, 57, 28, -39, -86, 43, -104, -42, -31, -43, -65, -32, 46, 127, 0, 36, 4, 120, -17, 88,
        61, 97, 58, -122, -24, -33, -66, 74, 8, 95, -69, -73, 114, 36, -110, -51, -109, -8, -97, 99,
        93, 23, -72, -92, -27, -55, 52, -12, 107, -69, -80, 16, -97, 49, -19, 65, 60, -6, -112,
        -125, -97, -79, 61, -62, -34, -68, 67, 94, 113, 99, 17, 65, 94, -2, 65, -19, 90, -97, -40,
        36, -72, 12, 20, 12, 127, 68, -9, 1, 37, 50, 120, 67, -5, -39, -85, -82, 63, 97, -36, -31,
        72, 80, -35, 10, -78, 1, 14, 25, 104, -70, -77, -66, -74, 56, 75, -84, 102, -119, 40, 80,
        -31, -16, 25, -25, -9, 120, -88, -17, -48, 94, -116, 19, 55, 21, 118, -27, -63, 88, -118,
        71, 5, -108, -50, -84, 78, -31, -81, -8, -101, 53, 0, 62, -13, -32, -28, -24, 58, -125, 61,
        -21, -45, 72, -51, -98, 3, -10, 61, -60, 83, 57, -41, 5, 105, -15, -125, 115, 123, -24, 91,
        64, -24, -66, 26, 59, 73, 2, 119, 24, -13, 62, -86, -22, -91, -19, -78, -40, 98, 122, 73,
        123, -120, 109, 7, 54, -63, -19, -75, -14, 68, 43, 34, -93, 49, 49, 22, 7, 89, 33, -95, -10,
        49, -97, -86, -72, -81, 111, 82, 11, -118, 39, -21, -20, -12, -44, 77, -30, -105, -5, 107,
        -60, 7, 125, 8, 33, 93, 76, 75, -16, -95, 45, 99, 98, 12, -95, 65, 69, -128, -103, 24, 43,
        -117, -46, -35, 90, 38, -11, -30, 2, -50, -96, -56, 21, 99, -50, 29, 62, -33, -128, -8, -70,
        17, 57, -116, -111, 24, 109, -44, 72, 31, -16, 47, -16, 87, 109, -15, 22, -30, 60, -22, 116,
        68, 72, -70, 32, -73, 75, -8, 29, 38, 68, 103, 83, 6, -47, 83, 55, -19, 96, 99, 126, -33,
        -64, 11, 91, -122, 65, -85, 108, -14, 23, 107, -67, 21, 55, 105, -68, -86, 104, -65, -6, 32,
        15, -72, -101, -36, 66, 67, 104, 36, -117, 19, -93, -95, 57, -43, -102, 10, 71, 101, 85, -6,
        -101, 105, 92, 54, 94, -71, 66, -86, -37, -51, 103, -41, 111, 97, -9, -71, 77, 76, 106, -7,
        72, 36, -91, 5, -125, 26, 115, 35, -102, 83, 100, -50, 35, -103, -84, 51, -116, 119, 22,
        116, -86, -5, 102, -86, 101};


    byte[] ivk = {-73,11,124,-48,-19,3,-53,-33,-41,-83,-87,80,46,-30,69,-79,62,86,-99,84,-91,113,-99,45,-86,15,95,20,81,71,-110,4};
    byte[] epk = {105,-101,72,44,-39,27,-55,-24,-22,106,72,-2,28,-28,102,10,88,16,-40,-16,-78,-95,119,-93,9,-80,71,95,105,-33,21,97};
    Optional<EncPlaintext> ret = AttemptSaplingEncDecryption( ciphertext, ivk,  epk);

    EncPlaintext result = ret.get();

    System.out.println("plain text:");
    for(byte b : result.data) {
      System.out.print(b + ",");
    }
    System.out.println();

  }

}
