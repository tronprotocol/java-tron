package org.tron.core.zen.note;

import static org.tron.common.zksnark.JLibsodium.crypto_aead_chacha20poly1305_IETF_NPUBBYTES;
import static org.tron.core.zen.note.NoteEncryption.Encryption.NOTEENCRYPTION_CIPHER_KEYSIZE;
import static org.tron.core.zen.note.ZenChainParams.ZC_ENCCIPHERTEXT_SIZE;
import static org.tron.core.zen.note.ZenChainParams.ZC_ENCPLAINTEXT_SIZE;
import static org.tron.core.zen.note.ZenChainParams.ZC_OUTCIPHERTEXT_SIZE;
import static org.tron.core.zen.note.ZenChainParams.ZC_OUTPLAINTEXT_SIZE;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.LibrustzcashParam.KaAgreeParams;
import org.tron.common.zksnark.LibrustzcashParam.KaDerivepublicParams;
import org.tron.common.zksnark.JLibsodium;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.note.NoteEncryption.Encryption.EncCiphertext;
import org.tron.core.zen.note.NoteEncryption.Encryption.EncPlaintext;
import org.tron.core.zen.note.NoteEncryption.Encryption.OutCiphertext;
import org.tron.core.zen.note.NoteEncryption.Encryption.OutPlaintext;

@AllArgsConstructor
public class NoteEncryption {

  // Ephemeral public key
  public byte[] epk;
  // Ephemeral secret key
  public byte[] esk;

  public boolean alreadyEncryptedEnc;
  public boolean alreadyEncryptedOut;

  public NoteEncryption(byte[] epk, byte[] esk) {
    this.epk = epk;
    this.esk = esk;
  }
  
  /**
   * generate pair of (esk,epk). epk = esk * d
   * @param d
   * @return
   * @throws ZksnarkException
   */
  public static Optional<NoteEncryption> fromDiversifier(DiversifierT d) throws ZksnarkException {
    byte[] epk = new byte[32];
    byte[] esk = new byte[32];
    JLibrustzcash.librustzcashSaplingGenerateR(esk);
    if (!JLibrustzcash
        .librustzcashSaplingKaDerivepublic(new KaDerivepublicParams(d.data, esk, epk))) {
      return Optional.empty();
    }
    return Optional.of(new NoteEncryption(epk, esk));
  }
  
  /**
   * encrypt plain_enc by kEnc to cEnc with sharedsecret and epk, use this esk,epk
   * kEnc can use in encrypt also in decrypt，symmetric encryption.
   * @param pkD
   * @param message
   * @return
   * @throws ZksnarkException
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
    Encryption.KDFSapling(kEnc, dhsecret, epk);
    byte[] cipherNonce = new byte[crypto_aead_chacha20poly1305_IETF_NPUBBYTES];
    EncCiphertext ciphertext = new EncCiphertext();
    JLibsodium.cryptoAeadChacha20Poly1305IetfEncrypt(ciphertext.data, null, message.data,
        ZenChainParams.ZC_ENCPLAINTEXT_SIZE, null, 0, null, cipherNonce, kEnc);
    alreadyEncryptedEnc = true;
    return Optional.of(ciphertext);
  }
  
  /**
   * encrypt plain_out with ock to c_out, use this epk
   * @param ovk
   * @param cv
   * @param cm
   * @param message
   * @return
   * @throws ZksnarkException
   */
  public OutCiphertext encryptToOurselves(
      byte[] ovk, byte[] cv, byte[] cm, OutPlaintext message) throws ZksnarkException {
    if (alreadyEncryptedOut) {
      throw new ZksnarkException("already encrypted to the recipient using this key");
    }

    byte[] ock = new byte[NOTEENCRYPTION_CIPHER_KEYSIZE];
    Encryption.PRFOck(ock, ovk, cv, cm, epk);

    byte[] cipherNonce = new byte[crypto_aead_chacha20poly1305_IETF_NPUBBYTES];
    OutCiphertext ciphertext = new OutCiphertext();
    JLibsodium.cryptoAeadChacha20Poly1305IetfEncrypt(ciphertext.data, null, message.data,
        ZenChainParams.ZC_OUTPLAINTEXT_SIZE, null, 0, null, cipherNonce, ock);
    alreadyEncryptedOut = true;
    return ciphertext;
  }

