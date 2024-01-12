package org.tron.core.net.peer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.ByteString;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.message.Message;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.Metrics;
import org.tron.common.utils.Pair;
import org.tron.common.utils.Sha256Hash;
import org.tron.consensus.pbft.message.PbftBaseMessage;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.config.args.Args;
import org.tron.core.metrics.MetricsKey;
import org.tron.core.metrics.MetricsUtil;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.InventoryMessage;
import org.tron.core.net.message.adv.TransactionsMessage;
import org.tron.core.net.message.base.DisconnectMessage;
import org.tron.core.net.message.handshake.HelloMessage;
import org.tron.core.net.message.keepalive.PingMessage;
import org.tron.core.net.message.keepalive.PongMessage;
import org.tron.core.net.service.adv.AdvService;
import org.tron.core.net.service.statistics.NodeStatistics;
import org.tron.core.net.service.statistics.PeerStatistics;
import org.tron.core.net.service.statistics.TronStatsManager;
import org.tron.core.net.service.sync.SyncService;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol;

@Slf4j(topic = "net")
@Component
@Scope("prototype")
public class PeerConnection {

  private static List<InetSocketAddress> relayNodes = Args.getInstance().getFastForwardNodes();

  @Getter
  private PeerStatistics peerStatistics = new PeerStatistics();

  @Getter
  private NodeStatistics nodeStatistics;

  @Getter
  private Channel channel;

  @Getter
  @Setter
  private volatile boolean isRelayPeer;

  @Setter
  @Getter
  private volatile boolean fetchAble;

  @Setter
  @Getter
  private volatile boolean isBadPeer;

  @Getter
  @Setter
  private ByteString address;

  @Getter
  @Setter
  private TronState tronState = TronState.INIT;

  @Autowired
  private TronNetDelegate tronNetDelegate;

  @Autowired
  private SyncService syncService;

  @Autowired
  private AdvService advService;

  @Setter
  @Getter
  private HelloMessage helloMessageReceive;

  @Setter
  @Getter
  private HelloMessage helloMessageSend;

  private int invCacheSize = 20_000;

  private long BAD_PEER_BAN_TIME = 3_600_000;

  @Setter
  @Getter
  private Cache<Item, Long> advInvReceive = CacheBuilder.newBuilder().maximumSize(invCacheSize)
      .expireAfterWrite(1, TimeUnit.HOURS).recordStats().build();

  @Setter
  @Getter
  private Cache<Item, Long> advInvSpread = CacheBuilder.newBuilder().maximumSize(invCacheSize)
      .expireAfterWrite(1, TimeUnit.HOURS).recordStats().build();

  @Setter
  @Getter
  private Map<Item, Long> advInvRequest = new ConcurrentHashMap<>();

  @Setter
  private BlockId fastForwardBlock;

  @Getter
  private BlockId blockBothHave = new BlockId();
  @Getter
  private volatile long blockBothHaveUpdateTime = System.currentTimeMillis();
  @Setter
  @Getter
  private BlockId lastSyncBlockId;
  @Setter
  @Getter
  private volatile long remainNum;
  @Getter
  private Cache<Sha256Hash, Long> syncBlockIdCache = CacheBuilder.newBuilder()
      .maximumSize(2 * NetConstants.SYNC_FETCH_BATCH_NUM).recordStats().build();
  @Setter
  @Getter
  private Deque<BlockId> syncBlockToFetch = new ConcurrentLinkedDeque<>();
  @Setter
  @Getter
  private Map<BlockId, Long> syncBlockRequested = new ConcurrentHashMap<>();
  @Setter
  @Getter
  private Pair<Deque<BlockId>, Long> syncChainRequested = null;
  @Setter
  @Getter
  private Set<BlockId> syncBlockInProcess = new HashSet<>();
  @Setter
  @Getter
  private volatile boolean needSyncFromPeer = true;
  @Setter
  @Getter
  private volatile boolean needSyncFromUs = true;

  public void setChannel(Channel channel) {
    this.channel = channel;
    if (relayNodes.stream().anyMatch(n -> n.getAddress().equals(channel.getInetAddress()))) {
      this.isRelayPeer = true;
    }
    this.nodeStatistics = TronStatsManager.getNodeStatistics(channel.getInetAddress());
  }

  public void setBlockBothHave(BlockId blockId) {
    this.blockBothHave = blockId;
    this.blockBothHaveUpdateTime = System.currentTimeMillis();
  }

  public boolean isIdle() {
    return advInvRequest.isEmpty() && syncBlockRequested.isEmpty() && syncChainRequested == null;
  }

