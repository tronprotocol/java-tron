package org.tron.common.zksnark.sapling.address;

//Decryption using a Full Viewing Key
public class FullViewingKey {

  uint256 ak;
  uint256 nk;
  //the outgoing viewing key
  uint256 ovk;

  //! Get the fingerprint of this full viewing key (as defined in ZIP 32).
  uint256 GetFingerprint() const

  {
    CBLAKE2bWriter ss (SER_GETHASH, 0, ZCASH_SAPLING_FVFP_PERSONALIZATION);
    ss << *this;
    return ss.GetHash();
  }

  IncomingViewingKey in_viewing_key() {

    uint256 ivk;//the incoming viewing key
    librustzcash_crh_ivk(ak.begin(), nk.begin(), ivk.begin());
    return IncomingViewingKey(ivk);
  }

  bool is_valid() {
    uint256 ivk;
    librustzcash_crh_ivk(ak.begin(), nk.begin(), ivk.begin());
    return !ivk.IsNull();
  }
}
