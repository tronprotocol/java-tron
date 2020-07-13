package org.tron.common.parameter;


import lombok.Getter;
import lombok.Setter;

public class CommonParameter {

  public static CommonParameter PARAMETER = new CommonParameter();

  @Getter
  @Setter
  public long oldSolidityBlockNum = -1;

  public static CommonParameter getInstance() {
    return PARAMETER;
  }
}
