package org.tron.core;

import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

import com.google.protobuf.ByteString;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.storage.metric.DbStatService;
import org.tron.common.utils.ForkController;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.zksnark.MerkleContainer;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.utils.AssetUtil;
import org.tron.core.capsule.utils.BlockUtil;
import org.tron.core.db.BlockIndexStore;
import org.tron.core.db.BlockStore;
import org.tron.core.db.CommonDataBase;
import org.tron.core.db.CommonStore;
import org.tron.core.db.KhaosDatabase;
import org.tron.core.db.PbftSignDataStore;
import org.tron.core.db.RecentBlockStore;
import org.tron.core.db.RecentTransactionStore;
import org.tron.core.db.TransactionStore;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.HeaderNotFound;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.service.MortgageService;
import org.tron.core.store.AbiStore;
import org.tron.core.store.AccountAssetStore;
import org.tron.core.store.AccountIdIndexStore;
import org.tron.core.store.AccountIndexStore;
import org.tron.core.store.AccountStore;
import org.tron.core.store.AccountTraceStore;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.AssetIssueV2Store;
import org.tron.core.store.BalanceTraceStore;
import org.tron.core.store.CodeStore;
import org.tron.core.store.ContractStateStore;
import org.tron.core.store.ContractStore;
import org.tron.core.store.DelegatedResourceAccountIndexStore;
import org.tron.core.store.DelegatedResourceStore;
import org.tron.core.store.DelegationStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.ExchangeStore;
import org.tron.core.store.ExchangeV2Store;
import org.tron.core.store.IncrementalMerkleTreeStore;
import org.tron.core.store.MarketAccountStore;
import org.tron.core.store.MarketOrderStore;
import org.tron.core.store.MarketPairPriceToOrderStore;
import org.tron.core.store.MarketPairToPriceStore;
import org.tron.core.store.NullifierStore;
import org.tron.core.store.ProposalStore;
import org.tron.core.store.SectionBloomStore;
import org.tron.core.store.StorageRowStore;
import org.tron.core.store.TransactionHistoryStore;
import org.tron.core.store.TransactionRetStore;
import org.tron.core.store.TreeBlockIndexStore;
import org.tron.core.store.VotesStore;
import org.tron.core.store.WitnessScheduleStore;
import org.tron.core.store.WitnessStore;
import org.tron.core.store.ZKProofStore;

@Slf4j(topic = "DB")
@Component
public class ChainBaseManager {

  @Getter
  private static volatile ChainBaseManager chainBaseManager;

  // db store
  @Autowired
  @Getter
  private AccountStore accountStore;
  @Autowired
  @Getter
  private AccountAssetStore accountAssetStore;
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
  private MarketAccountStore marketAccountStore;
  @Autowired
  @Getter
  private MarketOrderStore marketOrderStore;
  @Autowired
  @Getter
  private MarketPairPriceToOrderStore marketPairPriceToOrderStore;
  @Autowired
  @Getter
  private MarketPairToPriceStore marketPairToPriceStore;
  @Autowired
  @Getter
  private AbiStore abiStore;
  @Autowired
  @Getter
  private CodeStore codeStore;
  @Autowired
  @Getter
  private ContractStore contractStore;
  @Autowired
  @Getter
  private ContractStateStore contractStateStore;
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
  private MortgageService mortgageService;

  @Autowired
  @Getter
  private DelegationStore delegationStore;

  @Autowired
  @Getter
  private KhaosDatabase khaosDb;

  @Autowired
  @Getter
  private CommonStore commonStore;

  @Autowired
  @Getter
  private TransactionStore transactionStore;
  @Autowired
  @Getter
  private TransactionRetStore transactionRetStore;
  @Autowired
  @Getter
  private RecentBlockStore recentBlockStore;
  @Autowired
  @Getter
  private RecentTransactionStore recentTransactionStore;
  @Autowired
  @Getter
  private TransactionHistoryStore transactionHistoryStore;

  @Getter
  @Setter
  private BlockCapsule genesisBlock;

  @Autowired
  @Getter
  private CommonDataBase commonDataBase;

  @Autowired
  @Getter
  private PbftSignDataStore pbftSignDataStore;

  @Autowired
  @Getter
  private BalanceTraceStore balanceTraceStore;

  @Autowired
  @Getter
  private AccountTraceStore accountTraceStore;

  @Getter
  private ForkController forkController = ForkController.instance();

  @Autowired
  @Getter
  @Setter
  private TreeBlockIndexStore merkleTreeIndexStore;

  @Autowired
  @Getter
  private SectionBloomStore sectionBloomStore;

  @Autowired
  private DbStatService dbStatService;

  @Getter
  @Setter
  private NodeType nodeType;

