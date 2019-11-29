# Tron Protocol 

## Overview 

This is the description of  Google Protobuf implementation of Tron's protocol.

## Contents 

#### 1.[Account](#account)

#### 2.[Witness](#witness)

#### 3.[Block](#block)

#### 4.[Transaction](#trans)

#### 5.[Contract](#contract)

#### 6.[Network](#net)

## Protocols

###<span id="account"> 1.Account</span>

Account and account-related messages.

- Tron has 3 `types` of account: Normal, AssetIssue, Contract

```java
enum AccountType {
  Normal = 0;
  AssetIssue = 1;
  Contract = 2;
}
```

- message `Account` has multiple attributes and 2 nested messages: 

  message `Frozen`:

  ```java
  message Frozen {
    int64 frozen_balance = 1; // the frozen trx balance
    int64 expire_time = 2; // the expire time
  }
  ```

  message `AccountResource`:

  ```java
  message AccountResource {
    // energy resource, get from frozen
    int64 energy_usage = 1;
    // the frozen balance for energy
    Frozen frozen_balance_for_energy = 2;
    int64 latest_consume_time_for_energy = 3;
  
    //Frozen balance provided by other accounts to this account
    int64 acquired_delegated_frozen_balance_for_energy = 4;
    //Frozen balances provided to other accounts
    int64 delegated_frozen_balance_for_energy = 5;
  
    // storage resource, get from market
    int64 storage_limit = 6;
    int64 storage_usage = 7;
    int64 latest_exchange_storage_time = 8;
  
  }
  ```

  `account_name`:

  `type`:

  `address`:

  `account_id`:

  `balance`:

  `votes`:

  `asset`:

  `assetV2`:

  `frozen`:

  `net_usage`:

  `acquired_delegated_fronzen_balance_for_bandwidth`:

  `delegated_frozen_balance_for_bandwidth`:

  `create_time`:

  `latest_opration_time`:

  `allowance`:

  `latest_withdrew_time`:

  `code`:

  `is_witness`:

  `is_committee`:

  `frozen_supply`:

  `asset_issued_name`:

  `asset_issued_ID`:

  `latest_asset_operation_time`:

  `latest_asset_operation_timeV2`:

  `free_net_usage`:

  `free_asset_net_usage`:

  `free_asset_net_usageV2`:

  `latest_consume_time`:

  `latest_consume_free_time`:

  ```java
  message Account {
    /* frozen balance */
    message Frozen {
      int64 frozen_balance = 1; // the frozen trx balance
      int64 expire_time = 2; // the expire time
    }
    // account nick name
    bytes account_name = 1;
    AccountType type = 2;
    // the create address
    bytes address = 3;
    // the trx balance
    int64 balance = 4;
    // the votes
    repeated Vote votes = 5;
    // the other asset owned by this account
    map<string, int64> asset = 6;
  
    // the other asset owned by this account，key is assetId
    map<string, int64> assetV2 = 56;
  
    // the frozen balance for bandwidth
    repeated Frozen frozen = 7;
    // bandwidth, get from frozen
    int64 net_usage = 8;
    //Frozen balance provided by other accounts to this account
    int64 acquired_delegated_frozen_balance_for_bandwidth = 41;
    //Freeze and provide balances to other accounts
    int64 delegated_frozen_balance_for_bandwidth = 42;
  
    // this account create time
    int64 create_time = 0x09;
    // this last operation time, including transfer, voting and so on. //FIXME fix grammar
    int64 latest_opration_time = 10;
    // witness block producing allowance
    int64 allowance = 0x0B;
    // last withdraw time
    int64 latest_withdraw_time = 0x0C;
    // not used so far
    bytes code = 13;
    bool is_witness = 14;
    bool is_committee = 15;
    // frozen asset(for asset issuer)
    repeated Frozen frozen_supply = 16;
    // asset_issued_name
    bytes asset_issued_name = 17;
    bytes asset_issued_ID = 57;
    map<string, int64> latest_asset_operation_time = 18;
    map<string, int64> latest_asset_operation_timeV2 = 58;
    int64 free_net_usage = 19;
    map<string, int64> free_asset_net_usage = 20;
    map<string, int64> free_asset_net_usageV2 = 59;
    int64 latest_consume_time = 21;
    int64 latest_consume_free_time = 22;
  
    // the identity of this account, case insensitive
    bytes account_id = 23;
  
    message AccountResource {
      // energy resource, get from frozen
      int64 energy_usage = 1;
      // the frozen balance for energy
      Frozen frozen_balance_for_energy = 2;
      int64 latest_consume_time_for_energy = 3;
  
      //Frozen balance provided by other accounts to this account
      int64 acquired_delegated_frozen_balance_for_energy = 4;
      //Frozen balances provided to other accounts
      int64 delegated_frozen_balance_for_energy = 5;
  
      // storage resource, get from market
      int64 storage_limit = 6;
      int64 storage_usage = 7;
      int64 latest_exchange_storage_time = 8;
  
    }
    AccountResource account_resource = 26;
    bytes codeHash = 30;
    Permission owner_permission = 31;
    Permission witness_permission = 32;
    repeated Permission active_permission = 33;
  }
  ```

  - message `Vote`

    `vote_address`:

    `vote_count`:

    ```java
    message Vote {
      // the super rep address
      bytes vote_address = 1;
      // the vote num to this super rep.
      int64 vote_count = 2;
    }
    ```

  - Message `AccountId`

    `name`:

    `address`:

    ```java
    message AccountId {
      bytes name = 1;
      bytes address = 2;
    }
    ```

  - Message

    

    

### <span id="witness"> 2.Witness</span>

Witness and witness-related messages.

- message `Witness`

  `address`:

  `voteCount`:

  `pubkey`:

  `url`:

  `totalProduce`:

  `totalMissed`:

  `latestBlockNum`:

  `latestSlotNum`:

  `isJobs`:

  ```java
  message Witness {
    bytes address = 1;
    int64 voteCount = 2;
    bytes pubKey = 3;
    string url = 4;
    int64 totalProduced = 5;
    int64 totalMissed = 6;
    int64 latestBlockNum = 7;
    int64 latestSlotNum = 8;
    bool isJobs = 9;
  }
  ```

