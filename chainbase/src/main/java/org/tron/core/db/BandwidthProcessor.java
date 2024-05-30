package org.tron.core.db;

import static org.tron.core.Constant.PER_SIGN_LENGTH;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;
import static org.tron.protos.Protocol.Transaction.Contract.ContractType.ShieldedTransferContract;
import static org.tron.protos.Protocol.Transaction.Contract.ContractType.TransferAssetContract;
import static org.tron.protos.contract.Common.ResourceCode.BANDWIDTH;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.StringUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.TooBigTransactionException;
import org.tron.core.exception.TooBigTransactionResultException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.AssetIssueV2Store;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import org.tron.protos.contract.BalanceContract.TransferContract;

@Slf4j(topic = "DB")
public class BandwidthProcessor extends ResourceProcessor {

  private AssetIssueStore assetIssueStore;
  private AssetIssueV2Store assetIssueV2Store;

  public BandwidthProcessor(ChainBaseManager chainBaseManager) {
    super(chainBaseManager.getDynamicPropertiesStore(), chainBaseManager.getAccountStore());
    this.assetIssueStore = chainBaseManager.getAssetIssueStore();
    this.assetIssueV2Store = chainBaseManager.getAssetIssueV2Store();
  }

  public BandwidthProcessor(DynamicPropertiesStore dynamicPropertiesStore,
                            AccountStore accountStore, AssetIssueStore assetIssueStore,
                            AssetIssueV2Store assetIssueV2Store) {
    super(dynamicPropertiesStore, accountStore);
    this.assetIssueStore = assetIssueStore;
    this.assetIssueV2Store = assetIssueV2Store;
  }

  public void updateUsageForDelegated(AccountCapsule ac) {
    long now = getHeadSlot();
    long oldNetUsage = ac.getNetUsage();
    long latestConsumeTime = ac.getLatestConsumeTime();
    ac.setNetUsage(increase(ac, BANDWIDTH, oldNetUsage, 0, latestConsumeTime, now));
  }

  public void updateUsage(AccountCapsule accountCapsule) {
    long now = getHeadSlot();
    long oldNetUsage = accountCapsule.getNetUsage();
    long latestConsumeTime = accountCapsule.getLatestConsumeTime();
    accountCapsule.setNetUsage(increase(accountCapsule, BANDWIDTH,
            oldNetUsage, 0, latestConsumeTime, now));
    long oldFreeNetUsage = accountCapsule.getFreeNetUsage();
    long latestConsumeFreeTime = accountCapsule.getLatestConsumeFreeTime();
    accountCapsule.setFreeNetUsage(increase(oldFreeNetUsage, 0, latestConsumeFreeTime, now));

    if (dynamicPropertiesStore.getAllowSameTokenName() == 0) {
      Map<String, Long> assetMap = accountCapsule.getAssetMap();
      assetMap.forEach((assetName, balance) -> {
        long oldFreeAssetNetUsage = accountCapsule.getFreeAssetNetUsage(assetName);
        long latestAssetOperationTime = accountCapsule.getLatestAssetOperationTime(assetName);
        accountCapsule.putFreeAssetNetUsage(assetName,
            increase(oldFreeAssetNetUsage, 0, latestAssetOperationTime, now));
      });
    }
    Map<String, Long> assetMapV2 = accountCapsule.getAssetMapV2();
    Map<String, Long> map = new HashMap<>(assetMapV2);
    accountCapsule.getAllFreeAssetNetUsageV2().forEach((k, v) -> {
      if (!map.containsKey(k)) {
        map.put(k, 0L);
      }
    });
    map.forEach((assetName, balance) -> {
      long oldFreeAssetNetUsage = accountCapsule.getFreeAssetNetUsageV2(assetName);
      long latestAssetOperationTime = accountCapsule.getLatestAssetOperationTimeV2(assetName);
      accountCapsule.putFreeAssetNetUsageV2(assetName,
          increase(oldFreeAssetNetUsage, 0, latestAssetOperationTime, now));
    });
  }

  // update usage for asset issue
  public void updateUsage(AssetIssueCapsule assetIssueCapsule) {
    long now = getHeadSlot();
    updateUsage(assetIssueCapsule, now);
  }

