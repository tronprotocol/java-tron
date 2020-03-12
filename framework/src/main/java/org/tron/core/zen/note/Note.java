package org.tron.core.zen.note;

import static org.tron.core.utils.ZenChainParams.ZC_DIVERSIFIER_SIZE;
import static org.tron.core.utils.ZenChainParams.ZC_ENCPLAINTEXT_SIZE;
import static org.tron.core.utils.ZenChainParams.ZC_MEMO_SIZE;
import static org.tron.core.utils.ZenChainParams.ZC_NOTEPLAINTEXT_LEADING;
import static org.tron.core.utils.ZenChainParams.ZC_R_SIZE;
import static org.tron.core.utils.ZenChainParams.ZC_V_SIZE;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.LibrustzcashParam.ComputeCmParams;
import org.tron.common.zksnark.LibrustzcashParam.ComputeNfParams;
import org.tron.common.zksnark.LibrustzcashParam.IvkToPkdParams;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.address.FullViewingKey;
import org.tron.core.zen.address.IncomingViewingKey;
import org.tron.core.zen.address.PaymentAddress;


public class Note {

  @Getter
  @Setter
  private DiversifierT d;
  @Getter
  @Setter
  private byte[] pkD; // 256
  @Getter
  @Setter
  private long value = 0;
  @Getter
  @Setter
  private byte[] rcm; // 256
  @Getter
  private byte[] memo = new byte[ZC_MEMO_SIZE];

  public Note() {
    d = new DiversifierT();
    rcm = new byte[ZC_R_SIZE];
  }

  public Note(PaymentAddress address, long value) throws ZksnarkException {
    this.value = value;
    this.d = address.getD();
    this.pkD = address.getPkD();
    rcm = new byte[32];
    JLibrustzcash.librustzcashSaplingGenerateR(rcm);
  }

  public Note(DiversifierT d, byte[] pkD, long value, byte[] r) {
    this.d = d;
    this.pkD = pkD;
    this.value = value;
    this.rcm = r;
  }

  public Note(DiversifierT d, byte[] pkD, long value, byte[] r, byte[] memo) {
    this.d = d;
    this.pkD = pkD;
    this.value = value;
    this.rcm = r;
    this.setMemo(memo);
  }

  public static byte[] generateR() throws ZksnarkException {
    byte[] r = new byte[32];
    JLibrustzcash.librustzcashSaplingGenerateR(r);
    return r;
  }

  /**
   * decode plain_enc to note
   */
  public static Note decode(NoteEncryption.Encryption.EncPlaintext encPlaintext)
      throws ZksnarkException {
    byte[] data = encPlaintext.getData();

    ByteBuffer buffer = ByteBuffer.allocate(ZC_V_SIZE);
    if (encPlaintext.getData()[0] != 0x01) {
      throw new ZksnarkException("lead byte of NotePlaintext is not recognized");
    }

    Note ret = new Note();
    byte[] noteD = new byte[ZC_DIVERSIFIER_SIZE];
    System.arraycopy(data, ZC_NOTEPLAINTEXT_LEADING, noteD, 0, ZC_DIVERSIFIER_SIZE);
    ret.d.setData(noteD);

    byte[] valueLong = new byte[ZC_V_SIZE];
    System.arraycopy(data, ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE, valueLong, 0, ZC_V_SIZE);
    for (int i = 0; i < valueLong.length / 2; i++) {
      byte temp = valueLong[i];
      valueLong[i] = valueLong[valueLong.length - 1 - i];
      valueLong[valueLong.length - 1 - i] = temp;
    }
    buffer.put(valueLong, 0, valueLong.length);
    buffer.flip();
    ret.value = buffer.getLong();

    byte[] noteRcm = new byte[ZC_R_SIZE];
    System.arraycopy(
        data, ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE + ZC_V_SIZE, noteRcm, 0, ZC_R_SIZE);
    ret.rcm = noteRcm;

    byte[] noteMemo = new byte[ZC_MEMO_SIZE];
    System.arraycopy(
        data,
        ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE + ZC_V_SIZE + ZC_R_SIZE,
        noteMemo,
        0,
        ZC_MEMO_SIZE);
    ret.memo = noteMemo;

    return ret;
  }

  /**
   * decrypt c_enc with ivk to plain_enc
   */
  public static Optional<Note> decrypt(
      byte[] ciphertext, byte[] ivk, byte[] epk, byte[] cmu) throws ZksnarkException {
    Optional<NoteEncryption.Encryption.EncPlaintext> pt =
        NoteEncryption.Encryption.attemptEncDecryption(ciphertext, ivk, epk);
    if (!pt.isPresent()) {
      return Optional.empty();
    }
    Note ret = decode(pt.get());
    byte[] pkD = new byte[32];
    if (!JLibrustzcash
        .librustzcashIvkToPkd(new IvkToPkdParams(ivk, ret.d.getData(), pkD))) {
      return Optional.empty();
    }
    byte[] cmuExpected = new byte[32];
    if (!JLibrustzcash.librustzcashComputeCm(
        new ComputeCmParams(ret.d.getData(), pkD, ret.value, ret.rcm, cmuExpected))) {
      return Optional.empty();
    }
    if (!Arrays.equals(cmu, cmuExpected)) {
      return Optional.empty();
    }
    return Optional.of(ret);
  }

