package org.tron.core.actuator;

import static org.tron.common.utils.DBConfig.allowTvmConstantinople;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.Commons;
import org.tron.common.utils.DBConfig;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.contract.SmartContractOuterClass.ClearABIContract;
import org.tron.core.store.AccountStore;
import org.tron.core.store.ContractStore;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "actuator")
public class ClearABIContractActuator extends AbstractActuator {

  ClearABIContractActuator(Any contract, AccountStore accountStore, ContractStore contractStore) {
    super(contract, accountStore, contractStore);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    long fee = calcFee();
    try {
      ClearABIContract usContract = contract.unpack(ClearABIContract.class);

      byte[] contractAddress = usContract.getContractAddress().toByteArray();
      ContractCapsule deployedContract = contractStore.get(contractAddress);

      deployedContract.clearABI();
      contractStore.put(contractAddress, deployedContract);

      ret.setStatus(fee, code.SUCESS);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (!allowTvmConstantinople()) {
      throw new ContractValidateException(
          "contract type error,unexpected type [ClearABIContract]");
    }

    if (this.contract == null) {
      throw new ContractValidateException("No contract!");
    }
    if (accountStore == null || contractStore == null) {
      throw new ContractValidateException("No account store or contract store!");
    }
    if (!this.contract.is(ClearABIContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [ClearABIContract],real type["
              + contract
              .getClass() + "]");
    }
    final ClearABIContract contract;
    try {
      contract = this.contract.unpack(ClearABIContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    if (!Commons.addressValid(contract.getOwnerAddress().toByteArray())) {
      throw new ContractValidateException("Invalid address");
    }
    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);

    AccountCapsule accountCapsule = accountStore.get(ownerAddress);
    if (accountCapsule == null) {
      throw new ContractValidateException(
          "Account[" + readableOwnerAddress + "] not exists");
    }

    byte[] contractAddress = contract.getContractAddress().toByteArray();
    ContractCapsule deployedContract = contractStore.get(contractAddress);

    if (deployedContract == null) {
      throw new ContractValidateException(
          "Contract not exists");
    }

    byte[] deployedContractOwnerAddress = deployedContract.getInstance().getOriginAddress()
        .toByteArray();

    if (!Arrays.equals(ownerAddress, deployedContractOwnerAddress)) {
      throw new ContractValidateException(
          "Account[" + readableOwnerAddress + "] is not the owner of the contract");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(ClearABIContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

}
