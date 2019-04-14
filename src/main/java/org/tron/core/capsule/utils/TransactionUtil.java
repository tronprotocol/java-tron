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

package org.tron.core.capsule.utils;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.AccountCreateContract;
import org.tron.protos.Contract.AccountUpdateContract;
import org.tron.protos.Contract.SetAccountIdContract;
import org.tron.protos.Contract.TransferAssetContract;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Contract.UnfreezeAssetContract;
import org.tron.protos.Contract.UpdateAssetContract;
import org.tron.protos.Contract.UpdateEnergyLimitContract;
import org.tron.protos.Contract.UpdateSettingContract;
import org.tron.protos.Protocol.DeferredStage;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;

@Slf4j(topic = "capsule")
public class TransactionUtil {

  public static Transaction newGenesisTransaction(byte[] key, long value)
      throws IllegalArgumentException {

    if (!Wallet.addressValid(key)) {
      throw new IllegalArgumentException("Invalid address");
    }
    TransferContract transferContract = TransferContract.newBuilder()
        .setAmount(value)
        .setOwnerAddress(ByteString.copyFrom("0x000000000000000000000".getBytes()))
        .setToAddress(ByteString.copyFrom(key))
        .build();

    return new TransactionCapsule(transferContract,
        Contract.ContractType.TransferContract).getInstance();
  }

  public static boolean validAccountName(byte[] accountName) {
    if (ArrayUtils.isEmpty(accountName)) {
      return true;   //accountname can empty
    }

    return accountName.length <= 200;
  }

  public static boolean validAccountId(byte[] accountId) {
    if (ArrayUtils.isEmpty(accountId)) {
      return false;
    }

    if (accountId.length < 8) {
      return false;
    }

    if (accountId.length > 32) {
      return false;
    }
    // b must read able.
    for (byte b : accountId) {
      if (b < 0x21) {
        return false; // 0x21 = '!'
      }
      if (b > 0x7E) {
        return false; // 0x7E = '~'
      }
    }
    return true;
  }

  public static boolean validAssetName(byte[] assetName) {
    if (ArrayUtils.isEmpty(assetName)) {
      return false;
    }
    if (assetName.length > 32) {
      return false;
    }
    // b must read able.
    for (byte b : assetName) {
      if (b < 0x21) {
        return false; // 0x21 = '!'
      }
      if (b > 0x7E) {
        return false; // 0x7E = '~'
      }
    }
    return true;
  }

  public static boolean validTokenAbbrName(byte[] abbrName) {
    if (ArrayUtils.isEmpty(abbrName)) {
      return false;
    }
    if (abbrName.length > 5) {
      return false;
    }
    // b must read able.
    for (byte b : abbrName) {
      if (b < 0x21) {
        return false; // 0x21 = '!'
      }
      if (b > 0x7E) {
        return false; // 0x7E = '~'
      }
    }
    return true;
  }


  public static boolean validAssetDescription(byte[] description) {
    if (ArrayUtils.isEmpty(description)) {
      return true;   //description can empty
    }

    return description.length <= 200;
  }

  public static boolean validUrl(byte[] url) {
    if (ArrayUtils.isEmpty(url)) {
      return false;
    }
    return url.length <= 256;
  }

  public static boolean isNumber(byte[] id) {
    if (ArrayUtils.isEmpty(id)) {
      return false;
    }
    for (byte b : id) {
      if (b < '0' || b > '9') {
        return false;
      }
    }

    return !(id.length > 1 && id[0] == '0');
  }

  public static Transaction setTransactionDelaySeconds(Transaction transaction, long delaySeconds) {
    if (delaySeconds <= 0) return transaction;
    DeferredStage deferredStage = transaction.getRawData().toBuilder().
        getDeferredStage().toBuilder().setDelaySeconds(delaySeconds)
        .setStage(Constant.UNEXECUTEDDEFERREDTRANSACTION).build();
    Transaction.raw rawData = transaction.toBuilder().getRawData().toBuilder()
        .setDeferredStage(deferredStage).build();
    return transaction.toBuilder().setRawData(rawData).build();
  }

