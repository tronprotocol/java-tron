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

import org.apache.commons.collections4.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.message.ReasonCode;
import org.tron.core.config.args.Args;
import org.tron.core.db.ByteArrayWrapper;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;

import static org.tron.common.overlay.message.ReasonCode.DUPLICATE_PEER;
import static org.tron.common.overlay.message.ReasonCode.TOO_MANY_PEERS;


@Component
public class ChannelManager {

  private static final Logger logger = LoggerFactory.getLogger("ChannelManager");

  private static final int inboundConnectionBanTimeout = 10 * 1000;

  private List<Channel> newPeers = new CopyOnWriteArrayList<>();

  private final Map<ByteArrayWrapper, Channel> activePeers = new ConcurrentHashMap<>();

  private Map<InetAddress, Date> recentlyDisconnected = Collections.synchronizedMap(new LRUMap<InetAddress, Date>(500));

  private ScheduledExecutorService mainWorker = Executors.newSingleThreadScheduledExecutor();

  private Args args = Args.getInstance();

  private int maxActivePeers = args.getNodeMaxActiveNodes() > 0 ? args.getNodeMaxActiveNodes() : 30;

  private PeerServer peerServer;

  @Autowired
  private SyncPool syncPool;

  @Autowired
  private ChannelManager(final PeerServer peerServer) {
    this.peerServer = peerServer;

    mainWorker.scheduleWithFixedDelay(() -> {
      try {
        processNewPeers();
      } catch (Throwable t) {
        logger.error("Error", t);
      }
    }, 0, 1, TimeUnit.SECONDS);

    if (this.args.getNodeListenPort() > 0) {
      new Thread(() -> peerServer.start(Args.getInstance().getNodeListenPort()),
        "PeerServerThread").start();
    }
  }

  public Set<String> nodesInUse() {
    Set<String> ids = new HashSet<>();
    for (Channel peer : getActivePeers()) {
      ids.add(peer.getPeerId());
    }
    for (Channel peer : newPeers) {
      ids.add(peer.getPeerId());
    }
    return ids;
  }

  private void processNewPeers() {
    if (newPeers.isEmpty()) {
      return;
    }
    List<Channel> processed = new ArrayList<>();
    int addCnt = 0;
    for (Channel peer : newPeers) {
      if (peer.isProtocolsInitialized()) {
        if (!activePeers.containsKey(peer.getNodeIdWrapper())) {
          if (!peer.isActive() && activePeers.size() >= maxActivePeers //&& !trustedPeers.accept(peer.getNode())
              ) {
            disconnect(peer, TOO_MANY_PEERS);
          } else {
            logger.info("Add active peer {}", peer);
            activePeers.put(peer.getNodeIdWrapper(), peer);
            addCnt++;
          }
        } else {
          disconnect(peer, DUPLICATE_PEER);
        }
        processed.add(peer);
      }
    }

    if (addCnt > 0) {
      logger.info("New peers processed: " + processed + ", active peers added: " + addCnt
          + ", total active peers: " + activePeers.size());
    }

    newPeers.removeAll(processed);
  }

  public void disconnect(Channel peer, ReasonCode reason) {
    logger.info("Disconnecting peer with reason " + reason + ": " + peer);
    peer.disconnect(reason);
    recentlyDisconnected.put(peer.getInetSocketAddress().getAddress(), new Date());
  }

  public void notifyDisconnect(Channel channel) {
    syncPool.onDisconnect(channel);
    activePeers.values().remove(channel);
    newPeers.remove(channel);
  }

  public boolean isRecentlyDisconnected(InetAddress peerAddr) {
    Date disconnectTime = recentlyDisconnected.get(peerAddr);
    if (disconnectTime != null &&
        System.currentTimeMillis() - disconnectTime.getTime() < inboundConnectionBanTimeout) {
      return true;
    } else {
      recentlyDisconnected.remove(peerAddr);
      return false;
    }
  }

  public void add(Channel peer) {
    newPeers.add(peer);
  }

  public Collection<Channel> getActivePeers() {
    return new ArrayList<>(activePeers.values());
  }

  public void close() {
    try {
      mainWorker.shutdownNow();
      mainWorker.awaitTermination(5, TimeUnit.SECONDS);
    } catch (Exception e) {
      logger.warn("Problems shutting down", e);
    }
    peerServer.close();

    ArrayList<Channel> allPeers = new ArrayList<>(activePeers.values());
    allPeers.addAll(newPeers);

    for (Channel channel : allPeers) {
      try {
        //channel.dropConnection();
      } catch (Exception e) {
        logger.warn("Problems disconnecting channel " + channel, e);
      }
    }
  }
}
