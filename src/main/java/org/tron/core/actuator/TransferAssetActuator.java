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
      throw new ContractExeException();
    }
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    try {
      TransferAssetContract transferAssetContract = this.contract
          .unpack(TransferAssetContract.class);

      Preconditions.checkNotNull(transferAssetContract.getOwnerAddress(), "OwnerAddress is null");
      Preconditions.checkNotNull(transferAssetContract.getToAddress(), "ToAddress is null");
      Preconditions.checkNotNull(transferAssetContract.getAssetName(), "AssetName is null");
      Preconditions.checkNotNull(transferAssetContract.getAmount(), "Amount is null");
      if (transferAssetContract.getOwnerAddress().equals(transferAssetContract.getToAddress())) {
        throw new ContractValidateException("Cannot transfer asset to yourself.");
      }
      byte[] ownerKey = transferAssetContract.getOwnerAddress().toByteArray();
      if (!this.dbManager.getAccountStore().has(ownerKey)) {
        throw new ContractValidateException();
      }

      // if account with to_address is not existed,  create it.
      ByteString toAddress = transferAssetContract.getToAddress();
      if (!dbManager.getAccountStore().has(toAddress.toByteArray())) {
        AccountCapsule account = new AccountCapsule(toAddress, AccountType.Normal);
        dbManager.getAccountStore().put(toAddress.toByteArray(), account);
      }

      byte[] nameKey = transferAssetContract.getAssetName().toByteArray();
      if (!this.dbManager.getAssetIssueStore().has(nameKey)) {
        throw new ContractValidateException();
      }

      long amount = transferAssetContract.getAmount();

      AccountCapsule ownerAccount = this.dbManager.getAccountStore().get(ownerKey);
      Map<String, Long> asset = ownerAccount.getAssetMap();

      if (asset.isEmpty()) {
        throw new ContractValidateException();
      }

      Long assetAmount = asset.get(ByteArray.toStr(nameKey));
      if (amount <= 0 || null == assetAmount || amount > assetAmount || assetAmount <= 0) {
        throw new ContractValidateException();
      }
    } catch (InvalidProtocolBufferException e) {
      throw new ContractValidateException();
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
