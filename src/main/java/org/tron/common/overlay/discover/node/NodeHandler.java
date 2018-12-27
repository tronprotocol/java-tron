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

package org.tron.common.overlay.discover.node;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.net.udp.handler.UdpEvent;
import org.tron.common.net.udp.message.Message;
import org.tron.common.net.udp.message.discover.FindNodeMessage;
import org.tron.common.net.udp.message.discover.NeighborsMessage;
import org.tron.common.net.udp.message.discover.PingMessage;
import org.tron.common.net.udp.message.discover.PongMessage;
import org.tron.common.overlay.discover.node.statistics.NodeStatistics;
import org.tron.core.config.args.Args;

/**
 * The instance of this class responsible for discovery messages exchange with the specified Node It
 * also manages itself regarding inclusion/eviction from Kademlia table
 **/
public class NodeHandler {

  private static final Logger logger = LoggerFactory.getLogger("NodeHandler");

  private static long PingTimeout = 15000;

  public enum State {
    /**
     * The new node was just discovered either by receiving it with Neighbours message or by
     * receiving Ping from a new node In either case we are sending Ping and waiting for Pong If the
     * Pong is received the node becomes {@link #Alive} If the Pong was timed out the node becomes
     * {@link #Dead}
     */
    Discovered,
    /**
     * The node didn't send the Pong message back withing acceptable timeout This is the final
     * state
     */
    Dead,
    /**
     * The node responded with Pong and is now the candidate for inclusion to the table If the table
     * has bucket space for this node it is added to table and becomes {@link #Active} If the table
     * bucket is full this node is challenging with the old node from the bucket if it wins then old
     * node is dropped, and this node is added and becomes {@link #Active} else this node becomes
     * {@link #NonActive}
     */
    Alive,
    /**
     * The node is included in the table. It may become {@link #EvictCandidate} if a new node wants
     * to become Active but the table bucket is full.
     */
    Active,
    /**
     * This node is in the table but is currently challenging with a new Node candidate to survive
     * in the table bucket If it wins then returns back to {@link #Active} state, else is evicted
     * from the table and becomes {@link #NonActive}
     */
    EvictCandidate,
    /**
     * Veteran. It was Alive and even Active but is now retired due to loosing the challenge with
     * another Node. For no this is the final state It's an option for future to return veterans
     * back to the table
     */
    NonActive
  }

  private Node sourceNode;
  private Node node;
  private State state;
  private NodeManager nodeManager;
  private NodeStatistics nodeStatistics;
  private NodeHandler replaceCandidate;
  private InetSocketAddress inetSocketAddress;
  private volatile boolean waitForPong = false;
  private volatile boolean waitForNeighbors = false;
  private volatile int pingTrials = 3;
  private long pingSent;

  public NodeHandler(Node node, NodeManager nodeManager) {
    this.node = node;
    this.nodeManager = nodeManager;
    this.inetSocketAddress = new InetSocketAddress(node.getHost(), node.getPort());
    this.nodeStatistics = new NodeStatistics(node);
    changeState(State.Discovered);
  }

  public InetSocketAddress getInetSocketAddress() {
    return inetSocketAddress;
  }

  public void setSourceNode(Node sourceNode) {
    this.sourceNode = sourceNode;
  }

  public Node getSourceNode() {
    return sourceNode;
  }

  public Node getNode() {
    return node;
  }

  public State getState() {
    return state;
  }

  public void setNode(Node node) {
    this.node = node;
  }

  public NodeStatistics getNodeStatistics() {
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
      if (sourceNode != null && sourceNode.getPort() != node.getPort()) {
        changeState(State.Dead);
      } else {
        sendPing();
      }
    }
    if (!node.isDiscoveryNode()) {
      if (newState == State.Alive) {
        Node evictCandidate = nodeManager.getTable().addNode(this.node);
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
          nodeManager.getTable().addNode(node);
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
          nodeManager.getTable().dropNode(node);
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

  public void handlePing(PingMessage msg) {
    if (!nodeManager.getTable().getNode().equals(node)) {
      sendPong();
    }
    if (msg.getVersion() != Args.getInstance().getNodeP2pVersion()) {
      changeState(State.NonActive);
    } else if (state.equals(State.NonActive) || state.equals(State.Dead)) {
      changeState(State.Discovered);
    }
  }

  public void handlePong(PongMessage msg) {
    if (waitForPong) {
      waitForPong = false;
      getNodeStatistics().discoverMessageLatency
          .add((double) System.currentTimeMillis() - pingSent);
      getNodeStatistics().lastPongReplyTime.set(System.currentTimeMillis());
      node.setId(msg.getFrom().getId());
      if (msg.getVersion() != Args.getInstance().getNodeP2pVersion()) {
        changeState(State.NonActive);
      } else {
        changeState(State.Alive);
      }
    }
  }

  public void handleNeighbours(NeighborsMessage msg) {
    if (!waitForNeighbors) {
      logger.warn("Receive neighbors from {} without send find nodes.", node.getHost());
      return;
    }
    waitForNeighbors = false;
    for (Node n : msg.getNodes()) {
      if (!nodeManager.getPublicHomeNode().getHexId().equals(n.getHexId())) {
        nodeManager.getNodeHandler(n);
      }
    }
  }

  public void handleFindNode(FindNodeMessage msg) {
    List<Node> closest = nodeManager.getTable().getClosestNodes(msg.getTargetId());
    sendNeighbours(closest);
  }

  public void handleTimedOut() {
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

  public void sendPing() {
    Message ping = new PingMessage(nodeManager.getPublicHomeNode(), getNode());
    waitForPong = true;
    pingSent = System.currentTimeMillis();
    sendMessage(ping);

    if (nodeManager.getPongTimer().isShutdown()) {
      return;
    }
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

  public void sendPong() {
    Message pong = new PongMessage(nodeManager.getPublicHomeNode());
    sendMessage(pong);
  }

  public void sendNeighbours(List<Node> neighbours) {
    Message neighbors = new NeighborsMessage(nodeManager.getPublicHomeNode(), neighbours);
    sendMessage(neighbors);
  }

  public void sendFindNode(byte[] target) {
    waitForNeighbors = true;
    Message findNode = new FindNodeMessage(nodeManager.getPublicHomeNode(), target);
    sendMessage(findNode);
  }

  private void sendMessage(Message msg) {
    nodeManager.sendOutbound(new UdpEvent(msg, getInetSocketAddress()));
    nodeStatistics.messageStatistics.addUdpOutMessage(msg.getType());
  }

  @Override
  public String toString() {
    return "NodeHandler[state: " + state + ", node: " + node.getHost() + ":" + node.getPort() + "]";
  }

}
