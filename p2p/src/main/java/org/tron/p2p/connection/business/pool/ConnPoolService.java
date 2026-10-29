package org.tron.p2p.connection.business.pool;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.netty.channel.ChannelFuture;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.bouncycastle.util.encoders.Hex;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pEventHandler;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.connection.ConnectionPolicy;
import org.tron.p2p.connection.message.base.P2pDisconnectMessage;
import org.tron.p2p.connection.socket.PeerClient;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.NodeManager;
import org.tron.p2p.dns.DnsManager;
import org.tron.p2p.dns.DnsNode;
import org.tron.p2p.exception.P2pException;
import org.tron.p2p.protos.Connect.DisconnectReason;
import org.tron.p2p.utils.CollectionUtils;
import org.tron.p2p.utils.NetUtil;

@Slf4j(topic = "net")
public class ConnPoolService extends P2pEventHandler {

  // A remote host supporting TCP port reuse can connect from its listening port.
  // Incoming and outgoing channels can then share a remote address; track instances separately.
  // Includes registered inbound and outbound peers, including configured active nodes.
  private final List<Channel> registeredPeers = Collections.synchronizedList(new ArrayList<>());
  // Ordinary outbound sockets awaiting P2P handshake completion; excludes startup-configured nodes.
  // Reserve minConnections budget until onConnect or socket close, even after TCP connects.
  private final Set<io.netty.channel.Channel> pendingOrdinaryOutboundConnections =
      ConcurrentHashMap.newKeySet();
  private final Cache<InetAddress, Long> peerClientCache = CacheBuilder.newBuilder()
      .maximumSize(1000).expireAfterWrite(120, TimeUnit.SECONDS).recordStats().build();

  // Number of registered inbound peers.
  @Getter
  private final AtomicInteger inboundPeerCount = new AtomicInteger(0);
  // Number of registered outbound peers, including configured active nodes.
  @Getter
  private final AtomicInteger outboundPeerCount = new AtomicInteger(0);
  // Ordinary outbound connections, from dialing until disconnect;
  // excludes startup-configured active nodes and includes established connections.
  @Getter
  private final AtomicInteger ordinaryOutboundConnectionCount = new AtomicInteger(0);

  private final ScheduledThreadPoolExecutor poolLoopExecutor = new ScheduledThreadPoolExecutor(1,
      BasicThreadFactory.builder().namingPattern("connPool").build());
  private final ScheduledExecutorService disconnectExecutor =
      Executors.newSingleThreadScheduledExecutor(
          BasicThreadFactory.builder().namingPattern("randomDisconnect").build());

  public P2pConfig p2pConfig = Parameter.p2pConfig;
  private PeerClient peerClient;

  // Snapshot of active nodes configured when this pool is created.
  private final List<InetSocketAddress> configActiveNodes = new ArrayList<>();
  private int minCandidateSize = 50;

  /** @deprecated Use {@link #getInboundPeerCount()}. */
  @Deprecated
  public AtomicInteger getPassivePeersCount() {
    return getInboundPeerCount();
  }

  /** @deprecated Use {@link #getOutboundPeerCount()}. */
  @Deprecated
  public AtomicInteger getActivePeersCount() {
    return getOutboundPeerCount();
  }

  /** @deprecated Use {@link #getOrdinaryOutboundConnectionCount()}. */
  @Deprecated
  public AtomicInteger getConnectingPeersCount() {
    return getOrdinaryOutboundConnectionCount();
  }

  public ConnPoolService() {
    this.messageTypes = new HashSet<>(); //no message type registers
    try {
      Parameter.addP2pEventHandle(this);
      configActiveNodes.addAll(p2pConfig.getActiveNodes());
    } catch (P2pException e) {
      //no exception will throw
    }
  }

  public void init(PeerClient peerClient) {
    this.peerClient = peerClient;
    poolLoopExecutor.scheduleWithFixedDelay(() -> {
      try {
        connect(false);
      } catch (Exception t) {
        logger.error("Exception in poolLoopExecutor worker", t);
      }
    }, 200, 3600, TimeUnit.MILLISECONDS);

    if (p2pConfig.isDisconnectionPolicyEnable()) {
      disconnectExecutor.scheduleWithFixedDelay(() -> {
        try {
          check();
        } catch (Exception t) {
          logger.error("Exception in disconnectExecutor worker", t);
        }
      }, 30, 30, TimeUnit.SECONDS);
    }
  }

