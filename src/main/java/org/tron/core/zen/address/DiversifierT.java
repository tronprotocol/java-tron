package org.tron.core.zen.address;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.zksnark.Librustzcash;
import org.tron.core.Constant;

@AllArgsConstructor
public class DiversifierT {

  @Setter
  @Getter
  public byte[] data = new byte[Constant.ZC_DIVERSIFIER_SIZE];

  public DiversifierT() {

  }

  public DiversifierT random() {
    byte[] d;
    while (true) {
      d = org.tron.keystore.Wallet.generateRandomBytes(Constant.ZC_DIVERSIFIER_SIZE);
      if (Librustzcash.librustzcashCheckDiversifier(d)) {
        break;
      }
    }
    this.data = d;
    return this;
  }
}
