package org.tron.core.zen.address;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.tron.core.Constant;

@AllArgsConstructor
public class DiversifierT {

  @Setter
  @Getter
  public byte[] data = new byte[Constant.ZC_DIVERSIFIER_SIZE];

  public DiversifierT() {

  }
}
