package org.tron.core.actuator;

import static org.tron.core.actuator.ActuatorConstant.ACCOUNT_EXCEPTION_STR;
import static org.tron.core.actuator.ActuatorConstant.NOT_EXIST_STR;
import static org.tron.core.actuator.ActuatorConstant.WITNESS_EXCEPTION_STR;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.Commons;
import org.tron.common.utils.DBConfig;
import org.tron.common.utils.ForkUtils;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.ProposalStore;
import org.tron.core.store.WitnessStore;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.ProposalContract.ProposalCreateContract;

@Slf4j(topic = "actuator")
public class ProposalCreateActuator extends AbstractActuator {

  ProposalCreateActuator(Any contract, AccountStore accountStore, ProposalStore proposalStore, WitnessStore witnessStore,
      DynamicPropertiesStore dynamicPropertiesStore, ForkUtils forkUtils) {
    super(contract, accountStore, proposalStore, witnessStore, dynamicPropertiesStore, forkUtils);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    long fee = calcFee();
    try {
      final ProposalCreateContract proposalCreateContract = this.contract
          .unpack(ProposalCreateContract.class);
      long id = dynamicStore.getLatestProposalNum() + 1;
      ProposalCapsule proposalCapsule =
          new ProposalCapsule(proposalCreateContract.getOwnerAddress(), id);

      proposalCapsule.setParameters(proposalCreateContract.getParametersMap());

      long now = dynamicStore.getLatestBlockHeaderTimestamp();
      long maintenanceTimeInterval = dynamicStore.getMaintenanceTimeInterval();
      proposalCapsule.setCreateTime(now);

      long currentMaintenanceTime = dynamicStore.getNextMaintenanceTime();
      long now3 = now + DBConfig.getProposalExpireTime();
      long round = (now3 - currentMaintenanceTime) / maintenanceTimeInterval;
      long expirationTime =
          currentMaintenanceTime + (round + 1) * maintenanceTimeInterval;
      proposalCapsule.setExpirationTime(expirationTime);

      proposalStore.put(proposalCapsule.createDbKey(), proposalCapsule);
      dynamicStore.saveLatestProposalNum(id);

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
    if (this.contract == null) {
      throw new ContractValidateException("No contract!");
    }
    if (accountStore == null || dynamicStore == null) {
      throw new ContractValidateException("No dbManager!");
    }
    if (!this.contract.is(ProposalCreateContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [ProposalCreateContract],real type[" + contract
              .getClass() + "]");
    }
    final ProposalCreateContract contract;
    try {
      contract = this.contract.unpack(ProposalCreateContract.class);
    } catch (InvalidProtocolBufferException e) {
      throw new ContractValidateException(e.getMessage());
    }

    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);

    if (!Commons.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    if (!accountStore.has(ownerAddress)) {
      throw new ContractValidateException(
          ACCOUNT_EXCEPTION_STR + readableOwnerAddress + NOT_EXIST_STR);
    }

    if (!witnessStore.has(ownerAddress)) {
      throw new ContractValidateException(
          WITNESS_EXCEPTION_STR + readableOwnerAddress + NOT_EXIST_STR);
    }

    if (contract.getParametersMap().size() == 0) {
      throw new ContractValidateException("This proposal has no parameter.");
    }

    for (Map.Entry<Long, Long> entry : contract.getParametersMap().entrySet()) {
      validateValue(entry);
    }

    return true;
  }

  private void validateValue(Map.Entry<Long, Long> entry) throws ContractValidateException {
    ProposalService.validator(dbManager, entry.getKey(), entry.getValue());
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(ProposalCreateContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

}
