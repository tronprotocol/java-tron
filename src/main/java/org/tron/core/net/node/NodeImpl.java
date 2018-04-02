package org.tron.core.net.node;

import com.google.common.collect.Iterables;
import io.netty.util.internal.ConcurrentSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.discover.NodeHandler;
import org.tron.common.overlay.message.Message;
import org.tron.common.overlay.message.ReasonCode;
import org.tron.common.overlay.server.Channel.TronState;
import org.tron.common.overlay.server.SyncPool;
import org.tron.common.utils.ExecutorLoop;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Time;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.Parameter.BlockConstant;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.config.Parameter.NodeConstant;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.BadTransactionException;
import org.tron.core.exception.TraitorPeerException;
import org.tron.core.exception.TronException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.core.exception.UnReachBlockException;
import org.tron.core.net.message.BlockInventoryMessage;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.ChainInventoryMessage;
import org.tron.core.net.message.FetchInvDataMessage;
import org.tron.core.net.message.InventoryMessage;
import org.tron.core.net.message.ItemNotFound;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.SyncBlockChainMessage;
import org.tron.core.net.message.TransactionMessage;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerConnectionDelegate;
import org.tron.protos.Protocol.Inventory.InventoryType;

@Slf4j
@Component
public class NodeImpl extends PeerConnectionDelegate implements Node {

  @Autowired
  private SyncPool pool;

  class InvToSend {

    private HashMap<PeerConnection, HashMap<InventoryType, LinkedList<Sha256Hash>>> send
        = new HashMap<>();

    public void clear() {
      this.send.clear();
    }

    public void add(Entry<Sha256Hash, InventoryType> id, PeerConnection peer) {
      if (send.containsKey(peer) && send.get(peer).containsKey(id.getValue())) {
        send.get(peer).get(id.getValue()).offer(id.getKey());
      } else if (send.containsKey(peer)) {
        send.get(peer).put(id.getValue(), new LinkedList<>());
        send.get(peer).get(id.getValue()).offer(id.getKey());
      } else {
        send.put(peer, new HashMap<>());
        send.get(peer).put(id.getValue(), new LinkedList<>());
        send.get(peer).get(id.getValue()).offer(id.getKey());
      }
    }

    public void sendInv() {
      send.forEach((peer, ids) ->
          ids.forEach((key, value) -> peer.sendMessage(new InventoryMessage(value, key))));
    }

    public void sendFetch() {
      send.forEach((peer, ids) ->
          ids.forEach((key, value) -> peer.sendMessage(new FetchInvDataMessage(value, key))));
    }
  }

  private final List<Sha256Hash> trxToAdvertise = new ArrayList<>();

  private final List<BlockId> blockToAdvertise = new ArrayList<>();

  //public
  //TODO:need auto erase oldest block
  private Queue<BlockId> freshBlockId = new LinkedBlockingQueue<>();

  private ConcurrentHashMap<Sha256Hash, PeerConnection> syncMap = new ConcurrentHashMap<>();

  private ConcurrentHashMap<Sha256Hash, PeerConnection> fetchMap = new ConcurrentHashMap<>();

  private NodeDelegate del;

  private volatile boolean isAdvertiseActive;

  private volatile boolean isFetchActive;

  private volatile boolean isHandleSyncBlockActive;

  private ScheduledExecutorService disconnectInactiveExecutor = Executors.newSingleThreadScheduledExecutor();

  //broadcast
  private ConcurrentHashMap<Sha256Hash, InventoryType> advObjToSpread = new ConcurrentHashMap<>();

  private HashMap<Sha256Hash, Long> advObjWeRequested = new HashMap<>();

  private ConcurrentHashMap<Sha256Hash, InventoryType> advObjToFetch = new ConcurrentHashMap<>();

  private Thread advertiseLoopThread;

  private Thread advObjFetchLoopThread;

  private HashMap<Sha256Hash, Long> badAdvObj = new HashMap<>(); //TODO:need auto erase oldest obj

  //sync
  private HashMap<BlockId, Long> syncBlockIdWeRequested = new HashMap<>();

  private Long unSyncNum = 0L;

