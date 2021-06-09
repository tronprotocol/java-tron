package org.tron.core.cross;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.DecodeUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.db.CrossRevokingStore;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.AssetIssueV2Store;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.BalanceContract.CrossContract;
import org.tron.protos.contract.BalanceContract.CrossToken;

@Slf4j
public class TokenProcess {

  public static void sourceExecute(ChainBaseManager chainBaseManager, CrossContract crossContract)
      throws InvalidProtocolBufferException {
    AccountStore accountStore = chainBaseManager.getAccountStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    AssetIssueStore assetIssueStore = chainBaseManager.getAssetIssueStore();
    AssetIssueV2Store assetIssueV2Store = chainBaseManager.getAssetIssueV2Store();
    CrossToken crossToken = CrossToken.parseFrom(crossContract.getData());
    CrossRevokingStore crossRevokingStore = chainBaseManager.getCrossRevokingStore();
    long amount = crossToken.getAmount();
    byte[] ownerAddress = crossContract.getOwnerAddress().toByteArray();
    String tokenId = ByteArray.toStr(crossToken.getTokenId().toByteArray());
    String tokenChainId = ByteArray.toHexString(crossToken.getChainId().toByteArray());
    Long inTokenCount = crossRevokingStore.getInTokenCount(tokenChainId, tokenId);
    if (inTokenCount != null) {//
      crossRevokingStore.saveInTokenCount(tokenChainId, tokenId, inTokenCount - amount);
      tokenId = crossRevokingStore.getDestTokenFromMapping(tokenChainId, tokenId);
    } else {//source token
      Long outTokenCount = crossRevokingStore.getOutTokenCount(tokenChainId, tokenId);
      crossRevokingStore.saveOutTokenCount(tokenChainId, tokenId,
          outTokenCount == null ? amount : outTokenCount + amount);
    }
    AccountCapsule accountCapsule = accountStore.get(ownerAddress);
    accountCapsule.reduceAssetAmountV2(ByteArray.fromString(tokenId),
        amount, dynamicStore, assetIssueStore);
    AssetIssueCapsule assetIssueCapsule = assetIssueV2Store
        .getUnchecked(ByteArray.fromString(tokenId));
    assetIssueCapsule.setTotalSupply(assetIssueCapsule.getTotalSupply() - amount);
    accountStore.put(ownerAddress, accountCapsule);
    assetIssueV2Store.put(ByteArray.fromString(tokenId), assetIssueCapsule);
  }

  public static void destExecute(ChainBaseManager chainBaseManager, CrossContract crossContract)
      throws InvalidProtocolBufferException {
    AccountStore accountStore = chainBaseManager.getAccountStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    AssetIssueStore assetIssueStore = chainBaseManager.getAssetIssueStore();
    AssetIssueV2Store assetIssueV2Store = chainBaseManager.getAssetIssueV2Store();
    CrossToken crossToken = CrossToken.parseFrom(crossContract.getData());
    CrossRevokingStore crossRevokingStore = chainBaseManager.getCrossRevokingStore();
    long amount = crossToken.getAmount();
    byte[] ownerAddress = crossContract.getOwnerAddress().toByteArray();
    byte[] toAddress = crossContract.getToAddress().toByteArray();
    String tokenId = ByteArray.toStr(crossToken.getTokenId().toByteArray());
    String tokenChainId = ByteArray.toHexString(crossToken.getChainId().toByteArray());
    Long outTokenCount = crossRevokingStore.getOutTokenCount(tokenChainId, tokenId);
    if (outTokenCount != null) {//source token
      crossRevokingStore.saveOutTokenCount(tokenChainId, tokenId, outTokenCount - amount);
      AssetIssueCapsule assetIssueCapsule = assetIssueV2Store
          .getUnchecked(ByteArray.fromString(tokenId));
      assetIssueCapsule.setTotalSupply(assetIssueCapsule.getTotalSupply() + amount);
      AccountCapsule accountCapsule = accountStore.get(toAddress);
      accountCapsule.addAssetAmountV2(ByteArray.fromString(tokenId),
          amount, dynamicStore, assetIssueStore);
      accountStore.put(toAddress, accountCapsule);
      assetIssueV2Store.put(ByteArray.fromString(tokenId), assetIssueCapsule);
    } else {
      String destTokenId = crossRevokingStore
          .getDestTokenFromMapping(tokenChainId, tokenId);
      if (StringUtils.isNotBlank(destTokenId)) {//Exist dest token
        AssetIssueCapsule assetIssueCapsule = assetIssueV2Store
            .getUnchecked(ByteArray.fromString(destTokenId));
        assetIssueCapsule.setTotalSupply(assetIssueCapsule.getTotalSupply() + amount);
        AccountCapsule accountCapsule = accountStore.get(toAddress);
        accountCapsule.addAssetAmountV2(ByteArray.fromString(destTokenId),
            amount, dynamicStore, assetIssueStore);
        accountStore.put(toAddress, accountCapsule);
        assetIssueV2Store.put(ByteArray.fromString(destTokenId), assetIssueCapsule);
      } else {
        //create the asset
        long descTokenId = createAsset(chainBaseManager, crossToken, crossContract);
        crossRevokingStore
            .saveTokenMapping(tokenChainId, tokenId, Long.toString(descTokenId));
      }
      Long inTokenCount = crossRevokingStore.getInTokenCount(tokenChainId, tokenId);
      crossRevokingStore.saveInTokenCount(tokenChainId, tokenId,
          inTokenCount == null ? amount : inTokenCount + amount);
    }
  }

