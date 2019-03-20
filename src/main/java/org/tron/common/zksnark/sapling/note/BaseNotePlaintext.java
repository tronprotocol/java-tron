package org.tron.common.zksnark.sapling.note;

import java.util.Optional;
import org.tron.common.zksnark.sapling.Librustzcash;
import org.tron.common.zksnark.sapling.ZkChainParams;
import org.tron.common.zksnark.sapling.address.DiversifierT;
import org.tron.common.zksnark.sapling.address.IncomingViewingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.note.BaseNote.Note;

public class BaseNotePlaintext {

  public long value = 0L; // 64
  public byte[] memo = new byte[ZkChainParams.ZC_MEMO_SIZE];

  public class SaplingNotePlaintextEncryptionResult {

    NoteEncryption.EncCiphertext encCiphertext;
    SaplingNoteEncryption noteEncryption;
    // pair<EncCiphertext, SaplingNoteEncryption> SaplingNotePlaintextEncryptionResult;
  }

  public static class NotePlaintext extends BaseNotePlaintext {

    public DiversifierT d;
    public byte[] rcm;

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
      if (!Librustzcash.librustzcash_ivk_to_pkd(ivk, ret.d.getData(), pk_d)) {
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

    Optional<SaplingNotePlaintextEncryptionResult> encrypt(byte[] pk_d) {

      //      // Get the encryptor
      //      auto sne = SaplingNoteEncryption::FromDiversifier (d);
      //      if (!sne) {
      //        return Optional.empty();
      //      }
      //      SaplingNoteEncryption enc = sne.get();
      //
      //      // Create the plaintext
      //      CDataStream ss (SER_NETWORK, PROTOCOL_VERSION);
      //      ss << ( * this);
      //      EncPlaintext pt;
      //      assert (pt.size() == ss.size());
      //      memcpy(pt[0], ss[0], pt.size());
      //
      //      // Encrypt the plaintext
      //      auto encciphertext = enc.encrypt_to_recipient(pk_d, pt);
      //      if (!encciphertext) {
      //        return Optional.empty();
      //      }
      //      return SaplingNotePlaintextEncryptionResult(encciphertext.get(), enc);
      return null;
    }

    public static NotePlaintext decode(NoteEncryption.EncPlaintext encPlaintext) {
      byte[] data = encPlaintext.data;

      //todo
      NotePlaintext ret = new NotePlaintext();
      return ret;
    }
  }
}
