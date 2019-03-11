package org.tron.common.zksnark.sapling.note;

import org.tron.common.zksnark.sapling.address.diversifier_t;
import org.tron.common.zksnark.sapling.transaction.Ciphertext.SaplingEncCiphertext;
import org.tron.common.zksnark.sapling.transaction.Ciphertext.SaplingEncPlaintext;
import org.tron.common.zksnark.sapling.transaction.Ciphertext.SaplingOutCiphertext;
import org.tron.common.zksnark.sapling.transaction.Ciphertext.SaplingOutPlaintext;

public class SaplingNoteEncryption {

  // Ephemeral public key
  uint256 epk;

  // Ephemeral secret key
  uint256 esk;

  bool already_encrypted_enc;
  bool already_encrypted_out;


  static boost::

  optional<SaplingNoteEncryption> FromDiversifier(diversifier_t d);

  boost::

  optional<SaplingEncCiphertext> encrypt_to_recipient(
        const uint256 &pk_d,
        const SaplingEncPlaintext &message
  );

  SaplingOutCiphertext encrypt_to_ourselves(
        const uint256 &ovk,
        const uint256 &cv,
        const uint256 &cm,
        const SaplingOutPlaintext &message
  );
}
