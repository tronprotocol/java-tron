package org.tron.common.zksnark.sapling.note;

import java.util.Optional;
import org.tron.common.zksnark.sapling.Params;
import org.tron.common.zksnark.sapling.address.DiversifierT;
import org.tron.common.zksnark.sapling.address.IncomingViewingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.note.BaseNote.Note;
import org.tron.common.zksnark.sapling.transaction.Ciphertext.SaplingEncCiphertext;

public class BaseNotePlaintext {

  long value_ = 0L; // 64
  byte[] memo_ = new byte[Params.ZC_MEMO_SIZE];

  public class SaplingNotePlaintextEncryptionResult {

    SaplingEncCiphertext encCiphertext;
    SaplingNoteEncryption noteEncryption;
    // pair<SaplingEncCiphertext, SaplingNoteEncryption> SaplingNotePlaintextEncryptionResult;
  }

  public class SaplingNotePlaintext extends BaseNotePlaintext {

    DiversifierT d;
    byte[] rcm;

    Optional<Note> note(IncomingViewingKey ivk) {
      Optional<PaymentAddress> addr = ivk.address(d);
      if (addr.isPresent()) {
        return Optional.of(new Note(d, addr.get().getPkD(), value_, rcm));
      } else {
        return Optional.empty();
      }
    }

    Optional<SaplingNotePlaintext> decrypt(
        SaplingEncCiphertext ciphertext, byte[] ivk, byte[] epk, byte[] cmu) {
      //
      //      Optional<SaplingEncPlaintext> pt =
      // NoteEncryption.AttemptSaplingEncDecryption(ciphertext, ivk, epk);
      //      if (!pt.isPresent()) {
      //        return Optional.empty();
      //      }
      //
      //      // Deserialize from the plaintext
      //      CDataStream ss (SER_NETWORK, PROTOCOL_VERSION);
      //      ss << pt.get();
      //
      //      SaplingNotePlaintext ret;
      //      ss >> ret;
      //
      //      assert (ss.size() == 0);
      //
      //      byte[] pk_d;
      //      if (!librustzcash_ivk_to_pkd(ivk.begin(), ret.d.data(), pk_d.begin())) {
      //        return Optional.empty();
      //      }
      //
      //      byte[] cmu_expected;
      //      if (!librustzcash_sapling_compute_cm(
      //          ret.d.data(),
      //          pk_d.begin(),
      //          ret.value(),
      //          ret.rcm.begin(),
      //          cmu_expected.begin()
      //      )) {
      //        return Optional.empty();
      //      }
      //
      //      if (cmu_expected != cmu) {
      //        return Optional.empty();
      //      }
      //
      //      return ret;
      return null;
    }

    public Optional<SaplingNotePlaintext> decrypt(
        SaplingEncCiphertext ciphertext, byte[] epk, byte[] esk, byte[] pk_d, byte[] cmu) {
      //      auto pt = AttemptSaplingEncDecryption(ciphertext, epk, esk, pk_d);
      ////      if (!pt) {
      ////        return Optional.empty();
      ////      }
      ////
      ////      // Deserialize from the plaintext
      ////      CDataStream ss (SER_NETWORK, PROTOCOL_VERSION);
      ////      ss << pt.get();
      ////
      ////      SaplingNotePlaintext ret;
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
      //      SaplingEncPlaintext pt;
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
  }
}
