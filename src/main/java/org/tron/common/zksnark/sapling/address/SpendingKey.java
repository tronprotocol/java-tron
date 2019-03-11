package org.tron.common.zksnark.sapling.address;

import org.tron.common.zksnark.sapling.utils.PRF;


public class SpendingKey {
  // class SpendingKey : public uint256 {

  static SpendingKey random() {
    while (true) {
      SpendingKey sk = SpendingKey(random_uint256());
      if (sk.full_viewing_key().is_valid()) {
        return sk;
      }
    }
  }

  ExpandedSpendingKey expanded_spending_key() {
    return ExpandedSpendingKey(PRF.PRF_ask( * this),PRF.PRF_nsk( * this),PRF.PRF_ovk( * this))
  }

  FullViewingKey full_viewing_key() {

    return expanded_spending_key().full_viewing_key();
  }

  // Can derive  addr from default diversifier
  PaymentAddress default_address() {
    // Iterates within default_diversifier to ensure a valid address is returned
    auto addrOpt = full_viewing_key().in_viewing_key().address(default_diversifier( * this));
    assert (addrOpt != boost::none);
    return addrOpt.value();
  }


}
