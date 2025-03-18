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

import static org.tron.common.crypto.Hash.sha3omit12;
import static org.tron.common.math.Maths.max;
import static org.tron.core.config.Parameter.ChainConstant.DELEGATE_COST_BASE_SIZE;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

import com.google.common.base.CaseFormat;
import com.google.common.primitives.Longs;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.GrpcAPI.TransactionSignWeight;
import org.tron.api.GrpcAPI.TransactionSignWeight.Result;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.PermissionException;
import org.tron.core.exception.SignatureFormatException;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Permission.PermissionType;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import org.tron.protos.contract.BalanceContract.DelegateResourceContract;

@Slf4j(topic = "capsule")
@Component
public class TransactionUtil {

  private static final int MAX_ACCOUNT_NAME_LEN = 200;
  private static final int MAX_ACCOUNT_ID_LEN = 32;
  private static final int MIN_ACCOUNT_ID_LEN = 8;
  private static final int MAX_ASSET_NAME_LEN = 32;
  private static final int MAX_TOKEN_ABBR_NAME_LEN = 5;
  private static final int MAX_ASSET_DESCRIPTION_LEN = 200;
  private static final int MAX_URL_LEN = 256;

  @Autowired
  private ChainBaseManager chainBaseManager;

  public static boolean validAccountName(byte[] accountName) {
    return validBytes(accountName, MAX_ACCOUNT_NAME_LEN, true);
  }

  public static boolean validAssetDescription(byte[] description) {
    return validBytes(description, MAX_ASSET_DESCRIPTION_LEN, true);
  }

  public static boolean validUrl(byte[] url) {
    return validBytes(url, MAX_URL_LEN, false);
  }

  public static boolean validAccountId(byte[] accountId) {
    return validReadableBytes(accountId, MAX_ACCOUNT_ID_LEN) && accountId.length >= MIN_ACCOUNT_ID_LEN;
  }

  public static boolean validAssetName(byte[] assetName) {
    return validReadableBytes(assetName, MAX_ASSET_NAME_LEN);
  }

  public static boolean validTokenAbbrName(byte[] abbrName) {
    return validReadableBytes(abbrName, MAX_TOKEN_ABBR_NAME_LEN);
  }

  private static boolean validBytes(byte[] bytes, int maxLength, boolean allowEmpty) {
    if (ArrayUtils.isEmpty(bytes)) {
      return allowEmpty;
    }
    return bytes.length <= maxLength;
  }

  private static boolean validReadableBytes(byte[] bytes, int maxLength) {
    if (ArrayUtils.isEmpty(bytes) || bytes.length > maxLength) {
      return false;
    }
    // b must be readable
    for (byte b : bytes) {
      if (b < 0x21) {
        return false; // 0x21 = '!'
      }
      if (b > 0x7E) {
        return false; // 0x7E = '~'
      }
    }
    return true;
  }

  public static Sha256Hash getTransactionId(Transaction transaction) {
    return Sha256Hash.of(CommonParameter.getInstance().isECKeyCryptoEngine(),
        transaction.getRawData().toByteArray());
  }


  public static contractResult getContractRet(Transaction transaction) {
    if (transaction.getRetCount() <= 0) {
      return null;
    }
    return transaction.getRet(0).getContractRet();
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

  public static byte[] generateContractAddress(byte[] ownerAddress, byte[] txRawDataHash) {

    byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
    System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
    System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

    return sha3omit12(combined);

  }

  // for `CREATE`
  public static byte[] generateContractAddress(byte[] transactionRootId, long nonce) {
    byte[] nonceBytes = Longs.toByteArray(nonce);
    byte[] combined = new byte[transactionRootId.length + nonceBytes.length];
    System.arraycopy(transactionRootId, 0, combined, 0, transactionRootId.length);
    System.arraycopy(nonceBytes, 0, combined, transactionRootId.length, nonceBytes.length);

    return sha3omit12(combined);
  }

  public static boolean checkPermissionOperations(Permission permission, Contract contract)
      throws PermissionException {
    ByteString operations = permission.getOperations();
    if (operations.size() != 32) {
      throw new PermissionException("operations size must be 32");
    }
    int contractType = contract.getTypeValue();
    boolean b = (operations.byteAt(contractType / 8) & (1 << (contractType % 8))) != 0;
    return b;
  }

  public static String makeUpperCamelMethod(String originName) {
    return "get" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, originName)
        .replace("_", "");
  }

