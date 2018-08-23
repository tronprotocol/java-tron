/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.client.PeerClient;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.overlay.discover.node.NodeHandler;
import org.tron.common.overlay.discover.node.NodeManager;
import org.tron.common.overlay.discover.node.statistics.NodeStatistics;
import org.tron.core.config.args.Args;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerConnectionDelegate;

@Component
public class SyncPool {

  public static final Logger logger = LoggerFactory.getLogger("SyncPool");

  private double factor = Args.getInstance().getConnectFactor();
  private double activeFactor = Args.getInstance().getActiveConnectFactor();

  private final List<PeerConnection> activePeers = Collections
      .synchronizedList(new ArrayList<PeerConnection>());
  private final AtomicInteger passivePeersCount = new AtomicInteger(0);
  private final AtomicInteger activePeersCount = new AtomicInteger(0);

  private Cache<NodeHandler, Long> nodeHandlerCache = CacheBuilder.newBuilder()
      .maximumSize(1000).expireAfterWrite(180, TimeUnit.SECONDS).recordStats().build();

  @Autowired
  private NodeManager nodeManager;

  @Autowired
  private ApplicationContext ctx;

  private ChannelManager channelManager;

  private PeerConnectionDelegate peerDel;

  private Args args = Args.getInstance();

  private int maxActiveNodes = args.getNodeMaxActiveNodes();

  private int getMaxActivePeersWithSameIp = args.getNodeMaxActiveNodesWithSameIp();

  private ScheduledExecutorService poolLoopExecutor = Executors.newSingleThreadScheduledExecutor();

  private ScheduledExecutorService logExecutor = Executors.newSingleThreadScheduledExecutor();

  private PeerClient peerClient;

  public void init(PeerConnectionDelegate peerDel) {
    this.peerDel = peerDel;

    channelManager = ctx.getBean(ChannelManager.class);

    peerClient = ctx.getBean(PeerClient.class);

    for (Node node : args.getActiveNodes()) {
      nodeManager.getNodeHandler(node).getNodeStatistics().setPredefined(true);
    }

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
      }
    }, 30, 10, TimeUnit.SECONDS);
  }

  private void fillUp() {
    int lackSize = Math.max((int) (maxActiveNodes * factor) - activePeers.size(),
        (int) (maxActiveNodes * activeFactor - activePeersCount.get()));
    if (lackSize <= 0) {
      return;
    }

    final Set<String> nodesInUse = new HashSet<>();
    channelManager.getActivePeers().forEach(channel -> nodesInUse.add(channel.getPeerId()));
    nodesInUse.add(nodeManager.getPublicHomeNode().getHexId());

    List<NodeHandler> newNodes = nodeManager.getNodes(new NodeSelector(nodesInUse), lackSize);
    newNodes.forEach(n -> {
      peerClient.connectAsync(n, false);
      nodeHandlerCache.put(n, System.currentTimeMillis());
    });
  }

  // for test only
  public void addActivePeers(PeerConnection p) {
    activePeers.add(p);
  }


  synchronized void logActivePeers() {

    logger.info("-------- active connect channel {}", activePeersCount.get());
    logger.info("-------- passive connect channel {}", passivePeersCount.get());
    logger.info("-------- all connect channel {}", channelManager.getActivePeers().size());
    for (Channel channel : channelManager.getActivePeers()) {
      logger.info(channel.toString());
    }

    if (logger.isInfoEnabled()) {
      StringBuilder sb = new StringBuilder("Peer stats:\n");
      sb.append("Active peers\n");
      sb.append("============\n");
      Set<Node> activeSet = new HashSet<>();
      for (PeerConnection peer : new ArrayList<>(activePeers)) {
        sb.append(peer.logSyncStats()).append('\n');
        activeSet.add(peer.getNode());
      }
      sb.append("Other connected peers\n");
      sb.append("============\n");
      for (Channel peer : new ArrayList<>(channelManager.getActivePeers())) {
        if (!activeSet.contains(peer.getNode())) {
          sb.append(peer.getNode()).append('\n');
        }
      }
      logger.info(sb.toString());
    }
  }

  public synchronized List<PeerConnection> getActivePeers() {
    List<PeerConnection> peers = Lists.newArrayList();
    activePeers.forEach(peer -> {
      if (!peer.isDisconnect()) {
        peers.add(peer);
      }
    });
    return peers;
  }

  public synchronized void onConnect(Channel peer) {
    if (!activePeers.contains(peer)) {
      if (!peer.isActive()) {
        passivePeersCount.incrementAndGet();
      } else {
        activePeersCount.incrementAndGet();
      }
      activePeers.add((PeerConnection) peer);
      activePeers.sort(Comparator.comparingDouble(c -> c.getPeerStats().getAvgLatency()));
      peerDel.onConnectPeer((PeerConnection) peer);
    }
  }

  public synchronized void onDisconnect(Channel peer) {
    if (activePeers.contains(peer)) {
      if (!peer.isActive()) {
        passivePeersCount.decrementAndGet();
      } else {
        activePeersCount.decrementAndGet();
      }
      activePeers.remove(peer);
      peerDel.onDisconnectPeer((PeerConnection) peer);
    }
  }

  public boolean isCanConnect() {
    if (passivePeersCount.get() >= maxActiveNodes * (1 - activeFactor)) {
      return false;
    }
    return true;
  }

  public void close() {
    try {
      poolLoopExecutor.shutdownNow();
      logExecutor.shutdownNow();
    } catch (Exception e) {
      logger.warn("Problems shutting down executor", e);
    }
  }

  class NodeSelector implements Predicate<NodeHandler> {

    Set<String> nodesInUse;

    public NodeSelector(Set<String> nodesInUse) {
      this.nodesInUse = nodesInUse;
    }

    @Override
    public boolean test(NodeHandler handler) {

      if (handler.getNode().getHost().equals(nodeManager.getPublicHomeNode().getHost()) &&
          handler.getNode().getPort() == nodeManager.getPublicHomeNode().getPort()) {
        return false;
      }

      if (nodesInUse != null && nodesInUse.contains(handler.getNode().getHexId())) {
        return false;
      }

      if (handler.getNodeStatistics().getReputation() >= NodeStatistics.REPUTATION_PREDEFINED){
        return true;
      }

      InetAddress inetAddress = handler.getInetSocketAddress().getAddress();
      if (channelManager.getRecentlyDisconnected().getIfPresent(inetAddress) != null) {
        return false;
      }
      if (channelManager.getBadPeers().getIfPresent(inetAddress) != null) {
        return false;
      }
      if (channelManager.getConnectionNum(inetAddress) >= getMaxActivePeersWithSameIp){
        return false;
      }

      if (nodeHandlerCache.getIfPresent(handler) != null) {
        return false;
      }

      if (handler.getNodeStatistics().getReputation() < 100) {
        return false;
      }

      return true;
    }
  }

}
