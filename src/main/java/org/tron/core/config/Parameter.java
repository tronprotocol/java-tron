package org.tron.core.config;

import lombok.Getter;

public class Parameter {

  public class ChainConstant {
    public static final long TRANSFER_FEE = 0; // free
    public static final int WITNESS_STANDBY_LENGTH = 127;
    public static final int SOLIDIFIED_THRESHOLD = 70; // 70%
    public static final int PRIVATE_KEY_LENGTH = 64;
    public static final int MAX_ACTIVE_WITNESS_NUM = 27;
    public static final int BLOCK_SIZE = 2_000_000;
    public static final int BLOCK_PRODUCED_INTERVAL = 3000; //ms,produce block period, must be divisible by 60. millisecond
    public static final long CLOCK_MAX_DELAY = 3600000; // 3600 * 1000 ms
    public static final int BLOCK_PRODUCED_TIME_OUT = 50; // 50%
    public static final long PRECISION = 1_000_000;
    public static final long WINDOW_SIZE_MS = 24 * 3600 * 1000L;
    public static final long MS_PER_YEAR = 365 * 24 * 3600 * 1000L;
    public static final long MAINTENANCE_SKIP_SLOTS = 2;
    public static final int SINGLE_REPEAT = 1;
    public static final int BLOCK_FILLED_SLOTS_NUMBER = 128;
    public static final int MAX_VOTE_NUMBER = 30;
    public static final int MAX_FROZEN_NUMBER = 1;
    public static final int BLOCK_VERSION = 8;
  }

  public class NodeConstant {
    public static final long SYNC_RETURN_BATCH_NUM = 1000;
    public static final long SYNC_FETCH_BATCH_NUM = 2000;
    public static final long MAX_BLOCKS_IN_PROCESS = 400;
    public static final long MAX_BLOCKS_ALREADY_FETCHED = 800;
    public static final long MAX_BLOCKS_SYNC_FROM_ONE_PEER = 1000;
    public static final long SYNC_CHAIN_LIMIT_NUM = 500;
    public static final int MAX_TRANSACTION_PENDING = 2000;
    public static final int MAX_HTTP_CONNECT_NUMBER = 50;
  }

  public class NetConstants {
    public static final long GRPC_IDLE_TIME_OUT = 60000L;
    public static final long ADV_TIME_OUT = 20000L;
    public static final long SYNC_TIME_OUT = 5000L;
    public static final long HEAD_NUM_MAX_DELTA = 1000L;
    public static final long HEAD_NUM_CHECK_TIME = 60000L;
    public static final int MAX_INVENTORY_SIZE_IN_MINUTES = 2;
    public static final long NET_MAX_TRX_PER_SECOND = 700L;
    public static final long MAX_TRX_PER_PEER = 200L;
    public static final int NET_MAX_INV_SIZE_IN_MINUTES = 2;
    public static final int MSG_CACHE_DURATION_IN_BLOCKS = 5;
    public static final int MAX_BLOCK_FETCH_PER_PEER = 100;
    public static final int MAX_TRX_FETCH_PER_PEER = 1000;
  }

  public class DatabaseConstants {
    public static final int TRANSACTIONS_COUNT_LIMIT_MAX = 1000;
    public static final int ASSET_ISSUE_COUNT_LIMIT_MAX = 1000;
    public static final int PROPOSAL_COUNT_LIMIT_MAX = 1000;
    public static final int EXCHANGE_COUNT_LIMIT_MAX = 1000;
  }

  public class AdaptiveResourceLimitConstants {
    public static final int CONTRACT_RATE_NUMERATOR = 99;
    public static final int CONTRACT_RATE_DENOMINATOR = 100;
    public static final int EXPAND_RATE_NUMERATOR = 1000;
    public static final int EXPAND_RATE_DENOMINATOR = 999;
    public static final int PERIODS_MS = 60_000;
    public static final int LIMIT_MULTIPLIER = 1000; //s
  }

  public enum ChainParameters {
    MAINTENANCE_TIME_INTERVAL, //ms  ,0
    ACCOUNT_UPGRADE_COST, //drop ,1
    CREATE_ACCOUNT_FEE, //drop ,2
    TRANSACTION_FEE, //drop ,3
    ASSET_ISSUE_FEE, //drop ,4
    WITNESS_PAY_PER_BLOCK, //drop ,5
    WITNESS_STANDBY_ALLOWANCE, //drop ,6
    CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT, //drop ,7
    CREATE_NEW_ACCOUNT_BANDWIDTH_RATE, // 1 ~ ,8
    ALLOW_CREATION_OF_CONTRACTS, // 0 / >0 ,9
    REMOVE_THE_POWER_OF_THE_GR,  // 1 ,10
    ENERGY_FEE, // drop, 11
    EXCHANGE_CREATE_FEE, // drop, 12
    MAX_CPU_TIME_OF_ONE_TX, // ms, 13
    ALLOW_UPDATE_ACCOUNT_NAME, // 1, 14
    ALLOW_SAME_TOKEN_NAME, // 1, 15
    ALLOW_DELEGATE_RESOURCE, // 0, 16
    TOTAL_ENERGY_LIMIT, // 50,000,000,000, 17
    ALLOW_TVM_TRANSFER_TRC10, // 1, 18
    TOTAL_CURRENT_ENERGY_LIMIT, // 50,000,000,000, 19
    ALLOW_MULTI_SIGN, // 1, 20
    ALLOW_ADAPTIVE_ENERGY, // 1, 21
    UPDATE_ACCOUNT_PERMISSION_FEE, // 100, 22
    MULTI_SIGN_FEE, // 1, 23
    ALLOW_PROTO_FILTER_NUM, // 1, 24
    ALLOW_ACCOUNT_STATE_ROOT, // 1, 25
    ALLOW_TVM_CONSTANTINOPLE, // 1, 26
  }

  @Deprecated
  public class ForkBlockVersionConsts {
    public static final int START_NEW_TRANSACTION = 4;
    public static final int ENERGY_LIMIT = 5;
  }

  public enum ForkBlockVersionEnum {
    ENERGY_LIMIT(5),
    VERSION_3_2_2(6),
    VERSION_3_5(7),
    VERSION_3_6(8);

    @Getter
    private int value;

    ForkBlockVersionEnum(int value) {
      this.value = value;
    }
  }

}