  public void updateUsage(AssetIssueCapsule assetIssueCapsule, long now) {
    long publicFreeAssetNetUsage = assetIssueCapsule.getPublicFreeAssetNetUsage();
    long publicLatestFreeNetTime = assetIssueCapsule.getPublicLatestFreeNetTime();

    assetIssueCapsule.setPublicFreeAssetNetUsage(increase(publicFreeAssetNetUsage, 0,
        publicLatestFreeNetTime, now));
  }


  @Override
  public void consume(TransactionCapsule trx, TransactionTrace trace)
      throws ContractValidateException, AccountResourceInsufficientException,
      TooBigTransactionResultException, TooBigTransactionException {
    List<Contract> contracts = trx.getInstance().getRawData().getContractList();
    long resultSizeWithMaxContractRet = trx.getResultSizeWithMaxContractRet();
    if (!trx.isInBlock() && resultSizeWithMaxContractRet >
        Constant.MAX_RESULT_SIZE_IN_TX * contracts.size()) {
      throw new TooBigTransactionResultException(String.format(
          "Too big transaction result, TxId %s, the result size is %d bytes, maxResultSize %d",
          trx.getTransactionId(), resultSizeWithMaxContractRet, Constant.MAX_RESULT_SIZE_IN_TX));
    }
    if (trx.getResultSerializedSize() > Constant.MAX_RESULT_SIZE_IN_TX * contracts.size()) {
      throw new TooBigTransactionResultException();
    }

    long bytesSize;

    if (dynamicPropertiesStore.supportVM()) {
      bytesSize = trx.getInstance().toBuilder().clearRet().build().getSerializedSize();
    } else {
      bytesSize = trx.getSerializedSize();
    }

    for (Contract contract : contracts) {
      if (contract.getType() == ShieldedTransferContract) {
        continue;
      }
      if (dynamicPropertiesStore.supportVM()) {
        bytesSize += Constant.MAX_RESULT_SIZE_IN_TX;
      }

      logger.debug("TxId {}, bandwidth cost: {}.", trx.getTransactionId(), bytesSize);
      trace.setNetBill(bytesSize, 0);
      byte[] address = TransactionCapsule.getOwner(contract);
      AccountCapsule accountCapsule = accountStore.get(address);
      if (accountCapsule == null) {
        throw new ContractValidateException(String.format("account [%s] does not exist",
            StringUtil.encode58Check(address)));
      }
      long now = getHeadSlot();
      if (contractCreateNewAccount(contract)) {
        if (!trx.isInBlock()) {
          long maxCreateAccountTxSize = dynamicPropertiesStore.getMaxCreateAccountTxSize();
          int signatureCount = trx.getInstance().getSignatureCount();
          long createAccountBytesSize = trx.getInstance().toBuilder().clearRet()
              .build().getSerializedSize() - (signatureCount * PER_SIGN_LENGTH);
          if (createAccountBytesSize > maxCreateAccountTxSize) {
            throw new TooBigTransactionException(String.format(
                "Too big new account transaction, TxId %s, the size is %d bytes, maxTxSize %d",
                trx.getTransactionId(), createAccountBytesSize, maxCreateAccountTxSize));
          }
        }
        consumeForCreateNewAccount(accountCapsule, bytesSize, now, trace);
        continue;
      }

      if (contract.getType() == TransferAssetContract && useAssetAccountNet(contract,
          accountCapsule, now, bytesSize)) {
        continue;
      }

      if (useAccountNet(accountCapsule, bytesSize, now)) {
        continue;
      }

      if (useFreeNet(accountCapsule, bytesSize, now)) {
        continue;
      }

      if (useTransactionFee(accountCapsule, bytesSize, trace)) {
        continue;
      }

      long fee = dynamicPropertiesStore.getTransactionFee() * bytesSize;
      throw new AccountResourceInsufficientException(
          String.format(
              "account [%s] has insufficient bandwidth[%d] and balance[%d] to create new account",
              StringUtil.encode58Check(address), bytesSize, fee));
    }
  }

