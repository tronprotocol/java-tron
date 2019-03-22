package org.tron.common.zksnark.sapling.note;

import org.tron.common.zksnark.sapling.Libsodium;

import java.util.Optional;

public class NoteEncryption {
  public static final int NOTEENCRYPTION_CIPHER_KEYSIZE = 32;
  public static final int crypto_generichash_blake2b_PERSONALBYTES = 32;

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

    byte[] personalization = new byte[crypto_generichash_blake2b_PERSONALBYTES];
    System.arraycopy("Zcash_Derive_ock", 0,  personalization, 0, 16);
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


  // todo:
  public static Optional<EncPlaintext> AttemptSaplingEncDecryption(
      EncCiphertext ciphertext, byte[] ivk, byte[] epk) {
    return null;
  }

  public static Optional<EncPlaintext> AttemptSaplingEncDecryption(
      EncCiphertext ciphertext, byte[] epk, byte[] esk, byte[] pk_d) {
    return null;
  }

  public static Optional<OutPlaintext> AttemptSaplingOutDecryption(
      OutCiphertext ciphertext, byte[] ovk, byte[] cv, byte[] cm, byte[] epk) {

    byte[] K = new byte[NOTEENCRYPTION_CIPHER_KEYSIZE];
    PRFOck(K, ovk, cv, cm, epk);

    return null;
  }
}