  private Thread handleSyncBlockLoop;

  private Set<BlockMessage> blockWaitToProc = new ConcurrentSet<>();

  private Set<BlockMessage> blockWaitToProcBak = new ConcurrentSet<>();

  private Set<BlockMessage> blockInProc = new ConcurrentSet<>();

  ExecutorLoop<SyncBlockChainMessage> loopSyncBlockChain;

  ExecutorLoop<FetchInvDataMessage> loopFetchBlocks;

  ExecutorLoop<Message> loopAdvertiseInv;

  @Override
  public void onMessage(PeerConnection peer, TronMessage msg) {
    logger.info("Handle Message: " + msg);
    switch (msg.getType()) {
      case BLOCK:
        onHandleBlockMessage(peer, (BlockMessage) msg);
        break;
      case TRX:
        onHandleTransactionMessage(peer, (TransactionMessage) msg);
        break;
      case SYNC_BLOCK_CHAIN:
        onHandleSyncBlockChainMessage(peer, (SyncBlockChainMessage) msg);
        break;
      case FETCH_INV_DATA:
        onHandleFetchDataMessage(peer, (FetchInvDataMessage) msg);
        break;
      case BLOCK_INVENTORY:
        onHandleBlockInventoryMessage(peer, (BlockInventoryMessage) msg);
        break;
      case BLOCK_CHAIN_INVENTORY:
        onHandleChainInventoryMessage(peer, (ChainInventoryMessage) msg);
        break;
      case INVENTORY:
        onHandleInventoryMessage(peer, (InventoryMessage) msg);
        break;
      default:
        throw new IllegalArgumentException("No such message");
    }
  }

  @Override
  public Message getMessage(Sha256Hash msgId) {
    return null;
  }


  @Override
  public void setNodeDelegate(NodeDelegate nodeDel) {
    this.del = nodeDel;
  }

  /**
   * broadcast msg.
   *
   * @param msg msg to bradcast
   */
  public void broadcast(Message msg) {
    InventoryType type;
    if (msg instanceof BlockMessage) {
      logger.info("Ready to broadcast a block, Its hash is " + msg.getMessageId());
      freshBlockId.offer(((BlockMessage) msg).getBlockId());
      blockToAdvertise.add(((BlockMessage) msg).getBlockId());
      type = InventoryType.BLOCK;
    } else if (msg instanceof TransactionMessage) {
      trxToAdvertise.add(msg.getMessageId());
      type = InventoryType.TRX;
    } else {
      return;
    }
    //TODO: here need to cache fresh message to let peer fetch these data not from DB
    advObjToSpread.put(msg.getMessageId(), type);
  }

  @Override
  public void listen() {
    isAdvertiseActive = true;
    isFetchActive = true;
    isHandleSyncBlockActive = true;
    activeTronPump();
  }

  @Override
  public void close() throws InterruptedException {
    loopFetchBlocks.join();
    loopSyncBlockChain.join();
    loopAdvertiseInv.join();
    isAdvertiseActive = false;
    isFetchActive = true;
    advertiseLoopThread.join();
    advObjFetchLoopThread.join();
    handleSyncBlockLoop.join();
    disconnectInactiveExecutor.shutdown();
  }

  @Override
  public List<NodeHandler> getActiveNodes() {
    return this.pool.getActiveNodes();
  }

