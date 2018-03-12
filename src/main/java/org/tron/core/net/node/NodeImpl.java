package org.tron.core.net.node;

import io.scalecube.transport.Address;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.overlay.node.GossipLocalNode;
import org.tron.common.utils.ExecutorLoop;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.BadTransactionException;
import org.tron.core.exception.TraitorPeerException;
import org.tron.core.exception.UnReachBlockException;
import org.tron.core.net.message.BlockInventoryMessage;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.ChainInventoryMessage;
import org.tron.core.net.message.FetchInvDataMessage;
import org.tron.core.net.message.InventoryMessage;
import org.tron.core.net.message.ItemNotFound;
import org.tron.core.net.message.Message;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.SyncBlockChainMessage;
import org.tron.core.net.message.TransactionMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerConnectionDelegate;
import org.tron.protos.Protocol.Inventory.InventoryType;


public class NodeImpl extends PeerConnectionDelegate implements Node {

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

  private HashMap<Address, PeerConnection> mapPeer = new HashMap<>();

  private final List<Sha256Hash> trxToAdvertise = new ArrayList<>();

  private final List<BlockId> blockToAdvertise = new ArrayList<>();

  private static final Logger logger = LoggerFactory.getLogger("Node");

  //public
  //TODO:need auto erase oldest block
  private Queue<BlockId> freshBlockId = new LinkedBlockingQueue<>();

  private ConcurrentHashMap<Sha256Hash, PeerConnection> syncMap = new ConcurrentHashMap<>();

  private ConcurrentHashMap<Sha256Hash, PeerConnection> fetchMap = new ConcurrentHashMap<>();

  private NodeDelegate del;

  private GossipLocalNode gossipNode;

  private volatile boolean isAdvertiseActive;

  private volatile boolean isFetchActive;


  //broadcast
  private HashMap<Sha256Hash, InventoryType> advObjToSpread = new HashMap<>();

  private HashMap<Sha256Hash, Long> advObjWeRequested = new HashMap<>();

  private HashMap<Sha256Hash, InventoryType> advObjToFetch = new HashMap<>();

  private Thread advertiseLoopThread;

  private Thread advObjFetchLoopThread;

  private HashMap<Sha256Hash, Long> badAdvObj = new HashMap<>(); //TODO:need auto erase oldest obj

  //sync
  private HashMap<BlockId, Long> syncBlockIdWeRequested = new HashMap<>();

  ExecutorLoop<SyncBlockChainMessage> loopSyncBlockChain;

  ExecutorLoop<FetchInvDataMessage> loopFetchBlocks;

  ExecutorLoop<Message> loopAdvertiseInv;

  @Override
  public void onMessage(PeerConnection peer, Message msg) {
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
    gossipNode = GossipLocalNode.getInstance();
    gossipNode.setPeerDel(this);
    gossipNode.start();
    isAdvertiseActive = true;
    isFetchActive = true;
  }

  @Override
  public void close() throws InterruptedException {
    gossipNode.stop();
    loopFetchBlocks.join();
    loopSyncBlockChain.join();
    loopAdvertiseInv.join();
    isAdvertiseActive = false;
    isFetchActive = true;
    advertiseLoopThread.join();
    advObjFetchLoopThread.join();
  }

