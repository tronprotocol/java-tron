package org.tron.core.net.messagehandler;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.Parameter.NodeConstant;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.NonCommonBlockException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.core.net.TronManager;
import org.tron.core.net.TronProxy;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.ChainInventoryMessage;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.peer.Item;
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
  private PeerSync peerSync;

  @Autowired
  private WitnessProductBlockService witnessProductBlockService;

  private Map<BlockMessage, PeerConnection> blockWaitToProc = new ConcurrentHashMap<>();

  private Map<BlockMessage, PeerConnection> blockJustReceived = new ConcurrentHashMap<>();

  private Queue<BlockId> freshBlockId = new ConcurrentLinkedQueue<BlockId>() {
    @Override
    public boolean offer(BlockId blockId) {
      if (size() > 200) {
        super.poll();
      }
      return super.offer(blockId);
    }
  };

  private boolean isHandleSyncBlockActive;

  @Override
  public void processMessage (PeerConnection peer, TronMessage msg) throws Exception {

    BlockMessage blockMessage = (BlockMessage) msg;

    BlockId blockId = blockMessage.getBlockId();
    Item item = new Item(blockId, InventoryType.BLOCK);
    boolean syncFlag = false;
    if (peer.getSyncBlockRequested().containsKey(blockId)) {
      peer.getSyncBlockRequested().remove(blockId);
      synchronized (blockJustReceived) {
        blockJustReceived.put(blockMessage, peer);
      }
      isHandleSyncBlockActive = true;
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

  private void check(PeerConnection peer, ChainInventoryMessage msg) throws Exception {

  }

  private void processAdvBlock(PeerConnection peer, BlockCapsule block) {
    synchronized (tronManager.getBlockLock()) {
      if (!freshBlockId.contains(block.getBlockId())) {
        try {
          witnessProductBlockService.validWitnessProductTwoBlock(block);
          LinkedList<Sha256Hash> trxIds = null;
          trxIds = del.handleBlock(block, false);
          freshBlockId.offer(block.getBlockId());

          trxIds.forEach(trxId -> advObjToFetch.remove(trxId));

          getActivePeer().stream()
              .filter(p -> p.getAdvObjSpreadToUs().containsKey(block.getBlockId()))
              .forEach(p -> updateBlockWeBothHave(p, block));

          broadcast(new BlockMessage(block));

        } catch (BadBlockException e) {
          logger.error("We get a bad block {}, from {}, reason is {} ",
              block.getBlockId().getString(), peer.getNode().getHost(), e.getMessage());
          disconnectPeer(peer, ReasonCode.BAD_BLOCK);
        } catch (UnLinkedBlockException e) {
          logger.error("We get a unlinked block {}, from {}, head is {}", block.getBlockId().
              getString(), peer.getNode().getHost(), del.getHeadBlockId().getString());
          startSyncWithPeer(peer);
        } catch (NonCommonBlockException e) {
          logger.error(
              "We get a block {} that do not have the most recent common ancestor with the main chain, from {}, reason is {} ",
              block.getBlockId().getString(), peer.getNode().getHost(), e.getMessage());
          disconnectPeer(peer, ReasonCode.FORKED);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  private boolean processSyncBlock(BlockCapsule block) {
    boolean isAccept = false;
    ReasonCode reason = null;
    try {
      try {
        del.handleBlock(block, true);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      freshBlockId.offer(block.getBlockId());
      logger.info("Success handle block {}", block.getBlockId().getString());
      isAccept = true;
    } catch (BadBlockException e) {
      logger.error("We get a bad block {}, reason is {} ", block.getBlockId().getString(),
          e.getMessage());
      reason = ReasonCode.BAD_BLOCK;
    } catch (UnLinkedBlockException e) {
      logger.error("We get a unlinked block {}, head is {}", block.getBlockId().getString(),
          del.getHeadBlockId().getString());
      reason = ReasonCode.UNLINKABLE;
    } catch (NonCommonBlockException e) {
      logger.error(
          "We get a block {} that do not have the most recent common ancestor with the main chain, head is {}",
          block.getBlockId().getString(),
          del.getHeadBlockId().getString());
      reason = ReasonCode.FORKED;
    }

    if (!isAccept) {
      ReasonCode finalReason = reason;
      getActivePeer().stream()
          .filter(peer -> peer.getBlockInProc().contains(block.getBlockId()))
          .forEach(peer -> disconnectPeer(peer, finalReason));
    }
    isHandleSyncBlockActive = true;
    return isAccept;
  }

  private void finishProcessSyncBlock(BlockCapsule block) {
    getActivePeer().forEach(peer -> {
      if (peer.getSyncBlockToFetch().isEmpty()
          && peer.getBlockInProc().isEmpty()
          && !peer.isNeedSyncFromPeer()
          && !peer.isNeedSyncFromUs()) {
        startSyncWithPeer(peer);
      } else if (peer.getBlockInProc().remove(block.getBlockId())) {
        updateBlockWeBothHave(peer, block);
        if (peer.getSyncBlockToFetch().isEmpty()) { //send sync to let peer know we are sync.
          syncNextBatchChainIds(peer);
        }
      }
    });
  }
}
