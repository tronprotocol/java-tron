package org.tron.common.zksnark.sapling.note;

import org.tron.common.zksnark.sapling.address.DiversifierT;
import org.tron.common.zksnark.sapling.note.BaseNote.Note;
import org.tron.common.zksnark.sapling.transaction.Ciphertext.SaplingEncCiphertext;
import org.tron.common.zksnark.sapling.transaction.Ciphertext.SaplingEncPlaintext;

public class BaseNotePlaintext {

  uint64_t value_ = 0;
  Array<char, ZC_MEMO_SIZE> memo_;

  public class SaplingNotePlaintextEncryptionResult {

    SaplingEncCiphertext encCiphertext;
    SaplingNoteEncryption noteEncryption;
    //pair<SaplingEncCiphertext, SaplingNoteEncryption> SaplingNotePlaintextEncryptionResult;
  }


  public class SaplingNotePlaintext extends BaseNotePlaintext {

    DiversifierT d;
    byte[] rcm;


    optional<Note> SaplingNotePlaintext::

    note(const SaplingIncomingViewingKey&ivk) const

    {
      auto addr = ivk.address(d);
      if (addr) {
        return SaplingNote(d, addr.get().pk_d, value_, rcm);
      } else {
        return boost::none;
      }
    }


    static boost::

    optional<SaplingNotePlaintext> decrypt(
    const SaplingEncCiphertext &ciphertext,
    const byte[] &ivk,
    const byte[] &epk,
    const byte[] &cmu
    ) {
      auto pt = AttemptSaplingEncDecryption(ciphertext, ivk, epk);
      if (!pt) {
        return boost::none;
      }

      // Deserialize from the plaintext
      CDataStream ss (SER_NETWORK, PROTOCOL_VERSION);
      ss << pt.get();

      SaplingNotePlaintext ret;
      ss >> ret;

      assert (ss.size() == 0);

      byte[] pk_d;
      if (!librustzcash_ivk_to_pkd(ivk.begin(), ret.d.data(), pk_d.begin())) {
        return boost::none;
      }

      byte[] cmu_expected;
      if (!librustzcash_sapling_compute_cm(
          ret.d.data(),
          pk_d.begin(),
          ret.value(),
          ret.rcm.begin(),
          cmu_expected.begin()
      )) {
        return boost::none;
      }

      if (cmu_expected != cmu) {
        return boost::none;
      }

      return ret;
    }

    static boost::

    optional<SaplingNotePlaintext> decrypt(
    const SaplingEncCiphertext &ciphertext,
    const byte[] &epk,
    const byte[] &esk,
    const byte[] &pk_d,
    const byte[] &cmu
    ) {
      auto pt = AttemptSaplingEncDecryption(ciphertext, epk, esk, pk_d);
      if (!pt) {
        return boost::none;
      }

      // Deserialize from the plaintext
      CDataStream ss (SER_NETWORK, PROTOCOL_VERSION);
      ss << pt.get();

      SaplingNotePlaintext ret;
      ss >> ret;

      byte[] cmu_expected;
      if (!librustzcash_sapling_compute_cm(
          ret.d.data(),
          pk_d.begin(),
          ret.value(),
          ret.rcm.begin(),
          cmu_expected.begin()
      )) {
        return boost::none;
      }

      if (cmu_expected != cmu) {
        return boost::none;
      }

      assert (ss.size() == 0);

      return ret;
    }

    boost::

    optional<SaplingNotePlaintextEncryptionResult> encrypt(const byte[]&pk_d) const

    {
      // Get the encryptor
      auto sne = SaplingNoteEncryption::FromDiversifier (d);
      if (!sne) {
        return boost::none;
      }
      SaplingNoteEncryption enc = sne.get();

      // Create the plaintext
      CDataStream ss (SER_NETWORK, PROTOCOL_VERSION);
      ss << ( * this);
      SaplingEncPlaintext pt;
      assert (pt.size() == ss.size());
      memcpy( & pt[0], &ss[0], pt.size());

      // Encrypt the plaintext
      auto encciphertext = enc.encrypt_to_recipient(pk_d, pt);
      if (!encciphertext) {
        return boost::none;
      }
      return SaplingNotePlaintextEncryptionResult(encciphertext.get(), enc);
    }

  }
}