  private void addNode(Set<InetSocketAddress> inetSet, Node node) {
    if (node != null) {
      if (node.getInetSocketAddressV4() != null) {
        inetSet.add(node.getInetSocketAddressV4());
      }
      if (node.getInetSocketAddressV6() != null) {
        inetSet.add(node.getInetSocketAddressV6());
      }
    }
  }

  private void connect(boolean isFilterActiveNodes) {
    List<Node> connectNodes = new ArrayList<>();

    // Collect addresses and node IDs of registered peers in both directions.
    Set<InetAddress> addressInUse = new HashSet<>();
    Set<InetSocketAddress> inetInUse = new HashSet<>();
    Set<String> nodesInUse = new HashSet<>();
    nodesInUse.add(Hex.toHexString(p2pConfig.getNodeID()));
    ChannelManager.getAllChannels().forEach(channel -> {
      Node node = channel.getNode();
      if (node != null && !NetUtil.validPort(node.getPort())) {
        logger.warn("Close peer {} with invalid advertised port {}",
            channel.getInetSocketAddress(), node.getPort());
        channel.close();
        return;
      }
      if (StringUtils.isNotEmpty(channel.getNodeId())) {
        nodesInUse.add(channel.getNodeId());
      }
      addressInUse.add(channel.getInetAddress());
      inetInUse.add(channel.getInetSocketAddress());
      addNode(inetInUse, node);
    });

    addNode(inetInUse, new Node(Parameter.p2pConfig.getNodeID(), Parameter.p2pConfig.getIp(),
        Parameter.p2pConfig.getIpv6(), Parameter.p2pConfig.getPort()));

    p2pConfig.getActiveNodes().forEach(address -> {
      if (!isFilterActiveNodes
          && !inetInUse.contains(address)
          && !addressInUse.contains(address.getAddress())
          && !ConnectionPolicy.isBlocked(address)
          && peerClientCache.getIfPresent(address.getAddress()) == null) {
        addressInUse.add(address.getAddress());
        inetInUse.add(address);
        Node node = new Node(address); //use a random NodeId for config activeNodes
        if (node.getPreferInetSocketAddress() != null) {
          connectNodes.add(node);
        }
      }
    });

    // Dial active nodes first to account for any connections established immediately.
    connectNodes.forEach(node -> connect(node, true));
    connectNodes.clear();

    // The total target includes established peers and pending ordinary dials.
    // Startup-configured active nodes count only after their handshake completes.
    // Only the ordinary outbound target excludes the startup-configured active nodes.
    int activeLackSize = p2pConfig.getMinActiveConnections()
        - ordinaryOutboundConnectionCount.get();
    int totalLackSize = p2pConfig.getMinConnections() - getTotalConnectionCount();
    int size = StrictMath.max(0, StrictMath.max(totalLackSize, activeLackSize));
    int lackSize = size;
    if (lackSize > 0) {
      List<Node> connectableNodes = ChannelManager.getNodeDetectService().getConnectableNodes();
      for (Node node : connectableNodes) {
        // nodesInUse and inetInUse don't change in method `validNode`
        if (validNode(node, nodesInUse, inetInUse, null)) {
          connectNodes.add(node);
          nodesInUse.add(node.getHexId());
          inetInUse.add(node.getPreferInetSocketAddress());
          lackSize -= 1;
          if (lackSize <= 0) {
            break;
          }
        }
      }
    }

    if (lackSize > 0) {
      List<Node> connectableNodes = NodeManager.getConnectableNodes();
      // nodesInUse and inetInUse don't change in method `getNodes`
      List<Node> newNodes = getNodes(nodesInUse, inetInUse, connectableNodes, lackSize);
      connectNodes.addAll(newNodes);
      for (Node node : newNodes) {
        nodesInUse.add(node.getHexId());
        inetInUse.add(node.getPreferInetSocketAddress());
      }
      lackSize -= newNodes.size();
    }

    if (lackSize > 0 && !p2pConfig.getTreeUrls().isEmpty()) {
      List<DnsNode> dnsNodes = DnsManager.getDnsNodes();
      List<DnsNode> filtered = new ArrayList<>();
      Collections.shuffle(dnsNodes);
      for (DnsNode node : dnsNodes) {
        if (validNode(node, nodesInUse, inetInUse, null)) {
          DnsNode copyNode = (DnsNode) node.clone();
          copyNode.setId(NetUtil.getNodeId());
          //for node1 {ipv4_1, ipv6}, node2 {ipv4_2, ipv6}, we will not connect it twice
          addNode(inetInUse, node);
          filtered.add(copyNode);
        }
      }
      List<DnsNode> newNodes = CollectionUtils.truncate(filtered, lackSize);
      connectNodes.addAll(newNodes);
    }

    logger.debug("Connection replenishment: ordinary outbound deficit {}, selected nodes {}, "
            + "skip configured active nodes {}",
        size, connectNodes.size(), isFilterActiveNodes);
    // Start outbound connection attempts for the selected nodes.
    connectNodes.forEach(node -> connect(node, false));
  }

