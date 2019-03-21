package org.tron.common.zksnark.sapling.note;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.tron.common.zksnark.sapling.note.NoteEncryption.OutCiphertext;
import org.tron.common.zksnark.sapling.note.NoteEncryption.OutPlaintext;

@AllArgsConstructor
public class SaplingOutgoingPlaintext {

  public byte[] pk_d;
  public byte[] esk;

  Optional<SaplingOutgoingPlaintext> decrypt(
      OutCiphertext ciphertext, byte[] ovk, byte[] cv, byte[] cm, byte[] epk) {
    //    Optional<OutPlaintext> pt = NoteEncryption.AttemptSaplingOutDecryption(ciphertext,
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

  public OutCiphertext encrypt(byte[] ovk, byte[] cv, byte[] cm, SaplingNoteEncryption enc) {

    OutPlaintext pt = this.encode();
    return enc.encrypt_to_ourselves(ovk, cv, cm, pt);
  }

  // todo:
  private OutPlaintext encode() {
    return null;
  }
}