  public TransactionSignWeight getTransactionSignWeight(Transaction trx) {
    TransactionSignWeight.Builder tswBuilder = TransactionSignWeight.newBuilder();
    TransactionExtention.Builder trxExBuilder = TransactionExtention.newBuilder();
    trxExBuilder.setTransaction(trx);
    trxExBuilder.setTxid(ByteString.copyFrom(Sha256Hash.hash(CommonParameter
        .getInstance().isECKeyCryptoEngine(), trx.getRawData().toByteArray())));
    Return.Builder retBuilder = Return.newBuilder();
    retBuilder.setResult(true).setCode(response_code.SUCCESS);
    trxExBuilder.setResult(retBuilder);
    tswBuilder.setTransaction(trxExBuilder);
    Result.Builder resultBuilder = Result.newBuilder();

    if (trx.getRawData().getContractCount() == 0) {
      resultBuilder.setCode(Result.response_code.OTHER_ERROR);
      resultBuilder.setMessage("Invalid transaction: no valid contract");
    } else {
      try {
        Contract contract = trx.getRawData().getContract(0);
        byte[] owner = TransactionCapsule.getOwner(contract);
        AccountCapsule account = chainBaseManager.getAccountStore().get(owner);
        if (Objects.isNull(account)) {
          throw new PermissionException("Account does not exist!");
        }
        int permissionId = contract.getPermissionId();
        Permission permission = account.getPermissionById(permissionId);
        if (permission == null) {
          throw new PermissionException("Permission for this, does not exist!");
        }
        if (permissionId != 0) {
          if (permission.getType() != PermissionType.Active) {
            throw new PermissionException("Permission type is wrong!");
          }
          //check operations
          if (!checkPermissionOperations(permission, contract)) {
            throw new PermissionException("Permission denied!");
          }
        }
        tswBuilder.setPermission(permission);
        if (trx.getSignatureCount() > 0) {
          List<ByteString> approveList = new ArrayList<>();
          long currentWeight = TransactionCapsule.checkWeight(permission, trx.getSignatureList(),
              Sha256Hash.hash(CommonParameter.getInstance()
                  .isECKeyCryptoEngine(), trx.getRawData().toByteArray()), approveList);
          tswBuilder.addAllApprovedList(approveList);
          tswBuilder.setCurrentWeight(currentWeight);
        }
        if (tswBuilder.getCurrentWeight() >= permission.getThreshold()) {
          resultBuilder.setCode(Result.response_code.ENOUGH_PERMISSION);
        } else {
          resultBuilder.setCode(Result.response_code.NOT_ENOUGH_PERMISSION);
        }
      } catch (SignatureFormatException signEx) {
        resultBuilder.setCode(Result.response_code.SIGNATURE_FORMAT_ERROR);
        resultBuilder.setMessage(signEx.getMessage());
      } catch (SignatureException signEx) {
        resultBuilder.setCode(Result.response_code.COMPUTE_ADDRESS_ERROR);
        resultBuilder.setMessage(signEx.getMessage());
      } catch (PermissionException permEx) {
        resultBuilder.setCode(Result.response_code.PERMISSION_ERROR);
        resultBuilder.setMessage(permEx.getMessage());
      } catch (Exception ex) {
        resultBuilder.setCode(Result.response_code.OTHER_ERROR);
        resultBuilder.setMessage(ex.getClass() + " : " + ex.getMessage());
      }
    }

    tswBuilder.setResult(resultBuilder);
    return tswBuilder.build();
  }

  public static long estimateConsumeBandWidthSize(DynamicPropertiesStore dps, long balance) {
    DelegateResourceContract.Builder builder;
    if (dps.supportMaxDelegateLockPeriod()) {
      builder = DelegateResourceContract.newBuilder()
              .setLock(true)
              .setLockPeriod(dps.getMaxDelegateLockPeriod())
              .setBalance(balance);
    } else {
      builder = DelegateResourceContract.newBuilder()
              .setLock(true)
              .setBalance(balance);
    }
    long builderSize = builder.build().getSerializedSize();
    DelegateResourceContract.Builder builder2 = DelegateResourceContract.newBuilder()
        .setBalance(TRX_PRECISION);
    long builder2Size = builder2.build().getSerializedSize();
    long addSize = max(builderSize - builder2Size, 0L, dps.disableJavaLangMath());

    return DELEGATE_COST_BASE_SIZE + addSize;
  }
}