  @Getter
  @Setter
  private long lowestBlockNum = -1; // except num = 0.

  @Getter
  @Setter
  private long latestSaveBlockTime;

  // for test only
  public List<ByteString> getWitnesses() {
    return witnessScheduleStore.getActiveWitnesses();
  }

  // for test only
  public void addWitness(final ByteString address) {
    List<ByteString> witnessAddresses =
        witnessScheduleStore.getActiveWitnesses();
    witnessAddresses.add(address);
    getWitnessScheduleStore().saveActiveWitnesses(witnessAddresses);
  }

  public BlockCapsule getHead() throws HeaderNotFound {
    List<BlockCapsule> blocks = getBlockStore().getBlockByLatestNum(1);
    if (CollectionUtils.isNotEmpty(blocks)) {
      return blocks.get(0);
    } else {
      throw new HeaderNotFound("header block not found");
    }
  }

  public synchronized BlockId getHeadBlockId() {
    return new BlockId(dynamicPropertiesStore.getLatestBlockHeaderHash());
  }

  public long getHeadBlockNum() {
    return dynamicPropertiesStore.getLatestBlockHeaderNumber();
  }

  public long getHeadBlockTimeStamp() {
    return dynamicPropertiesStore.getLatestBlockHeaderTimestamp();
  }

  public void initGenesis() {
    genesisBlock = BlockUtil.newGenesisBlockCapsule();
  }

  public long getHeadSlot() {
    return (getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() - getGenesisBlock()
        .getTimeStamp()) / BLOCK_PRODUCED_INTERVAL;
  }


  /**
   * judge id.
   *
   * @param blockHash blockHash
   */
  public boolean containBlock(final Sha256Hash blockHash) {
    try {
      return this.khaosDb.containBlockInMiniStore(blockHash)
          || getBlockStore()
          .get(blockHash.getBytes()) != null;
    } catch (ItemNotFoundException | BadItemException e) {
      return false;
    }
  }

  public boolean containBlockInMainChain(BlockId blockId) {
    try {
      return getBlockStore().get(blockId.getBytes()) != null;
    } catch (ItemNotFoundException | BadItemException e) {
      return false;
    }
  }

  public BlockCapsule getKhaosDbHead(){
    return this.khaosDb.getHead();
  }

  /**
   * Get a BlockCapsule by id.
   */
  public BlockCapsule getBlockById(final Sha256Hash hash)
      throws BadItemException, ItemNotFoundException {
    BlockCapsule block = this.khaosDb.getBlock(hash);
    if (block == null) {
      block = getBlockStore().get(hash.getBytes());
    }
    return block;
  }

  /**
   * judge has blocks.
   */
  public boolean hasBlocks() {
    return getBlockStore().isNotEmpty() || this.khaosDb.hasData();
  }

  public void setBlockReference(TransactionCapsule trans) {
    byte[] headHash = getDynamicPropertiesStore().getLatestBlockHeaderHash().getBytes();
    long headNum = getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    trans.setReference(headNum, headHash);
  }

  public BlockId getSolidBlockId() {
    try {
      long num = getDynamicPropertiesStore().getLatestSolidifiedBlockNum();
      return getBlockIdByNum(num);
    } catch (Exception e) {
      return getGenesisBlockId();
    }
  }

  public BlockId getGenesisBlockId() {
    return getGenesisBlock().getBlockId();
  }


  /**
   * Get the block id from the number.
   */
  public BlockId getBlockIdByNum(final long num) throws ItemNotFoundException {
    return getBlockIndexStore().get(num);
  }

  public BlockCapsule getBlockByNum(final long num) throws
      ItemNotFoundException, BadItemException {
    return getBlockById(getBlockIdByNum(num));
  }

  public static ChainBaseManager getInstance() {
    return chainBaseManager;
  }

  public static synchronized void init(ChainBaseManager manager) {
    chainBaseManager = manager;
    AssetUtil.setAccountAssetStore(manager.getAccountAssetStore());
    AssetUtil.setDynamicPropertiesStore(manager.getDynamicPropertiesStore());
  }

  @PostConstruct
  private void init() {
    this.lowestBlockNum = this.blockIndexStore.getLimitNumber(1, 1).stream()
            .map(BlockId::getNum).findFirst().orElse(0L);
    this.nodeType = getLowestBlockNum() > 1 ? NodeType.LITE : NodeType.FULL;
    this.latestSaveBlockTime = System.currentTimeMillis();
  }

  public void shutdown() {
    dbStatService.shutdown();
  }

  public boolean isLiteNode() {
    return getNodeType() == NodeType.LITE;
  }

  public enum  NodeType  {
    FULL(0),
    LITE(1);

    @Getter
    private final int type;

    NodeType(int type) {
      this.type = type;
    }
  }
}

