/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.DecodeUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.AssetIssueV2Store;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;

@Slf4j(topic = "actuator")
public class TransferAssetActuator extends AbstractActuator {

  public TransferAssetActuator() {
    super(ContractType.TransferAssetContract, TransferAssetContract.class);
  }

  @Override
  public boolean execute(Object result) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) result;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    long fee = calcFee();
    AccountStore accountStore = chainBaseManager.getAccountStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    AssetIssueStore assetIssueStore = chainBaseManager.getAssetIssueStore();
    try {
      TransferAssetContract transferAssetContract = this.any.unpack(TransferAssetContract.class);
      byte[] ownerAddress = transferAssetContract.getOwnerAddress().toByteArray();
      byte[] toAddress = transferAssetContract.getToAddress().toByteArray();
      AccountCapsule toAccountCapsule = accountStore.get(toAddress);
      if (toAccountCapsule == null) {
        boolean withDefaultPermission =
            dynamicStore.getAllowMultiSign() == 1;
        toAccountCapsule = new AccountCapsule(ByteString.copyFrom(toAddress), AccountType.Normal,
            dynamicStore.getLatestBlockHeaderTimestamp(), withDefaultPermission, dynamicStore);
        accountStore.put(toAddress, toAccountCapsule);

        fee = fee + dynamicStore.getCreateNewAccountFeeInSystemContract();
      }
      ByteString assetName = transferAssetContract.getAssetName();
      long amount = transferAssetContract.getAmount();

      AccountCapsule ownerAccountCapsule = accountStore.get(ownerAddress);
      if (!ownerAccountCapsule
          .reduceAssetAmountV2(assetName.toByteArray(), amount, dynamicStore, assetIssueStore)) {
        throw new ContractExeException("reduceAssetAmount failed !");
      }
      accountStore.put(ownerAddress, ownerAccountCapsule);

      toAccountCapsule
          .addAssetAmountV2(assetName.toByteArray(), amount, dynamicStore, assetIssueStore);
      accountStore.put(toAddress, toAccountCapsule);

      Commons.adjustBalance(accountStore, ownerAccountCapsule, -fee);
      if (dynamicStore.supportBlackHoleOptimization()) {
        dynamicStore.burnTrx(fee);
      } else {
        Commons.adjustBalance(accountStore, accountStore.getBlackhole(), fee);
      }
      ret.setStatus(fee, code.SUCESS);
    } catch (BalanceInsufficientException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    } catch (InvalidProtocolBufferException | ArithmeticException e) {
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
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    AssetIssueStore assetIssueStore = chainBaseManager.getAssetIssueStore();
    AssetIssueV2Store assetIssueV2Store = chainBaseManager.getAssetIssueV2Store();
    if (!this.any.is(TransferAssetContract.class)) {
      throw new ContractValidateException(
          "contract type error, expected type [TransferAssetContract], real type[" + any
              .getClass() + "]");
    }
    final TransferAssetContract transferAssetContract;
    try {
      transferAssetContract = this.any.unpack(TransferAssetContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    long fee = calcFee();
    byte[] ownerAddress = transferAssetContract.getOwnerAddress().toByteArray();
    byte[] toAddress = transferAssetContract.getToAddress().toByteArray();
    byte[] assetName = transferAssetContract.getAssetName().toByteArray();
    long amount = transferAssetContract.getAmount();

    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid ownerAddress");
    }
    if (!DecodeUtil.addressValid(toAddress)) {
      throw new ContractValidateException("Invalid toAddress");
    }

    if (amount <= 0) {
      throw new ContractValidateException("Amount must be greater than 0.");
    }

    if (Arrays.equals(ownerAddress, toAddress)) {
      throw new ContractValidateException("Cannot transfer asset to yourself.");
    }

    AccountCapsule ownerAccount = accountStore.get(ownerAddress);
    if (ownerAccount == null) {
      throw new ContractValidateException("No owner account!");
    }

    if (!Commons.getAssetIssueStoreFinal(dynamicStore, assetIssueStore, assetIssueV2Store)
        .has(assetName)) {
      throw new ContractValidateException("No asset!");
    }

    Map<String, Long> asset;
    if (dynamicStore.getAllowSameTokenName() == 0) {
      asset = ownerAccount.getAssetMap();
    } else {
      asset = ownerAccount.getAssetMapV2();
    }
    if (asset.isEmpty()) {
      throw new ContractValidateException("Owner has no asset!");
    }

    Long assetBalance = asset.get(ByteArray.toStr(assetName));
    if (null == assetBalance || assetBalance <= 0) {
      throw new ContractValidateException("assetBalance must be greater than 0.");
    }
    if (amount > assetBalance) {
      throw new ContractValidateException("assetBalance is not sufficient.");
    }

    AccountCapsule toAccount = accountStore.get(toAddress);
    if (toAccount != null) {
      //after ForbidTransferToContract proposal, send trx to smartContract by actuator is not allowed.
      if (dynamicStore.getForbidTransferToContract() == 1
          && toAccount.getType() == AccountType.Contract) {
        throw new ContractValidateException("Cannot transfer asset to smartContract.");
      }

      if (dynamicStore.getAllowSameTokenName() == 0) {
        assetBalance = toAccount.getAssetMap().get(ByteArray.toStr(assetName));
      } else {
        assetBalance = toAccount.getAssetMapV2().get(ByteArray.toStr(assetName));
      }
      if (assetBalance != null) {
        try {
          assetBalance = Math.addExact(assetBalance, amount); //check if overflow
        } catch (Exception e) {
          logger.debug(e.getMessage(), e);
          throw new ContractValidateException(e.getMessage());
        }
      }
    } else {
      fee = fee + dynamicStore.getCreateNewAccountFeeInSystemContract();
      if (ownerAccount.getBalance() < fee) {
        throw new ContractValidateException(
            "Validate TransferAssetActuator error, insufficient fee.");
      }
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(TransferAssetContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }
}
