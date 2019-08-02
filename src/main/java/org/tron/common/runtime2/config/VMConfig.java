package org.tron.common.runtime2.config;

import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

public class VMConfig {
  public static final int CONTRACT_NAME_LENGTH = 32;
  public static final int MIN_TOKEN_ID = 1000_000;

  // Numbers
  public static final int ONE_HUNDRED = 100;
  public static final int ONE_THOUSAND = 1000;


  //Max size for stack checks
  public static final int MAX_STACK_SIZE = 1024;
  // 3MB
  public static final BigInteger MEM_LIMIT = BigInteger.valueOf(3L * 1024 * 1024);

  public static final int MAX_CALLDEEP_DEPTH = 64;


  /*****************/
  /***  msg data ***/
  /*****************/
  /* NOTE: In the protocol there is no restriction on the maximum message data,
   * However msgData here is a byte[] and this can't hold more than 2^32-1
   */
  public static final BigInteger MAX_MSG_DATA = BigInteger.valueOf(Integer.MAX_VALUE);

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
