package org.tron.common.overlay.server;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.client.PeerClient;
import org.tron.common.overlay.discover.node.NodeHandler;
import org.tron.common.overlay.discover.node.NodeManager;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.Args;
import org.tron.core.net.peer.PeerConnection;

@Slf4j(topic = "net")
@Component
public class SyncPool {

  private final List<PeerConnection> activePeers = Collections
      .synchronizedList(new ArrayList<>());
  private final AtomicInteger passivePeersCount = new AtomicInteger(0);
  private final AtomicInteger activePeersCount = new AtomicInteger(0);
  private double factor = Args.getInstance().getConnectFactor();
  private double activeFactor = Args.getInstance().getActiveConnectFactor();
  private Cache<NodeHandler, Long> nodeHandlerCache = CacheBuilder.newBuilder()
      .maximumSize(1000).expireAfterWrite(180, TimeUnit.SECONDS).recordStats().build();

  @Autowired
  private NodeManager nodeManager;

  @Autowired
  private ApplicationContext ctx;

  private ChannelManager channelManager;

  private CommonParameter commonParameter = CommonParameter.getInstance();

  private int maxActiveNodes = commonParameter.getNodeMaxActiveNodes();

  private int maxActivePeersWithSameIp = commonParameter.getNodeMaxActiveNodesWithSameIp();

  private ScheduledExecutorService poolLoopExecutor = Executors.newSingleThreadScheduledExecutor();

  private ScheduledExecutorService logExecutor = Executors.newSingleThreadScheduledExecutor();

  private PeerClient peerClient;

  public void init() {

    channelManager = ctx.getBean(ChannelManager.class);

    peerClient = ctx.getBean(PeerClient.class);

    poolLoopExecutor.scheduleWithFixedDelay(() -> {
      try {
        fillUp();
      } catch (Throwable t) {
        logger.error("Exception in sync worker", t);
      }
    }, 30000, 3600, TimeUnit.MILLISECONDS);

    logExecutor.scheduleWithFixedDelay(() -> {
      try {
        logActivePeers();
      } catch (Throwable t) {
        logger.error("Exception in sync worker", t);
      }
    }, 30, 10, TimeUnit.SECONDS);
  }

  private void fillUp() {
    List<NodeHandler> connectNodes = new ArrayList<>();
    Set<InetAddress> addressInUse = new HashSet<>();
    Set<String> nodesInUse = new HashSet<>();
    channelManager.getActivePeers().forEach(channel -> {
      nodesInUse.add(channel.getPeerId());
      addressInUse.add(channel.getInetAddress());
    });

    channelManager.getActiveNodes().forEach((address, node) -> {
      nodesInUse.add(node.getHexId());
      if (!addressInUse.contains(address)) {
        connectNodes.add(nodeManager.getNodeHandler(node));
      }
    });

    int size = Math.max((int) (maxActiveNodes * factor) - activePeers.size(),
        (int) (maxActiveNodes * activeFactor - activePeersCount.get()));
    int lackSize = size - connectNodes.size();
    if (lackSize > 0) {
      nodesInUse.add(nodeManager.getPublicHomeNode().getHexId());
      List<NodeHandler> newNodes = nodeManager.getNodes(new NodeSelector(nodesInUse), lackSize);
      connectNodes.addAll(newNodes);
    }

    connectNodes.forEach(n -> {
      peerClient.connectAsync(n, false);
      nodeHandlerCache.put(n, System.currentTimeMillis());
    });
  }

  synchronized void logActivePeers() {
    String str = String.format("\n\n============ Peer stats: all %d, active %d, passive %d\n\n",
        channelManager.getActivePeers().size(), activePeersCount.get(), passivePeersCount.get());
    StringBuilder sb = new StringBuilder(str);
    for (PeerConnection peer : new ArrayList<>(activePeers)) {
      sb.append(peer.log()).append('\n');
    }
    logger.info(sb.toString());
  }

  public List<PeerConnection> getActivePeers() {
    List<PeerConnection> peers = Lists.newArrayList();
    activePeers.forEach(peer -> {
      if (!peer.isDisconnect()) {
        peers.add(peer);
      }
    });
    return peers;
  }

  public synchronized void onConnect(Channel peer) {
    PeerConnection peerConnection = (PeerConnection) peer;
    if (!activePeers.contains(peerConnection)) {
      if (!peerConnection.isActive()) {
        passivePeersCount.incrementAndGet();
      } else {
        activePeersCount.incrementAndGet();
      }
      activePeers.add(peerConnection);
      activePeers
          .sort(Comparator.comparingDouble(
              c -> c.getNodeStatistics().pingMessageLatency.getAvg()));
      peerConnection.onConnect();
    }
  }

  public synchronized void onDisconnect(Channel peer) {
    PeerConnection peerConnection = (PeerConnection) peer;
    if (activePeers.contains(peerConnection)) {
      if (!peerConnection.isActive()) {
        passivePeersCount.decrementAndGet();
      } else {
        activePeersCount.decrementAndGet();
      }
      activePeers.remove(peerConnection);
      peerConnection.onDisconnect();
    }
  }

  public boolean isCanConnect() {
    return passivePeersCount.get() < maxActiveNodes * (1 - activeFactor);
  }

  public void close() {
    try {
      poolLoopExecutor.shutdownNow();
      logExecutor.shutdownNow();
    } catch (Exception e) {
      logger.warn("Problems shutting down executor", e);
    }
  }

  public AtomicInteger getPassivePeersCount() {
    return passivePeersCount;
  }

  public AtomicInteger getActivePeersCount() {
    return activePeersCount;
  }

  class NodeSelector implements Predicate<NodeHandler> {

    private Set<String> nodesInUse;

    public NodeSelector(Set<String> nodesInUse) {
      this.nodesInUse = nodesInUse;
    }

    @Override
    public boolean test(NodeHandler handler) {

      InetAddress inetAddress = handler.getInetSocketAddress().getAddress();

      return !((handler.getNode().getHost().equals(nodeManager.getPublicHomeNode().getHost())
          && handler.getNode().getPort() == nodeManager.getPublicHomeNode().getPort())
          || (channelManager.getRecentlyDisconnected().getIfPresent(inetAddress) != null)
          || (channelManager.getBadPeers().getIfPresent(inetAddress) != null)
          || (channelManager.getConnectionNum(inetAddress) >= maxActivePeersWithSameIp)
          || (nodesInUse.contains(handler.getNode().getHexId()))
          || (nodeHandlerCache.getIfPresent(handler) != null));
    }
  }

}
