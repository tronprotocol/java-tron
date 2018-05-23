package org.tron.core.db;


import static org.tron.protos.Protocol.Transaction.Contract.ContractType.TransferAssetContract;

import com.google.protobuf.ByteString;
import java.util.List;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.exception.ValidateBandwidthException;
import org.tron.protos.Contract.TransferAssetContract;
import org.tron.protos.Protocol.Transaction.Contract;

public class BandwidthProcessor {

  private Manager dbManager;
  private long precision;

  public BandwidthProcessor(Manager manager){
    this.dbManager = manager;
    this.precision = ChainConstant.PRECISION;
  }

  private long divideCeil(long numerator, long denominator) {
    return (numerator / denominator) + ((numerator % denominator) > 0 ? 1 : 0);
  }

  private long increase(long lastUsage, long usage, long lastTime, long now, long windowSize)
      throws ValidateBandwidthException {
    long averageUsage = divideCeil(usage * precision, windowSize);

    if (lastTime != now) {
      if (now < lastTime) {
        throw new ValidateBandwidthException("new operation time must more than last time");
      }
      ;
      if (lastTime + windowSize > now) {
        long delta = now - lastTime;
        double decay = (windowSize - delta) / windowSize;
        lastUsage = Math.round(lastUsage * decay);
      } else {
        lastUsage = 0;
      }
    }
    lastUsage += averageUsage; // 更新新的平均使用量
    return lastUsage;
  }

  public void consumeBandwidth(TransactionCapsule trx) throws ValidateBandwidthException {
    List<Contract> contracts =
        trx.getInstance().getRawData().getContractList();

    long bandwidthPerTransaction = dbManager.getDynamicPropertiesStore().getBandwidthPerTransaction();
    long freeOperatingLimit = dbManager.getDynamicPropertiesStore().getFreeOperatingLimit();
    long interval = dbManager.getDynamicPropertiesStore().getFreeOperatingTimeInterval();

    for (Contract contract : contracts) {
      byte[] address = TransactionCapsule.getOwner(contract);
      AccountCapsule accountCapsule = dbManager.getAccountStore().get(address);
      if (accountCapsule == null) {
        throw new ValidateBandwidthException("account not exists");
      }
      long now = dbManager.getHeadBlockTimeStamp();
      long latestOperationTime = accountCapsule.getLatestOperationTime();
      long nextRefreshCountTime = accountCapsule.getNextRefreshCountTime();
      if (nextRefreshCountTime <= now) {
        accountCapsule.refreshCountTime(now);
      }

      boolean hasFreeOperationLeft = accountCapsule.getFreeOperationCount() < freeOperatingLimit;
      if (hasFreeOperationLeft && (now - latestOperationTime >= interval)) {
        accountCapsule.setLatestOperationTime(now);
        accountCapsule.increaseFreeOperationCount();
        dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
        continue;
      }

      if (contract.getType() == TransferAssetContract) {
        ByteString assetName;
        try {
          assetName = contract.getParameter().unpack(TransferAssetContract.class).getAssetName();
        } catch (Exception ex) {
          throw new RuntimeException(ex.getMessage());
        }
        String assetNameString = ByteArray.toStr(assetName.toByteArray());

        Long valueTmp = accountCapsule.getAssetNextRefreshCountTimeMap()
            .get(assetNameString);
        long assetNextRefreshCountTime = valueTmp == null ? 0 : (long) valueTmp;
        if (assetNextRefreshCountTime <= now) {
          accountCapsule.refreshAssetCountTime(assetNameString, now);
        }

        valueTmp = accountCapsule.getLatestAssetOperationTimeMap()
            .get(ByteArray.toStr(assetName.toByteArray()));
        long lastAssetOperationTime = valueTmp == null ? 0 : (long) valueTmp;

        valueTmp = accountCapsule.getLatestAssetFreeOperationCountMap()
            .get(ByteArray.toStr(assetName.toByteArray()));
        long latestAssetFreeOperationCount = valueTmp == null ? 0 : (long) valueTmp;

        AssetIssueCapsule assetIssueCapsule
            = dbManager.getAssetIssueStore().get(assetName.toByteArray());
        long assetInterval = assetIssueCapsule.getFreeOperatingTimeInterval(interval);
        long freeOperationLimit = assetIssueCapsule.getFreeOperationLimit(freeOperatingLimit);

        boolean assetHasFreeOperationLeft = latestAssetFreeOperationCount < freeOperationLimit;

        if (assetHasFreeOperationLeft && (now - lastAssetOperationTime >= assetInterval)) {

          AccountCapsule issuerAccountCapsule = dbManager.getAccountStore()
              .get(assetIssueCapsule.getOwnerAddress().toByteArray());
          long bandwidth = issuerAccountCapsule.getBandwidth();

          if (bandwidth < bandwidthPerTransaction) {
            throw new ValidateBandwidthException("bandwidth is not enough");
          }
          issuerAccountCapsule.setBandwidth(bandwidth - bandwidthPerTransaction);
          dbManager.getAccountStore().put(issuerAccountCapsule.createDbKey(), issuerAccountCapsule);
          accountCapsule.setLatestOperationTime(now);
          accountCapsule
              .putLatestAssetFreeOperationCountMap(ByteArray.toStr(assetName.toByteArray()),
                  latestAssetFreeOperationCount + 1);
          accountCapsule
              .putLatestAssetOperationTimeMap(ByteArray.toStr(assetName.toByteArray()), now);
          dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
          continue;
        }
        accountCapsule
            .putLatestAssetOperationTimeMap(ByteArray.toStr(assetName.toByteArray()), now);
      }

      long bandwidth = accountCapsule.getBandwidth();
      if (bandwidth < bandwidthPerTransaction) {
        throw new ValidateBandwidthException("bandwidth is not enough");
      }
      accountCapsule.setBandwidth(bandwidth - bandwidthPerTransaction);
      accountCapsule.setLatestOperationTime(now);
      dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    }
  }


}
