package org.tron.core.actuator;

import static org.tron.core.actuator.ActuatorConstant.ACCOUNT_EXCEPTION_STR;
import static org.tron.core.actuator.ActuatorConstant.NOT_EXIST_STR;
import static org.tron.core.actuator.ActuatorConstant.PROPOSAL_EXCEPTION_STR;
import static org.tron.core.actuator.ActuatorConstant.WITNESS_EXCEPTION_STR;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.ProposalStore;
import org.tron.core.store.WitnessStore;
import org.tron.protos.Protocol.Proposal.State;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.ProposalContract.ProposalApproveContract;

@Slf4j(topic = "actuator")
public class ProposalApproveActuator extends AbstractActuator {

  public ProposalApproveActuator() {
    super(ContractType.ProposalApproveContract, ProposalApproveContract.class);
  }

  @Override
  public boolean execute(Object result) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) result;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    long fee = calcFee();
    ProposalStore proposalStore = chainBaseManager.getProposalStore();
    try {
      final ProposalApproveContract proposalApproveContract =
          this.any.unpack(ProposalApproveContract.class);
      ProposalCapsule proposalCapsule = proposalStore
          .get(ByteArray.fromLong(proposalApproveContract.getProposalId()));
      ByteString committeeAddress = proposalApproveContract.getOwnerAddress();
      if (proposalApproveContract.getIsAddApproval()) {
        proposalCapsule.addApproval(committeeAddress);
      } else {
        proposalCapsule.removeApproval(committeeAddress);
      }
      proposalStore.put(proposalCapsule.createDbKey(), proposalCapsule);

      ret.setStatus(fee, code.SUCESS);
    } catch (ItemNotFoundException | InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (Objects.isNull(this.any)) {
      throw new ContractValidateException(ActuatorConstant.CONTRACT_NOT_EXIST);
    }
    if (Objects.isNull(chainBaseManager)) {
      throw new ContractValidateException(ActuatorConstant.STORE_NOT_EXIST);
    }
    AccountStore accountStore = chainBaseManager.getAccountStore();
    WitnessStore witnessStore = chainBaseManager.getWitnessStore();
    ProposalStore proposalStore = chainBaseManager.getProposalStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    if (!this.any.is(ProposalApproveContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [ProposalApproveContract],real type[" + any
              .getClass() + "]");
    }
    final ProposalApproveContract contract;
    try {
      contract = this.any.unpack(ProposalApproveContract.class);
    } catch (InvalidProtocolBufferException e) {
      throw new ContractValidateException(e.getMessage());
    }

    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);

    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    if (!accountStore.has(ownerAddress)) {
      throw new ContractValidateException(ACCOUNT_EXCEPTION_STR + readableOwnerAddress
          + NOT_EXIST_STR);
    }

    if (!witnessStore.has(ownerAddress)) {
      throw new ContractValidateException(WITNESS_EXCEPTION_STR + readableOwnerAddress
          + NOT_EXIST_STR);
    }

    long latestProposalNum = dynamicStore
        .getLatestProposalNum();
    if (contract.getProposalId() > latestProposalNum) {
      throw new ContractValidateException(PROPOSAL_EXCEPTION_STR + contract.getProposalId()
          + NOT_EXIST_STR);
    }

    long now = dynamicStore.getLatestBlockHeaderTimestamp();
    ProposalCapsule proposalCapsule;
    try {
      proposalCapsule = proposalStore.
          get(ByteArray.fromLong(contract.getProposalId()));
    } catch (ItemNotFoundException ex) {
      throw new ContractValidateException(PROPOSAL_EXCEPTION_STR + contract.getProposalId()
          + NOT_EXIST_STR);
    }

    if (now >= proposalCapsule.getExpirationTime()) {
      throw new ContractValidateException(PROPOSAL_EXCEPTION_STR + contract.getProposalId()
          + "] expired");
    }
    if (proposalCapsule.getState() == State.CANCELED) {
      throw new ContractValidateException(PROPOSAL_EXCEPTION_STR + contract.getProposalId()
          + "] canceled");
    }
    if (!contract.getIsAddApproval()) {
      if (!proposalCapsule.getApprovals().contains(contract.getOwnerAddress())) {
        throw new ContractValidateException(
            WITNESS_EXCEPTION_STR + readableOwnerAddress + "]has not approved proposal[" + contract
                .getProposalId() + "] before");
      }
    } else {
      if (proposalCapsule.getApprovals().contains(contract.getOwnerAddress())) {
        throw new ContractValidateException(
            WITNESS_EXCEPTION_STR + readableOwnerAddress + "]has approved proposal[" + contract
                .getProposalId() + "] before");
      }
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(ProposalApproveContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

}
