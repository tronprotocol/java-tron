package org.tron.core.config;

public interface Parameter {

  interface ChainConstant {

    long TRANSFER_FEE = 0; // 1 drop
    long ASSET_ISSUE_FEE = 1024000000; // 1024 trx 1024*10^6
    long VOTE_WITNESS_FEE = 10000; // 10000 drop
    long CREATE_ACCOUNT_FEE = 10000; // 10000 drop
    long WITNESS_PAY_PER_BLOCK = 32000000;  // 32trx
    int BLOCK_PRODUCED_INTERVAL = 3; // 3sec

    double SOLIDIFIED_THRESHOLD = 0.3;
  }

  interface NodeConstant {

    long SYNC_FETCH_BATCH_NUM = 1000;
    long SYNC_CHAIN_LIMIT_NUM = 500;

  }

  interface BlockConstant {
    long BLOCK_INTERVAL = 5000L;
  }

  interface NetConstants {
    long ADV_TIME_OUT = 1000L;
    long SYNC_TIME_OUT = 5000L;
    long HEAD_NUM_MAX_DELTA = 1000L;
    long HEAD_NUM_CHECK_TIME = 60000L;
    long MAX_INVENTORY_SIZE_IN_MINUTES = 2L;
  }



}
