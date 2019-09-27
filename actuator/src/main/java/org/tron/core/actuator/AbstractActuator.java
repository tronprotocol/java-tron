package org.tron.core.actuator;

import com.google.protobuf.Any;
import org.tron.common.utils.ForkUtils;
import org.tron.common.zksnark.MerkleContainer;
import org.tron.core.db.DelegationService;
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
import org.tron.core.store.VotesStore;
import org.tron.core.store.WitnessStore;
import org.tron.core.store.ZKProofStore;


public abstract class AbstractActuator implements Actuator {

  protected Any contract;
  protected AccountStore accountStore;
  protected AccountIdIndexStore accountIdIndexStore;
  protected DynamicPropertiesStore dynamicStore;
  protected AssetIssueStore assetIssueStore;
  protected AssetIssueV2Store assetIssueV2Store;
  protected ContractStore contractStore;
  protected ExchangeStore exchangeStore;
  protected ExchangeV2Store exchangeV2Store;
  protected DelegatedResourceStore delegatedResourceStore;
  protected AccountIndexStore accountIndexStore;
  protected DelegatedResourceAccountIndexStore delegatedResourceAccountIndexStore;
  protected VotesStore votesStore;
  protected WitnessStore witnessStore;
  protected ProposalStore proposalStore;
  protected ForkUtils forkUtils;

  protected NullifierStore nullifierStore;
  protected ZKProofStore proofStore;
  protected MerkleContainer merkleContainer;
  protected DelegationService delegationService;

  AbstractActuator(Any contract, AccountStore accountStore, DynamicPropertiesStore dynamicStore) {
    this.contract = contract;
    this.accountStore = accountStore;
    this.dynamicStore = dynamicStore;
  }

  AbstractActuator(Any contract, AccountStore accountStore, AssetIssueStore assetIssueStore,
      DynamicPropertiesStore dynamicPropertiesStore, NullifierStore nullifierStore, MerkleContainer merkleContainer,
      ZKProofStore zkProofStore) {
    this.contract = contract;
    this.accountStore = accountStore;
    this.assetIssueStore = assetIssueStore;
    this.nullifierStore = nullifierStore;
    this.merkleContainer = merkleContainer;
    this.dynamicStore = dynamicPropertiesStore;
    this.proofStore = zkProofStore;
  }

  AbstractActuator(Any contract, AccountStore accountStore, DynamicPropertiesStore dynamicStore, DelegatedResourceStore delegatedResourceStore,
      DelegatedResourceAccountIndexStore delegatedResourceAccountIndexStore, VotesStore votesStore, DelegationService delegationService) {
    this.contract = contract;
    this.accountStore = accountStore;
    this.dynamicStore = dynamicStore;
    this.delegatedResourceStore = delegatedResourceStore;
    this.delegatedResourceAccountIndexStore = delegatedResourceAccountIndexStore;
    this.votesStore = votesStore;
    this.delegationService = delegationService;
  }

  AbstractActuator(Any contract, AccountStore accountStore, WitnessStore witnessStore,  VotesStore votesStore, DynamicPropertiesStore dynamicPropertiesStore) {
    this.contract = contract;
    this.accountStore = accountStore;
    this.witnessStore = witnessStore;
    this.votesStore = votesStore;
    this.dynamicStore = dynamicPropertiesStore;
  }


  AbstractActuator(Any contract, AccountStore accountStore, AssetIssueStore assetIssueStore,  DynamicPropertiesStore dynamicStore) {
    this.contract = contract;
    this.accountStore = accountStore;
    this.dynamicStore = dynamicStore;
    this.assetIssueStore = assetIssueStore;
  }

  AbstractActuator(Any contract, AccountStore accountStore, AccountIdIndexStore accountIdIndexStore) {
    this.contract = contract;
    this.accountStore = accountStore;
    this.accountIdIndexStore = accountIdIndexStore;
  }

