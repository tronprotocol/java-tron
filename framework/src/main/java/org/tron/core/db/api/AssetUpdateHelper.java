package org.tron.core.db.api;

import static org.tron.core.config.Parameter.ChainSymbol.TRX_SYMBOL_BYTES;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.ExchangeCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.AccountAssetIssueCapsule;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;

@Slf4j(topic = "DB")
public class AssetUpdateHelper {

  private ChainBaseManager chainBaseManager;

  private HashMap<String, byte[]> assetNameToIdMap = new HashMap<>();

  public AssetUpdateHelper(ChainBaseManager chainBaseManager) {
    this.chainBaseManager = chainBaseManager;
  }

  public void doWork() {
    long start = System.currentTimeMillis();
    logger.info("Start to update the asset");
    init();
    updateAsset();
    updateExchange();
    updateAccountAssetIssue();
    finish();
    logger.info(
        "Complete the asset update, Total time:{} milliseconds",
        System.currentTimeMillis() - start);
  }

  public void init() {
    if (chainBaseManager.getAssetIssueV2Store().iterator().hasNext()) {
      logger.warn("AssetIssueV2Store is not empty");
    }
    chainBaseManager.getAssetIssueV2Store().reset();
    if (chainBaseManager.getExchangeV2Store().iterator().hasNext()) {
      logger.warn("ExchangeV2Store is not empty");
    }
    chainBaseManager.getExchangeV2Store().reset();
    chainBaseManager.getDynamicPropertiesStore().saveTokenIdNum(1000000L);
  }

  public List<AssetIssueCapsule> getAllAssetIssues() {

    List<AssetIssueCapsule> result = new ArrayList<>();

    long latestBlockHeaderNumber =
        chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    long blockNum = 1;
    while (blockNum <= latestBlockHeaderNumber) {
      if (blockNum % 100000 == 0) {
        logger.info("The number of block that have processed: {}", blockNum);
      }
      try {
        BlockCapsule block = chainBaseManager.getBlockByNum(blockNum);
        for (TransactionCapsule transaction : block.getTransactions()) {
          if (transaction.getInstance().getRawData().getContract(0).getType()
              == ContractType.AssetIssueContract) {
            AssetIssueContract obj =
                transaction
                    .getInstance()
                    .getRawData()
                    .getContract(0)
                    .getParameter()
                    .unpack(AssetIssueContract.class);

            AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(obj);

            result.add(chainBaseManager.getAssetIssueStore().get(assetIssueCapsule.createDbKey()));
          }
        }

      } catch (Exception e) {
        throw new RuntimeException("Block does not exist, num:" + blockNum);
      }

      blockNum++;
    }
    logger.info("Total block:{}", blockNum);

    if (chainBaseManager.getAssetIssueStore().getAllAssetIssues().size() != result.size()) {
      throw new RuntimeException("Asset num is wrong!");
    }

    return result;
  }

  public void updateAsset() {
    long tokenIdNum = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum();
    long count = 0;

    List<AssetIssueCapsule> assetIssueCapsuleList = getAllAssetIssues();
    for (AssetIssueCapsule assetIssueCapsule : assetIssueCapsuleList) {
      tokenIdNum++;
      count++;

      assetIssueCapsule.setId(Long.toString(tokenIdNum));
      chainBaseManager.getAssetIssueStore().put(assetIssueCapsule.createDbKey(), assetIssueCapsule);
      assetIssueCapsule.setPrecision(0);
      chainBaseManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(),
          assetIssueCapsule);

      assetNameToIdMap.put(
          ByteArray.toStr(assetIssueCapsule.createDbKey()), assetIssueCapsule.createDbV2Key());
    }
    chainBaseManager.getDynamicPropertiesStore().saveTokenIdNum(tokenIdNum);

    logger.info("Complete the asset store update, Total assets:{}", count);
  }

  public void updateExchange() {
    long count = 0;

    for (ExchangeCapsule exchangeCapsule : chainBaseManager.getExchangeStore().getAllExchanges()) {
      count++;
      if (!Arrays.equals(exchangeCapsule.getFirstTokenId(), TRX_SYMBOL_BYTES)) {
        exchangeCapsule.setFirstTokenId(
            assetNameToIdMap.get(ByteArray.toStr(exchangeCapsule.getFirstTokenId())));
      }

      if (!Arrays.equals(exchangeCapsule.getSecondTokenId(), TRX_SYMBOL_BYTES)) {
        exchangeCapsule.setSecondTokenId(
            assetNameToIdMap.get(ByteArray.toStr(exchangeCapsule.getSecondTokenId())));
      }

      chainBaseManager.getExchangeV2Store().put(exchangeCapsule.createDbKey(), exchangeCapsule);
    }

    logger.info("Complete the exchange store update, Total exchanges:{}", count);
  }

  public void updateAccountAssetIssue() {
    long count = 0;

    Iterator<Entry<byte[], AccountAssetIssueCapsule>> iterator =
        chainBaseManager.getAccountAssetIssueStore().iterator();
    while (iterator.hasNext()) {
      AccountAssetIssueCapsule accountAssetIssueCapsule = iterator.next().getValue();

      accountAssetIssueCapsule.clearAssetV2();
      if (accountAssetIssueCapsule.getAssetMap().size() != 0) {
        HashMap<String, Long> map = new HashMap<>();
        for (Map.Entry<String, Long> entry : accountAssetIssueCapsule.getAssetMap().entrySet()) {
          map.put(ByteArray.toStr(assetNameToIdMap.get(entry.getKey())), entry.getValue());
        }

        accountAssetIssueCapsule.addAssetMapV2(map);
      }

      accountAssetIssueCapsule.clearFreeAssetNetUsageV2();
      if (accountAssetIssueCapsule.getAllFreeAssetNetUsage().size() != 0) {
        HashMap<String, Long> map = new HashMap<>();
        for (Map.Entry<String, Long> entry : accountAssetIssueCapsule.getAllFreeAssetNetUsage().entrySet()) {
          map.put(ByteArray.toStr(assetNameToIdMap.get(entry.getKey())), entry.getValue());
        }
        accountAssetIssueCapsule.addAllFreeAssetNetUsageV2(map);
      }

      accountAssetIssueCapsule.clearLatestAssetOperationTimeV2();
      if (accountAssetIssueCapsule.getLatestAssetOperationTimeMap().size() != 0) {
        HashMap<String, Long> map = new HashMap<>();
        for (Map.Entry<String, Long> entry :
                accountAssetIssueCapsule.getLatestAssetOperationTimeMap().entrySet()) {
          map.put(ByteArray.toStr(assetNameToIdMap.get(entry.getKey())), entry.getValue());
        }
        accountAssetIssueCapsule.addAllLatestAssetOperationTimeV2(map);
      }

      if (!accountAssetIssueCapsule.getAssetIssuedName().isEmpty()) {
        accountAssetIssueCapsule.setAssetIssuedID(
                assetNameToIdMap.get(
                        ByteArray.toStr(accountAssetIssueCapsule.getAssetIssuedName().toByteArray())));
      }

      chainBaseManager.getAccountAssetIssueStore().put(accountAssetIssueCapsule.createDbKey(), accountAssetIssueCapsule);

      if (count % 50000 == 0) {
        logger.info("The number of accounts that have completed the update: {}", count);
      }
      count++;
    }

    logger.info("Complete the account store update, total assets: {}", count);
  }

  public void finish() {
    chainBaseManager.getDynamicPropertiesStore().saveTokenUpdateDone(1);
    assetNameToIdMap.clear();
  }
}
