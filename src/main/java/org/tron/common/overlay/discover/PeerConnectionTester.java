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
package org.tron.common.overlay.discover;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.config.SystemProperties;
import org.tron.common.overlay.client.PeerClient;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Makes test RLPx connection to the peers to acquire statistics
 *
 * Created by Anton Nashatyrev on 17.07.2015.
 */
@Component
public class PeerConnectionTester {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("discover");

    private int ConnectThreads;
    private long ReconnectPeriod;
    private long ReconnectMaxPeers;

    @Autowired
    private PeerClient peerClient;

    private SystemProperties config = SystemProperties.getDefault();

    // NodeHandler instance should be unique per Node instance
    private Map<NodeHandler, ?> connectedCandidates = Collections.synchronizedMap(new IdentityHashMap());

    // executor with Queue which picks up the Node with the best reputation
    private ExecutorService peerConnectionPool;

    private Timer reconnectTimer = new Timer("DiscoveryReconnectTimer");
    private int reconnectPeersCount = 0;

    private class ConnectTask implements Runnable {
        NodeHandler nodeHandler;

        public ConnectTask(NodeHandler nodeHandler) {
            this.nodeHandler = nodeHandler;
        }

        @Override
        public void run() {
            try {
                if (nodeHandler != null) {
                    nodeHandler.getNodeStatistics().rlpxConnectionAttempts.add();
                    logger.debug("Trying node connection: " + nodeHandler);
                    Node node = nodeHandler.getNode();
                    peerClient.connect(node.getHost(), node.getPort(),
                            Hex.encodeHexString(node.getId()), true);
                    logger.debug("Terminated node connection: " + nodeHandler);
                    nodeHandler.getNodeStatistics().disconnected();
                    if (!nodeHandler.getNodeStatistics().getEthTotalDifficulty().equals(BigInteger.ZERO) &&
                            ReconnectPeriod > 0 && (reconnectPeersCount < ReconnectMaxPeers || ReconnectMaxPeers == -1)) {
                        // trying to keep good peers information up-to-date
                        reconnectPeersCount++;
                        reconnectTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                logger.debug("Trying the node again: " + nodeHandler);
                                peerConnectionPool.execute(new ConnectTask(nodeHandler));
                                reconnectPeersCount--;
                            }
                        }, ReconnectPeriod);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                connectedCandidates.remove(nodeHandler);
            }
        }
    }

    @Autowired
    public PeerConnectionTester(final SystemProperties config) {
//        this.config = config;
//        ConnectThreads = config.peerDiscoveryWorkers();
//        ReconnectPeriod = config.peerDiscoveryTouchPeriod() * 1000;
//        ReconnectMaxPeers = config.peerDiscoveryTouchMaxNodes();
//        peerConnectionPool = new ThreadPoolExecutor(ConnectThreads,
//                ConnectThreads, 0L, TimeUnit.SECONDS,
//                new MutablePriorityQueue<>((Comparator<ConnectTask>) (h1, h2) ->
//                        h2.nodeHandler.getNodeStatistics().getReputation() -
//                                h1.nodeHandler.getNodeStatistics().getReputation()),
//                new ThreadFactoryBuilder().setDaemon(true).setNameFormat("discovery-tester-%d").build());
    }

    public void close() {
        logger.info("Closing PeerConnectionTester...");
        try {
            peerConnectionPool.shutdownNow();
        } catch (Exception e) {
            logger.warn("Problems closing PeerConnectionTester", e);
        }
        try {
            reconnectTimer.cancel();
        } catch (Exception e) {
            logger.warn("Problems cancelling reconnectTimer", e);
        }
    }

    public void nodeStatusChanged(final NodeHandler nodeHandler) {
        if (peerConnectionPool.isShutdown()) return;
        if (connectedCandidates.size() < NodeManager.MAX_NODES
                && !connectedCandidates.containsKey(nodeHandler)
                && !nodeHandler.getNode().isDiscoveryNode()) {
            logger.debug("Submitting node for RLPx connection : " + nodeHandler);
            connectedCandidates.put(nodeHandler, null);
            peerConnectionPool.execute(new ConnectTask(nodeHandler));
        }
    }

    /**
     * The same as PriorityBlockQueue but with assumption that elements are mutable
     * and priority changes after enqueueing, thus the list is sorted by priority
     * each time the head queue element is requested.
     * The class has poor synchronization since the prioritization might be approximate
     * though the implementation should be inheritedly thread-safe
     */
    public static class MutablePriorityQueue<T, C extends T> extends LinkedBlockingQueue<T> {
        Comparator<C> comparator;

        public MutablePriorityQueue(Comparator<C> comparator) {
            this.comparator = comparator;
        }

        @Override
        public synchronized T take() throws InterruptedException {
            if (isEmpty()) {
                return super.take();
            } else {
                T ret = Collections.min(this, (Comparator<? super T>) comparator);
                remove(ret);
                return ret;
            }
        }

        @Override
        public synchronized T poll(long timeout, TimeUnit unit) throws InterruptedException {
            if (isEmpty()) {
                return super.poll(timeout, unit);
            } else {
                T ret = Collections.min(this, (Comparator<? super T>) comparator);
                remove(ret);
                return ret;
            }
        }

        @Override
        public synchronized T poll() {
            if (isEmpty()) {
                return super.poll();
            } else {
                T ret = Collections.min(this, (Comparator<? super T>) comparator);
                remove(ret);
                return ret;
            }
        }

        @Override
        public synchronized T peek() {
            if (isEmpty()) {
                return super.peek();
            } else {
                T ret = Collections.min(this, (Comparator<? super T>) comparator);
                return ret;
            }
        }
    }

}
