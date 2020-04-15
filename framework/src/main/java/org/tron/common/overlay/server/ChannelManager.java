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
import org.tron.core.config.args.Args;
import org.tron.core.db.ByteArrayWrapper;
import org.tron.core.metrics.MetricsKey;
import org.tron.core.metrics.MetricsUtil;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
public class ChannelManager {

  private final Map<ByteArrayWrapper, Channel> activePeers = new ConcurrentHashMap<>();
  @Autowired
  private PeerServer peerServer;
  @Autowired
  private PeerClient peerClient;
  @Autowired
  private SyncPool syncPool;
  @Autowired
  private FastForward fastForward;
  private CommonParameter parameter = CommonParameter.getInstance();
  private Cache<InetAddress, ReasonCode> badPeers = CacheBuilder.newBuilder().maximumSize(10000)
      .expireAfterWrite(1, TimeUnit.HOURS).recordStats().build();

  private Cache<InetAddress, ReasonCode> recentlyDisconnected = CacheBuilder.newBuilder()
      .maximumSize(1000).expireAfterWrite(30, TimeUnit.SECONDS).recordStats().build();

  @Getter
  private Cache<InetAddress, Node> trustNodes = CacheBuilder.newBuilder().maximumSize(100).build();

  @Getter
  private Map<InetAddress, Node> activeNodes = new ConcurrentHashMap();

  @Getter
  private Map<InetAddress, Node> fastForwardNodes = new ConcurrentHashMap();

  private int maxActivePeers = parameter.getNodeMaxActiveNodes();

  private int getMaxActivePeersWithSameIp = parameter.getNodeMaxActiveNodesWithSameIp();

  public void init() {
    if (this.parameter.getNodeListenPort() > 0) {
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
  }

  public void notifyDisconnect(Channel channel) {
    syncPool.onDisconnect(channel);
    activePeers.values().remove(channel);
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

      if (!peer.isActive() && activePeers.size() >= maxActivePeers) {
        peer.disconnect(TOO_MANY_PEERS);
        return false;
      }

      if (getConnectionNum(peer.getInetAddress()) >= getMaxActivePeersWithSameIp) {
        peer.disconnect(TOO_MANY_PEERS_WITH_SAME_IP);
        return false;
      }
    }

    Channel channel = activePeers.get(peer.getNodeIdWrapper());
    if (channel != null) {
      if (channel.getStartTime() > peer.getStartTime()) {
        logger.info("Disconnect connection established later, {}", channel.getNode());
        channel.disconnect(DUPLICATE_PEER);
      } else {
        peer.disconnect(DUPLICATE_PEER);
        return false;
      }
    }
    activePeers.put(peer.getNodeIdWrapper(), peer);
    logger.info("Add active peer {}, total active peers: {}", peer, activePeers.size());
    return true;
  }

  public int getConnectionNum(InetAddress inetAddress) {
    int cnt = 0;
    for (Channel channel : activePeers.values()) {
      if (channel.getInetAddress().equals(inetAddress)) {
        cnt++;
      }
    }
    return cnt;
  }

  public Collection<Channel> getActivePeers() {
    return activePeers.values();
  }

  public Cache<InetAddress, ReasonCode> getRecentlyDisconnected() {
    return this.recentlyDisconnected;
  }

  public Cache<InetAddress, ReasonCode> getBadPeers() {
    return this.badPeers;
  }

  public void close() {
    peerServer.close();
    peerClient.close();
    syncPool.close();
  }
}
