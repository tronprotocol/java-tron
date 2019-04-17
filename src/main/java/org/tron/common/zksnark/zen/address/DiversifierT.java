package org.tron.common.zksnark.zen.address;

import lombok.Getter;
import lombok.Setter;

public class DiversifierT {

  public static int ZC_DIVERSIFIER_SIZE = 11;

  @Setter
  @Getter
  byte[] data;

  public DiversifierT() {
    data = new byte[ZC_DIVERSIFIER_SIZE];
  }
}
