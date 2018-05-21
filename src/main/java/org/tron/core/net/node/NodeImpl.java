package org.tron.core.net.node;

import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.tron.core.config.Parameter.NetConstants.MAX_TRX_PER_PEER;
import static org.tron.core.config.Parameter.NetConstants.MSG_CACHE_DURATION_IN_BLOCKS;
import static org.tron.core.config.Parameter.NodeConstant.MAX_BLOCKS_ALREADY_FETCHED;
import static org.tron.core.config.Parameter.NodeConstant.MAX_BLOCKS_IN_PROCESS;
import static org.tron.core.config.Parameter.NodeConstant.MAX_BLOCKS_SYNC_FROM_ONE_PEER;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.netty.util.internal.ConcurrentSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javafx.util.Pair;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.message.Message;
import org.tron.common.overlay.message.ReasonCode;
import org.tron.common.overlay.server.Channel.TronState;
import org.tron.common.overlay.server.SyncPool;
import org.tron.common.utils.ExecutorLoop;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Time;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.config.Parameter.NodeConstant;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.BadTransactionException;
import org.tron.core.exception.StoreException;
import org.tron.core.exception.TraitorPeerException;
import org.tron.core.exception.TronException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.ChainInventoryMessage;
import org.tron.core.net.message.FetchInvDataMessage;
import org.tron.core.net.message.InventoryMessage;
import org.tron.core.net.message.ItemNotFound;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.SyncBlockChainMessage;
import org.tron.core.net.message.TransactionMessage;
import org.tron.core.net.message.TransactionsMessage;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerConnectionDelegate;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Inventory.InventoryType;

@Slf4j
@Component
public class NodeImpl extends PeerConnectionDelegate implements Node {

  @Autowired
  private SyncPool pool;

  private Cache<Sha256Hash, TransactionMessage> TrxCache = CacheBuilder.newBuilder()
      .maximumSize(100_000).expireAfterWrite(1, TimeUnit.HOURS).initialCapacity(100_000)
      .recordStats().build();

  private Cache<Sha256Hash, BlockMessage> BlockCache = CacheBuilder.newBuilder()
      .maximumSize(10).expireAfterWrite(60, TimeUnit.SECONDS)
      .recordStats().build();

  private int maxTrxsSize = 1_000_000;

  private int maxTrxsCnt = 100;


  @Getter
  class PriorItem implements java.lang.Comparable<PriorItem> {

    private long count;

    private Item item;

    private long time;

    public Sha256Hash getHash() {
      return item.getHash();
    }

    public InventoryType getType() {
      return item.getType();
    }

    public PriorItem(Item item, long count) {
      this.item = item;
      this.count = count;
      this.time = Time.getCurrentMillis();
    }

    public void refreshTime() {
      this.time = Time.getCurrentMillis();
    }

    @Override
    public int compareTo(final PriorItem o) {
      if (!this.item.getType().equals(o.getItem().getType())) {
        return this.item.getType().equals(InventoryType.BLOCK) ? -1 : 1;
      }
      return Long.compare(this.count, o.getCount());
    }
  }

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

    public void add(PriorItem id, PeerConnection peer) {
      if (send.containsKey(peer) && send.get(peer).containsKey(id.getType())) {
        send.get(peer).get(id.getType()).offer(id.getHash());
      } else if (send.containsKey(peer)) {
        send.get(peer).put(id.getType(), new LinkedList<>());
        send.get(peer).get(id.getType()).offer(id.getHash());
      } else {
        send.put(peer, new HashMap<>());
        send.get(peer).put(id.getType(), new LinkedList<>());
        send.get(peer).get(id.getType()).offer(id.getHash());
      }
    }

    public int getSize(PeerConnection peer) {
      if (send.containsKey(peer)) {
        return send.get(peer).values().stream().mapToInt(LinkedList::size).sum();
      }

      return 0;
    }

    void sendInv() {
      send.forEach((peer, ids) ->
          ids.forEach((key, value) -> {
            if (key.equals(InventoryType.BLOCK)) {
              value.sort(Comparator.comparingDouble(value1 -> value1.getBlockNum()));
            }
            peer.sendMessage(new InventoryMessage(value, key));
          }));
    }

