package org.tron.core.actuator;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ForkUtils;
import org.tron.common.zksnark.MerkleContainer;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.TypeMismatchNamingException;
import org.tron.core.store.AccountIdIndexStore;
import org.tron.core.store.AccountIndexStore;
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
import org.tron.core.store.StoreFactory;
import org.tron.core.store.VotesStore;
import org.tron.core.store.WitnessStore;
import org.tron.core.store.ZKProofStore;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract;

@Slf4j(topic = "actuator")
public class ActuatorCreator  {

  private AccountStore accountStore;

  private AccountIdIndexStore accountIdIndexStore;

  private DynamicPropertiesStore dynamicPropertiesStore;

  private AssetIssueStore assetIssueStore;

  private ContractStore contractStore;

  private AssetIssueV2Store assetIssueV2Store;

  private WitnessStore witnessStore;

  private VotesStore votesStore;

  private ZKProofStore proofStore;

  private NullifierStore nullifierStore;

  private DelegatedResourceStore delegatedResourceStore;

  private DelegatedResourceAccountIndexStore delegatedResourceAccountIndexStore;

  private ExchangeStore exchangeStore;

  private ExchangeV2Store exchangeV2Store;

  private ProposalStore proposalStore;

  private AccountIndexStore accountIndexStore;

  private ForkUtils forkUtils = new ForkUtils();
  ;

  private MerkleContainer merkleContainer;


  public ActuatorCreator(StoreFactory storeFactory) {
    try {
      accountStore = storeFactory.getStore(AccountStore.class);
      accountIdIndexStore = storeFactory.getStore(AccountIdIndexStore.class);
      dynamicPropertiesStore = storeFactory.getStore(DynamicPropertiesStore.class);
      assetIssueStore = storeFactory.getStore(AssetIssueStore.class);
      assetIssueV2Store = storeFactory.getStore(AssetIssueV2Store.class);
      contractStore = storeFactory.getStore(ContractStore.class);
      witnessStore = storeFactory.getStore(WitnessStore.class);
      votesStore = storeFactory.getStore(VotesStore.class);
      proofStore = storeFactory.getStore(ZKProofStore.class);
      nullifierStore = storeFactory.getStore(NullifierStore.class);
      delegatedResourceAccountIndexStore = storeFactory
          .getStore(DelegatedResourceAccountIndexStore.class);
      delegatedResourceStore = storeFactory.getStore(DelegatedResourceStore.class);
      exchangeStore = storeFactory.getStore(ExchangeStore.class);
      exchangeV2Store = storeFactory.getStore(ExchangeV2Store.class);
      proposalStore = storeFactory.getStore(ProposalStore.class);
      merkleContainer = storeFactory.getStore(MerkleContainer.class);
      accountIndexStore = storeFactory.getStore(AccountIndexStore.class);
      forkUtils.setDynamicPropertiesStore(dynamicPropertiesStore);
    } catch (TypeMismatchNamingException e) {
      logger.error("ActuatorCreator error", e);
    }

  }

  /**
   * create actuator.
   */
  public List<Actuator> createActuator(TransactionCapsule transactionCapsule) {
    List<Actuator> actuatorList = Lists.newArrayList();
    if (null == transactionCapsule || null == transactionCapsule.getInstance()) {
      logger.info("transactionCapsule or Transaction is null");
      return actuatorList;
    }

    Protocol.Transaction.raw rawData = transactionCapsule.getInstance().getRawData();
    rawData.getContractList()
        .forEach(contract -> actuatorList
            .add(getActuatorByContract(contract, transactionCapsule)));
    return actuatorList;
  }

