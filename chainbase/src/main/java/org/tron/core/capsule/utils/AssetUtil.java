package org.tron.core.capsule.utils;

import com.google.common.primitives.Bytes;
import java.util.HashMap;
import java.util.Map;
import org.apache.tuweni.bytes.Bytes32;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.store.AccountAssetStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.Account;

public class AssetUtil {

  private static AccountAssetStore accountAssetStore;

  private static DynamicPropertiesStore dynamicPropertiesStore;

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
    if (Bytes32.ZERO.equals(root)) {
      byte[] dbKey = Bytes.concat(account.getAddress().toByteArray(), key);
      return accountAssetStore.get(dbKey) != null;
    }
    return ChainBaseManager.fetch(root).hasAssetV2(account, key);
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

    long balance;
    if (Bytes32.ZERO.equals(root)) {
      balance = accountAssetStore.getBalance(account, key);
    } else {
      balance = ChainBaseManager.fetch(root).getAccountAsset(account, key);
    }

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
    Map<String, Long> map;
    if (Bytes32.ZERO.equals(root)) {
      map = accountAssetStore.getAllAssets(account);
    } else {
      map =  ChainBaseManager.fetch(root).importAllAsset(account);
    }
    return account.toBuilder().clearAssetV2().putAllAssetV2(map).build();
  }

  public static void setAccountAssetStore(
          AccountAssetStore accountAssetStore) {
    AssetUtil.accountAssetStore = accountAssetStore;
  }

  public static void setDynamicPropertiesStore(DynamicPropertiesStore dynamicPropertiesStore) {
    AssetUtil.dynamicPropertiesStore = dynamicPropertiesStore;
  }

  public static boolean isAllowAssetOptimization(Bytes32 root) {
    if (Bytes32.ZERO.equals(root)) {
      return dynamicPropertiesStore.supportAllowAssetOptimization();
    }
    return ChainBaseManager.fetch(root).supportAllowAssetOptimization();

  }

}
