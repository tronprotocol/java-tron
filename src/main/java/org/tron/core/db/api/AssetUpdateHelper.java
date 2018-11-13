package org.tron.core.db.api;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.ExchangeCapsule;
import org.tron.core.db.Manager;


public class AssetUpdateHelper {

  private Manager dbManager;

  public AssetUpdateHelper(Manager dbManager) {
    this.dbManager = dbManager;
  }

  public void doWork() {

    updateAsset();
    updateExchange();
    updateAccount();
    finish();
  }

  public void updateAsset() {

    long tokenIdNum = dbManager.getDynamicPropertiesStore().getTokenIdNum();

    for (AssetIssueCapsule assetIssueCapsule : dbManager.getAssetIssueStore().getAllAssetIssues()) {
      assetIssueCapsule.setId(tokenIdNum);
      dbManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);
      dbManager.getAssetIssueStore().put(assetIssueCapsule.createDbKey(), assetIssueCapsule);
      tokenIdNum++;
    }

  }

  public void updateExchange() {

    for (ExchangeCapsule exchangeCapsule : dbManager.getExchangeStore().getAllExchanges()) {

      AssetIssueCapsule firstAssetIssueCapsule = dbManager.getAssetIssueStore()
          .get(exchangeCapsule.getFirstTokenId());
      exchangeCapsule.setSecondTokenId(ByteArray.fromLong(firstAssetIssueCapsule.getId()));

      AssetIssueCapsule secondAssetIssueCapsule = dbManager.getAssetIssueStore()
          .get(exchangeCapsule.getSecondTokenId());
      exchangeCapsule.setSecondTokenId(ByteArray.fromLong(secondAssetIssueCapsule.getId()));

      dbManager.getExchangeV2Store().put(exchangeCapsule.createDbKey(), exchangeCapsule);
    }

  }

  public void updateAccount() {

    Iterator<Entry<byte[], AccountCapsule>> iterator = dbManager.getAccountStore().iterator();
    while (iterator.hasNext()) {
      AccountCapsule accountCapsule = iterator.next().getValue();
      HashMap<String, Long> assetV2Map = new HashMap<>();
      for (Map.Entry<String, Long> entry : accountCapsule.getAssetMap().entrySet()) {
        AssetIssueCapsule assetIssueCapsule = dbManager.getAssetIssueStore()
            .get(ByteArray.fromString(entry.getKey()));
        assetV2Map.put(ByteArray.toStr(assetIssueCapsule.createDbV2Key()), entry.getValue());
      }

      accountCapsule.addAssetV2Map(assetV2Map);
      dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    }

  }

  public void finish() {
    dbManager.getDynamicPropertiesStore().saveTokenUpdateDone(1);
  }


}
