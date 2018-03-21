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
import org.joda.time.DateTime;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;

public class ParticipateAssetIssueActuator extends AbstractActuator {

  ParticipateAssetIssueActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    long fee = calcFee();

    try {
      Contract.ParticipateAssetIssueContract participateAssetIssueContract =
          contract.unpack(Contract.ParticipateAssetIssueContract.class);

      long cost = participateAssetIssueContract.getAmount();

      //subtract from owner address
      byte[] ownerAddressBytes = participateAssetIssueContract.getOwnerAddress().toByteArray();
      AccountCapsule ownerAccount = this.dbManager.getAccountStore().get(ownerAddressBytes);
      ownerAccount.setBalance(ownerAccount.getBalance() - cost);

      //calculate the exchange amount
      AssetIssueCapsule assetIssueCapsule =
          this.dbManager.getAssetIssueStore()
              .get(participateAssetIssueContract.getAssetName().toByteArray());
      long exchangeAmount = cost / assetIssueCapsule.getTrxNum() * assetIssueCapsule.getNum();
      ownerAccount.addAssetAmount(assetIssueCapsule.getName(), exchangeAmount);
      //add to to_address
      byte[] toAddressBytes = participateAssetIssueContract.getToAddress().toByteArray();
      AccountCapsule toAccount = this.dbManager.getAccountStore().get(toAddressBytes);
      toAccount.setBalance(toAccount.getBalance() + cost);
      toAccount.reduceAssetAmount(assetIssueCapsule.getName(), exchangeAmount);
      //write to db
      dbManager.getAccountStore().put(ownerAddressBytes, ownerAccount);
      dbManager.getAccountStore().put(toAddressBytes, toAccount);

      ret.setStatus(fee, Protocol.Transaction.Result.code.SUCESS);

      return true;
    } catch (InvalidProtocolBufferException e) {
      ret.setStatus(fee, Protocol.Transaction.Result.code.FAILED);
      e.printStackTrace();
      throw new ContractExeException(e.getMessage());
    }
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (!this.contract.is(Contract.ParticipateAssetIssueContract.class)) {
      throw new ContractValidateException();
    }

    try {
      final Contract.ParticipateAssetIssueContract participateAssetIssueContract =
          this.contract.unpack(Contract.ParticipateAssetIssueContract.class);

      Preconditions
          .checkNotNull(participateAssetIssueContract.getOwnerAddress(), "OwnerAddress is null");
      Preconditions.checkNotNull(participateAssetIssueContract.getToAddress(), "ToAddress is null");
      Preconditions.checkNotNull(participateAssetIssueContract.getAssetName(), "trx name is null");
      if (participateAssetIssueContract.getAmount() < 0) {
        throw new ContractValidateException("Trx Num can not be negative!");
      }

      byte[] addressBytes = participateAssetIssueContract.getOwnerAddress().toByteArray();
      //Whether the account exist
      if (!this.dbManager.getAccountStore().has(addressBytes)) {
        throw new ContractValidateException("Account does not exist!");
      }

      AccountCapsule ac = this.dbManager.getAccountStore().get(addressBytes);
      //Whether the balance is enough
      if (ac.getBalance() < participateAssetIssueContract.getAmount()) {
        throw new ContractValidateException();
      }

      //Whether have the mapping
      if (!this.dbManager.getAssetIssueStore()
          .has(participateAssetIssueContract.getAssetName().toByteArray())) {
        throw new ContractValidateException();
      }

      //Whether the exchange can be processed: to see if the exchange can be the exact int
      long cost = participateAssetIssueContract.getAmount();
      AssetIssueCapsule assetIssueCapsule =
          this.dbManager.getAssetIssueStore()
              .get(participateAssetIssueContract.getAssetName().toByteArray());
      DateTime now = DateTime.now();
      if (now.getMillis() >= assetIssueCapsule.getEndTime() || now.getMillis() < assetIssueCapsule
          .getStartTime()) {
        throw new ContractValidateException("No longer valid period!");
      }
      int trxNum = assetIssueCapsule.getTrxNum();
      int num = assetIssueCapsule.getNum();
      long exchangeAmount = cost / trxNum * num;
      float preciseExchangeAmount = (float) cost / (float) trxNum * (float) num;
      if (preciseExchangeAmount - exchangeAmount >= 0.000001f) {
        throw new ContractValidateException("Can not process the exchange!");
      }
    } catch (InvalidProtocolBufferException e) {
      throw new ContractValidateException();
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return null;
  }

  @Override
  public long calcFee() {
    return 0;
  }
}
