package org.tron.p2p.connection.business.pool;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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

  private final List<Channel> activePeers = Collections.synchronizedList(new ArrayList<>());
  private final Cache<InetAddress, Long> peerClientCache = CacheBuilder.newBuilder()
      .maximumSize(1000).expireAfterWrite(120, TimeUnit.SECONDS).recordStats().build();
  @Getter
  private final AtomicInteger passivePeersCount = new AtomicInteger(0);
  @Getter
  private final AtomicInteger activePeersCount = new AtomicInteger(0);
  @Getter
  private final AtomicInteger connectingPeersCount = new AtomicInteger(0);
  private final ScheduledThreadPoolExecutor poolLoopExecutor = new ScheduledThreadPoolExecutor(1,
      BasicThreadFactory.builder().namingPattern("connPool").build());
  private final ScheduledExecutorService disconnectExecutor =
      Executors.newSingleThreadScheduledExecutor(
          BasicThreadFactory.builder().namingPattern("randomDisconnect").build());

  public P2pConfig p2pConfig = Parameter.p2pConfig;
  private PeerClient peerClient;
  private final List<InetSocketAddress> configActiveNodes = new ArrayList<>();

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
        log.error("Exception in poolLoopExecutor worker", t);
      }
    }, 200, 3600, TimeUnit.MILLISECONDS);

    if (p2pConfig.isDisconnectionPolicyEnable()) {
      disconnectExecutor.scheduleWithFixedDelay(() -> {
        try {
          check();
        } catch (Exception t) {
          log.error("Exception in disconnectExecutor worker", t);
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

    //collect already used nodes in channelManager
    Set<InetAddress> addressInUse = new HashSet<>();
    Set<InetSocketAddress> inetInUse = new HashSet<>();
    Set<String> nodesInUse = new HashSet<>();
    nodesInUse.add(Hex.toHexString(p2pConfig.getNodeID()));
    ChannelManager.getChannels().values().forEach(channel -> {
      if (StringUtils.isNotEmpty(channel.getNodeId())) {
        nodesInUse.add(channel.getNodeId());
      }
      addressInUse.add(channel.getInetAddress());
      inetInUse.add(channel.getInetSocketAddress());
      addNode(inetInUse, channel.getNode());
    });

    addNode(inetInUse, new Node(Parameter.p2pConfig.getNodeID(), Parameter.p2pConfig.getIp(),
        Parameter.p2pConfig.getIpv6(), Parameter.p2pConfig.getPort()));

    p2pConfig.getActiveNodes().forEach(address -> {
      if (!isFilterActiveNodes && !inetInUse.contains(address) && !addressInUse.contains(
          address.getAddress())) {
        addressInUse.add(address.getAddress());
        inetInUse.add(address);
        Node node = new Node(address); //use a random NodeId for config activeNodes
        if (node.getPreferInetSocketAddress() != null) {
          connectNodes.add(node);
        }
      }
    });

    //calculate lackSize exclude config activeNodes
    int activeLackSize = p2pConfig.getMinActiveConnections() - connectingPeersCount.get();
    int size = Math.max(
        p2pConfig.getMinConnections() - connectingPeersCount.get() - passivePeersCount.get(),
        activeLackSize);
    if (p2pConfig.getMinConnections() <= activePeers.size() && activeLackSize <= 0) {
      size = 0;
    }
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

    log.debug("Lack size:{}, connectNodes size:{}, is disconnect trigger: {}",
        size, connectNodes.size(), isFilterActiveNodes);
    //establish tcp connection with chose nodes by peerClient
    {
      connectNodes.forEach(n -> {
        log.info("Connect to peer {}", n.getPreferInetSocketAddress());
        peerClient.connectAsync(n, false);
        peerClientCache.put(n.getPreferInetSocketAddress().getAddress(),
            System.currentTimeMillis());
        if (!configActiveNodes.contains(n.getPreferInetSocketAddress())) {
          connectingPeersCount.incrementAndGet();
        }
      });
    }
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
    return CollectionUtils.truncate(filtered, limit);
  }

  private boolean validNode(Node node, Set<String> nodesInUse, Set<InetSocketAddress> inetInUse,
      Set<InetSocketAddress> dynamicInet) {
    long now = System.currentTimeMillis();
    InetSocketAddress inetSocketAddress = node.getPreferInetSocketAddress();
    InetAddress inetAddress = inetSocketAddress.getAddress();
    Long forbiddenTime = ChannelManager.getBannedNodes().getIfPresent(inetAddress);
    if ((forbiddenTime != null && now <= forbiddenTime)
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
    if (ChannelManager.getChannels().size() < p2pConfig.getMaxConnections()) {
      return;
    }

    List<Channel> channels = new ArrayList<>(activePeers);
    Collection<Channel> peers = channels.stream()
        .filter(peer -> !peer.isDisconnect())
        .filter(peer -> !peer.isTrustPeer())
        .filter(peer -> !peer.isActive())
        .collect(Collectors.toList());

    // if len(peers) >= 0, disconnect randomly
    if (!peers.isEmpty()) {
      List<Channel> list = new ArrayList<>(peers);
      Channel peer = list.get(new Random().nextInt(peers.size()));
      log.info("Disconnect with peer randomly: {}", peer);
      peer.send(new P2pDisconnectMessage(DisconnectReason.RANDOM_ELIMINATION));
      peer.close();
    }
  }

  private synchronized void logActivePeers() {
    log.info("Peer stats: channels {}, activePeers {}, active {}, passive {}",
        ChannelManager.getChannels().size(), activePeers.size(), activePeersCount.get(),
        passivePeersCount.get());
  }

  public void triggerConnect(InetSocketAddress address) {
    if (configActiveNodes.contains(address)) {
      return;
    }
    connectingPeersCount.decrementAndGet();
    if (poolLoopExecutor.getQueue().size() >= Parameter.CONN_MAX_QUEUE_SIZE) {
      log.warn("ConnPool task' size is greater than or equal to {}", Parameter.CONN_MAX_QUEUE_SIZE);
      return;
    }
    try {
      if (!ChannelManager.isShutdown) {
        poolLoopExecutor.submit(() -> {
          try {
            connect(true);
          } catch (Exception t) {
            log.error("Exception in poolLoopExecutor worker", t);
          }
        });
      }
    } catch (Exception e) {
      log.warn("Submit task failed, message:{}", e.getMessage());
    }
  }

  @Override
  public synchronized void onConnect(Channel peer) {
    if (!activePeers.contains(peer)) {
      if (!peer.isActive()) {
        passivePeersCount.incrementAndGet();
      } else {
        activePeersCount.incrementAndGet();
      }
      activePeers.add(peer);
    }
    logActivePeers();
  }

  @Override
  public synchronized void onDisconnect(Channel peer) {
    if (activePeers.contains(peer)) {
      if (!peer.isActive()) {
        passivePeersCount.decrementAndGet();
      } else {
        activePeersCount.decrementAndGet();
      }
      activePeers.remove(peer);
    }
    logActivePeers();
  }

  @Override
  public void onMessage(Channel channel, byte[] data) {
    //do nothing
  }

  public void close() {
    List<Channel> channels = new ArrayList<>(activePeers);
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
      log.warn("Problems shutting down executor", e);
    }
  }
}
