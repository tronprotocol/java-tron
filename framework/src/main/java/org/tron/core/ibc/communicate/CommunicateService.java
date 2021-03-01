package org.tron.core.ibc.communicate;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.common.overlay.server.SyncPool;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.BlockHeaderCapsule;
import org.tron.core.capsule.PbftSignCapsule;
import org.tron.core.capsule.utils.MerkleTree;
import org.tron.core.capsule.utils.MerkleTree.ProofLeaf;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.PbftBaseImpl;
import org.tron.core.db.BlockHeaderIndexStore;
import org.tron.core.db.BlockHeaderStore;
import org.tron.core.db.BlockIndexStore;
import org.tron.core.db.BlockStore;
import org.tron.core.db.Manager;
import org.tron.core.db.PbftSignDataStore;
import org.tron.core.db.TransactionStore;
import org.tron.core.exception.BadItemException;
import org.tron.core.ibc.common.CrossUtils;
import org.tron.core.ibc.connect.CrossChainConnectPool;
import org.tron.core.net.message.CrossChainMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.CrossMessage;
import org.tron.protos.Protocol.CrossMessage.Type;
import org.tron.protos.Protocol.Proof;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

@Slf4j(topic = "Communicate")
@Service
public class CommunicateService implements Communicate {

  private Cache<Sha256Hash, CrossMessage> receiveCrossMsgCache = CacheBuilder.newBuilder()
      .initialCapacity(1000).maximumSize(10000).expireAfterWrite(5, TimeUnit.MINUTES).build();
  private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Autowired
  private SyncPool syncPool;

  @Autowired
  private Manager manager;

  @Autowired
  private CrossChainConnectPool crossChainConnectPool;

  @Autowired
  private PbftBaseImpl pbftBaseImpl;

  @Autowired
  private BlockHeaderStore blockHeaderStore;

  @Autowired
  private BlockHeaderIndexStore blockHeaderIndexStore;

  @PreDestroy
  public void destroy() {
    executorService.shutdown();
  }

  public void setPbftBlockListener(PbftBlockListener pbftBlockListener) {
    manager.setPbftBlockListener(pbftBlockListener);
    manager.setCommunicateService(this);
    executorService
        .scheduleWithFixedDelay(() -> receiveCrossMsgCache.asMap().forEach((hash, crossMessage) -> {
          try {
            if (validProof(crossMessage)) {
              broadcastCrossMessage(crossMessage);
              receiveCrossMsgCache.invalidate(hash);
            } else {
              logger.warn("valid proof fail!");
            }
          } catch (Exception e) {
            logger.error("", e);
          }
        }), 1, 1, TimeUnit.SECONDS);
  }

  @Override
  public void sendCrossMessage(CrossMessage crossMessage, boolean save) {
    Sha256Hash txId = getTxId(crossMessage);
    if (checkCommit(txId)) {
      if (save) {
        chainBaseManager.getCrossStore().saveSendCrossMsg(txId, crossMessage);
      }
      try {
        //generate proof path
        BlockStore blockStore = chainBaseManager.getBlockStore();
        BlockIndexStore blockIndexStore = chainBaseManager.getBlockIndexStore();
        TransactionStore transactionStore = chainBaseManager.getTransactionStore();
        long blockNum = transactionStore.get(txId.getBytes()).getBlockNum();
        BlockCapsule blockCapsule = blockStore.get(blockIndexStore.get(blockNum).getBytes());
        List<Sha256Hash> hashList;
        if (blockCapsule.getInstance().getCrossMessageList().isEmpty()) {
          hashList = blockCapsule.getInstance().getTransactionsList().stream()
              .map(transaction -> Sha256Hash.of(true, transaction.toByteArray()))
              .collect(Collectors.toList());
        } else {
          hashList = blockCapsule.getInstance().getCrossMessageList().stream()
              .map(crossMsg -> Sha256Hash.of(true, crossMsg.getTransaction().toByteArray()))
              .collect(Collectors.toList());
        }
        List<ProofLeaf> proofLeafList = MerkleTree.getInstance()
            .generateProofPath(hashList, getTxMerkleHash(crossMessage));
        List<Proof> proofList = proofLeafList.stream().map(proofLeaf -> {
          Proof.Builder builder = Proof.newBuilder();
          return builder.setHash(proofLeaf.getHash().getByteString())
              .setLeftOrRight(proofLeaf.isLeftOrRight()).build();
        }).collect(Collectors.toList());
        //set the proof and root height
        crossMessage = crossMessage.toBuilder().addAllProof(proofList)
            .setRootHeight(blockNum).build();
        //send data
        sendData(crossMessage);
      } catch (Exception e) {
        //wait the time out or auto rollback
        //if wait the time out, nothing to do
        logger.error("send cross message fail! txId: {}", txId, e);
      }
    }
  }

  @Override
  public void receiveCrossMessage(PeerConnection peer, CrossMessage crossMessage) {
    Sha256Hash txId = getTxId(crossMessage);
    receiveCrossMsgCache.put(txId, crossMessage);
  }