  private boolean useTransactionFee(AccountCapsule accountCapsule, long bytes,
      TransactionTrace trace) {
    long fee = dynamicPropertiesStore.getTransactionFee() * bytes;
    if (consumeFeeForBandwidth(accountCapsule, fee)) {
      trace.setNetBill(0, fee);
      dynamicPropertiesStore.addTotalTransactionCost(fee);
      return true;
    } else {
      return false;
    }
  }

  private void consumeForCreateNewAccount(AccountCapsule accountCapsule, long bytes,
      long now, TransactionTrace trace)
      throws AccountResourceInsufficientException {
    boolean ret = consumeBandwidthForCreateNewAccount(accountCapsule, bytes, now, trace);

    if (!ret) {
      ret = consumeFeeForCreateNewAccount(accountCapsule, trace);
      if (!ret) {
        throw new AccountResourceInsufficientException(String.format(
            "account [%s] has insufficient bandwidth[%d] and balance[%d] to create new account",
            StringUtil.encode58Check(accountCapsule.createDbKey()), bytes,
            dynamicPropertiesStore.getCreateAccountFee()));
      }
    }
  }

  public boolean consumeBandwidthForCreateNewAccount(AccountCapsule accountCapsule, long bytes,
      long now, TransactionTrace trace) {

    long createNewAccountBandwidthRatio = dynamicPropertiesStore
        .getCreateNewAccountBandwidthRate();

    long netUsage = accountCapsule.getNetUsage();
    long latestConsumeTime = accountCapsule.getLatestConsumeTime();
    long netLimit = calculateGlobalNetLimit(accountCapsule);
    long newNetUsage;
    if (!dynamicPropertiesStore.supportUnfreezeDelay()) {
      newNetUsage = increase(netUsage, 0, latestConsumeTime, now);
    } else {
      // only participate in the calculation as a temporary variable, without disk flushing
      newNetUsage = recovery(accountCapsule, BANDWIDTH, netUsage, latestConsumeTime, now);
    }

    long netCost = bytes * createNewAccountBandwidthRatio;
    if (netCost <= (netLimit - newNetUsage)) {
      long latestOperationTime = dynamicPropertiesStore.getLatestBlockHeaderTimestamp();
      if (!dynamicPropertiesStore.supportUnfreezeDelay()) {
        newNetUsage = increase(newNetUsage, netCost, now, now);
      } else {
        // Participate in calculation and flush disk persistence
        newNetUsage = increase(accountCapsule, BANDWIDTH,
            netUsage, netCost, latestConsumeTime, now);
      }
      accountCapsule.setLatestConsumeTime(now);
      accountCapsule.setLatestOperationTime(latestOperationTime);
      accountCapsule.setNetUsage(newNetUsage);

      trace.setNetBillForCreateNewAccount(netCost, 0);
      accountStore.put(accountCapsule.createDbKey(), accountCapsule);

      return true;
    }
    return false;
  }

  public boolean consumeFeeForCreateNewAccount(AccountCapsule accountCapsule,
      TransactionTrace trace) {
    long fee = dynamicPropertiesStore.getCreateAccountFee();
    if (consumeFeeForNewAccount(accountCapsule, fee)) {
      trace.setNetBillForCreateNewAccount(0, fee);
      dynamicPropertiesStore.addTotalCreateAccountCost(fee);
      return true;
    } else {
      return false;
    }
  }

  public boolean contractCreateNewAccount(Contract contract) {
    AccountCapsule toAccount;
    switch (contract.getType()) {
      case AccountCreateContract:
        return true;
      case TransferContract:
        TransferContract transferContract;
        try {
          transferContract = contract.getParameter().unpack(TransferContract.class);
        } catch (Exception ex) {
          throw new RuntimeException(ex.getMessage());
        }
        toAccount = accountStore.get(transferContract.getToAddress().toByteArray());
        return toAccount == null;
      case TransferAssetContract:
        TransferAssetContract transferAssetContract;
        try {
          transferAssetContract = contract.getParameter().unpack(TransferAssetContract.class);
        } catch (Exception ex) {
          throw new RuntimeException(ex.getMessage());
        }
        toAccount = accountStore.get(transferAssetContract.getToAddress().toByteArray());
        return toAccount == null;
      default:
        return false;
    }
  }