  AbstractActuator(Any contract, AccountStore accountStore, AccountIndexStore accountIndexStore, DynamicPropertiesStore dynamicPropertiesStore) {
    this.contract = contract;
    this.accountStore = accountStore;
    this.accountIndexStore = accountIndexStore;
    this.dynamicStore = dynamicPropertiesStore;
  }

  AbstractActuator(Any contract, AccountStore accountStore, DynamicPropertiesStore dynamicStore,
      DelegatedResourceStore delegatedResourceStore, DelegatedResourceAccountIndexStore delegatedResourceAccountIndexStore) {
    this.contract = contract;
    this.accountStore = accountStore;
    this.dynamicStore = dynamicStore;
    this.delegatedResourceStore = delegatedResourceStore;
    this.delegatedResourceAccountIndexStore = delegatedResourceAccountIndexStore;
  }

  AbstractActuator(Any contract, AccountStore accountStore, AssetIssueStore assetIssueStore,
      DynamicPropertiesStore dynamicStore, ExchangeStore exchangeStore, ExchangeV2Store exchangeV2Store) {
    this.contract = contract;
    this.accountStore = accountStore;
    this.dynamicStore = dynamicStore;
    this.exchangeStore = exchangeStore;
    this.assetIssueStore = assetIssueStore;
    this.exchangeV2Store = exchangeV2Store;
  }

  AbstractActuator(Any contract, AccountStore accountStore, DynamicPropertiesStore dynamicStore,
      AssetIssueStore assetIssueStore, AssetIssueV2Store assetIssueV2Store) {
    this.contract = contract;
    this.accountStore = accountStore;
    this.dynamicStore = dynamicStore;
    this.assetIssueStore = assetIssueStore;
    this.assetIssueV2Store = assetIssueV2Store;
  }

  AbstractActuator(Any contract, AccountStore accountStore, ContractStore contractStore) {
    this.contract = contract;
    this.accountStore = accountStore;
    this.contractStore = contractStore;
  }

  AbstractActuator(Any contract, AccountStore accountStore, DynamicPropertiesStore dynamicPropertiesStore, WitnessStore witnessStore) {
    this.contract = contract;
    this.accountStore = accountStore;
    this.dynamicStore = dynamicPropertiesStore;
    this.witnessStore = witnessStore;
  }

  AbstractActuator(Any contract, AccountStore accountStore, WitnessStore witnessStore) {
    this.contract = contract;
    this.accountStore = accountStore;
    this.witnessStore = witnessStore;
  }

  AbstractActuator(Any contract, AccountStore accountStore, ContractStore contractStore, DynamicPropertiesStore dynamicPropertiesStore) {
    this.contract = contract;
    this.accountStore = accountStore;
    this.dynamicStore = dynamicPropertiesStore;
    this.contractStore = contractStore;
  }

  AbstractActuator(Any contract, AccountStore accountStore, WitnessStore witnessStore, ProposalStore proposalStore, DynamicPropertiesStore dynamicPropertiesStore) {
    this.contract = contract;
    this.accountStore = accountStore;
    this.dynamicStore = dynamicPropertiesStore;
    this.proposalStore = proposalStore;
    this.witnessStore = witnessStore;
  }

  AbstractActuator(Any contract, AccountStore accountStore, ProposalStore proposalStore, DynamicPropertiesStore dynamicPropertiesStore) {
    this.contract = contract;
    this.accountStore = accountStore;
    this.dynamicStore = dynamicPropertiesStore;
    this.proposalStore = proposalStore;
  }

  AbstractActuator(Any contract, AccountStore accountStore, ProposalStore proposalStore, WitnessStore witnessStore,
      DynamicPropertiesStore dynamicPropertiesStore, ForkUtils forkUtils) {
    this.contract = contract;
    this.accountStore = accountStore;
    this.dynamicStore = dynamicPropertiesStore;
    this.proposalStore = proposalStore;
    this.witnessStore = witnessStore;
    this.forkUtils = forkUtils;
  }
}
