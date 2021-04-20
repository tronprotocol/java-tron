package org.tron.core.capsule.utils;

import org.tron.core.capsule.AccountAssetIssueCapsule;
import org.tron.protos.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AssetUtil {

  public static Protocol.AccountAssetIssue buildAccountAssetIssue(Protocol.Account account) {
    return Protocol.AccountAssetIssue.newBuilder()
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

  private static List<Protocol.AccountAssetIssue.Frozen> getFrozen(List<Protocol.Account.Frozen> frozenSupplyList) {
    return frozenSupplyList
            .stream()
            .map(frozen -> Protocol.AccountAssetIssue.Frozen.newBuilder()
                    .setExpireTime(frozen.getExpireTime())
                    .setFrozenBalance(frozen.getFrozenBalance())
                    .build())
            .collect(Collectors.toList());
  }


  public static Protocol.Account importAsset(Protocol.Account account,
                                      AccountAssetIssueCapsule accountAssetIssueCapsule) {
    return account.toBuilder()
            .setAssetIssuedID(accountAssetIssueCapsule.getAssetIssuedID())
            .setAssetIssuedName(accountAssetIssueCapsule.getAssetIssuedName())
            .putAllAsset(accountAssetIssueCapsule.getAssetMap())
            .putAllAssetV2(accountAssetIssueCapsule.getAssetMapV2())
            .putAllFreeAssetNetUsage(accountAssetIssueCapsule.getAllFreeAssetNetUsage())
            .putAllFreeAssetNetUsageV2(accountAssetIssueCapsule.getAllFreeAssetNetUsageV2())
            .putAllLatestAssetOperationTime(accountAssetIssueCapsule.getLatestAssetOperationTimeMap())
            .putAllLatestAssetOperationTimeV2(
                    accountAssetIssueCapsule.getLatestAssetOperationTimeMapV2())
            .addAllFrozenSupply(getAccountFrozenSupplyList(accountAssetIssueCapsule.getFrozenSupplyList()))
            .build();
  }

  private static List<Protocol.Account.Frozen> getAccountFrozenSupplyList(List<Protocol.AccountAssetIssue.Frozen> frozenSupplyList) {
    return Optional.ofNullable(frozenSupplyList)
            .orElseGet(ArrayList::new)
            .stream()
            .map(frozen -> Protocol.Account.Frozen.newBuilder()
                    .setExpireTime(frozen.getExpireTime())
                    .setFrozenBalance(frozen.getFrozenBalance())
                    .build())
            .collect(Collectors.toList());
  }

  public static Protocol.Account clearAccountAsset(Protocol.Account account) {
    return account.toBuilder()
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
