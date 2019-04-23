package org.tron.common.zksnark.zen.address;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.zksnark.zen.Librustzcash;

// ivk
@AllArgsConstructor
public class IncomingViewingKey {

  @Setter
  @Getter
  public byte[] value; // 256

  public Optional<PaymentAddress> address(DiversifierT d) {
    byte[] pkD = new byte[32]; // 32
    if (Librustzcash.librustzcashCheckDiversifier(d.data)) {
      Librustzcash.librustzcashIvkToPkd(value, d.data, pkD);
      return Optional.of(new PaymentAddress(d, pkD));
    } else {
      return Optional.empty();
    }
  }
}