  private void activeTronPump() {
    // broadcast inv
    loopAdvertiseInv = new ExecutorLoop<>(2, 10, b -> {
      //logger.info("loop advertise inv");
      for (PeerConnection peer : getActivePeer()) {
        if (!peer.isNeedSyncFromUs()) {
          logger.info("Advertise adverInv to " + peer);
          peer.sendMessage(b);
        }
      }
    }, throwable -> logger.error("Unhandled exception: ", throwable));

    // fetch blocks
    loopFetchBlocks = new ExecutorLoop<>(2, 10, c -> {
      logger.info("loop fetch blocks");
      if (fetchMap.containsKey(c.getMessageId())) {
        fetchMap.get(c.getMessageId()).sendMessage(c);
      }
    }, throwable -> logger.error("Unhandled exception: ", throwable));

    // sync block chain
    loopSyncBlockChain = new ExecutorLoop<>(2, 10, d -> {
      //logger.info("loop sync block chain");
      if (syncMap.containsKey(d.getMessageId())) {
        syncMap.get(d.getMessageId()).sendMessage(d);
      }
    }, throwable -> logger.error("Unhandled exception: ", throwable));

    advertiseLoopThread = new Thread(() -> {
      while (isAdvertiseActive) {
        if (advObjToSpread.isEmpty()) {
          try {
            Thread.sleep(1000);
            continue;
          } catch (InterruptedException e) {
            logger.debug(e.getMessage(), e);
          }
        }

        synchronized (advObjToSpread) {
          HashMap<Sha256Hash, InventoryType> spread = new HashMap<>();
          InvToSend sendPackage = new InvToSend();
          spread.putAll(advObjToSpread);
          advObjToSpread.clear();

          getActivePeer().stream()
              .filter(peer -> !peer.isNeedSyncFromUs())
              .forEach(peer -> {
                spread.entrySet().stream()
                    .filter(idToSpread ->
                        !peer.getAdvObjSpreadToUs().containsKey(idToSpread.getKey())
                            && !peer.getAdvObjWeSpread().containsKey(idToSpread.getKey()))
                    .forEach(idToSpread -> {
                      peer.getAdvObjWeSpread().put(idToSpread.getKey(), Time.getCurrentMillis());
                      sendPackage.add(idToSpread, peer);
                    });
                peer.cleanInvGarbage();
              });

          sendPackage.sendInv();
        }
      }
    });

    advObjFetchLoopThread = new Thread(() -> {
      while (isFetchActive) {
        if (advObjToFetch.isEmpty()) {
          try {
            Thread.sleep(1000);
            continue;
          } catch (InterruptedException e) {
            logger.debug(e.getMessage(), e);
          }
        }

        synchronized (advObjToFetch) {
          InvToSend sendPackage = new InvToSend();
          advObjToFetch.entrySet()
              .forEach(idToFetch -> {
                getActivePeer().stream().filter(peer -> !peer.isBusy()
                    && peer.getAdvObjSpreadToUs().containsKey(idToFetch.getKey()))
                    .findFirst()
                    .ifPresent(peer -> {
                      //TODO: don't fetch too much obj from only one peer
                      sendPackage.add(idToFetch, peer);
                      advObjToFetch.remove(idToFetch.getKey());
                      peer.getAdvObjWeRequested()
                          .put(idToFetch.getKey(), Time.getCurrentMillis());
                    });
              });
          sendPackage.sendFetch();
        }
      }
    });

    handleSyncBlockLoop = new Thread(() -> {
      while (isHandleSyncBlockActive) {
        if (blockWaitToProcBak.isEmpty()) {
          try {
            Thread.sleep(1000);
            continue;
          } catch (InterruptedException e) {
            logger.debug(e.getMessage(), e);
          }
        }

        final boolean[] isBlockProc = {false};

        do {
          synchronized (blockWaitToProcBak) {
            blockWaitToProc.addAll(blockWaitToProcBak);
            //need lock here
            blockWaitToProcBak.clear();
          }

          isBlockProc[0] = false;
          Set<BlockMessage> pool = new HashSet<>();
          pool.addAll(blockWaitToProc);
          pool.forEach(msg -> {
            final boolean[] isFound = {false};
            getActivePeer().stream()
                .filter(peer ->
                    !peer.getSyncBlockToFetch().isEmpty()
                        && peer.getSyncBlockToFetch().peek().equals(msg.getBlockId()))
                .forEach(peer -> {
                  peer.getSyncBlockToFetch().pop();
                  peer.getBlockInProc().add(msg.getBlockId());
                  isFound[0] = true;
                });

            if (isFound[0]) {
              if (!freshBlockId.contains(msg.getBlockId())) {
                blockWaitToProc.remove(msg);
                processSyncBlock(msg.getBlockCapsule());
                isBlockProc[0] = true;
              }
            }
          });
        } while (isBlockProc[0]);
      }

    });

    //TODO: wait to refactor these threads.
    advertiseLoopThread.start();
    advObjFetchLoopThread.start();
    handleSyncBlockLoop.start();

    //terminate inactive loop
    disconnectInactiveExecutor.scheduleWithFixedDelay(() -> {
      try {
        disconnectInactive();
      } catch (Throwable t) {
        logger.error("Unhandled exception", t);
      }
    }, 30000, BlockConstant.BLOCK_INTERVAL / 2, TimeUnit.MILLISECONDS);

  }