  public static class Encryption {

    public static final int NOTEENCRYPTION_CIPHER_KEYSIZE = 32;
  
    /**
     * generate ock by ovk, cv, cm, epk
     * @param ock
     * @param ovk
     * @param cv
     * @param cm
     * @param epk
     * @throws ZksnarkException
     */
    public static void PRFOck(byte[] ock, byte[] ovk, byte[] cv, byte[] cm, byte[] epk)
        throws ZksnarkException {
      byte[] block = new byte[128];
      System.arraycopy(ovk, 0, block, 0, 32);
      System.arraycopy(cv, 0, block, 32, 32);
      System.arraycopy(cm, 0, block, 64, 32);
      System.arraycopy(epk, 0, block, 96, 32);

      byte[] personalization = new byte[JLibsodium.crypto_generichash_blake2b_PERSONALBYTES];
      byte[] temp = "Ztron_Derive_ock".getBytes();
      System.arraycopy(temp, 0, personalization, 0, temp.length);
      if (JLibsodium.cryptoGenerichashBlack2bSaltPersonal(ock, NOTEENCRYPTION_CIPHER_KEYSIZE,
          block, 128,
          null, 0, // No key.
          null,    // No salt.
          personalization
      ) != 0) {
        throw new ZksnarkException("hash function failure");
      }

      return;
    }
  
    /**
     * generate kEnc by sharedsecret and epk
     * @param kEnc
     * @param sharedsecret
     * @param epk
     * @throws ZksnarkException
     */
    public static void KDFSapling(byte[] kEnc, byte[] sharedsecret, byte[] epk) throws ZksnarkException {
      byte[] block = new byte[64];
      System.arraycopy(sharedsecret, 0, block, 0, 32);
      System.arraycopy(epk, 0, block, 32, 32);
      byte[] personalization = new byte[JLibsodium.crypto_generichash_blake2b_PERSONALBYTES];
      byte[] temp = "Ztron_SaplingKDF".getBytes();
      System.arraycopy(temp, 0, personalization, 0, temp.length);
      if (JLibsodium.cryptoGenerichashBlack2bSaltPersonal(kEnc, NOTEENCRYPTION_CIPHER_KEYSIZE,
          block, 64,
          null, 0, // No key.
          null,    // No salt.
          personalization
      ) != 0) {
        throw new ZksnarkException(("hash function failure"));
      }

      return;
    }
  
    /**
     * decrypt cEnc by kEnc to plain_enc generate with epk + ivk
     * kEnc can use in encrypt also in decrypt，symmetric encryption.
     * @param ciphertext
     * @param ivk
     * @param epk
     * @return
     * @throws ZksnarkException
     */
    public static Optional<EncPlaintext> AttemptEncDecryption(
        byte[] ciphertext, byte[] ivk, byte[] epk) throws ZksnarkException {
      byte[] sharedsecret = new byte[32];
      //generate sharedsecret by epk and ivk
      if (!JLibrustzcash.librustzcashKaAgree(new KaAgreeParams(epk, ivk, sharedsecret))) {
        return Optional.empty();
      }
      byte[] kEnc = new byte[NOTEENCRYPTION_CIPHER_KEYSIZE];
      //generate kEnc by sharedsecret and epk
      KDFSapling(kEnc, sharedsecret, epk);
      byte[] cipher_nonce = new byte[crypto_aead_chacha20poly1305_IETF_NPUBBYTES];
      EncPlaintext plaintext = new EncPlaintext();
      plaintext.data = new byte[ZC_ENCPLAINTEXT_SIZE];
      //decrypt cEnc by kEnc
      if (JLibsodium.cryptoAeadChacha20poly1305IetfDecrypt(
          plaintext.data, null,
          null,
          ciphertext, ZC_ENCCIPHERTEXT_SIZE,
          null,
          0,
          cipher_nonce, kEnc) != 0) {
        return Optional.empty();
      }
      return Optional.of(plaintext);
    }
  
