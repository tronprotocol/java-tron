package org.tron.common.zksnark.sapling.address;

import lombok.Getter;
import lombok.Setter;

public class DiversifierT {

  public static int ZC_DIVERSIFIER_SIZE = 11;

  @Setter
  @Getter
  byte[] data; // ZC_DIVERSIFIER_SIZE

  // typedef array<char, ZC_DIVERSIFIER_SIZE> DiversifierT;
}
