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
import org.tron.core.config.args.Parameter.ChainParameters;
import org.tron.core.config.args.Parameter.ForkBlockVersionConsts;
import org.tron.core.config.args.Parameter.ForkBlockVersionEnum;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.contract.ProposalContract.ProposalCreateContract;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.ProposalStore;
import org.tron.core.store.WitnessStore;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "actuator")
public class ProposalCreateActuator extends AbstractActuator {

  ProposalCreateActuator(Any contract, AccountStore accountStore, ProposalStore proposalStore, WitnessStore witnessStore,
      DynamicPropertiesStore dynamicPropertiesStore, ForkUtils forkUtils) {
    super(contract, accountStore, proposalStore, witnessStore,
        dynamicPropertiesStore, forkUtils);
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

      long currentMaintenanceTime =
          dynamicStore.getNextMaintenanceTime();
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
      throw new ContractValidateException("No account store or dynamic store!");
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
      if (!validKey(entry.getKey())) {
        throw new ContractValidateException("Bad chain parameter id");
      }
      validateValue(entry);
    }

    return true;
  }

  private void validateValue(Map.Entry<Long, Long> entry) throws ContractValidateException {

    switch (entry.getKey().intValue()) {
      case (0): {
        if (entry.getValue() < 3 * 27 * 1000 || entry.getValue() > 24 * 3600 * 1000) {
          throw new ContractValidateException(
              "Bad chain parameter value,valid range is [3 * 27 * 1000,24 * 3600 * 1000]");
        }
        return;
      }
      case (1):
      case (2):
      case (3):
      case (4):
      case (5):
      case (6):
      case (7):
      case (8): {
        if (entry.getValue() < 0 || entry.getValue() > 100_000_000_000_000_000L) {
          throw new ContractValidateException(
              "Bad chain parameter value,valid range is [0,100_000_000_000_000_000L]");
        }
        break;
      }
      case (9): {
        if (entry.getValue() != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_CREATION_OF_CONTRACTS] is only allowed to be 1");
        }
        break;
      }
      case (10): {
        if (dynamicStore.getRemoveThePowerOfTheGr() == -1) {
          throw new ContractValidateException(
              "This proposal has been executed before and is only allowed to be executed once");
        }

        if (entry.getValue() != 1) {
          throw new ContractValidateException(
              "This value[REMOVE_THE_POWER_OF_THE_GR] is only allowed to be 1");
        }
        break;
      }
      case (11):
        break;
      case (12):
        break;
      case (13):
        if (entry.getValue() < 10 || entry.getValue() > 100) {
          throw new ContractValidateException(
              "Bad chain parameter value,valid range is [10,100]");
        }
        break;
      case (14): {
        if (entry.getValue() != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_UPDATE_ACCOUNT_NAME] is only allowed to be 1");
        }
        break;
      }
      case (15): {
        if (entry.getValue() != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_SAME_TOKEN_NAME] is only allowed to be 1");
        }
        break;
      }
      case (16): {
        if (entry.getValue() != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_DELEGATE_RESOURCE] is only allowed to be 1");
        }
        break;
      }
      case (17): { // deprecated
          if (!forkUtils.pass(ForkBlockVersionConsts.ENERGY_LIMIT)) {
          throw new ContractValidateException("Bad chain parameter id");
        }
        if (forkUtils.pass(ForkBlockVersionEnum.VERSION_3_2_2)) {
          throw new ContractValidateException("Bad chain parameter id");
        }
        if (entry.getValue() < 0 || entry.getValue() > 100_000_000_000_000_000L) {
          throw new ContractValidateException(
              "Bad chain parameter value,valid range is [0,100_000_000_000_000_000L]");
        }
        break;
      }
      case (18): {
        if (entry.getValue() != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_TVM_TRANSFER_TRC10] is only allowed to be 1");
        }
        if (dynamicStore.getAllowSameTokenName() == 0) {
          throw new ContractValidateException("[ALLOW_SAME_TOKEN_NAME] proposal must be approved "
              + "before [ALLOW_TVM_TRANSFER_TRC10] can be proposed");
        }
        break;
      }
      case (19): {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_3_2_2)) {
          throw new ContractValidateException("Bad chain parameter id");
        }
        if (entry.getValue() < 0 || entry.getValue() > 100_000_000_000_000_000L) {
          throw new ContractValidateException(
              "Bad chain parameter value,valid range is [0,100_000_000_000_000_000L]");
        }
        break;
      }
      case (20): {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_3_5)) {
          throw new ContractValidateException("Bad chain parameter id: ALLOW_MULTI_SIGN");
        }
        if (entry.getValue() != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_MULTI_SIGN] is only allowed to be 1");
        }
        break;
      }
      case (21): {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_3_5)) {
          throw new ContractValidateException("Bad chain parameter id: ALLOW_ADAPTIVE_ENERGY");
        }
        if (entry.getValue() != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_ADAPTIVE_ENERGY] is only allowed to be 1");
        }
        break;
      }
      case (22): {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_3_5)) {
          throw new ContractValidateException(
              "Bad chain parameter id: UPDATE_ACCOUNT_PERMISSION_FEE");
        }
        if (entry.getValue() < 0 || entry.getValue() > 100_000_000_000L) {
          throw new ContractValidateException(
              "Bad chain parameter value,valid range is [0,100_000_000_000L]");
        }
        break;
      }
      case (23): {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_3_5)) {
          throw new ContractValidateException("Bad chain parameter id: MULTI_SIGN_FEE");
        }
        if (entry.getValue() < 0 || entry.getValue() > 100_000_000_000L) {
          throw new ContractValidateException(
              "Bad chain parameter value,valid range is [0,100_000_000_000L]");
        }
        break;
      }
      case (24): {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_3_6)) {
          throw new ContractValidateException("Bad chain parameter id");
        }
        if (entry.getValue() != 1 && entry.getValue() != 0) {
          throw new ContractValidateException(
              "This value[ALLOW_PROTO_FILTER_NUM] is only allowed to be 1 or 0");
        }
        break;
      }
      case (25): {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_3_6)) {
          throw new ContractValidateException("Bad chain parameter id");
        }
        if (entry.getValue() != 1 && entry.getValue() != 0) {
          throw new ContractValidateException(
              "This value[ALLOW_ACCOUNT_STATE_ROOT] is only allowed to be 1 or 0");
        }
        break;
      }
      case (26): {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_3_6)) {
          throw new ContractValidateException("Bad chain parameter id");
        }
        if (entry.getValue() != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_TVM_CONSTANTINOPLE] is only allowed to be 1");
        }
        if (dynamicStore.getAllowTvmTransferTrc10() == 0) {
          throw new ContractValidateException(
              "[ALLOW_TVM_TRANSFER_TRC10] proposal must be approved "
                  + "before [ALLOW_TVM_CONSTANTINOPLE] can be proposed");
        }
        break;
      }
      case (27): {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_4_0)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_SHIELDED_TRANSACTION]");
        }
        if (entry.getValue() != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_SHIELDED_TRANSACTION] is only allowed to be 1");
        }
        break;
      }
      case (28): {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_4_0)) {
          throw new ContractValidateException("Bad chain parameter id [SHIELD_TRANSACTION_FEE]");
        }
        if (!dynamicStore.supportShieldedTransaction()) {
          throw new ContractValidateException(
              "Shielded Transaction is not activated,Can't set Shielded Transaction fee");
        }
        if (entry.getValue() < 0 || entry.getValue() > 10_000_000_000L) {
          throw new ContractValidateException(
              "Bad SHIELD_TRANSACTION_FEE parameter value,valid range is [0,10_000_000_000L]");
        }
        break;
      }
      case (29): {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_4_0)) {
          throw new ContractValidateException("Bad chain parameter id");
        }
        if (entry.getValue() != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_TVM_SOLIDITY_059] is only allowed to be 1");
        }
        if (dynamicStore.getAllowCreationOfContracts() == 0) {
          throw new ContractValidateException(
              "[ALLOW_CREATION_OF_CONTRACTS] proposal must be approved "
                  + "before [ALLOW_TVM_SOLIDITY_059] can be proposed");
        }
        break;
      }
      default:
        break;
    }
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(ProposalCreateContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

  private boolean validKey(long idx) {
    return idx >= 0 && idx < ChainParameters.values().length;
  }

}
