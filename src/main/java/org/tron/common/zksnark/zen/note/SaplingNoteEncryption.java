package org.tron.common.zksnark.zen.note;

import java.util.Optional;
import org.tron.common.zksnark.zen.address.DiversifierT;
import org.tron.common.zksnark.zen.note.NoteEncryption.EncCiphertext;
import org.tron.common.zksnark.zen.note.NoteEncryption.EncPlaintext;
import org.tron.common.zksnark.zen.note.NoteEncryption.OutCiphertext;
import org.tron.common.zksnark.zen.note.NoteEncryption.OutPlaintext;

public class SaplingNoteEncryption {

  // Ephemeral public key
  public byte[] epk;

  // Ephemeral secret key
  public byte[] esk;

  public boolean already_encrypted_enc;
  public boolean already_encrypted_out;

  //todo:
  public static Optional<SaplingNoteEncryption> FromDiversifier(DiversifierT d) {
    return null;
  }

  Optional<EncCiphertext> encryptToRecipient(byte[] pk_d, EncPlaintext message) {
    return null;
  }

  OutCiphertext encrypt_to_ourselves(
      byte[] ovk, byte[] cv, byte[] cm, OutPlaintext message) {
    return null;
  }
}
