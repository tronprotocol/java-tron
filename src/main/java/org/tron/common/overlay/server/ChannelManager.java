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
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections4.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.SystemProperties;
import org.tron.common.overlay.client.PeerClient;
import org.tron.common.overlay.discover.Node;
import org.tron.common.overlay.message.ReasonCode;
import org.tron.core.db.ByteArrayWrapper;

/**
 * @author Roman Mandeleil
 * @since 11.11.2014
 */
@Component
public class ChannelManager {

    private static final Logger logger = LoggerFactory.getLogger("net");

    // If the inbound peer connection was dropped by us with a reason message
    // then we ban that peer IP on any connections for some time to protect from
    // too active peers
    private static final int inboundConnectionBanTimeout = 10 * 1000;

    private List<Channel> newPeers = new CopyOnWriteArrayList<>();
    private final Map<ByteArrayWrapper, Channel> activePeers = new ConcurrentHashMap<>();

    private ScheduledExecutorService mainWorker = Executors.newSingleThreadScheduledExecutor();
    private int maxActivePeers;
    private Map<InetAddress, Date> recentlyDisconnected = Collections.synchronizedMap(new LRUMap<InetAddress, Date>(500));
    //private NodeFilter trustedPeers;

    /**
     * Queue with new blocks from other peers
     */
    //private BlockingQueue<BlockWrapper> newForeignBlocks = new LinkedBlockingQueue<>();

    /**
     * Queue with new peers used for after channel init tasks
     */
    private BlockingQueue<Channel> newActivePeers = new LinkedBlockingQueue<>();

    private SystemProperties config;

    private PeerServer peerServer;

    private PeerClient peerClient;

    @Autowired
    private ChannelManager(final SystemProperties config, final PeerClient peerClient, final PeerServer peerServer) {
        this.config = config;
        //this.syncManager = syncManager;
        this.peerClient = peerClient;
        this.peerServer = peerServer;
        maxActivePeers = config.maxActivePeers();
        //trustedPeers = config.peerTrusted();
        mainWorker.scheduleWithFixedDelay(() -> {
            try {
                processNewPeers();
            } catch (Throwable t) {
                logger.error("Error", t);
            }
        }, 0, 1, TimeUnit.SECONDS);

        if (config.listenPort() > 0) {
            new Thread(() -> peerServer.start(config.listenPort()),
            "PeerServerThread").start();
        }
    }

    public void connect(Node node) {
        if (logger.isTraceEnabled()) logger.trace(
                "Peer {}: initiate connection",
                node.getHexIdShort()
        );
        if (nodesInUse().contains(node.getHexId())) {
            if (logger.isTraceEnabled()) logger.trace(
                    "Peer {}: connection already initiated",
                    node.getHexIdShort()
            );
            return;
        }

        //todo ethereum.connect(node);
        peerClient.connectAsync(node.getHost(), node.getPort(), node.getHexId(), false);

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
        if (newPeers.isEmpty()) return;

        List<Channel> processed = new ArrayList<>();

        int addCnt = 0;
        for(Channel peer : newPeers) {

            logger.debug("Processing new peer: " + peer);

            //if(peer.isProtocolsInitialized()) {

                logger.debug("Protocols initialized");

                if (!activePeers.containsKey(peer.getNodeIdWrapper())) {
                    if (!peer.isActive() &&
                        activePeers.size() >= maxActivePeers //&&
                        //!trustedPeers.accept(peer.getNode())
                            ) {

                        // restricting inbound connections unless this is a trusted peer

                        disconnect(peer, TOO_MANY_PEERS);
                    } else {
                        process(peer);
                        addCnt++;
                    }
                } else {
                    disconnect(peer, DUPLICATE_PEER);
                }

                processed.add(peer);
            //}
        }

        if (addCnt > 0) {
            logger.info("New peers processed: " + processed + ", active peers added: " + addCnt + ", total active peers: " + activePeers.size());
        }

        newPeers.removeAll(processed);
    }

    private void disconnect(Channel peer, ReasonCode reason) {
        logger.debug("Disconnecting peer with reason " + reason + ": " + peer);
        peer.disconnect(reason);
        recentlyDisconnected.put(peer.getInetSocketAddress().getAddress(), new Date());
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

    private void process(Channel peer) {
//        if(peer.hasEthStatusSucceeded()) {
//            // prohibit transactions processing until main sync is done
//            if (syncManager.isSyncDone()) {
//                peer.onSyncDone(true);
//                // So we could perform some tasks on recently connected peer
//                newActivePeers.add(peer);
//            }
        newActivePeers.add(peer);
            activePeers.put(peer.getNodeIdWrapper(), peer);
//        }
    }


    public void add(Channel peer) {
        logger.debug("New peer in ChannelManager {}", peer);
        newPeers.add(peer);
    }

    public void notifyDisconnect(Channel channel) {
        logger.debug("Peer {}: notifies about disconnect", channel);
        channel.onDisconnect();
        //syncPool.onDisconnect(channel);
        activePeers.values().remove(channel);
        newPeers.remove(channel);
    }

    public void onSyncDone(boolean done) {
        for (Channel channel : activePeers.values())
            channel.onSyncDone(done);
    }

    public Collection<Channel> getActivePeers() {
        return new ArrayList<>(activePeers.values());
    }

    public Channel getActivePeer(byte[] nodeId) {
        return activePeers.get(new ByteArrayWrapper(nodeId));
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
