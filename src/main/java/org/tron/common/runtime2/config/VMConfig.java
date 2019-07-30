package org.tron.common.runtime2.config;

import lombok.Getter;
import lombok.Setter;

public class VMConfig {
  public static final int CONTRACT_NAME_LENGTH = 32;
  public static final int MIN_TOKEN_ID = 1000_000;

  // Numbers
  public static final int ONE_HUNDRED = 100;
  public static final int ONE_THOUSAND = 1000;

  @Getter
  @Setter
  private int maxFeeLimit;
  @Getter
  @Setter
  private boolean vmTraceCompressed;
  @Getter
  @Setter
  private boolean vmTrace;
  @Getter
  @Setter
  private boolean switchVm2;
  @Getter
  @Setter
  private boolean eventPluginLoaded;
  @Getter
  @Setter
  private long maxCpuTimeOfOneTx;
  @Getter
  @Setter
  private double minTimeRatio;
  @Getter
  @Setter
  private double maxTimeRatio;
}
