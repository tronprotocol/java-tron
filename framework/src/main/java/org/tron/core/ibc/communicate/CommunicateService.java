package org.tron.core.ibc.communicate;

import com.google.protobuf.ByteString;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.common.overlay.server.SyncPool;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.PbftSignCapsule;
import org.tron.core.capsule.utils.MerkleTree;
import org.tron.core.capsule.utils.MerkleTree.ProofLeaf;
import org.tron.core.consensus.PbftBaseImpl;
import org.tron.core.db.BlockIndexStore;
import org.tron.core.db.BlockStore;
import org.tron.core.db.Manager;
import org.tron.core.db.PbftSignDataStore;
import org.tron.core.db.TransactionStore;
import org.tron.core.exception.BadItemException;
import org.tron.core.ibc.connect.CrossChainConnectPool;
import org.tron.core.net.message.CrossChainMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.CrossMessage;
import org.tron.protos.Protocol.CrossMessage.Type;
import org.tron.protos.Protocol.Proof;
import org.tron.protos.Protocol.ReasonCode;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

@Slf4j(topic = "Communicate")
@Service
public class CommunicateService implements Communicate {

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
        List<Sha256Hash> hashList = blockCapsule.getInstance().getTransactionsList().stream()
            .map(transaction -> Sha256Hash.of(transaction.getRawData().toByteArray()))
            .collect(Collectors.toList());
        List<ProofLeaf> proofLeafList = MerkleTree.getInstance().generateProofPath(hashList, txId);
        List<Proof> proofList = proofLeafList.stream().map(proofLeaf -> {
          Proof.Builder builder = Proof.newBuilder();
          return builder.setHash(proofLeaf.getHash().getByteString())
              .setLeftOrRight(proofLeaf.isLeftOrRight()).build();
        }).collect(Collectors.toList());
        //set the proof and time out height
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
    if (validProof(crossMessage)) {
      broadcastCrossMessage(crossMessage);
    } else {
      //todo: create a new reason
      peer.disconnect(ReasonCode.BAD_PROTOCOL);
    }
  }

  @Override
  public boolean validProof(CrossMessage crossMessage) {
    Contract contract = crossMessage.getTransaction().getRawData().getContract(0);
    if (contract.getType() != ContractType.CrossContract) {
      return false;
    }
    List<Proof> proofList = crossMessage.getProofList();
    Sha256Hash txId = getTxId(crossMessage);
    Sha256Hash root = getRoot(crossMessage, crossMessage.getRootHeight());
    MerkleTree merkleTree = MerkleTree.getInstance();
    List<ProofLeaf> proofLeafList = proofList.stream().map(proof -> merkleTree.new ProofLeaf(
        Sha256Hash.of(proof.getHash().toByteArray()),
        proof.getLeftOrRight())).collect(Collectors.toList());
    return merkleTree.validProof(root, proofLeafList, txId);
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
    syncPool.getActivePeers().stream().filter(peerConnection -> peerConnection.isNeedSyncFromUs())
        .forEach(peerConnection -> peerConnection.sendMessage(new CrossChainMessage(crossMessage)));
    return false;
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
      txId = Sha256Hash.of(crossMessage.getTransaction().getRawData().toByteArray());
    }
    return txId;
  }

  /**
   * todo: other chain block tx merkel root
   */
  private Sha256Hash getRoot(CrossMessage crossMessage, long blockHeight) {
    ByteString fromChainId = crossMessage.getFromChainId();
    ByteString routeChainId = crossMessage.getRouteChainId();
    if (routeChainId.isEmpty() || getLocalChainId().equals(routeChainId)) {
      //use fromChainId
      return null;
    } else {
      //use routeChainId
      return null;
    }
  }

  /**
   * todo: other chain block tx merkel root
   */
  public long getHeight(ByteString toChainId) {
    //use toChainId
    return 0;
  }

  /**
   * done: use genesisBlockId to chainId
   */
  public ByteString getLocalChainId() {
    return manager.getGenesisBlockId().getByteString();
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
