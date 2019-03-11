package org.tron.common.zksnark.sapling.transaction;

public class SpendDescription {


  uint256 cv;                    //!< A value commitment to the value of the input note.
  uint256 anchor;                //!< A Merkle root of the Sapling note commitment tree at some block height in the past.
  uint256 nullifier;             //!< The nullifier of the input note.
  uint256 rk;                    //!< The randomized public key for spendAuthSig.
  GrothProof zkproof;  //!< A zero-knowledge proof using the spend circuit.
  spend_auth_sig_t spendAuthSig; //!< A signature authorizing this spend.

  class spend_auth_sig_t {
    //typedef Array<char,64> spend_auth_sig_t;
  }
}
