package org.tron.common.utils;

import lombok.Getter;
import lombok.Setter;

public class DBConfig {

  //Odyssey3.2 hard fork -- ForkBlockVersionConsts.ENERGY_LIMIT
  @Setter
  @Getter
  private static boolean ENERGY_LIMIT_HARD_FORK = false;
}
