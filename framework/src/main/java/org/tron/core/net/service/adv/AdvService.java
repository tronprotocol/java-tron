package org.tron.core.net.service.adv;

import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.tron.core.config.Parameter.NetConstants.MAX_TRX_FETCH_PER_PEER;
import static org.tron.core.config.Parameter.NetConstants.MSG_CACHE_DURATION_IN_BLOCKS;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Time;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.config.args.Args;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.message.adv.FetchInvDataMessage;
import org.tron.core.net.message.adv.InventoryMessage;
import org.tron.core.net.message.adv.TransactionMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.fetchblock.FetchBlockService;
import org.tron.core.net.service.statistics.MessageCount;
import org.tron.protos.Protocol.Inventory.InventoryType;

@Slf4j(topic = "net")
@Component
public class AdvService {

  private final int MAX_INV_TO_FETCH_CACHE_SIZE = 100_000;
  private final int MAX_TRX_CACHE_SIZE = 50_000;
  private final int MAX_BLOCK_CACHE_SIZE = 10;
  private final int MAX_SPREAD_SIZE = 1_000;
  private final long TIMEOUT = MSG_CACHE_DURATION_IN_BLOCKS * BLOCK_PRODUCED_INTERVAL;

  @Autowired
  private TronNetDelegate tronNetDelegate;

  @Autowired
  private FetchBlockService fetchBlockService;

  private ConcurrentHashMap<Item, Long> invToFetch = new ConcurrentHashMap<>();

  private ConcurrentHashMap<Item, Long> invToSpread = new ConcurrentHashMap<>();

  private long blockCacheTimeout = Args.getInstance().getBlockCacheTimeout();
  private Cache<Item, Long> invToFetchCache = CacheBuilder.newBuilder()
      .maximumSize(MAX_INV_TO_FETCH_CACHE_SIZE)
      .expireAfterWrite(blockCacheTimeout, TimeUnit.MINUTES)
      .recordStats().build();

  private Cache<Item, Message> trxCache = CacheBuilder.newBuilder()
      .maximumSize(MAX_TRX_CACHE_SIZE).expireAfterWrite(1, TimeUnit.HOURS)
      .recordStats().build();

  private Cache<Item, Message> blockCache = CacheBuilder.newBuilder()
      .maximumSize(MAX_BLOCK_CACHE_SIZE).expireAfterWrite(1, TimeUnit.MINUTES)
      .recordStats().build();

  private final String spreadName = "adv-spread";
  private final String fetchName = "adv-fetch";
  private final ScheduledExecutorService spreadExecutor = ExecutorServiceManager
      .newSingleThreadScheduledExecutor(spreadName);

  private final ScheduledExecutorService fetchExecutor = ExecutorServiceManager
      .newSingleThreadScheduledExecutor(fetchName);

  @Getter
  private MessageCount trxCount = new MessageCount();

  private boolean fastForward = Args.getInstance().isFastForward();

  public void init() {

    spreadExecutor.scheduleWithFixedDelay(() -> {
      try {
        consumerInvToSpread();
      } catch (Exception exception) {
        logger.error("Spread thread error", exception);
      }
    }, 100, 30, TimeUnit.MILLISECONDS);

    fetchExecutor.scheduleWithFixedDelay(() -> {
      try {
        consumerInvToFetch();
      } catch (Exception exception) {
        logger.error("Fetch thread error", exception);
      }
    }, 100, 30, TimeUnit.MILLISECONDS);
  }

  public void close() {
    ExecutorServiceManager.shutdownAndAwaitTermination(spreadExecutor, spreadName);
    ExecutorServiceManager.shutdownAndAwaitTermination(fetchExecutor, fetchName);
  }

  public synchronized void addInvToCache(Item item) {
    invToFetchCache.put(item, System.currentTimeMillis());
    invToFetch.remove(item);
  }

  public void recordInventory(PeerConnection peer, Item item, long receivedAt) {
    if (item.getType() != InventoryType.BLOCK) {
      peer.getAdvInvReceive().put(item, receivedAt);
      return;
    }
    synchronized (this) {
      // Capture eligibility before fetching; validation may advance the head immediately.
      if (peer.getAdvInvSpread().getIfPresent(item) == null
          && new BlockId(item.getHash()).getNum() > tronNetDelegate.getHeadBlockId().getNum()) {
        peer.getAdvBlockInvReceive().put(item, receivedAt);
      }
      peer.getAdvInvReceive().put(item, receivedAt);
    }
  }

  /**
   * Confirms eligible announcements only after the block has been successfully processed.
   */
  public synchronized void confirmBlockInventory(BlockId blockId) {
    Item item = new Item(blockId, InventoryType.BLOCK);
    // Share the registration lock so confirmation cannot miss an eligible announcement.
    tronNetDelegate.getActivePeer().forEach(peer -> {
      Long receivedAt = peer.getAdvBlockInvReceive().asMap().remove(item);
      if (receivedAt != null) {
        peer.updateLastInteractiveTime(receivedAt);
      }
    });
  }