- 

### <span id="block"> 3.Block</span>

- message `Block`

  `transaction`: refer to [`Transaction`](#trans).

  `block_header`:

  ```java
  message Block {
    repeated Transaction transactions = 1;
    BlockHeader block_header = 2;
  }
  ```

- Message `BlockHeader`

  message `BlockHeader` has multiple attributes and 1 nested message.

  message `raw`:

  ​    `timestamp`:

  ​    `txTrieRoot`:

  ​    `parentHash`:

  ​    `number`:

  ​    `witness_id`:

  ​    `witness_address`:

  ​    `version`:

  ​    `accountStateRoot`:

  ```java
  message raw {
    int64 timestamp = 1;
    bytes txTrieRoot = 2;
    bytes parentHash = 3;
    //bytes nonce = 5;
    //bytes difficulty = 6;
    int64 number = 7;
    int64 witness_id = 8;
    bytes witness_address = 9;
    int32 version = 10;
    bytes accountStateRoot = 11;
  }
  ```

  `raw_data`:

  `witness_signature`:

  ```java
  message BlockHeader {
    message raw {
      int64 timestamp = 1;
      bytes txTrieRoot = 2;
      bytes parentHash = 3;
      //bytes nonce = 5;
      //bytes difficulty = 6;
      int64 number = 7;
      int64 witness_id = 8;
      bytes witness_address = 9;
      int32 version = 10;
      bytes accountStateRoot = 11;
    }
    raw raw_data = 1;
    bytes witness_signature = 2;
  }
  ```

- 

### <span id="trans"> 4.Transaction</span>

Transaction and transaction-related messages.

- Any behaviors which consume energy are regarded as transaction.

  

- message `TXInput` has multiple attributes and 1 nested message

  message `raw`:

  ​    `txID`:

  ​    `vout`:

  ​    `pubKey`:   

  ```java
  message raw {
    bytes txID = 1;
    int64 vout = 2;
    bytes pubKey = 3;
  }
  ```

  `raw_data`:

  `signature`:

  ```java
  message TXInput {
    message raw {
      bytes txID = 1;
      int64 vout = 2;
      bytes pubKey = 3;
    }
    raw raw_data = 1;
    bytes signature = 4;
  }
  ```

- message `txOutput`

  `value`:

  `pubKeyHash`:

  ```java
  message TXOutput {
    int64 value = 1;
    bytes pubKeyHash = 2;
  }
  ```

- message `TransactionRet`

  `blockNumber`:

  `blockTimeStamp`:

  `transactionInfo`:

  ```java
  message TransactionRet {
    int64 blockNumber = 1;
    int64 blockTimeStamp = 2;
    repeated TransactionInfo transactioninfo = 3;
  }
  ```

  - message `TransactionSign`

    `transaction`:

    `privateKey`:

    ```java
    message TransactionSign {
      Transaction transaction = 1;
      bytes privateKey = 2;
    }
    ```

  - message `ResourceReceipt`

    `energy_usage`:

    `energy_fee`:

    `origin_energy_usage`:

    `energy_usage_total`:

    `net_usage`:

    `net_fee`:

    `result`:

    ```java
    message ResourceReceipt {
      int64 energy_usage = 1;
      int64 energy_fee = 2;
      int64 origin_energy_usage = 3;
      int64 energy_usage_total = 4;
      int64 net_usage = 5;
      int64 net_fee = 6;
      Transaction.Result.contractResult result = 7;
    }
    ```

  - Message `InternalTransaction`

    message `InternalTransaction` has multiple attributes and 1 nested message

    message `CallValueInfo`:

    ​    `note`:

    ​    `rejected`:

    ```java
    message CallValueInfo {
      // trx (TBD: or token) value
      int64 callValue = 1;
      // TBD: tokenName, trx should be empty
      string tokenId = 2;
    }
    ```

    `hash`:

    `caller_address`:

    `transferTo_address`:

    `callValueInfo`:

    `note`:

    `rejected`:

    ```java
    message InternalTransaction {
      // internalTransaction identity, the root InternalTransaction hash
      // should equals to root transaction id.
      bytes hash = 1;
      // the one send trx (TBD: or token) via function
      bytes caller_address = 2;
      // the one recieve trx (TBD: or token) via function
      bytes transferTo_address = 3;
      message CallValueInfo {
        // trx (TBD: or token) value
        int64 callValue = 1;
        // TBD: tokenName, trx should be empty
        string tokenId = 2;
      }
      repeated CallValueInfo callValueInfo = 4;
      bytes note = 5;
      bool rejected = 6;
    }
    ```

  - message `Transaction`

    message `Transaction` has multiple attributes and 3 nested messages.

    message `Contract`: refer to [`Contract`](#contract).

    message  `Result`

    ​    enum `code`:

    ```java
    enum code {
      SUCESS = 0;
      FAILED = 1;
    }
    ```

    ​    enum `contractResult`: refer to [`Contract`](#contract).

    ​    `fee`:

    ​    `ret`:

    ​    `contractRet`:

    ​    `assetIssueID`:

    ​    `withdraw_amount`:

    ​    `unfreeze_amount`:

    ​    `exchange_received_amount`:

    ​    `exchange_inject_another_amount`:

    ​    `exchange_withdraw_another_amount`:

    ​    `exchange_id`:

    ​    `shielded_transaction_fee`:

    ```java
    message Result {
      enum code {
        SUCESS = 0;
        FAILED = 1;
      }
      enum contractResult {
        DEFAULT = 0;
        SUCCESS = 1;
        REVERT = 2;
        BAD_JUMP_DESTINATION = 3;
        OUT_OF_MEMORY = 4;
        PRECOMPILED_CONTRACT = 5;
        STACK_TOO_SMALL = 6;
        STACK_TOO_LARGE = 7;
        ILLEGAL_OPERATION = 8;
        STACK_OVERFLOW = 9;
        OUT_OF_ENERGY = 10;
        OUT_OF_TIME = 11;
        JVM_STACK_OVER_FLOW = 12;
        UNKNOWN = 13;
        TRANSFER_FAILED = 14;
      }
      int64 fee = 1;
      code ret = 2;
      contractResult contractRet = 3;
    
      string assetIssueID = 14;
      int64 withdraw_amount = 15;
      int64 unfreeze_amount = 16;
      int64 exchange_received_amount = 18;
      int64 exchange_inject_another_amount = 19;
      int64 exchange_withdraw_another_amount = 20;
      int64 exchange_id = 21;
      int64 shielded_transaction_fee = 22;
    }
    ```

    message `raw`

    ​    `ref_block_bytes`:

    ​    `ref_block_num`:

    ​    `ref_block_hash`:

    ​    `expiration`:

    ​    `auths`:

    ​    `contract`:

    ​    `timestamp`:

    ​    `fee_limit`:

    ```java
    message raw {
      bytes ref_block_bytes = 1;
      int64 ref_block_num = 3;
      bytes ref_block_hash = 4;
      int64 expiration = 8;
      repeated authority auths = 9;
      // data not used
      bytes data = 10;
      //only support size = 1,  repeated list here for extension
      repeated Contract contract = 11;
      // scripts not used
      bytes scripts = 12;
      int64 timestamp = 14;
      int64 fee_limit = 18;
    }
    ```

    `raw_data`:

    `signature`:

    `ret`:

    ```java
    message Transaction {
      message Contract {
        enum ContractType {
          AccountCreateContract = 0;
          TransferContract = 1;
          TransferAssetContract = 2;
          VoteAssetContract = 3;
          VoteWitnessContract = 4;
          WitnessCreateContract = 5;
          AssetIssueContract = 6;
          WitnessUpdateContract = 8;
          ParticipateAssetIssueContract = 9;
          AccountUpdateContract = 10;
          FreezeBalanceContract = 11;
          UnfreezeBalanceContract = 12;
          WithdrawBalanceContract = 13;
          UnfreezeAssetContract = 14;
          UpdateAssetContract = 15;
          ProposalCreateContract = 16;
          ProposalApproveContract = 17;
          ProposalDeleteContract = 18;
          SetAccountIdContract = 19;
          CustomContract = 20;
          CreateSmartContract = 30;
          TriggerSmartContract = 31;
          GetContract = 32;
          UpdateSettingContract = 33;
          ExchangeCreateContract = 41;
          ExchangeInjectContract = 42;
          ExchangeWithdrawContract = 43;
          ExchangeTransactionContract = 44;
          UpdateEnergyLimitContract = 45;
          AccountPermissionUpdateContract = 46;
          ClearABIContract = 48;
          UpdateBrokerageContract = 49;
          ShieldedTransferContract = 51;
        }
        ContractType type = 1;
        google.protobuf.Any parameter = 2;
        bytes provider = 3;
        bytes ContractName = 4;
        int32 Permission_id = 5;
      }
    
      message Result {
        enum code {
          SUCESS = 0;
          FAILED = 1;
        }
        enum contractResult {
          DEFAULT = 0;
          SUCCESS = 1;
          REVERT = 2;
          BAD_JUMP_DESTINATION = 3;
          OUT_OF_MEMORY = 4;
          PRECOMPILED_CONTRACT = 5;
          STACK_TOO_SMALL = 6;
          STACK_TOO_LARGE = 7;
          ILLEGAL_OPERATION = 8;
          STACK_OVERFLOW = 9;
          OUT_OF_ENERGY = 10;
          OUT_OF_TIME = 11;
          JVM_STACK_OVER_FLOW = 12;
          UNKNOWN = 13;
          TRANSFER_FAILED = 14;
        }
        int64 fee = 1;
        code ret = 2;
        contractResult contractRet = 3;
    
        string assetIssueID = 14;
        int64 withdraw_amount = 15;
        int64 unfreeze_amount = 16;
        int64 exchange_received_amount = 18;
        int64 exchange_inject_another_amount = 19;
        int64 exchange_withdraw_another_amount = 20;
        int64 exchange_id = 21;
        int64 shielded_transaction_fee = 22;
      }
    
      message raw {
        bytes ref_block_bytes = 1;
        int64 ref_block_num = 3;
        bytes ref_block_hash = 4;
        int64 expiration = 8;
        repeated authority auths = 9;
        // data not used
        bytes data = 10;
        //only support size = 1,  repeated list here for extension
        repeated Contract contract = 11;
        // scripts not used
        bytes scripts = 12;
        int64 timestamp = 14;
        int64 fee_limit = 18;
      }
    
      raw raw_data = 1;
      // only support size = 1,  repeated list here for muti-sig extension
      repeated bytes signature = 2;
      repeated Result ret = 5;
    }
    ```

  - message `TransactionInfo`

    message `TransactionInfo` has multiple attributes, a nested enumeration and 1 nested message.

    enum `code`

    ```java
    enum code {
      SUCESS = 0;
      FAILED = 1;
    }
    ```

    message `log`

    ​    `address`:

    ​    `topics`:

    ​    `data`:

    ```java
    message Log {
      bytes address = 1;
      repeated bytes topics = 2;
      bytes data = 3;
    }
    ```

    `id`:

    `fee`:

    `blockNumber`:

    `blockTimeStamp`:

    `contractResult`:

    `contract_address`:

    `receipt`:

    `log`:

    `result`:

    `resMessage`:

    `assetIssueID`:

    `withdraw_amount`:

    `unfreeze_amount`:

    `internal_transactions`:

    `exchange_received_amount`:

    `exchange_inject_another_amount`:

    `exchange_withdraw_another_amount`:

    `exchange_id`:

    `shielded_transaction_fee`:

    ```java
    message TransactionInfo {
      enum code {
        SUCESS = 0;
        FAILED = 1;
      }
      message Log {
        bytes address = 1;
        repeated bytes topics = 2;
        bytes data = 3;
      }
      bytes id = 1;
      int64 fee = 2;
      int64 blockNumber = 3;
      int64 blockTimeStamp = 4;
      repeated bytes contractResult = 5;
      bytes contract_address = 6;
      ResourceReceipt receipt = 7;
      repeated Log log = 8;
      code result = 9;
      bytes resMessage = 10;
    
      string assetIssueID = 14;
      int64 withdraw_amount = 15;
      int64 unfreeze_amount = 16;
      repeated InternalTransaction internal_transactions = 17;
      int64 exchange_received_amount = 18;
      int64 exchange_inject_another_amount = 19;
      int64 exchange_withdraw_another_amount = 20;
      int64 exchange_id = 21;
      int64 shielded_transaction_fee = 22;
    }
    ```

  - message `Transactions`

    `transaction`:

    ```java
    message Transactions {
      repeated Transaction transactions = 1;
    }
    ```

  - message `Authority`

    `account`:

    `permission_name`:

    ```java
    message authority {
      AccountId account = 1;
      bytes permission_name = 2;
    }
    ```

  - message `TXOutputs`

    `outputs`:

    ```java
    message TXOutputs {
      repeated TXOutput outputs = 1;
    }
    ```

  - 

    

    



### <span id="contract"> 5.Contract</span>

Contract and contract-related messages.

- Tron has 33 types of Contracts declared within [`Transaction`](#trans).

- message `Contract`

  enum `ContractType`

  `type`:

  `parameter`:

  `provider`:

  `ContractName`:

  `Permission_id`:

  ```java
  message Contract {
    enum ContractType {
      AccountCreateContract = 0;
      TransferContract = 1;
      TransferAssetContract = 2;
      VoteAssetContract = 3;
      VoteWitnessContract = 4;
      WitnessCreateContract = 5;
      AssetIssueContract = 6;
      WitnessUpdateContract = 8;
      ParticipateAssetIssueContract = 9;
      AccountUpdateContract = 10;
      FreezeBalanceContract = 11;
      UnfreezeBalanceContract = 12;
      WithdrawBalanceContract = 13;
      UnfreezeAssetContract = 14;
      UpdateAssetContract = 15;
      ProposalCreateContract = 16;
      ProposalApproveContract = 17;
      ProposalDeleteContract = 18;
      SetAccountIdContract = 19;
      CustomContract = 20;
      CreateSmartContract = 30;
      TriggerSmartContract = 31;
      GetContract = 32;
      UpdateSettingContract = 33;
      ExchangeCreateContract = 41;
      ExchangeInjectContract = 42;
      ExchangeWithdrawContract = 43;
      ExchangeTransactionContract = 44;
      UpdateEnergyLimitContract = 45;
      AccountPermissionUpdateContract = 46;
      ClearABIContract = 48;
      UpdateBrokerageContract = 49;
      ShieldedTransferContract = 51;
    }
    ContractType type = 1;
    google.protobuf.Any parameter = 2;
    bytes provider = 3;
    bytes ContractName = 4;
    int32 Permission_id = 5;
  }
  ```

- There are 15 types of results while deploying contracts (refer to `Transaction.Result`):

  ```java
  enum contractResult {
    DEFAULT = 0;
    SUCCESS = 1;
    REVERT = 2;
    BAD_JUMP_DESTINATION = 3;
    OUT_OF_MEMORY = 4;
    PRECOMPILED_CONTRACT = 5;
    STACK_TOO_SMALL = 6;
    STACK_TOO_LARGE = 7;
    ILLEGAL_OPERATION = 8;
    STACK_OVERFLOW = 9;
    OUT_OF_ENERGY = 10;
    OUT_OF_TIME = 11;
    JVM_STACK_OVER_FLOW = 12;
    UNKNOWN = 13;
    TRANSFER_FAILED = 14;
  }
  ```

  #### Contract Details

  - message `AccountCreateContract`

    `owner_address`:

    `account_address`:

    `type`:

    ```java
    message AccountCreateContract {
      bytes owner_address = 1;
      bytes account_address = 2;
      AccountType type = 3;
    }
    ```

  - message `TransferContract`

    `owner_address`:

    `to_address`:

    `amount`：

    ```java
    message TransferContract {
        bytes owner_address = 1;
        bytes to_address = 2;
        int64 amount = 3;
    }
    ```

  - message `TransferAssetContract`

    `asset_name`:

    `owner_address`:

    `to_address`:

    `amount`:

    ```java
    message TransferAssetContract {
        bytes asset_name = 1; // this field is token name before the proposal ALLOW_SAME_TOKEN_NAME is active, otherwise it is token id and token is should be in string format.
        bytes owner_address = 2;
        bytes to_address = 3;
        int64 amount = 4;
    }
    ```

  - message `VoteAssetContract`

    `owner_address`:

    `vote_address`:

    `support`:

    `count`:

    ```java
    message VoteAssetContract {
        bytes owner_address = 1;
        repeated bytes vote_address = 2;
        bool support = 3;
        int32 count = 5;
    }
    ```

  - message `VoteWitnessContract`

    message `Vote`:  

    ```java
    message Vote {
        bytes vote_address = 1;
        int64 vote_count = 2;
    }
    ```

    `owner_address`:

    `votes`:

    `support`:

    ```java
    message VoteWitnessContract {
        message Vote {
            bytes vote_address = 1;
            int64 vote_count = 2;
        }
        bytes owner_address = 1;
        repeated Vote votes = 2;
        bool support = 3;
    }
    ```

    - message `WitnessCreateContract`

      `owner_address`:

      `url`:

      ```java
      message WitnessCreateContract {
          bytes owner_address = 1;
          bytes url = 2;
      }
      ```

    - message `AssetIssueContract`

      `id`:

      message `FrozenSupply`:

      ​    `frozen_amount`:

      ​    `frozen_days`:

      ```java
      message FrozenSupply {
          int64 frozen_amount = 1;
          int64 frozen_days = 2;
      }
      ```

      `owner_address`:

      `name`:

      `abbr`:

      `total_supply`:

      `frozen_supply`:

      `trx_num`:

      `precision`:

      `num`:

      `start_time`:

      `end_time`:

      `vote_score`:

      `description`:

      `url`:

      `free_asset_net_limit`:

      `public_free_asset_net_limit`:

      `public_free_asset_net_usage`:

      `public_latest_free_net_time`:

      ```java
      message AssetIssueContract {
          string id = 41;
      
          message FrozenSupply {
              int64 frozen_amount = 1;
              int64 frozen_days = 2;
          }
          bytes owner_address = 1;
          bytes name = 2;
          bytes abbr = 3;
          int64 total_supply = 4;
          repeated FrozenSupply frozen_supply = 5;
          int32 trx_num = 6;
          int32 precision = 7;
          int32 num = 8;
          int64 start_time = 9;
          int64 end_time = 10;
          int64 order = 11; // useless
          int32 vote_score = 16;
          bytes description = 20;
          bytes url = 21;
          int64 free_asset_net_limit = 22;
          int64 public_free_asset_net_limit = 23;
          int64 public_free_asset_net_usage = 24;
          int64 public_latest_free_net_time = 25;
      }
      ```

    - message `WitnessUpdateContract`

      `owner_address`:

      `update_url`:

      ```java
      message WitnessUpdateContract {
          bytes owner_address = 1;
          bytes update_url = 12;
      }
      ```

    - message `ParticipateAssetIssueContract`

      `owner_address`:

      `to_address`:

      `asset_name`:

      `amount`:

      ```java
      message ParticipateAssetIssueContract {
          bytes owner_address = 1;
          bytes to_address = 2;
          bytes asset_name = 3; // this field is token name before the proposal ALLOW_SAME_TOKEN_NAME is active, otherwise it is token id and token is should be in string format.
          int64 amount = 4; // the amount of drops
      }
      ```

    - message `AccountUpdateContract`

      `account_name`:

      `owner_address`:

      ```java
      message AccountUpdateContract {
        bytes account_name = 1;
        bytes owner_address = 2;
      }
      ```

    - message `FreezeBalanceContract`

      `owner_address`:

      `frozen_balance`:

      `frozen_duration`:

      `resource`:

      `receiver_address`:

      ```java
      message FreezeBalanceContract {
          bytes owner_address = 1;
          int64 frozen_balance = 2;
          int64 frozen_duration = 3;
      
          ResourceCode resource = 10;
          bytes receiver_address = 15;
      }
      ```

    - message `UnfreezeBalanceContract`

      `owner_address`:

      `resource`:

      `receiver_address`:

      ```java
      message UnfreezeBalanceContract {
          bytes owner_address = 1;
      
          ResourceCode resource = 10;
          bytes receiver_address = 15;
      }
      ```

    - message `WithdrawBalanceContract`

      `owner_address`:

      ```java
      message WithdrawBalanceContract {
          bytes owner_address = 1;
      }
      ```

    - message `UnfreezeAssetContract`

      `owner_address`:

      ```java
      message UnfreezeAssetContract {
          bytes owner_address = 1;
      }
      ```

    - message `UpdateAssetContract`

      `owner_address`:

      `description`:

      `url`:

      `new_limit`:

      `new_public_limit`:

      ```java
      message UpdateAssetContract {
          bytes owner_address = 1;
          bytes description = 2;
          bytes url = 3;
          int64 new_limit = 4;
          int64 new_public_limit = 5;
      }
      ```

    - message `ProposalCreateContract`

      `owner_address`:

      `parameters`:

      ```java
      message ProposalCreateContract {
          bytes owner_address = 1;
          map<int64, int64> parameters = 2;
      }
      ```

    - message `ProposalApproveContract`

      `owner_address`:

      `proposal_id`:

      `is_add_approval`:

      ```java
      message ProposalApproveContract {
          bytes owner_address = 1;
          int64 proposal_id = 2;
          bool is_add_approval = 3; // add or remove approval
      }
      ```

    - message `ProposalDeleteContract`

      `owner_address`:

      `proposal_id`:

      ```java
      message ProposalDeleteContract {
          bytes owner_address = 1;
          int64 proposal_id = 2;
      }
      ```

    - message `SetAccountIdContract`

      `account_id`:

      `owner_address`:

      ```java
      message SetAccountIdContract {
        bytes account_id = 1;
        bytes owner_address = 2;
      }
      ```

    - `CustomContract`

    - message `CreateSmartContract`

      `owner_address`:

      `new_contract`: 

      `call_token_value`:

      `token_id`:

      ```java
      message CreateSmartContract {
          bytes owner_address = 1;
          SmartContract new_contract = 2;
          int64 call_token_value = 3;
          int64 token_id = 4;
      }
      ```

    - message `TriggerSmartContract`

      `owner_address`:

      `contract_address`:

      `call_value`:

      `data`:

      `call_token_value`:

      `token_id`:

      ```java
      message TriggerSmartContract {
          bytes owner_address = 1;
          bytes contract_address = 2;
          int64 call_value = 3;
          bytes data = 4;
          int64 call_token_value = 5;
          int64 token_id = 6;
      }
      ```

    - `GetContract`

    - message `UpdateSettingContract`

      `owner_address`:

      `contract_address`:

      `consume_user_resource_percent`:

      ```java
      message UpdateSettingContract {
          bytes owner_address = 1;
          bytes contract_address = 2;
          int64 consume_user_resource_percent = 3;
      }
      ```

    - message `ExchangeCreateContract`

      `owner_address`:

      `first_token_id`:

      `first_token_balance`:

      `second_token_id`:

      `second_token_balance`:

      ```java
      message ExchangeCreateContract {
          bytes owner_address = 1;
          bytes first_token_id = 2;
          int64 first_token_balance = 3;
          bytes second_token_id = 4;
          int64 second_token_balance = 5;
      }
      ```

    - message `ExchangeInjectContract`

      `owner_address`:

      `exchange_id`:

      `token_id`:

      `quant`:

      ```java
      message ExchangeInjectContract {
          bytes owner_address = 1;
          int64 exchange_id = 2;
          bytes token_id = 3;
          int64 quant = 4;
      }
      ```

    - message `ExchangeWithdrawContract`

      `owner_address`:

      `exchange_id`:

      `token_id`:

      `quant`:

      ```java
      message ExchangeWithdrawContract {
          bytes owner_address = 1;
          int64 exchange_id = 2;
          bytes token_id = 3;
          int64 quant = 4;
      }
      ```

    - message `ExchangeTransactionContract`

      `owner_address`:

      `exchange_id`:

      `token_id`:

      `quant`:

      `expected`:

      ```java
      message ExchangeTransactionContract {
          bytes owner_address = 1;
          int64 exchange_id = 2;
          bytes token_id = 3;
          int64 quant = 4;
          int64 expected = 5;
      }
      ```

    - message `UpdateEnergyLimitContract`:

      `owner_address`:

      `contract_address`:

      `origin_energy_limit`:

      ```java
      message UpdateEnergyLimitContract {
          bytes owner_address = 1;
          bytes contract_address = 2;
          int64 origin_energy_limit = 3;
      }
      ```

    - message `AccountPermissionUpdateContract`

      `owner_address`:

      `owner`:

      `witness`:

      `actives`:

      ```java
      message AccountPermissionUpdateContract {
        bytes owner_address = 1;
        Permission owner = 2; //Empty is invalidate
        Permission witness = 3; //Can be empty
        repeated Permission actives = 4; //Empty is invalidate
      }
      ```

    - message `ClearABIContract`

      `owner_address`:

      `contract_address`:

      ```java
      message ClearABIContract {
          bytes owner_address = 1;
          bytes contract_address = 2;
      }
      ```

    - message `UpdateBrokerageContract`

      `owner_address`:

      `brokerage`:

      ```java
      message UpdateBrokerageContract {
          bytes owner_address = 1;
          int32 brokerage = 2; // 1 mean 1%
      }
      ```

    - message `ShieldedTransferContract`

      `transparent_from_address`:

      `from_amount`:

      `spend_description`:

      `receive_description`:

      `binding_signature`:

      `transparent_to_address`:

      `to_amount`:

      ```java
      message ShieldedTransferContract {
          bytes transparent_from_address = 1; // transparent address
          int64 from_amount = 2;
          repeated SpendDescription spend_description = 3;
          repeated ReceiveDescription receive_description = 4;
          bytes binding_signature = 5;
          bytes transparent_to_address = 6; // transparent address
          int64 to_amount = 7; // the amount to transparent to_address
      }
      ```

      attributes' type refer to [Shield Contract Related](#shieldc)

  

  #### <span id="smartc">Smart Contract</span>

  message `SmartContract` has mutiple attributes and nested message `ABI`

  - message `SmartContract`

    - message `ABI`

      - message `Entry`

        - Enum `EntryType`

          ```java
          enum EntryType {
              UnknownEntryType = 0;
              Constructor = 1;
              Function = 2;
              Event = 3;
              Fallback = 4;
          }
          ```

        - message `Param`

          `indexed`:

          `name`:

          `type`:

          ```java
          message Param {
              bool indexed = 1;
              string name = 2;
              string type = 3;
              // SolidityType type = 3;
          }
          ```

        - Enum `StateMutabilityType`

          ```java
          enum StateMutabilityType {
              UnknownMutabilityType = 0;
              Pure = 1;
              View = 2;
              Nonpayable = 3;
              Payable = 4;
          }
          ```

        `anonymous`:

        `constant`:

        `name`:

        `inputs`:

        `outputs`:

        `type`:

        `payable`:

        `stateMutability`:

      `entrys`:

    `origin_address`:

    `contract_address`:

    `abi`:

    `bytecode`:

    `call_value`:

    `consume_user_resource_percent`:

    `name`:

    `origin_energy_limit`:

    `code_hash`:

    `trx_hash`:

    ```java
    message SmartContract {
        message ABI {
            message Entry {
                enum EntryType {
                     UnknownEntryType = 0;
                     Constructor = 1;
                     Function = 2;
                     Event = 3;
                     Fallback = 4;
                }
                message Param {
                    bool indexed = 1;
                    string name = 2;
                    string type = 3;
                    // SolidityType type = 3;
                }
                enum StateMutabilityType {
                    UnknownMutabilityType = 0;
                    Pure = 1;
                    View = 2;
                    Nonpayable = 3;
                    Payable = 4;
                }
    
                bool anonymous = 1;
                bool constant = 2;
                string name = 3;
                repeated Param inputs = 4;
                repeated Param outputs = 5;
                EntryType type = 6;
                bool payable = 7;
                StateMutabilityType stateMutability = 8;
            }
            repeated Entry entrys = 1;
        }
        bytes origin_address = 1;
        bytes contract_address = 2;
        ABI abi = 3;
        bytes bytecode = 4;
        int64 call_value = 5;
        int64 consume_user_resource_percent = 6;
        string name = 7;
        int64 origin_energy_limit = 8;
        bytes code_hash = 9;
        bytes trx_hash = 10;
    }
    ```

  ####<span id="shieldc">Shield Contract Related</span>

  - message `AuthenticationPath`

    `value`:

    ```java
    message AuthenticationPath {
        repeated bool value = 1;
    }
    ```

  - message `MerklePath`

    `authentication_paths`:

    `index`:

    `rt`:

    ```java
    message MerklePath {
        repeated AuthenticationPath authentication_paths = 1;
        repeated bool index = 2;
        bytes rt = 3;
    }
    ```

  - message `OutputPoint`

    `hash`:

    `index`:

    ```java
    message OutputPoint {
        bytes hash = 1;
        int32 index = 2;
    }
    ```

  - message `OutputPointInfo`

    `out_points`:

    `block_num`:

    ```java
    message OutputPointInfo {
        repeated OutputPoint out_points = 1;
        int32 block_num = 2;
    }
    ```

  - message `PedersenHash`

    `content`:

    ```java
    message PedersenHash {
        bytes content = 1;
    }
    ```

  - message `IncrementalMerkleTree`

    `left`:

    `right`:

    `parents`:

    ```java
    message IncrementalMerkleTree {
        PedersenHash left = 1;
        PedersenHash right = 2;
        repeated PedersenHash parents = 3;
    }
    ```

  - message `IncrementalMerkleVoucher`

    `tree`:

    `filled`:

    `cursor`:

    `cursor_depth`:

    `rt`:

    `output_point`:

    ```java
    message IncrementalMerkleVoucher {
        IncrementalMerkleTree tree = 1;
        repeated PedersenHash filled = 2;
        IncrementalMerkleTree cursor = 3;
        int64 cursor_depth = 4;
        bytes rt = 5;
        OutputPoint output_point = 10;
    }
    ```

  - message `IncrementalMerkleVoucherInfo`

    `vouchers`:

    `paths`:

    ```java
    message IncrementalMerkleVoucherInfo {
        repeated IncrementalMerkleVoucher vouchers = 1;
        repeated bytes paths = 2;
    }
    ```

  - message `SpendDescription`

    `value_commitment`:

    `anchor`:

    `nullifier`:

    `rk`:

    `zkproof`:

    `spend_authority_signature`:

    ```java
    message SpendDescription {
        bytes value_commitment = 1;
        bytes anchor = 2; // merkle root
        bytes nullifier = 3; // used for check double spend
        bytes rk = 4; // used for check spend authority signature
        bytes zkproof = 5;
        bytes spend_authority_signature = 6;
    }
    ```

  - message `ReceiveDescription`

    `value_commitment`:

    `note_commitment`:

    `epk`:

    `c_enc`:

    `c_out`:

    `zkproof`:

    ```java
    message ReceiveDescription {
        bytes value_commitment = 1;
        bytes note_commitment = 2;
        bytes epk = 3; // for Encryption
        bytes c_enc = 4; // Encryption for incoming, decrypt it with ivk
        bytes c_out = 5; // Encryption for audit, decrypt it with ovk
        bytes zkproof = 6;
    }
    ```

  - message `ShieldedTransferContract`

    `transparent_from_address`:

    `from_amount`:

    `spend_description`:

    `receive_description`:

    `binding_signature`:

    `transparent_to_address`:

    `to_amount:

    ```java
    message ShieldedTransferContract {
        bytes transparent_from_address = 1; // transparent address
        int64 from_amount = 2;
        repeated SpendDescription spend_description = 3;
        repeated ReceiveDescription receive_description = 4;
        bytes binding_signature = 5;
        bytes transparent_to_address = 6; // transparent address
        int64 to_amount = 7; // the amount to transparent to_address
    }
    ```

### <span id="net"> 6.Network</span>

- #### Inventory

  - message `ChainInventory`

    - message `BlockId`

      `hash`:

      `number`:

      ```java
      message BlockId {
        bytes hash = 1;
        int64 number = 2;
      }
      ```

    `ids`:

    `remain_num`:

    ```java
    message ChainInventory {
      message BlockId {
        bytes hash = 1;
        int64 number = 2;
      }
      repeated BlockId ids = 1;
      int64 remain_num = 2;
    }
    ```

  - message `BlockInventory`

    - Enum `Type`

      ```java
      enum Type {
        SYNC = 0;
        ADVTISE = 1;
        FETCH = 2;
      }
      ```

    - message `BlockId`

      `hash`:

      `number`:

      ```java
      message BlockId {
        bytes hash = 1;
        int64 number = 2;
      }
      ```

    `ids`:

    `type`:

    ```java
    message BlockInventory {
      enum Type {
        SYNC = 0;
        ADVTISE = 1;
        FETCH = 2;
      }
    
      message BlockId {
        bytes hash = 1;
        int64 number = 2;
      }
      repeated BlockId ids = 1;
      Type type = 2;
    }
    ```

  - message `Inventory`

    Enum `InventoryType`:

    ```java
    enum InventoryType {
      TRX = 0;
      BLOCK = 1;
    }
    ```

    `type`:

    `ids`:

    ```java
    message Inventory {
      enum InventoryType {
        TRX = 0;
        BLOCK = 1;
      }
      InventoryType type = 1;
      repeated bytes ids = 2;
    }
    ```

  - message `Items`

    Enum `ItemType`:

    ```java
    enum ItemType {
      ERR = 0;
      TRX = 1;
      BLOCK = 2;
      BLOCKHEADER = 3;
    }
    ```

    `type`:

    `blocks`:

    `block_headers`:

    `transactions`:

    ```java
    message Items {
      enum ItemType {
        ERR = 0;
        TRX = 1;
        BLOCK = 2;
        BLOCKHEADER = 3;
      }
    
      ItemType type = 1;
      repeated Block blocks = 2;
      repeated BlockHeader block_headers = 3;
      repeated Transaction transactions = 4;
    }
    ```

- #### DynamicProperty

  - message `DynamicProperties`

    `last_solidity_block_num`:

    ```java
    message DynamicProperties {
      int64 last_solidity_block_num = 1;
    }
    ```

- #### Reason Code

  - enum `ReasonCode`

    ```java
    enum ReasonCode {
      REQUESTED = 0x00;
      BAD_PROTOCOL = 0x02;
      TOO_MANY_PEERS = 0x04;
      DUPLICATE_PEER = 0x05;
      INCOMPATIBLE_PROTOCOL = 0x06;
      NULL_IDENTITY = 0x07;
      PEER_QUITING = 0x08;
      UNEXPECTED_IDENTITY = 0x09;
      LOCAL_IDENTITY = 0x0A;
      PING_TIMEOUT = 0x0B;
      USER_REASON = 0x10;
      RESET = 0x11;
      SYNC_FAIL = 0x12;
      FETCH_FAIL = 0x13;
      BAD_TX = 0x14;
      BAD_BLOCK = 0x15;
      FORKED = 0x16;
      UNLINKABLE = 0x17;
      INCOMPATIBLE_VERSION = 0x18;
      INCOMPATIBLE_CHAIN = 0x19;
      TIME_OUT = 0x20;
      CONNECT_FAIL = 0x21;
      TOO_MANY_PEERS_WITH_SAME_IP = 0x22;
      UNKNOWN = 0xFF;
    }
    ```

- #### Message

  - message `DisconnectMessage`

    `reason`:

    ```java
    message DisconnectMessage {
      ReasonCode reason = 1;
    }
    ```

  - message  `HelloMessage`

    - message `BlockId`:

      `hash`:

      `number`:

      ```java
      message BlockId {
        bytes hash = 1;
        int64 number = 2;
      }
      ```

    `from`:

    `version`:

    `timestamp`:

    `genesisBlockId`:

    `solidBlockId`:

    `headBlockId`:

    `address`:

    `signature`:

    ```java
    message DisconnectMessage {
      ReasonCode reason = 1;
    }
    
    message HelloMessage {
      message BlockId {
        bytes hash = 1;
        int64 number = 2;
      }
    
      Endpoint from = 1;
      int32 version = 2;
      int64 timestamp = 3;
      BlockId genesisBlockId = 4;
      BlockId solidBlockId = 5;
      BlockId headBlockId = 6;
      bytes address = 7;
      bytes signature = 8;
    }
    ```

- #### Node Information

  Node information is separaed into several parts and implemented by nested messages.

  

  - message `NodeInfo`

    `beginSyncNum`:

    `block`:

    `solidityBlock`:

    `currentConnectCount`:

    `activeConnectCount`:

    `passiveConnectCount`:

    `totalFlow`:

    `peerInfoList`:

    `configNodeInfo`:

    `machineInfo`:

    `cheatWitnessInfoMap`:

    - message `PeerInfo`:

      `lastSyncBlock`:

      `remainNum`:

      `lastBlockUpdateTime`:

      `syncFlag`:

      `headBlockTimeWeBothHave`:

      `needSyncFromPeer`:

      `needSyncFromUs`:

      `host`:

      `port`:

      `nodeId`:

      `connectTime`:

      `avgLatency`:

      `syncToFetchSize`:

      `syncToFetchSizePeekNum`:

      `syncBlockRequestedSize`:

      `unFetchSynNum`:

      `blockInPorcSize`:

      `headBlockWeBothHave`:

      `isActive`:

      `score`:

      `nodeCount`:

      `inFlow`:

      `disconnectTimes`:

      `localDisconnectReason`:

      `remoteDisconnectReason`:

      ```java
      message PeerInfo {
        string lastSyncBlock = 1;
        int64 remainNum = 2;
        int64 lastBlockUpdateTime = 3;
        bool syncFlag = 4;
        int64 headBlockTimeWeBothHave = 5;
        bool needSyncFromPeer = 6;
        bool needSyncFromUs = 7;
        string host = 8;
        int32 port = 9;
        string nodeId = 10;
        int64 connectTime = 11;
        double avgLatency = 12;
        int32 syncToFetchSize = 13;
        int64 syncToFetchSizePeekNum = 14;
        int32 syncBlockRequestedSize = 15;
        int64 unFetchSynNum = 16;
        int32 blockInPorcSize = 17;
        string headBlockWeBothHave = 18;
        bool isActive = 19;
        int32 score = 20;
        int32 nodeCount = 21;
        int64 inFlow = 22;
        int32 disconnectTimes = 23;
        string localDisconnectReason = 24;
        string remoteDisconnectReason = 25;
      }
      ```

    - message `ConfigNodeInfo`:

      `codeVersion`:

      `p2pVersion`:

      `listenPort`:

      `discoverEnable`:

      `activeNodeSize`:

      `passiveNodeSize`:

      `sendNodeSize`:

      `maxConnectCount`:

      `sameIpMaxConnectCount`:

      `backupListenPort`:

      `backupMemberSize`:

      `backupPriority`:

      `dbVersion`:

      `minParticipationRate`:

      `supportConstant`:

      `minTimeRatio`:

      `maxTimeRatio`:

      `allowCreationOfContracts`:

      `allowAdaptiveEnergy`:

      ```java
      message ConfigNodeInfo {
        string codeVersion = 1;
        string p2pVersion = 2;
        int32 listenPort = 3;
        bool discoverEnable = 4;
        int32 activeNodeSize = 5;
        int32 passiveNodeSize = 6;
        int32 sendNodeSize = 7;
        int32 maxConnectCount = 8;
        int32 sameIpMaxConnectCount = 9;
        int32 backupListenPort = 10;
        int32 backupMemberSize = 11;
        int32 backupPriority = 12;
        int32 dbVersion = 13;
        int32 minParticipationRate = 14;
        bool supportConstant = 15;
        double minTimeRatio = 16;
        double maxTimeRatio = 17;
        int64 allowCreationOfContracts = 18;
        int64 allowAdaptiveEnergy = 19;
      }
      ```

    - message `MachineInfo`:

      `threadCount`:

      `deadLockThreadCount`:

      `cpuCount`:

      `totalMemory`:

      `freeMemory`:

      `cpuRate`:

      `javaVersion`:

      `osName`:

      `jvmTotalMemoery`:

      `jvmFreeMemory`:

      `processCpuRate`:

      `memoryDescInfoList`:

      `deadLockThreadInfoList`:

      - message `MemoryDescInfo`:

        `name`:

        `initSize`:

        `useSize`:

        `maxSize`:

        `useRate`:

        ```java
        message MemoryDescInfo {
          string name = 1;
          int64 initSize = 2;
          int64 useSize = 3;
          int64 maxSize = 4;
          double useRate = 5;
        }
        ```

      - message `DeadLockThreadInfo`:

        `name`:

        `lockName`:

        `lockOwner`:

        `state`:

        `blockTime`:

        `waitTime`:

        `stackTrace`:

        ```java
        message DeadLockThreadInfo {
          string name = 1;
          string lockName = 2;
          string lockOwner = 3;
          string state = 4;
          int64 blockTime = 5;
          int64 waitTime = 6;
          string stackTrace = 7;
        }
        ```

      ```java
      message MachineInfo {
        int32 threadCount = 1;
        int32 deadLockThreadCount = 2;
        int32 cpuCount = 3;
        int64 totalMemory = 4;
        int64 freeMemory = 5;
        double cpuRate = 6;
        string javaVersion = 7;
        string osName = 8;
        int64 jvmTotalMemoery = 9;
        int64 jvmFreeMemory = 10;
        double processCpuRate = 11;
        repeated MemoryDescInfo memoryDescInfoList = 12;
        repeated DeadLockThreadInfo deadLockThreadInfoList = 13;
      
        message MemoryDescInfo {
          string name = 1;
          int64 initSize = 2;
          int64 useSize = 3;
          int64 maxSize = 4;
          double useRate = 5;
        }
      
        message DeadLockThreadInfo {
          string name = 1;
          string lockName = 2;
          string lockOwner = 3;
          string state = 4;
          int64 blockTime = 5;
          int64 waitTime = 6;
          string stackTrace = 7;
        }
      }
      ```

  