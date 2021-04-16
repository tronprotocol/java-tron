package org.tron.core.store;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.AccountAssetIssueCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.AccountAssetIssue;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j(topic = "DB")
@Component
public class AccountAssetIssueStore extends TronStoreWithRevoking<AccountAssetIssueCapsule> {

  @Autowired
  protected AccountAssetIssueStore(@Value("account-asset-issue") String dbName) {
    super(dbName);
  }

  @Override
  public AccountAssetIssueCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new AccountAssetIssueCapsule(value);
  }

  public AccountAssetIssue buildAccountAssetIssue(Account account) {
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

  private List<AccountAssetIssue.Frozen> getFrozen(List<Account.Frozen> frozenSupplyList) {
    return frozenSupplyList
            .stream()
            .map(frozen -> AccountAssetIssue.Frozen.newBuilder()
                    .setExpireTime(frozen.getExpireTime())
                    .setFrozenBalance(frozen.getFrozenBalance())
                    .build())
            .collect(Collectors.toList());
  }
}
