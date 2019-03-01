package org.tron.core.actuator;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.Manager;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract;

@Slf4j(topic = "actuator")
public class ActuatorFactory {

  public static final ActuatorFactory INSTANCE = new ActuatorFactory();

  private ActuatorFactory() {
  }

  public static ActuatorFactory getInstance() {
    return INSTANCE;
  }

  /**
   * create actuator.
   */
  public static List<Actuator> createActuator(TransactionCapsule transactionCapsule,
      Manager manager) {
    List<Actuator> actuatorList = Lists.newArrayList();
    if (null == transactionCapsule || null == transactionCapsule.getInstance()) {
      logger.info("transactionCapsule or Transaction is null");
      return actuatorList;
    }

    Preconditions.checkNotNull(manager, "manager is null");
    Protocol.Transaction.raw rawData = transactionCapsule.getInstance().getRawData();
    rawData.getContractList()
        .forEach(contract -> actuatorList.add(getActuatorByContract(contract, manager,transactionCapsule.getContractType())));
    return actuatorList;
  }

  private static Actuator getActuatorByContract(Contract contract, Manager manager, int contractType) {
    switch (contract.getType()) {
      case AccountUpdateContract:
        return new UpdateAccountActuator(contract.getParameter(), manager, contractType);
      case TransferContract:
        return new TransferActuator(contract.getParameter(), manager, contractType);
      case TransferAssetContract:
        return new TransferAssetActuator(contract.getParameter(), manager, contractType);
      case VoteAssetContract:
        break;
      case VoteWitnessContract:
        return new VoteWitnessActuator(contract.getParameter(), manager);
      case WitnessCreateContract:
        return new WitnessCreateActuator(contract.getParameter(), manager);
      case AccountCreateContract:
        return new CreateAccountActuator(contract.getParameter(), manager, contractType);
      case AssetIssueContract:
        return new AssetIssueActuator(contract.getParameter(), manager);
      case UnfreezeAssetContract:
        return new UnfreezeAssetActuator(contract.getParameter(), manager, contractType);
      case WitnessUpdateContract:
        return new WitnessUpdateActuator(contract.getParameter(), manager);
      case ParticipateAssetIssueContract:
        return new ParticipateAssetIssueActuator(contract.getParameter(), manager, contractType);
      case FreezeBalanceContract:
        return new FreezeBalanceActuator(contract.getParameter(), manager, contractType);
      case UnfreezeBalanceContract:
        return new UnfreezeBalanceActuator(contract.getParameter(), manager, contractType);
      case WithdrawBalanceContract:
        return new WithdrawBalanceActuator(contract.getParameter(), manager, contractType);
      case UpdateAssetContract:
        return new UpdateAssetActuator(contract.getParameter(), manager, contractType);
      case ProposalCreateContract:
        return new ProposalCreateActuator(contract.getParameter(), manager, contractType);
      case ProposalApproveContract:
        return new ProposalApproveActuator(contract.getParameter(), manager, contractType);
      case ProposalDeleteContract:
        return new ProposalDeleteActuator(contract.getParameter(), manager, contractType);
      case SetAccountIdContract:
        return new SetAccountIdActuator(contract.getParameter(), manager, contractType);
//      case BuyStorageContract:
//        return new BuyStorageActuator(contract.getParameter(), manager);
//      case BuyStorageBytesContract:
//        return new BuyStorageBytesActuator(contract.getParameter(), manager);
//      case SellStorageContract:
//        return new SellStorageActuator(contract.getParameter(), manager);
      case UpdateSettingContract:
        return new UpdateSettingContractActuator(contract.getParameter(), manager, contractType);
      case UpdateEnergyLimitContract:
        return new UpdateEnergyLimitContractActuator(contract.getParameter(), manager, contractType);
      case ExchangeCreateContract:
        return new ExchangeCreateActuator(contract.getParameter(), manager, contractType);
      case ExchangeInjectContract:
        return new ExchangeInjectActuator(contract.getParameter(), manager, contractType);
      case ExchangeWithdrawContract:
        return new ExchangeWithdrawActuator(contract.getParameter(), manager, contractType);
      case ExchangeTransactionContract:
        return new ExchangeTransactionActuator(contract.getParameter(), manager, contractType);
      case AccountPermissionUpdateContract:
        return new AccountPermissionUpdateActuator(contract.getParameter(), manager, contractType);
      case CancelDeferredTransactionContract:
        return new CancelDeferredTransactionContractActuator(contract.getParameter(), manager);
      default:
        break;

    }
    return null;
  }

}
