package org.tron.common.zksnark.sapling.address;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.tron.common.zksnark.sapling.Librustzcash;

// ivk
@AllArgsConstructor
public class IncomingViewingKey {

  public byte[] value; // 256

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
