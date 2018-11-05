package org.tron.core.net.messagehandler;

import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.tron.core.config.Parameter.ChainConstant.BLOCK_SIZE;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.Parameter.NodeConstant;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.BadNumberBlockException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.DupTransactionException;
import org.tron.core.exception.NonCommonBlockException;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.TaposException;
import org.tron.core.exception.TooBigTransactionException;
import org.tron.core.exception.TooBigTransactionResultException;
import org.tron.core.exception.TransactionExpirationException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.exception.ValidateScheduleException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.net.TronManager;
import org.tron.core.net.TronProxy;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.ChainInventoryMessage;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.node.NodeImpl.PriorItem;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerAdv;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerSync;
import org.tron.core.services.WitnessProductBlockService;
import org.tron.protos.Protocol.Inventory.InventoryType;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j
@Component
public class BlockMsgHandler implements TronMsgHandler {

  @Autowired
  private TronProxy tronProxy;

  @Autowired
  private TronManager tronManager;

  @Autowired
  private PeerAdv peerAdv;

  @Autowired
  private PeerSync peerSync;

  @Autowired
  private WitnessProductBlockService witnessProductBlockService;

  private Map<BlockMessage, PeerConnection> blockWaitToProc = new ConcurrentHashMap<>();

  private Map<BlockMessage, PeerConnection> blockJustReceived = new ConcurrentHashMap<>();

  private HashMap<Sha256Hash, Long> advObjWeRequested = new HashMap<>();

  private Queue<BlockId> freshBlockId = new ConcurrentLinkedQueue<BlockId>() {
    @Override
    public boolean offer(BlockId blockId) {
      if (size() > 200) {
        super.poll();
      }
      return super.offer(blockId);
    }
  };

  private ScheduledExecutorService syncHandleExecutor = Executors.newSingleThreadScheduledExecutor();

  private boolean syncHandleFlag;

  public void init () {
    syncHandleExecutor.scheduleWithFixedDelay(() -> {
      try {
        if (syncHandleFlag) {
          syncHandleFlag = false;
          handleSyncBlock();
        }
      } catch (Throwable t) {
        logger.error("Unhandled exception", t);
      }
    }, 10, 1, TimeUnit.SECONDS);
  }

  @Override
  public void processMessage (PeerConnection peer, TronMessage msg) throws Exception {

    BlockMessage blockMessage = (BlockMessage) msg;

    check(peer, blockMessage);

    BlockId blockId = blockMessage.getBlockId();
    Item item = new Item(blockId, InventoryType.BLOCK);
    boolean syncFlag = false;
    if (peer.getSyncBlockRequested().containsKey(blockId)) {
      peer.getSyncBlockRequested().remove(blockId);
      synchronized (blockJustReceived) {
        blockJustReceived.put(blockMessage, peer);
      }
      syncHandleFlag = true;
      syncFlag = true;
      if (!peer.isBusy()) {
        if (peer.getUnfetchSyncNum() > 0 && peer.getSyncBlockToFetch().size() <= NodeConstant.SYNC_FETCH_BATCH_NUM) {
          peerSync.syncNext(peer);
        } else {
          tronManager.setSyncBlockFetchFlag(true);
        }
      }
    }
    if (peer.getAdvObjWeRequested().containsKey(item)) {
      peer.getAdvObjWeRequested().remove(item);
      if (!syncFlag) {
        processAdvBlock(peer, blockMessage.getBlockCapsule());
      }
    }
  }

  private void check (PeerConnection peer, BlockMessage msg) throws Exception {
    BlockCapsule blockCapsule = msg.getBlockCapsule();
    if (blockCapsule.getInstance().getSerializedSize() > BLOCK_SIZE + 100) {
      throw new P2pException(TypeEnum.BAD_MESSAGE, "block size over limit");
    }

    long gap = blockCapsule.getTimeStamp() - System.currentTimeMillis();
    if (gap >= BLOCK_PRODUCED_INTERVAL) {
      throw new P2pException(TypeEnum.BAD_MESSAGE, "block time error");
    }
  }

