package org.tron.core.actuator;

import static org.tron.core.actuator.ActuatorConstant.ACCOUNT_EXCEPTION_STR;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AccountAssetIssueCapsule;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AccountAssetIssueStore;
import org.tron.core.store.AccountStore;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Account.Frozen;
import org.tron.protos.Protocol.AccountAssetIssue;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.AssetIssueContractOuterClass.UnfreezeAssetContract;

@Slf4j(topic = "actuator")
public class UnfreezeAssetActuator extends AbstractActuator {

  public UnfreezeAssetActuator() {
    super(ContractType.UnfreezeAssetContract, UnfreezeAssetContract.class);
  }

  @Override
  public boolean execute(Object object) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) object;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    long fee = calcFee();
    AccountStore accountStore = chainBaseManager.getAccountStore();
    AssetIssueStore assetIssueStore = chainBaseManager.getAssetIssueStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    AccountAssetIssueStore accountAssetIssueStore = chainBaseManager.getAccountAssetIssueStore();

    try {
      final UnfreezeAssetContract unfreezeAssetContract = any.unpack(UnfreezeAssetContract.class);
      byte[] ownerAddress = unfreezeAssetContract.getOwnerAddress().toByteArray();
      long unfreezeAsset = 0L;

      if (dynamicStore.getAllowAccountAssetOptimization() == 1) {
        AccountAssetIssueCapsule accountAssetIssueCapsule = accountAssetIssueStore.get(ownerAddress);
        List<AccountAssetIssue.Frozen> frozenList = Lists.newArrayList();
        frozenList.addAll(accountAssetIssueCapsule.getFrozenSupplyList());
        unfreezeAsset = getAccountAssetIssueFrozenList(frozenList, dynamicStore, unfreezeAsset);

        if (dynamicStore.getAllowSameTokenName() == 0) {
          accountAssetIssueCapsule
                  .addAssetAmountV2(accountAssetIssueCapsule.getAssetIssuedName().toByteArray(), unfreezeAsset,
                          dynamicStore, assetIssueStore);
        } else {
          accountAssetIssueCapsule
                  .addAssetAmountV2(accountAssetIssueCapsule.getAssetIssuedID().toByteArray(), unfreezeAsset,
                          dynamicStore, assetIssueStore);
        }
        accountAssetIssueCapsule.setInstance(accountAssetIssueCapsule.getInstance().toBuilder()
                .clearFrozenSupply().addAllFrozenSupply(frozenList).build());
        accountAssetIssueStore.put(ownerAddress, accountAssetIssueCapsule);
      } else {
        AccountCapsule accountCapsule = accountStore.get(ownerAddress);
        List<Account.Frozen> frozenList = Lists.newArrayList();
        frozenList.addAll(accountCapsule.getFrozenSupplyList());
        unfreezeAsset = getAccountFrozenList(frozenList, dynamicStore, unfreezeAsset);

        if (dynamicStore.getAllowSameTokenName() == 0) {
          accountCapsule
                  .addAssetAmountV2(accountCapsule.getAssetIssuedName().toByteArray(), unfreezeAsset,
                          dynamicStore, assetIssueStore);
        } else {
          accountCapsule
                  .addAssetAmountV2(accountCapsule.getAssetIssuedID().toByteArray(), unfreezeAsset,
                          dynamicStore, assetIssueStore);
        }
        accountCapsule.setInstance(accountCapsule.getInstance().toBuilder()
                .clearFrozenSupply().addAllFrozenSupply(frozenList).build());
        accountStore.put(ownerAddress, accountCapsule);
      }
      ret.setStatus(fee, code.SUCESS);
    } catch (InvalidProtocolBufferException | ArithmeticException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.any == null) {
      throw new ContractValidateException(ActuatorConstant.CONTRACT_NOT_EXIST);
    }
    if (chainBaseManager == null) {
      throw new ContractValidateException(ActuatorConstant.STORE_NOT_EXIST);
    }
    AccountStore accountStore = chainBaseManager.getAccountStore();
    AccountAssetIssueStore accountAssetIssueStore = chainBaseManager.getAccountAssetIssueStore();

    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    if (!this.any.is(UnfreezeAssetContract.class)) {
      throw new ContractValidateException(
          "contract type error, expected type [UnfreezeAssetContract], real type[" + any
              .getClass() + "]");
    }
    final UnfreezeAssetContract unfreezeAssetContract;
    try {
      unfreezeAssetContract = this.any.unpack(UnfreezeAssetContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = unfreezeAssetContract.getOwnerAddress().toByteArray();
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    AccountCapsule accountCapsule = accountStore.get(ownerAddress);
    if (accountCapsule == null) {
      String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
      throw new ContractValidateException(
          ACCOUNT_EXCEPTION_STR + readableOwnerAddress + "] does not exist");
    }

    if (dynamicStore.getAllowAccountAssetOptimization() == 1) {
      AccountAssetIssueCapsule accountAssetIssueCapsule = accountAssetIssueStore.get(ownerAddress);
      if (accountAssetIssueCapsule.getFrozenSupplyCount() <= 0) {
        throw new ContractValidateException("no frozen supply balance");
      }
      if (dynamicStore.getAllowSameTokenName() == 0) {
        if (accountAssetIssueCapsule.getAssetIssuedName().isEmpty()) {
          throw new ContractValidateException("this account has not issued any asset");
        }
      } else {
        if (accountAssetIssueCapsule.getAssetIssuedID().isEmpty()) {
          throw new ContractValidateException("this account has not issued any asset");
        }
      }
      long now = dynamicStore.getLatestBlockHeaderTimestamp();
      long allowedUnfreezeCount = accountAssetIssueCapsule.getFrozenSupplyList().stream()
              .filter(frozen -> frozen.getExpireTime() <= now).count();
      if (allowedUnfreezeCount <= 0) {
        throw new ContractValidateException("It's not time to unfreeze asset supply");
      }
    } else {
      if (accountCapsule.getFrozenSupplyCount() <= 0) {
        throw new ContractValidateException("no frozen supply balance");
      }
      if (dynamicStore.getAllowSameTokenName() == 0) {
        if (accountCapsule.getAssetIssuedName().isEmpty()) {
          throw new ContractValidateException("this account has not issued any asset");
        }
      } else {
        if (accountCapsule.getAssetIssuedID().isEmpty()) {
          throw new ContractValidateException("this account has not issued any asset");
        }
      }
      long now = dynamicStore.getLatestBlockHeaderTimestamp();
      long allowedUnfreezeCount = accountCapsule.getFrozenSupplyList().stream()
              .filter(frozen -> frozen.getExpireTime() <= now).count();
      if (allowedUnfreezeCount <= 0) {
        throw new ContractValidateException("It's not time to unfreeze asset supply");
      }
    }
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(UnfreezeAssetContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

  public long getAccountAssetIssueFrozenList(List<AccountAssetIssue.Frozen> frozenList, DynamicPropertiesStore dynamicStore, long unfreezeAsset) {
    Iterator<AccountAssetIssue.Frozen> iterator = frozenList.iterator();
    long now = dynamicStore.getLatestBlockHeaderTimestamp();
    while (iterator.hasNext()) {
      AccountAssetIssue.Frozen next = iterator.next();
      if (next.getExpireTime() <= now) {
        unfreezeAsset += next.getFrozenBalance();
        iterator.remove();
      }
    }
    return unfreezeAsset;
  }

  public long getAccountFrozenList(List<Account.Frozen> frozenList, DynamicPropertiesStore dynamicStore, long unfreezeAsset) {
    Iterator<Account.Frozen> iterator = frozenList.iterator();
    long now = dynamicStore.getLatestBlockHeaderTimestamp();
    while (iterator.hasNext()) {
      Frozen next = iterator.next();
      if (next.getExpireTime() <= now) {
        unfreezeAsset += next.getFrozenBalance();
        iterator.remove();
      }
    }
    return unfreezeAsset;
  }

}