  public void sendMessage(Message message) {
    if (needToLog(message)) {
      logger.info("Send peer {} message {}", channel.getInetSocketAddress(), message);
    }
    channel.send(message.getSendBytes());
    peerStatistics.messageStatistics.addTcpOutMessage(message);
  }

  public void onConnect() {
    long headBlockNum = helloMessageSend.getHeadBlockId().getNum();
    long peerHeadBlockNum = helloMessageReceive.getHeadBlockId().getNum();

    if (peerHeadBlockNum > headBlockNum) {
      needSyncFromUs = false;
      syncService.startSync(this);
    } else {
      needSyncFromPeer = false;
      if (peerHeadBlockNum == headBlockNum) {
        needSyncFromUs = false;
      }
      setTronState(TronState.SYNC_COMPLETED);
    }
  }

  public void onDisconnect() {
    syncService.onDisconnect(this);
    advService.onDisconnect(this);
    advInvReceive.cleanUp();
    advInvSpread.cleanUp();
    advInvRequest.clear();
    syncBlockIdCache.cleanUp();
    syncBlockToFetch.clear();
    syncBlockRequested.clear();
    syncBlockInProcess.clear();
  }

  public String log() {
    long now = System.currentTimeMillis();
    BlockId syncBlockId = syncBlockToFetch.peek();
    Pair<Deque<BlockId>, Long> requested = syncChainRequested;
    return String.format(
        "Peer %s\n"
            + "connect time: %ds [%sms]\n"
            + "last know block num: %s\n"
            + "needSyncFromPeer:%b\n"
            + "needSyncFromUs:%b\n"
            + "syncToFetchSize:%d\n"
            + "syncToFetchSizePeekNum:%d\n"
            + "syncBlockRequestedSize:%d\n"
            + "remainNum:%d\n"
            + "syncChainRequested:%d\n"
            + "blockInProcess:%d\n",
        channel.getInetSocketAddress(),
        (now - channel.getStartTime()) / Constant.ONE_THOUSAND,
        channel.getAvgLatency(),
        fastForwardBlock != null ? fastForwardBlock.getNum() : blockBothHave.getNum(),
        isNeedSyncFromPeer(),
        isNeedSyncFromUs(),
        syncBlockToFetch.size(),
        syncBlockId != null ? syncBlockId.getNum() : -1,
        syncBlockRequested.size(),
        remainNum,
        requested == null ? 0 : (now - requested.getValue())
                / Constant.ONE_THOUSAND,
        syncBlockInProcess.size());
  }

  public boolean isSyncFinish() {
    return !(needSyncFromPeer || needSyncFromUs);
  }

  public void disconnect(Protocol.ReasonCode code) {
    sendMessage(new DisconnectMessage(code));
    processDisconnect(code);
    nodeStatistics.nodeDisconnectedLocal(code);
  }

  public InetSocketAddress getInetSocketAddress() {
    return channel.getInetSocketAddress();
  }

  public InetAddress getInetAddress() {
    return channel.getInetAddress();
  }

  public boolean isDisconnect() {
    return channel.isDisconnect();
  }

  private void processDisconnect(Protocol.ReasonCode reason) {
    InetAddress inetAddress = channel.getInetAddress();
    if (inetAddress == null) {
      return;
    }
    switch (reason) {
      case BAD_PROTOCOL:
      case BAD_BLOCK:
      case BAD_TX:
        channel.close(BAD_PEER_BAN_TIME);
        break;
      default:
        channel.close();
        break;
    }
    MetricsUtil.counterInc(MetricsKey.NET_DISCONNECTION_COUNT);
    MetricsUtil.counterInc(MetricsKey.NET_DISCONNECTION_DETAIL + reason);
    Metrics.counterInc(MetricKeys.Counter.P2P_DISCONNECT, 1,
            reason.name().toLowerCase(Locale.ROOT));
  }

  public static boolean needToLog(Message msg) {
    if (msg instanceof PingMessage
            || msg instanceof PongMessage
            || msg instanceof TransactionsMessage
            || msg instanceof PbftBaseMessage) {
      return false;
    }

    if (msg instanceof InventoryMessage && ((InventoryMessage) msg)
            .getInventoryType().equals(Protocol.Inventory.InventoryType.TRX)) {
      return false;
    }

    return true;
  }

  public synchronized boolean checkAndPutAdvInvRequest(Item key, Long value) {
    if (advInvRequest.containsKey(key)) {
      return false;
    }
    advInvRequest.put(key, value);
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof PeerConnection)) {
      return false;
    }
    return this.channel.equals(((PeerConnection) o).getChannel());
  }

  @Override
  public int hashCode() {
    return this.channel.hashCode();
  }

}
