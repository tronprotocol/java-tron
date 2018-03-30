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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.client.PeerClient;
import org.tron.common.overlay.discover.Node;
import org.tron.common.overlay.discover.NodeHandler;
import org.tron.common.overlay.discover.NodeManager;
import org.tron.core.config.args.Args;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerConnectionDelegate;

@Component
public class SyncPool {

  public static final Logger logger = LoggerFactory.getLogger("SyncPool");

  private static final long WORKER_TIMEOUT = 3;

  private final List<PeerConnection> activePeers = Collections.synchronizedList(new ArrayList<PeerConnection>());

  @Autowired
  private NodeManager nodeManager;

  @Autowired
  private ChannelManager channelManager;

  private PeerConnectionDelegate peerDel;

  private Args args = Args.getInstance();

  private int maxActiveNodes = args.getNodeMaxActiveNodes() > 0 ? args.getNodeMaxActiveNodes() : 30;

  private ScheduledExecutorService poolLoopExecutor = Executors.newSingleThreadScheduledExecutor();

  private ScheduledExecutorService logExecutor = Executors.newSingleThreadScheduledExecutor();

  private PeerClient peerClient;

  @Autowired
  public SyncPool(PeerConnectionDelegate peerDel, PeerClient peerClient) {
    this.peerDel = peerDel;
    this.peerClient = peerClient;
    init();
  }

  public void init() {
    poolLoopExecutor.scheduleWithFixedDelay(() -> {
      try {
        fillUp();
        prepareActive();
      } catch (Throwable t) {
        logger.error("Exception in sync worker", t);
      }
    }, WORKER_TIMEOUT, WORKER_TIMEOUT, TimeUnit.SECONDS);

    logExecutor.scheduleWithFixedDelay(() -> {
      try {
        logActivePeers();
      } catch (Throwable t) {
        logger.error("Exception in log worker", t);
      }
    }, 10, 10, TimeUnit.SECONDS);
  }

  private void fillUp() {
    int lackSize = maxActiveNodes - channelManager.getActivePeers().size();
    if(lackSize <= 0) return;

    final Set<String> nodesInUse = channelManager.nodesInUse();
    nodesInUse.add(nodeManager.getPublicHomeNode().getHexId());

    List<NodeHandler> newNodes = nodeManager.getNodes(new NodeSelector(nodesInUse), lackSize);
    newNodes.forEach(n -> peerClient.connectAsync(n.getNode().getHost(), n.getNode().getPort(),
            n.getNode().getHexId(), false));
  }

  private synchronized void prepareActive() {
    List<Channel> managerActive = new ArrayList<>(channelManager.getActivePeers());
    NodeSelector nodeSelector = new NodeSelector();
    List<PeerConnection> active = new ArrayList<>();
    for (Channel channel : managerActive) {
      if (nodeSelector.test(nodeManager.getNodeHandler(channel.getNode()))) {
        active.add((PeerConnection)channel);
      }
    }

    if (active.isEmpty()) return;

    active.sort(Comparator.comparingDouble(c -> c.getPeerStats().getAvgLatency()));

    for (PeerConnection channel : active) {
      if (!activePeers.contains(channel)) {
        peerDel.onConnectPeer(channel);
      }
    }

    activePeers.clear();
    activePeers.addAll(active);
  }

  synchronized void logActivePeers() {
    logger.info("-------- active node.");

    for (NodeHandler nodeHandler: nodeManager.getActiveNodes()){
      logger.info(nodeHandler.toString());
    }
    logger.info("-------- active channel {}, node in user size {}", channelManager.getActivePeers().size(), channelManager.nodesInUse().size());
    for (Channel channel: channelManager.getActivePeers()){
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
          sb.append(peer.logSyncStats()).append('\n');
        }
      }
      logger.info(sb.toString());
    }
  }

  public List<NodeHandler> getActiveNodes() {
    return nodeManager.getActiveNodes();
  }

  public synchronized List<PeerConnection> getActivePeers() {
    return new ArrayList<>(activePeers);
  }

  public synchronized void onDisconnect(Channel peer) {
    if (activePeers.contains(peer)) {
      logger.info("Peer {}: disconnected", peer.getPeerIdShort());
      peerDel.onDisconnectPeer((PeerConnection)peer);
      activePeers.remove(peer);
    }
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

    public NodeSelector() {}

    public NodeSelector(Set<String> nodesInUse) {
      this.nodesInUse = nodesInUse;
    }

    @Override
    public boolean test(NodeHandler handler) {

      //TODO: use reputation sysytem

      if (!nodeManager.isNodeAlive(handler)){
        return false;
      }

      if (handler.getNode().getHost().equals(nodeManager.getPublicHomeNode().getHost()) &&
              handler.getNode().getPort() == nodeManager.getPublicHomeNode().getPort()) {
        return false;
      }

      if (channelManager.isRecentlyDisconnected(handler.getInetSocketAddress().getAddress())){
          return false;
      }

      if (nodesInUse != null && nodesInUse.contains(handler.getNode().getHexId())) {
        return false;

      }

      return  true;
    }
  }

}
