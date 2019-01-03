package org.tron.core.db.fast.callback;

import static org.tron.common.crypto.Hash.EMPTY_TRIE_HASH;
import static org.tron.core.db.fast.FastSyncStoreConstant.ACCOUNT_ID_INDEX_STORE_KEY;
import static org.tron.core.db.fast.FastSyncStoreConstant.ACCOUNT_INDEX_STORE_KEY;
import static org.tron.core.db.fast.FastSyncStoreConstant.ASSET_ISSUE_STORE_KEY;
import static org.tron.core.db.fast.FastSyncStoreConstant.ASSET_ISSUE_V_2_STORE_KEY;
import static org.tron.core.db.fast.FastSyncStoreConstant.CONTRACT_STORE_KEY;
import static org.tron.core.db.fast.FastSyncStoreConstant.DELEGATED_RESOURCE_ACCOUNT_STORE_KEY;
import static org.tron.core.db.fast.FastSyncStoreConstant.DELEGATED_RESOURCE_STORE_KEY;
import static org.tron.core.db.fast.FastSyncStoreConstant.DYNAMIC_PROPERTIES_STORE_KEY;
import static org.tron.core.db.fast.FastSyncStoreConstant.EXCHANGE_STORE_KEY;
import static org.tron.core.db.fast.FastSyncStoreConstant.EXCHANGE_V_2_STORE_KEY;
import static org.tron.core.db.fast.FastSyncStoreConstant.PROPOSAL_STORE_KEY;
import static org.tron.core.db.fast.FastSyncStoreConstant.STORAGE_STORE_KEY;
import static org.tron.core.db.fast.FastSyncStoreConstant.VOTES_STORE_KEY;
import static org.tron.core.db.fast.FastSyncStoreConstant.WITNESS_STORE_KEY;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.ByteString;
import com.google.protobuf.Internal;
import java.util.Arrays;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.utils.FastByteComparisons;
import org.tron.core.capsule.utils.RLP;
import org.tron.core.db.Manager;
import org.tron.core.db.fast.FastSyncStoreConstant.TrieEnum;
import org.tron.core.db.fast.storetrie.AccountIdIndexStoreTrie;
import org.tron.core.db.fast.storetrie.AccountIndexStoreTrie;
import org.tron.core.db.fast.storetrie.AccountStateStoreTrie;
import org.tron.core.db.fast.storetrie.AssetIssueStoreTrie;
import org.tron.core.db.fast.storetrie.AssetIssueV2StoreTrie;
import org.tron.core.db.fast.storetrie.ContractStoreTrie;
import org.tron.core.db.fast.storetrie.DelegatedResourceAccountStoreTrie;
import org.tron.core.db.fast.storetrie.DelegatedResourceStoreTrie;
import org.tron.core.db.fast.storetrie.DynamicPropertiesStoreTrie;
import org.tron.core.db.fast.storetrie.ExchangeStoreTrie;
import org.tron.core.db.fast.storetrie.ExchangeV2StoreTrie;
import org.tron.core.db.fast.storetrie.ProposalStoreTrie;
import org.tron.core.db.fast.storetrie.StorageRowStoreTrie;
import org.tron.core.db.fast.storetrie.VotesStoreTrie;
import org.tron.core.db.fast.storetrie.WitnessStoreTrie;
import org.tron.core.exception.BadBlockException;
import org.tron.core.trie.TrieImpl;

@Slf4j
@Component
public class FastSyncCallBack {

  public static final Cache<String, byte[]> rootHashCache = CacheBuilder.newBuilder()
      .initialCapacity(100).maximumSize(100).build();

  private BlockCapsule blockCapsule;
  private boolean execute = false;
  private TrieImpl trie;

  private TrieImpl dynamicTrieImpl;
  private TrieImpl assetIssueTrieImpl;
  private TrieImpl assetIssueV2TrieImpl;
  private TrieImpl exchangeTrieImpl;
  private TrieImpl exchangeV2TrieImpl;
  private TrieImpl delegatedResourceTrieImpl;
  private TrieImpl contractTrieImpl;
  private TrieImpl delegatedResourceAccountTrieImpl;
  private TrieImpl witnessTrieImpl;
  private TrieImpl proposalTrieImpl;
  private TrieImpl accountIdIndexTrieImpl;
  private TrieImpl votesTrieImpl;
  private TrieImpl accountIndexTrieImpl;
  private TrieImpl storageTrieImpl;

  @Setter
  private Manager manager;

