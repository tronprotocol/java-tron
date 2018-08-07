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

package org.tron.common.overlay.discover.node.statistics;

import static java.lang.Math.min;

import java.util.concurrent.atomic.AtomicLong;
import org.tron.common.overlay.discover.node.Node;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol.ReasonCode;

public class NodeStatistics {

  public final static int REPUTATION_PREDEFINED = 100000;
  public final static long TOO_MANY_PEERS_PENALIZE_TIMEOUT = 60 * 1000L;
  private static final long CLEAR_CYCLE_TIME = 60 * 60 * 1000L;
  private final long MIN_DATA_LENGTH = Args.getInstance().getReceiveTcpMinDataLength();

  private boolean isPredefined = false;
  private int persistedReputation = 0;
  private int disconnectTimes = 0;
  private ReasonCode tronLastRemoteDisconnectReason = null;
  private ReasonCode tronLastLocalDisconnectReason = null;
  private long lastDisconnectedTime = 0;
  private long firstDisconnectedTime = 0;

  public final MessageStatistics messageStatistics = new MessageStatistics();
  public final MessageCount p2pHandShake = new MessageCount();
  public final MessageCount tcpFlow = new MessageCount();

  public final SimpleStatter discoverMessageLatency;
  public final AtomicLong lastPongReplyTime = new AtomicLong(0l); // in milliseconds



  public NodeStatistics(Node node) {
    discoverMessageLatency = new SimpleStatter(node.getIdString());
  }

  private int getSessionFairReputation() {
    int discoverReput = 0;

    discoverReput +=
        min(messageStatistics.discoverInPong.getTotalCount(), 1) * (
            messageStatistics.discoverOutPing.getTotalCount() == messageStatistics.discoverInPong.getTotalCount() ? 50 : 1);

    discoverReput +=
        min(messageStatistics.discoverInNeighbours.getTotalCount(), 1) * (
            messageStatistics.discoverOutFindNode.getTotalCount() == messageStatistics.discoverInNeighbours.getTotalCount() ? 50 : 1);

    discoverReput += (int)discoverMessageLatency.getAvrg() == 0 ? 0 : 1000 / discoverMessageLatency.getAvrg();

    int reput = 0;
    reput += p2pHandShake.getTotalCount() > 0 ? 20 : 0;
    reput += min(messageStatistics.tronInMessage.getTotalCount(), 10) * 3;

    if (wasDisconnected()) {
      if (tronLastLocalDisconnectReason == null && tronLastRemoteDisconnectReason == null) {
        // means connection was dropped without reporting any reason - bad
        reput *= 0.3;
      } else if (tronLastLocalDisconnectReason != ReasonCode.REQUESTED) {
        // the disconnect was not initiated by discover mode
        if (tronLastRemoteDisconnectReason == ReasonCode.TOO_MANY_PEERS) {
          // The peer is popular, but we were unlucky
          reput *= 0.3;
        } else if (tronLastRemoteDisconnectReason != ReasonCode.REQUESTED) {
          // other disconnect reasons
          reput *= 0.2;
        }
      }
    }
    if (disconnectTimes > 20) {
      return 0;
    }
    int score =
        discoverReput + 10 * reput - (int) Math.pow(2, disconnectTimes) * (disconnectTimes > 0 ? 10
            : 0);
    return score > 0 ? score : 0;
  }

  public int getReputation() {
    int score = 0;
    if (!isReputationPenalized()){
      score += persistedReputation / 2 + getSessionFairReputation();
    }
    if (isPredefined){
      score += REPUTATION_PREDEFINED;
    }
    return score;
  }

  public ReasonCode getDisconnectReason() {
    if (tronLastLocalDisconnectReason != null) {
      return tronLastLocalDisconnectReason;
    }
    if (tronLastRemoteDisconnectReason != null) {
      return tronLastRemoteDisconnectReason;
    }
    return ReasonCode.UNKNOWN;
  }

