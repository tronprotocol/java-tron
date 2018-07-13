package org.tron.core.net.node;

import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.tron.core.config.Parameter.NetConstants.MAX_TRX_PER_PEER;
import static org.tron.core.config.Parameter.NetConstants.MSG_CACHE_DURATION_IN_BLOCKS;
import static org.tron.core.config.Parameter.NetConstants.NET_MAX_TRX_PER_SECOND;
import static org.tron.core.config.Parameter.NodeConstant.MAX_BLOCKS_ALREADY_FETCHED;
import static org.tron.core.config.Parameter.NodeConstant.MAX_BLOCKS_IN_PROCESS;
import static org.tron.core.config.Parameter.NodeConstant.MAX_BLOCKS_SYNC_FROM_ONE_PEER;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
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
import org.tron.common.overlay.server.Channel.TronState;
import org.tron.common.overlay.server.SyncPool;
import org.tron.common.utils.ExecutorLoop;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.SlidingWindowCounter;
import org.tron.common.utils.Time;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.config.Parameter.NodeConstant;
import org.tron.core.config.args.Args;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.BadTransactionException;
import org.tron.core.exception.NonCommonBlockException;
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
import org.tron.protos.Protocol.ReasonCode;
import org.tron.protos.Protocol.Transaction;

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

  private SlidingWindowCounter fetchWaterLine =
      new SlidingWindowCounter(BLOCK_PRODUCED_INTERVAL * MSG_CACHE_DURATION_IN_BLOCKS / 100);

  private AdvBlockDisorder advBlockDisorder = new AdvBlockDisorder();

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
          ids.forEach((key, value) -> {  //区块和交易各发一条消息，所有的区块在一条消息，所有的交易在一条消息
            if (key.equals(InventoryType.BLOCK)) { //block消息要进行一下排序
              value.sort(Comparator.comparingLong(value1 -> new BlockId(value1).getNum()));
            }
            peer.sendMessage(new InventoryMessage(value, key));
          }));
    }

    void sendFetch() {
      send.forEach((peer, ids) ->
          ids.forEach((key, value) -> {
            if (key.equals(InventoryType.BLOCK)) {
              value.sort(Comparator.comparingLong(value1 -> new BlockId(value1).getNum()));
            }
            peer.sendMessage(new FetchInvDataMessage(value, key));
          }));
    }
  }

  private ScheduledExecutorService logExecutor = Executors.newSingleThreadScheduledExecutor();

  private ExecutorService trxsHandlePool = Executors
      .newFixedThreadPool(Args.getInstance().getValidateSignThreadNum(),
          new ThreadFactoryBuilder()
              .setNameFormat("TrxsHandlePool-%d").build());

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

  private ConcurrentHashMap<Sha256Hash, PriorItem> advObjToFetch = new ConcurrentHashMap<Sha256Hash, PriorItem>();

  private ExecutorService broadPool = Executors.newFixedThreadPool(2, new ThreadFactory() {
    @Override
    public Thread newThread(Runnable r) {
      return new Thread(r, "broad-msg-");
    }
  });

  private HashMap<Sha256Hash, Long> badAdvObj = new HashMap<>(); //TODO:need auto erase oldest obj

  //blocks we requested but not received

  private Cache<BlockId, Long> syncBlockIdWeRequested = CacheBuilder.newBuilder()
      .maximumSize(10000).expireAfterWrite(1, TimeUnit.HOURS).initialCapacity(10000)
      .recordStats().build();

  private Long unSyncNum = 0L;

  private Thread handleSyncBlockLoop;

  private Map<BlockMessage, PeerConnection> blockWaitToProc = new ConcurrentHashMap<>();

  private Map<BlockMessage, PeerConnection> blockJustReceived = new ConcurrentHashMap<>();

  private ExecutorLoop<SyncBlockChainMessage> loopSyncBlockChain;

  private ExecutorLoop<FetchInvDataMessage> loopFetchBlocks;

  private ExecutorLoop<Message> loopAdvertiseInv;

  private ExecutorService handleBackLogBlocksPool = Executors.newCachedThreadPool();


  private ScheduledExecutorService fetchSyncBlocksExecutor = Executors
      .newSingleThreadScheduledExecutor();

  private ScheduledExecutorService handleSyncBlockExecutor = Executors
      .newSingleThreadScheduledExecutor();

  private ScheduledExecutorService fetchWaterLineExecutor = Executors
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
   * @param msg msg to broadcast
   */
  public void broadcast(Message msg) {
    //广播块消息或者交易消息
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

    //fetchWaterLine:
    fetchWaterLineExecutor.scheduleWithFixedDelay(() -> {
      try {
        fetchWaterLine.advance();
      } catch (Throwable t) {
        logger.error("Unhandled exception", t);
      }
    }, 1000, 100, TimeUnit.MILLISECONDS);
  }

  private void consumerAdvObjToFetch() {
    //向空闲的活跃节点进行数据抓取请求 组织请求消息
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
        advObjToFetch.remove(hash);  //过期的inv数据就不再广播请求了。
        return;
      }
      filterActivePeer.stream()
          .filter(peer -> peer.getAdvObjSpreadToUs().containsKey(hash)  //发给向我们广播过该清单的节点，有可能有多个节点广播过该清单
              && sendPackage.getSize(peer) < MAX_TRX_PER_PEER)  //每个节点的交易数量不要超过限制
          .sorted(Comparator.comparingInt(peer -> sendPackage.getSize(peer)))
          .findFirst().ifPresent(peer -> { //多个节点发送过这个清单的话 只找其中一个请求数据
        sendPackage.add(idToFetch, peer);
        peer.getAdvObjWeRequested().put(idToFetch.getItem(), now);  //记录我们向节点请求的数据
        advObjToFetch.remove(hash);  //移除要抓取的数据
      });
    });

    sendPackage.sendFetch();  //发送FETCH_INV_DATA消息
  }

  //广播Inventory消息
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
      spread.putAll(advObjToSpread);  //广播到我的Inventory消息
      advObjToSpread.clear();
    }
    getActivePeer().stream()
        .filter(peer -> !peer.isNeedSyncFromUs())  // 只向不需要从我们这里同步数据的节点广播
        .forEach(peer ->
            spread.entrySet().stream()
                .filter(idToSpread ->
                    !peer.getAdvObjSpreadToUs().containsKey(idToSpread.getKey()) //我们之间没有交流过这个id
                        && !peer.getAdvObjWeSpread().containsKey(idToSpread.getKey()))
                .forEach(idToSpread -> {
                  peer.getAdvObjWeSpread().put(idToSpread.getKey(), Time.getCurrentMillis()); // 放到我们广播的数据集合中
                  sendPackage.add(idToSpread, peer);  //广播给同一个peer同一类数据（区块或者交易）的多个id放到1条inventory消息中去
                }));
    sendPackage.sendInv();  //INVENTORY消息
  }

  private synchronized void handleSyncBlock() {
    //处理收到的区块 blockJustReceived存储收到的区块集合
    if (((ThreadPoolExecutor) handleBackLogBlocksPool).getActiveCount() > MAX_BLOCKS_IN_PROCESS) {
      logger.info("we're already processing too many blocks");
      return;
    } else if (isSuspendFetch) {
      isSuspendFetch = false;
    }

    final boolean[] isBlockProc = {true};

    while (isBlockProc[0]) {

      isBlockProc[0] = false;

      synchronized (blockJustReceived) {
        blockWaitToProc.putAll(blockJustReceived);
        blockJustReceived.clear();
      }

      blockWaitToProc.forEach((msg, peerConnection) -> {

        if (peerConnection.isDisconnect()) {
          logger.error("Peer {} is disconnect, drop block {}", peerConnection.getNode().getHost(),
              msg.getBlockId().getString());
          blockWaitToProc.remove(msg);
          syncBlockIdWeRequested.invalidate(msg.getBlockId());
          isFetchSyncActive = true;
          return;
        }

        synchronized (freshBlockId) {
          final boolean[] isFound = {false};
          getActivePeer().stream()
              .filter( // 选出当前正要获取这个区块的节点，（保证按顺序处理）
                  peer -> !peer.getSyncBlockToFetch().isEmpty() && peer.getSyncBlockToFetch().peek()
                      .equals(msg.getBlockId()))
              .forEach(peer -> {
                peer.getSyncBlockToFetch().pop();
                peer.getBlockInProc().add(msg.getBlockId()); //添加到节点的blockInProc集合 有序的
                isFound[0] = true;
              });
          if (isFound[0]) { //确实有节点提出过抓取这个区块
            blockWaitToProc.remove(msg);
            isBlockProc[0] = true;
            if (freshBlockId.contains(msg.getBlockId()) || processSyncBlock(
                msg.getBlockCapsule())) {  //处理同步的块
              finishProcessSyncBlock(msg.getBlockCapsule());
            }
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

    }
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
    //定时执行断开交互的任务 针对所有活跃节点
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
    //收到广播的inventory消息 （区块或者交易）。
    for (Sha256Hash id : msg.getHashList()) { // 遍历
      //交易信息数量较多
      if (msg.getInventoryType().equals(InventoryType.TRX) && TrxCache.getIfPresent(id) != null) {
        logger.info("{} {} from peer {} Already exist.", msg.getInventoryType(), id,
            peer.getNode().getHost());
        continue;
      }
      final boolean[] spreaded = {false};
      final boolean[] requested = {false};
      getActivePeer().forEach(p -> {
        if (p.getAdvObjWeSpread().containsKey(id)) {
          spreaded[0] = true;   // 我们向任意节点广播过这条消息
        }
        if (p.getAdvObjWeRequested().containsKey(new Item(id, msg.getInventoryType()))) {
          requested[0] = true;  // 我们向任意节点请求过这条消息
        }
      });

      if (!spreaded[0]  // 我们没有向任何节点广播过这个inv消息，并且广播给我的节点我们之间不需要相互同步
          && !peer.isNeedSyncFromPeer()
          && !peer.isNeedSyncFromUs()) {

        //avoid TRX flood attack here.
        if (msg.getInventoryType().equals(InventoryType.TRX)
            && (peer.isAdvInvFull()
            || isFlooded())) {  // 单个节点洪泛攻击或者全局洪泛攻击
          logger.warn("A peer is flooding us, stop handle inv, the peer is: " + peer);
          return;
        }

        peer.getAdvObjSpreadToUs().put(id, System.currentTimeMillis());  //记录节点广播给我们的inv数据和时间戳
        if (!requested[0]) {  //如果我们没有向任何节点请求过
          if (!badAdvObj.containsKey(id)) {
            PriorItem targetPriorItem = this.advObjToFetch.get(id); //查找是否是我们要请求的数据

            if (targetPriorItem != null) {  //别的节点广播给过我们，刷新一下时间即可
              //another peer tell this trx to us, refresh its time.
              targetPriorItem.refreshTime();
            } else { //第一次收到这个inv数据的广播  放到advObjToFetch等待去广播获取
              fetchWaterLine.increase();
              this.advObjToFetch.put(id, new PriorItem(new Item(id, msg.getInventoryType()),
                  fetchSequenceCounter.incrementAndGet()));
            }
          }
        }
      }
    }
  }

  private boolean isFlooded() {
    return fetchWaterLine.totalCount()
        > BLOCK_PRODUCED_INTERVAL * NET_MAX_TRX_PER_SECOND * MSG_CACHE_DURATION_IN_BLOCKS / 1000;
  }

  @Override
  public void syncFrom(Sha256Hash myHeadBlockHash) {
    try {
      while (getActivePeer().isEmpty()) {
        logger.info("other peer is nil, please wait ... ");
        Thread.sleep(10000L);
      }
    } catch (InterruptedException e) {
      logger.debug(e.getMessage(), e);
    }
    logger.info("wait end");
  }

  private void onHandleBlockMessage(PeerConnection peer, BlockMessage blkMsg) {
    //收到了一个区块消息。检查收到的区块是否是我们向节点请求的区块，判断节点的同步标记（如果已经发送过断开节点连接的消息的话，标记为假，即不进行同步）
    //如果同步标记为真，将区块从请求集合删掉，将blockmsg放入blockJustReceived集合中，等待处理区块的线程定时处理
    Map<Item, Long> advObjWeRequested = peer.getAdvObjWeRequested();
    Map<BlockId, Long> syncBlockRequested = peer.getSyncBlockRequested();
    BlockId blockId = blkMsg.getBlockId();
    Item item = new Item(blockId, InventoryType.BLOCK);
    boolean syncFlag = false;
    if (syncBlockRequested.containsKey(blockId)) { // 我们向这个节点发送过同步这个块的请求
      if (!peer.getSyncFlag()) {
        logger.info("Received a block {} from no need sync peer {}", blockId.getNum(),
            peer.getNode().getHost());
        return;  //不需要从这个节点进行同步了，什么都不做返回。
      }
      peer.getSyncBlockRequested().remove(blockId);
      synchronized (blockJustReceived) {
        blockJustReceived.put(blkMsg, peer);
      }
      isHandleSyncBlockActive = true;  //打开处理区块的开关，等待执行器的处理。
      syncFlag = true;  //是同步模式请求的这个块，就不要在广播模式再处理这个区块了
      if (!peer.isBusy()) {  //节点空闲
        if (peer.getUnfetchSyncNum() > 0
            && peer.getSyncBlockToFetch().size() <= NodeConstant.SYNC_FETCH_BATCH_NUM) {
          syncNextBatchChainIds(peer); //尚且有remain的数据并且当前要处理的区块数量小于2000，开启与这个节点的下一次的区块链同步
        } else {
          isFetchSyncActive = true;  //暂时不进行下一轮的区块链同步，而是开启区块抓取开关，尽快同步块
        }
      }
    }

    //是通过广播的方式发过来的Block消息，查看我们广播请求的块
    if (advObjWeRequested.containsKey(item)) { //广播请求包含这个块
      advObjWeRequested.remove(item);
      if (!syncFlag) { //不是同步模式请求的这个块，而是广播模式请求的这个块，所以需要再广播出去
        processAdvBlock(peer, blkMsg.getBlockCapsule()); //如果请求过，进行广播收块的处理
        startFetchItem();
      }
    }
  }

  /**
  * @Description: process the advertise block.
   * if the block been processed successfully, continue processed his child block if exsits.
  * @Param: * @param peer the peer who advertise the block;
  * @param block the block we receive by advertise mode;
  * @return: void
  * @Author: shydesky@gmail.com
  * @Date: 2018/7/13
  */
  private void processAdvBlock(PeerConnection peer, BlockCapsule block) {
    boolean isSuccess = true;
    while (isSuccess && block!=null) {
      isSuccess = realProcessAdvBlock(peer, block);
      if(!isSuccess){
        break;
      }

      //Find whether block's child block exists or not, if exists,handle this child block simultaneously;
      peer = advBlockDisorder.getPeer(block.getBlockId());
      block = advBlockDisorder.getBlockCapsule(block.getBlockId());
    }
  }

  /**
  * @Description: "real" ProcessAdvBlock
   * If UnLinkedBlockException because of disorder, we store the block temporarily and
   * then we can processed this block again when his parent block if processed successfully
  * @Param: * @param peer the peer who advertise the block;
   * @param block the block we receive by advertise mode;
  * @return: boolean
  * @Author: shydesky@gmail.com
  * @Date: 2018/7/13
  */
  private boolean realProcessAdvBlock(PeerConnection peer, BlockCapsule block){
    if (!freshBlockId.contains(block.getBlockId())) {
      try {
        LinkedList<Sha256Hash> trxIds = null;
        trxIds = del.handleBlock(block, false); //广播模式处理收到的块
        freshBlockId.offer(block.getBlockId());

        trxIds.forEach(trxId -> advObjToFetch.remove(trxId)); //移除掉区块中包含的交易的广播请求

        getActivePeer().stream()
            .filter(p -> p.getAdvObjSpreadToUs().containsKey(block.getBlockId()))
            .forEach(p -> updateBlockWeBothHave(p, block));  //把收到的区块作为共有的头块 是否中间有缺失或者覆盖后块的问题？

        broadcast(new BlockMessage(block));  // 广播区块消息  生产advObjToSpread
        advBlockDisorder.remove(block.getParentHash());
        return true;
      } catch (BadBlockException e) {
        logger.error("We get a bad block {}, from {}, reason is {} ",
            block.getBlockId().getString(), peer.getNode().getHost(), e.getMessage());
        badAdvObj.put(block.getBlockId(), System.currentTimeMillis());
        disconnectPeer(peer, ReasonCode.BAD_BLOCK);
      } catch (UnLinkedBlockException e) {
        logger.error("We get a unlinked block {}, from {}, head is {}",
            block.getBlockId().getString(), peer.getNode().getHost(),
            del.getHeadBlockId().getString());
        advBlockDisorder.add(block.getParentHash(), peer, block);
        //startSyncWithPeer(peer);
      } catch (NonCommonBlockException e) {
        logger.error("We get a block {} that do not have the most recent common ancestor with the main chain, from {}, reason is {} ",
            block.getBlockId().getString(), peer.getNode().getHost(), e.getMessage());
        badAdvObj.put(block.getBlockId(), System.currentTimeMillis());
        disconnectPeer(peer, ReasonCode.FORKED);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return false;
    }
    return false;
  }

  private boolean processSyncBlock(BlockCapsule block) {
    boolean isAccept = false;
    ReasonCode reason = null;
    try {
      try {
        del.handleBlock(block, true);  //同步模式处理区块
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      freshBlockId.offer(block.getBlockId());  //添加入freshBlockId集合
      logger.info("Success handle block {}", block.getBlockId().getString());
      isAccept = true;
    } catch (BadBlockException e) {
      logger.error("We get a bad block {}, reason is {} ", block.getBlockId().getString(),
          e.getMessage());
      badAdvObj.put(block.getBlockId(), System.currentTimeMillis());
      reason = ReasonCode.BAD_BLOCK;
    } catch (UnLinkedBlockException e) {
      logger.error("We get a unlinked block {}, head is {}", block.getBlockId().getString(),
          del.getHeadBlockId().getString());
      reason = ReasonCode.UNLINKABLE;
    } catch (NonCommonBlockException e) {
      logger.error("We get a block {} that do not have the most recent common ancestor with the main chain, head is {}",
          block.getBlockId().getString(),
          del.getHeadBlockId().getString());
      reason = ReasonCode.FORKED;
    }

    if (!isAccept) {  //这个区块有问题，没有被接受
      ReasonCode finalReason = reason;
      getActivePeer().stream()
          .filter(peer -> peer.getBlockInProc().contains(block.getBlockId())) // 待获取块集合包含这个区块的节点
          .forEach(peer -> disconnectPeer(peer, finalReason));  // 断开
    }
    isHandleSyncBlockActive = true;
    return isAccept;
  }

  private void finishProcessSyncBlock(BlockCapsule block) {
    // 完成处理同步区块
    getActivePeer().forEach(peer -> {
      if (peer.getSyncBlockToFetch().isEmpty()
          && peer.getBlockInProc().isEmpty()
          && !peer.isNeedSyncFromPeer()
          && !peer.isNeedSyncFromUs()) { // 没有待获取的区块也没有处理中的区块，相互不需要同步
        startSyncWithPeer(peer);  // 开始同步
      } else if (peer.getBlockInProc().remove(block.getBlockId())) {  //这个块是节点正在处理的
        updateBlockWeBothHave(peer, block);  //更新和这个节点的共同头块
        if (peer.getSyncBlockToFetch().isEmpty()) { //send sync to let peer know we are sync.
          syncNextBatchChainIds(peer);
        }
      }
    });
  }

  synchronized boolean isTrxExist(TransactionMessage trxMsg) {
    if (TrxCache.getIfPresent(trxMsg.getMessageId()) != null) {
      return true;
    }
    TrxCache.put(trxMsg.getMessageId(), trxMsg);
    return false;
  }

  private void onHandleTransactionMessage(PeerConnection peer, TransactionMessage trxMsg) {
    // 处理收到的交易信息
    try {
      Item item = new Item(trxMsg.getMessageId(), InventoryType.TRX);
      if (!peer.getAdvObjWeRequested().containsKey(item)) {
        throw new TraitorPeerException("We don't send fetch request to" + peer);
      }
      peer.getAdvObjWeRequested().remove(item);
      if (isTrxExist(trxMsg)) {
        logger.info("Trx {} from Peer {} already processed.", trxMsg.getMessageId(),
            peer.getNode().getHost());
        return;
      }
      if(del.handleTransaction(trxMsg.getTransactionCapsule())){
        broadcast(trxMsg);
      }
    } catch (TraitorPeerException e) {
      logger.error(e.getMessage());
      banTraitorPeer(peer, ReasonCode.BAD_PROTOCOL);
    } catch (BadTransactionException e) {
      badAdvObj.put(trxMsg.getMessageId(), System.currentTimeMillis());
      banTraitorPeer(peer, ReasonCode.BAD_TX);
    }
  }

  private void onHandleTransactionsMessage(PeerConnection peer, TransactionsMessage msg) {
    for (Transaction trans : msg.getTransactions().getTransactionsList()) {
      trxsHandlePool
          .submit(() -> onHandleTransactionMessage(peer, new TransactionMessage(trans)));
    }
  }

  private void onHandleSyncBlockChainMessage(PeerConnection peer, SyncBlockChainMessage syncMsg) {
    //处理收到的请求同步区块链的消息。 首先获取到消息中的区块链清单，查找该节点缺失的区块。
    peer.setTronState(TronState.SYNCING);
    LinkedList<BlockId> blockIds = new LinkedList<>();
    List<BlockId> summaryChainIds = syncMsg.getBlockIds();
    long remainNum = 0;

    try {
      blockIds = del.getLostBlockIds(summaryChainIds);  //查找节点缺失的区块
    } catch (StoreException e) {
      logger.error(e.getMessage());
    }

    if (blockIds.isEmpty()) { // 没找到缺失的块
      //你发过来的summary不空，我却没有找到你缺失的，关键是你要的块序号居然比我的固化块还要低，所以你发过来的summary一定有问题
      if (CollectionUtils.isNotEmpty(summaryChainIds) && !del
          .canChainRevoke(summaryChainIds.get(0).getNum())) {
        logger.info("Node sync block fail, disconnect peer {}, no block {}", peer,
            summaryChainIds.get(0).getString());
        peer.disconnect(ReasonCode.SYNC_FAIL);  //断开连接
        return;
      } else {
        peer.setNeedSyncFromUs(false);  //如果没有找到缺失的块，因为我们是在主链进行查找的，所以你发过来的链可能在我这里不是主链，设置你不需要从我这里进行区块同步
      }
    } else if (blockIds.size() == 1  // 只找到了一个缺失的块，并且缺失块是在你的summary清单的，或者缺失块就是创世块
        && !summaryChainIds.isEmpty()
        && (summaryChainIds.contains(blockIds.peekFirst())
        || blockIds.peek().getNum() == 0)) {
      peer.setNeedSyncFromUs(false);  //标记这个节点不需要从本节点同步
    } else {
      peer.setNeedSyncFromUs(true);  //如果是找到了缺失块的其他情况，标记这个节点需要从本节点进行同步
      remainNum = del.getHeadBlockId().getNum() - blockIds.peekLast().getNum();  //计算剩余的区块数
    }

    //TODO: need a block older than revokingDB size exception. otherwise will be a dead loop here
    //这一块儿逻辑？？
    if (!peer.isNeedSyncFromPeer() //不需要从节点同步
        && CollectionUtils.isNotEmpty(summaryChainIds) //
        && !del.contain(Iterables.getLast(summaryChainIds), MessageTypes.BLOCK)
        && del.canChainRevoke(summaryChainIds.get(0).getNum())) {
      startSyncWithPeer(peer);
    }

    peer.sendMessage(new ChainInventoryMessage(blockIds, remainNum));  //向节点发送区块链清单消息
  }

  private void onHandleFetchDataMessage(PeerConnection peer, FetchInvDataMessage fetchInvDataMsg) {
    //处理请求数据抓取消息（有可能是块数据、有可能是交易数据）
    MessageTypes type = fetchInvDataMsg.getInvMessageType();

    BlockCapsule block = null;

    List<Protocol.Transaction> transactions = Lists.newArrayList();

    int size = 0;

    for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {

      Message msg;

      //查缓存
      if (type == MessageTypes.BLOCK) {
        msg = BlockCache.getIfPresent(hash);
      } else {
        msg = TrxCache.getIfPresent(hash);
      }

      //查库
      if (msg == null) {
        msg = del.getData(hash, type);
      }

      //没查到，回送消息
      if (msg == null) {
        logger.error("fetch message {} {} failed.", type, hash);
        peer.sendMessage(new ItemNotFound());
        return;
      }

      //回送查到的数据项消息 块消息或者是交易消息
      if (type.equals(MessageTypes.BLOCK)) {
        block = ((BlockMessage) msg).getBlockCapsule();
        peer.sendMessage(msg);  //发送数据块消息
      } else {
        transactions.add(((TransactionMessage) msg).getTransactionCapsule().getInstance());
        size += ((TransactionMessage) msg).getTransactionCapsule().getInstance().getSerializedSize();
        if (transactions.size() % maxTrxsCnt == 0 || size > maxTrxsSize) {  //交易集合满了，发送一次交易数据信息
          peer.sendMessage(new TransactionsMessage(transactions));
          transactions = Lists.newArrayList();
          size = 0;
        }
      }
    }

    if (block != null) {
      updateBlockWeBothHave(peer, block);  //更新和节点的共有头块信息
    }
    if (transactions.size() > 0) { // 发送剩余的交易数据信息
      peer.sendMessage(new TransactionsMessage(transactions));
    }
  }

  private void banTraitorPeer(PeerConnection peer, ReasonCode reason) {
    disconnectPeer(peer, reason); //TODO: ban it
  }

  private void onHandleChainInventoryMessage(PeerConnection peer, ChainInventoryMessage msg) {
    //处理收到的区块清单消息。
    //blockIdWeGet = msg.getBlockIds() peer发过来的区块清单
    //remainNum = msg.getRemainNum()  peer发过来的剩余未发送的区块数量
    try {
      //验证
      if (peer.getSyncChainRequested() != null) {  //判断本地节点是否向peer发送了区块链同步请求。
        //List<BlockId> blockIds = msg.getBlockIds();
        Deque<BlockId> blockIdWeGet = new LinkedList<>(msg.getBlockIds());

        //check if the peer is a traitor
        if (!blockIdWeGet.isEmpty()) {  //检验区块的顺序
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
        if (msg.getRemainNum() == 0  //没有剩余区块需要同步
            && (blockIdWeGet.isEmpty() || (blockIdWeGet.size() == 1 && del
            .containBlock(blockIdWeGet.peek()))) // 缺失的区块是我们已经有的了
            && peer.getSyncBlockToFetch().isEmpty()  //没有需要获取的区块了
            && peer.getUnfetchSyncNum() == 0) {
          peer.setNeedSyncFromPeer(false);  //和这个节点没有剩余区块需要同步了，并且同步中的区块也没有了，标记不需要从该节点进行区块同步了
          unSyncNum = getUnSyncNum();
          if (unSyncNum == 0) {
            del.syncToCli(0);
          }
          //TODO: check whole sync status and notify del sync status.
          //TODO: if sync finish call del.syncToCli();
          return;
        }

        if (!blockIdWeGet.isEmpty() && peer.getSyncBlockToFetch().isEmpty()) {
          //如果有缺失的区块并且从这个节点待抓取的区块是空的
          boolean isFound = false;

          //判断是否从其他节点处进行着相同的区块同步。
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

          //如果并没有从其他节点获得这些块isFound为false，说明我们很可能已经有了这些块了，那么就遍历查找一下，如果有
          //就把已经有的移除掉。
          if (!isFound) {//没有从其他节点处进行区块同步
            while (!blockIdWeGet.isEmpty() && del.containBlock(blockIdWeGet.peek())) {
              updateBlockWeBothHave(peer, blockIdWeGet.peek());
              blockIdWeGet.poll(); // 从缺失清单中移除，不再获取这个块
            }
          }
        } else if (!blockIdWeGet.isEmpty()) {
          // 目前有抓取中的区块。好的情况是抓取中的最后一个和缺失的第一个是相同的，
          // 但不幸的是可能发生了切链，抓取中的区块和缺失的区块有对不上的甚至一个都对不上。
          while (!peer.getSyncBlockToFetch().isEmpty()) {
            if (!peer.getSyncBlockToFetch().peekLast().equals(blockIdWeGet.peekFirst())) {  //缺失块的集合至多只有第一个是和待抓取的区块集合是重复的
              peer.getSyncBlockToFetch().pollLast();
            } else {
              break;
            }
          }

          // 等待抓取的块都被移除了。 有可能之前已经是共有头块的数据现在也不是共有头块了，此时需要再更新一次共有头块，因此收到区块清单时候更新共有头块很重要
          if (peer.getSyncBlockToFetch().isEmpty() && del.containBlock(blockIdWeGet.peek())) {
            updateBlockWeBothHave(peer, blockIdWeGet.peek());

          }
          //poll the block we both have.
          blockIdWeGet.poll();  //移除掉缺失块的第一个
        }

        //sew it
        peer.setUnfetchSyncNum(msg.getRemainNum());  //设置余块的数量
        peer.getSyncBlockToFetch().addAll(blockIdWeGet); // 要把加工过的blockIdWeGet设置到tofetch集合
        synchronized (freshBlockId) {  // 可能从其他节点处已经获得了区块，如有则不再次获得这个区块
          while (!peer.getSyncBlockToFetch().isEmpty() && freshBlockId
              .contains(peer.getSyncBlockToFetch().peek())) {
            BlockId blockId = peer.getSyncBlockToFetch().pop();
            updateBlockWeBothHave(peer, blockId);  //更新共有头块
            logger.info("Block {} from {} is processed", blockId.getString(),
                peer.getNode().getHost());
          }
        }

        if (msg.getRemainNum() == 0 && peer.getSyncBlockToFetch().size() == 0) {
          peer.setNeedSyncFromPeer(false);  //节点没有剩余块且没有获取中的区块，设置不从这个节点同步
        }

        long newUnSyncNum = getUnSyncNum();
        if (unSyncNum != newUnSyncNum) {
          unSyncNum = newUnSyncNum;
          del.syncToCli(unSyncNum);
        }

        //优先进行区块的同步，再进行链的同步
        if (msg.getRemainNum() == 0) {
          if (!peer.getSyncBlockToFetch().isEmpty()) {
            //startFetchSyncBlock();
            isFetchSyncActive = true;  //打开执行区块同步的开关
          } else {
            //let peer know we are sync.
            syncNextBatchChainIds(peer);  //本次要同步的区块已经结束，进行下一轮链同步 这里可能是通过发一个链同步消息来告知节点同步已经完成？
          }
        } else { //有剩余区块
          if (peer.getSyncBlockToFetch().size() > NodeConstant.SYNC_FETCH_BATCH_NUM) {
            //如果要同步的区块数量大于2000 开启执行区块同步的开关 先不进行区块清单的同步
            //one batch by one batch.
            //startFetchSyncBlock();
            isFetchSyncActive = true;
          } else {//请求下一次的区块链同步
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
    //向所有节点请求区块同步
    //TODO: check how many block is processing and decide if fetch more
    HashMap<PeerConnection, List<BlockId>> send = new HashMap<>();
    HashSet<BlockId> request = new HashSet<>();

    getActivePeer().stream()
        .filter(peer -> peer.isNeedSyncFromPeer() && !peer.isBusy())  //过滤出需要进行同步的节点（有想要的区块数据且节点空闲）
        .forEach(peer -> {
          if (!send.containsKey(peer)) { //TODO: Attention multi thread here
            send.put(peer, new LinkedList<>());  //初始化send集合的key（key是peer，value是要同步的区块的list）
          }
          for (BlockId blockId :
              peer.getSyncBlockToFetch()) {  //把要同步的区块集合查出来，去掉已经发过请求的区块，或者在getSyncBlockToFetch这个方法里面获取到的重复的区块
            if (!request.contains(blockId) //TODO: clean processing block
                && (syncBlockIdWeRequested.getIfPresent(blockId) == null)) {
              send.get(peer).add(blockId);
              request.add(blockId);  //用做去重
              //TODO: check max block num to fetch from one peer.
              if (send.get(peer).size()
                  > MAX_BLOCKS_SYNC_FROM_ONE_PEER) { //Max Blocks peer get one time  //限制单个节点每次请求的区块数量
                break;
              }
            }
          }
        });

    //开始向每个节点发送FetchInvDataMessage的消息来获取block数据
    send.forEach((peer, blockIds) -> {
      //TODO: use collector
      blockIds.forEach(blockId -> {
        syncBlockIdWeRequested.put(blockId, System.currentTimeMillis());  //把请求的区块放入全局syncBlockIdWeRequested这个集合
        peer.getSyncBlockRequested().put(blockId, System.currentTimeMillis()); // 把请求的区块放入节点的请求集合
      });
      List<Sha256Hash> ids = new LinkedList<>();
      ids.addAll(blockIds);
      if (!ids.isEmpty()) {
        peer.sendMessage(new FetchInvDataMessage(ids, InventoryType.BLOCK)); //向节点发送抓块信息
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
    //进行下一轮的区块链同步
    if (peer.getSyncChainRequested() != null) {
      logger.info("Peer {} is in sync.", peer.getNode().getHost());
      return;
    }
    try {
      Deque<BlockId> chainSummary =
          del.getBlockChainSummary(peer.getHeadBlockWeBothHave(),
              peer.getSyncBlockToFetch());
      peer.setSyncChainRequested(
          new Pair<>(chainSummary, System.currentTimeMillis()));
      peer.sendMessage(new SyncBlockChainMessage((LinkedList<BlockId>) chainSummary));
    } catch (TronException e) {
      logger.error("Peer {} sync next batch chainIds failed, error: {}", peer.getNode().getHost(),
          e.getMessage());
      disconnectPeer(peer, ReasonCode.FORKED);
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

    if (!peer.getSyncBlockRequested().isEmpty()) {
      peer.getSyncBlockRequested().keySet()
          .forEach(blockId -> syncBlockIdWeRequested.invalidate(blockId));
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

  public void shutDown() {
    logExecutor.shutdown();
    trxsHandlePool.shutdown();
    disconnectInactiveExecutor.shutdown();
    cleanInventoryExecutor.shutdown();
    broadPool.shutdown();
    loopSyncBlockChain.shutdown();
    loopFetchBlocks.shutdown();
    loopAdvertiseInv.shutdown();
    handleBackLogBlocksPool.shutdown();
    fetchSyncBlocksExecutor.shutdown();
    handleSyncBlockExecutor.shutdown();
  }

  private void disconnectPeer(PeerConnection peer, ReasonCode reason) {
    peer.setSyncFlag(false);
    peer.disconnect(reason);
  }

}

