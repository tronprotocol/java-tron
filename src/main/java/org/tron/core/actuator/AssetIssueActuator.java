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
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.utils.TransactionUtil;
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
      byte[] ownerAddress = assetIssueContract.getOwnerAddress().toByteArray();
      AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
      dbManager.getAssetIssueStore()
          .put(assetIssueCapsule.getName().toByteArray(), assetIssueCapsule);

      dbManager.adjustBalance(ownerAddress, -calcFee());
      dbManager.adjustBalance(dbManager.getAccountStore().getBlackhole().getAddress().toByteArray(),
          calcFee());//send to blackhole

      AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      accountCapsule.addAsset(ByteArray.toStr(assetIssueContract.getName().toByteArray()),
          assetIssueContract.getTotalSupply());
      dbManager.getAccountStore().put(ownerAddress, accountCapsule);

      ret.setStatus(fee, code.SUCESS);
      return true;
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    } catch (BalanceInsufficientException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    } catch (ArithmeticException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (!this.contract.is(AssetIssueContract.class)) {
      throw new ContractValidateException();
    }

    try {
      final AssetIssueContract assetIssueContract = this.contract.unpack(AssetIssueContract.class);

      if (!Wallet.addressValid(assetIssueContract.getOwnerAddress().toByteArray())) {
        throw new ContractValidateException("Invalidate ownerAddress");
      }
      if (!TransactionUtil.validAssetName(assetIssueContract.getName().toByteArray())) {
        throw new ContractValidateException("Invalidate assetName");
      }
      if (!TransactionUtil.validUrl(assetIssueContract.getUrl().toByteArray())) {
        throw new ContractValidateException("Invalidate url");
      }
      if (!TransactionUtil
          .validAssetDescription(assetIssueContract.getDescription().toByteArray())) {
        throw new ContractValidateException("Invalidate description");
      }
      if (assetIssueContract.getTotalSupply() <= 0) {
        throw new ContractValidateException("TotalSupply must greater than 0!");
      }

      if (assetIssueContract.getTrxNum() <= 0) {
        throw new ContractValidateException("TrxNum must greater than 0!");
      }

      if (assetIssueContract.getNum() <= 0) {
        throw new ContractValidateException("Num must greater than 0!");
      }

      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(assetIssueContract.getOwnerAddress().toByteArray());
      if (accountCapsule == null) {
        throw new ContractValidateException("Account not exists");
      }

      if (this.dbManager.getAssetIssueStore().get(assetIssueContract.getName().toByteArray())
          != null) {
        throw new ContractValidateException("Token exists");
      }

      if (accountCapsule.getBalance() < calcFee()) {
        throw new ContractValidateException("No enough blance for fee!");
      }
    } catch (InvalidProtocolBufferException e) {
      throw new ContractValidateException(e.getMessage());
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(AssetIssueContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return ChainConstant.ASSET_ISSUE_FEE;
  }

  public long calcUsage() {
    return 0;
  }
}
