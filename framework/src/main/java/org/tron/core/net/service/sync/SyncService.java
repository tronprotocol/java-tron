package org.tron.core.net.service.sync;

import static org.tron.core.config.Parameter.NetConstants.MAX_BLOCK_FETCH_PER_PEER;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.utils.Pair;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.args.Args;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.message.adv.FetchInvDataMessage;
import org.tron.core.net.message.sync.SyncBlockChainMessage;
import org.tron.core.net.messagehandler.PbftDataSyncHandler;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.TronState;
import org.tron.protos.Protocol.Inventory.InventoryType;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
public class SyncService {

  @Autowired
  private TronNetDelegate tronNetDelegate;

  @Autowired
  private PbftDataSyncHandler pbftDataSyncHandler;

  private Map<BlockMessage, PeerConnection> blockWaitToProcess = new ConcurrentHashMap<>();

  private Map<BlockMessage, PeerConnection> blockJustReceived = new ConcurrentHashMap<>();

  private long blockCacheTimeout = Args.getInstance().getBlockCacheTimeout();
  private Cache<BlockId, PeerConnection> requestBlockIds = CacheBuilder.newBuilder()
      .maximumSize(10_000)
      .expireAfterWrite(blockCacheTimeout, TimeUnit.MINUTES).initialCapacity(10_000)
      .recordStats().build();

  private final String fetchEsName = "sync-fetch-block";
  private final String handleEsName = "sync-handle-block";
  private final ScheduledExecutorService fetchExecutor = ExecutorServiceManager
      .newSingleThreadScheduledExecutor(fetchEsName);

  private final ScheduledExecutorService blockHandleExecutor = ExecutorServiceManager
      .newSingleThreadScheduledExecutor(handleEsName);

  private volatile boolean handleFlag = false;

  @Setter
  private volatile boolean fetchFlag = false;

  private final long syncFetchBatchNum = Args.getInstance().getSyncFetchBatchNum();

  public void init() {
    fetchExecutor.scheduleWithFixedDelay(() -> {
      try {
        if (fetchFlag) {
          fetchFlag = false;
          startFetchSyncBlock();
        }
      } catch (Exception e) {
        logger.error("Fetch sync block error", e);
      }
    }, 10, 1, TimeUnit.SECONDS);

    blockHandleExecutor.scheduleWithFixedDelay(() -> {
      try {
        if (handleFlag) {
          handleFlag = false;
          handleSyncBlock();
        }
      } catch (Exception e) {
        logger.error("Handle sync block error", e);
      }
    }, 10, 1, TimeUnit.SECONDS);
  }

  public void close() {
    ExecutorServiceManager.shutdownAndAwaitTermination(fetchExecutor, fetchEsName);
    ExecutorServiceManager.shutdownAndAwaitTermination(blockHandleExecutor, handleEsName);
  }

  public void startSync(PeerConnection peer) {
    if (peer.getTronState().equals(TronState.SYNCING)) {
      logger.warn("Start sync failed, peer {} is in sync", peer.getInetSocketAddress());
      return;
    }
    peer.setTronState(TronState.SYNCING);
    peer.setNeedSyncFromPeer(true);
    peer.getSyncBlockToFetch().clear();
    peer.setRemainNum(0);
    peer.setBlockBothHave(tronNetDelegate.getGenesisBlockId());
    syncNext(peer);
  }

  public void syncNext(PeerConnection peer) {
    try {
      if (peer.getSyncChainRequested() != null) {
        logger.warn("Peer {} is in sync", peer.getInetSocketAddress());
        return;
      }
      LinkedList<BlockId> chainSummary;
      synchronized (tronNetDelegate.getForkLock()) {
        chainSummary = getBlockChainSummary(peer);
      }
      peer.setSyncChainRequested(new Pair<>(chainSummary, System.currentTimeMillis()));
      peer.sendMessage(new SyncBlockChainMessage(chainSummary));
    } catch (Exception e) {
      logger.error("Peer {} sync failed, reason: {}", peer.getInetAddress(), e);
      peer.disconnect(ReasonCode.SYNC_FAIL);
    }
  }