  public static void sourceValidate(ChainBaseManager chainBaseManager, CrossContract crossContract)
      throws ContractValidateException, InvalidProtocolBufferException {
    if (chainBaseManager.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
      throw new ContractValidateException("must allow same token name");
    }
    AccountStore accountStore = chainBaseManager.getAccountStore();
    AssetIssueV2Store assetIssueV2Store = chainBaseManager.getAssetIssueV2Store();
    CrossRevokingStore crossRevokingStore = chainBaseManager.getCrossRevokingStore();

    String ownerChainId = ByteArray.toHexString(crossContract.getOwnerChainId().toByteArray());
    String toChainId = ByteArray.toHexString(crossContract.getToChainId().toByteArray());
    CrossToken crossToken = CrossToken.parseFrom(crossContract.getData());
    long fee = calcFee(chainBaseManager);
    byte[] ownerAddress = crossContract.getOwnerAddress().toByteArray();
    byte[] assetId = crossToken.getTokenId().toByteArray();
    long amount = crossToken.getAmount();
    String tokenChainId = ByteArray.toHexString(crossToken.getChainId().toByteArray());
    String localChainId = ByteArray
        .toHexString(chainBaseManager.getGenesisBlockId().getByteString().toByteArray());

    if (!StringUtils.equals(ownerChainId, localChainId)) {
      logger.error("ownerChainId not equals localChainId! ownerChainId:{}, localChainId:{}",
          ownerChainId, localChainId);
      throw new ContractValidateException("Invalid owner chainId");
    }
    if (StringUtils.equals(toChainId, localChainId)) {
      logger.error("toChainId equals localChainId! toChainId:{}, localChainId:{}",
          ownerChainId, localChainId);
      throw new ContractValidateException("Invalid toChainId");
    }

    if (!StringUtils.equals(tokenChainId, ownerChainId) && !StringUtils
        .equals(tokenChainId, toChainId)) {
      logger.error("tokenChainId:{}, ownerChainId:{}, toChainId:{}", tokenChainId,
          ownerChainId, toChainId);
      throw new ContractValidateException("Invalid token chainId");
    }

    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid ownerAddress");
    }
    if (amount <= 0) {
      throw new ContractValidateException("Amount must be greater than 0.");
    }

