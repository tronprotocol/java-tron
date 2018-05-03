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

import com.google.common.base.Preconditions;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.AccountStore;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.TransferAssetContract;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;

public class TransferAssetActuator extends AbstractActuator {

  TransferAssetActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    long fee = calcFee();
    if (!this.contract.is(TransferAssetContract.class)) {
      throw new ContractExeException();
    }

    if (this.dbManager == null) {
      throw new ContractExeException();
    }

    try {
      TransferAssetContract transferAssetContract = this.contract
          .unpack(TransferAssetContract.class);
      AccountStore accountStore = this.dbManager.getAccountStore();
      byte[] ownerKey = transferAssetContract.getOwnerAddress().toByteArray();
      byte[] toKey = transferAssetContract.getToAddress().toByteArray();
      ByteString assertName = transferAssetContract.getAssetName();
      long amount = transferAssetContract.getAmount();

      AccountCapsule ownerAccountCapsule = accountStore.get(ownerKey);
      if (!ownerAccountCapsule.reduceAssetAmount(assertName, amount)) {
        throw new ContractExeException("reduceAssetAmount failed !");
      }
      accountStore.put(ownerKey, ownerAccountCapsule);

      AccountCapsule toAccountCapsule = accountStore.get(toKey);
      toAccountCapsule.addAssetAmount(assertName, amount);
      accountStore.put(toKey, toAccountCapsule);

      ret.setStatus(fee, code.SUCESS);
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
    try {
      TransferAssetContract transferAssetContract = this.contract
          .unpack(TransferAssetContract.class);

      if (!Wallet.addressValid(transferAssetContract.getOwnerAddress().toByteArray())) {
        throw new ContractValidateException("Invalidate ownerAddress");
      }
      if (!Wallet.addressValid(transferAssetContract.getToAddress().toByteArray())) {
        throw new ContractValidateException("Invalidate toAddress");
      }
      Preconditions.checkNotNull(transferAssetContract.getAssetName(), "AssetName is null");
      Preconditions.checkNotNull(transferAssetContract.getAmount(), "Amount is null");

      if (transferAssetContract.getOwnerAddress().equals(transferAssetContract.getToAddress())) {
        throw new ContractValidateException("Cannot transfer asset to yourself.");
      }

      byte[] ownerKey = transferAssetContract.getOwnerAddress().toByteArray();
      if (!this.dbManager.getAccountStore().has(ownerKey)) {
        throw new ContractValidateException("No owner account!");
      }

      // if account with to_address is not existed,  create it.
      ByteString toAddress = transferAssetContract.getToAddress();
      if (!dbManager.getAccountStore().has(toAddress.toByteArray())) {
        AccountCapsule account = new AccountCapsule(toAddress, AccountType.Normal);
        dbManager.getAccountStore().put(toAddress.toByteArray(), account);
      }

      byte[] nameKey = transferAssetContract.getAssetName().toByteArray();
      if (!this.dbManager.getAssetIssueStore().has(nameKey)) {
        throw new ContractValidateException("No asset !");
      }

      long amount = transferAssetContract.getAmount();

      AccountCapsule ownerAccount = this.dbManager.getAccountStore().get(ownerKey);
      if (ownerAccount == null) {
        throw new ContractValidateException("Owner account is null!");
      }
      Map<String, Long> asset = ownerAccount.getAssetMap();

      if (asset.isEmpty()) {
        throw new ContractValidateException("Owner no asset!");
      }

      Long assetBalance = asset.get(ByteArray.toStr(nameKey));
      if (amount <= 0) {
        throw new ContractValidateException("Amount must greater than 0.");
      }
      if (null == assetBalance || assetBalance <= 0) {
        throw new ContractValidateException("assetBalance must greater than 0.");
      }
      if (amount > assetBalance) {
        throw new ContractValidateException("assetBalance is not sufficient.");
      }
      AccountCapsule toAccount = this.dbManager.getAccountStore().get(toAddress.toByteArray());
      assetBalance = toAccount.getAssetMap().get(ByteArray.toStr(nameKey));
      if (assetBalance == null) {
        assetBalance = 0L;
      }
      assetBalance = Math.addExact(assetBalance, amount);
    } catch (InvalidProtocolBufferException e) {
      throw new ContractValidateException(e.getMessage());
    } catch (ArithmeticException e) {
      throw new ContractValidateException(e.getMessage());
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() {
    return null;
  }

  @Override
  public long calcFee() {
    return 0;
  }
}