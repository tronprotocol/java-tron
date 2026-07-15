package org.tron.core.zen.note;

import static org.tron.common.zksnark.JLibsodium.CRYPTO_AEAD_CHACHA20POLY1305_IETF_NPUBBYTES;
import static org.tron.core.utils.ZenChainParams.ZC_ENCCIPHERTEXT_SIZE;
import static org.tron.core.utils.ZenChainParams.ZC_ENCPLAINTEXT_SIZE;
import static org.tron.core.utils.ZenChainParams.ZC_OUTCIPHERTEXT_SIZE;
import static org.tron.core.utils.ZenChainParams.ZC_OUTPLAINTEXT_SIZE;
import static org.tron.core.zen.note.NoteEncryption.Encryption.NOTEENCRYPTION_CIPHER_KEYSIZE;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.crypto.Hash;
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
    public static final int BURN_CIPHER_LEN = 80;
    public static final int BURN_NONCE_LEN = 12;
    public static final int BURN_RESERVED_LEN = 4;
    public static final int BURN_CIPHER_RECORD_SIZE = 96;
    public static final int BURN_NONCE_OFFSET = BURN_CIPHER_LEN;
    public static final int BURN_RESERVED_OFFSET = BURN_NONCE_OFFSET + BURN_NONCE_LEN;
    private static final byte[] BURN_RECORD_V2_MARKER = new byte[]{0, 0, 0, 1};
    private static final byte[] BURN_NONCE_DOMAIN =
        "Ztron_BurnNonce".getBytes(StandardCharsets.UTF_8);

    public static byte[] getBurnRecordV2Marker() {
      return BURN_RECORD_V2_MARKER.clone();
    }

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
     * encrypt burn message with nonce bound to (nf, amount, addr21), returns a 96B record:
     * cipher(80) + nonce(12) + reserved/version(4).
     */
    public static Optional<byte[]> encryptBurnMessageByOvk(byte[] ovk, BigInteger toAmount,
        byte[] transparentToAddress, byte[] nf)
        throws ZksnarkException {
      if (ovk == null || ovk.length != NOTEENCRYPTION_CIPHER_KEYSIZE) {
        throw new ZksnarkException("invalid ovk length");
      }
      if (transparentToAddress == null || transparentToAddress.length != 21) {
        throw new ZksnarkException("invalid transparentToAddress length");
      }
      if (nf == null || nf.length != 32) {
        throw new ZksnarkException("invalid nullifier length");
      }
      byte[] plaintext = new byte[64];
      byte[] amountArray = ByteUtil.bigIntegerToBytes(toAmount, 32);
      byte[] nonce = deriveBurnNonce(nf, amountArray, transparentToAddress);
      byte[] cipher = new byte[BURN_CIPHER_LEN];
      System.arraycopy(amountArray, 0, plaintext, 0, 32);
      System.arraycopy(transparentToAddress, 0, plaintext, 32, 21);

      if (JLibsodium.cryptoAeadChacha20Poly1305IetfEncrypt(new Chacha20Poly1305IetfEncryptParams(
          cipher, null, plaintext,
          64, null, 0, null, nonce, ovk)) != 0) {
        return Optional.empty();
      }

      byte[] record = new byte[BURN_CIPHER_RECORD_SIZE];
      System.arraycopy(cipher, 0, record, 0, BURN_CIPHER_LEN);
      System.arraycopy(nonce, 0, record, BURN_NONCE_OFFSET, BURN_NONCE_LEN);
      System.arraycopy(BURN_RECORD_V2_MARKER, 0, record, BURN_RESERVED_OFFSET, BURN_RESERVED_LEN);
      return Optional.of(record);
    }

    /**
     * Derive a 12-byte ChaCha20-Poly1305 nonce from (nf, amount, addr21).
     * Binding the plaintext fields ensures that repeated encryption with the same nf
     * but different amount/addr produces distinct nonces, preserving AEAD nonce
     * uniqueness even when the same input note is used to generate multiple burn
     * trigger inputs off-chain.
     */
    public static byte[] deriveBurnNonce(byte[] nf, byte[] amount32, byte[] addr21) {
      if (nf == null || nf.length != 32) {
        throw new IllegalArgumentException("invalid nullifier length");
      }
      if (amount32 == null || amount32.length != 32) {
        throw new IllegalArgumentException("invalid amount length");
      }
      if (addr21 == null || addr21.length != 21) {
        throw new IllegalArgumentException("invalid addr21 length");
      }
      byte[] tagged = new byte[BURN_NONCE_DOMAIN.length + nf.length + amount32.length
          + addr21.length];
      int off = 0;
      System.arraycopy(BURN_NONCE_DOMAIN, 0, tagged, off, BURN_NONCE_DOMAIN.length);
      off += BURN_NONCE_DOMAIN.length;
      System.arraycopy(nf, 0, tagged, off, nf.length);
      off += nf.length;
      System.arraycopy(amount32, 0, tagged, off, amount32.length);
      off += amount32.length;
      System.arraycopy(addr21, 0, tagged, off, addr21.length);
      byte[] hash = Hash.sha3(tagged);
      byte[] nonce = new byte[BURN_NONCE_LEN];
      System.arraycopy(hash, 0, nonce, 0, BURN_NONCE_LEN);
      return nonce;
    }

    /**
     * decrypt burn message. The trailing 4-byte reserved field is treated as an explicit
     * record-version marker:
     * - reserved = 0x00000000 and nonce = 0x000000000000000000000000 -> legacy v1 path.
     * - reserved = 0x00000001 -> v2 path; nonce must equal
     *   deriveBurnNonce(nf, amount32, addr21) using the public log fields.
     * - any other reserved value -> reject.
     */
    public static Optional<byte[]> decryptBurnMessageByOvk(byte[] ovk, byte[] ciphertext,
        byte[] nonceFromLog, byte[] reservedFromLog, byte[] nf, byte[] amount32, byte[] addr21)
        throws ZksnarkException {
      if (ovk == null || ovk.length != NOTEENCRYPTION_CIPHER_KEYSIZE) {
        throw new ZksnarkException("invalid ovk length");
      }
      if (ciphertext == null || ciphertext.length != BURN_CIPHER_LEN
          || nonceFromLog == null || nonceFromLog.length != BURN_NONCE_LEN
          || reservedFromLog == null || reservedFromLog.length != BURN_RESERVED_LEN) {
        return Optional.empty();
      }

      byte[] effectiveNonce;
      if (isAllZero(reservedFromLog)) {
        if (!isAllZero(nonceFromLog)) {
          return Optional.empty();
        }
        effectiveNonce = nonceFromLog;
      } else if (Arrays.equals(reservedFromLog, BURN_RECORD_V2_MARKER)) {
        if (nf == null || nf.length != 32
            || amount32 == null || amount32.length != 32
            || addr21 == null || addr21.length != 21) {
          return Optional.empty();
        }
        byte[] derived = deriveBurnNonce(nf, amount32, addr21);
        if (!Arrays.equals(nonceFromLog, derived)) {
          return Optional.empty();
        }
        effectiveNonce = nonceFromLog;
      } else {
        return Optional.empty();
      }

      byte[] outPlaintext = new byte[64];
      if (JLibsodium.cryptoAeadChacha20poly1305IetfDecrypt(new Chacha20poly1305IetfDecryptParams(
          outPlaintext, null,
          null,
          ciphertext, BURN_CIPHER_LEN,
          null,
          0,
          effectiveNonce, ovk)) != 0) {
        return Optional.empty();
      }
      return Optional.of(outPlaintext);
    }

    private static boolean isAllZero(byte[] data) {
      for (byte b : data) {
        if (b != 0) {
          return false;
        }
      }
      return true;
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