    AccountCapsule ownerAccount = accountStore.get(ownerAddress);
    if (ownerAccount == null) {
      throw new ContractValidateException("No owner account!");
    }
    if (ownerAccount.getBalance() < fee) {
      throw new ContractValidateException(
          "Validate CrossChainActuator error, insufficient fee.");
    }
    Long inToken = crossRevokingStore
        .getInTokenCount(tokenChainId, ByteArray.toStr(assetId));
    if (inToken != null) {
      if (amount > inToken.longValue()) {
        throw new ContractValidateException("inToken is not sufficient.");
      }
      if (!StringUtils.equals(tokenChainId, toChainId)) {
        logger.error("tokenChainId:{}, toChainId:{}", tokenChainId, toChainId);
        throw new ContractValidateException("Invalid token chainId");
      }
      assetId = ByteArray.fromString(crossRevokingStore
          .getDestTokenFromMapping(tokenChainId, ByteArray.toStr(assetId)));
    } else if (crossRevokingStore.containMapping(ByteArray.toStr(assetId))) {
      logger.error("assetId {} is mapping token,tokenChainId:{}, ownerChainId:{}, toChainId:{}",
          ByteArray.toStr(assetId), tokenChainId, ownerChainId, toChainId);
      throw new ContractValidateException("Invalid token chainId");
    }
    boolean contain = assetIssueV2Store.has(assetId);
    Long assetBalance = ownerAccount.getAssetMapV2().get(ByteArray.toStr(assetId));
    if (!contain) {
      throw new ContractValidateException("No asset!");
    }
    if (null == assetBalance || assetBalance <= 0) {
      throw new ContractValidateException("assetBalance must be greater than 0.");
    }
    if (amount > assetBalance) {
      throw new ContractValidateException("assetBalance is not sufficient.");
    }
    AssetIssueCapsule assetIssueCapsule = assetIssueV2Store.get(assetId);
    if (!assetIssueCapsule.getName().equals(crossToken.getTokenName())) {
      throw new ContractValidateException("token name is not matched");
    }
    if (assetIssueCapsule.getPrecision() != crossToken.getPrecision()) {
      throw new ContractValidateException("token precision is not matched");
    }
  }

  public static void destValidate(ChainBaseManager chainBaseManager, CrossContract crossContract)
      throws ContractValidateException, InvalidProtocolBufferException {
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    if (dynamicStore.getAllowSameTokenName() == 0) {
      throw new ContractValidateException("must allow same token name");
    }
    AccountStore accountStore = chainBaseManager.getAccountStore();
    CrossRevokingStore crossRevokingStore = chainBaseManager.getCrossRevokingStore();
    String ownerChainId = ByteArray.toHexString(crossContract.getOwnerChainId().toByteArray());
    String toChainId = ByteArray.toHexString(crossContract.getToChainId().toByteArray());
    CrossToken crossToken = CrossToken.parseFrom(crossContract.getData());
    byte[] toAddress = crossContract.getToAddress().toByteArray();
    long amount = crossToken.getAmount();
    byte[] assetId = crossToken.getTokenId().toByteArray();
    String tokenChainId = ByteArray.toHexString(crossToken.getChainId().toByteArray());
    String localChainId = ByteArray
        .toHexString(chainBaseManager.getGenesisBlockId().getByteString().toByteArray());

    if (StringUtils.equals(ownerChainId, localChainId)) {
      logger.error("ownerChainId equals localChainId! ownerChainId:{}, localChainId:{}",
          ownerChainId, localChainId);
      throw new ContractValidateException("Invalid owner chainId");
    }
    if (!StringUtils.equals(toChainId, localChainId)) {
      logger.error("toChainId not equals localChainId! toChainId:{}, localChainId:{}",
          ownerChainId, localChainId);
      throw new ContractValidateException("Invalid toChainId");
    }

    if (!StringUtils.equals(tokenChainId, ownerChainId) && !StringUtils
        .equals(tokenChainId, toChainId)) {
      logger.error("tokenChainId:{}, ownerChainId:{}, toChainId:{}", tokenChainId,
          ownerChainId, toChainId);
      throw new ContractValidateException("Invalid token chainId");
    }

    if (!DecodeUtil.addressValid(toAddress)) {
      throw new ContractValidateException("Invalid toAddress");
    }
    if (amount <= 0) {
      throw new ContractValidateException("Amount must be greater than 0.");
    }

    AccountCapsule toAccount = accountStore.get(toAddress);
    if (toAccount != null) {
      //after TvmSolidity059 proposal, send trx to smartContract by actuator is not allowed.
      if (dynamicStore.getAllowTvmSolidity059() == 1
          && toAccount.getType() == AccountType.Contract) {
        throw new ContractValidateException("Cannot transfer asset to smartContract.");
      }
    } else {
      throw new ContractValidateException("Invalid toAddress");
    }
    Long outToken = crossRevokingStore
        .getOutTokenCount(tokenChainId, ByteArray.toStr(assetId));
    if (outToken != null && amount > outToken.longValue()) {
      throw new ContractValidateException("outToken is not sufficient.");
    }
  }

  private static long createAsset(ChainBaseManager chainBaseManager, CrossToken crossToken,
      CrossContract crossContract) {
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    AssetIssueV2Store assetIssueV2Store = chainBaseManager.getAssetIssueV2Store();
    AccountStore accountStore = chainBaseManager.getAccountStore();
    long tokenIdNum = dynamicStore.getTokenIdNum();
    tokenIdNum++;
    AssetIssueContract.Builder builder = AssetIssueContract.newBuilder();
    builder.setName(crossToken.getTokenName()).setTotalSupply(crossToken.getAmount())
        .setOwnerAddress(crossContract.getToAddress()).setId(Long.toString(tokenIdNum))
        .setPrecision(crossToken.getPrecision());
    AssetIssueCapsule assetIssueCapsuleV2 = new AssetIssueCapsule(builder.build());
    dynamicStore.saveTokenIdNum(tokenIdNum);
    assetIssueV2Store.put(assetIssueCapsuleV2.createDbV2Key(), assetIssueCapsuleV2);
    byte[] ownerAddress = crossContract.getToAddress().toByteArray();
    AccountCapsule accountCapsule = accountStore.get(ownerAddress);
    long remainSupply = crossToken.getAmount();

    accountCapsule.addAssetV2(assetIssueCapsuleV2.createDbV2Key(), remainSupply);
    accountStore.put(ownerAddress, accountCapsule);
    return tokenIdNum;
  }

  public static long calcFee(ChainBaseManager chainBaseManager) {
    return 0;
  }
}
