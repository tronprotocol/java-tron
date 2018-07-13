package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.StringUtil;
import org.tron.core.Wallet;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.protos.Contract.ProposalApproveContract;
import org.tron.protos.Protocol.Proposal.State;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class ProposalApproveActuator extends AbstractActuator {

  ProposalApproveActuator(final Any contract, final Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    long fee = calcFee();
    try {
      final ProposalApproveContract proposalApproveContract =
          this.contract.unpack(ProposalApproveContract.class);
      ProposalCapsule proposalCapsule = dbManager.getProposalStore().
          get(ByteArray.fromLong(proposalApproveContract.getProposalId()));

      ByteString committeeAddress = proposalApproveContract.getOwnerAddress();
      if (proposalApproveContract.getIsAddApproval()) {
        proposalCapsule.addApproval(committeeAddress);
      } else {
        proposalCapsule.removeApproval(committeeAddress);
      }
      dbManager.getProposalStore().put(proposalCapsule.createDbKey(), proposalCapsule);
      ret.setStatus(fee, code.SUCESS);
    } catch (ItemNotFoundException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
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
    if (this.dbManager == null) {
      throw new ContractValidateException("No dbManager!");
    }
    if (!this.contract.is(ProposalApproveContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [ProposalApproveContract],real type[" + contract
              .getClass() + "]");
    }
    final ProposalApproveContract contract;
    try {
      contract = this.contract.unpack(ProposalApproveContract.class);
    } catch (InvalidProtocolBufferException e) {
      throw new ContractValidateException(e.getMessage());
    }

    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);

    if (!Wallet.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    if (!this.dbManager.getAccountStore().has(ownerAddress)) {
      throw new ContractValidateException("account[" + readableOwnerAddress + "] not exists");
    }

    if (!this.dbManager.getWitnessStore().has(ownerAddress)) {
      throw new ContractValidateException("Witness[" + readableOwnerAddress + "] not exists");
    }

    if (contract.getProposalId() > dbManager.getDynamicPropertiesStore().getLatestProposalNum()) {
      throw new ContractValidateException("Proposal[" + contract.getProposalId() + "] not exists");
    }

    long now = dbManager.getHeadBlockTimeStamp();
    ProposalCapsule proposalCapsule;
    try {
      proposalCapsule = dbManager.getProposalStore().
          get(ByteArray.fromLong(contract.getProposalId()));
    } catch (ItemNotFoundException ex) {
      throw new ContractValidateException("Proposal[" + contract.getProposalId() + "] not exists");
    }

    if (now >= proposalCapsule.getExpirationTime()) {
      throw new ContractValidateException("Proposal[" + contract.getProposalId() + "] expired");
    }
    if (proposalCapsule.getState() == State.CANCELED) {
      throw new ContractValidateException("Proposal[" + contract.getProposalId() + "] canceled");
    }
    if (!contract.getIsAddApproval()) {
      if (!proposalCapsule.getApprovals().contains(contract.getOwnerAddress())) {
        throw new ContractValidateException(
            "witness [" + readableOwnerAddress + "]has not approved proposal[" + contract
                .getProposalId() + "] before");
      }
    } else {
      if (proposalCapsule.getApprovals().contains(contract.getOwnerAddress())) {
        throw new ContractValidateException(
            "witness [" + readableOwnerAddress + "]has approved proposal[" + contract
                .getProposalId() + "] before");
      }
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(ProposalApproveContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

}
