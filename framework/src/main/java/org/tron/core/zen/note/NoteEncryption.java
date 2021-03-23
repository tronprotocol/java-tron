package org.tron.core.zen.note;

import static org.tron.common.zksnark.JLibsodium.CRYPTO_AEAD_CHACHA20POLY1305_IETF_NPUBBYTES;
import static org.tron.core.utils.ZenChainParams.ZC_ENCCIPHERTEXT_SIZE;
import static org.tron.core.utils.ZenChainParams.ZC_ENCPLAINTEXT_SIZE;
import static org.tron.core.utils.ZenChainParams.ZC_OUTCIPHERTEXT_SIZE;
import static org.tron.core.utils.ZenChainParams.ZC_OUTPLAINTEXT_SIZE;
import static org.tron.core.zen.note.NoteEncryption.Encryption.NOTEENCRYPTION_CIPHER_KEYSIZE;

import java.math.BigInteger;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.utils.ByteUtil;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.JLibsodium;
import org.tron.common.zksnark.JLibsodiumParam.Black2bSaltPersonalParams;
import org.tron.common.zksnark.JLibsodiumParam.Chacha20Poly1305IetfEncryptParams;
import org.tron.common.zksnark.JLibsodiumParam.Chacha20poly1305IetfDecryptParams;
import org.tron.common.zksnark.LibrustzcashParam.KaAgreeParams;
import org.tron.common.zksnark.LibrustzcashParam.KaDerivepublicParams;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.utils.ZenChainParams;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.note.NoteEncryption.Encryption.EncCiphertext;
import org.tron.core.zen.note.NoteEncryption.Encryption.EncPlaintext;
import org.tron.core.zen.note.NoteEncryption.Encryption.OutCiphertext;
import org.tron.core.zen.note.NoteEncryption.Encryption.OutPlaintext;

@AllArgsConstructor
public class NoteEncryption {

  // Ephemeral public key
  @Getter
  private byte[] epk;
  // Ephemeral secret key
  @Getter
  private byte[] esk;

  private boolean alreadyEncryptedEnc;
  private boolean alreadyEncryptedOut;

  public NoteEncryption(byte[] epk, byte[] esk) {
    this.epk = epk;
    this.esk = esk;
  }

  /**
   * generate pair of (esk,epk). epk = esk * d
   */
  public static Optional<NoteEncryption> fromDiversifier(DiversifierT d) throws ZksnarkException {
    byte[] epk = new byte[32];
    byte[] esk = new byte[32];
    JLibrustzcash.librustzcashSaplingGenerateR(esk);
    if (!JLibrustzcash
        .librustzcashSaplingKaDerivepublic(new KaDerivepublicParams(d.getData(), esk, epk))) {
      return Optional.empty();
    }
    return Optional.of(new NoteEncryption(epk, esk));
  }

  /**
   * encrypt plain_enc by kEnc to cEnc with sharedsecret and epk, use this esk,epk kEnc can use in
   * encrypt also in decrypt，symmetric encryption.
   */
  public Optional<EncCiphertext> encryptToRecipient(byte[] pkD, EncPlaintext message)
      throws ZksnarkException {
    if (alreadyEncryptedEnc) {
      throw new ZksnarkException("already encrypted to the recipient using this key");
    }

    byte[] dhsecret = new byte[32];
    if (!JLibrustzcash.librustzcashKaAgree(new KaAgreeParams(pkD, esk, dhsecret))) {
      return Optional.empty();
    }

    byte[] kEnc = new byte[NOTEENCRYPTION_CIPHER_KEYSIZE];
    //generate kEnc by sharedsecret and epk
    Encryption.kdfSapling(kEnc, dhsecret, epk);
    byte[] cipherNonce = new byte[CRYPTO_AEAD_CHACHA20POLY1305_IETF_NPUBBYTES];
    EncCiphertext ciphertext = new EncCiphertext();
    JLibsodium.cryptoAeadChacha20Poly1305IetfEncrypt(new Chacha20Poly1305IetfEncryptParams(
        ciphertext.data, null, message.data,
        ZenChainParams.ZC_ENCPLAINTEXT_SIZE, null, 0, null, cipherNonce, kEnc));
    alreadyEncryptedEnc = true;
    return Optional.of(ciphertext);
  }

