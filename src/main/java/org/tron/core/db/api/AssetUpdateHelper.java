package org.tron.core.db.api;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.ExchangeCapsule;
import org.tron.core.db.Manager;

@Slf4j
public class AssetUpdateHelper {

  private Manager dbManager;

  private HashMap<String, byte[]> assetNameToIdMap = new HashMap<>();

  public AssetUpdateHelper(Manager dbManager) {
    this.dbManager = dbManager;
  }

  public void doWork() {
    long start = System.currentTimeMillis();
    logger.info("Start updating the asset");
    init();
    updateAsset();
    updateExchange();
    updateAccount();
    finish();
    logger.info(
        "Complete the asset update,Total time：{} milliseconds", System.currentTimeMillis() - start);
  }

  public void init() {
    if (dbManager.getAssetIssueV2Store().iterator().hasNext()) {
      logger.warn("AssetIssueV2Store is not empty");
    }
    dbManager.getAssetIssueV2Store().reset();
    if (dbManager.getExchangeV2Store().iterator().hasNext()) {
      logger.warn("ExchangeV2Store is not empty");
    }
    dbManager.getExchangeV2Store().reset();
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(1000000L);
  }

  public void updateAsset() {
    long tokenIdNum = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    long count = 0;

    for (AssetIssueCapsule assetIssueCapsule : dbManager.getAssetIssueStore().getAllAssetIssues()) {
      tokenIdNum++;
      count++;

      assetIssueCapsule.setId(Long.toString(tokenIdNum));
      dbManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);
      dbManager.getAssetIssueStore().put(assetIssueCapsule.createDbKey(), assetIssueCapsule);

      assetNameToIdMap.put(
          ByteArray.toStr(assetIssueCapsule.createDbKey()), assetIssueCapsule.createDbV2Key());
    }
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(tokenIdNum);

    logger.info("Complete the asset store update,Total assets：{}", count);
  }

  public void updateExchange() {
    long count = 0;

    for (ExchangeCapsule exchangeCapsule : dbManager.getExchangeStore().getAllExchanges()) {
      count++;
      if (!Arrays.equals(exchangeCapsule.getFirstTokenId(), "_".getBytes())) {
        exchangeCapsule.setFirstTokenId(
            assetNameToIdMap.get(ByteArray.toStr(exchangeCapsule.getFirstTokenId())));
      }

      if (!Arrays.equals(exchangeCapsule.getSecondTokenId(), "_".getBytes())) {
        exchangeCapsule.setSecondTokenId(
            assetNameToIdMap.get(ByteArray.toStr(exchangeCapsule.getSecondTokenId())));
      }

      dbManager.getExchangeV2Store().put(exchangeCapsule.createDbKey(), exchangeCapsule);
    }

    logger.info("Complete the exchange store update,Total exchanges：{}", count);
  }

  public void updateAccount() {
    long count = 0;

    Iterator<Entry<byte[], AccountCapsule>> iterator = dbManager.getAccountStore().iterator();
    while (iterator.hasNext()) {
      AccountCapsule accountCapsule = iterator.next().getValue();

      accountCapsule.clearAssetV2();
      if (accountCapsule.getAssetMap().size() != 0) {
        HashMap<String, Long> map = new HashMap<>();
        for (Map.Entry<String, Long> entry : accountCapsule.getAssetMap().entrySet()) {
          map.put(ByteArray.toStr(assetNameToIdMap.get(entry.getKey())), entry.getValue());
        }

        accountCapsule.addAssetMapV2(map);
      }

      accountCapsule.clearFreeAssetNetUsageV2();
      if (accountCapsule.getAllFreeAssetNetUsage().size() != 0) {
        HashMap<String, Long> map = new HashMap<>();
        for (Map.Entry<String, Long> entry :
            accountCapsule.getAllFreeAssetNetUsage().entrySet()) {
          map.put(ByteArray.toStr(assetNameToIdMap.get(entry.getKey())), entry.getValue());
        }
        accountCapsule.addAllFreeAssetNetUsageV2(map);
      }

      accountCapsule.clearLatestAssetOperationTimeV2();
      if (accountCapsule.getLatestAssetOperationTimeMap().size() != 0) {
        HashMap<String, Long> map = new HashMap<>();
        for (Map.Entry<String, Long> entry :
            accountCapsule.getLatestAssetOperationTimeMap().entrySet()) {
          map.put(ByteArray.toStr(assetNameToIdMap.get(entry.getKey())), entry.getValue());
        }
        accountCapsule.addAllLatestAssetOperationTimeV2(map);
      }

      if (!accountCapsule.getAssetIssuedName().isEmpty()) {
        accountCapsule.setAssetIssuedID(
            assetNameToIdMap.get(
                ByteArray.toStr(accountCapsule.getAssetIssuedName().toByteArray())));
      }

      dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

      if (count % 50000 == 0) {
        logger.info("The number of accounts that have completed the update ：{}", count);
      }
      count++;
    }

    logger.info("Complete the account store update,Total assets：{}", count);
  }

  public void finish() {
    dbManager.getDynamicPropertiesStore().saveTokenUpdateDone(1);
    assetNameToIdMap.clear();
  }
}
