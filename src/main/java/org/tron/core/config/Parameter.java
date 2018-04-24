package org.tron.core.config;

public interface Parameter {

  interface ChainConstant {

    long TRANSFER_FEE = 0; // 1 drop
    long ASSET_ISSUE_FEE = 1024000000; // 1024 trx 1024*10^6
    long VOTE_WITNESS_FEE = 10000; // 10000 drop
    long CREATE_ACCOUNT_FEE = 10000; // 10000 drop
    long WITNESS_PAY_PER_BLOCK = 32000000;  // 32trx
    double SOLIDIFIED_THRESHOLD = 0.7;
    int PRIVATE_KEY_LENGTH = 64;
    int MAX_ACTIVE_WITNESS_NUM = 21;
    int TRXS_SIZE = 2_000_000; // < 2MiB
    int BLOCK_PRODUCED_INTERVAL = 5000; //ms,produce block period, must be divisible by 60. millisecond
  }

  interface NodeConstant {

    long SYNC_RETURN_BATCH_NUM = 1000;
    long SYNC_FETCH_BATCH_NUM = 500;
    long MAX_BLOCKS_IN_PROCESS = 400;
    long MAX_BLOCKS_ALREADY_FETCHED = 800;
    long MAX_BLOCKS_SYNC_FROM_ONE_PEER = 200;
    long SYNC_CHAIN_LIMIT_NUM = 500;

  }


  interface NetConstants {
    long ADV_TIME_OUT = 20000L;
    long SYNC_TIME_OUT = 5000L;
    long HEAD_NUM_MAX_DELTA = 1000L;
    long HEAD_NUM_CHECK_TIME = 60000L;
    long MAX_INVENTORY_SIZE_IN_MINUTES = 2L;
  }

  interface CatTransactionStatus {
    String VALIDATE_SIGANATURE = "The siganature is not validated.";
    String VALIDATE_MERKLER = "The merkler root doesn't match it.";
    String VALIDATE_WITNESS_SCHEDULE = "Witness schedule is not validated.";
    String LOWER_BLOCK = "Lower block.";
    String SWITCH_FORK = "Switch fork.";
    String REVOKING_STORE_ERROR = "Revoking store illegal state exception.";
    String GET_WITNESS_ERROR_SLOT = "CurrentSlot should be positive.";
    String GET_WITNESS_ERROR_NULL = "Active Witnesses is null.";
    String UNLINKED_BLOCK = "Unlinked block.";
    String UPDATE_LATEST_SOLIDIFIED_BLOCK_ERROR = "Update latest solidified block error.";
    String TRANSACTION_VALIDATE_SIGNATURE_ERROR = "Miss sig or contract.";
    String BLOCK_VALIDATE_ERROR = "Block validate signature error.";
    String BAD_BLOCK_EXCEPTION = "Bad block exception.";
    String TRON_EXCEPTION = "TRON exception.";
    String TRAITOR_PEER_EXCEPTION = "Traitor peer exception.";
    String BAD_TRANSACTION_EXCEPTION = "Bad transaction exception.";
    String ON_HANDLE_CHAIN_INVENTORY_MESSAGE_EXCEPTION = "On handle chain inventory message exception.";
  }


}
