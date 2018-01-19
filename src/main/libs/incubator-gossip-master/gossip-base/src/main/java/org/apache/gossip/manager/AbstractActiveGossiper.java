/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gossip.manager;

import java.util.Map.Entry;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import org.apache.gossip.GossipSettings;
import org.apache.gossip.LocalMember;
import org.apache.gossip.model.ActiveGossipOk;
import org.apache.gossip.model.PerNodeDataMessage;
import org.apache.gossip.model.Member;
import org.apache.gossip.model.Response;
import org.apache.gossip.model.SharedDataMessage;
import org.apache.gossip.model.ShutdownMessage;
import org.apache.gossip.udp.*;
import org.apache.log4j.Logger;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * The ActiveGossipThread sends information. Pick a random partner and send the membership list to that partner
 */
public abstract class AbstractActiveGossiper {

  protected static final Logger LOGGER = Logger.getLogger(AbstractActiveGossiper.class);

  protected final GossipManager gossipManager;
  protected final GossipCore gossipCore;
  private final Histogram sharedDataHistogram;
  private final Histogram sendPerNodeDataHistogram;
  private final Histogram sendMembershipHistogram;
  private final Random random;
  private final GossipSettings gossipSettings;

  public AbstractActiveGossiper(GossipManager gossipManager, GossipCore gossipCore, MetricRegistry registry) {
    this.gossipManager = gossipManager;
    this.gossipCore = gossipCore;
    sharedDataHistogram = registry.histogram(name(AbstractActiveGossiper.class, "sharedDataHistogram-time"));
    sendPerNodeDataHistogram = registry.histogram(name(AbstractActiveGossiper.class, "sendPerNodeDataHistogram-time"));
    sendMembershipHistogram = registry.histogram(name(AbstractActiveGossiper.class, "sendMembershipHistogram-time"));
    random = new Random();
    gossipSettings = gossipManager.getSettings();
  }

  public void init() {

  }

  public void shutdown() {

  }

  public final void sendShutdownMessage(LocalMember me, LocalMember target){
    if (target == null){
      return;
    }
    ShutdownMessage m = new ShutdownMessage();
    m.setNodeId(me.getId());
    m.setShutdownAtNanos(gossipManager.getClock().nanoTime());
    gossipCore.sendOneWay(m, target.getUri());
  }

  public final void sendSharedData(LocalMember me, LocalMember member) {
    if (member == null) {
      return;
    }
    long startTime = System.currentTimeMillis();
    if (gossipSettings.isBulkTransfer()) {
      sendSharedDataInBulkInternal(me, member);
    } else {
      sendSharedDataInternal(me, member);
    }
    sharedDataHistogram.update(System.currentTimeMillis() - startTime);
  }

  /** Send shared data one entry at a time. */
  private void sendSharedDataInternal(LocalMember me, LocalMember member) {
    for (Entry<String, SharedDataMessage> innerEntry : gossipCore.getSharedData().entrySet()){
      if (innerEntry.getValue().getReplicable() != null && !innerEntry.getValue().getReplicable()
              .shouldReplicate(me, member, innerEntry.getValue())) {
        continue;
      }
      UdpSharedDataMessage message = new UdpSharedDataMessage();
      message.setUuid(UUID.randomUUID().toString());
      message.setUriFrom(me.getId());
      copySharedDataMessage(innerEntry.getValue(), message);
      gossipCore.sendOneWay(message, member.getUri());
    }
  }

  /** Send shared data by batching together several entries. */
  private void sendSharedDataInBulkInternal(LocalMember me, LocalMember member) {
    UdpSharedDataBulkMessage udpMessage = new UdpSharedDataBulkMessage();
    udpMessage.setUuid(UUID.randomUUID().toString());
    udpMessage.setUriFrom(me.getId());
    for (Entry<String, SharedDataMessage> innerEntry : gossipCore.getSharedData().entrySet()) {
      if (innerEntry.getValue().getReplicable() != null && !innerEntry.getValue().getReplicable()
              .shouldReplicate(me, member, innerEntry.getValue())) {
        continue;
      }
      SharedDataMessage message = new SharedDataMessage();
      copySharedDataMessage(innerEntry.getValue(), message);
      udpMessage.addMessage(message);
      if (udpMessage.getMessages().size() == gossipSettings.getBulkTransferSize()) {
        gossipCore.sendOneWay(udpMessage, member.getUri());
        udpMessage = new UdpSharedDataBulkMessage();
        udpMessage.setUuid(UUID.randomUUID().toString());
        udpMessage.setUriFrom(me.getId());
      }
    }
    if (udpMessage.getMessages().size() > 0) {
      gossipCore.sendOneWay(udpMessage, member.getUri());
    }
  }

  private void copySharedDataMessage(SharedDataMessage original, SharedDataMessage copy) {
    copy.setExpireAt(original.getExpireAt());
    copy.setKey(original.getKey());
    copy.setNodeId(original.getNodeId());
    copy.setTimestamp(original.getTimestamp());
    copy.setPayload(original.getPayload());
    copy.setReplicable(original.getReplicable());
  }

