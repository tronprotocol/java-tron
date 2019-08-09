package org.tron.core.config;

import lombok.Getter;

public class Parameter {
  public class ChainConstant {

    public static final long TRANSFER_FEE = 0; // free
    public static final int MAX_VOTE_NUMBER = 30;
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
    ALLOW_SHIELDED_TRANSACTION, // 1, 27
    SHIELDED_TRANSACTION_FEE, // 28
    ALLOW_TVM_SOLIDITY_059, // 1, 29
  }

  @Deprecated
  public class ForkBlockVersionConsts {

    public static final int START_NEW_TRANSACTION = 4;
    public static final int ENERGY_LIMIT = 5;
  }
}