  public boolean addInv(Item item) {
    if (fastForward && item.getType().equals(InventoryType.TRX)) {
      return false;
    }

    if (item.getType().equals(InventoryType.TRX) && trxCache.getIfPresent(item) != null) {
      return false;
    }

    if (item.getType().equals(InventoryType.BLOCK)) {
      if (blockCache.getIfPresent(item) != null) {
        return false;
      }

      long solidNum = tronNetDelegate.getSolidifiedBlockNum();
      if (new BlockId(item.getHash()).getNum() <= solidNum) {
        return false;
      }
    }

    synchronized (this) {
      if (invToFetchCache.getIfPresent(item) != null) {
        return false;
      }
      invToFetchCache.put(item, System.currentTimeMillis());
      invToFetch.put(item, System.currentTimeMillis());
    }

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

  public int fastBroadcastTransaction(TransactionMessage msg) {

    List<PeerConnection> peers = tronNetDelegate.getActivePeer().stream()
        .filter(peer -> !peer.isNeedSyncFromPeer() && !peer.isNeedSyncFromUs())
        .collect(Collectors.toList());

    if (peers.size() == 0) {
      logger.warn("Broadcast transaction {} failed, no connection", msg.getMessageId());
      return 0;
    }

    Item item = new Item(msg.getMessageId(), InventoryType.TRX);
    trxCount.add();
    trxCache.put(item, new TransactionMessage(msg.getTransactionCapsule().getInstance()));

    List<Sha256Hash> list = new ArrayList<>();
    list.add(msg.getMessageId());
    InventoryMessage inventoryMessage = new InventoryMessage(list, InventoryType.TRX);

    int peersCount = 0;
    for (PeerConnection peer : peers) {
      if (peer.getAdvInvReceive().getIfPresent(item) == null
          && peer.getAdvInvSpread().getIfPresent(item) == null) {
        peersCount++;
        peer.getAdvInvSpread().put(item, Time.getCurrentMillis());
        peer.sendMessage(inventoryMessage);
      }
    }
    if (peersCount == 0) {
      logger.warn("Broadcast transaction {} failed, no peers", msg.getMessageId());
    }
    return peersCount;
  }

  public void broadcast(Message msg) {

    if (fastForward) {
      return;
    }

    if (invToSpread.size() > MAX_SPREAD_SIZE) {
      logger.warn("Drop message, type: {}, ID: {}", msg.getType(), msg.getMessageId());
      return;
    }

    Item item;
    if (msg instanceof BlockMessage) {
      BlockMessage blockMsg = (BlockMessage) msg;
      item = new Item(blockMsg.getMessageId(), InventoryType.BLOCK);
      logger.info("Ready to broadcast block {}", blockMsg.getBlockId().getString());
      blockMsg.getBlockCapsule().getTransactions().forEach(transactionCapsule -> {
        Sha256Hash tid = transactionCapsule.getTransactionId();
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

  public void onDisconnect(PeerConnection peer) {
    // Release this peer's retry tracking before looking for another outstanding request.
    fetchBlockService.onDisconnect(peer);
    if (!peer.getAdvInvRequest().isEmpty()) {
      peer.getAdvInvRequest().keySet().forEach(item -> {
        synchronized (this) {
          Collection<PeerConnection> peers = tronNetDelegate.getActivePeer().stream()
              .filter(p -> !p.equals(peer) && !p.isDisconnect()).collect(Collectors.toList());
          // Another provider may have already delivered the block.
          if (item.getType() == InventoryType.BLOCK
              && (blockCache.getIfPresent(item) != null
              || tronNetDelegate.containBlock(new BlockId(item.getHash())))) {
            return;
          }
          if (item.getType() == InventoryType.BLOCK) {
            Optional<PeerConnection> pending = peers.stream()
                .filter(p -> p.getAdvInvRequest().containsKey(item)).findFirst();
            if (pending.isPresent()) {
              // Reuse the pending request and its original timestamp instead of sending it again.
              fetchBlockService.fetchBlock(Collections.singletonList(item.getHash()),
                  pending.get());
              return;
            }
          }
          if (peers.stream().anyMatch(p -> p.getAdvInvReceive().getIfPresent(item) != null)) {
            // Let normal scheduling select a replacement from the remaining advertisers.
            invToFetch.put(item, System.currentTimeMillis());
          } else {
            // Allow future announcements to enqueue the item again when no source remains.
            invToFetch.remove(item);
            invToFetchCache.invalidate(item);
          }
        }
      });
    }

    if (!invToFetch.isEmpty()) {
      consumerInvToFetch();
    }
  }

  private void consumerInvToFetch() {
    // Snapshot connected peers; block fetching can still use peers with pending TRX requests.
    Collection<PeerConnection> peers = tronNetDelegate.getActivePeer().stream()
        .filter(peer -> !peer.isDisconnect())
        .collect(Collectors.toList());
    Collection<PeerConnection> trxPeers = peers.stream().filter(PeerConnection::isIdle)
        .collect(Collectors.toList());
    // Batch sizes count requests assigned in this pass, not all outstanding requests on a peer.
    InvSender invSender = new InvSender();
    // Coordinate queue updates with inventory admission, other consumers, and disconnect recovery.
    synchronized (this) {
      if (invToFetch.isEmpty() || peers.isEmpty()) {
        return;
      }
      long now = System.currentTimeMillis();
      List<Item> blocks = new ArrayList<>();
      invToFetch.forEach((item, time) -> {
        long timeout = item.getType() == InventoryType.BLOCK ? NetConstants.ADV_TIME_OUT : TIMEOUT;
        if (time < now - timeout) {
          // Release the deduplication entry so a later announcement can enqueue this item again.
          logger.info("This obj is too late to fetch, type: {} hash: {}", item.getType(),
              item.getHash());
          invToFetch.remove(item);
          invToFetchCache.invalidate(item);
          return;
        }
        if (item.getType() == InventoryType.BLOCK) {
          // Defer block allocation until all queued blocks can be ordered by height.
          blocks.add(item);
          return;
        }
        // Use recent advertisers, enforce the per-peer TRX batch limit, and prefer smaller batches.
        trxPeers.stream()
            .filter(peer -> {
              Long t = peer.getAdvInvReceive().getIfPresent(item);
              return t != null && now - t < TIMEOUT
                  && invSender.getSize(peer) < MAX_TRX_FETCH_PER_PEER;
            })
            .min(Comparator.comparingInt(invSender::getSize))
            .ifPresent(peer -> {
              // Reserve the request before sending; an existing request must not be sent again.
              if (peer.checkAndPutAdvInvRequest(item, now)) {
                invSender.add(item, peer);
              }
              invToFetch.remove(item);
            });
      });

      // Reserve peers for earlier blocks before later blocks can make them busy.
      blocks.sort(Comparator.comparingLong(item -> new BlockId(item.getHash()).getNum()));
      blocks.forEach(item -> {
        // A cached, stored, or already requested block no longer needs an initial fetch.
        if (blockCache.getIfPresent(item) != null
            || tronNetDelegate.containBlock(new BlockId(item.getHash()))
            || peers.stream().anyMatch(peer -> peer.getAdvInvRequest().containsKey(item))) {
          invToFetch.remove(item);
          return;
        }
        // Recheck eligibility after earlier reservations. Initial fetches prefer smaller batches;
        // latency ranking is reserved for backup fetches in FetchBlockService.
        peers.stream()
            .filter(peer -> fetchBlockService.canFetchBlock(peer, item, now))
            .min(Comparator.comparingInt(invSender::getSize))
            .ifPresent(peer -> {
              if (peer.checkAndPutAdvInvRequest(item, now)) {
                invSender.add(item, peer);
                invToFetch.remove(item);
              }
            });
      });
      // Items without an eligible provider remain queued for a later pass.
    }

    // Send outside the scheduling lock; sendFetch() registers block retry state before sending.
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

  private class InvSender {

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
      HashMap<InventoryType, LinkedList<Sha256Hash>> map = send.get(peer);
      if (map == null) {
        map = new HashMap<>();
        send.put(peer, map);
      }

      LinkedList<Sha256Hash> list = map.get(id.getType());
      if (list == null) {
        list = new LinkedList<>();
        map.put(id.getType(), list);
      }

      list.offer(id.getHash());
    }

    public int getSize(PeerConnection peer) {
      if (send.containsKey(peer)) {
        return send.get(peer).values().stream().mapToInt(LinkedList::size).sum();
      }
      return 0;
    }

    public void sendInv() {
      send.forEach((peer, ids) -> ids.forEach((key, value) -> {
        if (peer.isRelayPeer() && key.equals(InventoryType.TRX)) {
          return;
        }
        if (key.equals(InventoryType.BLOCK)) {
          value.sort(Comparator.comparingLong(value1 -> new BlockId(value1).getNum()));
          peer.sendMessage(new InventoryMessage(value, key));
        } else {
          peer.sendMessage(new InventoryMessage(value, key));
        }
      }));
    }

    private void sendFetch() {
      send.forEach((peer, ids) -> ids.forEach((key, value) -> {
        if (key.equals(InventoryType.BLOCK)) {
          value.sort(Comparator.comparingLong(value1 -> new BlockId(value1).getNum()));
          fetchBlockService.fetchBlock(value, peer);
          peer.sendMessage(new FetchInvDataMessage(value, key));
        } else {
          peer.sendMessage(new FetchInvDataMessage(value, key));
        }
      }));
    }
  }

}
