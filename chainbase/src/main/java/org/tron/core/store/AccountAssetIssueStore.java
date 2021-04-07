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
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.protos.Protocol;
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

  public AccountCapsule convertAccountAssetIssuePut(AccountCapsule accountCapsule) {
    AccountAssetIssue accountAssetIssue = buildAccountAssetIssue(accountCapsule);
    AccountAssetIssueCapsule accountAssetIssueCapsule = new AccountAssetIssueCapsule(accountAssetIssue);
    this.put(accountCapsule.createDbKey(), accountAssetIssueCapsule);
    Account account = clearAccountAsset(accountCapsule);
    accountCapsule.setInstance(account);
    return accountCapsule;
  }

  public AccountAssetIssueCapsule convertAccountAssetIssue(AccountCapsule accountCapsule) {
    if (null != accountCapsule) {
      AccountAssetIssue accountAssetIssue = buildAccountAssetIssue(accountCapsule);
      AccountAssetIssueCapsule accountAssetIssueCapsule = new AccountAssetIssueCapsule(accountAssetIssue);
      Account account = clearAccountAsset(accountCapsule);
      accountCapsule.setInstance(account);
      return accountAssetIssueCapsule;
    }
    return null;
  }

  public void validateAssetIssue(TransactionCapsule trxCap, AccountStore accountStore) {
    List<Protocol.Transaction.Contract> contracts = trxCap.getInstance().getRawData().getContractList();
    Optional.ofNullable(contracts)
            .orElseGet(ArrayList::new)
            .stream()
            .peek(contract -> {
              byte[] ownerAddress = TransactionCapsule.getOwner(contract);
              AccountCapsule ownerAccount = accountStore.get(ownerAddress);
              if (ownerAccount != null) {
                accountStore.checkAsset(ownerAccount, this);
              }
            })
            .peek(contract -> {
              byte[] toAddress = TransactionCapsule.getToAddress(contract);
              AccountCapsule toAccount = accountStore.get(toAddress);
              if (toAccount != null) {
                accountStore.checkAsset(toAccount, this);
              }
            })
            .collect(Collectors.toList());
  }

  public static AccountAssetIssue buildAccountAssetIssue (AccountCapsule accountCapsule) {
    return AccountAssetIssue.newBuilder()
            .setAddress(accountCapsule.getAddress())
            .setAssetIssuedID(accountCapsule.getAssetIssuedID())
            .setAssetIssuedName(accountCapsule.getAssetIssuedName())
            .putAllAsset(accountCapsule.getAssetMap())
            .putAllAssetV2(accountCapsule.getAssetMapV2())
            .putAllFreeAssetNetUsage(accountCapsule.getAllFreeAssetNetUsage())
            .putAllFreeAssetNetUsageV2(accountCapsule.getAllFreeAssetNetUsageV2())
            .putAllLatestAssetOperationTime(accountCapsule.getLatestAssetOperationTimeMap())
            .putAllLatestAssetOperationTimeV2(
                    accountCapsule.getLatestAssetOperationTimeMapV2())
            .addAllFrozenSupply(getFrozen(accountCapsule.getFrozenSupplyList()))
            .build();
  }

  private static List<AccountAssetIssue.Frozen> getFrozen(List<Account.Frozen> frozenSupplyList) {
    return Optional.ofNullable(frozenSupplyList)
            .orElseGet(ArrayList::new)
            .stream()
            .map(frozen -> AccountAssetIssue.Frozen.newBuilder()
                    .setExpireTime(frozen.getExpireTime())
                    .setFrozenBalance(frozen.getFrozenBalance())
                    .build())
            .collect(Collectors.toList());
  }

  public static Account clearAccountAsset(AccountCapsule accountCapsule) {
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
