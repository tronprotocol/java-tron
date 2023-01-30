# Tron Protocol 

## Overview 

This is the description of  Google Protobuf implementation of Tron's protocol.

## Contents 

#### [1. Account](#account)

#### [2. Witness](#witness)

#### [3. Block](#block)

#### [4. Transaction](#trans)

#### [5. Contract](#contract)

#### [6. Network](#net)

## Protocols

### <span id="account">1. Account</span>

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
    int64 frozen_balance = 1; 
    int64 expire_time = 2; 
  }
  ```

  message `AccountResource`:

  ```java
  message AccountResource {
    int64 energy_usage = 1;
    Frozen frozen_balance_for_energy = 2;
    int64 latest_consume_time_for_energy = 3;
    int64 acquired_delegated_frozen_balance_for_energy = 4;
    int64 delegated_frozen_balance_for_energy = 5;
    int64 storage_limit = 6;
    int64 storage_usage = 7;
    int64 latest_exchange_storage_time = 8;
  }
  ```
  
  `account_name`: the name of this account. – e.g. “*BillsAccount*”
  
  `type`: what type of this account is – e.g. *0* stands for type Normal.
  
  `address`: the address of this account
  
  `account_id`: the id of this account 

  `balance`: the TRX balance of this account.

  `votes`: received votes of this account. – e.g. *{(“0x1b7w…9xj3”,323), (“0x8djq…j12m”,88),…,(“0x82nd…mx6i”,10001)}*.

  `asset`: other assets except TRX in this account – e.g. *{<“WishToken”,66666>,<”Dogie”,233>}*.

  `assetV2`: other assets except TRX in this account – e.g. *{<“WishToken”,66666>,<”Dogie”,233>}*. (used after allowing same name of token87)

  `frozen`: the freezed TRX of this account for receiving bandwidth

  `net_usage`: the used bandwidth of this account

  `acquired_delegated_fronzen_balance_for_bandwidth`: the freezed balance for receiving delegated bandwidth this account acquired.

  `delegated_frozen_balance_for_bandwidth`: the balance for delegated bandwidth this account freezed

  `create_time`: he create time of this account.

  `latest_opration_time`: the latest operation time of this account.

  `allowance`: the allowance of this account.

  `latest_withdraw_time`: the latest operation time of this account.

  `code`: reserved

  `is_witness`:  identifies whether the account is a witness node.

  `is_committee`: reserved

  `frozen_supply`: 

  `asset_issued_name`: the name of asset issued by this account.

  `asset_issued_ID`: the ID of asset issued by this account.

  `latest_asset_operation_time`: the latest time of operating asset.

  `latest_asset_operation_timeV2`: the latest time of operating asset(used after allowing same name of token)

  `free_net_usage`: free bandwidth used of this account.

  `free_asset_net_usage`: the free bandwidth used when this account transferring asset.

  `free_asset_net_usageV2`: the free bandwidth used when this account transferring asset (used after allowing same name of token)

  `latest_consume_time`: the latest consume energy time of this account.

  `latest_consume_free_time`: the latest consume free bandwidth time of this account.

 ```java
