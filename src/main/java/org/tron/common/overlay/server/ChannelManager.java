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

import static org.tron.protos.Protocol.ReasonCode.DUPLICATE_PEER;
import static org.tron.protos.Protocol.ReasonCode.TOO_MANY_PEERS;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.collections4.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.client.PeerClient;
import org.tron.core.config.args.Args;
import org.tron.core.db.ByteArrayWrapper;
import org.tron.protos.Protocol.ReasonCode;


@Component
public class ChannelManager {

  private static final Logger logger = LoggerFactory.getLogger("ChannelManager");

  private static final int inboundConnectionBanTimeout = 30 * 1000;

  private final Map<ByteArrayWrapper, Channel> activePeers = new ConcurrentHashMap<>();

  private Map<InetAddress, Date> recentlyDisconnected = Collections
      .synchronizedMap(new LRUMap<InetAddress, Date>(500));

  private Args args = Args.getInstance();

  private int maxActivePeers = args.getNodeMaxActiveNodes() > 0 ? args.getNodeMaxActiveNodes() : 30;

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
  }

  public Set<String> nodesInUse() {
    Set<String> ids = new HashSet<>();
    for (Channel peer : getActivePeers()) {
      ids.add(peer.getPeerId());
    }
    return ids;
  }

  public void disconnect(Channel peer, ReasonCode reason) {
    peer.disconnect(reason);
    InetSocketAddress socketAddress = (InetSocketAddress) peer.getChannelHandlerContext().channel()
        .remoteAddress();
    recentlyDisconnected.put(socketAddress.getAddress(), new Date());
  }

  public void notifyDisconnect(Channel channel) {
    syncPool.onDisconnect(channel);
    activePeers.values().remove(channel);
    if (channel == null || channel.getChannelHandlerContext() == null
        || channel.getChannelHandlerContext().channel() == null) {
      return;
    }
    if (channel.getNodeStatistics() != null) {
      channel.getNodeStatistics().notifyDisconnect();
    }
    InetSocketAddress socketAddress = (InetSocketAddress) channel.getChannelHandlerContext()
        .channel().remoteAddress();
    recentlyDisconnected.put(socketAddress.getAddress(), new Date());
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

  public synchronized boolean procPeer(Channel peer) {
    if (peer.getNodeStatistics().isPenalized()) {
      disconnect(peer, peer.getNodeStatistics().getDisconnectReason());
      return false;
    }

    if (!peer.isActive() && activePeers.size() >= maxActivePeers) {
      disconnect(peer, TOO_MANY_PEERS);
      return false;
    }

    if (activePeers.containsKey(peer.getNodeIdWrapper())) {
      Channel channel = activePeers.get(peer.getNodeIdWrapper());
      if (channel.getStartTime() > peer.getStartTime()) {
        logger.info("Disconnect connection established later, {}", channel.getNode());
        disconnect(channel, DUPLICATE_PEER);
      } else {
        disconnect(peer, DUPLICATE_PEER);
        return false;
      }
    }
    activePeers.put(peer.getNodeIdWrapper(), peer);
    logger.info("Add active peer {}, total active peers: {}", peer, activePeers.size());
    return true;
  }

  public Collection<Channel> getActivePeers() {
    return new ArrayList<>(activePeers.values());
  }

  public void close() {
    peerServer.close();
    peerClient.close();
  }
}