  private boolean useAssetAccountNet(Contract contract, AccountCapsule accountCapsule, long now,
      long bytes)
      throws ContractValidateException {

    ByteString assetName;
    try {
      assetName = contract.getParameter().unpack(TransferAssetContract.class).getAssetName();
    } catch (Exception ex) {
      throw new RuntimeException(ex.getMessage());
    }

    AssetIssueCapsule assetIssueCapsule;
    AssetIssueCapsule assetIssueCapsuleV2;
    assetIssueCapsule = Commons.getAssetIssueStoreFinal(
        dynamicPropertiesStore, assetIssueStore, assetIssueV2Store)
        .get(assetName.toByteArray());
    if (assetIssueCapsule == null) {
      throw new ContractValidateException(String.format("asset [%s] does not exist", assetName));
    }

    String tokenName = ByteArray.toStr(assetName.toByteArray());
    String tokenID = assetIssueCapsule.getId();
    if (assetIssueCapsule.getOwnerAddress() == accountCapsule.getAddress()) {
      return useAccountNet(accountCapsule, bytes, now);
    }

    long publicFreeAssetNetLimit = assetIssueCapsule.getPublicFreeAssetNetLimit();
    long publicFreeAssetNetUsage = assetIssueCapsule.getPublicFreeAssetNetUsage();
    long publicLatestFreeNetTime = assetIssueCapsule.getPublicLatestFreeNetTime();

    long newPublicFreeAssetNetUsage = increase(publicFreeAssetNetUsage, 0,
        publicLatestFreeNetTime, now);

    if (bytes > (publicFreeAssetNetLimit - newPublicFreeAssetNetUsage)) {
      logger.debug("The {} public free bandwidth is not enough."
              + " Bytes: {}, publicFreeAssetNetLimit: {}, newPublicFreeAssetNetUsage: {}.",
          tokenID, bytes, publicFreeAssetNetLimit,  newPublicFreeAssetNetUsage);
      return false;
    }

    long freeAssetNetLimit = assetIssueCapsule.getFreeAssetNetLimit();

    long freeAssetNetUsage;
    long latestAssetOperationTime;
    if (dynamicPropertiesStore.getAllowSameTokenName() == 0) {
      freeAssetNetUsage = accountCapsule
          .getFreeAssetNetUsage(tokenName);
      latestAssetOperationTime = accountCapsule
          .getLatestAssetOperationTime(tokenName);
    } else {
      freeAssetNetUsage = accountCapsule.getFreeAssetNetUsageV2(tokenID);
      latestAssetOperationTime = accountCapsule.getLatestAssetOperationTimeV2(tokenID);
    }

    long newFreeAssetNetUsage = increase(freeAssetNetUsage, 0,
        latestAssetOperationTime, now);

    if (bytes > (freeAssetNetLimit - newFreeAssetNetUsage)) {
      logger.debug("The {} free bandwidth is not enough."
              + " Bytes: {}, freeAssetNetLimit: {}, newFreeAssetNetUsage:{}.",
          tokenID, bytes, freeAssetNetLimit, newFreeAssetNetUsage);
      return false;
    }

    AccountCapsule issuerAccountCapsule = accountStore
        .get(assetIssueCapsule.getOwnerAddress().toByteArray());

    long issuerNetUsage = issuerAccountCapsule.getNetUsage();
    long latestConsumeTime = issuerAccountCapsule.getLatestConsumeTime();
    long issuerNetLimit = calculateGlobalNetLimit(issuerAccountCapsule);
    long newIssuerNetUsage;
    if (!dynamicPropertiesStore.supportUnfreezeDelay()) {
      newIssuerNetUsage = increase(issuerNetUsage, 0, latestConsumeTime, now);
    } else {
      // only participate in the calculation as a temporary variable, without disk flushing
      newIssuerNetUsage = recovery(issuerAccountCapsule, BANDWIDTH, issuerNetUsage,
          latestConsumeTime, now);
    }

    if (bytes > (issuerNetLimit - newIssuerNetUsage)) {
      logger.debug("The {} issuer's bandwidth is not enough."
              + " Bytes: {}, issuerNetLimit: {}, newIssuerNetUsage:{}.",
          tokenID, bytes, issuerNetLimit, newIssuerNetUsage);
      return false;
    }

    latestAssetOperationTime = now;
    publicLatestFreeNetTime = now;
    long latestOperationTime = dynamicPropertiesStore.getLatestBlockHeaderTimestamp();
    if (!dynamicPropertiesStore.supportUnfreezeDelay()) {
      newIssuerNetUsage = increase(newIssuerNetUsage, bytes, now, now);
    } else {
      // Participate in calculation and flush disk persistence
      newIssuerNetUsage = increase(issuerAccountCapsule, BANDWIDTH,
          issuerNetUsage, bytes, latestConsumeTime, now);
    }

    newFreeAssetNetUsage = increase(newFreeAssetNetUsage,
        bytes, latestAssetOperationTime, now);
    newPublicFreeAssetNetUsage = increase(newPublicFreeAssetNetUsage, bytes,
        publicLatestFreeNetTime, now);

    issuerAccountCapsule.setNetUsage(newIssuerNetUsage);
    issuerAccountCapsule.setLatestConsumeTime(now);

    assetIssueCapsule.setPublicFreeAssetNetUsage(newPublicFreeAssetNetUsage);
    assetIssueCapsule.setPublicLatestFreeNetTime(publicLatestFreeNetTime);

    accountCapsule.setLatestOperationTime(latestOperationTime);
    if (dynamicPropertiesStore.getAllowSameTokenName() == 0) {
      accountCapsule.putLatestAssetOperationTimeMap(tokenName,
          latestAssetOperationTime);
      accountCapsule.putFreeAssetNetUsage(tokenName, newFreeAssetNetUsage);
      accountCapsule.putLatestAssetOperationTimeMapV2(tokenID,
          latestAssetOperationTime);
      accountCapsule.putFreeAssetNetUsageV2(tokenID, newFreeAssetNetUsage);

      assetIssueStore.put(assetIssueCapsule.createDbKey(), assetIssueCapsule);

      assetIssueCapsuleV2 = assetIssueV2Store.get(assetIssueCapsule.createDbV2Key());
      assetIssueCapsuleV2.setPublicFreeAssetNetUsage(newPublicFreeAssetNetUsage);
      assetIssueCapsuleV2.setPublicLatestFreeNetTime(publicLatestFreeNetTime);
      assetIssueV2Store
          .put(assetIssueCapsuleV2.createDbV2Key(), assetIssueCapsuleV2);
    } else {
      accountCapsule.putLatestAssetOperationTimeMapV2(tokenID,
          latestAssetOperationTime);
      accountCapsule.putFreeAssetNetUsageV2(tokenID, newFreeAssetNetUsage);
      assetIssueV2Store
          .put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);
    }