  public void processBlock(PeerConnection peer, BlockMessage blockMessage) {
    synchronized (blockJustReceived) {
      blockJustReceived.put(blockMessage, peer);
    }
    handleFlag = true;
    if (peer.isSyncIdle()) {
      if (peer.getRemainNum() > 0
          && peer.getSyncBlockToFetch().size() <= syncFetchBatchNum) {
        syncNext(peer);
      } else {
        fetchFlag = true;
      }
    }
  }

  public void onDisconnect(PeerConnection peer) {
    if (!peer.getSyncBlockRequested().isEmpty()) {
      peer.getSyncBlockRequested().keySet().forEach(blockId -> invalid(blockId, peer));
    }
  }

  private void invalid(BlockId blockId, PeerConnection peerConnection) {
    PeerConnection p = requestBlockIds.getIfPresent(blockId);
    if (peerConnection.equals(p)) {
      requestBlockIds.invalidate(blockId);
      fetchFlag = true;
    }
  }

  private LinkedList<BlockId> getBlockChainSummary(PeerConnection peer) throws P2pException {

    BlockId beginBlockId = peer.getBlockBothHave();
    List<BlockId> blockIds = new ArrayList<>(peer.getSyncBlockToFetch());
    List<BlockId> forkList = new LinkedList<>();
    LinkedList<BlockId> summary = new LinkedList<>();
    long syncBeginNumber = tronNetDelegate.getSyncBeginNumber();
    long low = syncBeginNumber < 0 ? 0 : syncBeginNumber;
    long highNoFork;
    long high;

    if (beginBlockId.getNum() == 0) {
      highNoFork = high = tronNetDelegate.getHeadBlockId().getNum();
    } else {
      if (tronNetDelegate.getKhaosDbHeadBlockId().equals(beginBlockId)
          || tronNetDelegate.containBlockInMainChain(beginBlockId)) {
        highNoFork = high = beginBlockId.getNum();
      } else {
        forkList = tronNetDelegate.getBlockChainHashesOnFork(beginBlockId);
        if (forkList.isEmpty()) {
          throw new P2pException(TypeEnum.SYNC_FAILED,
              "can't find blockId: " + beginBlockId.getString());
        }
        highNoFork = ((LinkedList<BlockId>) forkList).peekLast().getNum();
        ((LinkedList) forkList).pollLast();
        Collections.reverse(forkList);
        high = highNoFork + forkList.size();
      }
    }

    if (low > highNoFork) {
      throw new P2pException(TypeEnum.SYNC_FAILED, "low: " + low + " gt highNoFork: " + highNoFork);
    }

    long realHigh = high + blockIds.size();

    logger.info("Get block chain summary, low: {}, highNoFork: {}, high: {}, realHigh: {}",
        low, highNoFork, high, realHigh);

    while (low <= realHigh) {
      if (low <= highNoFork) {
        summary.offer(getBlockIdByNum(low));
      } else if (low <= high) {
        summary.offer(forkList.get((int) (low - highNoFork - 1)));
      } else {
        summary.offer(blockIds.get((int) (low - high - 1)));
      }
      low += (realHigh - low + 2) / 2;
    }

    return summary;
  }

  private BlockId getBlockIdByNum(long num) throws P2pException {
    BlockId head = tronNetDelegate.getKhaosDbHeadBlockId();
    if (num == head.getNum()) {
      return head;
    }
    head = tronNetDelegate.getHeadBlockId();
    if (num == head.getNum()) {
      return head;
    }
    return tronNetDelegate.getBlockIdByNum(num);
  }