  private void connect(Node node, boolean activeCandidate) {
    InetSocketAddress address = node.getPreferInetSocketAddress();
    if (ConnectionPolicy.isBlocked(address)
        || (activeCandidate && !p2pConfig.getActiveNodes().contains(address))) {
      return;
    }
    logger.info("Connect to peer {}", address);
    peerClientCache.put(address.getAddress(), System.currentTimeMillis());
    ChannelFuture future = peerClient.connectAsync(node, false);
    if (future == null) {
      peerClientCache.invalidate(address.getAddress());
      return;
    }
    if (!configActiveNodes.contains(address)) {
      trackConnection(future);
      ordinaryOutboundConnectionCount.incrementAndGet();
    }
  }

  private synchronized int getTotalConnectionCount() {
    return registeredPeers.size() + pendingOrdinaryOutboundConnections.size();
  }

  private synchronized void trackConnection(ChannelFuture future) {
    io.netty.channel.Channel socket = future.channel();
    // The handshake may already have completed before connectAsync returns.
    if (registeredPeers.stream().noneMatch(peer -> peer.getCtx().channel() == socket)) {
      pendingOrdinaryOutboundConnections.add(socket);
    }
    socket.closeFuture().addListener(closed -> pendingOrdinaryOutboundConnections.remove(socket));
  }

  public List<Node> getNodes(Set<String> nodesInUse, Set<InetSocketAddress> inetInUse,
      List<Node> connectableNodes, int limit) {
    List<Node> filtered = new ArrayList<>();
    Set<InetSocketAddress> dynamicInetInUse = new HashSet<>(inetInUse);
    for (Node node : connectableNodes) {
      if (validNode(node, nodesInUse, inetInUse, dynamicInetInUse)) {
        filtered.add((Node) node.clone());
        addNode(dynamicInetInUse, node);
      }
    }

    filtered.sort(Comparator.comparingLong(node -> -node.getUpdateTime()));
    int candidateSize = StrictMath.max(limit * 10, minCandidateSize);
    if (filtered.size() > candidateSize) {
      filtered = filtered.subList(0, candidateSize);
    }
    Collections.shuffle(filtered);
    return CollectionUtils.truncate(filtered, limit);
  }

  private boolean validNode(Node node, Set<String> nodesInUse, Set<InetSocketAddress> inetInUse,
      Set<InetSocketAddress> dynamicInet) {
    if (node == null || !NetUtil.validPort(node.getPort())) {
      return false;
    }
    long now = System.currentTimeMillis();
    InetSocketAddress inetSocketAddress = node.getPreferInetSocketAddress();
    if (inetSocketAddress == null || inetSocketAddress.isUnresolved()) {
      return false;
    }
    InetAddress inetAddress = inetSocketAddress.getAddress();
    Long forbiddenTime = ChannelManager.getBannedNodes().getIfPresent(inetAddress);
    if (ConnectionPolicy.isBlocked(inetAddress)
        || (forbiddenTime != null && now <= forbiddenTime)
        || (ChannelManager.getConnectionNum(inetAddress)
        >= p2pConfig.getMaxConnectionsWithSameIp())
        || (node.getId() != null && nodesInUse.contains(node.getHexId()))
        || (peerClientCache.getIfPresent(inetAddress) != null)
        || inetInUse.contains(inetSocketAddress)
        || (dynamicInet != null && dynamicInet.contains(inetSocketAddress))) {
      return false;
    }
    return true;
  }

