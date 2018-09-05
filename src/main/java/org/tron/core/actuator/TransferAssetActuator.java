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

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.AccountStore;
import org.tron.core.db.Manager;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.TransferAssetContract;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class TransferAssetActuator extends AbstractActuator {

  TransferAssetActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    long fee = calcFee();
    try {
      TransferAssetContract transferAssetContract = this.contract
          .unpack(TransferAssetContract.class);
      AccountStore accountStore = this.dbManager.getAccountStore();
      byte[] ownerAddress = transferAssetContract.getOwnerAddress().toByteArray();
      byte[] toAddress = transferAssetContract.getToAddress().toByteArray();
      AccountCapsule toAccountCapsule = accountStore.get(toAddress);
      if (toAccountCapsule == null) {
        toAccountCapsule = new AccountCapsule(ByteString.copyFrom(toAddress), AccountType.Normal,
            dbManager.getHeadBlockTimeStamp());
        dbManager.getAccountStore().put(toAddress, toAccountCapsule);

        fee = fee + dbManager.getDynamicPropertiesStore().getCreateNewAccountFeeInSystemContract();
      }
      ByteString assetName = transferAssetContract.getAssetName();
      long amount = transferAssetContract.getAmount();

      dbManager.adjustBalance(ownerAddress, -fee);
      dbManager.adjustBalance(dbManager.getAccountStore().getBlackhole().createDbKey(), fee);

      AccountCapsule ownerAccountCapsule = accountStore.get(ownerAddress);
      if (!ownerAccountCapsule.reduceAssetAmount(assetName.toByteArray(), amount)) {
        throw new ContractExeException("reduceAssetAmount failed !");
      }
      accountStore.put(ownerAddress, ownerAccountCapsule);

      toAccountCapsule.addAssetAmount(assetName.toByteArray(), amount);
      accountStore.put(toAddress, toAccountCapsule);

      ret.setStatus(fee, code.SUCESS);
    } catch (BalanceInsufficientException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    } catch (InvalidProtocolBufferException e) {
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    } catch (ArithmeticException e) {
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.contract == null) {
      throw new ContractValidateException("No contract!");
    }
    if (this.dbManager == null) {
      throw new ContractValidateException("No dbManager!");
    }
    if (!this.contract.is(TransferAssetContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [TransferAssetContract],real type[" + contract
              .getClass() + "]");
    }
    final TransferAssetContract transferAssetContract;
    try {
      transferAssetContract = this.contract.unpack(TransferAssetContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    long fee = calcFee();
    byte[] ownerAddress = transferAssetContract.getOwnerAddress().toByteArray();
    byte[] toAddress = transferAssetContract.getToAddress().toByteArray();
    byte[] assetName = transferAssetContract.getAssetName().toByteArray();
    long amount = transferAssetContract.getAmount();

    if (!Wallet.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid ownerAddress");
    }
    if (!Wallet.addressValid(toAddress)) {
      throw new ContractValidateException("Invalid toAddress");
    }
//    if (!TransactionUtil.validAssetName(assetName)) {
//      throw new ContractValidateException("Invalid assetName");
//    }
    if (amount <= 0) {
      throw new ContractValidateException("Amount must greater than 0.");
    }

    if (Arrays.equals(ownerAddress, toAddress)) {
      throw new ContractValidateException("Cannot transfer asset to yourself.");
    }

    AccountCapsule ownerAccount = this.dbManager.getAccountStore().get(ownerAddress);
    if (ownerAccount == null) {
      throw new ContractValidateException("No owner account!");
    }

    if (!this.dbManager.getAssetIssueStore().has(assetName)) {
      throw new ContractValidateException("No asset !");
    }

    Map<String, Long> asset = ownerAccount.getAssetMap();
    if (asset.isEmpty()) {
      throw new ContractValidateException("Owner no asset!");
    }

    Long assetBalance = asset.get(ByteArray.toStr(assetName));
    if (null == assetBalance || assetBalance <= 0) {
      throw new ContractValidateException("assetBalance must greater than 0.");
    }
    if (amount > assetBalance) {
      throw new ContractValidateException("assetBalance is not sufficient.");
    }

    AccountCapsule toAccount = this.dbManager.getAccountStore().get(toAddress);
    if (toAccount != null) {
      assetBalance = toAccount.getAssetMap().get(ByteArray.toStr(assetName));
      if (assetBalance != null) {
        try {
          assetBalance = Math.addExact(assetBalance, amount); //check if overflow
        } catch (Exception e) {
          logger.debug(e.getMessage(), e);
          throw new ContractValidateException(e.getMessage());
        }
      }
    } else {
      fee = fee + dbManager.getDynamicPropertiesStore().getCreateNewAccountFeeInSystemContract();
      if (ownerAccount.getBalance() < fee) {
        throw new ContractValidateException(
            "Validate TransferAssetActuator error, insufficient fee.");
      }
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(TransferAssetContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }
}