  @Override
  public void connectToP2PNetWork() {

    // broadcast inv
    loopAdvertiseInv = new ExecutorLoop<>(2, 10, b -> {
      logger.info("loop advertise inv");
      for (PeerConnection peer : mapPeer.values()) {
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
      logger.info("loop sync block chain");
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
            e.printStackTrace();
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
                      peer.getAdvObjWeSpread().put(idToSpread.getKey(), System.currentTimeMillis());
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
            e.printStackTrace();
          }
        }

        synchronized (advObjToFetch) {
          InvToSend sendPackage = new InvToSend();
          advObjToFetch.entrySet()
              .forEach(idToFetch -> {
                for (PeerConnection peer :
                    getActivePeer()) {
                  //TODO: don't fetch too much obj from only one peer
                  if (peer.getAdvObjSpreadToUs().containsKey(idToFetch.getKey())) {
                    sendPackage.add(idToFetch, peer);
                    advObjToFetch.remove(idToFetch.getKey());
                    peer.getAdvObjWeRequested().put(idToFetch.getKey(), System.currentTimeMillis());
                    break;
                  }
                }
              });
          sendPackage.sendFetch();
        }
      }
    });

    advertiseLoopThread.start();
    advObjFetchLoopThread.start();
  }

  private void onHandleInventoryMessage(PeerConnection peer, InventoryMessage msg) {
    logger.info("on handle advertise inventory message");
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
      while (mapPeer.isEmpty()) {
        logger.info("other peer is nil, please wait ... ");
        Thread.sleep(10000L);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    //loopSyncBlockChain.push(new SyncBlockChainMessage(hashList));
  }


  private void onHandleBlockMessage(PeerConnection peer, BlockMessage blkMsg) {
    logger.info("on handle block message");
    //peer.setLastBlockPeerKnow((BlockId) blkMsg.getMessageId());

    if (peer.getAdvObjWeRequested().containsKey(blkMsg.getBlockId())) {
      //broadcast mode
      peer.getAdvObjWeRequested().remove(blkMsg.getBlockId());
      processAdvBlock(peer, blkMsg.getBlockCapsule());
      startFetchItem();
    } else if (peer.getSyncBlockRequested().containsKey(blkMsg.getBlockId())) {
      //sync mode
      peer.getSyncBlockRequested().remove(blkMsg.getBlockId());
      peer.getSyncBlockToFetch().remove(blkMsg.getBlockId());
      syncBlockIdWeRequested.remove(blkMsg.getBlockId());
      processSyncBlock(blkMsg.getBlockCapsule());
      if (peer.isNeedSyncFromPeer()) {
        syncNextBatchChainIds(peer);
      }
      startFetchSyncBlock();
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
        broadcast(new BlockMessage(block));

      } catch (BadBlockException e) {
        badAdvObj.put(block.getBlockId(), System.currentTimeMillis());
      }  //TODO:unlinked block and call startSyncWithPeer(peer);

    }
  }

  private void processSyncBlock(BlockCapsule block) {
    //TODO: add processing backlog cache here, use multi thread

    getActivePeer()
        .forEach(peer -> {
          if (!peer.getSyncBlockToFetch().isEmpty()
              && peer.getSyncBlockToFetch().peek().equals(block.getBlockId())) {
            peer.getSyncBlockToFetch().poll();
          }
        });

    if (freshBlockId.contains(block.getBlockId())) {
      return;
    }

    try {
      del.handleBlock(block, true);
      freshBlockId.offer(block.getBlockId());
    } catch (BadBlockException e) {
      badAdvObj.put(block.getBlockId(), System.currentTimeMillis());
    }

    Deque<PeerConnection> needSync = new LinkedList<>();
    Deque<PeerConnection> needFetchAgain = new LinkedList<>();

    getActivePeer()
        .forEach(peer -> {
          if (peer.getSyncBlockToFetch().isEmpty() //TODO: need process here
              && !peer.isNeedSyncFromPeer()
              && !peer.isNeedSyncFromUs()) {
            needSync.offer(peer);
          } else {
            //TODO: erase process here
            peer.setHeadBlockTimeWeBothHave(block.getTimeStamp());
            peer.setHeadBlockWeBothHave(block.getBlockId());
            if (peer.getSyncBlockToFetch().isEmpty()) { //TODO: check unfetch number and process
              needFetchAgain.offer(peer);
            }
          }
        });

    needSync.forEach(peer -> startSyncWithPeer(peer));
    needFetchAgain.forEach(peer -> syncNextBatchChainIds(peer));
  }

  private void onHandleTransactionMessage(PeerConnection peer, TransactionMessage trxMsg) {
    logger.info("on handle transaction message");
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
    logger.info("on handle sync block chain message");
    List<BlockId> blockIds = new ArrayList<>();
    List<BlockId> summaryCHhainIds = syncMsg.getBlockIds();
    try {
      blockIds = del.getLostBlockIds(summaryCHhainIds);
    } catch (UnReachBlockException e) {
      e.printStackTrace();
    }

    if (blockIds.isEmpty()) {
      peer.setNeedSyncFromUs(false);
    } else { //TODO: here must check when blockIds.size == 1, it is maybe is in sync status
      peer.setNeedSyncFromUs(true);
    }

    if (!peer.isNeedSyncFromPeer()
        && !summaryCHhainIds.isEmpty()
        && !del.contain(summaryCHhainIds.get(summaryCHhainIds.size() - 1), MessageTypes.BLOCK)) {
      startSyncWithPeer(peer);
    }
    peer.sendMessage(new ChainInventoryMessage(blockIds));
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
    disconnectPeer(peer);
  }

  private void onHandleChainInventoryMessage(PeerConnection peer, ChainInventoryMessage msg) {
    logger.info("on handle block chain inventory message");
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
        if (blockIdWeGet.isEmpty() && peer.getSyncBlockToFetch().isEmpty()) {
          peer.setNeedSyncFromPeer(false);
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
            if (peer.getSyncBlockToFetch().peekLast() != blockIdWeGet.peekFirst()) {
              peer.getSyncBlockToFetch().pop();
            } else {
              break;
            }
          }
          if (peer.getSyncBlockToFetch().isEmpty()) {
            updateBlockWeBothHave(peer, blockIdWeGet.peek());
          }
          //poll the block we both have.
          blockIdWeGet.poll();
        }

        //sew it
        peer.getSyncBlockToFetch().addAll(blockIdWeGet);

        //TODO: check head block time is legal here
        //TODO: refresh sync status to cli. call del.syncToCli() here

        //TODO: depends on peer's BlockChainToFetch count and remaining block count
        //TODO: to decide to fetch again or sync, now do it together

        startFetchSyncBlock();

        //syncNextBatchChainIds(peer);


      } else {
        throw new TraitorPeerException("We don't send sync request to " + peer);
      }

    } catch (TraitorPeerException e) {
      banTraitorPeer(peer);
    }
  }

  private void startFetchItem() {

  }

  private synchronized void startFetchSyncBlock() {
    //TODO: check how many block is processing and decide if fetch more
    HashMap<PeerConnection, List<BlockId>> send = new HashMap<>();
    HashSet<BlockId> request = new HashSet<>();

    getActivePeer().stream()
        .filter(peer -> peer.isNeedSyncFromPeer())
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
              if (send.get(peer).size() > 200) { //Max Blocks peer get one time
                break;
              }
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

  private void updateBlockWeBothHave(PeerConnection peer, BlockId id) {
    peer.setHeadBlockWeBothHave(id);
    peer.setHeadBlockTimeWeBothHave(del.getBlockTime(id));
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

  private void startSync() {
    mapPeer.values().forEach(this::startSyncWithPeer);
  }

  private Collection<PeerConnection> getActivePeer() {
    //TODO: filter active peer, exclude banned, dead, traitor peers
    return mapPeer.values();
  }

  private void startSyncWithPeer(PeerConnection peer) {
    peer.setNeedSyncFromPeer(true);
    peer.getSyncBlockToFetch().clear();
    peer.setNumUnfetchBlock(0);
    peer.setHeadBlockWeBothHave(del.getGenesisBlock().getBlockId());
    peer.setHeadBlockTimeWeBothHave(del.getGenesisBlock().getTimeStamp());
    peer.setBanned(false);
    syncNextBatchChainIds(peer);
  }

  @Override
  public PeerConnection getPeer(io.scalecube.transport.Message msg) {
    return mapPeer.get(msg.sender());
  }

  private void syncNextBatchChainIds(PeerConnection peer) {
    try {
      Deque<BlockId> chainSummary =
              del.getBlockChainSummary(peer.getHeadBlockWeBothHave(),
                      ((LinkedList<BlockId>) peer.getSyncBlockToFetch()));
      peer.setSyncChainRequested(new Pair<>((LinkedList<BlockId>) chainSummary, System.currentTimeMillis()));
      peer.sendMessage(new SyncBlockChainMessage((LinkedList<BlockId>) chainSummary));
    } catch (Exception e) { //TODO: use tron excpetion here
      e.printStackTrace();
      disconnectPeer(peer);
    }

  }

  @Override
  public void connectPeer(PeerConnection peer) {
    logger.info("Discover new peer:" + peer);
    mapPeer.put(peer.getAddress(), peer);
    startSyncWithPeer(peer);
  }

  @Override
  public void disconnectPeer(PeerConnection peer) {
    mapPeer.remove(peer.getAddress());
  }
}

