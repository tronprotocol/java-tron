package org.tron.core.net.service;

import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.tron.core.config.Parameter.NetConstants.MAX_TRX_FETCH_PER_PEER;
import static org.tron.core.config.Parameter.NetConstants.MSG_CACHE_DURATION_IN_BLOCKS;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.discover.node.statistics.MessageCount;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Time;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.args.Args;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.FetchInvDataMessage;
import org.tron.core.net.message.InventoryMessage;
import org.tron.core.net.message.TransactionMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.Inventory.InventoryType;

@Slf4j(topic = "net")
@Component
public class AdvService {
  
  private final int MAX_INV_TO_FETCH_CACHE_SIZE = 100_000;
  private final int MAX_TRX_CACHE_SIZE = 50_000;
  private final int MAX_BLOCK_CACHE_SIZE = 10;
  private final int MAX_SPREAD_SIZE = 1_000;

  @Autowired
  private TronNetDelegate tronNetDelegate;

  private ConcurrentHashMap<Item, Long> invToFetch = new ConcurrentHashMap<>();

  private ConcurrentHashMap<Item, Long> invToSpread = new ConcurrentHashMap<>();

  private Cache<Item, Long> invToFetchCache = CacheBuilder.newBuilder()
      .maximumSize(MAX_INV_TO_FETCH_CACHE_SIZE).expireAfterWrite(1, TimeUnit.HOURS)
      .recordStats().build();

  private Cache<Item, Message> trxCache = CacheBuilder.newBuilder()
      .maximumSize(MAX_TRX_CACHE_SIZE).expireAfterWrite(1, TimeUnit.HOURS)
      .recordStats().build();

  private Cache<Item, Message> blockCache = CacheBuilder.newBuilder()
      .maximumSize(MAX_BLOCK_CACHE_SIZE).expireAfterWrite(1, TimeUnit.MINUTES)
      .recordStats().build();

  private ScheduledExecutorService spreadExecutor = Executors.newSingleThreadScheduledExecutor();

  private ScheduledExecutorService fetchExecutor = Executors.newSingleThreadScheduledExecutor();

  @Getter
  private MessageCount trxCount = new MessageCount();

  private boolean fastForward = Args.getInstance().isFastForward();

  public void init() {

    if (fastForward) {
      return;
    }

    spreadExecutor.scheduleWithFixedDelay(() -> {
      try {
        consumerInvToSpread();
      } catch (Exception exception) {
        logger.error("Spread thread error.", exception.getMessage());
      }
    }, 100, 30, TimeUnit.MILLISECONDS);

    fetchExecutor.scheduleWithFixedDelay(() -> {
      try {
        consumerInvToFetch();
      } catch (Exception exception) {
        logger.error("Fetch thread error.", exception.getMessage());
      }
    }, 100, 30, TimeUnit.MILLISECONDS);
  }

  public void close() {
    spreadExecutor.shutdown();
    fetchExecutor.shutdown();
  }

  public synchronized void addInvToCache(Item item) {
    invToFetchCache.put(item, System.currentTimeMillis());
    invToFetch.remove(item);
  }

  public synchronized boolean addInv(Item item) {

    if (fastForward && item.getType().equals(InventoryType.TRX)) {
      return false;
    }

    if (invToFetchCache.getIfPresent(item) != null) {
      return false;
    }

    if (item.getType().equals(InventoryType.TRX)) {
      if (trxCache.getIfPresent(item) != null) {
        return false;
      }
    } else {
      if (blockCache.getIfPresent(item) != null) {
        return false;
      }
    }

    invToFetchCache.put(item, System.currentTimeMillis());
    invToFetch.put(item, System.currentTimeMillis());

    if (InventoryType.BLOCK.equals(item.getType())) {
      consumerInvToFetch();
    }

    return true;
  }

  public Message getMessage(Item item) {
    if (item.getType() == InventoryType.TRX) {
      return trxCache.getIfPresent(item);
    } else {
      return blockCache.getIfPresent(item);
    }
  }

  public void broadcast(Message msg) {

    if (fastForward) {
      return;
    }

    if (invToSpread.size() > MAX_SPREAD_SIZE) {
      logger.warn("Drop message, type: {}, ID: {}.", msg.getType(), msg.getMessageId());
      return;
    }

    Item item;
    if (msg instanceof BlockMessage) {
      BlockMessage blockMsg = (BlockMessage) msg;
      item = new Item(blockMsg.getMessageId(), InventoryType.BLOCK);
      logger.info("Ready to broadcast block {}", blockMsg.getBlockId().getString());
      blockMsg.getBlockCapsule().getTransactions().forEach(transactionCapsule -> {
        Sha256Hash tid = transactionCapsule.getTransactionId();
        invToSpread.remove(tid);
        trxCache.put(new Item(tid, InventoryType.TRX),
            new TransactionMessage(transactionCapsule.getInstance()));
      });
      blockCache.put(item, msg);
    } else if (msg instanceof TransactionMessage) {
      TransactionMessage trxMsg = (TransactionMessage) msg;
      item = new Item(trxMsg.getMessageId(), InventoryType.TRX);
      trxCount.add();
      trxCache.put(item, new TransactionMessage(trxMsg.getTransactionCapsule().getInstance()));
    } else {
      logger.error("Adv item is neither block nor trx, type: {}", msg.getType());
      return;
    }

    invToSpread.put(item, System.currentTimeMillis());

    if (InventoryType.BLOCK.equals(item.getType())) {
      consumerInvToSpread();
    }
  }

