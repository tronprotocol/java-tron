package org.tron.common.zksnark.sapling.note;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.tron.common.zksnark.sapling.Librustzcash;
import org.tron.common.zksnark.sapling.ZkChainParams;
import org.tron.common.zksnark.sapling.address.DiversifierT;
import org.tron.common.zksnark.sapling.address.IncomingViewingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.note.BaseNote.Note;
import org.tron.common.zksnark.sapling.note.NoteEncryption.EncCiphertext;
import org.tron.common.zksnark.sapling.note.NoteEncryption.EncPlaintext;

public class BaseNotePlaintext {

  public long value = 0L; // 64
  public byte[] memo = new byte[ZkChainParams.ZC_MEMO_SIZE];

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
      return null;
    }

    public static NotePlaintext decode(NoteEncryption.EncPlaintext encPlaintext) {
      byte[] data = encPlaintext.data;

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

      // todo
      NotePlaintext ret = new NotePlaintext();

      //(ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE + ZC_V_SIZE + ZC_R_SIZE + ZC_MEMO_SIZE);
      System.arraycopy(encPlaintext.data, 1,  ret.d, 0, 11);
      System.arraycopy(encPlaintext.data, 1 + 11,  ret.value, 0, 8);
      System.arraycopy(encPlaintext.data, 1 + 11 + 8,  ret.rcm, 0, 32);
      System.arraycopy(encPlaintext.data, 1 + 11 + 8 + 32,  ret.memo, 0, 512);

      return ret;
    }
  }
}
