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

import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.overlay.discover.message.*;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The instance of this class responsible for discovery messages exchange with the specified Node
 * It also manages itself regarding inclusion/eviction from Kademlia table
 */
public class NodeHandler {
    static final org.slf4j.Logger logger = LoggerFactory.getLogger("NodeHandler");

    static long PingTimeout = 15000; //KademliaOptions.REQ_TIMEOUT;
    static final int WARN_PACKET_SIZE = 1400;
    private static volatile int msgInCount = 0, msgOutCount = 0;
    private static boolean initialLogging = true;

    // gradually reducing log level for dumping discover messages
    // they are not so informative when everything is already up and running
    // but could be interesting when discovery just starts
    private void logMessage(Message msg, boolean inbound) {
        //logger.info("handle msg type: {}, len {}, date: {}", msg.getType(), msg.getSendData().length,  msg);
        String s = String.format("%s[%s (%s)] %s", inbound ? " ===>  " : "<===  ", msg.getClass().getSimpleName(),
                msg.getData().length, this);
        if (msgInCount > 1024) {
            logger.trace(s);
        } else {
            logger.debug(s);
        }

        if (!inbound && msg.getData().length > WARN_PACKET_SIZE) {
            logger.warn("Sending UDP packet exceeding safe size of {} bytes, actual: {} bytes",
                    WARN_PACKET_SIZE, msg.getData().length);
            logger.warn(s);
        }

        if (initialLogging) {
            if (msgOutCount == 0) {
                logger.info("Pinging discovery nodes...");
            }
            if (msgInCount == 0 && inbound) {
                logger.info("Received response.");
            }
            if (inbound && msg instanceof NeighborsMessage) {
                logger.info("New peers discovered.");
                initialLogging = false;
            }
        }

        if (inbound) msgInCount++; else msgOutCount++;
    }

    public enum State {
        /**
         * The new node was just discovered either by receiving it with Neighbours
         * message or by receiving Ping from a new node
         * In either case we are sending Ping and waiting for Pong
         * If the Pong is received the node becomes {@link #Alive}
         * If the Pong was timed out the node becomes {@link #Dead}
         */
        Discovered,
        /**
         * The node didn't send the Pong message back withing acceptable timeout
         * This is the final state
         */
        Dead,
        /**
         * The node responded with Pong and is now the candidate for inclusion to the table
         * If the table has bucket space for this node it is added to table and becomes {@link #Active}
         * If the table bucket is full this node is challenging with the old node from the bucket
         *     if it wins then old node is dropped, and this node is added and becomes {@link #Active}
         *     else this node becomes {@link #NonActive}
         */
        Alive,
        /**
         * The node is included in the table. It may become {@link #EvictCandidate} if a new node
         * wants to become Active but the table bucket is full.
         */
        Active,
        /**
         * This node is in the table but is currently challenging with a new Node candidate
         * to survive in the table bucket
         * If it wins then returns back to {@link #Active} state, else is evicted from the table
         * and becomes {@link #NonActive}
         */
        EvictCandidate,
        /**
         * Veteran. It was Alive and even Active but is now retired due to loosing the challenge
         * with another Node.
         * For no this is the final state
         * It's an option for future to return veterans back to the table
         */
        NonActive
    }

    Node node;
    NodeManager nodeManager;
    private NodeStatistics nodeStatistics;

    State state;
    boolean waitForPong = false;
    long pingSent;
    int pingTrials = 3;
    NodeHandler replaceCandidate;

    public NodeHandler(Node node, NodeManager nodeManager) {
        this.node = node;
        this.nodeManager = nodeManager;
        changeState(State.Discovered);
    }

    public InetSocketAddress getInetSocketAddress() {
        return new InetSocketAddress(node.getHost(), node.getPort());
    }

    public Node getNode() {
        return node;
    }

    public State getState() {
        return state;
    }

    public NodeStatistics getNodeStatistics() {
        if (nodeStatistics == null) {
            nodeStatistics = new NodeStatistics(node);
        }
        return nodeStatistics;
    }

    private void challengeWith(NodeHandler replaceCandidate) {
        this.replaceCandidate = replaceCandidate;
        changeState(State.EvictCandidate);
    }

