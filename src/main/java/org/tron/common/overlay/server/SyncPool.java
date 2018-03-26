package org.tron.common.overlay.server;

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

import static java.lang.Math.min;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.discover.Node;
import org.tron.common.overlay.discover.NodeHandler;
import org.tron.common.overlay.discover.NodeManager;
import org.tron.common.utils.Utils;
import org.tron.core.config.args.Args;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerConnectionDelegate;

/**
 * <p>Encapsulates logic which manages peers involved in blockchain sync</p>
 *
 * Holds connections, bans, disconnects and other peers logic<br>
 * The pool is completely threadsafe<br>
 * Implements {@link Iterable} and can be used in "foreach" loop<br>
 *
 * @author Mikhail Kalinin
 * @since 10.08.2015
 */
@Component
public class SyncPool {

  public static final Logger logger = LoggerFactory.getLogger("SyncPool");

  private static final long WORKER_TIMEOUT = 3; // 3 seconds

  private final List<PeerConnection> activePeers = Collections.synchronizedList(new ArrayList<PeerConnection>());

  private BigInteger lowerUsefulDifficulty = BigInteger.ZERO;

//  @Autowired
//  private EthereumListener ethereumListener;

  @Autowired
  private NodeManager nodeManager;

  @Autowired
  private PeerConnectionDelegate peerDel;

  @Autowired
  private ChannelManager channelManager;

  private Args args;

  private ScheduledExecutorService poolLoopExecutor = Executors.newSingleThreadScheduledExecutor();

  private Predicate<NodeHandler> nodesSelector;
  private ScheduledExecutorService logExecutor = Executors.newSingleThreadScheduledExecutor();

  @Autowired
  public SyncPool(final Args args) {
    this.args = args;
    init(channelManager, null);
  }

  public void init(final ChannelManager channelManager, PeerConnectionDelegate peerDel) {
    if (this.channelManager != null) return; // inited already
    this.channelManager = channelManager;
    //updateLowerUsefulDifficulty();x
    this.peerDel = peerDel;

    //updateLowerUsefulDifficulty();

    poolLoopExecutor.scheduleWithFixedDelay(() -> {
      try {
        heartBeat();
////        updateLowerUsefulDifficulty();
        fillUp();
        prepareActive();
        cleanupActive();
      } catch (Throwable t) {
        logger.error("Unhandled exception", t);
      }
    }, WORKER_TIMEOUT, WORKER_TIMEOUT, TimeUnit.SECONDS);


    logExecutor.scheduleWithFixedDelay(() -> {
      try {
        logActivePeers();
        logger.info("\n");
      } catch (Throwable t) {
        t.printStackTrace();
        logger.error("Exception in log worker", t);
      }
    }, 10, 10, TimeUnit.SECONDS);
  }

  public void setNodesSelector(Predicate<NodeHandler> nodesSelector) {
    this.nodesSelector = nodesSelector;
  }

  public void close() {
    try {
      poolLoopExecutor.shutdownNow();
      logExecutor.shutdownNow();
    } catch (Exception e) {
      logger.warn("Problems shutting down executor", e);
    }
  }

  @Nullable
  public synchronized Channel getAnyIdle() {
    ArrayList<Channel> channels = new ArrayList<>(activePeers);
    Collections.shuffle(channels);
    for (Channel peer : channels) {
      if (peer.isIdle())
        return peer;
    }
    return null;
  }

  @Nullable
  public synchronized Channel getBestIdle() {
    for (Channel peer : activePeers) {
      if (peer.isIdle())
        return peer;
    }
    return null;
  }

  @Nullable
  public synchronized Channel getNotLastIdle() {
    ArrayList<Channel> channels = new ArrayList<>(activePeers);
    Collections.shuffle(channels);
    Channel candidate = null;
    for (Channel peer : channels) {
      if (peer.isIdle()) {
        if (candidate == null) {
          candidate = peer;
        } else {
          return candidate;
        }
      }
    }
    return null;
  }

  public synchronized List<Channel> getAllIdle() {
    List<Channel> ret = new ArrayList<>();
    for (Channel peer : activePeers) {
      if (peer.isIdle())
        ret.add(peer);
    }
    return ret;
  }

  public synchronized List<PeerConnection> getActivePeers() {
    return new ArrayList<>(activePeers);
  }

  public synchronized int getActivePeersCount() {
    return activePeers.size();
  }

  @Nullable
  public synchronized Channel getByNodeId(byte[] nodeId) {
    return channelManager.getActivePeer(nodeId);
  }

  public synchronized void onDisconnect(Channel peer) {
    if (activePeers.remove(peer)) {
      logger.info("Peer {}: disconnected", peer.getPeerIdShort());
    }
  }

//  public synchronized Set<String> nodesInUse() {
//    Set<String> ids = new HashSet<>();
//    if (channelManager.getActivePeers() == null){
//      return ids;
//    }
//    for (Channel peer : channelManager.getActivePeers()) {
//      ids.add(peer.getPeerId());
//    }
//    return ids;
//  }

  public synchronized Set<String> nodesInUse() {
    Set<String> ids = new HashSet<>();
    for (Channel peer : channelManager.getActivePeers()) {
      ids.add(peer.getPeerId());
    }
    return ids;
  }