message Account {
    message Frozen {
    int64 frozen_balance = 1; 
      int64 expire_time = 2;
  }
    bytes account_name = 1;
    AccountType type = 2;
    bytes address = 3;
    int64 balance = 4;
    repeated Vote votes = 5;
    map<string, int64> asset = 6;
    map<string, int64> assetV2 = 56;
    repeated Frozen frozen = 7;
    int64 net_usage = 8;
    int64 acquired_delegated_frozen_balance_for_bandwidth = 41;
    int64 delegated_frozen_balance_for_bandwidth = 42;
    int64 create_time = 0x09;
    int64 latest_opration_time = 10;
    int64 allowance = 0x0B;
    int64 latest_withdraw_time = 0x0C;
    bytes code = 13;
    bool is_witness = 14;
    bool is_committee = 15;
    repeated Frozen frozen_supply = 16;
    bytes asset_issued_name = 17;
    bytes asset_issued_ID = 57;
    map<string, int64> latest_asset_operation_time = 18;
    map<string, int64> latest_asset_operation_timeV2 = 58;
    int64 free_net_usage = 19;
    map<string, int64> free_asset_net_usage = 20;
    map<string, int64> free_asset_net_usageV2 = 59;
    int64 latest_consume_time = 21;
    int64 latest_consume_free_time = 22;
    bytes account_id = 23;
    message AccountResource {
      int64 energy_usage = 1;
      Frozen frozen_balance_for_energy = 2;
      int64 latest_consume_time_for_energy = 3;
      int64 acquired_delegated_frozen_balance_for_energy = 4;
      int64 delegated_frozen_balance_for_energy = 5;
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
  
    `vote_address`: the super representative address.
  
    `vote_count`: the vote number to this super representative.
  
    ```java
    message Vote {
      bytes vote_address = 1;
      int64 vote_count = 2;
    }
    ```
    
  - Message `AccountId`
  
    `name`: the name of this account.
  
    `address`: the address of this account.
  
    ```java
    message AccountId {
      bytes name = 1;
      bytes address = 2;
    }
    ```
  
    
  
    
  

### <span id="witness"> 2. Witness</span>

Witness and witness-related messages.

- message `Witness`

  `address`: the address of this witness.

  `voteCount`: total votes received.

  `pubkey`: the public key of this witness.

  `url`: witness information related to url.

  `totalProduce`: total number of blocks produced.

  `totalMissed`: total number of blocks missed.

  `latestBlockNum`: the latest block height.

  `latestSlotNum`: the latest produce block slot.

  `isJobs`: whether it can produce blocks.

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


### <span id="block"> 3. Block</span>

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

  ​    `timestamp`: the timestamp of this block.

  ​    `txTrieRoot`: the root hash of Transactions Merkle Tree in this block.

  ​    `parentHash`: the parent block’s hash of this block.

  ​    `number`: the height of this block.

  ​    `witness_id`: the id of witness which packed this block.

  ​    `witness_address`: the address of witness which packed this block.

  ​    `version`: the version of this block.

  ​    `accountStateRoot`: the account state root of this block.

  ```java
  message raw {
    int64 timestamp = 1;
    bytes txTrieRoot = 2;
    bytes parentHash = 3;
    int64 number = 7;
    int64 witness_id = 8;
    bytes witness_address = 9;
    int32 version = 10;
    bytes accountStateRoot = 11;
  }
  ```
  
  `raw_data`: 

  `witness_signature`: signature for this block header from witness node.

 ```java
message BlockHeader {
    message raw {
      int64 timestamp = 1;
      bytes txTrieRoot = 2;
      bytes parentHash = 3;
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
  


### <span id="trans"> 4. Transaction</span>

Transaction and transaction-related messages.

- Any behaviors which consume energy are regarded as transaction.

  

- message `TXInput` has multiple attributes and 1 nested message

  message `raw`:

  ​    `txID`: transaction ID.

  ​    `vout`: value of last output.

  ​    `pubKey`: public key.

  ```java
  message raw {
    bytes txID = 1;
    int64 vout = 2;
    bytes pubKey = 3;
  }
  ```

  `raw_data`: a message `raw`.

  `signature`: signature for this `TXInput`.

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

  `value`: output value.

  `pubKeyHash`: hash of public key.

  ```java
  message TXOutput {
    int64 value = 1;
    bytes pubKeyHash = 2;
  }
  ```

- message `TransactionRet`

  `blockNumber`: the block number of transaction.

  `blockTimeStamp`: the time stamp of packing transaction into block.

  `transactionInfo`: transaction information.

  ```java
  message TransactionRet {
    int64 blockNumber = 1;
    int64 blockTimeStamp = 2;
    repeated TransactionInfo transactioninfo = 3;
  }
  ```

  - message `TransactionSign`

    `transaction`: transaction data.

    `privateKey`: private key.

    ```java
    message TransactionSign {
      Transaction transaction = 1;
      bytes privateKey = 2;
    }
    ```

  - message `ResourceReceipt`

    `energy_usage`: consume yourself account energy.

    `energy_fee`: consume yourself account fee.

    `origin_energy_usage`: consume contract owner account energy.

    `energy_usage_total`: consume total account fee.

    `net_usage`: consume yourself net.

    `net_fee`: consume yourself trx of net usage.

    `result`: the result of executing transaction.

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

    ​    `note`: note is a comment of internal contract transaction.

    ​    `rejected`: rejected is whether internal transaction is rejected or not.

    ```java
    message CallValueInfo {
      int64 callValue = 1;
      string tokenId = 2;
    }
    ```
    
    `hash`:  internal transaction hash, and it should equals to root transaction id.

    `caller_address`:

    `transferTo_address`:

    `callValueInfo`: Refers to asset transfer information in internal transactions, including trx and trc10.

   ```java
       message InternalTransaction {
          bytes hash = 1;
          bytes caller_address = 2;
          bytes transferTo_address = 3;
          message CallValueInfo {
            int64 callValue = 1;
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
  
    ​enum `code`:
    
    ```java
    enum code {
          SUCESS = 0;
          FAILED = 1;
        }
    ```
    enum `contractResult`: refer to [`Contract`](#contract).
    
     `fee`:
     
     `ret`:
     
     `contractRet`:
     
     `assetIssueID`:
     
     `withdraw_amount`:
     
     `unfreeze_amount`:
     
     `exchange_received_amount`:
     
     `exchange_inject_another_amount`:
     
     `exchange_withdraw_another_amount`:
     
     `exchange_id`:
     
     `shielded_transaction_fee`:
     
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
  
   `ref_block_bytes`: Deprecated.
  
   `ref_block_num`: now block number in transaction head.
  
   `ref_block_hash`: now block hash in transaction head.
  
   `expiration`: the expiration time in transaction head.
  
   `auths`: deprecated.

   `contract`: the contract type for transaction, and only support size = 1 when repeated list here for extension.

   `timestamp`: timestamp for transaction.

   `fee_limit`: the cost limit for energy and fee when trigger and create contract.
   
   ```java
  message raw {
      bytes ref_block_bytes = 1;
    int64 ref_block_num = 3;
      bytes ref_block_hash = 4;
    int64 expiration = 8;
      repeated authority auths = 9;
    bytes data = 10;
      repeated Contract contract = 11;
    bytes scripts = 12;
      int64 timestamp = 14;
    int64 fee_limit = 18;
    }
   ```
    
   `raw_data`: raw data in transaction.
    
  `signature`: signature in transaction.
    
  `ret`: result for transaction.
    
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
          MarketSellAssetContract = 52;
          MarketCancelOrderContract = 53;
          FreezeBalanceV2Contract = 54;
          UnfreezeBalanceV2Contract = 55;
          WithdrawExpireUnfreezeContract = 56;
          DelegateResourceContract = 57;
          UnDelegateResourceContract = 58;
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
        bytes data = 10;
        repeated Contract contract = 11;
        bytes scripts = 12;
        int64 timestamp = 14;
        int64 fee_limit = 18;
      }
      raw raw_data = 1;
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
    
      `address`: the address for log contract.
      
      `topics`: subscribed topics for contract. 
      
      `data`: unsubscribed topics for contract.
        
       ```java
    message Log {
      bytes address = 1;
      repeated bytes topics = 2;
      bytes data = 3;
    }
       ```
        
   `id`: transaction id.
  
   `fee`: transaction fee.

   `blockNumber`: the block number of packing this transaction.

   `blockTimeStamp`: the time of generating block for this transaction.
   
   `contractResult`: the contract result of this transaction.

   `contract_address`: the address of call or create contract.

   `receipt`: the receipt of fee and energy usage. 
   
   `log`: the log for triggering contract.
   
   `result`: the result code for triggering contract.
  
   `resMessage`: the response message for triggering contract .

   `assetIssueID`: the ID for issue an asset.

   `withdraw_amount`: the amount for witness withdraw.

   `unfreeze_amount`: unfreeze trx amount.

   `internal_transactions`: internal transaction lists.

   `exchange_received_amount`: The number of tokens received by the transaction, only has value when the contract type is ExchangeTransactionContract.

   `exchange_inject_another_amount`: The number of another token injected into the exchange pair, only has value when the contract type is ExchangeInjectContract.

   `exchange_withdraw_another_amount`:  The number of tokens withdrew from the exchange pair, only has value when the contract type is ExchangeWithdrawContract.

   `exchange_id`: the token pair id.

   `shielded_transaction_fee`: the usage fee for shielded transaction.

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
  
     `transaction`: list of transactions.
     
     ```java
     message Transactions {
       repeated Transaction transactions = 1;
     }
     ```
  
  - message `Authority` (deprecated)
  
    `account`:
  
    `permission_name`:
  
    ```java
    message authority {
      AccountId account = 1;
      bytes permission_name = 2;
    }
     ```
  
- message `TXOutputs`
  
    `outputs`: output value.
  
    ```java
    message TXOutputs {
    repeated TXOutput outputs = 1;
    }
  ```
  


### <span id="contract"> 5. Contract</span>

Contract and contract-related messages.

- Tron has 33 types of Contracts declared within [`Transaction`](#trans).

- message `Contract`

  enum `ContractType`

  `type`: the type of the contract, it is a Enumuration type.

  `parameter`: binary data of the contract after serialization.

  `provider`: reservedUpdateEnergyLimitContract.

  `ContractName`: reserved

  `Permission_id`: for multisign, the value is in [0, 9], 0 is owner，1 is witness, 2-9 is active.

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
      MarketSellAssetContract = 52;
      MarketCancelOrderContract = 53;
      FreezeBalanceV2Contract = 54;
      UnfreezeBalanceV2Contract = 55;
      WithdrawExpireUnfreezeContract = 56;
      DelegateResourceContract = 57;
      UnDelegateResourceContract = 58;
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

    `owner_address`: the address of the contract owner.

    `account_address`: the new address of the new account.

    `type`: the type of the account.

    ```java
    message AccountCreateContract {
      bytes owner_address = 1;
      bytes account_address = 2;
      AccountType type = 3;
    }
    ```

  - message `TransferContract`

    `owner_address`: address of contract owner.

    `to_address`: receiver address.

    `amount`：amount of TRX.

    ```java
    message TransferContract {
        bytes owner_address = 1;
        bytes to_address = 2;
        int64 amount = 3;
    }
    ```

  - message `TransferAssetContract`

    `asset_name`: name of asset.

    `owner_address`: address of contract owner.

    `to_address`: receiver address.

    `amount`: amount of asset.

    ```java
    message TransferAssetContract {
        bytes asset_name = 1;
        bytes owner_address = 2;
        bytes to_address = 3;
        int64 amount = 4;
    }
    ```

  - message `VoteAssetContract`

    `owner_address`: assress of contract owner.

    `vote_address`: voted address of asset.

    `support`: votes supportive or not.

    `count`: votes count.

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

    `owner_address`: address of the owner.

    `votes`: voting list.

    `support`: votes supportive or not.

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

      `owner_address`: address of the owner.

      `url`: url of witness.

      ```java
      message WitnessCreateContract {
          bytes owner_address = 1;
          bytes url = 2;
      }
      ```

    - message `AssetIssueContract`

      `id`: id.

      message `FrozenSupply`:

      ​    `frozen_amount`: frozen amount of token.

      ​    `frozen_days`: frozen period of token.

      ```java
      message FrozenSupply {
          int64 frozen_amount = 1;
          int64 frozen_days = 2;
      }
      ```

      `owner_address`: address of the owner.

      `name`: contract name.

      `abbr`: contract abbr.

      `total_supply`: maximum of asset.

      `frozen_supply`: frozen supplt of asset.

      `trx_num`: trx num defines token price.

      `precision`: precision.

      `num`: trx num defines token price.

      `start_time`: starting date of contract.

      `end_time`: ending date of contract.

      `vote_score`: vote score of contract received.

      `description`: description of contract.

      `url`: url of contract.

      `free_asset_net_limit`: free bandwidth limit each account owns when transfers asset.

      `public_free_asset_net_limit`: free bandwidth limit for all acoounts.

      `public_free_asset_net_usage`: free bandwidth usage of all accounts.

      `public_latest_free_net_time`: the latest bandwidth consumption time fo token transfer.

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
          int64 order = 11;
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

      `owner_address`: address of owner.

      `update_url`: witness url.

      ```java
      message WitnessUpdateContract {
          bytes owner_address = 1;
          bytes update_url = 12;
      }
      ```

    - message `ParticipateAssetIssueContract`

      `owner_address`: owner address.

      `to_address`: reveiver address.

      `asset_name`: target asset name.

      `amount`: amount of suns.

      ```java
      message ParticipateAssetIssueContract {
          bytes owner_address = 1;
          bytes to_address = 2;
          bytes asset_name = 3;
          int64 amount = 4; 
      }
      ```

    - message `AccountUpdateContract`

      `account_name`: account name.

      `owner_address`: address of owner.

      ```java
      message AccountUpdateContract {
        bytes account_name = 1;
        bytes owner_address = 2;
      }
      ```

    - message `FreezeBalanceContract`

      `owner_address`: address of owner.

      `frozen_balance`: frozen amount of TRX.

      `frozen_duration`: frozen duration of TRX.

      `resource`: type of resource gained from freezing TRX.

      `receiver_address`: account address to receive resource.

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

      `owner_address`: address of owner.

      `resource`: type of resource, BANDWIDTH / ENERGY. 

      `receiver_address`: resource receiver address.

      ```java
      message UnfreezeBalanceContract {
          bytes owner_address = 1;
          ResourceCode resource = 10;
          bytes receiver_address = 15;
      }
      ```
      
     - message `WithdrawBalanceContract`
    
       `owner_address`: address of owner.
    
      ```java
      message WithdrawBalanceContract {
          bytes owner_address = 1;
      }
      ```
    
     - message `UnfreezeAssetContract`
    
       `owner_address`: owner address.
    
      ```java
      message UnfreezeAssetContract {
          bytes owner_address = 1;
      }
       ```
    
     - message `UpdateAssetContract`
    
       `owner_address`: address of owner.
    
       `description`: description of asset.
    
       `url`: asset url.
    
       `new_limit`: bandwidth consumption limit for each account when transfer.
    
       `new_public_limit`: bandwidth consumption limit of the accounts.
    
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
    
       `owner_address`: address of owner.
    
       `parameters`: options and their values of proposals.
    
      ```java
      message ProposalCreateContract {
          bytes owner_address = 1;
          map<int64, int64> parameters = 2;
      }
      ```
    
     - message `ProposalApproveContract`
    
       `owner_address`: address of owner.
    
       `proposal_id`: proposal id.
    
       `is_add_approval`: whether to approve.
    
      ```java
      message ProposalApproveContract {
          bytes owner_address = 1;
          int64 proposal_id = 2;
          bool is_add_approval = 3; 
      }
      ```
    
     - message `ProposalDeleteContract`
    
       `owner_address`: address of owner.
    
       `proposal_id`: proposal id.
    
      ```java
      message ProposalDeleteContract {
          bytes owner_address = 1;
          int64 proposal_id = 2;
      }
      ```
    
     - message `SetAccountIdContract`
    
       `account_id`: account id.
    
       `owner_address`: address of owner.
    
      ```java
      message SetAccountIdContract {
        bytes account_id = 1;
        bytes owner_address = 2;
      }
      ```
    
     - `CustomContract`
    
     - message `CreateSmartContract`
    
       `owner_address`: address of owner.
    
       `new_contract`:  details of the new smart contract.
    
       `call_token_value`: amount of TRC10 token sent to the newly created smart contract.
    
       `token_id`: TRC10 token id.
    
      ```java
      message CreateSmartContract {
          bytes owner_address = 1;
          SmartContract new_contract = 2;
          int64 call_token_value = 3;
          int64 token_id = 4;
      }
      ```
    
     - message `TriggerSmartContract`
    
       `owner_address`: address of owner.
    
       `contract_address`: smart contract address to interact with.
    
       `call_value`: TRX amount sent to smart contract.
    
       `data`: functions and parameters called in smart contract.
    
       `call_token_value`: TRC10 token amount sent to smart contract.
    
       `token_id`: TRC10 token id.
    
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
    
       `owner_address`: address of owner.
    
       `contract_address`: smart contract address.
    
       `consume_user_resource_percent`: user energy payment percentage of whole energy payment includes contract deployer’s and user's energy payment.
    
      ```java
      message UpdateSettingContract {
          bytes owner_address = 1;
          bytes contract_address = 2;
          int64 consume_user_resource_percent = 3;
      }
      ```
    
     - message `ExchangeCreateContract`
    
       `owner_address`: address of owner.
    
       `first_token_id`: supplied token.
    
       `first_token_balance`: supplied token amount.
    
       `second_token_id`: second token id.
    
       `second_token_balance`: second token balance.
    
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
    
       `owner_address`: address of owner.
    
       `exchange_id`: token pair id.
    
       `token_id`: token id to inject.
    
       `quant`: token amount to inject.
    
      ```java
      message ExchangeInjectContract {
          bytes owner_address = 1;
          int64 exchange_id = 2;
          bytes token_id = 3;
          int64 quant = 4;
      }
      ```
    
     - message `ExchangeWithdrawContract`
    
       `owner_address`: address of owner.
    
       `exchange_id`: token pair id.
    
       `token_id`: token id to withdraw.
    
       `quant`: token amount to withdraw.
    
      ```java
      message ExchangeWithdrawContract {
          bytes owner_address = 1;
          int64 exchange_id = 2;
          bytes token_id = 3;
          int64 quant = 4;
      }
      ```
    
     - message `ExchangeTransactionContract`
    
       `owner_address`: address of owner.
    
       `exchange_id`: token pair id.
    
       `token_id`: token id to sell.
    
       `quant`: token amount to sell.
    
       `expected`: expected minimum number of tokens.
    
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
    
       `owner_address`: address of owner.
    
       `contract_address`: smart contract address.
    
       `origin_energy_limit`: value of owner’s consume energy limit for each transaction.
    
      ```java
      message UpdateEnergyLimitContract {
          bytes owner_address = 1;
          bytes contract_address = 2;
          int64 origin_energy_limit = 3;
      }
      ```
    
     - message `AccountPermissionUpdateContract`
    
       `owner_address`: address of owner.
    
       `owner`: autuority to execute all contracts.
    
       `witness`: used by SR for generating blocks.
    
       `actives`: custom a combination of contracts permission sets.
    
      ```java
      message AccountPermissionUpdateContract {
        bytes owner_address = 1;
        Permission owner = 2; 
        Permission witness = 3; 
        repeated Permission actives = 4;
      }
      ```
    
     - message `ClearABIContract`
    
       `owner_address`: address of owner.
    
       `contract_address`: contract address.
    
      ```java
      message ClearABIContract {
          bytes owner_address = 1;
          bytes contract_address = 2;
      }
      ```
    
     - message `UpdateBrokerageContract`
    
       `owner_address`: address of owner.
         
       `brokerage`: draw ratio of SR.
    
      ```java
      message UpdateBrokerageContract {
          bytes owner_address = 1;
          int32 brokerage = 2;
      }
      ```
    
     - message `ShieldedTransferContract`
    
       `transparent_from_address`: transparent address of sender.
    
       `from_amount`: amount from sender.
    
       `spend_description`: input data of transaction.
    
       `receive_description`: output data of transaction.
    
       `binding_signature`: signature to verify transaction.
    
       `transparent_to_address`: transparent address of reveiver.
    
       `to_amount`: amount to transparent to_address
    
      ```java
      message ShieldedTransferContract {
          bytes transparent_from_address = 1; 
          int64 from_amount = 2;
          repeated SpendDescription spend_description = 3;
          repeated ReceiveDescription receive_description = 4;
          bytes binding_signature = 5;
          bytes transparent_to_address = 6;
          int64 to_amount = 7; 
      }
      ```
    
  attributes' type refer to [Shield Contract Related](#shieldc)
  

  
### <span id="smartc">Smart Contract</span>
  
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
              Receive = 5;
          }
          ```
  
      - message `Param`
  
        `indexed`: `true` if the field is part of the log’s topics, `false` if it one of the log’s data segment.
  
        `name`: name of the parameter.
  
        `type`: canonical type of the parameter (more below).
  
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
  
      `anonymous`: `true` if the event was declared as `anonymous`.
  
      `constant`: `true` if function is either `pure` or `view`, `false` otherwise.
  
      `name`: function name.
  
      `inputs`: an array of objects.
  
      `outputs`: an array of objects similar to `inputs`.
  
      `type`: can be omitted, defaulting to `"function"`, likewise `payable` and `constant` can be omitted, both defaulting to `false`.
  
      `payable`: `true` if function accepts Ether, `false` otherwise.
  
      `stateMutability`: a string with one of the following values: `pure` (specified to not read blockchain state), `view` (specified to not modify the blockchain state), `nonpayable` (function does not accept Ether) and `payable` (function accepts Ether).
  
    `entrys`: a function description.
  
  `origin_address`: address of smart contract owner.
  
  `contract_address`:  address of the smart contract.
  
  `abi`: abi of the smart contract.
  
  `bytecode`: bytecode of the smart contract.
  
  `call_value`: amount of TRX that send to the smart contract.
  
  `consume_user_resource_percent`: user energy payment percentage of the whole energy payment which includes both contract deployer’s payment and user energy payment.
  
  `name`: the name of the smart contract.
  
  `origin_energy_limit`: value of the owner’s consume energy limit for each transaction.
  
  `code_hash`: hash of smart contract bytecode.
  
  `trx_hash`:  transactionId of Deploying contract transaction.
  
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
                     Receive = 5;
                }
                message Param {
                    bool indexed = 1;
                    string name = 2;
                    string type = 3;
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

    `value`: merkle authentication path.

   ```java
  message AuthenticationPath {
        repeated bool value = 1;
    }
    ```
  
  - message `MerklePath`

    `authentication_paths`: merkle tree authentication path.

    `index`: index for the merkle authentication path.

    `rt`: merkle tree root.

   ```java
  message MerklePath {
        repeated AuthenticationPath authentication_paths = 1;
        repeated bool index = 2;
        bytes rt = 3;
    }
    ```
  
  - message `OutputPoint`

    `hash`: transaction hash value.

    `index`: output index.

   ```java
  message OutputPoint {
        bytes hash = 1;
        int32 index = 2;
    }
    ```
  
  - message `OutputPointInfo`

    `out_points`: output points.

    `block_num`: block number.

   ```java
  message OutputPointInfo {
        repeated OutputPoint out_points = 1;
        int32 block_num = 2;
    }
    ```
  
  - message `PedersenHash`

    `content`: pedersen hash value.

   ```java
  message PedersenHash {
        bytes content = 1;
    }
    ```
  
  - message `IncrementalMerkleTree`

    `left`: PedersenHash value of left child node.

    `right`: PedersenHash value of right child node.

    `parents`: PedersenHash values of parent nodes.

   ```java
  message IncrementalMerkleTree {
        PedersenHash left = 1;
        PedersenHash right = 2;
        repeated PedersenHash parents = 3;
    }
    ```
  
  - message `IncrementalMerkleVoucher`

    `tree`: incremental merkle tree.

    `filled`: this is a array, it contains the root of the subtree which can be combined with the param tree to be a new merkle tree.

    `cursor`: the node that can be combined to a subtree, when they are combined to a subtree, compute its root and put it into the filled.

    `cursor_depth`:  the tree height, in which depth it can be combined to be a subtree.

    `rt`: merkle tree root.

    `output_point`: output point.

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

    `vouchers`: this is an array, each items represents the merklevoucher of the outputpoint.

    `paths`: his is an array each items represents the path of the outputpoint.

   ```java
  message IncrementalMerkleVoucherInfo {
        repeated IncrementalMerkleVoucher vouchers = 1;
        repeated bytes paths = 2;
    }
    ```
  
  - message `SpendDescription`

    `value_commitment`: commitment to value.

    `anchor`: merkle root.

    `nullifier`: used for check double spend.

    `rk`: used for spend authority signature.

    `zkproof`: zero-knowledge proof of input.

    `spend_authority_signature`: signature for the spend authority.

   ```java
  message SpendDescription {
        bytes value_commitment = 1;
        bytes anchor = 2; 
        bytes nullifier = 3;
        bytes rk = 4;
        bytes zkproof = 5;
        bytes spend_authority_signature = 6;
    }
    ```
  
  - message `ReceiveDescription`

    `value_commitment`: commitment to the value.

    `note_commitment`: commitment to note.

    `epk`: ephemeral public key for encryption.

    `c_enc`: encryption for incoming, decrypt it with ivk.

    `c_out`: encryption for audit, decrypt it with ovk.

    `zkproof`: zero-knowledge proof of output.

   ```java
  message ReceiveDescription {
        bytes value_commitment = 1;
        bytes note_commitment = 2;
        bytes epk = 3; 
        bytes c_enc = 4; 
        bytes c_out = 5; 
        bytes zkproof = 6;
    }
    ```
  
  - message `ShieldedTransferContract`

    `transparent_from_address`: sender transparent address.

    `from_amount`: sender amount.

    `spend_description`: transaction input data.

    `receive_description`: transaction output data.

    `binding_signature`: signature to verify the transaction.

    `transparent_to_address`: transparent address of receiver.

    `to_amount`: amount to receiver.

   ```java
  message ShieldedTransferContract {
        bytes transparent_from_address = 1; 
        int64 from_amount = 2;
        repeated SpendDescription spend_description = 3;
        repeated ReceiveDescription receive_description = 4;
        bytes binding_signature = 5;
        bytes transparent_to_address = 6; 
        int64 to_amount = 7; 
    }
    ```

### <span id="net"> 6. Network</span>

- #### Inventory

  - message `ChainInventory`

    - message `BlockId`

      `hash`: block hash

      `number`: block height

      ```java
      message BlockId {
        bytes hash = 1;
        int64 number = 2;
      }
      ```

    `ids`: block header list of blockchain.

    `remain_num`: number of remaining blocks in blockchain.

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

      `hash`: block hash value.

      `number`: block height.

      ```java
      message BlockId {
        bytes hash = 1;
        int64 number = 2;
      }
      ```

    `ids`: block header list of block inventory.

    `type`: type of block inventory.

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

    `type`: type of inventory.

    `ids`: hash list of transaction or block.

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

    `type`: item type.

    `blocks`: block list of item.

    `block_headers`: block header list of item.

    `transactions`: transaction list of item.

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

    `last_solidity_block_num`: number of latest solidity block.

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
      RANDOM_ELIMINATION = 0x07;
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

    `reason`: disconnect reason from ReasonCode above

    ```java
    message DisconnectMessage {
      ReasonCode reason = 1;
    }
    ```

  - message  `HelloMessage`

    - message `BlockId`:  

      `hash`: block hash value.

      `number`: block height.

      ```java
      message BlockId {
        bytes hash = 1;
        int64 number = 2;
      }
      ```

    `from`: ip, port and nodeID of message sender.

    `version`: p2p version.

    `timestamp`: time of establishing connection.

    `genesisBlockId`: genesis block id.

    `solidBlockId`: solid block id.

    `headBlockId`: head block id.

    `address`: node account address, for signature verification.

    `signature`: signature for sender.

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

    `beginSyncNum`: beginning block height for synchornize.

    `block`: head block id.

    `solidityBlock`: latest solidity block id.

    `currentConnectCount`: current connection count.

    `activeConnectCount`: active connection count.

    `passiveConnectCount`: trusted connection count.

    `totalFlow`: total TCP flow.

    `peerInfoList`: peer information list.

    `configNodeInfo`: node config information.

    `machineInfo`: machine information.

    `cheatWitnessInfoMap`: cheating witness information map.

    - message `PeerInfo`:

      `lastSyncBlock`: last block id for synchornize.

      `remainNum`: number of remaining blocks.

      `lastBlockUpdateTime`: latest block update time .

      `syncFlag`: is synchroniing or not.

      `headBlockTimeWeBothHave`: timestamp of common head block.

      `needSyncFromPeer`: need to sync from peer or not.

      `needSyncFromUs`: need to sync from myself or not.

      `host`: IP address

      `port`: listening port.

      `nodeId`: ramdomly generated node ID

      `connectTime`: connection time period from established.

      `avgLatency`: average latency

      `syncToFetchSize`:  block count in sync queue.

      `syncToFetchSizePeekNum`: height of the first block in sync queue.

      `syncBlockRequestedSize`: block count in request sync queue.

      `unFetchSynNum`: unsync block count.

      `blockInPorcSize`: size of the processing block queue.

      `headBlockWeBothHave`: common head block id.

      `isActive`: true if not `dead` or `nonActive`

      `score`: peer score calculated from connection information.

      `nodeCount`: neighbors count.

      `inFlow`: TCP flow from exact peer.

      `disconnectTimes`: disconnection time.

      `localDisconnectReason`: local disconnect reason.

      `remoteDisconnectReason`: remote disconnect reason.

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

      `codeVersion`: code version.

      `p2pVersion`: p2p version.

      `listenPort`: listening port.

      `discoverEnable`: whether to turn on neighbor discovery.

      `activeNodeSize`: size of active node.

      `passiveNodeSize`: size of passive node.

      `sendNodeSize`: size of sending node.

      `maxConnectCount`: maximum connection.

      `sameIpMaxConnectCount`: maximum connection from the same host.

      `backupListenPort`:  backup listening port.

      `backupMemberSize`: backup member size.

      `backupPriority`: priority of backup.

      `dbVersion`: database version 

      `minParticipationRate`: minimum participation rate.

      `supportConstant`: whether to support constant.

      `minTimeRatio`: time ratio to force timeout.

      `maxTimeRatio`: time ratio to avoid timeout.

      `allowCreationOfContracts`:  permission of creating contracts.

      `allowAdaptiveEnergy`: permission of turning on adaptive energy.

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

      `threadCount`: number of threads

      `deadLockThreadCount`: number of dead lock threads

      `cpuCount`: CPU cores.

      `totalMemory`: total memory

      `freeMemory`: memory not in use.

      `cpuRate`: CPU unusing rate 

      `javaVersion`: java version.

      `osName`: os name.

      `jvmTotalMemory`: jvm total memory.

      `jvmFreeMemory`: jvm unused memory.

      `processCpuRate`: cpu usage.

      `memoryDescInfoList`: memory description information.

      `deadLockThreadInfoList`: deadlock thread information.

      - message `MemoryDescInfo`:

        `name`: memory name.

        `initSize`: memory initialize size.

        `useSize`: memory use size.

        `maxSize`: memory max size.

        `useRate`: memory use rate.

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

        `name`: thread name.

        `lockName`: lock name.

        `lockOwner`: lock owner.

        `state`: thread state.

        `blockTime`: dead block time.

        `waitTime`: wait time.

        `stackTrace`: stack trace message.

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
        int64 jvmTotalMemory = 9;
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

  
