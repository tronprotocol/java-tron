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
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.net.udp.handler.UdpEvent;
import org.tron.common.net.udp.message.Message;
import org.tron.common.net.udp.message.discover.FindNodeMessage;
import org.tron.common.net.udp.message.discover.NeighborsMessage;
import org.tron.common.net.udp.message.discover.PingMessage;
import org.tron.common.net.udp.message.discover.PongMessage;
import org.tron.common.overlay.discover.node.statistics.NodeStatistics;
import org.tron.core.config.args.Args;

@Slf4j(topic = "discover")
public class NodeHandler {

  private static long pingTimeout = Args.getInstance().getNodeDiscoveryPingTimeout();
  private Node sourceNode;
  private Node node;
  private State state;
  private NodeManager nodeManager;
  private NodeStatistics nodeStatistics;
  private NodeHandler replaceCandidate;
  private InetSocketAddress inetSocketAddress;
  private AtomicInteger pingTrials = new AtomicInteger(3);
  private volatile boolean waitForPong = false;
  private volatile boolean waitForNeighbors = false;
  private volatile long pingSent;

  public NodeHandler(Node node, NodeManager nodeManager) {
    this.node = node;
    this.nodeManager = nodeManager;
    this.inetSocketAddress = new InetSocketAddress(node.getHost(), node.getPort());
    this.nodeStatistics = new NodeStatistics();
    changeState(State.DISCOVERED);
  }

  public InetSocketAddress getInetSocketAddress() {
    return inetSocketAddress;
  }

  public Node getSourceNode() {
    return sourceNode;
  }

  public void setSourceNode(Node sourceNode) {
    this.sourceNode = sourceNode;
  }

  public Node getNode() {
    return node;
  }

  public void setNode(Node node) {
    this.node = node;
  }

  public State getState() {
    return state;
  }

  public NodeStatistics getNodeStatistics() {
    return nodeStatistics;
  }

  private void challengeWith(NodeHandler replaceCandidate) {
    this.replaceCandidate = replaceCandidate;
    changeState(State.EVICTCANDIDATE);
  }

  // Manages state transfers
  public void changeState(State newState) {
    State oldState = state;
    if (newState == State.DISCOVERED) {
      if (sourceNode != null && sourceNode.getPort() != node.getPort()) {
        changeState(State.DEAD);
      } else {
        sendPing();
      }
    }
    if (!node.isDiscoveryNode()) {
      if (newState == State.ALIVE) {
        Node evictCandidate = nodeManager.getTable().addNode(this.node);
        if (evictCandidate == null) {
          newState = State.ACTIVE;
        } else {
          NodeHandler evictHandler = nodeManager.getNodeHandler(evictCandidate);
          if (evictHandler.state != State.EVICTCANDIDATE) {
            evictHandler.challengeWith(this);
          }
        }
      }
      if (newState == State.ACTIVE) {
        if (oldState == State.ALIVE) {
          // new node won the challenge
          nodeManager.getTable().addNode(node);
        } else if (oldState == State.EVICTCANDIDATE) {
          // nothing to do here the node is already in the table
        } else {
          // wrong state transition
        }
      }

      if (newState == State.NONACTIVE) {
        if (oldState == State.EVICTCANDIDATE) {
          // lost the challenge
          // Removing ourselves from the table
          nodeManager.getTable().dropNode(node);
          // Congratulate the winner
          replaceCandidate.changeState(State.ACTIVE);
        } else if (oldState == State.ALIVE) {
          // ok the old node was better, nothing to do here
        } else {
          // wrong state transition
        }
      }
    }

    if (newState == State.EVICTCANDIDATE) {
      // trying to survive, sending ping and waiting for pong
      sendPing();
    }
    state = newState;
  }

  public void handlePing(PingMessage msg) {
    if (!nodeManager.getTable().getNode().equals(node)) {
      sendPong();
    }
    node.setP2pVersion(msg.getVersion());
    if (!node.isConnectible(Args.getInstance().getNodeP2pVersion())) {
      changeState(State.NONACTIVE);
    } else if (state.equals(State.NONACTIVE) || state.equals(State.DEAD)) {
      changeState(State.DISCOVERED);
    }
  }