  @Autowired
  private AccountStateStoreTrie db;
  @Autowired
  private DynamicPropertiesStoreTrie dynamicStoreTrie;
  @Autowired
  private AssetIssueStoreTrie assetIssueStoreTrie;
  @Autowired
  private AssetIssueV2StoreTrie assetIssueV2StoreTrie;
  @Autowired
  private ExchangeStoreTrie exchangeStoreTrie;
  @Autowired
  private ExchangeV2StoreTrie exchangeV2StoreTrie;
  @Autowired
  private DelegatedResourceStoreTrie delegatedResourceStoreTrie;
  @Autowired
  private ContractStoreTrie contractStoreTrie;
  @Autowired
  private DelegatedResourceAccountStoreTrie delegatedResourceAccountStoreTrie;
  @Autowired
  private WitnessStoreTrie witnessStoreTrie;
  @Autowired
  private ProposalStoreTrie proposalStoreTrie;
  @Autowired
  private AccountIdIndexStoreTrie accountIdIndexStoreTrie;
  @Autowired
  private VotesStoreTrie votesStoreTrie;
  @Autowired
  private AccountIndexStoreTrie accountIndexStoreTrie;
  @Autowired
  private StorageRowStoreTrie storageRowStoreTrie;

  public void accountCallBack(byte[] key, AccountCapsule item) {
    if (!exe()) {
      return;
    }
    if (item == null || ArrayUtils.isEmpty(item.getData())) {
      return;
    }
    trie.put(RLP.encodeElement(key), item.getData());
  }

  public void deleteAccount(byte[] key) {
    if (!exe()) {
      return;
    }
    trie.delete(RLP.encodeElement(key));
  }

  public void callBack(byte[] key, byte[] value, TrieEnum trieEnum) {
    if (!exe()) {
      return;
    }
    TrieImpl trieImpl = selectTrie(trieEnum);
    if (trieImpl != null) {
      if (ArrayUtils.isEmpty(value)) {
        return;
      }
      trieImpl.put(RLP.encodeElement(key), value);
    }
  }

  public void delete(byte[] key, TrieEnum trieEnum) {
    if (!exe()) {
      return;
    }
    TrieImpl trieImpl = selectTrie(trieEnum);
    if (trieImpl != null) {
      trieImpl.delete(RLP.encodeElement(key));
    }
  }

  public void preExecute(BlockCapsule blockCapsule) {
    this.blockCapsule = blockCapsule;
    this.execute = true;
    if (!exe()) {
      return;
    }
    byte[] rootHash = null;
    try {
      BlockCapsule parentBlockCapsule = manager.getBlockById(blockCapsule.getParentBlockId());
      rootHash = parentBlockCapsule.getInstance().getBlockHeader().getRawData()
          .getAccountStateRoot().toByteArray();
      rootHash = rootHashCache.getIfPresent(parentBlockCapsule.getBlockId().toString());
    } catch (Exception e) {
      logger.error("", e);
    }
    if (Arrays.equals(Internal.EMPTY_BYTE_ARRAY, rootHash)) {
      rootHash = Hash.EMPTY_TRIE_HASH;
    }
    trie = new TrieImpl(db, rootHash);
    //init other trie
    initTrie(rootHash);
  }

  private void initTrie(byte[] rootHash) {
    byte[] dynamicRootHash = null;
    byte[] assetIssueRootHash = null;
    byte[] assetIssueV2RootHash = null;
    byte[] exchangeRootHash = null;
    byte[] exchangeV2RootHash = null;
    byte[] delegatedResourceRootHash = null;
    byte[] contractRootHash = null;
    byte[] delegatedResourceAccountRootHash = null;
    byte[] witnessRootHash = null;
    byte[] proposalRootHash = null;
    byte[] accountIdIndexRootHash = null;
    byte[] votesRootHash = null;
    byte[] accountIndexRootHash = null;
    byte[] storageRootHash = null;

    if (ArrayUtils.isNotEmpty(rootHash)) {
      dynamicRootHash = trie.get(RLP.encodeString(DYNAMIC_PROPERTIES_STORE_KEY));
      assetIssueRootHash = trie.get(RLP.encodeString(ASSET_ISSUE_STORE_KEY));
      assetIssueV2RootHash = trie.get(RLP.encodeString(ASSET_ISSUE_V_2_STORE_KEY));
      exchangeRootHash = trie.get(RLP.encodeString(EXCHANGE_STORE_KEY));
      exchangeV2RootHash = trie.get(RLP.encodeString(EXCHANGE_V_2_STORE_KEY));
      delegatedResourceRootHash = trie.get(RLP.encodeString(DELEGATED_RESOURCE_STORE_KEY));
      contractRootHash = trie.get(RLP.encodeString(CONTRACT_STORE_KEY));
      delegatedResourceAccountRootHash = trie
          .get(RLP.encodeString(DELEGATED_RESOURCE_ACCOUNT_STORE_KEY));
      witnessRootHash = trie.get(RLP.encodeString(WITNESS_STORE_KEY));
      proposalRootHash = trie.get(RLP.encodeString(PROPOSAL_STORE_KEY));
      accountIdIndexRootHash = trie.get(RLP.encodeString(ACCOUNT_ID_INDEX_STORE_KEY));
      votesRootHash = trie.get(RLP.encodeString(VOTES_STORE_KEY));
      accountIndexRootHash = trie.get(RLP.encodeString(ACCOUNT_INDEX_STORE_KEY));
      storageRootHash = trie.get(RLP.encodeString(STORAGE_STORE_KEY));
    }
    dynamicTrieImpl = new TrieImpl(dynamicStoreTrie, dynamicRootHash);
    assetIssueTrieImpl = new TrieImpl(assetIssueStoreTrie, assetIssueRootHash);
    assetIssueV2TrieImpl = new TrieImpl(assetIssueV2StoreTrie, assetIssueV2RootHash);
    exchangeTrieImpl = new TrieImpl(exchangeStoreTrie, exchangeRootHash);
    exchangeV2TrieImpl = new TrieImpl(exchangeV2StoreTrie, exchangeV2RootHash);
    delegatedResourceTrieImpl = new TrieImpl(delegatedResourceStoreTrie, delegatedResourceRootHash);
    contractTrieImpl = new TrieImpl(contractStoreTrie, contractRootHash);
    delegatedResourceAccountTrieImpl = new TrieImpl(delegatedResourceAccountStoreTrie,
        delegatedResourceAccountRootHash);
    witnessTrieImpl = new TrieImpl(witnessStoreTrie, witnessRootHash);
    proposalTrieImpl = new TrieImpl(proposalStoreTrie, proposalRootHash);
    accountIdIndexTrieImpl = new TrieImpl(accountIdIndexStoreTrie, accountIdIndexRootHash);
    votesTrieImpl = new TrieImpl(votesStoreTrie, votesRootHash);
    accountIndexTrieImpl = new TrieImpl(accountIndexStoreTrie, accountIndexRootHash);
    storageTrieImpl = new TrieImpl(storageRowStoreTrie, storageRootHash);
  }

