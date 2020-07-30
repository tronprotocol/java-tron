package org.tron.core.config.args;

import lombok.Getter;

public class Parameter {

  public enum ForkBlockVersionEnum {
    ENERGY_LIMIT(5, 0L, 0),
    VERSION_3_2_2(6, 0L, 0),
    VERSION_3_5(7, 0L, 0),
    VERSION_3_6(8, 0L, 0),
    VERSION_3_6_5(9, 0L, 0),
    VERSION_3_6_6(10, 0L, 0),
    VERSION_4_0(16, 0L, 0),
    VERSION_4_0_1(17, 1596780000000L, 22);//GMT 2020-08-07 06:00:00

    @Getter
    private int value;
    @Getter
    private long hardForkTime;
    @Getter
    private int hardForkCount;

    ForkBlockVersionEnum(int value, long hardForkTime, int hardForkCount) {
      this.value = value;
      this.hardForkTime = hardForkTime;
      this.hardForkCount = hardForkCount;
    }

    public static ForkBlockVersionEnum getForkBlockVersionEnum(int value) {
      for (ForkBlockVersionEnum versionEnum : values()) {
        if (versionEnum.getValue() == value) {
          return versionEnum;
        }
      }
      return null;
    }
  }

  public class ChainConstant {

    public static final long TRANSFER_FEE = 0; // free
    public static final int BLOCK_PRODUCED_INTERVAL = 3000; //ms,produce block period, must be divisible by 60. millisecond
    public static final int MAX_VOTE_NUMBER = 30;
    public static final long WINDOW_SIZE_MS = 24 * 3600 * 1000L;
    public static final long PRECISION = 1_000_000;
    public static final int MAX_ACTIVE_WITNESS_NUM = 27;
  }

  @Deprecated
  public class ForkBlockVersionConsts {

    public static final int START_NEW_TRANSACTION = 4;
    public static final int ENERGY_LIMIT = 5;
  }

}