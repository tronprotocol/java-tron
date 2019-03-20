package org.tron.common.zksnark.sapling.address;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class PaymentAddress {

  // diversified payment address addrd = (d, pkd)
  @Setter
  @Getter
  DiversifierT d;
  @Setter
  @Getter
  byte[] pkD; // 256

  public byte[] encode() {

    byte[] mBytes = new byte[11 + 32];

    System.arraycopy(d.getData(), 0, mBytes, 0, 11);
    System.arraycopy(pkD, 0, mBytes, 1, 32);
    return mBytes;
  }

  public static PaymentAddress decode(byte[] data) {

    DiversifierT d = new DiversifierT();
    byte[] pkD = new byte[32];
    System.arraycopy(data, 0, d.data, 0, 11);
    System.arraycopy(data, 11, pkD, 0, 32);

    return new PaymentAddress(d, pkD);
  }

  //todo:
  public boolean equals(PaymentAddress other) {

    return false;
  }
}