  private void disconnectInactive() {
    getActivePeer().forEach(peer -> {
      final boolean[] isDisconnected = {false};

      peer.getAdvObjWeRequested().values().stream()
          .filter(time -> time < Time.getCurrentMillis() - NetConstants.ADV_TIME_OUT)
          .findFirst().ifPresent(time -> isDisconnected[0] = true);

      if (!isDisconnected[0]) {
        peer.getSyncBlockRequested().values().stream()
            .filter(time -> time < Time.getCurrentMillis() - NetConstants.SYNC_TIME_OUT)
            .findFirst().ifPresent(time -> isDisconnected[0] = true);
      }

      //TODO:optimize here
      if (!isDisconnected[0]) {
        if (del.getHeadBlockId().getNum() - peer.getHeadBlockWeBothHave().getNum()
            > 2 * NetConstants.HEAD_NUM_CHECK_TIME / BlockConstant.BLOCK_INTERVAL
            && peer.getConnectTime() < Time.getCurrentMillis() - NetConstants.HEAD_NUM_CHECK_TIME) {
          isDisconnected[0] = true;
        }
      }

      if (isDisconnected[0]) {
        //TODO use new reason
        disconnectPeer(peer, ReasonCode.USER_REASON);
      }
    });
  }



  private void onHandleInventoryMessage(PeerConnection peer, InventoryMessage msg) {
    //logger.info("on handle advertise inventory message");
    peer.cleanInvGarbage();

    msg.getHashList().forEach(id -> {
      final boolean[] spreaded = {false};
      final boolean[] requested = {false};
      getActivePeer().forEach(p -> {
        if (p.getAdvObjWeSpread().containsKey(id)) {
          spreaded[0] = true;
        }
        if (p.getAdvObjWeRequested().containsKey(id)) {
          requested[0] = true;
        }
      });

      if (!spreaded[0]) {
        //TODO: avoid TRX flood attack here.
        peer.getAdvObjSpreadToUs().put(id, System.currentTimeMillis());
        if (!requested[0]) {
          //TODO: make a error cache here, Don't handle error TRX or BLK repeatedly.
          if (!badAdvObj.containsKey(id)) {
            this.advObjToFetch.put(id, msg.getInventoryType());
          }
        }
      }
    });
  }

  @Override
  public void syncFrom(Sha256Hash myHeadBlockHash) {
    //List<Sha256Hash> hashList = del.getBlockChainSummary(myHeadBlockHash, 100);

    try {
      while (getActivePeer().isEmpty()) {
        logger.info("other peer is nil, please wait ... ");
        Thread.sleep(10000L);
      }
    } catch (InterruptedException e) {
      logger.debug(e.getMessage(), e);
    }

    logger.info("wait end");

    //loopSyncBlockChain.push(new SyncBlockChainMessage(hashList));
  }


  private void onHandleBlockMessage(PeerConnection peer, BlockMessage blkMsg) {
    //logger.info("on handle block message");
    //peer.setLastBlockPeerKnow((BlockId) blkMsg.getMessageId());

    HashMap<Sha256Hash, Long> advObjWeRequested = peer.getAdvObjWeRequested();
    HashMap<BlockId, Long> syncBlockRequested = peer.getSyncBlockRequested();
    BlockId blockId = blkMsg.getBlockId();
    //logger.info("Block number is " + blkMsg.getBlockId().getNum());

    if (advObjWeRequested.containsKey(blockId)) {
      //broadcast mode
      advObjWeRequested.remove(blockId);
      processAdvBlock(peer, blkMsg.getBlockCapsule());
      startFetchItem();
    } else if (syncBlockRequested.containsKey(blockId)) {
      //sync mode
      syncBlockRequested.remove(blockId);
      //peer.getSyncBlockToFetch().remove(blockId);
      syncBlockIdWeRequested.remove(blockId);
      //TODO: maybe use consume pipe here better
      blockWaitToProcBak.add(blkMsg);
      //processSyncBlock(blkMsg.getBlockCapsule());
      if (!peer.isBusy()) {
        if (peer.getUnfetchSyncNum() > 0
            && peer.getSyncBlockToFetch().size() < NodeConstant.SYNC_FETCH_BATCH_NUM) {
          syncNextBatchChainIds(peer);
        } else {
          //TODO: here should be a loop do this thing
          //startFetchSyncBlock();
        }
      }

    }
  }

