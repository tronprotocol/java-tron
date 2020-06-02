package org.tron.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.cross.TokenProcess;
import org.tron.core.db.CrossRevokingStore;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.AssetIssueV2Store;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.CrossMessage.Type;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
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
      CrossContract crossContract = any.unpack(CrossContract.class);
      byte[] ownerAddress = crossContract.getOwnerAddress().toByteArray();
      if (tx.isSource()) {
        switch (crossContract.getType()) {
          case TOKEN: {
            TokenProcess.sourceExecute(chainBaseManager, crossContract);
          }
          break;
          default:
            break;
        }
      } else {
        switch (crossContract.getType()) {
          case TOKEN: {
            TokenProcess.destExecute(chainBaseManager, crossContract);
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
    //ack don't valid
    if (tx.getType() != null && (tx.getType() == Type.ACK || tx.getType() == Type.TIME_OUT)) {
      return true;
    }
    //
    try {
      CrossContract crossContract = any.unpack(CrossContract.class);
      if (tx.isSource()) {
        switch (crossContract.getType()) {
          case TOKEN: {
            TokenProcess.sourceValidate(chainBaseManager, crossContract);
          }
          break;
          default:
            break;
        }
      } else {
        switch (crossContract.getType()) {
          case TOKEN: {
            TokenProcess.destValidate(chainBaseManager, crossContract);
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

  public void timeOutCallBack() {
    try {
      Contract contract = tx.getInstance().getRawData().getContract(0);
      CrossContract crossContract = contract.getParameter().unpack(CrossContract.class);
      DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
      AccountStore accountStore = chainBaseManager.getAccountStore();
      AssetIssueStore assetIssueStore = chainBaseManager.getAssetIssueStore();
      AssetIssueV2Store assetIssueV2Store = chainBaseManager.getAssetIssueV2Store();
      byte[] ownerAddress = crossContract.getOwnerAddress().toByteArray();
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
