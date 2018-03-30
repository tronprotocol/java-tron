package org.tron.core.config;

public interface Parameter {

  interface ChainConstant {

    long TRANSFER_FEE = 0; // 1 drop
    long ASSET_ISSUE_FEE = 1000000000; // 1000 trx 10^9
    long VOTE_WITNESS_FEE = 10000; // 10000 drop
    long CREATE_ACCOUNT_FEE = 10000; // 10000 drop
    long WITNESS_PAY_PER_BLOCK = 32000000;  // 3trx
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
  }



}