  private void processAdvBlock(PeerConnection peer, BlockCapsule block) {
    //TODO: lack the complete flow.
    if (!freshBlockId.contains(block.getBlockId())) {
      try {
        LinkedList<Sha256Hash> trxIds = del.handleBlock(block, false);
        freshBlockId.offer(block.getBlockId());
        trxIds.forEach(trxId -> advObjToFetch.remove(trxId));

        //TODO:save message cache again.
        getActivePeer().stream()
            .filter(p -> p.getAdvObjSpreadToUs().containsKey(block.getBlockId()))
            .forEach(p -> {
              p.setHeadBlockWeBothHave(block.getBlockId());
              p.setHeadBlockTimeWeBothHave(block.getTimeStamp());
            });

        getActivePeer().forEach(p -> p.cleanInvGarbage());
        //rebroadcast
        broadcast(new BlockMessage(block));

      } catch (BadBlockException e) {
        badAdvObj.put(block.getBlockId(), System.currentTimeMillis());
      } catch (UnLinkedBlockException e) {
        //reSync
        startSyncWithPeer(peer);
      }
    }
  }

  private void processSyncBlock(BlockCapsule block) {
    //TODO: add processing backlog cache here, use multi thread

    try {
      del.handleBlock(block, true);
      freshBlockId.offer(block.getBlockId());
    } catch (BadBlockException e) {
      badAdvObj.put(block.getBlockId(), System.currentTimeMillis());
    } catch (TronException e) {
      //should not go here.
      logger.debug(e.getMessage(), e);
      //logger.error(e.getMessage());
      return;
    }

    Deque<PeerConnection> needSync = new LinkedList<>();
    Deque<PeerConnection> needFetchAgain = new LinkedList<>();

    getActivePeer()
        .forEach(peer -> {
          if (peer.getSyncBlockToFetch().isEmpty()
              && peer.getBlockInProc().isEmpty()
              && !peer.isNeedSyncFromPeer()
              && !peer.isNeedSyncFromUs()) {
            needSync.offer(peer);
          } else {
            //TODO: erase process here
            if (peer.getBlockInProc().remove(block.getBlockId())) {
              updateBlockWeBothHave(peer, block);
              if (peer.getSyncBlockToFetch().isEmpty()
                  && peer.getUnfetchSyncNum() == 0
                  && peer.getBlockInProc().isEmpty()) { //send sync to let peer know we are sync.
                needFetchAgain.offer(peer);
              }
            }

          }
        });

    needSync.forEach(peer -> startSyncWithPeer(peer));
    needFetchAgain.forEach(peer -> syncNextBatchChainIds(peer));
  }

  private void onHandleTransactionMessage(PeerConnection peer, TransactionMessage trxMsg) {
    //logger.info("on handle transaction message");
    try {
      if (!peer.getAdvObjWeRequested().containsKey(trxMsg.getMessageId())) {
        throw new TraitorPeerException("We don't send fetch request to" + peer);
      } else {
        peer.getAdvObjWeRequested().remove(trxMsg.getMessageId());
        del.handleTransaction(trxMsg.getTransactionCapsule());
      }
    } catch (TraitorPeerException e) {
      banTraitorPeer(peer);
    } catch (BadTransactionException e) {
      badAdvObj.put(trxMsg.getMessageId(), System.currentTimeMillis());
    }
  }