  public static boolean validateDeferredTransaction(TransactionCapsule transactionCapsule) {
    if (transactionCapsule.getDeferredSeconds() > Constant.MAX_DEFERRED_TRANSACTION_DELAY_SECONDS
        || transactionCapsule.getDeferredSeconds() < 0) {
      logger.warn("deferred transaction delay seconds is illegal");
      return false;
    }
    boolean result = true;
    if (transactionCapsule.getDeferredStage() != Constant.EXECUTINGDEFERREDTRANSACTION
        && transactionCapsule.getDeferredStage() != Constant.UNEXECUTEDDEFERREDTRANSACTION) {
      result = false;
    }
    return result;
  }


  public static long getDelaySeconds(TransactionCapsule transactionCapsule) {
    if (Objects.isNull(transactionCapsule)|| Objects.isNull(transactionCapsule.getInstance())
        || Objects.isNull(transactionCapsule.getInstance().getRawData())
        || Objects.isNull(transactionCapsule.getInstance().getRawData().getContractList())
        || transactionCapsule.getInstance().getRawData().getContractList().size() < 1) {
      return 0;
    }
    Transaction.Contract contract = transactionCapsule.getInstance().getRawData().getContract(0);
    long delaySeconds = 0;
    try {
      Any contractParameter = contract.getParameter();
      switch (contract.getType()) {
        case AccountUpdateContract:
          delaySeconds = contractParameter.unpack(AccountUpdateContract.class).getDelaySeconds();
          break;
        case TransferContract:
          delaySeconds = contractParameter.unpack(TransferContract.class).getDelaySeconds();
          break;
        case TransferAssetContract:
          delaySeconds = contractParameter.unpack(TransferAssetContract.class).getDelaySeconds();
          break;
        case AccountCreateContract:
          delaySeconds = contractParameter.unpack(AccountCreateContract.class).getDelaySeconds();
          break;
        case UnfreezeAssetContract:
          delaySeconds = contractParameter.unpack(UnfreezeAssetContract.class).getDelaySeconds();
          break;
        case UpdateAssetContract:
          delaySeconds = contractParameter.unpack(UpdateAssetContract.class).getDelaySeconds();
          break;
        case SetAccountIdContract:
          delaySeconds = contractParameter.unpack(SetAccountIdContract.class).getDelaySeconds();
          break;
        case UpdateSettingContract:
          delaySeconds = contractParameter.unpack(UpdateSettingContract.class).getDelaySeconds();
          break;
        case UpdateEnergyLimitContract:
          delaySeconds = contractParameter.unpack(UpdateEnergyLimitContract.class).getDelaySeconds();
          break;
        default:
          return 0;
      }
      return delaySeconds;
    } catch (Exception ex) {
      logger.info("get deferred transaction delay second failed");
      return 0;
    }
  }


  public static void validateDeferredTransactionFee(TransactionCapsule trx, long delaySecond,  Manager dbManager) throws ContractValidateException {
    if (Objects.isNull(trx)
        ||Objects.isNull(trx.getInstance())
        || Objects.isNull(trx.getInstance().getRawData())
        || trx.getInstance().getRawData().getContractList().size() < 1) {
      throw new ContractValidateException("validate contract failed");
    }
    byte[] ownerAddress = TransactionCapsule.getOwner(trx.getInstance().getRawData().getContract(0));
    if (!Wallet.addressValid(ownerAddress)){
      logger.error("empty owner address");
      throw new ContractValidateException("empty owner address");
    }
    AccountCapsule ownerAccount = dbManager.getAccountStore().getUnchecked(ownerAddress);
    if (ownerAccount == null) {
      throw new ContractValidateException("validate contract error, no OwnerAccount.");
    }

    long fee = dbManager.getDynamicPropertiesStore().getDeferredTransactionFee() * (delaySecond  / (24 * 60 * 60) + 1);
    if (ownerAccount.getBalance() < fee ) {
      logger.error("no enough money for deferred transaction");
      throw new ContractValidateException(
          "create deferred transaction error, insufficient fee.");
    }
  }

  /**
   * Get sender.
   */
 /* public static byte[] getSender(Transaction tx) {
    byte[] pubKey = tx.getRawData().getVin(0).getRawData().getPubKey().toByteArray();
    return ECKey.computeAddress(pubKey);
  } */

}
