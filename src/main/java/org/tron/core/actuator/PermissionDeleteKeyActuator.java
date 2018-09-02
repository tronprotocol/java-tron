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
import org.tron.protos.Contract.PermissionDeleteKeyContract;
import org.tron.protos.Protocol.Transaction.Result.code;


@Slf4j
public class PermissionDeleteKeyActuator extends AbstractActuator {

  PermissionDeleteKeyActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule result) throws ContractExeException {
    long fee = calcFee();
    final PermissionDeleteKeyContract permissionDeleteKeyContract;
    try {
      permissionDeleteKeyContract = contract.unpack(PermissionDeleteKeyContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      result.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    byte[] ownerAddress = permissionDeleteKeyContract.getOwnerAddress().toByteArray();
    AccountStore accountStore = dbManager.getAccountStore();
    AccountCapsule account = accountStore.get(ownerAddress);
    account.permissionDeleteKey(permissionDeleteKeyContract.getKeyAddress(),
        permissionDeleteKeyContract.getPermissionName());
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
    if (!this.contract.is(PermissionDeleteKeyContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [PermissionDeleteKeyContract],real type[" + contract
              .getClass() + "]");
    }
    final PermissionDeleteKeyContract permissionDeleteKeyContract;
    try {
      permissionDeleteKeyContract = contract.unpack(PermissionDeleteKeyContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = permissionDeleteKeyContract.getOwnerAddress().toByteArray();
    if (!Wallet.addressValid(ownerAddress)) {
      throw new ContractValidateException("invalidate ownerAddress");
    }
    if (permissionDeleteKeyContract.getPermissionName().isEmpty()) {
      throw new ContractValidateException("permission name should be not empty");
    }
    if (!permissionDeleteKeyContract.getPermissionName().equalsIgnoreCase("owner") &&
        !permissionDeleteKeyContract.getPermissionName().equalsIgnoreCase("active")) {
      throw new ContractValidateException("permission name should be owner or active");
    }
    if (!Wallet.addressValid(permissionDeleteKeyContract.getKeyAddress().toByteArray())) {
      throw new ContractValidateException("address in key is invalidate");
    }
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(PermissionDeleteKeyContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }
}