  public final void sendPerNodeData(LocalMember me, LocalMember member){
    if (member == null){
      return;
    }
    long startTime = System.currentTimeMillis();
    if (gossipSettings.isBulkTransfer()) {
      sendPerNodeDataInBulkInternal(me, member);
    } else {
      sendPerNodeDataInternal(me, member);
    }
    sendPerNodeDataHistogram.update(System.currentTimeMillis() - startTime);
  }

  /** Send per node data one entry at a time. */
  private void sendPerNodeDataInternal(LocalMember me, LocalMember member) {
    for (Entry<String, ConcurrentHashMap<String, PerNodeDataMessage>> entry : gossipCore.getPerNodeData().entrySet()){
      for (Entry<String, PerNodeDataMessage> innerEntry : entry.getValue().entrySet()){
        if (innerEntry.getValue().getReplicable() != null && !innerEntry.getValue().getReplicable()
                .shouldReplicate(me, member, innerEntry.getValue())) {
          continue;
        }
        UdpPerNodeDataMessage message = new UdpPerNodeDataMessage();
        message.setUuid(UUID.randomUUID().toString());
        message.setUriFrom(me.getId());
        copyPerNodeDataMessage(innerEntry.getValue(), message);
        gossipCore.sendOneWay(message, member.getUri());
      }
    }

  }

  /** Send per node data by batching together several entries. */
  private void sendPerNodeDataInBulkInternal(LocalMember me, LocalMember member) {
    for (Entry<String, ConcurrentHashMap<String, PerNodeDataMessage>> entry : gossipCore.getPerNodeData().entrySet()){
      UdpPerNodeDataBulkMessage udpMessage = new UdpPerNodeDataBulkMessage();
      udpMessage.setUuid(UUID.randomUUID().toString());
      udpMessage.setUriFrom(me.getId());
      for (Entry<String, PerNodeDataMessage> innerEntry : entry.getValue().entrySet()){
        if (innerEntry.getValue().getReplicable() != null && !innerEntry.getValue().getReplicable()
                .shouldReplicate(me, member, innerEntry.getValue())) {
          continue;
        }
        PerNodeDataMessage message = new PerNodeDataMessage();
        copyPerNodeDataMessage(innerEntry.getValue(), message);
        udpMessage.addMessage(message);
        if (udpMessage.getMessages().size() == gossipSettings.getBulkTransferSize()) {
          gossipCore.sendOneWay(udpMessage, member.getUri());
          udpMessage = new UdpPerNodeDataBulkMessage();
          udpMessage.setUuid(UUID.randomUUID().toString());
          udpMessage.setUriFrom(me.getId());
        }
      }
      if (udpMessage.getMessages().size() > 0) {
        gossipCore.sendOneWay(udpMessage, member.getUri());
      }
    }
  }

  private void copyPerNodeDataMessage(PerNodeDataMessage original, PerNodeDataMessage copy) {
    copy.setExpireAt(original.getExpireAt());
    copy.setKey(original.getKey());
    copy.setNodeId(original.getNodeId());
    copy.setTimestamp(original.getTimestamp());
    copy.setPayload(original.getPayload());
    copy.setReplicable(original.getReplicable());
  }

  /**
   * Performs the sending of the membership list, after we have incremented our own heartbeat.
   */
  protected void sendMembershipList(LocalMember me, LocalMember member) {
    if (member == null){
      return;
    }
    long startTime = System.currentTimeMillis();
    me.setHeartbeat(System.nanoTime());
    UdpActiveGossipMessage message = new UdpActiveGossipMessage();
    message.setUriFrom(gossipManager.getMyself().getUri().toASCIIString());
    message.setUuid(UUID.randomUUID().toString());
    message.getMembers().add(convert(me));
    for (LocalMember other : gossipManager.getMembers().keySet()) {
      message.getMembers().add(convert(other));
    }
    Response r = gossipCore.send(message, member.getUri());
    if (r instanceof ActiveGossipOk){
      //maybe count metrics here
    } else {
      LOGGER.debug("Message " + message + " generated response " + r);
    }
    sendMembershipHistogram.update(System.currentTimeMillis() - startTime);
  }

  protected final Member convert(LocalMember member){
    Member gm = new Member();
    gm.setCluster(member.getClusterName());
    gm.setHeartbeat(member.getHeartbeat());
    gm.setUri(member.getUri().toASCIIString());
    gm.setId(member.getId());
    gm.setProperties(member.getProperties());
    return gm;
  }

  /**
   *
   * @param memberList
   *          An immutable list
   * @return The chosen LocalGossipMember to gossip with.
   */
  protected LocalMember selectPartner(List<LocalMember> memberList) {
    LocalMember member = null;
    if (memberList.size() > 0) {
      int randomNeighborIndex = random.nextInt(memberList.size());
      member = memberList.get(randomNeighborIndex);
    }
    return member;
  }
}