  private void onHandleSyncBlockChainMessage(PeerConnection peer, SyncBlockChainMessage syncMsg) {
    //logger.info("on handle sync block chain message");
    peer.setTronState(TronState.SYNCING);
    LinkedList<BlockId> blockIds;
    List<BlockId> summaryChainIds = syncMsg.getBlockIds();
    long remainNum = 0;
    try {
      blockIds = del.getLostBlockIds(summaryChainIds);
    } catch (UnReachBlockException e) {
      //TODO: disconnect this peer casue this peer can not switch
      logger.debug(e.getMessage(), e);
      return;
    }

    if (blockIds.isEmpty()) {
      peer.setNeedSyncFromUs(false);
    } else if (blockIds.size() == 1
        && !summaryChainIds.isEmpty()
        && summaryChainIds.contains(blockIds.peekFirst())) {
      peer.setNeedSyncFromUs(false);
    } else {
      peer.setNeedSyncFromUs(true);
      remainNum = del.getHeadBlockId().getNum() - blockIds.peekLast().getNum();
    }

    if (!peer.isNeedSyncFromPeer()
        && !summaryChainIds.isEmpty()
        && !del.contain(Iterables.getLast(summaryChainIds), MessageTypes.BLOCK)) {
      startSyncWithPeer(peer);
    }

    peer.sendMessage(new ChainInventoryMessage(blockIds, remainNum));
  }

  private void onHandleFetchDataMessage(PeerConnection peer, FetchInvDataMessage fetchInvDataMsg) {
    logger.info("on handle fetch block message");
    MessageTypes type = fetchInvDataMsg.getInvMessageType();

    //TODO:maybe can use message cache here
    final BlockCapsule[] blocks = {del.getGenesisBlock()};
    //get data and send it one by one
    fetchInvDataMsg.getHashList()
        .forEach(hash -> {
          if (del.contain(hash, type)) {
            Message msg = del.getData(hash, type);
            if (type.equals(MessageTypes.BLOCK)) {
              blocks[0] = ((BlockMessage) msg).getBlockCapsule();
            }
            peer.sendMessage(msg);
          } else {
            peer.sendMessage(new ItemNotFound());
          }
        });

    if (blocks[0] != null) {
      peer.setHeadBlockWeBothHave(blocks[0].getBlockId());
      peer.setHeadBlockTimeWeBothHave(blocks[0].getTimeStamp());
    }
  }

  private void banTraitorPeer(PeerConnection peer) {
    disconnectPeer(peer, ReasonCode.BAD_PROTOCOL); //TODO: ban it
  }

