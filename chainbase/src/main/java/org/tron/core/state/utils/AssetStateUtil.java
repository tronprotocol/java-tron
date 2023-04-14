package org.tron.core.state.utils;

import org.apache.tuweni.bytes.Bytes32;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.state.store.DynamicPropertiesStateStore;
import org.tron.protos.Protocol.Account;

import java.util.HashMap;
import java.util.Map;

public class AssetStateUtil {


  public static boolean hasAssetV2(Account account, byte[] key, Bytes32 root) {
    if (account.getAssetV2Map().containsKey(ByteArray.toStr(key))) {
      return true;
    }
    if (!isAllowAssetOptimization(root)) {
      return false;
    }
    if (!account.getAssetOptimized()) {
      return false;
    }
    return ChainBaseManager.fetch(root).hasAssetV2(account, Long.valueOf(ByteArray.toStr(key)));
  }

  public static Account importAssetV2(Account account, byte[] key, Bytes32 root) {
    String tokenId = ByteArray.toStr(key);
    if (account.getAssetV2Map().containsKey(tokenId)) {
      return account;
    }
    if (!isAllowAssetOptimization(root)) {
      return account;
    }
    if (!account.getAssetOptimized()) {
      return account;
    }

    long balance = ChainBaseManager.fetch(root).getAccountAsset(account,
            Long.valueOf(ByteArray.toStr(key)));

    Map<String, Long> map = new HashMap<>(account.getAssetV2Map());
    map.put(tokenId, balance);
    return account.toBuilder().clearAssetV2().putAllAssetV2(map).build();
  }

  public static Account importAllAsset(Account account, Bytes32 root) {
    if (!isAllowAssetOptimization(root)) {
      return account;
    }

    if (!account.getAssetOptimized()) {
      return account;
    }
    Map<String, Long> map =  ChainBaseManager.fetch(root).importAllAsset(account);
    return account.toBuilder().clearAssetV2().putAllAssetV2(map).build();
  }

  public static boolean isAllowAssetOptimization(Bytes32 root) {
    try (DynamicPropertiesStateStore store = new DynamicPropertiesStateStore()) {
      store.init(ChainBaseManager.fetch(root));
      return store.supportAllowAssetOptimization();
    }
  }

}
