package org.tron.common.overlay.server;

import static org.tron.protos.Protocol.ReasonCode.DUPLICATE_PEER;
import static org.tron.protos.Protocol.ReasonCode.TOO_MANY_PEERS;
import static org.tron.protos.Protocol.ReasonCode.TOO_MANY_PEERS_WITH_SAME_IP;
import static org.tron.protos.Protocol.ReasonCode.UNKNOWN;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.client.PeerClient;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.Metrics;
import org.tron.core.config.args.Args;
import org.tron.core.db.ByteArrayWrapper;
import org.tron.core.metrics.MetricsKey;
import org.tron.core.metrics.MetricsUtil;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
public class ChannelManager {

  private final Map<ByteArrayWrapper, Channel> activeChannels = new ConcurrentHashMap<>();
  @Autowired
  private PeerServer peerServer;
  @Autowired
  private PeerClient peerClient;
  @Autowired
  private SyncPool syncPool;
  @Autowired
  private PeerConnectionCheckService peerConnectionCheckService;
  @Autowired
  private FastForward fastForward;
  private CommonParameter parameter = CommonParameter.getInstance();
  private Cache<InetAddress, ReasonCode> badPeers = CacheBuilder.newBuilder().maximumSize(10000)
      .expireAfterWrite(1, TimeUnit.HOURS).recordStats().build();

  private Cache<InetAddress, ReasonCode> recentlyDisconnected = CacheBuilder.newBuilder()
      .maximumSize(1000).expireAfterWrite(30, TimeUnit.SECONDS).recordStats().build();

  @Getter
  private Cache<String, Protocol.HelloMessage> helloMessageCache = CacheBuilder.newBuilder()
          .maximumSize(2000).expireAfterWrite(24, TimeUnit.HOURS).recordStats().build();

  @Getter
  private Cache<InetAddress, Node> trustNodes = CacheBuilder.newBuilder().maximumSize(100).build();

  @Getter
  private Map<InetAddress, Node> activeNodes = new ConcurrentHashMap();

  @Getter
  private Map<InetAddress, Node> fastForwardNodes = new ConcurrentHashMap();

  private int maxConnections = parameter.getMaxConnections();

  private int maxConnectionsWithSameIp = parameter.getMaxConnectionsWithSameIp();

  public void init() {
    if (this.parameter.getNodeListenPort() > 0 && !this.parameter.isSolidityNode()) {
      new Thread(() -> peerServer.start(Args.getInstance().getNodeListenPort()),
          "PeerServerThread").start();
    }

    InetAddress address;
    for (Node node : parameter.getPassiveNodes()) {
      address = new InetSocketAddress(node.getHost(), node.getPort()).getAddress();
      trustNodes.put(address, node);
    }

    for (Node node : parameter.getActiveNodes()) {
      address = new InetSocketAddress(node.getHost(), node.getPort()).getAddress();
      trustNodes.put(address, node);
      activeNodes.put(address, node);
    }

    for (Node node : parameter.getFastForwardNodes()) {
      address = new InetSocketAddress(node.getHost(), node.getPort()).getAddress();
      trustNodes.put(address, node);
      fastForwardNodes.put(address, node);
    }

    logger.info("Node config, trust {}, active {}, forward {}.",
        trustNodes.size(), activeNodes.size(), fastForwardNodes.size());

    peerConnectionCheckService.init();
    syncPool.init();
    fastForward.init();
  }

  public void processDisconnect(Channel channel, ReasonCode reason) {
    InetAddress inetAddress = channel.getInetAddress();
    if (inetAddress == null) {
      return;
    }
    switch (reason) {
      case BAD_PROTOCOL:
      case BAD_BLOCK:
      case BAD_TX:
        badPeers.put(channel.getInetAddress(), reason);
        break;
      default:
        recentlyDisconnected.put(channel.getInetAddress(), reason);
        break;
    }
    MetricsUtil.counterInc(MetricsKey.NET_DISCONNECTION_COUNT);
    MetricsUtil.counterInc(MetricsKey.NET_DISCONNECTION_DETAIL + reason);
    Metrics.counterInc(MetricKeys.Counter.P2P_DISCONNECT, 1,
        reason.name().toLowerCase(Locale.ROOT));
  }

  public void notifyDisconnect(Channel channel) {
    syncPool.onDisconnect(channel);
    activeChannels.values().remove(channel);
    if (channel != null) {
      if (channel.getNodeStatistics() != null) {
        channel.getNodeStatistics().notifyDisconnect();
      }
      InetAddress inetAddress = channel.getInetAddress();
      if (inetAddress != null && recentlyDisconnected.getIfPresent(inetAddress) == null) {
        recentlyDisconnected.put(channel.getInetAddress(), UNKNOWN);
      }
    }
  }

  public synchronized boolean processPeer(Channel peer) {

    if (trustNodes.getIfPresent(peer.getInetAddress()) == null) {
      if (recentlyDisconnected.getIfPresent(peer) != null) {
        logger.info("Peer {} recently disconnected.", peer.getInetAddress());
        return false;
      }

      if (badPeers.getIfPresent(peer) != null) {
        peer.disconnect(peer.getNodeStatistics().getDisconnectReason());
        return false;
      }

      if (!peer.isActive() && activeChannels.size() >= maxConnections) {
        peer.disconnect(TOO_MANY_PEERS);
        return false;
      }

      if (getConnectionNum(peer.getInetAddress()) >= maxConnectionsWithSameIp) {
        peer.disconnect(TOO_MANY_PEERS_WITH_SAME_IP);
        return false;
      }
    }

    Channel channel = activeChannels.get(peer.getNodeIdWrapper());
    if (channel != null) {
      if (channel.getStartTime() > peer.getStartTime()) {
        logger.info("Disconnect connection established later, {}", channel.getNode());
        channel.disconnect(DUPLICATE_PEER);
      } else {
        peer.disconnect(DUPLICATE_PEER);
        return false;
      }
    }
    activeChannels.put(peer.getNodeIdWrapper(), peer);
    logger.info("Add active peer {}, total active peers: {}", peer, activeChannels.size());
    return true;
  }

  public int getConnectionNum(InetAddress inetAddress) {
    int cnt = 0;
    for (Channel channel : activeChannels.values()) {
      if (channel.getInetAddress().equals(inetAddress)) {
        cnt++;
      }
    }
    return cnt;
  }

  public Collection<Channel> getActivePeers() {
    return activeChannels.values();
  }

  public Cache<InetAddress, ReasonCode> getRecentlyDisconnected() {
    return this.recentlyDisconnected;
  }

  public Cache<InetAddress, ReasonCode> getBadPeers() {
    return this.badPeers;
  }

  public void close() {
    peerConnectionCheckService.close();
    syncPool.close();
    peerServer.close();
    peerClient.close();
  }
}