  private void check() {
    if (ChannelManager.getChannelCount() < p2pConfig.getMaxConnections()) {
      return;
    }

    List<Channel> channels = new ArrayList<>(registeredPeers);
    Collection<Channel> peers = channels.stream()
        .filter(peer -> !peer.isDisconnect())
        .filter(peer -> !peer.isTrustPeer())
        .filter(peer -> !peer.isOutbound())
        .collect(Collectors.toList());

    // Randomly disconnect an eligible inbound peer when the connection limit is reached.
    if (!peers.isEmpty()) {
      List<Channel> list = new ArrayList<>(peers);
      Channel peer = list.get(new Random().nextInt(peers.size()));
      logger.info("Randomly disconnect inbound peer {}", peer);
      peer.send(new P2pDisconnectMessage(DisconnectReason.RANDOM_ELIMINATION));
      peer.close();
    }
  }

  private synchronized void logPeerStats() {
    logger.info("Peer stats: channels {}, registeredPeers {}, outbound {}, inbound {}",
        ChannelManager.getChannelCount(), registeredPeers.size(), outboundPeerCount.get(),
        inboundPeerCount.get());
  }

  public void triggerConnect(InetSocketAddress address) {
    boolean activeNode = p2pConfig.getActiveNodes().contains(address);
    if (activeNode) {
      peerClientCache.invalidate(address.getAddress());
    }
    if (configActiveNodes.contains(address)) {
      return;
    }
    ordinaryOutboundConnectionCount.decrementAndGet();
    if (activeNode) {
      return;
    }
    if (poolLoopExecutor.getQueue().size() >= Parameter.CONN_MAX_QUEUE_SIZE) {
      logger.warn("ConnPool task' size is greater than or equal to {}",
          Parameter.CONN_MAX_QUEUE_SIZE);
      return;
    }
    try {
      if (!ChannelManager.isShutdown) {
        poolLoopExecutor.submit(() -> {
          try {
            connect(true);
          } catch (Exception t) {
            logger.error("Exception in poolLoopExecutor worker", t);
          }
        });
      }
    } catch (Exception e) {
      logger.warn("Submit task failed, message:{}", e.getMessage());
    }
  }

  @Override
  public synchronized void onConnect(Channel peer) {
    // Move the same socket from pending to established without counting it twice.
    pendingOrdinaryOutboundConnections.removeIf(socket -> socket == peer.getCtx().channel());
    // Channel.equals() compares remote addresses; membership must compare channel instances.
    if (registeredPeers.stream().noneMatch(channel -> channel == peer)) {
      if (!peer.isOutbound()) {
        inboundPeerCount.incrementAndGet();
      } else {
        outboundPeerCount.incrementAndGet();
      }
      registeredPeers.add(peer);
    }
    logPeerStats();
  }

  @Override
  public synchronized void onDisconnect(Channel peer) {
    if (registeredPeers.removeIf(channel -> channel == peer)) {
      if (!peer.isOutbound()) {
        inboundPeerCount.decrementAndGet();
      } else {
        outboundPeerCount.decrementAndGet();
      }
    }
    logPeerStats();
  }

  @Override
  public void onMessage(Channel channel, byte[] data) {
    //do nothing
  }

  public void close() {
    List<Channel> channels = new ArrayList<>(registeredPeers);
    try {
      channels.forEach(p -> {
        if (!p.isDisconnect()) {
          p.send(new P2pDisconnectMessage(DisconnectReason.PEER_QUITING));
          p.close();
        }
      });
      poolLoopExecutor.shutdownNow();
      disconnectExecutor.shutdownNow();
    } catch (Exception e) {
      logger.warn("Problems shutting down executor", e);
    }
  }
}
