package org.tron.core.db.api;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.ExchangeCapsule;
import org.tron.core.db.Manager;

@Slf4j
public class AssetUpdateHelper {

  private Manager dbManager;

  private HashMap<byte[], byte[]> assetNameToIdMap = new HashMap<>();

  public AssetUpdateHelper(Manager dbManager) {
    this.dbManager = dbManager;
  }

  public void doWork() {
    logger.info("Start to update the asset");
    init();
    updateAsset();
    updateExchange();
    updateAccount();
    finish();
    logger.info("Complete the asset update");
  }

  public void init() {
    dbManager.getAssetIssueV2Store().reset();
    dbManager.getExchangeV2Store().reset();
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(1000000L);
  }

  public void updateAsset() {

    long tokenIdNum = dbManager.getDynamicPropertiesStore().getTokenIdNum();

    for (AssetIssueCapsule assetIssueCapsule : dbManager.getAssetIssueStore().getAllAssetIssues()) {
      tokenIdNum++;

      assetIssueCapsule.setId(tokenIdNum);
      dbManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);
      dbManager.getAssetIssueStore().put(assetIssueCapsule.createDbKey(), assetIssueCapsule);

      assetNameToIdMap.put(assetIssueCapsule.createDbKey(), assetIssueCapsule.createDbV2Key());
    }
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(tokenIdNum);

    logger.info("Complete the asset store update");

  }

  public void updateExchange() {

    for (ExchangeCapsule exchangeCapsule : dbManager.getExchangeStore().getAllExchanges()) {

      exchangeCapsule.setFirstTokenId(assetNameToIdMap.get(exchangeCapsule.getFirstTokenId()));
      exchangeCapsule.setSecondTokenId(assetNameToIdMap.get(exchangeCapsule.getSecondTokenId()));

      dbManager.getExchangeV2Store().put(exchangeCapsule.createDbKey(), exchangeCapsule);
    }

    logger.info("Complete the exchange store update");

  }

  public void updateAccount() {

    long count = 0;

    Iterator<Entry<byte[], AccountCapsule>> iterator = dbManager.getAccountStore().iterator();
    while (iterator.hasNext()) {
      AccountCapsule accountCapsule = iterator.next().getValue();
      HashMap<String, Long> assetV2Map = new HashMap<>();
      for (Map.Entry<String, Long> entry : accountCapsule.getAssetMap().entrySet()) {
        assetV2Map.put(new String(assetNameToIdMap.get(entry.getKey())), entry.getValue());
      }

      accountCapsule.clearAssetV2();
      accountCapsule.addAssetV2Map(assetV2Map);
      dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

      if (count % 50000 == 0) {
        logger.info("The number of accounts that have completed the update ：{}", count);
      }
      count++;
    }

    logger.info("Complete the account store update");

  }

  public void finish() {
    dbManager.getDynamicPropertiesStore().saveTokenUpdateDone(1);
    assetNameToIdMap.clear();
  }


}
