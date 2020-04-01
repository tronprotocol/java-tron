package org.tron.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.DecodeUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.CrossRevokingStore;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.AssetIssueV2Store;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.CrossMessage.Type;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.BalanceContract.CrossContract;
import org.tron.protos.contract.BalanceContract.CrossToken;

@Slf4j(topic = "actuator")
public class CrossChainActuator extends AbstractActuator {

  public CrossChainActuator() {
    super(ContractType.CrossContract, CrossContract.class);
  }

  @Override
  public boolean execute(Object result) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) result;
    try {
      //ack don't execute，todo：a->o->b
      if (tx.getType() != null && tx.getType() == Type.ACK) {
        ret.setStatus(calcFee(), code.SUCESS);
        return true;
      }
      //timeout
      if (tx.getType() != null && tx.getType() == Type.TIME_OUT) {
        timeOutCallBack();
        ret.setStatus(calcFee(), code.SUCESS);
        return true;
      }
      //
      long fee = calcFee();
      AccountStore accountStore = chainBaseManager.getAccountStore();
      DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
      AssetIssueStore assetIssueStore = chainBaseManager.getAssetIssueStore();
      AssetIssueV2Store assetIssueV2Store = chainBaseManager.getAssetIssueV2Store();
      CrossContract crossContract = any.unpack(CrossContract.class);
      byte[] ownerAddress = crossContract.getOwnerAddress().toByteArray();
      byte[] toAddress = crossContract.getToAddress().toByteArray();
      if (tx.isSource()) {
        switch (crossContract.getType()) {
          case TOKEN: {
            CrossToken crossToken = CrossToken.parseFrom(crossContract.getData());
            CrossRevokingStore crossRevokingStore = chainBaseManager.getCrossRevokingStore();
            long amount = crossToken.getAmount();
            String tokenId = ByteArray.toStr(crossToken.getTokenId().toByteArray());
            String tokenChainId = ByteArray.toStr(crossToken.getChainId().toByteArray());
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
          break;
          default:
            break;
        }
      } else {
        switch (crossContract.getType()) {
          case TOKEN: {
            CrossToken crossToken = CrossToken.parseFrom(crossContract.getData());
            CrossRevokingStore crossRevokingStore = chainBaseManager.getCrossRevokingStore();
            long amount = crossToken.getAmount();
            String tokenId = ByteArray.toStr(crossToken.getTokenId().toByteArray());
            String tokenChainId = ByteArray.toStr(crossToken.getChainId().toByteArray());
            Long outTokenCount = crossRevokingStore.getOutTokenCount(tokenChainId, tokenId);
            if (outTokenCount != null) {//source token
              crossRevokingStore.saveOutTokenCount(tokenChainId, tokenId, outTokenCount - amount);
              AssetIssueCapsule assetIssueCapsule = assetIssueV2Store
                  .getUnchecked(ByteArray.fromString(tokenId));
              assetIssueCapsule.setTotalSupply(assetIssueCapsule.getTotalSupply() + amount);
              AccountCapsule accountCapsule = accountStore.get(toAddress);
              accountCapsule.addAssetAmountV2(ByteArray.fromString(tokenId),
                  amount, dynamicStore, assetIssueStore);
              accountStore.put(ownerAddress, accountCapsule);
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
                accountStore.put(ownerAddress, accountCapsule);
                assetIssueV2Store.put(ByteArray.fromString(tokenId), assetIssueCapsule);
              } else {
                //create the asset
                long descTokenId = createAsset(crossToken, crossContract);
                crossRevokingStore
                    .saveTokenMapping(tokenChainId, tokenId, Long.toString(descTokenId));
              }
              Long inTokenCount = crossRevokingStore.getInTokenCount(tokenChainId, tokenId);
              crossRevokingStore.saveInTokenCount(tokenChainId, tokenId,
                  inTokenCount == null ? amount : inTokenCount + amount);
            }
          }
          break;
          default:
            break;
        }
      }
      Commons.adjustBalance(accountStore, ownerAddress, -fee);
      Commons.adjustBalance(accountStore, accountStore.getBlackhole().createDbKey(), fee);
      ret.setStatus(calcFee(), code.SUCESS);
    } catch (InvalidProtocolBufferException | BalanceInsufficientException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(calcFee(), code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (chainBaseManager == null) {
      throw new ContractValidateException("No account store or dynamic store!");
    }
    if (!chainBaseManager.getDynamicPropertiesStore().allowCrossChain()) {
      throw new ContractValidateException("not support cross chain!");
    }
    if (!this.any.is(CrossContract.class)) {
      throw new ContractValidateException(
          "contract type error, expected type [CrossContract], real type[" + any.getClass() + "]");
    }
    AccountStore accountStore = chainBaseManager.getAccountStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    AssetIssueV2Store assetIssueV2Store = chainBaseManager.getAssetIssueV2Store();
    CrossRevokingStore crossRevokingStore = chainBaseManager.getCrossRevokingStore();
    //ack don't valid
    if (tx.getType() != null && (tx.getType() == Type.ACK || tx.getType() == Type.TIME_OUT)) {
      return true;
    }
    //
    try {
      CrossContract crossContract = any.unpack(CrossContract.class);
      String ownerChainId = ByteArray.toStr(crossContract.getOwnerChainId().toByteArray());
      String toChainId = ByteArray.toStr(crossContract.getToChainId().toByteArray());
      if (tx.isSource()) {
        switch (crossContract.getType()) {
          case TOKEN: {
            if (chainBaseManager.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
              throw new ContractValidateException("must allow same token name");
            }
            CrossToken crossToken = CrossToken.parseFrom(crossContract.getData());
            long fee = calcFee();
            byte[] ownerAddress = crossContract.getOwnerAddress().toByteArray();
            byte[] assetId = crossToken.getTokenId().toByteArray();
            long amount = crossToken.getAmount();
            String tokenChainId = ByteArray.toStr(crossToken.getChainId().toByteArray());

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
              assetId = ByteArray.fromString(crossRevokingStore
                  .getDestTokenFromMapping(tokenChainId, ByteArray.toStr(assetId)));
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
          }
          break;
          default:
            break;
        }
      } else {
        switch (crossContract.getType()) {
          case TOKEN: {
            if (chainBaseManager.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
              throw new ContractValidateException("must allow same token name");
            }
            CrossToken crossToken = CrossToken.parseFrom(crossContract.getData());
            byte[] toAddress = crossContract.getToAddress().toByteArray();
            long amount = crossToken.getAmount();
            byte[] assetId = crossToken.getTokenId().toByteArray();
            String tokenChainId = ByteArray.toStr(crossToken.getChainId().toByteArray());

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
          break;
          default:
            break;
        }
      }
      return true;
    } catch (InvalidProtocolBufferException e) {
      throw new ContractValidateException(e.getMessage());
    }
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return null;
  }

  @Override
  public long calcFee() {
    return 0;
  }

  private long createAsset(CrossToken crossToken, CrossContract crossContract) {
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

  public void timeOutCallBack() {
    try {
      Contract contract = tx.getInstance().getRawData().getContract(0);
      CrossContract crossContract = contract.getParameter().unpack(CrossContract.class);
      DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
      AccountStore accountStore = chainBaseManager.getAccountStore();
      AssetIssueStore assetIssueStore = chainBaseManager.getAssetIssueStore();
      AssetIssueV2Store assetIssueV2Store = chainBaseManager.getAssetIssueV2Store();
      byte[] ownerAddress = crossContract.getOwnerAddress().toByteArray();
      String ownerChainId = ByteArray.toStr(crossContract.getOwnerChainId().toByteArray());
      String toChainId = ByteArray.toStr(crossContract.getToChainId().toByteArray());
      switch (crossContract.getType()) {
        case TOKEN: {
          CrossToken crossToken = CrossToken.parseFrom(crossContract.getData());
          CrossRevokingStore crossRevokingStore = chainBaseManager.getCrossRevokingStore();
          long amount = crossToken.getAmount();
          String tokenId = ByteArray.toStr(crossToken.getTokenId().toByteArray());
          String tokenChainId = ByteArray.toStr(crossToken.getChainId().toByteArray());
          Long inTokenCount = crossRevokingStore.getInTokenCount(tokenChainId, tokenId);
          if (inTokenCount != null) {//
            crossRevokingStore.saveInTokenCount(tokenChainId, tokenId, inTokenCount + amount);
            tokenId = crossRevokingStore.getDestTokenFromMapping(tokenChainId, tokenId);
          } else {//source token
            Long outTokenCount = crossRevokingStore.getOutTokenCount(tokenChainId, tokenId);
            crossRevokingStore.saveOutTokenCount(tokenChainId, tokenId,
                outTokenCount == null ? 0 : outTokenCount - amount);
          }
          AccountCapsule accountCapsule = accountStore.get(ownerAddress);
          accountCapsule.addAssetAmountV2(ByteArray.fromString(tokenId),
              amount, dynamicStore, assetIssueStore);
          AssetIssueCapsule assetIssueCapsule = assetIssueV2Store
              .getUnchecked(ByteArray.fromString(tokenId));
          assetIssueCapsule.setTotalSupply(assetIssueCapsule.getTotalSupply() + amount);
          accountStore.put(ownerAddress, accountCapsule);
          assetIssueV2Store.put(ByteArray.fromString(tokenId), assetIssueCapsule);
        }
        break;
        default:
          break;
      }
    } catch (Exception e) {
      logger.error("cross tx: {} execute timeout callback fail!", tx.toString(), e);
    }
  }
}