  private void onHandleChainInventoryMessage(PeerConnection peer, ChainInventoryMessage msg) {
    //logger.info("on handle block chain inventory message");
    try {
      if (peer.getSyncChainRequested() != null) {
        //List<BlockId> blockIds = msg.getBlockIds();
        Deque<BlockId> blockIdWeGet = new LinkedList<>(msg.getBlockIds());

        //check if the peer is a traitor
        if (!blockIdWeGet.isEmpty()) {
          long num = blockIdWeGet.peek().getNum();
          for (BlockId id : blockIdWeGet) {
            if (id.getNum() != num++) {
              throw new TraitorPeerException("We get a not continuous block inv from " + peer);
            }
          }

          if (peer.getSyncChainRequested().getKey().isEmpty()) {
            if (blockIdWeGet.peek().getNum() != 1) {
              throw new TraitorPeerException(
                  "We want a block inv starting from beginning from " + peer);
            }
          } else {
            boolean isFound = false;
            for (BlockId id :
                blockIdWeGet) {
              if (id.equals(blockIdWeGet.peek())) {
                isFound = true;
              }
            }
            if (!isFound) {
              throw new TraitorPeerException("We get a unlinked block chain from " + peer);
            }
          }
        }
        //check finish

        //here this peer's answer is legal
        peer.setSyncChainRequested(null);
        if (msg.getRemainNum() == 0
            && (blockIdWeGet.isEmpty()
            || (blockIdWeGet.size() == 1
            && del.containBlock(blockIdWeGet.peek())))
            && peer.getSyncBlockToFetch().isEmpty()
            && peer.getUnfetchSyncNum() == 0) {
          peer.setNeedSyncFromPeer(false);
          unSyncNum = getUnSyncNum();
          if (unSyncNum == 0) {
            del.syncToCli(0);
          }
          //TODO: check whole sync status and notify del sync status.
          //TODO: if sync finish call del.syncToCli();
          return;
        }

        if (!blockIdWeGet.isEmpty() && peer.getSyncBlockToFetch().isEmpty()) {
          boolean isFound = false;

          for (PeerConnection peerToCheck :
              getActivePeer()) {
            if (!peerToCheck.equals(peer)
                && !peerToCheck.getSyncBlockToFetch().isEmpty()
                && peerToCheck.getSyncBlockToFetch().peekFirst()
                .equals(blockIdWeGet.peekFirst())) {
              isFound = true;
              break;
            }
          }

          if (!isFound) {
            while (!blockIdWeGet.isEmpty()
                && del.containBlock(blockIdWeGet.peek())) {
              peer.setHeadBlockWeBothHave(blockIdWeGet.peek());
              peer.setHeadBlockTimeWeBothHave(del.getBlockTime(blockIdWeGet.peek()));
              blockIdWeGet.poll();
            }
          }
        } else if (!blockIdWeGet.isEmpty()) {
          while (!peer.getSyncBlockToFetch().isEmpty()) {
            if (!peer.getSyncBlockToFetch().peekLast().equals(blockIdWeGet.peekFirst())) {
              blockIdWeGet.pop();
            } else {
              break;
            }
          }
          if (peer.getSyncBlockToFetch().isEmpty()) {
            updateBlockWeBothHave(peer, ((BlockMessage) del.getData(blockIdWeGet.peek(), MessageTypes.BLOCK)).getBlockCapsule());
          }
          //poll the block we both have.
          blockIdWeGet.pop();
        }

        //sew it
        peer.getSyncBlockToFetch().addAll(blockIdWeGet);
        peer.setUnfetchSyncNum(msg.getRemainNum());

        long newUnSyncNum = getUnSyncNum();
        if (unSyncNum != newUnSyncNum) {
          unSyncNum = newUnSyncNum;
          del.syncToCli(unSyncNum);
        }

        if (msg.getRemainNum() == 0) {
          if (!peer.getSyncBlockToFetch().isEmpty()) {
            startFetchSyncBlock();
          } else {
            //let peer know we are sync.
            syncNextBatchChainIds(peer);
          }
        } else {
          if (peer.getSyncBlockToFetch().size() > NodeConstant.SYNC_FETCH_BATCH_NUM) {
            //one batch by one batch.
            startFetchSyncBlock();
          } else {
            syncNextBatchChainIds(peer);
          }
        }

        //TODO: check head block time is legal here
        //TODO: refresh sync status to cli. call del.syncToCli() here

      } else {
        throw new TraitorPeerException("We don't send sync request to " + peer);
      }

    } catch (TraitorPeerException e) {
      banTraitorPeer(peer);
    }
  }

  private void startFetchItem() {

  }

  private long getUnSyncNum() {
    if (getActivePeer().isEmpty()) {
      return 0;
    }
    return getActivePeer().stream()
        .mapToLong(peer -> peer.getUnfetchSyncNum() + peer.getSyncBlockToFetch().size())
        .max()
        .getAsLong();
  }

  private synchronized void startFetchSyncBlock() {
    //TODO: check how many block is processing and decide if fetch more
    HashMap<PeerConnection, List<BlockId>> send = new HashMap<>();
    HashSet<BlockId> request = new HashSet<>();

    getActivePeer().stream()
        .filter(peer -> peer.isNeedSyncFromPeer() && !peer.isBusy())
        .forEach(peer -> {
          if (!send.containsKey(peer)) { //TODO: Attention multi thread here
            send.put(peer, new LinkedList<>());
          }
          for (BlockId blockId :
              peer.getSyncBlockToFetch()) {
            if (!request.contains(blockId) //TODO: clean processing block
                && !syncBlockIdWeRequested.containsKey(blockId)) {
              send.get(peer).add(blockId);
              request.add(blockId);
              //TODO: check max block num to fetch from one peer.
              //if (send.get(peer).size() > 200) { //Max Blocks peer get one time
              //  break;
              //}
            }
          }
        });

    send.forEach((peer, blockIds) -> {
      //TODO: use collector
      blockIds.forEach(blockId -> {
        syncBlockIdWeRequested.put(blockId, System.currentTimeMillis());
        peer.getSyncBlockRequested().put(blockId, System.currentTimeMillis());
      });
      List<Sha256Hash> ids = new LinkedList<>();
      ids.addAll(blockIds);
      peer.sendMessage(new FetchInvDataMessage(ids, InventoryType.BLOCK));
    });

    send.clear();
  }

