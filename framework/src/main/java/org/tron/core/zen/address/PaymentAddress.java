package org.tron.core.zen.address;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class PaymentAddress {

  // diversified payment address addrd = (d, pkd)
  @Setter
  @Getter
  private DiversifierT d;
  @Setter
  @Getter
  private byte[] pkD; // 256

  public static PaymentAddress decode(byte[] data) {
    DiversifierT d = new DiversifierT();
    byte[] pkD = new byte[32];
    System.arraycopy(data, 0, d.getData(), 0, 11);
    System.arraycopy(data, 11, pkD, 0, 32);
    return new PaymentAddress(d, pkD);
  }

  public byte[] encode() {
    byte[] mBytes = new byte[11 + 32];
    System.arraycopy(d.getData(), 0, mBytes, 0, 11);
    System.arraycopy(pkD, 0, mBytes, 11, 32);
    return mBytes;
  }
}
