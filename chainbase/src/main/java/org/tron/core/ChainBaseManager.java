package org.tron.core;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.zksnark.MerkleContainer;
import org.tron.core.db.BlockIndexStore;
import org.tron.core.db.BlockStore;
import org.tron.core.db.DelegationService;
import org.tron.core.db.KhaosDatabase;
import org.tron.core.store.AccountIdIndexStore;
import org.tron.core.store.AccountIndexStore;
import org.tron.core.store.AccountStore;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.AssetIssueV2Store;
import org.tron.core.store.CodeStore;
import org.tron.core.store.ContractStore;
import org.tron.core.store.DelegatedResourceAccountIndexStore;
import org.tron.core.store.DelegatedResourceStore;
import org.tron.core.store.DelegationStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.ExchangeStore;
import org.tron.core.store.ExchangeV2Store;
import org.tron.core.store.IncrementalMerkleTreeStore;
import org.tron.core.store.NullifierStore;
import org.tron.core.store.ProposalStore;
import org.tron.core.store.StorageRowStore;
import org.tron.core.store.VotesStore;
import org.tron.core.store.WitnessScheduleStore;
import org.tron.core.store.WitnessStore;
import org.tron.core.store.ZKProofStore;

@Component
public class ChainBaseManager {

  // db store
  @Autowired
  @Getter
  private AccountStore accountStore;
  @Autowired
  @Getter
  private BlockStore blockStore;
  @Autowired
  @Getter
  private WitnessStore witnessStore;
  @Autowired
  @Getter
  private AssetIssueStore assetIssueStore;
  @Autowired
  @Getter
  private AssetIssueV2Store assetIssueV2Store;
  @Autowired
  @Getter
  private DynamicPropertiesStore dynamicPropertiesStore;
  @Autowired
  @Getter
  private BlockIndexStore blockIndexStore;
  @Autowired
  @Getter
  private AccountIdIndexStore accountIdIndexStore;
  @Autowired
  @Getter
  private AccountIndexStore accountIndexStore;
  @Autowired
  @Getter
  private WitnessScheduleStore witnessScheduleStore;
  @Autowired
  @Getter
  private VotesStore votesStore;
  @Autowired
  @Getter
  private ProposalStore proposalStore;
  @Autowired
  @Getter
  private ExchangeStore exchangeStore;
  @Autowired
  @Getter
  private ExchangeV2Store exchangeV2Store;
  @Autowired
  @Getter
  private CodeStore codeStore;
  @Autowired
  @Getter
  private ContractStore contractStore;
  @Autowired
  @Getter
  private DelegatedResourceStore delegatedResourceStore;
  @Autowired
  @Getter
  private DelegatedResourceAccountIndexStore delegatedResourceAccountIndexStore;
  @Autowired
  @Getter
  private StorageRowStore storageRowStore;
  @Autowired
  @Getter
  private NullifierStore nullifierStore;
  @Autowired
  @Getter
  private ZKProofStore proofStore;

  @Autowired
  @Getter
  private IncrementalMerkleTreeStore merkleTreeStore;

  @Getter
  @Setter
  private MerkleContainer merkleContainer;

  @Getter
  @Setter
  private DelegationService delegationService;

  @Autowired
  @Getter
  private DelegationStore delegationStore;

  @Autowired
  @Getter
  private KhaosDatabase khaosDb;

}
