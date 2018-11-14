package org.tron.core.db.api;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.ExchangeCapsule;
import org.tron.core.db.Manager;


public class AssetUpdateHelper {

  private Manager dbManager;

  private HashMap<byte[], byte[]> assetNameToIdMap = new HashMap<>();

  public AssetUpdateHelper(Manager dbManager) {
    this.dbManager = dbManager;
  }

  public void doWork() {
    init();
    updateAsset();
    updateExchange();
    updateAccount();
    finish();
  }

  public void init() {
    dbManager.getAssetIssueV2Store().reset();
    dbManager.getExchangeV2Store().reset();
  }

  public void updateAsset() {

    long tokenIdNum = dbManager.getDynamicPropertiesStore().getTokenIdNum();

    for (AssetIssueCapsule assetIssueCapsule : dbManager.getAssetIssueStore().getAllAssetIssues()) {
      assetIssueCapsule.setId(tokenIdNum);
      dbManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);
      dbManager.getAssetIssueStore().put(assetIssueCapsule.createDbKey(), assetIssueCapsule);

      assetNameToIdMap.put(assetIssueCapsule.createDbKey(), assetIssueCapsule.createDbV2Key());

      tokenIdNum++;
    }

  }

  public void updateExchange() {

    for (ExchangeCapsule exchangeCapsule : dbManager.getExchangeStore().getAllExchanges()) {

      exchangeCapsule.setSecondTokenId(assetNameToIdMap.get(exchangeCapsule.getFirstTokenId()));
      exchangeCapsule.setSecondTokenId(assetNameToIdMap.get(exchangeCapsule.getSecondTokenId()));

      dbManager.getExchangeV2Store().put(exchangeCapsule.createDbKey(), exchangeCapsule);
    }

  }

  public void updateAccount() {

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

    }

  }

  public void finish() {
    dbManager.getDynamicPropertiesStore().saveTokenUpdateDone(1);
    assetNameToIdMap.clear();
  }


}
