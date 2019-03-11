package org.tron.common.zksnark.sapling.note;

import org.tron.common.zksnark.sapling.transaction.Ciphertext.SaplingOutCiphertext;
import org.tron.common.zksnark.sapling.transaction.Ciphertext.SaplingOutPlaintext;

public class SaplingOutgoingPlaintext {

  uint256 pk_d;
  uint256 esk;


  boost::

  optional<SaplingOutgoingPlaintext> decrypt(
    const SaplingOutCiphertext &ciphertext,
    const uint256&ovk,
    const uint256&cv,
    const uint256&cm,
    const uint256&epk
  ) {
    auto pt = AttemptSaplingOutDecryption(ciphertext, ovk, cv, cm, epk);
    if (!pt) {
      return boost::none;
    }

    // Deserialize from the plaintext
    CDataStream ss (SER_NETWORK, PROTOCOL_VERSION);
    ss << pt.get();

    SaplingOutgoingPlaintext ret;
    ss >> ret;

    assert (ss.size() == 0);

    return ret;
  }


  SaplingOutCiphertext encrypt(
        const uint256&ovk,
        const uint256&cv,
        const uint256&cm,
      SaplingNoteEncryption&enc
  ) const

  {
    // Create the plaintext
    CDataStream ss (SER_NETWORK, PROTOCOL_VERSION);
    ss << ( * this);
    SaplingOutPlaintext pt;
    assert (pt.size() == ss.size());
    memcpy( & pt[0], &ss[0], pt.size());

    return enc.encrypt_to_ourselves(ovk, cv, cm, pt);
  }
}
