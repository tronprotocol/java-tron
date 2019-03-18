package org.tron.common.zksnark.sapling.note;

import java.util.Optional;
import org.tron.common.zksnark.sapling.transaction.Ciphertext.SaplingOutCiphertext;

public class SaplingOutgoingPlaintext {

  byte[] pk_d;
  byte[] esk;

  Optional<SaplingOutgoingPlaintext> decrypt(
      SaplingOutCiphertext ciphertext, byte[] ovk, byte[] cv, byte[] cm, byte[] epk) {
    //    Optional<SaplingOutPlaintext> pt = NoteEncryption.AttemptSaplingOutDecryption(ciphertext,
    // ovk, cv, cm, epk);
    //    if (!pt.isPresent()) {
    //      return  none;
    //    }
    //
    //    // Deserialize from the plaintext
    //    CDataStream ss (SER_NETWORK, PROTOCOL_VERSION);
    //    ss << pt.get();
    //
    //    SaplingOutgoingPlaintext ret;
    //    ss >> ret;
    //
    //    assert (ss.size() == 0);
    //
    //    return ret;
    return null;
  }

  SaplingOutCiphertext encrypt(byte[] ovk, byte[] cv, byte[] cm, SaplingNoteEncryption enc) {

    //    // Create the plaintext
    //    CDataStream ss (SER_NETWORK, PROTOCOL_VERSION);
    //    ss << ( * this);
    //    SaplingOutPlaintext pt;
    //    assert (pt.size() == ss.size());
    //    memcpy(pt[0], ss[0], pt.size());
    //
    //    return enc.encrypt_to_ourselves(ovk, cv, cm, pt);
    return null;
  }
}