    // Manages state transfers
    public void changeState(State newState) {
        State oldState = state;
        if (newState == State.Discovered) {
            sendPing();
        }
        if (!node.isDiscoveryNode()) {
            if (newState == State.Alive) {
                Node evictCandidate = nodeManager.table.addNode(this.node);
                if (evictCandidate == null) {
                    newState = State.Active;
                } else {
                    NodeHandler evictHandler = nodeManager.getNodeHandler(evictCandidate);
                    if (evictHandler.state != State.EvictCandidate) {
                        evictHandler.challengeWith(this);
                    }
                }
            }
            if (newState == State.Active) {
                if (oldState == State.Alive) {
                    // new node won the challenge
                    nodeManager.table.addNode(node);
                } else if (oldState == State.EvictCandidate) {
                    // nothing to do here the node is already in the table
                } else {
                    // wrong state transition
                }
            }

            if (newState == State.NonActive) {
                if (oldState == State.EvictCandidate) {
                    // lost the challenge
                    // Removing ourselves from the table
                    nodeManager.table.dropNode(node);
                    // Congratulate the winner
                    replaceCandidate.changeState(State.Active);
                } else if (oldState == State.Alive) {
                    // ok the old node was better, nothing to do here
                } else {
                    // wrong state transition
                }
            }
        }

        if (newState == State.EvictCandidate) {
            // trying to survive, sending ping and waiting for pong
            sendPing();
        }
        state = newState;
    }

    void handlePing(PingMessage msg) {
        logMessage(msg, true);
        getNodeStatistics().discoverInPing.add();
        if (!nodeManager.table.getNode().equals(node)) {
            sendPong();
        }
    }

    void handlePong(PongMessage msg) {
        logMessage(msg, true);
        if (waitForPong) {
            waitForPong = false;
            getNodeStatistics().discoverInPong.add();
            getNodeStatistics().discoverMessageLatency.add(System.currentTimeMillis() - pingSent);
            getNodeStatistics().lastPongReplyTime.set(System.currentTimeMillis());
            changeState(State.Alive);
            node.setId(msg.getNodeId());
        }
    }

    void handleNeighbours(NeighborsMessage msg) {
        logMessage(msg, true);
        getNodeStatistics().discoverInNeighbours.add();
        for (Node n : msg.getNodes()) {
            if (!nodeManager.getPublicHomeNode().getHexId().equals(n.getHexId())){
                nodeManager.getNodeHandler(n);
            }
        }
    }

    void handleFindNode(FindNodeMessage msg) {
        logMessage(msg, true);
        getNodeStatistics().discoverInFind.add();
        List<Node> closest = nodeManager.table.getClosestNodes(msg.getTargetId());
        sendNeighbours(closest);
    }

    void handleTimedOut() {
        logger.info("ping timeout {} {} {}", node.getHost(),node.getPort(),node.getHexIdShort());
        waitForPong = false;
        if (--pingTrials > 0) {
            sendPing();
        } else {
            if (state == State.Discovered) {
                changeState(State.Dead);
            } else if (state == State.EvictCandidate) {
                changeState(State.NonActive);
            } else {
                // TODO just influence to reputation
            }
        }
    }

    void sendPing() {
        Message ping = new PingMessage(nodeManager.table.getNode(), getNode());
        logMessage(ping, false);
        waitForPong = true;
        pingSent = System.currentTimeMillis();
        sendMessage(ping);
        getNodeStatistics().discoverOutPing.add();

        if (nodeManager.getPongTimer().isShutdown()) return;
        nodeManager.getPongTimer().schedule(() -> {
            try {
                if (waitForPong) {
                    waitForPong = false;
                    handleTimedOut();
                }
            } catch (Throwable t) {
                logger.error("Unhandled exception", t);
            }
        }, PingTimeout, TimeUnit.MILLISECONDS);
    }

    void sendPong() {
        Message pong = new PongMessage(nodeManager.getPublicHomeNode());
        logMessage(pong, false);
        sendMessage(pong);
        getNodeStatistics().discoverOutPong.add();
    }

    void sendNeighbours(List<Node> neighbours) {
        Message neighbors = new NeighborsMessage(nodeManager.getPublicHomeNode(), neighbours);
        logMessage(neighbors, false);
        sendMessage(neighbors);
        getNodeStatistics().discoverOutNeighbours.add();
    }

    void sendFindNode(byte[] target) {
        Message findNode = new FindNodeMessage(node, target);
        logMessage(findNode, false);
        sendMessage(findNode);
        getNodeStatistics().discoverOutFind.add();
    }

    private void sendMessage(Message msg) {
        nodeManager.sendOutbound(new DiscoveryEvent(msg, getInetSocketAddress()));
    }

    @Override
    public String toString() {
        return "NodeHandler[state: " + state + ", node: " + node.getHost() + ":" + node.getPort() + ", id="
                + (node.getId().length > 0 ? Hex.toHexString(node.getId(), 0, 4) : "empty") + "]";
    }


}