  private void processAdvBlock(PeerConnection peer,  BlockCapsule block) throws Exception {
    synchronized (tronManager.getBlockLock()) {
      BlockId blockId = block.getBlockId();
      if (freshBlockId.contains(blockId)) {
        return;
      }
      if (!tronProxy.containBlock(block.getBlockId())) {
        logger.warn("Get unlink block {} from {}, head is {}.", blockId.getString(),
            peer.getInetAddress(), tronProxy.getHeadBlockId().getString());
        peerSync.startSync(peer);
        return;
      }
      witnessProductBlockService.validWitnessProductTwoBlock(block);
      handleBlock(block);
      //trxIds.forEach(trxId -> advObjToFetch.remove(trxId));
      tronProxy.getActivePeer().forEach(p -> {
        if (p.getAdvObjSpreadToUs().containsKey(blockId)) {
          p.setHeadBlockWeBothHave(blockId);
        }
      });
      peerAdv.broadcast(new BlockMessage(block));
    }
  }

  private void processSyncBlock (BlockCapsule block) {
    synchronized (tronManager.getBlockLock()) {
      boolean flag = true;
      BlockId blockId = block.getBlockId();
      if (!freshBlockId.contains(blockId)) {
        try {
          handleBlock(block);
        } catch (Exception e) {
          logger.error("Process sync block failed.", e);
          flag = false;
        }
      }
      for (PeerConnection peer: tronProxy.getActivePeer()) {
        if (peer.getBlockInProc().remove(blockId)) {
          if (flag){
            peer.setHeadBlockWeBothHave(blockId);
            if (peer.getSyncBlockToFetch().isEmpty()) {
              peerSync.syncNext(peer);
            }
          }else {
            peer.disconnect(ReasonCode.BAD_BLOCK);
          }
        }
      }
      syncHandleFlag = true;
    }
  }


  private void handleBlock(BlockCapsule block) throws Exception {
    try {
      tronProxy.preValidateTransactionSign(block);
      tronProxy.pushBlock(block);
      freshBlockId.add(block.getBlockId());
    }catch (UnLinkedBlockException e){
      throw new P2pException(TypeEnum.UNLINK_BLOCK, block.getBlockId().getString(), e);
    }catch (Exception e){
      throw new P2pException(TypeEnum.BAD_BLOCK, block.getBlockId().getString(), e);
    }
  }

  private synchronized void handleSyncBlock() {

    synchronized (blockJustReceived) {
      blockWaitToProc.putAll(blockJustReceived);
      blockJustReceived.clear();
    }

    final boolean[] isBlockProc = {true};

    while (isBlockProc[0]) {

      isBlockProc[0] = false;

      blockWaitToProc.forEach((msg, peerConnection) -> {
        if (peerConnection.isDisconnect()) {
          logger.error("Peer {} is disconnect, drop block {}", peerConnection.getInetAddress(), msg.getBlockId().getString());
          blockWaitToProc.remove(msg);
          syncBlockIdWeRequested.invalidate(msg.getBlockId());
          isFetchSyncActive = true;
          isBlockProc[0] = true;
          return;
        }
        synchronized (freshBlockId) {
          final boolean[] isFound = {false};
          tronProxy.getActivePeer().stream()
              .filter(peer -> !peer.getSyncBlockToFetch().isEmpty() &&
                  peer.getSyncBlockToFetch().peek().equals(msg.getBlockId()))
              .forEach(peer -> {
                peer.getSyncBlockToFetch().pop();
                peer.getBlockInProc().add(msg.getBlockId());
                isFound[0] = true;
              });
          if (isFound[0]) {
            blockWaitToProc.remove(msg);
            isBlockProc[0] = true;
            processSyncBlock(msg.getBlockCapsule());
          }
        }
      });
    }
  }

}
