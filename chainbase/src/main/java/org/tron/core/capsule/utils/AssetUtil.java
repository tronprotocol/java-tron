package org.tron.core.capsule.utils;

import com.google.protobuf.ByteString;
import org.apache.commons.collections4.MapUtils;
import org.tron.core.capsule.AccountAssetCapsule;
import org.tron.core.store.AccountAssetIssueStore;
import org.tron.protos.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AssetUtil {

  private static AccountAssetIssueStore accountAssetIssueStore;

  private static boolean isAssetImport = false;

  public static Protocol.AccountAsset getAsset(Protocol.Account account) {
    if (!hasAsset(account)) {
      return null;
    }
    return Protocol.AccountAsset.newBuilder()
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

  private static List<Protocol.AccountAsset.Frozen> getFrozen(List<Protocol.Account.Frozen> frozenSupplyList) {
    return frozenSupplyList
            .stream()
            .map(frozen -> Protocol.AccountAsset.Frozen.newBuilder()
                    .setExpireTime(frozen.getExpireTime())
                    .setFrozenBalance(frozen.getFrozenBalance())
                    .build())
            .collect(Collectors.toList());
  }


  public static Protocol.Account importAsset(Protocol.Account account) {
    if (AssetUtil.hasAsset(account)) {
      isAssetImport = false;
      return account;
    }
    AccountAssetCapsule accountAssetCapsule = AssetUtil
            .getAssetByStore(account.getAddress().toByteArray());
    if (accountAssetCapsule != null) {
      isAssetImport = true;
      return account.toBuilder()
              .setAssetIssuedID(accountAssetCapsule.getAssetIssuedID())
              .setAssetIssuedName(accountAssetCapsule.getAssetIssuedName())
              .putAllAsset(accountAssetCapsule.getAssetMap())
              .putAllAssetV2(accountAssetCapsule.getAssetMapV2())
              .putAllFreeAssetNetUsage(accountAssetCapsule.getAllFreeAssetNetUsage())
              .putAllFreeAssetNetUsageV2(accountAssetCapsule.getAllFreeAssetNetUsageV2())
              .putAllLatestAssetOperationTime(accountAssetCapsule.getLatestAssetOperationTimeMap())
              .putAllLatestAssetOperationTimeV2(
                      accountAssetCapsule.getLatestAssetOperationTimeMapV2())
              .addAllFrozenSupply(getAccountFrozenSupplyList(accountAssetCapsule.getFrozenSupplyList()))
              .build();
    }
    return account;
  }

  private static List<Protocol.Account.Frozen> getAccountFrozenSupplyList(List<Protocol.AccountAsset.Frozen> frozenSupplyList) {
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


  public static boolean hasAsset(Protocol.Account account) {
    if (MapUtils.isNotEmpty(account.getAssetMap()) ||
            MapUtils.isNotEmpty(account.getAssetV2Map())) {
      return true;
    }
    ByteString assetIssuedName = account.getAssetIssuedName();
    if (assetIssuedName != null && !assetIssuedName.isEmpty()) {
      return true;
    }
    ByteString assetIssuedID = account.getAssetIssuedID();
    if (assetIssuedID != null && !assetIssuedID.isEmpty()) {
      return true;
    }
    if (MapUtils.isNotEmpty(account.getLatestAssetOperationTimeMap()) ||
            MapUtils.isNotEmpty(account.getLatestAssetOperationTimeV2Map())) {
      return true;
    }
    if (MapUtils.isNotEmpty(account.getFreeAssetNetUsageMap())) {
      return true;
    }
    if (MapUtils.isNotEmpty(account.getFreeAssetNetUsageV2Map())) {
      return true;
    }
    return false;
  }

  public static void setAccountAssetIssueStore(
          AccountAssetIssueStore accountAssetIssueStore) {
    AssetUtil.accountAssetIssueStore = accountAssetIssueStore;
  }

  public static AccountAssetCapsule getAssetByStore(byte[] key) {
    return accountAssetIssueStore.get(key);
  }

  public static boolean isIsAssetImport() {
    return isAssetImport;
  }
}
