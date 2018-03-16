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
import java.util.Map;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.AccountStore;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.TransferAssertContract;
import org.tron.protos.Protocol.Transaction.Result.code;

public class TransferAssertActuator extends AbstractActuator {

  TransferAssertActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    long fee = calcFee();
    if (!this.contract.is(TransferAssertContract.class)) {
      throw new ContractExeException();
    }

    if (this.dbManager == null) {
      throw new ContractExeException();
    }

    try {
      TransferAssertContract transferAssertContract = this.contract
          .unpack(TransferAssertContract.class);
      AccountStore accountStore = this.dbManager.getAccountStore();
      byte[] ownerKey = transferAssertContract.getOwnerAddress().toByteArray();
      byte[] toKey = transferAssertContract.getToAddress().toByteArray();
      ByteString assertName = transferAssertContract.getAssertName();
      long amount = transferAssertContract.getAmount();

      AccountCapsule ownerAccountCapsule = accountStore.get(ownerKey);
      ownerAccountCapsule.reduceAssetAmount(assertName, amount);
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
      TransferAssertContract transferAssertContract = this.contract
          .unpack(TransferAssertContract.class);

      byte[] ownerKey = transferAssertContract.getOwnerAddress().toByteArray();
      if (!this.dbManager.getAccountStore().has(ownerKey)) {
        throw new ContractValidateException();
      }

      byte[] toKey = transferAssertContract.getToAddress().toByteArray();
      if (!this.dbManager.getAccountStore().has(toKey)) {
        throw new ContractValidateException();
      }

      byte[] nameKey = transferAssertContract.getAssertName().toByteArray();
      if (!this.dbManager.getAssetIssueStore().has(nameKey)) {
        throw new ContractValidateException();
      }

      long amount = transferAssertContract.getAmount();

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
