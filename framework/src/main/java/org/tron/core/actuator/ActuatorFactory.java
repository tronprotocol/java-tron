package org.tron.core.actuator;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ForkUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.zksnark.MerkleContainer;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.DelegationService;
import org.tron.core.db.Manager;
import org.tron.core.store.AccountIdIndexStore;
import org.tron.core.store.AccountStore;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.AssetIssueV2Store;
import org.tron.core.store.ContractStore;
import org.tron.core.store.DelegatedResourceAccountIndexStore;
import org.tron.core.store.DelegatedResourceStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.ExchangeStore;
import org.tron.core.store.ExchangeV2Store;
import org.tron.core.store.NullifierStore;
import org.tron.core.store.ProposalStore;
import org.tron.core.store.VotesStore;
import org.tron.core.store.WitnessStore;
import org.tron.core.store.ZKProofStore;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
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
        .forEach(contract -> actuatorList
            .add(getActuatorByContract(contract, manager, transactionCapsule)));
    return actuatorList;
  }

  private static Actuator getActuatorByContract(Contract contract, Manager manager,
      TransactionCapsule tx) {
    switch (contract.getType()) {
      case AccountUpdateContract:
        return new UpdateAccountActuator(contract.getParameter(), manager.getAccountStore(),
            manager.getAccountIndexStore(), manager.getDynamicPropertiesStore());
      case TransferContract:
        return new TransferActuator(contract.getParameter(), manager.getAccountStore(), manager.getAssetIssueStore(), manager.getDynamicPropertiesStore());
      case TransferAssetContract:
        return new TransferAssetActuator(contract.getParameter(), manager.getAccountStore(),
            manager.getDynamicPropertiesStore(), manager.getAssetIssueStore(), manager.getAssetIssueV2Store());
      case VoteAssetContract:
        break;
      case VoteWitnessContract:
        return new VoteWitnessActuator(contract.getParameter(), manager.getAccountStore(), manager.getWitnessStore(), manager.getVotesStore(),
            manager.getDynamicPropertiesStore(), manager.getDelegationService());
      case WitnessCreateContract:
        return new WitnessCreateActuator(contract.getParameter(), manager.getAccountStore(), manager.getDynamicPropertiesStore(), manager.getWitnessStore());
      case AccountCreateContract:
        return new CreateAccountActuator(contract.getParameter(), manager.getDynamicPropertiesStore(),
            manager.getAccountStore());
      case AssetIssueContract:
        return new AssetIssueActuator(contract.getParameter(), manager.getAccountStore(), manager.getDynamicPropertiesStore(),
            manager.getAssetIssueStore(), manager.getAssetIssueV2Store());
      case UnfreezeAssetContract:
        return new UnfreezeAssetActuator(contract.getParameter(),  manager.getAccountStore(), manager.getAssetIssueStore(),
            manager.getDynamicPropertiesStore());
      case WitnessUpdateContract:
        return new WitnessUpdateActuator(contract.getParameter(), manager.getAccountStore(), manager.getWitnessStore());
      case ParticipateAssetIssueContract:
        return new ParticipateAssetIssueActuator(contract.getParameter(), manager.getAccountStore(),  manager.getDynamicPropertiesStore(),
            manager.getAssetIssueStore(), manager.getAssetIssueV2Store());
      case FreezeBalanceContract:
        return new FreezeBalanceActuator(contract.getParameter(), manager.getAccountStore(), manager.getDynamicPropertiesStore(),
            manager.getDelegatedResourceStore(), manager.getDelegatedResourceAccountIndexStore());
      case UnfreezeBalanceContract:
        return new UnfreezeBalanceActuator(contract.getParameter(), manager.getAccountStore(), manager.getDynamicPropertiesStore(),
            manager.getDelegatedResourceStore(), manager.getDelegatedResourceAccountIndexStore(), manager.getVotesStore(),
            manager.getDelegationService());
      case WithdrawBalanceContract:
        return new WithdrawBalanceActuator(contract.getParameter(), manager.getAccountStore(), manager.getDynamicPropertiesStore(),
            manager.getWitnessStore(), manager.getDelegationService());
      case UpdateAssetContract:
        return new UpdateAssetActuator(contract.getParameter(), manager.getAccountStore(), manager.getDynamicPropertiesStore(),
            manager.getAssetIssueStore(), manager.getAssetIssueV2Store());
      case ProposalCreateContract:
        return new ProposalCreateActuator(contract.getParameter(), manager.getAccountStore(), manager.getProposalStore(),
            manager.getWitnessStore(), manager.getDynamicPropertiesStore(), manager.getForkController());
      case ProposalApproveContract:
        return new ProposalApproveActuator(contract.getParameter(), manager.getAccountStore(), manager.getWitnessStore(), manager.getProposalStore(),
            manager.getDynamicPropertiesStore());
      case ProposalDeleteContract:
        return new ProposalDeleteActuator(contract.getParameter(), manager.getAccountStore(), manager.getProposalStore(),
            manager.getDynamicPropertiesStore());
      case SetAccountIdContract:
        return new SetAccountIdActuator(contract.getParameter(),  manager.getAccountStore(), manager.getAccountIdIndexStore());
//      case BuyStorageContract:
//        return new BuyStorageActuator(contract.getParameter(), manager);
//      case BuyStorageBytesContract:
//        return new BuyStorageBytesActuator(contract.getParameter(), manager);
//      case SellStorageContract:
//        return new SellStorageActuator(contract.getParameter(), manager);
      case UpdateSettingContract:
        return new UpdateSettingContractActuator(contract.getParameter(), manager.getAccountStore(), manager.getContractStore());
      case UpdateEnergyLimitContract:
        return new UpdateEnergyLimitContractActuator(contract.getParameter(), manager.getAccountStore(),
            manager.getContractStore(), manager.getDynamicPropertiesStore());
      case ClearABIContract:
        return new ClearABIContractActuator(contract.getParameter(), manager.getAccountStore(), manager.getContractStore());
      case ExchangeCreateContract:
        return new ExchangeCreateActuator(contract.getParameter(), manager.getDynamicPropertiesStore(), manager.getAccountStore(),
      manager.getAssetIssueStore(), manager.getExchangeStore(), manager.getExchangeV2Store());

      case ExchangeInjectContract:
        return new ExchangeInjectActuator(contract.getParameter(), manager.getAccountStore(), manager.getAssetIssueStore(),
      manager.getDynamicPropertiesStore(), manager.getExchangeStore(), manager.getExchangeV2Store());

      case ExchangeWithdrawContract:
        return new ExchangeWithdrawActuator(contract.getParameter(), manager.getAccountStore(), manager.getAssetIssueStore(),  manager.getDynamicPropertiesStore(),
            manager.getExchangeStore(), manager.getExchangeV2Store());
      case ExchangeTransactionContract:
        return new ExchangeTransactionActuator(contract.getParameter(), manager.getAccountStore(), manager.getAssetIssueStore(),  manager.getDynamicPropertiesStore(),
            manager.getExchangeStore(), manager.getExchangeV2Store());
      case AccountPermissionUpdateContract:
        return new AccountPermissionUpdateActuator(contract.getParameter(), manager.getAccountStore(), manager.getDynamicPropertiesStore());
      case ShieldedTransferContract:
        return new ShieldedTransferActuator(contract.getParameter(), manager.getAccountStore(), manager.getAssetIssueStore(),
            manager.getDynamicPropertiesStore(), manager.getNullfierStore(), manager.getMerkleContainer(),
            manager.getProofStore(), tx.getTransactionId(), tx.getInstance());
      case UpdateBrokerageContract:
        return new UpdateBrokerageActuator(contract.getParameter(), manager);
      default:
        break;

    }
    return null;
  }

}
