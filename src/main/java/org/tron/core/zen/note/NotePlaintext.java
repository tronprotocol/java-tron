package org.tron.core.zen.note;

import static org.tron.core.zen.note.ZenChainParams.ZC_DIVERSIFIER_SIZE;
import static org.tron.core.zen.note.ZenChainParams.ZC_ENCPLAINTEXT_SIZE;
import static org.tron.core.zen.note.ZenChainParams.ZC_MEMO_SIZE;
import static org.tron.core.zen.note.ZenChainParams.ZC_NOTEPLAINTEXT_LEADING;
import static org.tron.core.zen.note.ZenChainParams.ZC_R_SIZE;
import static org.tron.core.zen.note.ZenChainParams.ZC_V_SIZE;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.tron.common.zksnark.Librustzcash;
import org.tron.common.zksnark.LibrustzcashParam.IvkToPkdParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingComputeCmParams;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.address.IncomingViewingKey;
import org.tron.core.zen.address.PaymentAddress;
import org.tron.core.zen.note.NoteEncryption.Encryption;
import org.tron.core.zen.note.NoteEncryption.Encryption.EncCiphertext;
import org.tron.core.zen.note.NoteEncryption.Encryption.EncPlaintext;

public class NotePlaintext {

  public long value = 0L;
  public byte[] memo = new byte[ZC_MEMO_SIZE];
  public DiversifierT d;
  public byte[] rcm;

  public NotePlaintext() {
    d = new DiversifierT();
    rcm = new byte[ZC_R_SIZE];
  }

  public NotePlaintext(Note note, byte[] memo) {
    d = note.d;
    rcm = note.r;
    value = note.value;
    memo = memo;
  }

  public static Optional<NotePlaintext> decrypt(
      byte[] ciphertext, byte[] ivk, byte[] epk, byte[] cmu) throws ZksnarkException {
    Optional<Encryption.EncPlaintext> pt =
        Encryption.AttemptSaplingEncDecryption(ciphertext, ivk, epk);
    if (!pt.isPresent()) {
      return Optional.empty();
    }
    NotePlaintext ret = org.tron.core.zen.note.NotePlaintext.decode(pt.get());
    byte[] pk_d = new byte[32];
    if (!Librustzcash.librustzcashIvkToPkd(new IvkToPkdParams(ivk, ret.d.getData(), pk_d))) {
      return Optional.empty();
    }
    byte[] cmu_expected = new byte[32];
    if (!Librustzcash.librustzcashSaplingComputeCm(
        new SaplingComputeCmParams(ret.d.getData(), pk_d, ret.value, ret.rcm, cmu_expected))) {
      return Optional.empty();
    }
    if (!Arrays.equals(cmu, cmu_expected)) {
      return Optional.empty();
    }
    return Optional.of(ret);
  }

  public static Optional<NotePlaintext> decrypt(
      Encryption.EncCiphertext ciphertext, byte[] epk, byte[] esk, byte[] pk_d, byte[] cmu)
      throws ZksnarkException {
    Optional<Encryption.EncPlaintext> pt =
        Encryption.AttemptSaplingEncDecryption(ciphertext, epk, esk, pk_d);
    if (!pt.isPresent()) {
      return Optional.empty();
    }
    NotePlaintext ret = org.tron.core.zen.note.NotePlaintext.decode(pt.get());
    byte[] cmu_expected = new byte[32];
    if (!Librustzcash.librustzcashSaplingComputeCm(
        new SaplingComputeCmParams(ret.d.getData(), pk_d, ret.value, ret.rcm, cmu_expected))) {
      return Optional.empty();
    }
    if (!Arrays.equals(cmu, cmu_expected)) {
      return Optional.empty();
    }
    return Optional.of(ret);
  }

  public static NotePlaintext decode(Encryption.EncPlaintext encPlaintext) throws ZksnarkException {
    byte[] data = encPlaintext.data;
    byte[] valueLong = new byte[ZC_V_SIZE];
    ByteBuffer buffer = ByteBuffer.allocate(ZC_V_SIZE);
    if (encPlaintext.data[0] != 0x01) {
      throw new ZksnarkException("lead byte of SaplingNotePlaintext is not recognized");
    }
    NotePlaintext ret = new NotePlaintext();
    System.arraycopy(data, ZC_NOTEPLAINTEXT_LEADING, ret.d.getData(), 0, ZC_DIVERSIFIER_SIZE);
    System.arraycopy(data, ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE, valueLong, 0, ZC_V_SIZE);
    System.arraycopy(
        data, ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE + ZC_V_SIZE, ret.rcm, 0, ZC_R_SIZE);
    System.arraycopy(
        data,
        ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE + ZC_V_SIZE + ZC_R_SIZE,
        ret.memo,
        0,
        ZC_MEMO_SIZE);
    for (int i = 0; i < valueLong.length / 2; i++) {
      byte temp = valueLong[i];
      valueLong[i] = valueLong[valueLong.length - 1 - i];
      valueLong[valueLong.length - 1 - i] = temp;
    }
    buffer.put(valueLong, 0, valueLong.length);
    buffer.flip();
    ret.value = buffer.getLong();

    return ret;
  }

  public Optional<Note> note(IncomingViewingKey ivk) throws ZksnarkException {
    Optional<PaymentAddress> addr = ivk.address(d);
    if (addr.isPresent()) {
      return Optional.of(new Note(d, addr.get().getPkD(), value, rcm));
    } else {
      return Optional.empty();
    }
  }

  public Optional<NotePlaintextEncryptionResult> encrypt(byte[] pk_d) throws ZksnarkException {
    // Get the encryptor
    Optional<NoteEncryption> sne = NoteEncryption.fromDiversifier(d);
    if (!sne.isPresent()) {
      return Optional.empty();
    }
    NoteEncryption enc = sne.get();
    // Create the plaintext
    EncPlaintext pt = this.encode();
    // Encrypt the plaintext
    Optional<EncCiphertext> encciphertext = enc.encryptToRecipient(pk_d, pt);
    if (!encciphertext.isPresent()) {
      return Optional.empty();
    }
    return Optional.of(new NotePlaintextEncryptionResult(encciphertext.get().data, enc));
  }

  public Encryption.EncPlaintext encode() {
    ByteBuffer buffer = ByteBuffer.allocate(ZC_V_SIZE);
    byte[] valueLong;
    byte[] data;
    Encryption.EncPlaintext ret = new Encryption.EncPlaintext();
    ret.data = new byte[ZC_ENCPLAINTEXT_SIZE];
    data = ret.data;
    buffer.putLong(0, value);
    valueLong = buffer.array();
    for (int i = 0; i < valueLong.length / 2; i++) {
      byte temp = valueLong[i];
      valueLong[i] = valueLong[valueLong.length - 1 - i];
      valueLong[valueLong.length - 1 - i] = temp;
    }

    data[0] = 0x01;
    System.arraycopy(d.data, 0, data, ZC_NOTEPLAINTEXT_LEADING, ZC_DIVERSIFIER_SIZE);
    System.arraycopy(valueLong, 0, data, ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE, ZC_V_SIZE);
    System.arraycopy(
        rcm, 0, data, ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE + ZC_V_SIZE, ZC_R_SIZE);
    System.arraycopy(
        memo,
        0,
        data,
        ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE + ZC_V_SIZE + ZC_R_SIZE,
        ZC_MEMO_SIZE);

    return ret;
  }

  @AllArgsConstructor
  public class NotePlaintextEncryptionResult {

    public byte[] encCiphertext;
    public NoteEncryption noteEncryption;
  }
}