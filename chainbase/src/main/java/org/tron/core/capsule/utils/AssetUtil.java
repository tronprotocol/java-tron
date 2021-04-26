package org.tron.core.capsule.utils;

import com.google.protobuf.ByteString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.tron.core.capsule.AccountAssetCapsule;
import org.tron.core.store.AccountAssetStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.AccountAsset;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AssetUtil {

  private static AccountAssetStore accountAssetStore;

  private static DynamicPropertiesStore dynamicPropertiesStore;

  public static AccountAsset getAsset(Account account) {
    if (!hasAsset(account)) {
      return null;
    }
    return AccountAsset.newBuilder()
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

  private static List<AccountAsset.Frozen> getFrozen(List<Account.Frozen> frozenSupplyList) {
    return frozenSupplyList
            .stream()
            .map(frozen -> AccountAsset.Frozen.newBuilder()
                    .setExpireTime(frozen.getExpireTime())
                    .setFrozenBalance(frozen.getFrozenBalance())
                    .build())
            .collect(Collectors.toList());
  }


  public static Account importAsset(Account account) {
    if (AssetUtil.hasAsset(account)) {
      return null;
    }
    AccountAssetCapsule accountAssetCapsule = accountAssetStore.get(account.getAddress().toByteArray());
    if (accountAssetCapsule == null) {
      return null;
    }

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

  private static List<Account.Frozen> getAccountFrozenSupplyList(List<AccountAsset.Frozen> frozenSupplyList) {
    return Optional.ofNullable(frozenSupplyList)
            .orElseGet(ArrayList::new)
            .stream()
            .map(frozen -> Account.Frozen.newBuilder()
                    .setExpireTime(frozen.getExpireTime())
                    .setFrozenBalance(frozen.getFrozenBalance())
                    .build())
            .collect(Collectors.toList());
  }

  public static Account clearAsset(Account account) {
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

  public static boolean hasAsset(Account account) {
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
    List<Account.Frozen> frozenSupplyList =
            account.getFrozenSupplyList();
    if (CollectionUtils.isNotEmpty(frozenSupplyList)
            && frozenSupplyList.size() > 0) {
      return true;
    }
    return false;
  }

  public static void setAccountAssetStore(
          AccountAssetStore accountAssetStore) {
    AssetUtil.accountAssetStore = accountAssetStore;
  }

  public static void setDynamicPropertiesStore(DynamicPropertiesStore dynamicPropertiesStore) {
    AssetUtil.dynamicPropertiesStore = dynamicPropertiesStore;
  }

  public static boolean isAllowAssetOptimization() {
    return dynamicPropertiesStore.supportAllowAccountAssetOptimization();
  }

}