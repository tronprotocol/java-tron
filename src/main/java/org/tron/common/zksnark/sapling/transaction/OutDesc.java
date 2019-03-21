package org.tron.common.zksnark.sapling.transaction;

import org.tron.common.zksnark.sapling.note.NoteEncryption;

public class OutDesc {

  public byte[] cv; // !< A value commitment to the value of the output note.
  public byte[] cm; // !< The note commitment for the output note.
  public byte[] ephemeralKey; // !< A Jubjub public key.
  public NoteEncryption.EncCiphertext
      encCiphertext; // !< A ciphertext component for the encrypted output note.
  // a ciphertext component that allows the holder of a full viewing key to recover the recipient
  // diversied transmission key pkd and the ephemeral private key esk (and therefore the entire
  // note plaintext );
  public NoteEncryption.OutCiphertext
      outCiphertext; // !< A ciphertext component for the encrypted output  note.
  public GrothProof zkproof; // !< A zero-knowledge proof using the output circuit.
}