  private void updateBlockWeBothHave(PeerConnection peer, BlockCapsule block) {
    peer.setHeadBlockWeBothHave(block.getBlockId());
    peer.setHeadBlockTimeWeBothHave(block.getTimeStamp());
  }

  private void onHandleBlockInventoryMessage(PeerConnection peer, BlockInventoryMessage msg) {
    logger.info("on handle advertise blocks inventory message");
    peer.cleanInvGarbage();

    //todo: check this peer's advertise history and the history of our request to this peer.
    //simple implement here first
    List<Sha256Hash> fetchList = new ArrayList<>();
    msg.getBlockIds().forEach(hash -> {
      //TODO: Check this block whether we need it,Use peer.invToUs and peer.invWeAdv.
      logger.info("We will fetch " + hash + " from " + peer);
      fetchList.add(hash);
    });
    FetchInvDataMessage fetchMsg = new FetchInvDataMessage(fetchList, InventoryType.BLOCK);
    fetchMap.put(fetchMsg.getMessageId(), peer);
    loopFetchBlocks.push(fetchMsg);
  }

//  private void startSync() {
//    mapPeer.values().forEach(this::startSyncWithPeer);
//  }

  private Collection<PeerConnection> getActivePeer() {
    return pool.getActivePeers();
  }

  private void startSyncWithPeer(PeerConnection peer) {
    peer.setNeedSyncFromPeer(true);
    peer.getSyncBlockToFetch().clear();
    peer.setUnfetchSyncNum(0);
    peer.setHeadBlockWeBothHave(del.getGenesisBlock().getBlockId());
    peer.setHeadBlockTimeWeBothHave(del.getGenesisBlock().getTimeStamp());
    peer.setBanned(false);
    syncNextBatchChainIds(peer);
  }

  private void syncNextBatchChainIds(PeerConnection peer) {
    try {
      Deque<BlockId> chainSummary =
          del.getBlockChainSummary(peer.getHeadBlockWeBothHave(),
              ((LinkedList<BlockId>) peer.getSyncBlockToFetch()));
      peer.setSyncChainRequested(
          new Pair<>((LinkedList<BlockId>) chainSummary, System.currentTimeMillis()));
      peer.sendMessage(new SyncBlockChainMessage((LinkedList<BlockId>) chainSummary));
    } catch (Exception e) { //TODO: use tron excpetion here
      logger.debug(e.getMessage(), e);
      disconnectPeer(peer, ReasonCode.BAD_PROTOCOL);//TODO: unlink?
    }

  }

  @Override
  public void onConnectPeer(PeerConnection peer) {
    //TODO:when use new p2p framework, remove this
    logger.info("start sync with::" + peer);
    peer.setTronState(TronState.START_TO_SYNC);
    peer.setConnectTime(Time.getCurrentMillis());
    startSyncWithPeer(peer);
//    if (mapPeer.containsKey(peer.getAddress())) {
//      return;
//    }
//
//    logger.info("Discover new peer:" + peer);
//    mapPeer.put(peer.getAddress(), peer);
//    if (!peer.isNeedSyncFromPeer()) {
//      startSyncWithPeer(peer);
//    }
  }

  @Override
  public void onDisconnectPeer(PeerConnection peer) {
    //TODO:when use new p2p framework, remove this
    //peer.disconnect(reason);
  }

  private void disconnectPeer(PeerConnection peer, ReasonCode reason) {
    logger.info("disconnect with " + peer.getNode().getHost());
    peer.disconnect(reason);
  }
}