  /**
   * encrypt plain_out with ock to c_out, use this epk
   */
  public OutCiphertext encryptToOurselves(
      byte[] ovk, byte[] cv, byte[] cm, OutPlaintext message) throws ZksnarkException {
    if (alreadyEncryptedOut) {
      throw new ZksnarkException("already encrypted to the recipient using this key");
    }

    byte[] ock = new byte[NOTEENCRYPTION_CIPHER_KEYSIZE];
    Encryption.prfOck(ock, ovk, cv, cm, epk);

    byte[] cipherNonce = new byte[CRYPTO_AEAD_CHACHA20POLY1305_IETF_NPUBBYTES];
    OutCiphertext ciphertext = new OutCiphertext();
    JLibsodium.cryptoAeadChacha20Poly1305IetfEncrypt(new Chacha20Poly1305IetfEncryptParams(
        ciphertext.data, null, message.data,
        ZenChainParams.ZC_OUTPLAINTEXT_SIZE, null, 0, null, cipherNonce, ock));
    alreadyEncryptedOut = true;
    return ciphertext;
  }

  public static class Encryption {

    public static final int NOTEENCRYPTION_CIPHER_KEYSIZE = 32;

    /**
     * generate ock by ovk, cv, cm, epk
     */
    public static void prfOck(byte[] ock, byte[] ovk, byte[] cv, byte[] cm, byte[] epk)
        throws ZksnarkException {
      byte[] block = new byte[128];
      System.arraycopy(ovk, 0, block, 0, 32);
      System.arraycopy(cv, 0, block, 32, 32);
      System.arraycopy(cm, 0, block, 64, 32);
      System.arraycopy(epk, 0, block, 96, 32);

      byte[] personalization = new byte[JLibsodium.CRYPTO_GENERICHASH_BLAKE2B_PERSONALBYTES];
      byte[] temp = "Ztron_Derive_ock".getBytes();
      System.arraycopy(temp, 0, personalization, 0, temp.length);
      if (JLibsodium.cryptoGenerichashBlack2bSaltPersonal(new Black2bSaltPersonalParams(
          ock, NOTEENCRYPTION_CIPHER_KEYSIZE,
          block, 128,
          null, 0, // No key.
          null,    // No salt.
          personalization)
      ) != 0) {
        throw new ZksnarkException("hash function failure");
      }
    }

    /**
     * generate kEnc by sharedsecret and epk
     */
    public static void kdfSapling(byte[] kEnc, byte[] sharedsecret, byte[] epk)
        throws ZksnarkException {
      byte[] block = new byte[64];
      System.arraycopy(sharedsecret, 0, block, 0, 32);
      System.arraycopy(epk, 0, block, 32, 32);
      byte[] personalization = new byte[JLibsodium.CRYPTO_GENERICHASH_BLAKE2B_PERSONALBYTES];
      byte[] temp = "Ztron_SaplingKDF".getBytes();
      System.arraycopy(temp, 0, personalization, 0, temp.length);
      if (JLibsodium.cryptoGenerichashBlack2bSaltPersonal(new Black2bSaltPersonalParams(
          kEnc, NOTEENCRYPTION_CIPHER_KEYSIZE,
          block, 64,
          null, 0, // No key.
          null,    // No salt.
          personalization)
      ) != 0) {
        throw new ZksnarkException(("hash function failure"));
      }
    }

    /**
     * decrypt cEnc by kEnc to plain_enc generate with epk + ivk kEnc can use in encrypt also in
     * decrypt，symmetric encryption.
     */
    public static Optional<EncPlaintext> attemptEncDecryption(
        byte[] ciphertext, byte[] ivk, byte[] epk) throws ZksnarkException {
      byte[] sharedsecret = new byte[32];
      //generate sharedsecret by epk and ivk
      if (!JLibrustzcash.librustzcashKaAgree(new KaAgreeParams(epk, ivk, sharedsecret))) {
        return Optional.empty();
      }
      byte[] kEnc = new byte[NOTEENCRYPTION_CIPHER_KEYSIZE];
      //generate kEnc by sharedsecret and epk
      kdfSapling(kEnc, sharedsecret, epk);
      byte[] cipher_nonce = new byte[CRYPTO_AEAD_CHACHA20POLY1305_IETF_NPUBBYTES];
      EncPlaintext plaintext = new EncPlaintext();
      plaintext.data = new byte[ZC_ENCPLAINTEXT_SIZE];
      //decrypt cEnc by kEnc
      if (JLibsodium.cryptoAeadChacha20poly1305IetfDecrypt(new Chacha20poly1305IetfDecryptParams(
          plaintext.data, null,
          null,
          ciphertext, ZC_ENCCIPHERTEXT_SIZE,
          null,
          0,
          cipher_nonce, kEnc)) != 0) {
        return Optional.empty();
      }
      return Optional.of(plaintext);
    }

