package org.tron.common.zksnark.sapling.note;

import org.tron.common.zksnark.sapling.address.diversifier_t;
import org.tron.common.zksnark.sapling.note.BaseNote.SaplingNote;
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

    diversifier_t d;
    uint256 rcm;


    optional<SaplingNote> SaplingNotePlaintext::

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
    const uint256 &ivk,
    const uint256 &epk,
    const uint256 &cmu
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

      uint256 pk_d;
      if (!librustzcash_ivk_to_pkd(ivk.begin(), ret.d.data(), pk_d.begin())) {
        return boost::none;
      }

      uint256 cmu_expected;
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
    const uint256 &epk,
    const uint256 &esk,
    const uint256 &pk_d,
    const uint256 &cmu
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

      uint256 cmu_expected;
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

    optional<SaplingNotePlaintextEncryptionResult> encrypt(const uint256&pk_d) const

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
