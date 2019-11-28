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

package org.tron.core.utils;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.contract.ShieldContract.ShieldedTransferContract;
import org.tron.protos.contract.ShieldContract.SpendDescription;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;

@Slf4j(topic = "capsule")
public class TransactionUtil {

  public static boolean validAccountName(byte[] accountName) {
    if (ArrayUtils.isEmpty(accountName)) {
      return true;   //account name can be empty
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

  /**
   * Get sender.
   */
 /* public static byte[] getSender(Transaction tx) {
    byte[] pubKey = tx.getRawData().getVin(0).getRawData().getPubKey().toByteArray();
    return ECKey.computeAddress(pubKey);
  } */

  //make sure that contractType is validated before
  //No exception will be thrown here
  public static byte[] getShieldTransactionHashIgnoreTypeException(Transaction tx) {
    try {
      return hashShieldTransaction(tx);
    } catch (ContractValidateException | InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
    return null;
  }

  public static byte[] hashShieldTransaction(Transaction tx)
      throws ContractValidateException, InvalidProtocolBufferException {
    Any contractParameter = tx.getRawData().getContract(0).getParameter();
    if (!contractParameter.is(ShieldedTransferContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [ShieldedTransferContract],real type["
              + contractParameter
              .getClass() + "]");
    }

    ShieldedTransferContract shieldedTransferContract = contractParameter
        .unpack(ShieldedTransferContract.class);
    ShieldedTransferContract.Builder newContract = ShieldedTransferContract.newBuilder();
    newContract.setFromAmount(shieldedTransferContract.getFromAmount());
    newContract.addAllReceiveDescription(shieldedTransferContract.getReceiveDescriptionList());
    newContract.setToAmount(shieldedTransferContract.getToAmount());
    newContract.setTransparentFromAddress(shieldedTransferContract.getTransparentFromAddress());
    newContract.setTransparentToAddress(shieldedTransferContract.getTransparentToAddress());
    for (SpendDescription spendDescription : shieldedTransferContract.getSpendDescriptionList()) {
      newContract
          .addSpendDescription(spendDescription.toBuilder().clearSpendAuthoritySignature().build());
    }

    Transaction.raw.Builder rawBuilder = tx.toBuilder()
        .getRawDataBuilder()
        .clearContract()
        .addContract(
            Transaction.Contract.newBuilder().setType(ContractType.ShieldedTransferContract)
                .setParameter(
                    Any.pack(newContract.build())).build());

    Transaction transaction = tx.toBuilder().clearRawData()
        .setRawData(rawBuilder).build();

    return Sha256Hash.of(transaction.getRawData().toByteArray())
        .getBytes();
  }

  public static Sha256Hash getTransactionId(Transaction transaction) {
    return Sha256Hash.of(transaction.getRawData().toByteArray());
  }


  public static contractResult getContractRet(Transaction transaction) {
    if (transaction.getRetCount() <= 0) {
      return null;
    }
    return transaction.getRet(0).getContractRet();
  }


  public static long getCallValue(Transaction.Contract contract) {
    try {
      Any contractParameter = contract.getParameter();
      switch (contract.getType()) {
        case TriggerSmartContract:
          return contractParameter.unpack(TriggerSmartContract.class).getCallValue();

        case CreateSmartContract:
          return contractParameter.unpack(CreateSmartContract.class).getNewContract()
              .getCallValue();
        default:
          return 0L;
      }
    } catch (Exception ex) {
      logger.error(ex.getMessage());
      return 0L;
    }
  }

  public static long getCallTokenValue(Transaction.Contract contract) {
    try {
      Any contractParameter = contract.getParameter();
      switch (contract.getType()) {
        case TriggerSmartContract:
          return contractParameter.unpack(TriggerSmartContract.class).getCallTokenValue();

        case CreateSmartContract:
          return contractParameter.unpack(CreateSmartContract.class).getCallTokenValue();
        default:
          return 0L;
      }
    } catch (Exception ex) {
      logger.error(ex.getMessage());
      return 0L;
    }
  }


}
