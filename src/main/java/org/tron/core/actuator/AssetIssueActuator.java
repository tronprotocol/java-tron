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
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.db.Manager;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class AssetIssueActuator extends AbstractActuator {

  AssetIssueActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    long fee = calcFee();
    try {
      if (!this.contract.is(AssetIssueContract.class)) {
        throw new ContractExeException();
      }

      if (dbManager == null) {
        throw new ContractExeException();
      }
      AssetIssueContract assetIssueContract = contract.unpack(AssetIssueContract.class);
      AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
      dbManager.getAssetIssueStore()
          .put(assetIssueCapsule.getName().toByteArray(), assetIssueCapsule);

      dbManager.adjustBalance(assetIssueContract.getOwnerAddress().toByteArray(), -calcFee());
      ret.setStatus(fee, code.SUCESS);

      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(assetIssueContract.getOwnerAddress().toByteArray());

      accountCapsule.addAsset(ByteArray.toStr(assetIssueContract.getName().toByteArray()),
          assetIssueContract.getTotalSupply());

      dbManager.getAccountStore()
          .put(assetIssueContract.getOwnerAddress().toByteArray(), accountCapsule);
    } catch (InvalidProtocolBufferException e) {
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException();
    } catch (BalanceInsufficientException e) {
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException();
    }
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (!this.contract.is(AssetIssueContract.class)) {
      throw new ContractValidateException();
    }

    try {
      final AssetIssueContract assetIssueContract = this.contract.unpack(AssetIssueContract.class);

      Preconditions.checkNotNull(assetIssueContract.getOwnerAddress(), "OwnerAddress is null");
      Preconditions.checkNotNull(assetIssueContract.getName(), "name is null");

      if (this.dbManager.getAssetIssueStore().get(assetIssueContract.getName().toByteArray())
          != null) {
        throw new ContractValidateException();
      }

    } catch (InvalidProtocolBufferException e) {
      throw new ContractValidateException();
    }

    return false;
  }

  @Override
  public ByteString getOwnerAddress() {
    return null;
  }

  @Override
  public long calcFee() {
    return ChainConstant.ASSET_ISSUE_FEE;
  }
}
