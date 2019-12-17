package org.tron.core.ibc.communicate;

import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Map;
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
import org.tron.core.db.BlockIndexStore;
import org.tron.core.db.BlockStore;
import org.tron.core.db.PbftSignDataStore;
import org.tron.core.db.TransactionStore;
import org.tron.core.exception.BadItemException;
import org.tron.core.net.message.CrossChainMessage;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.CrossMessage;
import org.tron.protos.Protocol.Proof;

@Slf4j(topic = "Communicate")
@Service
public class CommunicateService implements Communicate {

  private Map<String, CrossMessage> data;

  private long timeOut = 1000 * 60 * 60L;

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Autowired
  private SyncPool syncPool;

  @Override
  public void sendCrossMessage(CrossMessage crossMessage) {
    Sha256Hash txId = Sha256Hash.of(crossMessage.getTransaction().getRawData().toByteArray());
    if (checkCommit(txId)) {
      try {
        //generate proof path
        BlockStore blockStore = chainBaseManager.getBlockStore();
        BlockIndexStore blockIndexStore = chainBaseManager.getBlockIndexStore();
        TransactionStore transactionStore = chainBaseManager.getTransactionStore();
        DynamicPropertiesStore propertiesStore = chainBaseManager.getDynamicPropertiesStore();
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
            .setTimeOutBlockHeight(timeOut / BLOCK_PRODUCED_INTERVAL
                + propertiesStore.getLatestSolidifiedBlockNum() + 1)
            .setRouteChainId(getLocalChainId()).setRootHeight(blockNum).build();
        chainBaseManager.getCrossStore().saveSendCrossMsg(txId, crossMessage);
        //todo: send data

      } catch (Exception e) {
        //todo

      }
    }
  }

  @Override
  public void receiveCrossMessage(CrossMessage crossMessage) {
    if (validProof(crossMessage)) {
      broadcastCrossMessage(crossMessage);
    } else {
      //done: disconnect to send end
      disconnect(crossMessage.getRouteChainId());
    }
  }

  @Override
  public boolean validProof(CrossMessage crossMessage) {
    List<Proof> proofList = crossMessage.getProofList();
    Sha256Hash txId = Sha256Hash.of(crossMessage.getTransaction().getRawData().toByteArray());
    Sha256Hash root = getRoot(crossMessage.getRouteChainId(), crossMessage.getRootHeight());
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
    syncPool.getActivePeers().forEach(peerConnection -> {
          peerConnection.sendMessage(new CrossChainMessage(crossMessage));
        }
    );
    return false;
  }

  /**
   * todo: other chain block tx merkel root
   */
  private Sha256Hash getRoot(ByteString fromChainId, long blockHeight) {
    if ("find the cross chain id".equals(fromChainId)) {
      return null;
    } else {
      return null;
    }
  }

  /**
   * todo:
   */
  private ByteString getLocalChainId() {
    return ByteString.copyFromUtf8("");
  }

  private void disconnect(ByteString fromChainId) {

  }
}