    /**
     * decrypt cEnc by kEnc to plain_enc generate with esk + pkD
     * kEnc can use in encrypt also in decrypt，symmetric encryption.
     * @param ciphertext
     * @param epk
     * @param esk
     * @param pkD
     * @return
     * @throws ZksnarkException
     */
    public static Optional<EncPlaintext> AttemptEncDecryption(
        EncCiphertext ciphertext, byte[] epk, byte[] esk, byte[] pkD) throws ZksnarkException {
      byte[] sharedsecret = new byte[32];
      //generate sharedsecret by esk and pkD. esk + pkD = sharedsecret = epk + ivk
      if (!JLibrustzcash.librustzcashKaAgree(new KaAgreeParams(pkD, esk, sharedsecret))) {
        return Optional.empty();
      }
      byte[] kEnc = new byte[NOTEENCRYPTION_CIPHER_KEYSIZE];
      //generate kEnc by sharedsecret and epk
      KDFSapling(kEnc, sharedsecret, epk);
      byte[] cipherNonce = new byte[crypto_aead_chacha20poly1305_IETF_NPUBBYTES];
      EncPlaintext plaintext = new EncPlaintext();
      plaintext.data = new byte[ZC_ENCPLAINTEXT_SIZE];
      //decrypt cEnc by kEnc.
      if (JLibsodium.cryptoAeadChacha20poly1305IetfDecrypt(
          plaintext.data, null,
          null,
          ciphertext.data, ZC_ENCCIPHERTEXT_SIZE,
          null,
          0,
          cipherNonce, kEnc) != 0) {
        return Optional.empty();
      }

      return Optional.of(plaintext);
    }
  
    /**
     * decrypt c_out to plain_out with ock generate ovk
     * @param ciphertext
     * @param ovk
     * @param cv
     * @param cm
     * @param epk
     * @return
     * @throws ZksnarkException
     */
    public static Optional<OutPlaintext> AttemptOutDecryption(
        OutCiphertext ciphertext, byte[] ovk, byte[] cv, byte[] cm, byte[] epk)
        throws ZksnarkException {
      byte[] ock = new byte[NOTEENCRYPTION_CIPHER_KEYSIZE];
      //generate ock by ovk, cv, cm, epk
      PRFOck(ock, ovk, cv, cm, epk);
      byte[] cipherNonce = new byte[crypto_aead_chacha20poly1305_IETF_NPUBBYTES];
      OutPlaintext plaintext = new OutPlaintext();
      plaintext.data = new byte[ZC_OUTPLAINTEXT_SIZE];
      //decrypt out by ock, get esk, pkD
      if (JLibsodium.cryptoAeadChacha20poly1305IetfDecrypt(plaintext.data, null,
          null,
          ciphertext.data, ZC_OUTCIPHERTEXT_SIZE,
          null,
          0,
          cipherNonce, ock) != 0) {
        return Optional.empty();
      }
      return Optional.of(plaintext);
    }

    public static class EncCiphertext {

      public byte[] data = new byte[ZC_ENCCIPHERTEXT_SIZE]; // ZC_ENCCIPHERTEXT_SIZE
    }

    public static class EncPlaintext {

      public byte[] data = new byte[ZC_ENCPLAINTEXT_SIZE]; // ZC_ENCPLAINTEXT_SIZE
    }

    public static class OutCiphertext {

      public byte[] data = new byte[ZC_OUTCIPHERTEXT_SIZE]; // ZC_OUTCIPHERTEXT_SIZE
    }

    public static class OutPlaintext {

      public byte[] data = new byte[ZC_OUTPLAINTEXT_SIZE]; // ZC_OUTPLAINTEXT_SIZE
    }
  }
}
