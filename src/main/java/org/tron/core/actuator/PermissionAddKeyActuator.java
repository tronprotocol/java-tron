package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.AccountStore;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.PermissionAddKeyContract;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Transaction.Result.code;


@Slf4j
public class PermissionAddKeyActuator extends AbstractActuator {

  PermissionAddKeyActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule result) throws ContractExeException {
    long fee = calcFee();
    final PermissionAddKeyContract permissionAddKeyContract;
    try {
      permissionAddKeyContract = contract.unpack(PermissionAddKeyContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      result.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    byte[] ownerAddress = permissionAddKeyContract.getOwnerAddress().toByteArray();
    AccountStore accountStore = dbManager.getAccountStore();
    AccountCapsule account = accountStore.get(ownerAddress);
    account.permissionAddKey(permissionAddKeyContract.getKey(),
        permissionAddKeyContract.getPermissionName());
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
    if (!this.contract.is(PermissionAddKeyContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [PermissionAddKeyContract],real type[" + contract
              .getClass() + "]");
    }
    final PermissionAddKeyContract permissionAddKeyContract;
    try {
      permissionAddKeyContract = contract.unpack(PermissionAddKeyContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = permissionAddKeyContract.getOwnerAddress().toByteArray();
    AccountStore accountStore = dbManager.getAccountStore();
    AccountCapsule account = accountStore.get(ownerAddress);
    String name = permissionAddKeyContract.getPermissionName();
    if (!name.equalsIgnoreCase("owner") &&
        !name.equalsIgnoreCase("active")) {
      throw new ContractValidateException("permission name should be owner or active");
    }
    Permission permission = account.getPermissionByName(name);
    if (permission == null) {
      throw new ContractValidateException("you have not set permission with the name " + name);
    }
    if (!Wallet.addressValid(ownerAddress)) {
      throw new ContractValidateException("invalidate ownerAddress");
    }
    if (name.isEmpty()) {
      throw new ContractValidateException("permission name should be not empty");
    }
    if (!permissionAddKeyContract.getKey().isInitialized()) {
      throw new ContractValidateException("key should be initialized");
    }
    if (!Wallet.addressValid(permissionAddKeyContract.getKey().getAddress().toByteArray())) {
      throw new ContractValidateException("address in key is invalidate");
    }
    for (Key key : permission.getKeysList()) {
      String address = Wallet
          .encode58Check(permissionAddKeyContract.getKey().getAddress().toByteArray());
      if (key.getAddress().equals(permissionAddKeyContract.getKey().getAddress())) {
        throw new ContractValidateException("address " + address + " is already in permission "
            + name);
      }
    }
    if (permissionAddKeyContract.getKey().getWeight() <= 0) {
      throw new ContractValidateException("key weight should be greater than 0");
    }
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(PermissionAddKeyContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }
}
