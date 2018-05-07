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
import org.joda.time.DateTime;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;


@Slf4j
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
      long balance = Math.subtractExact(ownerAccount.getBalance(), cost);
      balance = Math.subtractExact(balance, fee);
      ownerAccount.setBalance(balance);

      //calculate the exchange amount
      AssetIssueCapsule assetIssueCapsule =
          this.dbManager.getAssetIssueStore()
              .get(participateAssetIssueContract.getAssetName().toByteArray());
      long exchangeAmount = Math.multiplyExact(cost, assetIssueCapsule.getNum());
      exchangeAmount = Math.floorDiv(exchangeAmount, assetIssueCapsule.getTrxNum());
      ownerAccount.addAssetAmount(assetIssueCapsule.getName(), exchangeAmount);

      //add to to_address
      byte[] toAddressBytes = participateAssetIssueContract.getToAddress().toByteArray();
      AccountCapsule toAccount = this.dbManager.getAccountStore().get(toAddressBytes);
      toAccount.setBalance(Math.addExact(toAccount.getBalance(), cost));
      if (!toAccount.reduceAssetAmount(assetIssueCapsule.getName(), exchangeAmount)) {
        throw new ContractExeException("reduceAssetAmount failed !");
      }

      //write to db
      dbManager.getAccountStore().put(ownerAddressBytes, ownerAccount);
      dbManager.getAccountStore().put(toAddressBytes, toAccount);

      ret.setStatus(fee, Protocol.Transaction.Result.code.SUCESS);

      return true;
    } catch (InvalidProtocolBufferException e) {
      ret.setStatus(fee, Protocol.Transaction.Result.code.FAILED);
      logger.debug(e.getMessage(), e);
      throw new ContractExeException(e.getMessage());
    } catch (ArithmeticException e) {
      ret.setStatus(fee, Protocol.Transaction.Result.code.FAILED);
      logger.debug(e.getMessage(), e);
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
      //Parameters check
      if (!Wallet.addressValid(participateAssetIssueContract.getOwnerAddress().toByteArray())) {
        throw new ContractValidateException("Invalidate ownerAddress");
      }
      if (!Wallet.addressValid(participateAssetIssueContract.getToAddress().toByteArray())) {
        throw new ContractValidateException("Invalidate toAddress");
      }
      Preconditions
          .checkNotNull(participateAssetIssueContract.getAssetName(), "Asset name is null");
      if (participateAssetIssueContract.getAmount() <= 0) {
        throw new ContractValidateException("Amount must greater than 0!");
      }

      if (participateAssetIssueContract.getOwnerAddress()
          .equals(participateAssetIssueContract.getToAddress())) {
        throw new ContractValidateException("Cannot participate asset Issue yourself !");
      }

      byte[] addressBytes = participateAssetIssueContract.getOwnerAddress().toByteArray();
      //Whether the account exist
      AccountCapsule ac = this.dbManager.getAccountStore().get(addressBytes);
      if (ac == null) {
        throw new ContractValidateException("Account does not exist!");
      }

      long fee = calcFee();
      //Whether the balance is enough
      if (ac.getBalance() < Math.addExact(participateAssetIssueContract.getAmount(), fee)) {
        throw new ContractValidateException("No enough balance !");
      }

      //Whether have the mapping
      if (!this.dbManager.getAssetIssueStore()
          .has(participateAssetIssueContract.getAssetName().toByteArray())) {
        throw new ContractValidateException("No asset named " + ByteArray
            .toStr(participateAssetIssueContract.getAssetName().toByteArray()));
      }
      AssetIssueCapsule assetIssueCapsule =
          this.dbManager.getAssetIssueStore()
              .get(participateAssetIssueContract.getAssetName().toByteArray());
      if (!participateAssetIssueContract.getToAddress()
          .equals(assetIssueCapsule.getOwnerAddress())) {
        throw new ContractValidateException("The asset is not issued by " + ByteArray
            .toHexString(participateAssetIssueContract.getToAddress().toByteArray()));
      }
      //Whether the exchange can be processed: to see if the exchange can be the exact int
      long cost = participateAssetIssueContract.getAmount();

      DateTime now = DateTime.now();
      if (now.getMillis() >= assetIssueCapsule.getEndTime() || now.getMillis() < assetIssueCapsule
          .getStartTime()) {
        throw new ContractValidateException("No longer valid period!");
      }
      int trxNum = assetIssueCapsule.getTrxNum();
      int num = assetIssueCapsule.getNum();
      long exchangeAmount = Math.multiplyExact(cost, num);
      exchangeAmount = Math.floorDiv(exchangeAmount, trxNum);
      if (exchangeAmount <= 0) {
        throw new ContractValidateException("Can not process the exchange!");
      }
      AccountCapsule toAccount = this.dbManager.getAccountStore()
          .get(participateAssetIssueContract.getToAddress().toByteArray());
      if (toAccount == null) {
        throw new ContractValidateException("To account does not exist!");
      }
      if (!toAccount.assetBalanceEnough(assetIssueCapsule.getName(), exchangeAmount)) {
        throw new ContractValidateException("Asset balance is not enough !");
      }

    } catch (InvalidProtocolBufferException e) {
      throw new ContractValidateException(e.getMessage());
    } catch (ArithmeticException e) {
      throw new ContractValidateException(e.getMessage());
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
