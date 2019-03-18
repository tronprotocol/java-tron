package org.tron.common.zksnark.sapling.transaction;

public class SpendDescription {

  byte[] cv; // !< A value commitment to the value of the input note.
  byte[]
      anchor; // !< A Merkle root of the Sapling note commitment tree at some block height in the
  // past.
  byte[] nullifier; // !< The nullifier of the input note.
  byte[] rk; // !< The randomized public key for spendAuthSig.
  GrothProof zkproof; // !< A zero-knowledge proof using the spend circuit.
  spend_auth_sig_t spendAuthSig; // !< A signature authorizing this spend.

  class spend_auth_sig_t {
    // typedef Array<char,64> spend_auth_sig_t;
  }
}
