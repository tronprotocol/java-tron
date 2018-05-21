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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
import org.tron.protos.Contract.AssetIssueContract.FrozenSupply;
import org.tron.protos.Protocol.Account.Frozen;
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
      AssetIssueContract assetIssueContract = contract.unpack(AssetIssueContract.class);
      byte[] ownerAddress = assetIssueContract.getOwnerAddress().toByteArray();
      AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
      dbManager.getAssetIssueStore()
          .put(assetIssueCapsule.getName().toByteArray(), assetIssueCapsule);

      dbManager.adjustBalance(ownerAddress, -calcFee());
      dbManager.adjustBalance(dbManager.getAccountStore().getBlackhole().getAddress().toByteArray(),
          calcFee());//send to blackhole

      AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      List<FrozenSupply> frozenSupplyList = assetIssueContract.getFrozenSupplyList();
      Iterator<FrozenSupply> iterator = frozenSupplyList.iterator();
      long remainSupply = assetIssueContract.getTotalSupply();
      List<Frozen> frozenList = new ArrayList<>();
      long startTime = assetIssueContract.getStartTime();

      while (iterator.hasNext()) {
        FrozenSupply next = iterator.next();
        long expireTime = startTime + next.getFrozenDays() * 86_400_000;
        Frozen newFrozen = Frozen.newBuilder()
            .setFrozenBalance(next.getFrozenAmount())
            .setExpireTime(expireTime)
            .build();
        frozenList.add(newFrozen);
        remainSupply -= next.getFrozenAmount();
      }

      assert remainSupply > 0;
      accountCapsule.setAssetIssuedName(assetIssueContract.getName());
      accountCapsule.addAsset(ByteArray.toStr(assetIssueContract.getName().toByteArray()),
          remainSupply);
      accountCapsule.setInstance(accountCapsule.getInstance().toBuilder()
          .addAllFrozenSupply(frozenList).build());

      dbManager.getAccountStore().put(ownerAddress, accountCapsule);
      ret.setStatus(fee, code.SUCESS);
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

    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    try {
      if (!this.contract.is(AssetIssueContract.class)) {
        throw new ContractValidateException();
      }
      if (this.dbManager == null) {
        throw new ContractValidateException();
      }
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

      if (assetIssueContract.getStartTime() == 0) {
        throw new ContractValidateException("start time should be not empty");
      }
      if (assetIssueContract.getEndTime() == 0) {
        throw new ContractValidateException("end time should be not empty");
      }
      if (assetIssueContract.getEndTime() <= assetIssueContract.getStartTime()) {
        throw new ContractValidateException("end time should be greater than start time");
      }
      if (assetIssueContract.getStartTime() <= dbManager.getHeadBlockTimeStamp()){
        throw new ContractValidateException("start time should be greater than HeadBlockTime");
      }

      if (this.dbManager.getAssetIssueStore().get(assetIssueContract.getName().toByteArray())
          != null) {
        throw new ContractValidateException("Token exists");
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

      if (assetIssueContract.getFrozenSupplyCount()
          > this.dbManager.getDynamicPropertiesStore().getMaxFrozenSupplyNumber()) {
        throw new ContractValidateException("Frozen supply list length is too long");
      }

      long remainSupply = assetIssueContract.getTotalSupply();
      long minFrozenSupplyTime = dbManager.getDynamicPropertiesStore().getMinFrozenSupplyTime();
      long maxFrozenSupplyTime = dbManager.getDynamicPropertiesStore().getMaxFrozenSupplyTime();
      List<FrozenSupply> frozenList = assetIssueContract.getFrozenSupplyList();
      Iterator<FrozenSupply> iterator = frozenList.iterator();

      while (iterator.hasNext()) {
        FrozenSupply next = iterator.next();
        if (next.getFrozenAmount() <= 0) {
          throw new ContractValidateException("Frozen supply must be greater than 0!");
        }
        if (next.getFrozenAmount() > remainSupply) {
          throw new ContractValidateException("Frozen supply cannot exceed total supply");
        }
        if (!(next.getFrozenDays() >= minFrozenSupplyTime
            && next.getFrozenDays() <= maxFrozenSupplyTime)) {
          throw new ContractValidateException(
              "frozenDuration must be less than " + maxFrozenSupplyTime + " days "
                  + "and more than " + minFrozenSupplyTime + " days");
        }
        remainSupply -= next.getFrozenAmount();
      }

      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(assetIssueContract.getOwnerAddress().toByteArray());
      if (accountCapsule == null) {
        throw new ContractValidateException("Account not exists");
      }

      if (!accountCapsule.getAssetIssuedName().isEmpty()) {
        throw new ContractValidateException("An account can only issue one asset");
      }

      if (accountCapsule.getBalance() < calcFee()) {
        throw new ContractValidateException("No enough balance for fee!");
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