    void sendFetch() {
      send.forEach((peer, ids) ->
          ids.forEach((key, value) -> {
            if (key.equals(InventoryType.BLOCK)) {
              value.sort(Comparator.comparingDouble(value1 -> value1.getBlockNum()));
            }
            peer.sendMessage(new FetchInvDataMessage(value, key));
          }));
    }
  }

  private ScheduledExecutorService logExecutor = Executors.newSingleThreadScheduledExecutor();

  //public
  //TODO:need auto erase oldest block

  private Queue<BlockId> freshBlockId = new ConcurrentLinkedQueue<BlockId>() {
    @Override
    public boolean offer(BlockId blockId) {
      if (size() > 200) {
        super.poll();
      }
      return super.offer(blockId);
    }
  };

  private ConcurrentHashMap<Sha256Hash, PeerConnection> syncMap = new ConcurrentHashMap<>();

  private ConcurrentHashMap<Sha256Hash, PeerConnection> fetchMap = new ConcurrentHashMap<>();

  private NodeDelegate del;

  private volatile boolean isAdvertiseActive;

  private volatile boolean isFetchActive;


  private ScheduledExecutorService disconnectInactiveExecutor = Executors
      .newSingleThreadScheduledExecutor();

  private ScheduledExecutorService cleanInventoryExecutor = Executors
      .newSingleThreadScheduledExecutor();

  //broadcast
  private ConcurrentHashMap<Sha256Hash, InventoryType> advObjToSpread = new ConcurrentHashMap<>();

  private HashMap<Sha256Hash, Long> advObjWeRequested = new HashMap<>();

  //private ConcurrentHashMap<Sha256Hash, InventoryType> advObjToFetch = new ConcurrentHashMap<>();

  //private ConcurrentLinkedQueue<PriorItem> advObjToFetch = new ConcurrentLinkedQueue<PriorItem>();

  private ConcurrentHashMap<Sha256Hash, PriorItem> advObjToFetch =  new ConcurrentHashMap<Sha256Hash, PriorItem>();

  private ExecutorService broadPool = Executors.newFixedThreadPool(2, new ThreadFactory() {
    @Override
    public Thread newThread(Runnable r) {
      return new Thread(r, "broad-msg-");
    }
  });

  private HashMap<Sha256Hash, Long> badAdvObj = new HashMap<>(); //TODO:need auto erase oldest obj

  //blocks we requested but not received
  private Map<BlockId, Long> syncBlockIdWeRequested = new ConcurrentHashMap<>();

  private Long unSyncNum = 0L;

  private Thread handleSyncBlockLoop;

  private Set<BlockMessage> blockWaitToProc = new ConcurrentSet<>();

  private Set<BlockMessage> blockJustReceived = new ConcurrentSet<>();

  private ExecutorLoop<SyncBlockChainMessage> loopSyncBlockChain;

  private ExecutorLoop<FetchInvDataMessage> loopFetchBlocks;

  private ExecutorLoop<Message> loopAdvertiseInv;

  private ExecutorLoop<Message> handleBacklogBlocks;

  private ExecutorService handleBackLogBlocksPool = Executors.newCachedThreadPool();


  private ScheduledExecutorService fetchSyncBlocksExecutor = Executors
      .newSingleThreadScheduledExecutor();

  private ScheduledExecutorService handleSyncBlockExecutor = Executors
      .newSingleThreadScheduledExecutor();

  private volatile boolean isHandleSyncBlockActive = false;

  private AtomicLong fetchSequenceCounter = new AtomicLong(0L);

  //private volatile boolean isHandleSyncBlockRunning = false;

  private volatile boolean isSuspendFetch = false;

  private volatile boolean isFetchSyncActive = false;

  @Override
  public void onMessage(PeerConnection peer, TronMessage msg) {
    switch (msg.getType()) {
      case BLOCK:
        onHandleBlockMessage(peer, (BlockMessage) msg);
        break;
      case TRX:
        onHandleTransactionMessage(peer, (TransactionMessage) msg);
        break;
      case TRXS:
        onHandleTransactionsMessage(peer, (TransactionsMessage) msg);
        break;
      case SYNC_BLOCK_CHAIN:
        onHandleSyncBlockChainMessage(peer, (SyncBlockChainMessage) msg);
        break;
      case FETCH_INV_DATA:
        onHandleFetchDataMessage(peer, (FetchInvDataMessage) msg);
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

  // for test only
  public void setPool(SyncPool pool) {
    this.pool = pool;
  }

  /**
   * broadcast msg.
   *
   * @param msg msg to bradcast
   */
  public void broadcast(Message msg) {
    InventoryType type;
    if (msg instanceof BlockMessage) {
      logger.info("Ready to broadcast block {}", ((BlockMessage) msg).getBlockId());
      freshBlockId.offer(((BlockMessage) msg).getBlockId());
      BlockCache.put(msg.getMessageId(), (BlockMessage) msg);
      type = InventoryType.BLOCK;
    } else if (msg instanceof TransactionMessage) {
      TrxCache.put(msg.getMessageId(), (TransactionMessage) msg);
      type = InventoryType.TRX;
    } else {
      return;
    }
    //TODO: here need to cache fresh message to let peer fetch these data not from DB
    synchronized (advObjToSpread) {
      advObjToSpread.put(msg.getMessageId(), type);
    }
  }

  @Override
  public void listen() {
    pool.init(this);
    isAdvertiseActive = true;
    isFetchActive = true;
    activeTronPump();
  }

  @Override
  public void close() {
    getActivePeer().forEach(peer -> disconnectPeer(peer, ReasonCode.REQUESTED));
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

    broadPool.submit(() -> {
      while (isAdvertiseActive) {
        consumerAdvObjToSpread();
      }
    });

    broadPool.submit(() -> {
      while (isFetchActive) {
        consumerAdvObjToFetch();
      }
    });

    //TODO: wait to refactor these threads.
    //handleSyncBlockLoop.start();

    handleSyncBlockExecutor.scheduleWithFixedDelay(() -> {
      try {
        if (isHandleSyncBlockActive) {
          isHandleSyncBlockActive = false;
          //Thread handleSyncBlockThread = new Thread(() -> handleSyncBlock());
          handleSyncBlock();
        }
      } catch (Throwable t) {
        logger.error("Unhandled exception", t);
      }
    }, 10, 1, TimeUnit.SECONDS);

    //terminate inactive loop
    disconnectInactiveExecutor.scheduleWithFixedDelay(() -> {
      try {
        disconnectInactive();
      } catch (Throwable t) {
        logger.error("Unhandled exception", t);
      }
    }, 30000, BLOCK_PRODUCED_INTERVAL / 2, TimeUnit.MILLISECONDS);

    logExecutor.scheduleWithFixedDelay(() -> {
      try {
        logNodeStatus();
      } catch (Throwable t) {
        logger.error("Exception in log worker", t);
      }
    }, 10, 10, TimeUnit.SECONDS);

    cleanInventoryExecutor.scheduleWithFixedDelay(() -> {
      try {
        getActivePeer().forEach(p -> p.cleanInvGarbage());
      } catch (Throwable t) {
        logger.error("Unhandled exception", t);
      }
    }, 2, NetConstants.MAX_INVENTORY_SIZE_IN_MINUTES / 2, TimeUnit.MINUTES);

    fetchSyncBlocksExecutor.scheduleWithFixedDelay(() -> {
      try {
        if (isFetchSyncActive) {
          if (!isSuspendFetch) {
            startFetchSyncBlock();
          } else {
            logger.debug("suspend");
          }
        }
        isFetchSyncActive = false;
      } catch (Throwable t) {
        logger.error("Unhandled exception", t);
      }
    }, 10, 1, TimeUnit.SECONDS);
  }

  private void consumerAdvObjToFetch() {
    Collection<PeerConnection> filterActivePeer = getActivePeer().stream()
        .filter(peer -> !peer.isBusy()).collect(Collectors.toList());

    if (advObjToFetch.isEmpty() || filterActivePeer.isEmpty()) {
      try {
        Thread.sleep(100);
        return;
      } catch (InterruptedException e) {
        logger.debug(e.getMessage(), e);
      }
    }
    InvToSend sendPackage = new InvToSend();
    long now = Time.getCurrentMillis();
    advObjToFetch.values().stream().sorted(PriorItem::compareTo).forEach(idToFetch -> {
      Sha256Hash hash = idToFetch.getHash();
      if (idToFetch.getTime() < now - MSG_CACHE_DURATION_IN_BLOCKS * BLOCK_PRODUCED_INTERVAL) {
        logger.info("This obj is too late to fetch: " + idToFetch);
        advObjToFetch.remove(hash);
        return;
      }
      filterActivePeer.stream()
              .filter(peer -> peer.getAdvObjSpreadToUs().containsKey(hash) && sendPackage.getSize(peer) < MAX_TRX_PER_PEER)
              .sorted(Comparator.comparingInt(peer -> sendPackage.getSize(peer)))
              .findFirst().ifPresent(peer -> {
        sendPackage.add(idToFetch, peer);
        peer.getAdvObjWeRequested().put(idToFetch.getItem(), now);
        advObjToFetch.remove(hash);
      });
    });

    sendPackage.sendFetch();
  }

  private void consumerAdvObjToSpread() {
    if (advObjToSpread.isEmpty()) {
      try {
        Thread.sleep(100);
        return;
      } catch (InterruptedException e) {
        logger.debug(e.getMessage(), e);
      }
    }
    InvToSend sendPackage = new InvToSend();
    HashMap<Sha256Hash, InventoryType> spread = new HashMap<>();
    synchronized (advObjToSpread) {
      spread.putAll(advObjToSpread);
      advObjToSpread.clear();
    }
    getActivePeer().stream()
        .filter(peer -> !peer.isNeedSyncFromUs())
        .forEach(peer ->
            spread.entrySet().stream()
                .filter(idToSpread ->
                    !peer.getAdvObjSpreadToUs().containsKey(idToSpread.getKey())
                        && !peer.getAdvObjWeSpread().containsKey(idToSpread.getKey()))
                .forEach(idToSpread -> {
                  peer.getAdvObjWeSpread().put(idToSpread.getKey(), Time.getCurrentMillis());
                  sendPackage.add(idToSpread, peer);
                }));
    sendPackage.sendInv();
  }

  private synchronized void handleSyncBlock() {
    if (((ThreadPoolExecutor) handleBackLogBlocksPool).getActiveCount() > MAX_BLOCKS_IN_PROCESS) {
      logger.info("we're already processing too many blocks");
      return;
    } else if (isSuspendFetch) {
      isSuspendFetch = false;
    }

    final boolean[] isBlockProc = {false};

    do {
      synchronized (blockJustReceived) {
        blockWaitToProc.addAll(blockJustReceived);
        //need lock here
        blockJustReceived.clear();
      }

      isBlockProc[0] = false;
      Set<BlockMessage> pool = new HashSet<>();
      pool.addAll(blockWaitToProc);
      pool.forEach(msg -> {
        final boolean[] isFound = {false};
        getActivePeer().stream()
                .filter(peer -> !peer.getSyncBlockToFetch().isEmpty() && peer.getSyncBlockToFetch().peek().equals(msg.getBlockId()))
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

      if (((ThreadPoolExecutor) handleBackLogBlocksPool).getActiveCount() > MAX_BLOCKS_IN_PROCESS) {
        logger.info("we're already processing too many blocks");
        if (blockWaitToProc.size() >= MAX_BLOCKS_ALREADY_FETCHED) {
          isSuspendFetch = true;
        }
        break;
      }

    } while (isBlockProc[0]);
  }

  private synchronized void logNodeStatus() {
    StringBuilder sb = new StringBuilder("LocalNode stats:\n");
    sb.append("============\n");

    sb.append(String.format(
        "MyHeadBlockNum: %d\n"
            + "advObjToSpread: %d\n"
            + "advObjToFetch: %d\n"
            + "advObjWeRequested: %d\n"
            + "unSyncNum: %d\n"
            + "blockWaitToProc: %d\n"
            + "blockJustReceived: %d\n"
            + "syncBlockIdWeRequested: %d\n"
            + "badAdvObj: %d\n",
        del.getHeadBlockId().getNum(),
        advObjToSpread.size(),
        advObjToFetch.size(),
        advObjWeRequested.size(),
        getUnSyncNum(),
        blockWaitToProc.size(),
        blockJustReceived.size(),
        syncBlockIdWeRequested.size(),
        badAdvObj.size()
    ));

    logger.info(sb.toString());
  }

  public synchronized void disconnectInactive() {
    //logger.debug("size of activePeer: " + getActivePeer().size());
    getActivePeer().forEach(peer -> {
      final boolean[] isDisconnected = {false};
      final ReasonCode[] reasonCode = {ReasonCode.USER_REASON};

      peer.getAdvObjWeRequested().values().stream()
          .filter(time -> time < Time.getCurrentMillis() - NetConstants.ADV_TIME_OUT)
          .findFirst().ifPresent(time -> {
        isDisconnected[0] = true;
        reasonCode[0] = ReasonCode.FETCH_FAIL;
      });

      if (!isDisconnected[0]) {
        peer.getSyncBlockRequested().values().stream()
            .filter(time -> time < Time.getCurrentMillis() - NetConstants.SYNC_TIME_OUT)
            .findFirst().ifPresent(time -> {
          isDisconnected[0] = true;
          reasonCode[0] = ReasonCode.SYNC_FAIL;
        });
      }

//    TODO:optimize here
//      if (!isDisconnected[0]) {
//        if (del.getHeadBlockId().getNum() - peer.getHeadBlockWeBothHave().getNum()
//            > 2 * NetConstants.HEAD_NUM_CHECK_TIME / ChainConstant.BLOCK_PRODUCED_INTERVAL
//            && peer.getConnectTime() < Time.getCurrentMillis() - NetConstants.HEAD_NUM_CHECK_TIME
//            && peer.getSyncBlockRequested().isEmpty()) {
//          isDisconnected[0] = true;
//        }
//      }

      if (isDisconnected[0]) {
        disconnectPeer(peer, ReasonCode.TIME_OUT);
      }
    });
  }


  private void onHandleInventoryMessage(PeerConnection peer, InventoryMessage msg) {
    for (Sha256Hash id : msg.getHashList()){
      if (msg.getInventoryType().equals(InventoryType.TRX) && TrxCache.getIfPresent(id) != null) {
        logger.info("{} {} from peer {} Already exist.", msg.getInventoryType(), id, peer.getNode().getHost());
        continue;
      }
      final boolean[] spreaded = {false};
      final boolean[] requested = {false};
      getActivePeer().forEach(p -> {
        if (p.getAdvObjWeSpread().containsKey(id)) {
          spreaded[0] = true;
        }
        if (p.getAdvObjWeRequested().containsKey(new Item(id, msg.getInventoryType()))) {
          requested[0] = true;
        }
      });

      if (!spreaded[0]
          && !peer.isNeedSyncFromPeer()
          && !peer.isNeedSyncFromUs()) {

        //avoid TRX flood attack here.
        if (msg.getInventoryType().equals(InventoryType.TRX)
            && peer.isAdvInvFull()) {
          logger.info("A peer is flooding us, stop handle inv, the peer is:" + peer);
          return;
        }

        peer.getAdvObjSpreadToUs().put(id, System.currentTimeMillis());
        if (!requested[0]) {
          if (!badAdvObj.containsKey(id)) {
            if (!advObjToFetch.contains(id)) {
              this.advObjToFetch.put(id, new PriorItem(new Item(id, msg.getInventoryType()),
                  fetchSequenceCounter.incrementAndGet()));
            } else {
              //another peer tell this trx to us, refresh its time.
              this.advObjToFetch.get(id).refreshTime();
            }
          }
        }
      }
    }
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

    Map<Item, Long> advObjWeRequested = peer.getAdvObjWeRequested();
    Map<BlockId, Long> syncBlockRequested = peer.getSyncBlockRequested();
    BlockId blockId = blkMsg.getBlockId();
    Item item = new Item(blockId, InventoryType.BLOCK);
    boolean syncFlag = false;
    if (syncBlockRequested.containsKey(blockId)) {
      if (!peer.getSyncFlag()) {
        logger.info("rcv a block {} from no need sync peer {}", blockId.getNum(), peer.getNode());
        return;
      }
      peer.getSyncBlockRequested().remove(blockId);
      syncBlockIdWeRequested.remove(blockId);
      synchronized (blockJustReceived) {
        blockJustReceived.add(blkMsg);
      }
      isHandleSyncBlockActive = true;
      syncFlag = true;
      if (!peer.isBusy()) {
        if (peer.getUnfetchSyncNum() > 0 && peer.getSyncBlockToFetch().size() <= NodeConstant.SYNC_FETCH_BATCH_NUM) {
          syncNextBatchChainIds(peer);
        } else {
          isFetchSyncActive = true;
        }
      }
    }

    if (advObjWeRequested.containsKey(item)) {
      advObjWeRequested.remove(item);
      if (!syncFlag){
        processAdvBlock(peer, blkMsg.getBlockCapsule());
        startFetchItem();
      }
    }

  }

  private void processAdvBlock(PeerConnection peer, BlockCapsule block) {
    //TODO: lack the complete flow.
    if (!freshBlockId.contains(block.getBlockId())) {
      try {
        LinkedList<Sha256Hash> trxIds = del.handleBlock(block, false);
        freshBlockId.offer(block.getBlockId());

        //remove trxs in block from fetch data.
        trxIds.forEach(trxId -> advObjToFetch.remove(trxId));

        //TODO:save message cache again.
        getActivePeer().stream()
            .filter(p -> p.getAdvObjSpreadToUs().containsKey(block.getBlockId()))
            .forEach(p -> updateBlockWeBothHave(peer, block));

        //rebroadcast
        broadcast(new BlockMessage(block));

      } catch (BadBlockException e) {
        // TODO: punish bad peer
        logger.error("We get a bad block, reason is " + e.getMessage()
            + "\n the block is" + block);
        badAdvObj.put(block.getBlockId(), System.currentTimeMillis());
      } catch (UnLinkedBlockException e) {
        //reSync
        logger.info("get a unlink block ,so start sync!");
        startSyncWithPeer(peer);
      } catch (Exception e) {
        logger.error("broadcast fail", e);
      }
    }
  }

  private void processSyncBlock(BlockCapsule block) {
    //TODO: add processing backlog cache here, use multi thread

    boolean isAccept = false;

    //TODO: reason need to organize.
    ReasonCode reason = null;

    try {
      del.handleBlock(block, true);
      freshBlockId.offer(block.getBlockId());
      isAccept = true;
    } catch (BadBlockException e) {
      // TODO: punish bad peer
      logger.error("We get a bad block, reason is " + e.getMessage()
          + "\n the block is" + block);
      badAdvObj.put(block.getBlockId(), System.currentTimeMillis());
      reason = ReasonCode.BAD_BLOCK;
    } catch (UnLinkedBlockException e) {
      //should not go here.
      logger.debug(e.getMessage(), e);
      logger.error("We get a unlinked block, we can't find this block's parent in our db\n"
          + "this block is " + block);
      reason = ReasonCode.UNLINKABLE;
      //logger.error(e.getMessage());
    }

    if (isAccept) {
      Deque<PeerConnection> needSync = new LinkedList<>();
      Deque<PeerConnection> needFetchAgain = new LinkedList<>();
      logger.info("save block num:" + block.getNum());
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
    } else {
      ReasonCode finalReason = reason;
      getActivePeer().stream()
          .filter(peer -> peer.getBlockInProc().contains(block.getBlockId()))
          .forEach(peer -> cleanUpSyncPeer(peer, finalReason));
    }

    isHandleSyncBlockActive = true;
  }

  private void cleanUpSyncPeer(PeerConnection peer, ReasonCode reasonCode) {
    peer.setSyncFlag(false);
    while (!peer.getSyncBlockToFetch().isEmpty()) {
      BlockId blockId = peer.getSyncBlockToFetch().pop();
      removeTheBlockMessage(blockWaitToProc, blockId);
      removeTheBlockMessage(blockJustReceived, blockId);
    }
    disconnectPeer(peer, reasonCode);
  }

  private void removeTheBlockMessage(Set<BlockMessage> blockMessages, BlockId targetBlockId) {
    for (Iterator<BlockMessage> iterator = blockMessages.iterator(); iterator.hasNext(); ) {
      if (iterator.next().getBlockId().equals(targetBlockId)) {
        iterator.remove();
      }
    }
  }

  synchronized boolean isTrxExist(TransactionMessage trxMsg){
    if (TrxCache.getIfPresent(trxMsg.getMessageId()) != null){
      return true;
    }
    TrxCache.put(trxMsg.getMessageId(), trxMsg);
    return false;
  }

  private void onHandleTransactionMessage(PeerConnection peer, TransactionMessage trxMsg) {
    try {
      Item item = new Item(trxMsg.getMessageId(), InventoryType.TRX);
      if (!peer.getAdvObjWeRequested().containsKey(item)) {
        throw new TraitorPeerException("We don't send fetch request to" + peer);
      }
      peer.getAdvObjWeRequested().remove(item);
      if (isTrxExist(trxMsg)){
        logger.info("Trx {} from Peer {} already processed.", trxMsg.getMessageId(), peer.getNode().getHost());
        return;
      }
      del.handleTransaction(trxMsg.getTransactionCapsule());
      broadcast(trxMsg);
    } catch (TraitorPeerException e) {
      logger.error(e.getMessage());
      banTraitorPeer(peer, ReasonCode.BAD_PROTOCOL);
    } catch (BadTransactionException e) {
      badAdvObj.put(trxMsg.getMessageId(), System.currentTimeMillis());
      banTraitorPeer(peer, ReasonCode.BAD_TX);
    }
  }

  private void onHandleTransactionsMessage(PeerConnection peer, TransactionsMessage msg) {
    msg.getTransactions().getTransactionsList().forEach(transaction ->
        onHandleTransactionMessage(peer, new TransactionMessage(transaction)));
  }

  private void onHandleSyncBlockChainMessage(PeerConnection peer, SyncBlockChainMessage syncMsg) {
    peer.setTronState(TronState.SYNCING);
    LinkedList<BlockId> blockIds = new LinkedList<>();
    List<BlockId> summaryChainIds = syncMsg.getBlockIds();
    long remainNum = 0;

    try {
      blockIds = del.getLostBlockIds(summaryChainIds);
    } catch (StoreException e) {
      logger.error(e.getMessage());
    }

    if (blockIds.isEmpty()) {
      if (CollectionUtils.isNotEmpty(summaryChainIds) && !del.canChainRevoke(summaryChainIds.get(0).getNum())) {
        logger.info("Node sync block fail, disconnect peer {}, no block {}", peer, summaryChainIds.get(0).getString());
        peer.disconnect(ReasonCode.SYNC_FAIL);
        return;
      } else {
        peer.setNeedSyncFromUs(false);
      }
    } else if (blockIds.size() == 1
        && !summaryChainIds.isEmpty()
        && (summaryChainIds.contains(blockIds.peekFirst())
        || blockIds.peek().getNum() == 0)) {
      peer.setNeedSyncFromUs(false);
    } else {
      peer.setNeedSyncFromUs(true);
      remainNum = del.getHeadBlockId().getNum() - blockIds.peekLast().getNum();
    }

    //TODO: need a block older than revokingDB size exception. otherwise will be a dead loop here
    if (!peer.isNeedSyncFromPeer()
        && CollectionUtils.isNotEmpty(summaryChainIds)
        && !del.contain(Iterables.getLast(summaryChainIds), MessageTypes.BLOCK)
        && del.canChainRevoke(summaryChainIds.get(0).getNum())) {
      startSyncWithPeer(peer);
    }

    peer.sendMessage(new ChainInventoryMessage(blockIds, remainNum));
  }

  private void onHandleFetchDataMessage(PeerConnection peer, FetchInvDataMessage fetchInvDataMsg) {

    MessageTypes type = fetchInvDataMsg.getInvMessageType();

    BlockCapsule block = null;

    List<Protocol.Transaction> transactions = Lists.newArrayList();

    int size = 0;

    for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {

      Message msg;

      if (type == MessageTypes.BLOCK) {
        msg = BlockCache.getIfPresent(hash);
      } else {
        msg = TrxCache.getIfPresent(hash);
      }

      if (msg == null) {
        msg = del.getData(hash, type);
      }

      if (msg == null) {
        logger.error("fetch message {} {} failed.", type, hash);
        peer.sendMessage(new ItemNotFound());
        return;
      }

      if (type.equals(MessageTypes.BLOCK)) {
        block = ((BlockMessage) msg).getBlockCapsule();
        peer.sendMessage(msg);
      } else {
        transactions.add(((TransactionMessage) msg).getTransaction());
        size += ((TransactionMessage) msg).getTransaction().getSerializedSize();
        if (transactions.size() % maxTrxsCnt == 0 || size > maxTrxsSize) {
          peer.sendMessage(new TransactionsMessage(transactions));
          transactions = Lists.newArrayList();
          size = 0;
        }
      }
    }

    if (block != null) {
      updateBlockWeBothHave(peer, block);
    }
    if (transactions.size() > 0) {
      peer.sendMessage(new TransactionsMessage(transactions));
    }
  }

  private void banTraitorPeer(PeerConnection peer, ReasonCode reason) {
    disconnectPeer(peer, reason); //TODO: ban it
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
            if (!peer.getSyncChainRequested().getKey().contains(blockIdWeGet.peek())) {
              throw new TraitorPeerException(String.format(
                  "We get a unlinked block chain from " + peer
                      + "\n Our head is " + peer.getSyncChainRequested().getKey().getLast()
                      .getString()
                      + "\n Peer give us is " + blockIdWeGet.peek().getString()));
            }
          }

          if (del.getHeadBlockId().getNum() > 0) {
            long maxRemainTime = ChainConstant.CLOCK_MAX_DELAY + System.currentTimeMillis() - del
                .getBlockTime(del.getSolidBlockId());
            long maxFutureNum =
                maxRemainTime / BLOCK_PRODUCED_INTERVAL + del.getSolidBlockId()
                    .getNum();
            if (blockIdWeGet.peekLast().getNum() + msg.getRemainNum() > maxFutureNum) {
              throw new TraitorPeerException(
                  "Block num " + blockIdWeGet.peekLast().getNum() + "+" + msg.getRemainNum()
                      + " is gt future max num " + maxFutureNum + " from " + peer);
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
              updateBlockWeBothHave(peer, blockIdWeGet.peek());
              blockIdWeGet.poll();
            }
          }
        } else if (!blockIdWeGet.isEmpty()) {
          while (!peer.getSyncBlockToFetch().isEmpty()) {
            if (!peer.getSyncBlockToFetch().peekLast().equals(blockIdWeGet.peekFirst())) {
              peer.getSyncBlockToFetch().pollLast();
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
        peer.setUnfetchSyncNum(msg.getRemainNum());
        if (msg.getRemainNum() == 0 && peer.getSyncBlockToFetch().size() == 0){
          peer.setNeedSyncFromPeer(false);
        }

        long newUnSyncNum = getUnSyncNum();
        if (unSyncNum != newUnSyncNum) {
          unSyncNum = newUnSyncNum;
          del.syncToCli(unSyncNum);
        }

        if (msg.getRemainNum() == 0) {
          if (!peer.getSyncBlockToFetch().isEmpty()) {
            //startFetchSyncBlock();
            isFetchSyncActive = true;
          } else {
            //let peer know we are sync.
            syncNextBatchChainIds(peer);
          }
        } else {
          if (peer.getSyncBlockToFetch().size() > NodeConstant.SYNC_FETCH_BATCH_NUM) {
            //one batch by one batch.
            //startFetchSyncBlock();
            isFetchSyncActive = true;
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
      logger.error(e.getMessage());
      banTraitorPeer(peer, ReasonCode.BAD_PROTOCOL);
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
                && !syncBlockIdWeRequested.containsKey(blockId)
                && blockWaitToProc.stream()
                .noneMatch(blockMessage -> blockMessage.getBlockId().equals(blockId))
                && blockJustReceived.stream()
                .noneMatch(blockMessage -> blockMessage.getBlockId().equals(blockId))) {
              send.get(peer).add(blockId);
              request.add(blockId);
              //TODO: check max block num to fetch from one peer.
              if (send.get(peer).size()
                  > MAX_BLOCKS_SYNC_FROM_ONE_PEER) { //Max Blocks peer get one time
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
      if (!ids.isEmpty()) {
        peer.sendMessage(new FetchInvDataMessage(ids, InventoryType.BLOCK));
      }
    });

    send.clear();
  }

  private void updateBlockWeBothHave(PeerConnection peer, BlockCapsule block) {
    logger.info("update peer {} block both we have {}", peer.getNode().getHost(),
        block.getBlockId().getString());
    peer.setHeadBlockWeBothHave(block.getBlockId());
    peer.setHeadBlockTimeWeBothHave(block.getTimeStamp());
  }

  private void updateBlockWeBothHave(PeerConnection peer, BlockId blockId) {
    logger.info("update peer {} block both we have, {}", peer.getNode().getHost(),
        blockId.getString());
    peer.setHeadBlockWeBothHave(blockId);
    long time = ((BlockMessage) del.getData(blockId, MessageTypes.BLOCK)).getBlockCapsule()
        .getTimeStamp();
    peer.setHeadBlockTimeWeBothHave(time);
  }

  private void startSync() {
    getActivePeer().forEach(this::startSyncWithPeer);
  }

  private Collection<PeerConnection> getActivePeer() {
    return pool.getActivePeers();
  }

  private void startSyncWithPeer(PeerConnection peer) {
    peer.setNeedSyncFromPeer(true);
    peer.getSyncBlockToFetch().clear();
    peer.setUnfetchSyncNum(0);
    updateBlockWeBothHave(peer, del.getGenesisBlock());
    peer.setBanned(false);
    syncNextBatchChainIds(peer);
  }

  private void syncNextBatchChainIds(PeerConnection peer) {
    if (peer.getSyncChainRequested() != null) {
      logger.info("peer {}:{} is in sync.", peer.getNode().getHost(), peer.getNode().getPort());
      return;
    }
    try {
      Deque<BlockId> chainSummary =
          del.getBlockChainSummary(peer.getHeadBlockWeBothHave(),
              peer.getSyncBlockToFetch());
      peer.setSyncChainRequested(
          new Pair<>(chainSummary, System.currentTimeMillis()));
      peer.sendMessage(new SyncBlockChainMessage((LinkedList<BlockId>) chainSummary));
    } catch (TronException e) { //TODO: use tron excpetion here
      logger.info(e.getMessage());
      logger.debug(e.getMessage(), e);
      disconnectPeer(peer, ReasonCode.FORKED);//TODO: unlink?
    }
  }

  @Override
  public void onConnectPeer(PeerConnection peer) {
    if (peer.getHelloMessage().getHeadBlockId().getNum() > del.getHeadBlockId().getNum()) {
      peer.setTronState(TronState.SYNCING);
      startSyncWithPeer(peer);
    } else {
      peer.setTronState(TronState.SYNC_COMPLETED);
    }
  }

  @Override
  public void onDisconnectPeer(PeerConnection peer) {
    //TODO:when use new p2p framework, remove this

    if (!peer.getSyncBlockRequested().isEmpty()) {
      peer.getSyncBlockRequested().keySet()
          .forEach(blockId -> syncBlockIdWeRequested.remove(blockId));
      isFetchSyncActive = true;
    }

    if (!peer.getAdvObjWeRequested().isEmpty()) {
      peer.getAdvObjWeRequested().keySet()
          .forEach(item -> {
            if (getActivePeer().stream()
                .filter(peerConnection -> !peerConnection.equals(peer))
                .filter(peerConnection -> peerConnection.getInvToUs().contains(item.getHash()))
                .findFirst()
                .isPresent()) {
              advObjToFetch.put(item.getHash(), new PriorItem(item,
                  fetchSequenceCounter.incrementAndGet()));
            }
          });
    }
  }

  private void disconnectPeer(PeerConnection peer, ReasonCode reason) {
    logger.info("disconnect with " + peer.getNode().getHost() + "|| reason:" + reason);
    // if we had requested any sync or regular items from this peer that we haven't
    // received yet, reschedule them to be fetched from another peer
    peer.disconnect(reason);
  }

}

