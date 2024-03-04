package org.tron.core.actuator;

import static java.util.stream.Collectors.toList;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.utils.Commons;
import org.tron.common.utils.DecodeUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Permission.PermissionType;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.AccountContract.AccountPermissionUpdateContract;


@Slf4j(topic = "actuator")
public class AccountPermissionUpdateActuator extends AbstractActuator {

  public AccountPermissionUpdateActuator() {
    super(ContractType.AccountPermissionUpdateContract, AccountPermissionUpdateContract.class);
  }

  @Override
  public boolean execute(Object object) throws ContractExeException {
    TransactionResultCapsule result = (TransactionResultCapsule) object;
    if (Objects.isNull(result)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    AccountStore accountStore = chainBaseManager.getAccountStore();
    long fee = calcFee();
    final AccountPermissionUpdateContract accountPermissionUpdateContract;
    try {
      accountPermissionUpdateContract = any.unpack(AccountPermissionUpdateContract.class);

      byte[] ownerAddress = accountPermissionUpdateContract.getOwnerAddress().toByteArray();
      AccountCapsule account = accountStore.get(ownerAddress);
      account.updatePermissions(accountPermissionUpdateContract.getOwner(),
          accountPermissionUpdateContract.getWitness(),
          accountPermissionUpdateContract.getActivesList());
      accountStore.put(ownerAddress, account);

      Commons.adjustBalance(accountStore, ownerAddress, -fee);
      if (chainBaseManager.getDynamicPropertiesStore().supportBlackHoleOptimization()) {
        chainBaseManager.getDynamicPropertiesStore().burnTrx(fee);
      } else {
        Commons.adjustBalance(accountStore, accountStore.getBlackhole(), fee);
      }

      result.setStatus(fee, code.SUCESS);
    } catch (BalanceInsufficientException | InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      result.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    return true;
  }

  private boolean checkPermission(Permission permission) throws ContractValidateException {
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();

    // Check key-related permissions
    validateKeys(permission, dynamicStore);

    // Check permission's name and parent
    validateNameAndParent(permission);

    // Validate addresses, weight sum, and threshold
    validateAddressesAndWeights(permission);

    // Check operations for Active permission
    if (permission.getType() == PermissionType.Active) {
      validateOperations(permission, dynamicStore);
    } else {
      // Non-Active permissions should not have operations
      if (!permission.getOperations().isEmpty()) {
        throw new ContractValidateException(permission.getType() + " permission doesn't need operations");
      }
    }

    return true;
  }
  @Override
  public boolean validate() throws ContractValidateException {

    if (chainBaseManager == null) {
      throw new ContractValidateException(ActuatorConstant.STORE_NOT_EXIST);
    }

    if (this.any == null) {
      throw new ContractValidateException(ActuatorConstant.CONTRACT_NOT_EXIST);
    }

    AccountStore accountStore = chainBaseManager.getAccountStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();

    if (dynamicStore.getAllowMultiSign() != 1) {
      throw new ContractValidateException("multi sign is not allowed, "
          + "need to be opened by the committee");
    }
    if (!this.any.is(AccountPermissionUpdateContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [AccountPermissionUpdateContract],real type["
              + any.getClass() + "]");
    }
    final AccountPermissionUpdateContract accountPermissionUpdateContract;
    try {
      accountPermissionUpdateContract = any.unpack(AccountPermissionUpdateContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = accountPermissionUpdateContract.getOwnerAddress().toByteArray();
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("invalidate ownerAddress");
    }
    AccountCapsule accountCapsule = accountStore.get(ownerAddress);
    if (accountCapsule == null) {
      throw new ContractValidateException("ownerAddress account does not exist");
    }

    if (!accountPermissionUpdateContract.hasOwner()) {
      throw new ContractValidateException("owner permission is missed");
    }

    if (accountCapsule.getIsWitness()) {
      if (!accountPermissionUpdateContract.hasWitness()) {
        throw new ContractValidateException("witness permission is missed");
      }
    } else {
      if (accountPermissionUpdateContract.hasWitness()) {
        throw new ContractValidateException("account isn't witness can't set witness permission");
      }
    }

    if (accountPermissionUpdateContract.getActivesCount() == 0) {
      throw new ContractValidateException("active permission is missed");
    }
    if (accountPermissionUpdateContract.getActivesCount() > 8) {
      throw new ContractValidateException("active permission is too many");
    }

    Permission owner = accountPermissionUpdateContract.getOwner();
    Permission witness = accountPermissionUpdateContract.getWitness();
    List<Permission> actives = accountPermissionUpdateContract.getActivesList();

    if (owner.getType() != PermissionType.Owner) {
      throw new ContractValidateException("owner permission type is error");
    }
    checkPermission(owner);
    if (accountCapsule.getIsWitness()) {
      if (witness.getType() != PermissionType.Witness) {
        throw new ContractValidateException("witness permission type is error");
      }
      checkPermission(witness);
    }
    for (Permission permission : actives) {
      if (permission.getType() != PermissionType.Active) {
        throw new ContractValidateException("active permission type is error");
      }
      checkPermission(permission);
    }
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(AccountPermissionUpdateContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return chainBaseManager.getDynamicPropertiesStore().getUpdateAccountPermissionFee();
  }


  private void validateKeys(Permission permission, DynamicPropertiesStore dynamicStore) throws ContractValidateException {
    int keysCount = permission.getKeysCount();
    if (keysCount > dynamicStore.getTotalSignNum()) {
      throw new ContractValidateException("Number of keys in permission should not be greater than " + dynamicStore.getTotalSignNum());
    }
    if (keysCount == 0) {
      throw new ContractValidateException("Key's count should be greater than 0");
    }
    if (permission.getType() == PermissionType.Witness && keysCount != 1) {
      throw new ContractValidateException("Witness permission's key count should be 1");
    }
  }

  private void validateNameAndParent(Permission permission) throws ContractValidateException {
    String name = permission.getPermissionName();
    if (!StringUtils.isEmpty(name) && name.length() > 32) {
      throw new ContractValidateException("Permission's name is too long");
    }
    if (permission.getParentId() != 0) {
      throw new ContractValidateException("Permission's parent should be owner");
    }
  }

  private void validateAddressesAndWeights(Permission permission) throws ContractValidateException {
    Set<ByteString> uniqueAddresses = permission.getKeysList()
            .stream()
            .map(Key::getAddress)
            .collect(Collectors.toSet());
    if (uniqueAddresses.size() != permission.getKeysCount()) {
      throw new ContractValidateException("Address should be distinct in permission " + permission.getType());
    }

    long weightSum = permission.getKeysList().stream()
            .peek(key -> {
              if (!DecodeUtil.addressValid(key.getAddress().toByteArray())) {
                throw new ContractValidateException("Key is not a valid address");
              }
              if (key.getWeight() <= 0) {
                throw new ContractValidateException("Key's weight should be greater than 0");
              }
            })
            .mapToLong(Key::getWeight)
            .sum();

    if (weightSum < permission.getThreshold()) {
      throw new ContractValidateException("Sum of all key's weight should not be less than threshold in permission " + permission.getType());
    }
  }

  private void validateOperations(Permission permission, DynamicPropertiesStore dynamicStore) throws ContractValidateException {
    ByteString operations = permission.getOperations();
    if (operations.isEmpty() || operations.size() != 32) {
      throw new ContractValidateException("Operations size must be 32");
    }

    // Check available contract types
    byte[] types1 = dynamicStore.getAvailableContractType();
    for (int i = 0; i < 256; i++) {
      boolean b = (operations.byteAt(i / 8) & (1 << (i % 8))) != 0;
      boolean t = ((types1[(i / 8)] & 0xff) & (1 << (i % 8))) != 0;
      if (b && !t) {
        throw new ContractValidateException(i + " isn't a valid ContractType");
      }
    }
  }


}
