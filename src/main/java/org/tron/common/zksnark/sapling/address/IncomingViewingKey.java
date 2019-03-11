package org.tron.common.zksnark.sapling.address;

//Decryption using an Incoming Viewing Key
//ivk
public class IncomingViewingKey {
  // class IncomingViewingKey : public uint256 {

  //To create a new diversied payment address given an incoming viewing key ivk, repeatedly pick a diversier d uniformly at random
  optional<PaymentAddress> address(diversifier_t d) {
    uint256 pk_d;
    if (librustzcash_check_diversifier(d.data())) {
      librustzcash_ivk_to_pkd(this->begin(), d.data(), pk_d.begin());
      return PaymentAddress(d, pk_d);
    } else {
      return boost::none;
    }
  }
}