  public void handlePong(PongMessage msg) {
    if (waitForPong) {
      waitForPong = false;
      getNodeStatistics().discoverMessageLatency.add(System.currentTimeMillis() - pingSent);
      getNodeStatistics().lastPongReplyTime.set(System.currentTimeMillis());
      node.setId(msg.getFrom().getId());
      node.setP2pVersion(msg.getVersion());
      if (!node.isConnectible(Args.getInstance().getNodeP2pVersion())) {
        changeState(State.NONACTIVE);
      } else {
        changeState(State.ALIVE);
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
    sendNeighbours(closest, msg.getTimestamp());
  }

  public void handleTimedOut() {
    waitForPong = false;
    if (pingTrials.getAndDecrement() > 0) {
      sendPing();
    } else {
      if (state == State.DISCOVERED) {
        changeState(State.DEAD);
      } else if (state == State.EVICTCANDIDATE) {
        changeState(State.NONACTIVE);
      } else {
        // TODO just influence to reputation
      }
    }
  }

  public void sendPing() {
    PingMessage msg = new PingMessage(nodeManager.getPublicHomeNode(), getNode());
    waitForPong = true;
    pingSent = System.currentTimeMillis();
    sendMessage(msg);

    if (nodeManager.getPongTimer().isShutdown()) {
      return;
    }
    nodeManager.getPongTimer().schedule(() -> {
      try {
        if (waitForPong) {
          waitForPong = false;
          handleTimedOut();
        }
      } catch (Exception e) {
        logger.error("Unhandled exception", e);
      }
    }, pingTimeout, TimeUnit.MILLISECONDS);
  }

  public void sendPong() {
    Message pong = new PongMessage(nodeManager.getPublicHomeNode());
    sendMessage(pong);
  }

  public void sendFindNode(byte[] target) {
    waitForNeighbors = true;
    FindNodeMessage msg = new FindNodeMessage(nodeManager.getPublicHomeNode(), target);
    sendMessage(msg);
  }

  public void sendNeighbours(List<Node> neighbours, long sequence) {
    Message msg = new NeighborsMessage(nodeManager.getPublicHomeNode(), neighbours, sequence);
    sendMessage(msg);
  }

  private void sendMessage(Message msg) {
    nodeManager.sendOutbound(new UdpEvent(msg, getInetSocketAddress()));
    nodeStatistics.messageStatistics.addUdpOutMessage(msg.getType());
  }

  @Override
  public String toString() {
    return "NodeHandler[state: " + state + ", node: " + node.getHost() + ":" + node.getPort() + "]";
  }

  public enum State {
    /**
     * The new node was just discovered either by receiving it with Neighbours message or by
     * receiving Ping from a new node In either case we are sending Ping and waiting for Pong If the
     * Pong is received the node becomes {@link #ALIVE} If the Pong was timed out the node becomes
     * {@link #DEAD}
     */
    DISCOVERED,
    /**
     * The node didn't send the Pong message back withing acceptable timeout This is the final
     * state
     */
    DEAD,
    /**
     * The node responded with Pong and is now the candidate for inclusion to the table If the table
     * has bucket space for this node it is added to table and becomes {@link #ACTIVE} If the table
     * bucket is full this node is challenging with the old node from the bucket if it wins then old
     * node is dropped, and this node is added and becomes {@link #ACTIVE} else this node becomes
     * {@link #NONACTIVE}
     */
    ALIVE,
    /**
     * The node is included in the table. It may become {@link #EVICTCANDIDATE} if a new node wants
     * to become Active but the table bucket is full.
     */
    ACTIVE,
    /**
     * This node is in the table but is currently challenging with a new Node candidate to survive
     * in the table bucket If it wins then returns back to {@link #ACTIVE} state, else is evicted
     * from the table and becomes {@link #NONACTIVE}
     */
    EVICTCANDIDATE,
    /**
     * Veteran. It was Alive and even Active but is now retired due to loosing the challenge with
     * another Node. For no this is the final state It's an option for future to return veterans
     * back to the table
     */
    NONACTIVE
  }

}