  private Actuator getActuatorByContract(Contract contract,
      TransactionCapsule tx) {
    switch (contract.getType()) {
      case AccountUpdateContract:
        return new UpdateAccountActuator(contract.getParameter(), accountStore,
            accountIndexStore, dynamicPropertiesStore);
      case TransferContract:
        return new TransferActuator(contract.getParameter(), accountStore,
            assetIssueStore, dynamicPropertiesStore);
      case TransferAssetContract:
        return new TransferAssetActuator(contract.getParameter(), accountStore,
            dynamicPropertiesStore,
            assetIssueStore, assetIssueV2Store);
      case VoteAssetContract:
        break;
      case VoteWitnessContract:
        return new VoteWitnessActuator(contract.getParameter(), accountStore,
            witnessStore, votesStore,
            dynamicPropertiesStore);
      case WitnessCreateContract:
        return new WitnessCreateActuator(contract.getParameter(), accountStore,
            dynamicPropertiesStore, witnessStore);
      case AccountCreateContract:
        return new CreateAccountActuator(contract.getParameter(),
            dynamicPropertiesStore, accountStore);
      case AssetIssueContract:
        return new AssetIssueActuator(contract.getParameter(), accountStore,
            dynamicPropertiesStore,
            assetIssueStore, assetIssueV2Store);
      case UnfreezeAssetContract:
        return new UnfreezeAssetActuator(contract.getParameter(), accountStore,
            assetIssueStore,
            dynamicPropertiesStore);
      case WitnessUpdateContract:
        return new WitnessUpdateActuator(contract.getParameter(), accountStore,
            witnessStore);
      case ParticipateAssetIssueContract:
        return new ParticipateAssetIssueActuator(contract.getParameter(), accountStore,
            dynamicPropertiesStore,
            assetIssueStore, assetIssueV2Store);
      case FreezeBalanceContract:
        return new FreezeBalanceActuator(contract.getParameter(), accountStore,
            dynamicPropertiesStore,
            delegatedResourceStore, delegatedResourceAccountIndexStore);
      case UnfreezeBalanceContract:
        return new UnfreezeBalanceActuator(contract.getParameter(), accountStore,
            dynamicPropertiesStore,
            delegatedResourceStore, delegatedResourceAccountIndexStore,
            votesStore);
      case WithdrawBalanceContract:
        return new WithdrawBalanceActuator(contract.getParameter(), accountStore,
            dynamicPropertiesStore, witnessStore);
      case UpdateAssetContract:
        return new UpdateAssetActuator(contract.getParameter(), accountStore,
            dynamicPropertiesStore,
            assetIssueStore, assetIssueV2Store);
      case ProposalCreateContract:
        return new ProposalCreateActuator(contract.getParameter(), accountStore,
            proposalStore,
            witnessStore, dynamicPropertiesStore,
            forkUtils);
      case ProposalApproveContract:
        return new ProposalApproveActuator(contract.getParameter(), accountStore,
            witnessStore,
            proposalStore, dynamicPropertiesStore);
      case ProposalDeleteContract:
        return new ProposalDeleteActuator(contract.getParameter(), accountStore,
            proposalStore,
            dynamicPropertiesStore);
      case SetAccountIdContract:
        return new SetAccountIdActuator(contract.getParameter(), accountStore,
            accountIdIndexStore);
//      case BuyStorageContract:
//        return new BuyStorageActuator(contract.getParameter(), manager);
//      case BuyStorageBytesContract:
//        return new BuyStorageBytesActuator(contract.getParameter(), manager);
//      case SellStorageContract:
//        return new SellStorageActuator(contract.getParameter(), manager);
      case UpdateSettingContract:
        return new UpdateSettingContractActuator(contract.getParameter(), accountStore,
            contractStore);
      case UpdateEnergyLimitContract:
        return new UpdateEnergyLimitContractActuator(contract.getParameter(),
            accountStore,
            contractStore, dynamicPropertiesStore);
      case ClearABIContract:
        return new ClearABIContractActuator(contract.getParameter(), accountStore,
            contractStore);
      case ExchangeCreateContract:
        return new ExchangeCreateActuator(contract.getParameter(),
            dynamicPropertiesStore, accountStore,
            assetIssueStore, exchangeStore, exchangeV2Store);
      case ExchangeInjectContract:
        return new ExchangeInjectActuator(contract.getParameter(), accountStore,
            assetIssueStore,
            dynamicPropertiesStore, exchangeStore,
            exchangeV2Store);
      case ExchangeWithdrawContract:
        return new ExchangeWithdrawActuator(contract.getParameter(), accountStore,
            assetIssueStore,
            dynamicPropertiesStore, exchangeStore,
            exchangeV2Store);
      case ExchangeTransactionContract:
        return new ExchangeTransactionActuator(contract.getParameter(), accountStore,
            assetIssueStore,
            dynamicPropertiesStore, exchangeStore,
            exchangeV2Store);
      case AccountPermissionUpdateContract:
        return new AccountPermissionUpdateActuator(contract.getParameter(),
            accountStore, dynamicPropertiesStore);
      case ShieldedTransferContract:
        return new ShieldedTransferActuator(contract.getParameter(), accountStore,
            assetIssueStore, dynamicPropertiesStore,
            nullifierStore,
            merkleContainer, proofStore,
            tx.getTransactionId(), tx.getInstance());
      default:
        break;

    }
    return null;
  }
}