  private TrieImpl selectTrie(TrieEnum trieEnum) {
    switch (trieEnum) {
      case DYNAMIC:
        return dynamicTrieImpl;
      case ASSET:
        return assetIssueTrieImpl;
      case ASSET2:
        return assetIssueV2TrieImpl;
      case VOTES:
        return votesTrieImpl;
      case WITNESS:
        return witnessTrieImpl;
      case CONTRACT:
        return contractTrieImpl;
      case EXCHANGE:
        return exchangeTrieImpl;
      case EXCHANGE2:
        return exchangeV2TrieImpl;
      case PROPOSAL:
        return proposalTrieImpl;
      case ACCOUNT_INDEX:
        return accountIndexTrieImpl;
      case ACCOUNT_ID_INDEX:
        return accountIdIndexTrieImpl;
      case DELEGATED_RESOURCE:
        return delegatedResourceTrieImpl;
      case DELEGATED_RESOURCE_ACCOUNT_INDEX:
        return delegatedResourceAccountTrieImpl;
      case STORAGE:
        return storageTrieImpl;
    }
    return null;
  }

  private void setStoreKeyAndHash() {
    for (TrieEnum trieEnum : TrieEnum.values()) {
      TrieImpl childTrie = selectTrie(trieEnum);
      if (childTrie == null || ArrayUtils.isEmpty(childTrie.getRootHash())
          || FastByteComparisons.equal(childTrie.getRootHash(), EMPTY_TRIE_HASH)) {
        continue;
      }
      trie.put(RLP.encodeString(trieEnum.getKey()), childTrie.getRootHash());
    }
  }

  public void executePushFinish() throws BadBlockException {
    if (!exe()) {
      return;
    }
    ByteString oldRoot = blockCapsule.getInstance().getBlockHeader().getRawData()
        .getAccountStateRoot();
    execute = false;
    //
    setStoreKeyAndHash();
    //
    byte[] newRoot = trie.getRootHash();
    if (ArrayUtils.isEmpty(newRoot)) {
      newRoot = Hash.EMPTY_TRIE_HASH;
    }
    if (oldRoot.isEmpty()) {
//      blockCapsule.setAccountStateRoot(newRoot);
    } else if (!Arrays.equals(oldRoot.toByteArray(), newRoot)) {
      logger.error("The accountStateRoot hash is not validated. {}, oldRoot: {}, newRoot: {}",
          blockCapsule.getBlockId().getString(), ByteUtil.toHexString(oldRoot.toByteArray()),
          ByteUtil.toHexString(newRoot));
      throw new BadBlockException("The accountStateRoot hash is not validated");
    }
    setRootHashCache(newRoot);
  }

  public void executeGenerateFinish() {
    if (!exe()) {
      return;
    }
    //
    setStoreKeyAndHash();
    //
    byte[] newRoot = trie.getRootHash();
    if (ArrayUtils.isEmpty(newRoot)) {
      newRoot = Hash.EMPTY_TRIE_HASH;
    }
    blockCapsule.setAccountStateRoot(newRoot);
    execute = false;
    setRootHashCache(newRoot);
  }

  public void exceptionFinish() {
    execute = false;
  }

  private boolean exe() {
    if (!execute || blockCapsule.getNum() < 1) {
      //Agreement same block high to generate account state root
      return false;
    }
    return true;
  }

  private void setRootHashCache(byte[] rootHash) {
    rootHashCache.put(blockCapsule.getBlockId().toString(), rootHash);
  }
}