  private void startFetchSyncBlock() {
    HashMap<PeerConnection, List<BlockId>> send = new HashMap<>();
    tronNetDelegate.getActivePeer().stream()
        .filter(peer -> peer.isNeedSyncFromPeer() && peer.isSyncIdle())
        .filter(peer -> peer.isFetchAble())
        .forEach(peer -> {
          if (!send.containsKey(peer)) {
            send.put(peer, new LinkedList<>());
          }
          for (BlockId blockId : peer.getSyncBlockToFetch()) {
            if (requestBlockIds.getIfPresent(blockId) == null
                && !peer.getSyncBlockInProcess().contains(blockId)) {
              requestBlockIds.put(blockId, peer);
              peer.getSyncBlockRequested().put(blockId, System.currentTimeMillis());
              send.get(peer).add(blockId);
              if (send.get(peer).size() >= MAX_BLOCK_FETCH_PER_PEER) {
                break;
              }
            }
          }
        });

    send.forEach((peer, blockIds) -> {
      if (!blockIds.isEmpty()) {
        peer.sendMessage(new FetchInvDataMessage(new LinkedList<>(blockIds), InventoryType.BLOCK));
      }
    });
  }

  private synchronized void handleSyncBlock() {

    synchronized (blockJustReceived) {
      blockWaitToProcess.putAll(blockJustReceived);
      blockJustReceived.clear();
    }

    final boolean[] isProcessed = {true};
    long solidNum = tronNetDelegate.getSolidBlockId().getNum();

    while (isProcessed[0]) {

      isProcessed[0] = false;

      blockWaitToProcess.forEach((msg, peerConnection) -> {
        synchronized (tronNetDelegate.getBlockLock()) {
          if (peerConnection.isDisconnect()) {
            blockWaitToProcess.remove(msg);
            invalid(msg.getBlockId(), peerConnection);
            return;
          }
          if (msg.getBlockId().getNum() <= solidNum) {
            blockWaitToProcess.remove(msg);
            peerConnection.getSyncBlockInProcess().remove(msg.getBlockId());
            return;
          }
          final boolean[] isFound = {false};
          tronNetDelegate.getActivePeer().stream()
              .filter(peer -> msg.getBlockId().equals(peer.getSyncBlockToFetch().peek()))
              .forEach(peer -> {
                isFound[0] = true;
              });
          if (isFound[0]) {
            blockWaitToProcess.remove(msg);
            isProcessed[0] = true;
            processSyncBlock(msg.getBlockCapsule(), peerConnection);
            peerConnection.getSyncBlockInProcess().remove(msg.getBlockId());
          }
        }
      });
    }
  }

  private void processSyncBlock(BlockCapsule block, PeerConnection peerConnection) {
    boolean flag = true;
    boolean attackFlag = false;
    BlockId blockId = block.getBlockId();
    try {
      tronNetDelegate.validSignature(block);
      tronNetDelegate.processBlock(block, true);
      pbftDataSyncHandler.processPBFTCommitData(block);
    } catch (P2pException p2pException) {
      logger.error("Process sync block {} failed, type: {}",
              blockId.getString(), p2pException.getType());
      attackFlag = p2pException.getType().equals(TypeEnum.BLOCK_SIGN_ERROR)
              || p2pException.getType().equals(TypeEnum.BLOCK_MERKLE_ERROR);
      flag = false;
    } catch (Exception e) {
      logger.error("Process sync block {} failed", blockId.getString(), e);
      flag = false;
    }

    if (attackFlag) {
      invalid(blockId, peerConnection);
      peerConnection.disconnect(ReasonCode.BAD_BLOCK);
      return;
    }

    for (PeerConnection peer : tronNetDelegate.getActivePeer()) {
      BlockId bid = peer.getSyncBlockToFetch().peek();
      if (blockId.equals(bid)) {
        peer.getSyncBlockToFetch().remove(bid);
        if (flag) {
          peer.setBlockBothHave(blockId);
          if (peer.getSyncBlockToFetch().isEmpty() && peer.isFetchAble()) {
            syncNext(peer);
          }
        } else {
          peer.disconnect(ReasonCode.BAD_BLOCK);
        }
      }
    }
  }

}
