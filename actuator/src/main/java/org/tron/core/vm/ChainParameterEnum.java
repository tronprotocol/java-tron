package org.tron.core.vm;

import java.util.function.Function;

import org.tron.core.store.DynamicPropertiesStore;

public enum ChainParameterEnum {

  INVALID_PARAMETER_KEY(0, (any) -> 0L),

  TOTAL_ENERGY_CURRENT_LIMIT(1, DynamicPropertiesStore::getTotalEnergyCurrentLimit),

  TOTAL_ENERGY_WEIGHT(2, DynamicPropertiesStore::getTotalEnergyWeight),

  UNFREEZE_DELAY_DAYS(3, DynamicPropertiesStore::getUnfreezeDelayDays),

  ;

  private final long code;

  private final Function<DynamicPropertiesStore, Long> action;

  ChainParameterEnum(long code, Function<DynamicPropertiesStore, Long> action) {
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

  public Function<DynamicPropertiesStore, Long> getAction() {
    return action;
  }
}
