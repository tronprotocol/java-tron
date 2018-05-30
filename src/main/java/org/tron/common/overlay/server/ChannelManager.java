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
import static org.tron.protos.Protocol.ReasonCode.UNKNOWN;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections4.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.client.PeerClient;
import org.tron.common.overlay.discover.Node;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.config.args.Args;
import org.tron.core.db.ByteArrayWrapper;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.TransactionMessage;
import org.tron.protos.Protocol.ReasonCode;


@Component
public class ChannelManager {

  private static final Logger logger = LoggerFactory.getLogger("ChannelManager");

  private static final int inboundConnectionBanTimeout = 30 * 1000;

  private final Map<ByteArrayWrapper, Channel> activePeers = new ConcurrentHashMap<>();

  private Cache<InetAddress, ReasonCode> badPeers = CacheBuilder.newBuilder().maximumSize(10000)
      .expireAfterWrite(1, TimeUnit.HOURS).recordStats().build();

  private Cache<InetAddress, ReasonCode> recentlyDisconnected = CacheBuilder.newBuilder().maximumSize(1000)
      .expireAfterWrite(30, TimeUnit.SECONDS).recordStats().build();

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

  public void processDisconnect(Channel channel, ReasonCode reason){
    InetAddress inetAddress = channel.getInetAddress();
    if (inetAddress == null){
      return;
    }
    switch (reason){
      case FORKED:
      case BAD_PROTOCOL:
      case BAD_BLOCK:
      case INCOMPATIBLE_CHAIN:
      case INCOMPATIBLE_PROTOCOL:
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
      if (inetAddress != null && recentlyDisconnected.getIfPresent(inetAddress) != null){
        recentlyDisconnected.put(channel.getInetAddress(), UNKNOWN);
      }
    }
  }

  public synchronized boolean processPeer(Channel peer) {

    if (recentlyDisconnected.getIfPresent(peer) != null){
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

  public Collection<Channel> getActivePeers() {
    return activePeers.values();
  }

  public Cache<InetAddress, ReasonCode> getRecentlyDisconnected(){
    return this.recentlyDisconnected;
  }

  public Cache<InetAddress, ReasonCode> getBadPeers(){
    return this.badPeers;
  }

  public void close() {
    peerServer.close();
    peerClient.close();
  }
}
