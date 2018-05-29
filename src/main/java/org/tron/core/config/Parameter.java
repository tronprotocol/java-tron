package org.tron.core.config;

public interface Parameter {

  interface ChainConstant {

    long TRANSFER_FEE = 0; // free
    long ASSET_ISSUE_FEE = 1024000000; // 1024 trx 1024*10^6
    long VOTE_WITNESS_FEE = 10000; // 10000 drop
    long CREATE_ACCOUNT_FEE = 10000; // 10000 drop
    long WITNESS_PAY_PER_BLOCK = 32000000;  // 32trx
    long WITNESS_STANDBY_ALLOWANCE = 230_400_000_000L;// 6 * 1200 * 32000000
    int WITNESS_STANDBY_LENGTH = 127;
    double SOLIDIFIED_THRESHOLD = 0.7;
    int PRIVATE_KEY_LENGTH = 64;
    int MAX_ACTIVE_WITNESS_NUM = 27;
    //int TRXS_SIZE = 2_000_000; // < 2MiB
    int BLOCK_SIZE = 2_000_000;
    int BLOCK_PRODUCED_INTERVAL = 3000; //ms,produce block period, must be divisible by 60. millisecond
    long CLOCK_MAX_DELAY = 3600000; // 3600 * 1000 ms
    double BLOCK_PRODUCED_TIME_OUT = 0.75;
    long BATCH_FETCH_RESPONSE_SIZE = 1000; //for each inventory message from peer, the max count of fetch inv message
    long PRECISION = 1000_000;
    long ONE_DAY_NET_LIMIT = 57_600_000_000L;
    long WINDOW_SIZE_MS = 24 * 3600 * 1000L;
    long CREATE_NEW_ACCOUNT_COST = 10000;
  }

  interface NodeConstant {

    long SYNC_RETURN_BATCH_NUM = 1000;
    long SYNC_FETCH_BATCH_NUM = 2000;
    long MAX_BLOCKS_IN_PROCESS = 400;
    long MAX_BLOCKS_ALREADY_FETCHED = 800;
    long MAX_BLOCKS_SYNC_FROM_ONE_PEER = 1000;
    long SYNC_CHAIN_LIMIT_NUM = 500;
    int MAX_TRANSACTION_PENDING = 2000;
  }

  interface NetConstants {

    long GRPC_IDLE_TIME_OUT = 60000L;
    long ADV_TIME_OUT = 20000L;
    long SYNC_TIME_OUT = 5000L;
    long HEAD_NUM_MAX_DELTA = 1000L;
    long HEAD_NUM_CHECK_TIME = 60000L;
    int MAX_INVENTORY_SIZE_IN_MINUTES = 2;
    long NET_MAX_TRX_PER_SECOND = 1000L;
    long MAX_TRX_PER_PEER = 200L;
    int NET_MAX_INV_SIZE_IN_MINUTES = 2;
    int MSG_CACHE_DURATION_IN_BLOCKS = 5;
  }

  interface DatabaseConstants {

    int TRANSACTIONS_COUNT_LIMIT_MAX = 1000;
  }
}