    /**
     * decrypt cEnc by kEnc to plain_enc generate with esk + pkD kEnc can use in encrypt also in
     * decrypt，symmetric encryption.
     */
    public static Optional<EncPlaintext> attemptEncDecryption(
        EncCiphertext ciphertext, byte[] epk, byte[] esk, byte[] pkD) throws ZksnarkException {
      byte[] sharedsecret = new byte[32];
      //generate sharedsecret by esk and pkD. esk + pkD = sharedsecret = epk + ivk
      if (!JLibrustzcash.librustzcashKaAgree(new KaAgreeParams(pkD, esk, sharedsecret))) {
        return Optional.empty();
      }
      byte[] kEnc = new byte[NOTEENCRYPTION_CIPHER_KEYSIZE];
      //generate kEnc by sharedsecret and epk
      kdfSapling(kEnc, sharedsecret, epk);
      byte[] cipherNonce = new byte[CRYPTO_AEAD_CHACHA20POLY1305_IETF_NPUBBYTES];
      EncPlaintext plaintext = new EncPlaintext();
      plaintext.data = new byte[ZC_ENCPLAINTEXT_SIZE];
      //decrypt cEnc by kEnc.
      if (JLibsodium.cryptoAeadChacha20poly1305IetfDecrypt(new Chacha20poly1305IetfDecryptParams(
          plaintext.data, null,
          null,
          ciphertext.data, ZC_ENCCIPHERTEXT_SIZE,
          null,
          0,
          cipherNonce, kEnc)) != 0) {
        return Optional.empty();
      }

      return Optional.of(plaintext);
    }

    /**
     * decrypt c_out to plain_out with ock generate ovk
     */
    public static Optional<OutPlaintext> attemptOutDecryption(
        OutCiphertext ciphertext, byte[] ovk, byte[] cv, byte[] cm, byte[] epk)
        throws ZksnarkException {
      byte[] ock = new byte[NOTEENCRYPTION_CIPHER_KEYSIZE];
      //generate ock by ovk, cv, cm, epk
      prfOck(ock, ovk, cv, cm, epk);
      byte[] cipherNonce = new byte[CRYPTO_AEAD_CHACHA20POLY1305_IETF_NPUBBYTES];
      OutPlaintext plaintext = new OutPlaintext();
      plaintext.data = new byte[ZC_OUTPLAINTEXT_SIZE];
      //decrypt out by ock, get esk, pkD
      if (JLibsodium.cryptoAeadChacha20poly1305IetfDecrypt(new Chacha20poly1305IetfDecryptParams(
          plaintext.data, null,
          null,
          ciphertext.data, ZC_OUTCIPHERTEXT_SIZE,
          null,
          0,
          cipherNonce, ock)) != 0) {
        return Optional.empty();
      }
      return Optional.of(plaintext);
    }

    /**
     * encrypt the message by ovk used for scanning
     */
    public static Optional<byte[]> encryptBurnMessageByOvk(byte[] ovk, BigInteger toAmount,
        byte[] transparentToAddress)
        throws ZksnarkException {
      byte[] plaintext = new byte[64];
      byte[] amountArray = ByteUtil.bigIntegerToBytes(toAmount, 32);
      byte[] cipherNonce = new byte[12];
      byte[] cipher = new byte[80];
      System.arraycopy(amountArray, 0, plaintext, 0, 32);
      System.arraycopy(transparentToAddress, 0, plaintext, 32,
          21);

      if (JLibsodium.cryptoAeadChacha20Poly1305IetfEncrypt(new Chacha20Poly1305IetfEncryptParams(
          cipher, null, plaintext,
          64, null, 0, null, cipherNonce, ovk)) != 0) {
        return Optional.empty();
      }

      return Optional.of(cipher);
    }

    /**
     * decrypt the message by ovk used for scanning
     */
    public static Optional<byte[]> decryptBurnMessageByOvk(byte[] ovk, byte[] ciphertext)
        throws ZksnarkException {
      byte[] outPlaintext = new byte[64];
      byte[] cipherNonce = new byte[12];
      if (JLibsodium.cryptoAeadChacha20poly1305IetfDecrypt(new Chacha20poly1305IetfDecryptParams(
          outPlaintext, null,
          null,
          ciphertext, 80,
          null,
          0,
          cipherNonce, ovk)) != 0) {
        return Optional.empty();
      }
      return Optional.of(outPlaintext);
    }

    public static class EncCiphertext {

      @Getter
      @Setter
      private byte[] data = new byte[ZC_ENCCIPHERTEXT_SIZE]; // ZC_ENCCIPHERTEXT_SIZE
    }

    public static class EncPlaintext {

      @Getter
      @Setter
      private byte[] data = new byte[ZC_ENCPLAINTEXT_SIZE]; // ZC_ENCPLAINTEXT_SIZE
    }

    public static class OutCiphertext {

      @Getter
      @Setter
      private byte[] data = new byte[ZC_OUTCIPHERTEXT_SIZE]; // ZC_OUTCIPHERTEXT_SIZE
    }

    public static class OutPlaintext {

      @Getter
      @Setter
      private byte[] data = new byte[ZC_OUTPLAINTEXT_SIZE]; // ZC_OUTPLAINTEXT_SIZE
    }
  }
}
