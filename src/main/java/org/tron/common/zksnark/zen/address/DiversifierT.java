package org.tron.common.zksnark.zen.address;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.zksnark.zen.Constants;

@AllArgsConstructor
public class DiversifierT {

  @Setter
  @Getter
  public byte[] data = new byte[Constants.ZC_DIVERSIFIER_SIZE];

  public DiversifierT() {

  }
}
