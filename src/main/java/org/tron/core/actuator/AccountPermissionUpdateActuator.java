package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.AccountStore;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.AccountPermissionUpdateContract;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Transaction.Result.code;


@Slf4j
public class AccountPermissionUpdateActuator extends AbstractActuator {

  AccountPermissionUpdateActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule result) throws ContractExeException {
    long fee = calcFee();
    final AccountPermissionUpdateContract accountPermissionUpdateContract;
    try {
      accountPermissionUpdateContract = contract.unpack(AccountPermissionUpdateContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      result.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    byte[] ownerAddress = accountPermissionUpdateContract.getOwnerAddress().toByteArray();
    AccountStore accountStore = dbManager.getAccountStore();
    AccountCapsule account = accountStore.get(ownerAddress);
    account.updatePermissions(accountPermissionUpdateContract.getPermissionsList());
    accountStore.put(ownerAddress, account);
    result.setStatus(fee, code.SUCESS);
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.contract == null) {
      throw new ContractValidateException("No contract!");
    }
    if (this.dbManager == null) {
      throw new ContractValidateException("No dbManager!");
    }
    if (!this.contract.is(AccountPermissionUpdateContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [AccountPermissionUpdateContract],real type["
              + contract
              .getClass() + "]");
    }
    final AccountPermissionUpdateContract accountPermissionUpdateContract;
    try {
      accountPermissionUpdateContract = contract.unpack(AccountPermissionUpdateContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = accountPermissionUpdateContract.getOwnerAddress().toByteArray();
    if (!Wallet.addressValid(ownerAddress)) {
      throw new ContractValidateException("invalidate ownerAddress");
    }
    if (accountPermissionUpdateContract.getPermissionsCount() == 0) {
      throw new ContractValidateException("permission's count should be greater than 0");
    }
    for (Permission permission : accountPermissionUpdateContract.getPermissionsList()) {
      if (permission.getKeysCount() == 0) {
        throw new ContractValidateException("key's count should be greater than 0");
      }
      if (permission.getThreshold() <= 0) {
        throw new ContractValidateException("permission's threshold should be greater than 0");
      }
      if (StringUtils.isEmpty(permission.getName())) {
        throw new ContractValidateException("permission's name should not be empty");
      }
      String name = permission.getName();
      if (!name.equalsIgnoreCase("owner") && !name.equalsIgnoreCase("active")) {
        throw new ContractValidateException("permission's name should be owner or active");
      }
      String parent = permission.getParent().toStringUtf8();
      if (!parent.isEmpty() && !parent.equalsIgnoreCase("owner")) {
        throw new ContractValidateException("permission's parent should be owner");
      }
      long weightSum = 0;
      for (Key key : permission.getKeysList()) {
        if (!Wallet.addressValid(key.getAddress().toByteArray())) {
          throw new ContractValidateException("key is not a validate address");
        }
        if (key.getWeight() <= 0) {
          throw new ContractValidateException("key's weight should be greater than 0");
        }
        weightSum += key.getWeight();
      }
      if (weightSum < permission.getThreshold()) {
        throw new ContractValidateException(
            "sum of all key's weight should not be less than threshold");
      }
    }
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(AccountPermissionUpdateContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }
}