    accountStore.put(accountCapsule.createDbKey(), accountCapsule);
    accountStore.put(issuerAccountCapsule.createDbKey(),
        issuerAccountCapsule);

    return true;

  }

  public long calculateGlobalNetLimit(AccountCapsule accountCapsule) {
    long frozeBalance = accountCapsule.getAllFrozenBalanceForBandwidth();
    if (dynamicPropertiesStore.supportUnfreezeDelay()) {
      return calculateGlobalNetLimitV2(frozeBalance);
    }
    if (frozeBalance < TRX_PRECISION) {
      return 0;
    }
    long netWeight = frozeBalance / TRX_PRECISION;
    long totalNetLimit = dynamicPropertiesStore.getTotalNetLimit();
    long totalNetWeight = dynamicPropertiesStore.getTotalNetWeight();
    if (dynamicPropertiesStore.allowNewReward() && totalNetWeight <= 0) {
      return 0;
    }
    if (totalNetWeight == 0) {
      return 0;
    }
    return (long) (netWeight * ((double) totalNetLimit / totalNetWeight));
  }

  public long calculateGlobalNetLimitV2(long frozeBalance) {
    double netWeight = (double) frozeBalance / TRX_PRECISION;
    long totalNetLimit = dynamicPropertiesStore.getTotalNetLimit();
    long totalNetWeight = dynamicPropertiesStore.getTotalNetWeight();
    if (totalNetWeight == 0) {
      return 0;
    }
    return (long) (netWeight * ((double) totalNetLimit / totalNetWeight));
  }

  private boolean useAccountNet(AccountCapsule accountCapsule, long bytes, long now) {

    long netUsage = accountCapsule.getNetUsage();
    long latestConsumeTime = accountCapsule.getLatestConsumeTime();
    long netLimit = calculateGlobalNetLimit(accountCapsule);

    long newNetUsage;
    if (!dynamicPropertiesStore.supportUnfreezeDelay()) {
      newNetUsage = increase(netUsage, 0, latestConsumeTime, now);
    } else {
      // only participate in the calculation as a temporary variable, without disk flushing
      newNetUsage = recovery(accountCapsule, BANDWIDTH, netUsage, latestConsumeTime, now);
    }


    if (bytes > (netLimit - newNetUsage)) {
      logger.debug("Net usage is running out, now use free net usage."
              + " Bytes: {}, netLimit: {}, newNetUsage: {}.",
          bytes, netLimit, newNetUsage);
      return false;
    }

    long latestOperationTime = dynamicPropertiesStore.getLatestBlockHeaderTimestamp();
    if (!dynamicPropertiesStore.supportUnfreezeDelay()) {
      newNetUsage = increase(newNetUsage, bytes, now, now);
    } else {
      // Participate in calculation and flush disk persistence
      newNetUsage = increase(accountCapsule, BANDWIDTH, netUsage, bytes, latestConsumeTime, now);
    }

    accountCapsule.setNetUsage(newNetUsage);
    accountCapsule.setLatestOperationTime(latestOperationTime);
    accountCapsule.setLatestConsumeTime(now);

    accountStore.put(accountCapsule.createDbKey(), accountCapsule);
    return true;
  }

  private boolean useFreeNet(AccountCapsule accountCapsule, long bytes, long now) {

    long freeNetLimit = dynamicPropertiesStore.getFreeNetLimit();
    long freeNetUsage = accountCapsule.getFreeNetUsage();
    long latestConsumeFreeTime = accountCapsule.getLatestConsumeFreeTime();
    long newFreeNetUsage = increase(freeNetUsage, 0, latestConsumeFreeTime, now);

    if (bytes > (freeNetLimit - newFreeNetUsage)) {
      logger.debug("Free net usage is running out."
              + " Bytes: {}, freeNetLimit: {}, newFreeNetUsage: {}.",
          bytes, freeNetLimit, newFreeNetUsage);
      return false;
    }

    long publicNetLimit = dynamicPropertiesStore.getPublicNetLimit();
    long publicNetUsage = dynamicPropertiesStore.getPublicNetUsage();
    long publicNetTime = dynamicPropertiesStore.getPublicNetTime();

    long newPublicNetUsage = increase(publicNetUsage, 0, publicNetTime, now);

    if (bytes > (publicNetLimit - newPublicNetUsage)) {
      logger.debug("Free public net usage is running out."
              + " Bytes: {}, publicNetLimit: {}, newPublicNetUsage: {}.",
          bytes, publicNetLimit, newPublicNetUsage);
      return false;
    }

    latestConsumeFreeTime = now;
    long latestOperationTime = dynamicPropertiesStore.getLatestBlockHeaderTimestamp();
    publicNetTime = now;
    newFreeNetUsage = increase(newFreeNetUsage, bytes, latestConsumeFreeTime, now);
    newPublicNetUsage = increase(newPublicNetUsage, bytes, publicNetTime, now);
    accountCapsule.setFreeNetUsage(newFreeNetUsage);
    accountCapsule.setLatestConsumeFreeTime(latestConsumeFreeTime);
    accountCapsule.setLatestOperationTime(latestOperationTime);

    dynamicPropertiesStore.savePublicNetUsage(newPublicNetUsage);
    dynamicPropertiesStore.savePublicNetTime(publicNetTime);
    accountStore.put(accountCapsule.createDbKey(), accountCapsule);
    return true;

  }

}