  public void fastForward(BlockMessage msg) {
    Item item = new Item(msg.getBlockId(), InventoryType.BLOCK);
    List<PeerConnection> peers = tronNetDelegate.getActivePeer().stream()
        .filter(peer -> !peer.isNeedSyncFromPeer() && !peer.isNeedSyncFromUs())
        .filter(peer -> peer.getAdvInvReceive().getIfPresent(item) == null
            && peer.getAdvInvSpread().getIfPresent(item) == null)
        .collect(Collectors.toList());

    if (!fastForward) {
      peers = peers.stream().filter(peer -> peer.isFastForwardPeer()).collect(Collectors.toList());
    }

    peers.forEach(peer -> {
      peer.fastSend(msg);
      peer.getAdvInvSpread().put(item, System.currentTimeMillis());
      peer.setFastForwardBlock(msg.getBlockId());
    });
  }


  public void onDisconnect(PeerConnection peer) {
    if (!peer.getAdvInvRequest().isEmpty()) {
      peer.getAdvInvRequest().keySet().forEach(item -> {
        if (tronNetDelegate.getActivePeer().stream()
            .anyMatch(p -> !p.equals(peer) && p.getAdvInvReceive().getIfPresent(item) != null)) {
          invToFetch.put(item, System.currentTimeMillis());
        } else {
          invToFetchCache.invalidate(item);
        }
      });
    }

    if (invToFetch.size() > 0) {
      consumerInvToFetch();
    }
  }

  private synchronized void consumerInvToFetch() {
    Collection<PeerConnection> peers = tronNetDelegate.getActivePeer().stream()
        .filter(peer -> peer.isIdle())
        .collect(Collectors.toList());

    if (invToFetch.isEmpty() || peers.isEmpty()) {
      return;
    }

    InvSender invSender = new InvSender();
    long now = System.currentTimeMillis();
    invToFetch.forEach((item, time) -> {
      if (time < now - MSG_CACHE_DURATION_IN_BLOCKS * BLOCK_PRODUCED_INTERVAL) {
        logger.info("This obj is too late to fetch, type: {} hash: {}.", item.getType(),
            item.getHash());
        invToFetch.remove(item);
        invToFetchCache.invalidate(item);
        return;
      }
      peers.stream().filter(peer -> peer.getAdvInvReceive().getIfPresent(item) != null
          && invSender.getSize(peer) < MAX_TRX_FETCH_PER_PEER)
          .sorted(Comparator.comparingInt(peer -> invSender.getSize(peer)))
          .findFirst().ifPresent(peer -> {
            invSender.add(item, peer);
            peer.getAdvInvRequest().put(item, now);
            invToFetch.remove(item);
          });
    });

    invSender.sendFetch();
  }

  private synchronized void consumerInvToSpread() {

    List<PeerConnection> peers = tronNetDelegate.getActivePeer().stream()
        .filter(peer -> !peer.isNeedSyncFromPeer() && !peer.isNeedSyncFromUs())
        .collect(Collectors.toList());

    if (invToSpread.isEmpty() || peers.isEmpty()) {
      return;
    }

    InvSender invSender = new InvSender();

    invToSpread.forEach((item, time) -> peers.forEach(peer -> {
      if (peer.getAdvInvReceive().getIfPresent(item) == null
          && peer.getAdvInvSpread().getIfPresent(item) == null
          && !(item.getType().equals(InventoryType.BLOCK)
          && System.currentTimeMillis() - time > BLOCK_PRODUCED_INTERVAL)) {
        peer.getAdvInvSpread().put(item, Time.getCurrentMillis());
        invSender.add(item, peer);
      }
      invToSpread.remove(item);
    }));

    invSender.sendInv();
  }

  class InvSender {

    private HashMap<PeerConnection, HashMap<InventoryType, LinkedList<Sha256Hash>>> send
        = new HashMap<>();

    public void clear() {
      this.send.clear();
    }

    public void add(Entry<Sha256Hash, InventoryType> id, PeerConnection peer) {
      if (send.containsKey(peer) && !send.get(peer).containsKey(id.getValue())) {
        send.get(peer).put(id.getValue(), new LinkedList<>());
      } else if (!send.containsKey(peer)) {
        send.put(peer, new HashMap<>());
        send.get(peer).put(id.getValue(), new LinkedList<>());
      }
      send.get(peer).get(id.getValue()).offer(id.getKey());
    }

    public void add(Item id, PeerConnection peer) {
      if (send.containsKey(peer) && !send.get(peer).containsKey(id.getType())) {
        send.get(peer).put(id.getType(), new LinkedList<>());
      } else if (!send.containsKey(peer)) {
        send.put(peer, new HashMap<>());
        send.get(peer).put(id.getType(), new LinkedList<>());
      }
      send.get(peer).get(id.getType()).offer(id.getHash());
    }

    public int getSize(PeerConnection peer) {
      if (send.containsKey(peer)) {
        return send.get(peer).values().stream().mapToInt(LinkedList::size).sum();
      }
      return 0;
    }

    public void sendInv() {
      send.forEach((peer, ids) -> ids.forEach((key, value) -> {
        if (peer.isFastForwardPeer() && key.equals(InventoryType.TRX)) {
          return;
        }
        if (key.equals(InventoryType.BLOCK)) {
          value.sort(Comparator.comparingLong(value1 -> new BlockId(value1).getNum()));
          peer.fastSend(new InventoryMessage(value, key));
        } else {
          peer.sendMessage(new InventoryMessage(value, key));
        }
      }));
    }

    void sendFetch() {
      send.forEach((peer, ids) -> ids.forEach((key, value) -> {
        if (key.equals(InventoryType.BLOCK)) {
          value.sort(Comparator.comparingLong(value1 -> new BlockId(value1).getNum()));
          peer.fastSend(new FetchInvDataMessage(value, key));
        } else {
          peer.sendMessage(new FetchInvDataMessage(value, key));
        }
      }));
    }
  }

}
