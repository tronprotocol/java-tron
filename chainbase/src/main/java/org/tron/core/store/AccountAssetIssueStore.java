package org.tron.core.store;

import com.typesafe.config.ConfigObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.Commons;
import org.tron.core.capsule.AccountAssetIssueCapsule;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.AccountAssetIssue;

@Slf4j(topic = "DB")
@Component
public class AccountAssetIssueStore extends TronStoreWithRevoking<AccountAssetIssueCapsule> {

  private static Map<String, byte[]> assertsAddress = new HashMap<>();

  @Autowired
  protected AccountAssetIssueStore(@Value("account-asset-issue") String dbName) {
    super(dbName);
  }

  public static void setAccountAssetIssue(com.typesafe.config.Config config) {
    List<? extends ConfigObject> list = config.getObjectList("genesis.block.assets");
    for (ConfigObject obj : list) {
      String accountName = obj.get("accountName").unwrapped().toString();
      byte[] address = Commons.decodeFromBase58Check(obj.get("address").unwrapped().toString());
      assertsAddress.put(accountName, address);
    }
  }

  @Override
  public AccountAssetIssueCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new AccountAssetIssueCapsule(value);
  }

  /**
   * Min TRX account.
   */
  public AccountAssetIssueCapsule getBlackhole() {
    return getUnchecked(assertsAddress.get("Blackhole"));
  }

//  public AccountCapsule convertAccountAssetIssuePut(AccountCapsule accountCapsule) {
//    AccountAssetIssue accountAssetIssue = buildAccountAssetIssue(accountCapsule);
//    AccountAssetIssueCapsule accountAssetIssueCapsule = new AccountAssetIssueCapsule(accountAssetIssue);
//    this.put(accountCapsule.createDbKey(), accountAssetIssueCapsule);
//    Account account = clearAccountAsset(accountCapsule);
//    accountCapsule.setInstance(account);
//    return accountCapsule;
//  }

//  public AccountAssetIssueCapsule convertAccountAssetIssue(AccountCapsule accountCapsule) {
//    if (null != accountCapsule) {
//      AccountAssetIssue accountAssetIssue = buildAccountAssetIssue(accountCapsule);
//      AccountAssetIssueCapsule accountAssetIssueCapsule = new AccountAssetIssueCapsule(accountAssetIssue);
//      Account account = clearAccountAsset(accountCapsule);
//      accountCapsule.setInstance(account);
//      return accountAssetIssueCapsule;
//    }
//    return null;
//  }

//  public void validateAssetIssue(TransactionCapsule trxCap, AccountStore accountStore) {
//    List<Protocol.Transaction.Contract> contracts = trxCap.getInstance().getRawData().getContractList();
//    Optional.ofNullable(contracts)
//            .orElseGet(ArrayList::new)
//            .stream()
//            .peek(contract -> {
//              byte[] ownerAddress = TransactionCapsule.getOwner(contract);
//              AccountCapsule ownerAccount = accountStore.get(ownerAddress);
//              if (ownerAccount != null) {
//                accountStore.checkAsset(ownerAccount, this);
//              }
//            })
//            .peek(contract -> {
//              byte[] toAddress = TransactionCapsule.getToAddress(contract);
//              AccountCapsule toAccount = accountStore.get(toAddress);
//              if (toAccount != null) {
//                accountStore.checkAsset(toAccount, this);
//              }
//            })
//            .collect(Collectors.toList());
//  }

  public AccountAssetIssue buildAccountAssetIssue (AccountCapsule accountCapsule) {
    Account account = accountCapsule.getInstance();
    return AccountAssetIssue.newBuilder()
            .setAddress(account.getAddress())
            .setAssetIssuedID(account.getAssetIssuedID())
            .setAssetIssuedName(account.getAssetIssuedName())
            .putAllAsset(account.getAssetMap())
            .putAllAssetV2(account.getAssetV2Map())
            .putAllFreeAssetNetUsage(account.getFreeAssetNetUsageMap())
            .putAllFreeAssetNetUsageV2(account.getFreeAssetNetUsageV2Map())
            .putAllLatestAssetOperationTime(account.getLatestAssetOperationTimeMap())
            .putAllLatestAssetOperationTimeV2(
                    account.getLatestAssetOperationTimeV2Map())
            .addAllFrozenSupply(getFrozen(account.getFrozenSupplyList()))
            .build();
  }

  public Account convertAccountAssetIssueToAccount (AccountAssetIssueCapsule accountAssetIssueCapsule) {
    return Account.newBuilder()
            .setAddress(accountAssetIssueCapsule.getAddress())
            .setAssetIssuedID(accountAssetIssueCapsule.getAssetIssuedID())
            .setAssetIssuedName(accountAssetIssueCapsule.getAssetIssuedName())
            .putAllAsset(accountAssetIssueCapsule.getAssetMap())
            .putAllAssetV2(accountAssetIssueCapsule.getAssetMapV2())
            .putAllFreeAssetNetUsage(accountAssetIssueCapsule.getAllFreeAssetNetUsage())
            .putAllFreeAssetNetUsageV2(accountAssetIssueCapsule.getAllFreeAssetNetUsageV2())
            .putAllLatestAssetOperationTime(accountAssetIssueCapsule.getLatestAssetOperationTimeMap())
            .putAllLatestAssetOperationTimeV2(
                    accountAssetIssueCapsule.getLatestAssetOperationTimeMapV2())
            .addAllFrozenSupply(getAssetIssueFrozen(accountAssetIssueCapsule.getFrozenSupplyList()))
            .build();
  }


  private List<AccountAssetIssue.Frozen> getFrozen(List<Account.Frozen> frozenSupplyList) {
    return Optional.ofNullable(frozenSupplyList)
            .orElseGet(ArrayList::new)
            .stream()
            .map(frozen -> AccountAssetIssue.Frozen.newBuilder()
                    .setExpireTime(frozen.getExpireTime())
                    .setFrozenBalance(frozen.getFrozenBalance())
                    .build())
            .collect(Collectors.toList());
  }


  private List<Account.Frozen> getAssetIssueFrozen(List<AccountAssetIssue.Frozen> frozenSupplyList) {
    return Optional.ofNullable(frozenSupplyList)
            .orElseGet(ArrayList::new)
            .stream()
            .map(frozen -> Account.Frozen.newBuilder()
                    .setExpireTime(frozen.getExpireTime())
                    .setFrozenBalance(frozen.getFrozenBalance())
                    .build())
            .collect(Collectors.toList());
  }

  public Account clearAccountAsset(AccountCapsule accountCapsule) {
    return accountCapsule.getInstance().toBuilder()
            .clearAssetIssuedID()
            .clearAssetIssuedName()
            .clearAsset()
            .clearAssetV2()
            .clearFreeAssetNetUsage()
            .clearFreeAssetNetUsageV2()
            .clearLatestAssetOperationTime()
            .clearLatestAssetOperationTimeV2()
            .clearFrozenSupply()
            .build();
  }
}
