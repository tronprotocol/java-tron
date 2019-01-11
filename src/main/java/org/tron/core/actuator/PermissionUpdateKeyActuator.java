package org.tron.core.actuator;

import static java.util.stream.Collectors.toSet;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.AccountStore;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.PermissionUpdateKeyContract;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "actuator")
public class PermissionUpdateKeyActuator extends AbstractActuator {

  PermissionUpdateKeyActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule result) throws ContractExeException {
    long fee = calcFee();
    final PermissionUpdateKeyContract permissionUpdateKeyContract;
    try {
      permissionUpdateKeyContract = contract.unpack(PermissionUpdateKeyContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      result.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    byte[] ownerAddress = permissionUpdateKeyContract.getOwnerAddress().toByteArray();
    AccountStore accountStore = dbManager.getAccountStore();
    AccountCapsule account = accountStore.get(ownerAddress);
    account.permissionUpdateKey(permissionUpdateKeyContract.getKey(),
        permissionUpdateKeyContract.getPermissionId());
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
    if (this.dbManager.getDynamicPropertiesStore().getAllowMultiSign() != 1) {
      throw new ContractValidateException("multi sign is not allowed, "
          + "need to be opened by the committee");
    }
    if (!this.contract.is(PermissionUpdateKeyContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [PermissionUpdateKeyContract],real type[" + contract
              .getClass() + "]");
    }
    final PermissionUpdateKeyContract permissionUpdateKeyContract;
    try {
      permissionUpdateKeyContract = contract.unpack(PermissionUpdateKeyContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = permissionUpdateKeyContract.getOwnerAddress().toByteArray();
    if (!Wallet.addressValid(ownerAddress)) {
      throw new ContractValidateException("invalidate ownerAddress");
    }
    AccountCapsule account = dbManager.getAccountStore().get(ownerAddress);
    if (account == null) {
      throw new ContractValidateException("ownerAddress account does not exist");
    }
    int id = permissionUpdateKeyContract.getPermissionId();
    Permission ownerPermission = account.getInstance().getOwnerPermission();
    if (ownerPermission == null) {
      throw new ContractValidateException("you have not set owner permission");
    }
    Permission permission = account.getPermissionById(id);
    if (permission == null) {
      throw new ContractValidateException("you have not set permission with the id " + id);
    }
    if (!Wallet.addressValid(ownerAddress)) {
      throw new ContractValidateException("invalidate ownerAddress");
    }
    if (!Wallet.addressValid(permissionUpdateKeyContract.getKey().getAddress().toByteArray())) {
      throw new ContractValidateException("address in key is invalidate");
    }
    Set<ByteString> addressSet = permission.getKeysList()
        .stream()
        .map(x -> x.getAddress())
        .collect(toSet());
    if (!addressSet.contains(permissionUpdateKeyContract.getKey().getAddress())) {
      throw new ContractValidateException(String.format("address is not in permission %d", id));
    }
    if (permissionUpdateKeyContract.getKey().getWeight() <= 0) {
      throw new ContractValidateException("key weight should be greater than 0");
    }
    long weightSum = 0;
    for (Key key : permission.getKeysList()) {
      if (!key.getAddress().equals(permissionUpdateKeyContract.getKey().getAddress())) {
        try {
          weightSum = Math.addExact(weightSum, key.getWeight());
        } catch (ArithmeticException e) {
          throw new ContractValidateException(e.getMessage());
        }
      }
    }
    try {
      weightSum = Math.addExact(weightSum, permissionUpdateKeyContract.getKey().getWeight());
    } catch (ArithmeticException e) {
      throw new ContractValidateException(e.getMessage());
    }
    if (weightSum < permission.getThreshold()) {
      throw new ContractValidateException(
          "sum of all keys weight should not be less that threshold");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(PermissionUpdateKeyContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }
}
