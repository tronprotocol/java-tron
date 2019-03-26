package org.tron.common.zksnark.sapling.note;

import org.tron.common.zksnark.sapling.Librustzcash;
import org.tron.common.zksnark.sapling.Libsodium;

import java.util.Optional;

import static org.tron.common.zksnark.sapling.Libsodium.crypto_aead_chacha20poly1305_IETF_NPUBBYTES;
import static org.tron.common.zksnark.sapling.ZkChainParams.*;

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
    System.arraycopy(dhsecret, 0 ,block, 32, 32);

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
      EncCiphertext ciphertext, byte[] ivk, byte[] epk) {
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
            ciphertext.data, ZC_SAPLING_ENCCIPHERTEXT_SIZE,
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
    return null;
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

}