  synchronized void logActivePeers() {
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
          sb.append(((PeerConnection)peer).logSyncStats()).append('\n');
        }
      }
      logger.info(sb.toString());
    }
  }

  class NodeSelector implements Predicate<NodeHandler> {
    Set<String> nodesInUse;

    public NodeSelector() {
    }

    public NodeSelector(Set<String> nodesInUse) {
      this.nodesInUse = nodesInUse;
    }

    @Override
    public boolean test(NodeHandler handler) {

      if (nodesInUse != null && nodesInUse.contains(handler.getNode().getHexId())) {
        return false;
      }

      if (handler.getNode().getId().length == 0) {
        return false;
      }

      if (handler.getNode().getPort() == 0) {
        return false;
      }

//      if (!handler.getState().equals(State.Active)) {
//        return false;
//      }

      return  true;

//
//      if (handler.getNodeStatistics().isPredefined()) return true;
//
//      if (nodesSelector != null && !nodesSelector.test(handler)) return false;
//
//      if (lowerDifficulty.compareTo(BigInteger.ZERO) > 0 &&
//          handler.getNodeStatistics().getEthTotalDifficulty() == null) {
//        return false;
//      }
//
//      if (handler.getNodeStatistics().getReputation() < 100) return false;
//
//      return handler.getNodeStatistics().getEthTotalDifficulty().compareTo(lowerDifficulty) >= 0;
//
//      if (handler.getNodeStatistics().isPredefined()) return true;
//
//      if (nodesSelector != null && !nodesSelector.test(handler)) return false;
//
//      //TODO: use reputation sysytem
//      //if (handler.getNodeStatistics().getReputation() < 100) return false;
//      return true;
    }
  }

  private void fillUp() {
    //int lackSize = args.getNodeMaxActiveNodes() - channelManager.getActivePeers().size();
    //if(lackSize <= 0) return;
    int lackSize = 10;
    final Set<String> nodesInUse = nodesInUse();
    nodesInUse.add(Hex.toHexString(nodeManager.getPublicHomeNode().getId()));   // exclude home node


    //TODO: here can only use TCP connect seed peer.
    List<NodeHandler> newNodes;
    newNodes = nodeManager.getNodes(new NodeSelector(nodesInUse), lackSize);
   // newNodes = new ArrayList<>();

    //newNodes.add(nodeManager.getNodeHandler(new Node()))

    if (logger.isTraceEnabled()) {
      logDiscoveredNodes(newNodes);
    }

    //todo exclude home node from k bucket
    for(NodeHandler n : newNodes) {
      if (!nodeManager.isTheSameNode(n.getNode(), nodeManager.getPublicHomeNode())){

        logger.info("connect node--------------------");
        logger.info(n.getNode().toString() + " | " + n.getState().toString());
        channelManager.connect(n.getNode());
      }else {
        logger.info("isTheHomeNode {}", n.getNode());
      }
    }
  }


  private synchronized void prepareActive() {
    List<Channel> managerActive = new ArrayList<>(channelManager.getActivePeers());

    // Filtering out with nodeSelector because server-connected nodes were not tested
    NodeSelector nodeSelector = new NodeSelector();
    List<PeerConnection> active = new ArrayList<>();
    for (Channel channel : managerActive) {
      if (nodeSelector.test(nodeManager.getNodeHandler(channel.getNode()))) {
        active.add((PeerConnection)channel);
      }
    }

    if (active.isEmpty()) return;

//    // filtering by 20% from top difficulty
//    active.sort((c1, c2) -> c2.getTotalDifficulty().compareTo(c1.getTotalDifficulty()));
//
//    BigInteger highestDifficulty = active.get(0).getTotalDifficulty();
    int thresholdIdx = (int) (min(Args.getInstance().getSyncNodeCount(), active.size()) - 1);
//
//    for (int i = thresholdIdx; i >= 0; i--) {
//      if (isIn20PercentRange(active.get(i).getTotalDifficulty(), highestDifficulty)) {
//        thresholdIdx = i;
//        break;
//      }
//    }

    //List<PeerConnection> filtered = active.subList(0, thresholdIdx + 1);

    // sorting by latency in asc order
    active.sort(Comparator.comparingDouble(c -> c.getPeerStats().getAvgLatency()));

    for (PeerConnection channel : active) {
      if (!activePeers.contains(channel)) {
        peerDel.onConnectPeer(channel);
      }
    }

    activePeers.clear();
    activePeers.addAll(active);
  }

  private synchronized void cleanupActive() {
    Iterator<PeerConnection> iterator = activePeers.iterator();
    while (iterator.hasNext()) {
      PeerConnection next = iterator.next();
      if (next.isDisconnected()) {
        peerDel.onDisconnectPeer(next);
        logger.info("Removing peer " + next + " from active due to disconnect.");
        iterator.remove();
      }
    }
  }


  private void logDiscoveredNodes(List<NodeHandler> nodes) {
    StringBuilder sb = new StringBuilder();
    for(NodeHandler n : nodes) {
      sb.append(Utils.getIdShort(Hex.toHexString(n.getNode().getId())));
      sb.append(", ");
    }
    if(sb.length() > 0) {
      sb.delete(sb.length() - 2, sb.length());
    }
    logger.trace(
        "Node list obtained from discovery: {}",
        nodes.size() > 0 ? sb.toString() : "empty"
    );
  }

//  private void updateLowerUsefulDifficulty() {
//    BigInteger td = blockchain.getTotalDifficulty();
//    if (td.compareTo(lowerUsefulDifficulty) > 0) {
//      lowerUsefulDifficulty = td;
//    }
//  }

  public ChannelManager getChannelManager() {
    return channelManager;
  }

  private void heartBeat() {
//        for (Channel peer : channelManager.getActivePeers()) {
//            if (!peer.isIdle() && peer.getSyncStats().secondsSinceLastUpdate() > config.peerChannelReadTimeout()) {
//                logger.info("Peer {}: no response after {} seconds", peer.getPeerIdShort(), config.peerChannelReadTimeout());
//                peer.dropConnection();
//            }
//        }
  }
}
