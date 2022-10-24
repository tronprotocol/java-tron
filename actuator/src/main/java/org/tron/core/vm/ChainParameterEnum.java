package org.tron.core.vm;

import java.util.function.Function;
import org.tron.core.vm.repository.Repository;

public enum ChainParameterEnum {

  INVALID_PARAMETER_KEY(0, ignored -> 0L),

  TOTAL_NET_LIMIT(1, repository -> repository.getDynamicPropertiesStore().getTotalNetLimit()),

  TOTAL_NET_WEIGHT(2, Repository::getTotalNetWeight),

  TOTAL_ENERGY_CURRENT_LIMIT(3,
      repository -> repository.getDynamicPropertiesStore().getTotalEnergyCurrentLimit()),

  TOTAL_ENERGY_WEIGHT(4, Repository::getTotalEnergyWeight),

  UNFREEZE_DELAY_DAYS(5,
      repository -> repository.getDynamicPropertiesStore().getUnfreezeDelayDays()),

  ;

  private final long code;

  private final Function<Repository, Long> action;

  ChainParameterEnum(long code, Function<Repository, Long> action) {
    this.code = code;
    this.action = action;
  }

  public static ChainParameterEnum fromCode(long code) {
    for (ChainParameterEnum each : values()) {
      if (each.code == code) {
        return each;
      }
    }

    return INVALID_PARAMETER_KEY;
  }

  public long getCode() {
    return code;
  }

  public Function<Repository, Long> getAction() {
    return action;
  }
}
