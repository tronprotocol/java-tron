package org.tron.core.store;

import java.util.HashMap;
import java.util.Map;
import org.tron.common.zksnark.MerkleContainer;
import org.tron.core.ChainBaseManager;
import org.tron.core.db.BlockIndexStore;
import org.tron.core.db.BlockStore;
import org.tron.core.db.DelegationService;
import org.tron.core.db.KhaosDatabase;
import org.tron.core.exception.TypeMismatchNamingException;

public class StoreFactory {

  private static final StoreFactory INSTANCE = new StoreFactory();

  private Map<String, Object> stores;

  private ChainBaseManager chainBaseManager;

  public StoreFactory setDelegationService(DelegationService delegationService) {
    add(delegationService);
    return this;
  }


  public StoreFactory() {
    stores = new HashMap<>();
  }

  private StoreFactory(Map<String, Object> stores) {
    this.stores = stores;
  }

  public void add(Object store) {
    String name = store.getClass().getSimpleName();
    stores.put(name, store);
  }

  public <T> T getStore(Class<T> clz) throws TypeMismatchNamingException {
    String name = clz.getSimpleName();
    return getStore(name, clz);
  }

  public <T> T getStore(String name, Class<T> clz) throws TypeMismatchNamingException {
    @SuppressWarnings("unchecked")
    T t = (T) stores.get(name);
    if (clz != null && !clz.isInstance(t)) {
      throw new TypeMismatchNamingException(
          name, clz, (t != null ? t.getClass() : null));
    }
    return t;
  }

  public static StoreFactory getInstance() {
    return INSTANCE;
  }


  public StoreFactory setAccountStore(AccountStore accountStore) {
    add(accountStore);
    return this;
  }

  public StoreFactory setAccountIdIndexStore(AccountIdIndexStore accountIdIndexStore) {
    add(accountIdIndexStore);
    return this;
  }

  public StoreFactory setAccountIndexStore(AccountIndexStore accountIndexStore) {
    add(accountIndexStore);
    return this;
  }

  public StoreFactory setDynamicPropertiesStore(DynamicPropertiesStore dynamicPropertiesStore) {
    add(dynamicPropertiesStore);
    return this;

  }

  public StoreFactory setAssetIssueStore(AssetIssueStore assetIssueStore) {
    add(assetIssueStore);
    return this;
  }

  public StoreFactory setContractStore(ContractStore contractStore) {
    add(contractStore);
    return this;
  }

  public StoreFactory setAssetIssueV2Store(AssetIssueV2Store assetIssueV2Store) {
    add(assetIssueV2Store);
    return this;
  }

  public StoreFactory setWitnessStore(WitnessStore witnessStore) {
    add(witnessStore);
    return this;
  }

  public StoreFactory setVotesStore(VotesStore votesStore) {
    add(votesStore);
    return this;
  }

  public StoreFactory setProofStore(ZKProofStore proofStore) {
    add(proofStore);
    return this;
  }

  public StoreFactory setNullifierStore(NullifierStore nullifierStore) {
    add(nullifierStore);
    return this;
  }

  public StoreFactory setDelegatedResourceStore(DelegatedResourceStore delegatedResourceStore) {
    add(delegatedResourceStore);
    return this;
  }

  public StoreFactory setDelegatedResourceAccountIndexStore(
      DelegatedResourceAccountIndexStore delegatedResourceAccountIndexStore) {
    add(delegatedResourceAccountIndexStore);
    return this;
  }

  public StoreFactory setExchangeStore(ExchangeStore exchangeStore) {
    add(exchangeStore);
    return this;
  }

  public StoreFactory setExchangeV2Store(ExchangeV2Store exchangeV2Store) {
    add(exchangeV2Store);
    return this;
  }

  public StoreFactory setProposalStore(ProposalStore proposalStore) {
    add(proposalStore);
    return this;
  }

  public StoreFactory setCodeStore(CodeStore codeStore) {
    add(codeStore);
    return this;

  }

  public StoreFactory setStorageRowStore(StorageRowStore storageRowStore) {
    add(storageRowStore);
    return this;
  }

  public StoreFactory setBlockStore(BlockStore blockStore) {
    add(blockStore);
    return this;
  }

  public StoreFactory setKhaosDb(KhaosDatabase khaosDb) {
    add(khaosDb);
    return this;
  }

  public StoreFactory setBlockIndexStore(BlockIndexStore blockIndexStore) {
    add(blockIndexStore);
    return this;
  }

  public StoreFactory setMerkleContainer(MerkleContainer merkleContainer) {
    add(merkleContainer);
    return this;
  }

  public ChainBaseManager getChainBaseManager() {
    return chainBaseManager;
  }

  public StoreFactory setChainBaseManager(ChainBaseManager chainBaseManager) {
    this.chainBaseManager = chainBaseManager;
    return this;
  }
}
