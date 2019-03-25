package org.tron.common.zksnark.sapling.note;

import java.nio.ByteBuffer;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.tron.common.zksnark.sapling.Librustzcash;
import org.tron.common.zksnark.sapling.address.DiversifierT;
import org.tron.common.zksnark.sapling.address.IncomingViewingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.note.BaseNote.Note;
import org.tron.common.zksnark.sapling.note.NoteEncryption.EncCiphertext;
import org.tron.common.zksnark.sapling.note.NoteEncryption.EncPlaintext;
import static org.tron.common.zksnark.sapling.ZkChainParams.*;

public class BaseNotePlaintext {

  public long value = 0L; // 64
  public byte[] memo = new byte[ZC_MEMO_SIZE];

  @AllArgsConstructor
  public class SaplingNotePlaintextEncryptionResult {

    public NoteEncryption.EncCiphertext encCiphertext;
    public SaplingNoteEncryption noteEncryption;
    // pair<EncCiphertext, SaplingNoteEncryption> SaplingNotePlaintextEncryptionResult;
  }

  public static class NotePlaintext extends BaseNotePlaintext {

    public DiversifierT d;
    public byte[] rcm;

    public NotePlaintext() {

    }

    public NotePlaintext(Note note, byte[] memo) {
      d = note.d;
      rcm = note.r;
      value = note.value;
      memo = memo;
    }

    public Optional<Note> note(IncomingViewingKey ivk) {
      Optional<PaymentAddress> addr = ivk.address(d);
      if (addr.isPresent()) {
        return Optional.of(new Note(d, addr.get().getPkD(), value, rcm));
      } else {
        return Optional.empty();
      }
    }

    public static Optional<NotePlaintext> decrypt(
        NoteEncryption.EncCiphertext ciphertext, byte[] ivk, byte[] epk, byte[] cmu) {

      Optional<NoteEncryption.EncPlaintext> pt =
          NoteEncryption.AttemptSaplingEncDecryption(ciphertext, ivk, epk);
      if (!pt.isPresent()) {
        return Optional.empty();
      }

      NotePlaintext ret = NotePlaintext.decode(pt.get());

      byte[] pk_d = null;
      if (!Librustzcash.librustzcashIvkToPkd(ivk, ret.d.getData(), pk_d)) {
        return Optional.empty();
      }

      byte[] cmu_expected = null;
      if (!Librustzcash.librustzcashSaplingComputeCm(
          ret.d.getData(), pk_d, ret.value, ret.rcm, cmu_expected)) {
        return Optional.empty();
      }

      if (cmu_expected != cmu) {
        return Optional.empty();
      }

      return Optional.of(ret);
    }

    public static Optional<NotePlaintext> decrypt(
        NoteEncryption.EncCiphertext ciphertext, byte[] epk, byte[] esk, byte[] pk_d, byte[] cmu) {
      //      auto pt = AttemptSaplingEncDecryption(ciphertext, epk, esk, pk_d);
      ////      if (!pt) {
      ////        return Optional.empty();
      ////      }
      ////
      ////      // Deserialize from the plaintext
      ////      CDataStream ss (SER_NETWORK, PROTOCOL_VERSION);
      ////      ss << pt.get();
      ////
      ////      NotePlaintext ret;
      ////      ss >> ret;
      ////
      ////      byte[] cmu_expected;
      ////      if (!librustzcash_sapling_compute_cm(
      ////          ret.d.data(),
      ////          pk_d.begin(),
      ////          ret.value(),
      ////          ret.rcm.begin(),
      ////          cmu_expected.begin()
      ////      )) {
      ////        return Optional.empty();
      ////      }
      ////
      ////      if (cmu_expected != cmu) {
      ////        return Optional.empty();
      ////      }
      ////
      ////      assert (ss.size() == 0);
      ////
      ////      return ret;
      return null;
    }

    public Optional<SaplingNotePlaintextEncryptionResult> encrypt(byte[] pk_d) {

      // Get the encryptor
      Optional<SaplingNoteEncryption> sne = SaplingNoteEncryption.FromDiversifier(d);
      if (!sne.isPresent()) {
        return Optional.empty();
      }
      SaplingNoteEncryption enc = sne.get();

      // Create the plaintext
      EncPlaintext pt = this.encode();

      // Encrypt the plaintext
      Optional<EncCiphertext> encciphertext = enc.encryptToRecipient(pk_d, pt);
      if (!encciphertext.isPresent()) {
        return Optional.empty();
      }
      return Optional.of(new SaplingNotePlaintextEncryptionResult(encciphertext.get(), enc));
    }

    // todo:
    public NoteEncryption.EncPlaintext encode() {
      ByteBuffer buffer = ByteBuffer.allocate(ZC_V_SIZE);
      byte[] valueLong;
      byte[] data;
      NoteEncryption.EncPlaintext ret = new NoteEncryption.EncPlaintext();

      ret.data = new byte[ZC_SAPLING_ENCPLAINTEXT_SIZE];
      data = ret.data;

      //将long转换为byte[]
      buffer.putLong(0, value);
      valueLong =  buffer.array();

      data[0] = 0x01;
      System.arraycopy(d, 0, data, ZC_NOTEPLAINTEXT_LEADING, ZC_DIVERSIFIER_SIZE);
      System.arraycopy(valueLong, 0, data,
              ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE, ZC_V_SIZE);
      System.arraycopy(rcm, 0, data,
              ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE + ZC_V_SIZE, ZC_R_SIZE);
      System.arraycopy(memo, 0, data,
              ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE + ZC_V_SIZE + ZC_R_SIZE, ZC_MEMO_SIZE);

      return ret;
    }

    public static NotePlaintext decode(NoteEncryption.EncPlaintext encPlaintext) {
      byte[] data = encPlaintext.data;
      byte[] valueLong = new byte[ZC_V_SIZE];
      ByteBuffer buffer = ByteBuffer.allocate(ZC_V_SIZE);

//      READWRITE(leadingByte);
//
//      if (leadingByte != 0x01) {
//        throw std::ios_base::failure("lead byte of SaplingNotePlaintext is not recognized");
//      }
//
//      READWRITE(d);           // 11 bytes
//      READWRITE(value_);      // 8 bytes
//      READWRITE(rcm);         // 32 bytes
//      READWRITE(memo_);       // 512 bytes

      if(encPlaintext.data[0] != 0x01) {
        throw new RuntimeException("lead byte of SaplingNotePlaintext is not recognized");
      }

      NotePlaintext ret = new NotePlaintext();

      //(ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE + ZC_V_SIZE + ZC_R_SIZE + ZC_MEMO_SIZE);
      System.arraycopy(data, ZC_NOTEPLAINTEXT_LEADING,
              ret.d, 0, ZC_DIVERSIFIER_SIZE);
      System.arraycopy(data, ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE,
              valueLong, 0, ZC_V_SIZE);
      System.arraycopy(data, ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE + ZC_V_SIZE,
              ret.rcm, 0, ZC_R_SIZE);
      System.arraycopy(data, ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE + ZC_V_SIZE + ZC_R_SIZE,
              ret.memo, 0, ZC_MEMO_SIZE);

      //将byte[]转换成long
      buffer.put(valueLong, 0, valueLong.length);
      buffer.flip();
      ret.value = buffer.getLong();

      return ret;
    }
  }
}
