package org.tron.common.zksnark.sapling.note;

import java.util.Optional;
import org.tron.common.zksnark.sapling.address.DiversifierT;
import org.tron.common.zksnark.sapling.transaction.Ciphertext.SaplingEncCiphertext;
import org.tron.common.zksnark.sapling.transaction.Ciphertext.SaplingEncPlaintext;
import org.tron.common.zksnark.sapling.transaction.Ciphertext.SaplingOutCiphertext;
import org.tron.common.zksnark.sapling.transaction.Ciphertext.SaplingOutPlaintext;

public class SaplingNoteEncryption {

  // Ephemeral public key
  byte[] epk;

  // Ephemeral secret key
  byte[] esk;

  boolean already_encrypted_enc;
  boolean already_encrypted_out;

  public static Optional<SaplingNoteEncryption> FromDiversifier(DiversifierT d) {
    return null;
  }

  Optional<SaplingEncCiphertext> encrypt_to_recipient(byte[] pk_d, SaplingEncPlaintext message) {
    return null;
  }

  SaplingOutCiphertext encrypt_to_ourselves(
      byte[] ovk, byte[] cv, byte[] cm, SaplingOutPlaintext message) {
    return null;
  }
}
