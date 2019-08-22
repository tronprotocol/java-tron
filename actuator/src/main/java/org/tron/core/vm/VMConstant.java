package org.tron.core.vm;

public class VMConstant {

  public static final double TX_CPU_LIMIT_DEFAULT_RATIO = 1.0;

  public static final String REASON_ALREADY_TIME_OUT = "Haven Time Out";
  public static final int CONTRACT_NAME_LENGTH = 32;
  public static final int MIN_TOKEN_ID = 1000_000;
  // Numbers
  public static final int ONE_HUNDRED = 100;
  public static final int ONE_THOUSAND = 1000;
  public static final long SUN_PER_ENERGY = 100; // 1 us = 100 DROP = 100 * 10^-6 TRX
  public static final long ENERGY_LIMIT_IN_CONSTANT_TX = 3_000_000L; // ref: 1 us = 1 energy


  private VMConstant() {
  }
}
