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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.client.PeerClient;
import org.tron.common.overlay.discover.node.Node;
import org.tron.core.config.args.Args;
import org.tron.core.db.ByteArrayWrapper;
import org.tron.protos.Protocol.ReasonCode;


@Component
public class ChannelManager {

  private static final Logger logger = LoggerFactory.getLogger("ChannelManager");

  private final Map<ByteArrayWrapper, Channel> activePeers = new ConcurrentHashMap<>();

  private Cache<InetAddress, ReasonCode> badPeers = CacheBuilder.newBuilder().maximumSize(10000)
      .expireAfterWrite(1, TimeUnit.HOURS).recordStats().build();

  private Cache<InetAddress, ReasonCode> recentlyDisconnected = CacheBuilder.newBuilder()
      .maximumSize(1000).expireAfterWrite(30, TimeUnit.SECONDS).recordStats().build();

  @Getter
  private Map<InetAddress, Node> trustPeers = new ConcurrentHashMap();

  private Args args = Args.getInstance();

  private int maxActivePeers = args.getNodeMaxActiveNodes();

  private int getMaxActivePeersWithSameIp = args.getNodeMaxActiveNodesWithSameIp();

  private PeerServer peerServer;

  private PeerClient peerClient;

  @Autowired
  private SyncPool syncPool;

  @Autowired
  private ChannelManager(final PeerServer peerServer, final PeerClient peerClient) {
    this.peerServer = peerServer;
    this.peerClient = peerClient;

    if (this.args.getNodeListenPort() > 0) {
      new Thread(() -> peerServer.start(Args.getInstance().getNodeListenPort()),
          "PeerServerThread").start();
    }

    for (Node node : args.getPassiveNodes()) {
      trustPeers.put(new InetSocketAddress(node.getHost(), node.getPort()).getAddress(), node);
    }
    logger.info("Trust peer size {}", trustPeers.size());
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

    if (!trustPeers.containsKey(peer.getInetAddress())) {
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

    if (activePeers.containsKey(peer.getNodeIdWrapper())) {
      Channel channel = activePeers.get(peer.getNodeIdWrapper());
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
  }
}
