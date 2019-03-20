package org.tron.common.zksnark.sapling.address;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.tron.common.zksnark.sapling.Librustzcash;

// Decryption using an Incoming Viewing Key
// ivk
@AllArgsConstructor
public class IncomingViewingKey {

  public byte[] value; // 256
  // class IncomingViewingKey : public uint256 {

  // To create a new diversied payment address given an incoming viewing key ivk, repeatedly pick a
  // diversier d uniformly at random
  public Optional<PaymentAddress> address(DiversifierT d) {
    byte[] pkD = null; // 256
    if (Librustzcash.librustzcashCheckDiversifier(d.getData())) {
      Librustzcash.librustzcashIvkToPkd(value, d.getData(), pkD);
      return Optional.of(new PaymentAddress(d, pkD));
    } else {
      return Optional.empty();
    }
  }
}
