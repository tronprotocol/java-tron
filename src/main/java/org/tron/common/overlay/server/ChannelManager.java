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

import static org.tron.common.overlay.message.ReasonCode.DUPLICATE_PEER;
import static org.tron.common.overlay.message.ReasonCode.TOO_MANY_PEERS;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.ReadTimeoutException;
import org.apache.commons.collections4.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.message.DisconnectMessage;
import org.tron.common.overlay.message.ReasonCode;
import org.tron.core.config.args.Args;
import org.tron.core.db.ByteArrayWrapper;


@Component
public class ChannelManager {

  private static final Logger logger = LoggerFactory.getLogger("ChannelManager");

  private static final int inboundConnectionBanTimeout = 60 * 1000;

  private List<Channel> newPeers = new CopyOnWriteArrayList<>();

  private final Map<ByteArrayWrapper, Channel> activePeers = new ConcurrentHashMap<>();

  private Map<InetAddress, Date> recentlyDisconnected = Collections
      .synchronizedMap(new LRUMap<InetAddress, Date>(500));

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

      newPeers.sort(Comparator.comparingLong(c -> c.getStartTime()));

      for (Channel peer : newPeers) {
          if (!peer.isProtocolsInitialized()) {
              continue;
          }else if (peer.getNodeStatistics().isPenalized()) {
              disconnect(peer, peer.getNodeStatistics().getDisconnectReason());
          }else if (!peer.isActive() && activePeers.size() >= maxActivePeers) {
              disconnect(peer, TOO_MANY_PEERS);
          }else if (activePeers.containsKey(peer.getNodeIdWrapper())) {
              Channel channel = activePeers.get(peer.getNodeIdWrapper());
              if (channel.getStartTime() > peer.getStartTime()) {
                  logger.info("disconnect connection established later, {}", channel.getNode());
                  disconnect(channel, DUPLICATE_PEER);
              } else {
                  disconnect(peer, DUPLICATE_PEER);
              }
          }else {
              activePeers.put(peer.getNodeIdWrapper(), peer);
              newPeers.remove(peer);
              syncPool.onConnect(peer);
              logger.info("Add active peer {}, total active peers: {}", peer, activePeers.size());
          }
      }
  }

  public void disconnect(Channel peer, ReasonCode reason) {
    peer.disconnect(reason);
    InetSocketAddress socketAddress = (InetSocketAddress)peer.getChannelHandlerContext().channel().remoteAddress();
    recentlyDisconnected.put(socketAddress.getAddress(), new Date());
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