  public boolean isReputationPenalized() {

    if (wasDisconnected() && tronLastRemoteDisconnectReason == ReasonCode.TOO_MANY_PEERS &&
        System.currentTimeMillis() - lastDisconnectedTime < TOO_MANY_PEERS_PENALIZE_TIMEOUT) {
      return true;
    }

    if (wasDisconnected() && tronLastRemoteDisconnectReason == ReasonCode.DUPLICATE_PEER &&
        System.currentTimeMillis() - lastDisconnectedTime < TOO_MANY_PEERS_PENALIZE_TIMEOUT) {
      return true;
    }

    if (firstDisconnectedTime > 0
        && (System.currentTimeMillis() - firstDisconnectedTime) > CLEAR_CYCLE_TIME) {
      tronLastLocalDisconnectReason = null;
      tronLastRemoteDisconnectReason = null;
      disconnectTimes = 0;
      persistedReputation = 0;
      firstDisconnectedTime = 0;
    }

    if (tronLastLocalDisconnectReason == ReasonCode.INCOMPATIBLE_PROTOCOL ||
        tronLastRemoteDisconnectReason == ReasonCode.INCOMPATIBLE_PROTOCOL ||
        tronLastLocalDisconnectReason == ReasonCode.BAD_PROTOCOL ||
        tronLastRemoteDisconnectReason == ReasonCode.BAD_PROTOCOL ||
        tronLastLocalDisconnectReason == ReasonCode.BAD_BLOCK ||
        tronLastRemoteDisconnectReason == ReasonCode.BAD_BLOCK ||
        tronLastLocalDisconnectReason == ReasonCode.BAD_TX ||
        tronLastRemoteDisconnectReason == ReasonCode.BAD_TX ||
        tronLastLocalDisconnectReason == ReasonCode.FORKED ||
        tronLastRemoteDisconnectReason == ReasonCode.FORKED ||
        tronLastLocalDisconnectReason == ReasonCode.UNLINKABLE ||
        tronLastRemoteDisconnectReason == ReasonCode.UNLINKABLE ||
        tronLastLocalDisconnectReason == ReasonCode.INCOMPATIBLE_CHAIN ||
        tronLastRemoteDisconnectReason == ReasonCode.INCOMPATIBLE_CHAIN ||
        tronLastRemoteDisconnectReason == ReasonCode.SYNC_FAIL ||
        tronLastLocalDisconnectReason == ReasonCode.SYNC_FAIL ||
        tronLastRemoteDisconnectReason == ReasonCode.INCOMPATIBLE_VERSION ||
        tronLastLocalDisconnectReason == ReasonCode.INCOMPATIBLE_VERSION) {
      persistedReputation = 0;
      return true;
    }
    return false;
  }

  public void nodeDisconnectedRemote(ReasonCode reason) {
    lastDisconnectedTime = System.currentTimeMillis();
    tronLastRemoteDisconnectReason = reason;
  }

  public void nodeDisconnectedLocal(ReasonCode reason) {
    lastDisconnectedTime = System.currentTimeMillis();
    tronLastLocalDisconnectReason = reason;
  }

  public void notifyDisconnect() {
    lastDisconnectedTime = System.currentTimeMillis();
    if (firstDisconnectedTime <= 0) {
      firstDisconnectedTime = lastDisconnectedTime;
    }
    disconnectTimes++;
    persistedReputation = persistedReputation / 2;
  }

  public boolean wasDisconnected() {
    return lastDisconnectedTime > 0;
  }

  public void setPredefined(boolean isPredefined) {
    this.isPredefined = isPredefined;
  }

  public boolean isPredefined() {
    return isPredefined;
  }

  public void setPersistedReputation(int persistedReputation) {
    this.persistedReputation = persistedReputation;
  }

  @Override
  public String toString() {
    return "NodeStat[reput: " + getReputation() + "(" + persistedReputation + "), discover: " +
        messageStatistics.discoverInPong + "/" + messageStatistics.discoverOutPing + " " +
        messageStatistics.discoverOutPong + "/" + messageStatistics.discoverInPing + " " +
        messageStatistics.discoverInNeighbours + "/" + messageStatistics.discoverOutFindNode + " " +
        messageStatistics.discoverOutNeighbours + "/" + messageStatistics.discoverInFindNode + " " +
        ((int) discoverMessageLatency.getAvrg()) + "ms" +
        ", p2p: " + p2pHandShake + "/" + messageStatistics.p2pInHello + "/" + messageStatistics.p2pOutHello + " " +
        ", tron: " + messageStatistics.tronInMessage + "/" + messageStatistics.tronOutMessage + " " +
        (wasDisconnected() ? "X " + disconnectTimes : "") +
        (tronLastLocalDisconnectReason != null ? ("<=" + tronLastLocalDisconnectReason) : " ") +
        (tronLastRemoteDisconnectReason != null ? ("=>" + tronLastRemoteDisconnectReason) : " ") +
        ", tcp flow: " + tcpFlow.getTotalCount();
  }

  public class SimpleStatter {

    private final String name;
    private volatile double last;
    private volatile double sum;
    private volatile int count;

    public SimpleStatter(String name) {
      this.name = name;
    }

    public void add(double value) {
      last = value;
      sum += value;
      count++;
    }

    public double getLast() {
      return last;
    }

    public int getCount() {
      return count;
    }

    public double getSum() {
      return sum;
    }

    public double getAvrg() {
      return count == 0 ? 0 : sum / count;
    }

    public String getName() {
      return name;
    }

  }

  public boolean nodeIsHaveDataTransfer() {
    return tcpFlow.getTotalCount() > MIN_DATA_LENGTH;
  }

  public void resetTcpFlow() {
    tcpFlow.reset();
  }

}
