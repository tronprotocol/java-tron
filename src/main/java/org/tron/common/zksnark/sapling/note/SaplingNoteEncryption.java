package org.tron.common.zksnark.sapling.note;

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

  bool already_encrypted_enc;
  bool already_encrypted_out;


  static boost::

  optional<SaplingNoteEncryption> FromDiversifier(DiversifierT d);

  boost::

  optional<SaplingEncCiphertext> encrypt_to_recipient(
        const byte[] &pk_d,
        const SaplingEncPlaintext &message
  );

  SaplingOutCiphertext encrypt_to_ourselves(
        const byte[] &ovk,
        const byte[] &cv,
        const byte[] &cm,
        const SaplingOutPlaintext &message
  );
}
