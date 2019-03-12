package org.tron.common.zksnark.sapling.address;

import org.tron.common.utils.ByteArray;

public class ExpandedSpendingKey {

  uint256 ask;//the spend authorizing key
  uint256 nsk;//the proof authorizing key (ak, nsk)
  //Let ovk be an outgoing viewing key that is intended to be able to decrypt this payment
  uint256 ovk;//the outgoing viewing key

  // A note is spent by proving knowledge of (p, ak, nsk) in zero knowledge while
  // publically disclosing its nf, allowing nf to be used to prevent double-spending.

  FullViewingKey full_viewing_key() {

    uint256 ak;
    uint256 nk;
    librustzcash_ask_to_ak(ask.begin(), ak.begin());
    librustzcash_nsk_to_nk(nsk.begin(), nk.begin());
    return FullViewingKey(ak, nk, ovk);
  }

  public static ExpandedSpendingKey decode(ByteArray m_bytes) {

  }
}
