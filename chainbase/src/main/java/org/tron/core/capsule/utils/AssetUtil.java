package org.tron.core.capsule.utils;

import com.google.common.primitives.Bytes;
import java.util.HashMap;
import java.util.Map;
import org.tron.common.utils.ByteArray;
import org.tron.core.store.AccountAssetStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.Account;

public class AssetUtil {

  private static AccountAssetStore accountAssetStore;

  private static DynamicPropertiesStore dynamicPropertiesStore;

  public static boolean hasAssetV2(Account account, byte[] key) {
    if (!account.getAssetV2Map().isEmpty()
        && account.getAssetV2Map().containsKey(ByteArray.toStr(key))) {
      return true;
    }
    if (!isAllowAssetOptimization()) {
      return false;
    }
    byte[] dbKey = Bytes.concat(account.getAddress().toByteArray(), key);
    return accountAssetStore.get(dbKey) != null;
  }

  public static Account importAsset(Account account, byte[] key) {
    if (!isAllowAssetOptimization()) {
      return account;
    }

    String sKey = ByteArray.toStr(key);
    if (account.getAssetV2Map().containsKey(sKey)) {
      return account;
    }

    long balance = accountAssetStore.getBalance(account, key);
    Map<String, Long> map = new HashMap<>();
    map.putAll(account.getAssetV2Map());
    map.put(sKey, balance);
    return account.toBuilder().clearAssetV2().putAllAssetV2(map).build();
  }

  public static Account importAllAsset(Account account) {
    if (!isAllowAssetOptimization()) {
      return account;
    }
    Map<String, Long> map = accountAssetStore.getAllAssets(account);
    return account.toBuilder().clearAssetV2().putAllAssetV2(map).build();
  }

  public static void setAccountAssetStore(
          AccountAssetStore accountAssetStore) {
    AssetUtil.accountAssetStore = accountAssetStore;
  }

  public static void setDynamicPropertiesStore(DynamicPropertiesStore dynamicPropertiesStore) {
    AssetUtil.dynamicPropertiesStore = dynamicPropertiesStore;
  }

  public static boolean isAllowAssetOptimization() {
    return dynamicPropertiesStore.supportAllowAssetOptimization();
  }

}
