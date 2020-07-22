package org.tron.core.config;

public class Parameter {

  public class ChainConstant {

    public static final int WITNESS_STANDBY_LENGTH = 127;
    public static final int SOLIDIFIED_THRESHOLD = 70; // 70%
    public static final int PRIVATE_KEY_LENGTH = 64;
    public static final int MAX_ACTIVE_WITNESS_NUM = 27;
    public static final int BLOCK_SIZE = 2_000_000;
    public static final long CLOCK_MAX_DELAY = 3600000; // 3600 * 1000 ms
    public static final int BLOCK_PRODUCED_TIME_OUT = 50; // 50%
    public static final long PRECISION = 1_000_000;
    public static final long WINDOW_SIZE_MS = 24 * 3600 * 1000L;
    public static final long MS_PER_YEAR = 365 * 24 * 3600 * 1000L;
    public static final long MAINTENANCE_SKIP_SLOTS = 2;
    public static final int SINGLE_REPEAT = 1;
    public static final int BLOCK_FILLED_SLOTS_NUMBER = 128;
    public static final int MAX_FROZEN_NUMBER = 1;
    public static final int BLOCK_VERSION = 17;
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
}