  @Override
  public boolean validProof(CrossMessage crossMessage) {
    Contract contract = crossMessage.getTransaction().getRawData().getContract(0);
    if (contract.getType() != ContractType.CrossContract) {
      logger.error("{} ContractType error!", contract.getType());
      return false;
    }
    List<Proof> proofList = crossMessage.getProofList();
    Sha256Hash txHash = getTxMerkleHash(crossMessage);
    Sha256Hash root = getRoot(crossMessage);
    if (root == null) {
      logger.error("get the root is null");
      return false;
    }
    MerkleTree merkleTree = MerkleTree.getInstance();
    List<ProofLeaf> proofLeafList = proofList.stream().map(proof -> merkleTree.new ProofLeaf(
        Sha256Hash.of(true, proof.getHash().toByteArray()),
        proof.getLeftOrRight())).collect(Collectors.toList());
    logger.debug("root:{}, tx:{}", root, txHash);
    return merkleTree.validProof(root, proofLeafList, txHash);
  }

  /**
   * check the transaction whether in the solid block
   */
  @Override
  public boolean checkCommit(Sha256Hash hash) {
    TransactionStore transactionStore = chainBaseManager.getTransactionStore();
    PbftSignDataStore pbftSignDataStore = chainBaseManager.getPbftSignDataStore();
    try {
      long blockNum = transactionStore.get(hash.getBytes()).getBlockNum();
      PbftSignCapsule pbftSignCapsule = pbftSignDataStore.getBlockSignData(blockNum);
      return pbftSignCapsule != null;
    } catch (BadItemException e) {
      logger.error("{}", e.getMessage());
    }
    return false;
  }

  @Override
  public boolean broadcastCrossMessage(CrossMessage crossMessage) {
    logger.info("ready broadcast {} cross message", crossMessage.getType());
    syncPool.getActivePeers().stream().filter(peer -> !peer.isNeedSyncFromUs())
        .forEach(peer -> {
          peer.sendMessage(new CrossChainMessage(crossMessage));
          logger.info("to {} broadcast cross msg, txid is {}, type {}", peer, getTxId(crossMessage),
              crossMessage.getType());
        });
    return true;
  }

  @Override
  public boolean isSyncFinish() {
    return !pbftBaseImpl.isSyncing();
  }

  private Sha256Hash getTxId(CrossMessage crossMessage) {
    Sha256Hash txId;
    if (crossMessage.getType() == Type.ACK) {
      txId = Sha256Hash.wrap(crossMessage.getTransaction().getRawData().getSourceTxId());
    } else {
      txId = Sha256Hash.of(true, crossMessage.getTransaction().getRawData().toByteArray());
    }
    return txId;
  }

  private Sha256Hash getTxMerkleHash(CrossMessage crossMessage) {
    Sha256Hash txId;
    if (crossMessage.getType() == Type.ACK) {
      txId = CrossUtils.getSourceMerkleTxHash(crossMessage.getTransaction());
    } else {
      txId = Sha256Hash.of(true, crossMessage.getTransaction().toByteArray());
    }
    return txId;
  }

  /**
   * other chain block tx merkel root
   */
  private Sha256Hash getRoot(CrossMessage crossMessage) {
    ByteString fromChainId = crossMessage.getFromChainId();
    ByteString routeChainId = crossMessage.getRouteChainId();
    String chainId = null;
    if (routeChainId.isEmpty() || getLocalChainId().equals(routeChainId)) {
      //use fromChainId
      chainId = ByteArray.toHexString(fromChainId.toByteArray());
    } else {
      //use routeChainId
      chainId = ByteArray.toHexString(routeChainId.toByteArray());
    }
    BlockId blockId = blockHeaderIndexStore.getUnchecked(chainId, crossMessage.getRootHeight());
    if (blockId == null) {
      return null;
    }
    if (blockId.getNum() > chainBaseManager.getCommonDataBase().getLatestPBFTBlockNum(chainId)) {
      logger.warn("chain {} latest pbft height is {}, but block height is {} ", chainId,
          blockId.getNum(), chainBaseManager.getCommonDataBase().getLatestPBFTBlockNum(chainId));
      return null;
    }
    BlockHeaderCapsule blockHeaderCapsule = blockHeaderStore.getUnchecked(chainId, blockId);
    if (blockHeaderCapsule != null) {
      return blockHeaderCapsule.getCrossMerkleRoot().equals(Sha256Hash.ZERO_HASH)
          ? blockHeaderCapsule.getMerkleRoot() : blockHeaderCapsule.getCrossMerkleRoot();
    }
    return null;
  }

  /**
   *
   */
  public long getHeight(ByteString toChainId) {
    //use toChainId
    return chainBaseManager.getCommonDataBase()
        .getLatestHeaderBlockNum(ByteArray.toHexString(toChainId.toByteArray()));
  }

  /**
   * done: use genesisBlockId to chainId
   */
  public ByteString getLocalChainId() {
    return chainBaseManager.getGenesisBlockId().getByteString();
  }

  public ByteString getRouteChainId() {
    return Args.getInstance().getRouteChainId();
  }

  /**
   * done:
   */
  private void sendData(CrossMessage crossMessage) {
    ByteString toChainId = crossMessage.getToChainId();
    ByteString routeChainId = crossMessage.getRouteChainId();
    List<PeerConnection> peerConnectionList;
    if (!routeChainId.isEmpty() && !getLocalChainId().equals(routeChainId)) {
      peerConnectionList = crossChainConnectPool.getPeerConnect(routeChainId);
    } else {
      peerConnectionList = crossChainConnectPool.getPeerConnect(toChainId);
    }
    if (peerConnectionList != null) {
      peerConnectionList.forEach(peerConnection -> {
        peerConnection.sendMessage(new CrossChainMessage(crossMessage));
      });
    }
  }


}
