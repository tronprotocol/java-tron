package org.tron.core.config.args;

import lombok.Getter;

public class Parameter {
  public enum ForkBlockVersionEnum {
    ENERGY_LIMIT(5),
    VERSION_3_2_2(6),
    VERSION_3_5(7),
    VERSION_3_6(8),
    VERSION_4_0(9);

    @Getter
    private int value;

    ForkBlockVersionEnum(int value) {
      this.value = value;
    }
  }

  @Deprecated
  public class ForkBlockVersionConsts {

    public static final int START_NEW_TRANSACTION = 4;
    public static final int ENERGY_LIMIT = 5;
  }
}
