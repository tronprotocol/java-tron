package org.tron.core.zen.address;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.LibrustzcashParam.IvkToPkdParams;
import org.tron.core.exception.ZksnarkException;

// ivk
@Slf4j(topic = "shieldTransaction")
@AllArgsConstructor
public class IncomingViewingKey {

  @Setter
  @Getter
  public byte[] value; // 256

  public Optional<PaymentAddress> address(DiversifierT d) throws ZksnarkException {
    byte[] pkD = new byte[32]; // 32
    if (JLibrustzcash.librustzcashCheckDiversifier(d.getData())) {
      if (!JLibrustzcash.librustzcashIvkToPkd(new IvkToPkdParams(value, d.getData(), pkD))) {
        throw new ZksnarkException("librustzcashIvkToPkd error");
      }
      return Optional.of(new PaymentAddress(d, pkD));
    } else {
      return Optional.empty();
    }
  }
}
