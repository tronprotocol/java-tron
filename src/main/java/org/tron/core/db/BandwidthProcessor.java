package org.tron.core.db;


import static org.tron.protos.Protocol.Transaction.Contract.ContractType.TransferAssetContract;

import com.google.protobuf.ByteString;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.exception.ValidateBandwidthException;
import org.tron.protos.Contract.TransferAssetContract;
import org.tron.protos.Protocol.Transaction.Contract;

@Slf4j
public class BandwidthProcessor {

  private Manager dbManager;
  private long precision;
  private long windowSize;

  public BandwidthProcessor(Manager manager) {
    this.dbManager = manager;
    this.precision = ChainConstant.PRECISION;
    this.windowSize = ChainConstant.WINDOW_SIZE_MS / ChainConstant.BLOCK_PRODUCED_INTERVAL;
  }

  private long divideCeil(long numerator, long denominator) {
    return (numerator / denominator) + ((numerator % denominator) > 0 ? 1 : 0);
  }

  private long increase(long lastUsage, long usage, long lastTime, long now)
      throws ValidateBandwidthException {
    long averageUsage = divideCeil(usage * precision, windowSize);

    if (lastTime != now) {
      if (now < lastTime) {
        throw new ValidateBandwidthException("new operation time must more than last time");
      }
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

  private long getUsage(long usage) {
    return usage * windowSize / precision;
  }

  public void consumeBandwidth(TransactionCapsule trx) throws ValidateBandwidthException {
    List<Contract> contracts =
        trx.getInstance().getRawData().getContractList();

    long totalNetLimit = dbManager.getDynamicPropertiesStore().getTotalNetLimit();
    long totalNetWeight = dbManager.getDynamicPropertiesStore().getTotalNetWeight();

    for (Contract contract : contracts) {
      long bytes = contract.toByteArray().length;
      byte[] address = TransactionCapsule.getOwner(contract);
      AccountCapsule accountCapsule = dbManager.getAccountStore().get(address);
      if (accountCapsule == null) {
        throw new ValidateBandwidthException("account not exists");
      }
      long now = dbManager.getHeadBlockTimeStamp();
      long latestOperationTime;

      if (contract.getType() == TransferAssetContract) {
        ByteString assetName;
        try {
          assetName = contract.getParameter().unpack(TransferAssetContract.class).getAssetName();
        } catch (Exception ex) {
          throw new RuntimeException(ex.getMessage());
        }
        String assetNameString = ByteArray.toStr(assetName.toByteArray());
        AssetIssueCapsule assetIssueCapsule
            = dbManager.getAssetIssueStore().get(assetName.toByteArray());
        long freeAssetNetLimit = assetIssueCapsule.getFreeAssetNetLimit();

        long freeAssetNetUsage = accountCapsule
            .getFreeAssetNetUsage(assetNameString);
        long latestAssetOperationTime = accountCapsule
            .getLatestAssetOperationTime(assetNameString);

        long newFreeAssetNetUsage = increase(freeAssetNetUsage, 0,
            latestAssetOperationTime, now);

        if (bytes <= (freeAssetNetLimit - getUsage(newFreeAssetNetUsage))) {
          AccountCapsule issuerAccountCapsule = dbManager.getAccountStore()
              .get(assetIssueCapsule.getOwnerAddress().toByteArray());
          long issuerWeight = issuerAccountCapsule.getFrozenBalance();
          long issuerNetUsage = issuerAccountCapsule.getNetUsage();
          long latestConsumeTime = issuerAccountCapsule.getLatestConsumeTime();
          long issuerNetLimit = issuerWeight * totalNetLimit / totalNetWeight;

          long newIssuerNetUsage = increase(issuerNetUsage, 0, latestConsumeTime, now);

          if (bytes <= (issuerNetLimit - getUsage(newIssuerNetUsage))) {
            latestConsumeTime = now;
            latestAssetOperationTime = now;
            latestOperationTime = now;
            newIssuerNetUsage = increase(newIssuerNetUsage, bytes, latestConsumeTime, now);
            newFreeAssetNetUsage = increase(newFreeAssetNetUsage,
                bytes, latestAssetOperationTime, now);
            issuerAccountCapsule.setNetUsage(newIssuerNetUsage);
            issuerAccountCapsule.setLatestConsumeTime(latestConsumeTime);
            accountCapsule.setLatestOperationTime(latestOperationTime);
            accountCapsule.putLatestAssetOperationTimeMap(assetNameString,
                latestAssetOperationTime);
            accountCapsule.putFreeAssetNetUsage(assetNameString, newFreeAssetNetUsage);

            dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
            dbManager.getAccountStore().put(issuerAccountCapsule.createDbKey(),
                issuerAccountCapsule);
            continue;
          }
          logger.info("The " + assetNameString + "issuer does not have enough bandwidth"
              + "Asset Issuer address: "
              + StringUtil.createReadableString(assetIssueCapsule.getOwnerAddress().toByteArray()));
        }
        logger.info("The " + assetNameString + " free bandwidth is not enough");
      }
      long weight = accountCapsule.getFrozenBalance();
      long netUsage = accountCapsule.getNetUsage();
      long latestConsumeTime = accountCapsule.getLatestConsumeTime();
      long netLimit = weight * totalNetLimit / totalNetWeight;

      long newNetUsage = increase(netUsage, 0, latestConsumeTime, now);

      if (bytes <= (netLimit - getUsage(newNetUsage))) {
        latestConsumeTime = now;
        latestOperationTime = now;
        newNetUsage = increase(newNetUsage, bytes, latestConsumeTime, now);
        accountCapsule.setNetUsage(newNetUsage);
        accountCapsule.setLatestOperationTime(latestOperationTime);
        accountCapsule.setLatestConsumeTime(latestConsumeTime);

        dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
        continue;
      }
      logger.info("Bandwidth is running out. Now use free bandwidth");
      long freeNetLimit = dbManager.getDynamicPropertiesStore().getFreeNetLimit();
      long freeNetUsage = accountCapsule.getFreeNetUsage();
      long latestConsumeFreeTime = accountCapsule.getLatestConsumeFreeTime();

      long newFreeNetUsage = increase(freeNetUsage, 0, latestConsumeFreeTime, now);

      if (bytes <= (freeNetLimit - getUsage(newFreeNetUsage))) {
        long publicNetLimit = dbManager.getDynamicPropertiesStore().getPublicNetLimit();
        long publicNetUsage = dbManager.getDynamicPropertiesStore().getPublicNetUsage();
        long publicNetTime = dbManager.getDynamicPropertiesStore().getPublicNetTime();

        long newPublicNetUsage = increase(publicNetUsage, 0, publicNetTime, now);

        if (bytes <= (publicNetLimit - getUsage(newPublicNetUsage))) {
          latestConsumeFreeTime = now;
          latestOperationTime = now;
          publicNetTime = now;
          newFreeNetUsage = increase(newFreeNetUsage, bytes, latestConsumeTime, now);
          newPublicNetUsage = increase(newPublicNetUsage, bytes, publicNetTime, now);
          accountCapsule.setFreeNetUsage(newFreeNetUsage);
          accountCapsule.setLatestConsumeFreeTime(latestConsumeFreeTime);
          accountCapsule.setLatestOperationTime(latestOperationTime);

          dbManager.getDynamicPropertiesStore().savePublicNetUsage(newPublicNetUsage);
          dbManager.getDynamicPropertiesStore().savePublicNetTime(publicNetTime);
          dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
          continue;
        }
        logger.info("Public bandwidth is not enough");
      }
      throw new ValidateBandwidthException("Free bandwidth is running out");
    }
  }
}


