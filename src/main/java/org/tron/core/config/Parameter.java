package org.tron.core.config;

public interface Parameter {

  interface ChainConstant {

    long TRANSFER_FEE = 1; // 1 drop
    long ASSET_ISSUE_FEE = 100000; // 100000 drop
    long VOTE_WITNESS = 10000; // 10000 drop
  }

  interface NodeConstant {
    long SYNC_FETCH_BATCH_NUM = 1000;
    long SYNC_CHAIN_LIMIT_NUM = 500;
  }


}