  /**
   * decrypt c_enc with ovk to plain_enc
   */
  public static Optional<Note> decrypt(
      NoteEncryption.Encryption.EncCiphertext ciphertext, byte[] epk, byte[] esk, byte[] pkD,
      byte[] cmu)
      throws ZksnarkException {
    Optional<NoteEncryption.Encryption.EncPlaintext> pt =
        NoteEncryption.Encryption.attemptEncDecryption(ciphertext, epk, esk, pkD);
    if (!pt.isPresent()) {
      return Optional.empty();
    }
    Note ret = decode(pt.get());
    byte[] cmuExpected = new byte[32];
    if (!JLibrustzcash.librustzcashComputeCm(
        new ComputeCmParams(ret.d.getData(), pkD, ret.value, ret.rcm, cmuExpected))) {
      return Optional.empty();
    }
    if (!Arrays.equals(cmu, cmuExpected)) {
      return Optional.empty();
    }
    return Optional.of(ret);
  }

  /**
   * add the judgement of memo size
   */
  public void setMemo(byte[] memo) {
    if (ByteArray.isEmpty(memo)) {
      return;
    }
    int memoSize = memo.length < ZC_MEMO_SIZE ? memo.length : ZC_MEMO_SIZE;
    System.arraycopy(memo, 0, this.memo, 0, memoSize);
  }

  // Call librustzcash to compute the commitment
  public byte[] cm() throws ZksnarkException {
    byte[] result = new byte[32];
    if (!JLibrustzcash.librustzcashComputeCm(
        new ComputeCmParams(d.getData(), pkD, value, rcm, result))) {
      return null;
    }
    return result;
  }

  public byte[] nullifier(FullViewingKey vk, long position) throws ZksnarkException {
    byte[] ak = vk.getAk();
    byte[] nk = vk.getNk();
    byte[] result = new byte[32]; // 256
    if (!JLibrustzcash.librustzcashComputeNf(
        new ComputeNfParams(d.getData(), pkD, value, rcm, ak, nk, position, result))) {
      return null;
    }
    return result;
  }

  public byte[] nullifier(byte[] ak, byte[] nk, long position) throws ZksnarkException {
    byte[] result = new byte[32]; // 256
    if (!JLibrustzcash.librustzcashComputeNf(
        new ComputeNfParams(d.getData(), pkD, value, rcm, ak, nk, position, result))) {
      return null;
    }
    return result;
  }

  /**
   * filling pkD generated by ivk
   */
  public Optional<Note> note(IncomingViewingKey ivk) throws ZksnarkException {
    Optional<PaymentAddress> addr = ivk.address(d);
    if (addr.isPresent()) {
      return Optional.of(new Note(d, addr.get().getPkD(), value, rcm));
    } else {
      return Optional.empty();
    }
  }

  /**
   * encrypt plain_enc to c_enc with by k_enc
   */
  public Optional<NotePlaintextEncryptionResult> encrypt(byte[] pkD) throws ZksnarkException {
    // Get the encryptor
    Optional<NoteEncryption> sne = NoteEncryption.fromDiversifier(d);
    if (!sne.isPresent()) {
      return Optional.empty();
    }
    NoteEncryption enc = sne.get();
    // Create the plaintext
    NoteEncryption.Encryption.EncPlaintext pt = this.encode();
    // Encrypt the plaintext
    Optional<NoteEncryption.Encryption.EncCiphertext> encciphertext = enc
        .encryptToRecipient(pkD, pt);
    if (!encciphertext.isPresent()) {
      return Optional.empty();
    }
    return Optional.of(new NotePlaintextEncryptionResult(encciphertext.get().getData(), enc));
  }

  /**
   * construct plain_enc with columns of note
   */
  public NoteEncryption.Encryption.EncPlaintext encode() {
    ByteBuffer buffer = ByteBuffer.allocate(ZC_V_SIZE);
    buffer.putLong(0, value);
    byte[] valueLong = buffer.array();
    for (int i = 0; i < valueLong.length / 2; i++) {
      byte temp = valueLong[i];
      valueLong[i] = valueLong[valueLong.length - 1 - i];
      valueLong[valueLong.length - 1 - i] = temp;
    }

    byte[] data = new byte[ZC_ENCPLAINTEXT_SIZE];
    data[0] = 0x01;
    System.arraycopy(d.getData(), 0, data, ZC_NOTEPLAINTEXT_LEADING, ZC_DIVERSIFIER_SIZE);
    System.arraycopy(valueLong, 0, data, ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE, ZC_V_SIZE);
    System.arraycopy(rcm, 0, data, ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE + ZC_V_SIZE,
        ZC_R_SIZE);
    System.arraycopy(
        memo,
        0,
        data,
        ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE + ZC_V_SIZE + ZC_R_SIZE,
        ZC_MEMO_SIZE);

    NoteEncryption.Encryption.EncPlaintext ret = new NoteEncryption.Encryption.EncPlaintext();
    ret.setData(data);
    return ret;
  }

  @AllArgsConstructor
  public class NotePlaintextEncryptionResult {

    @Getter
    @Setter
    private byte[] encCiphertext;
    @Getter
    @Setter
    private NoteEncryption noteEncryption;
  }
}
