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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.apache.gossip.LocalMember;
import org.apache.gossip.Member;
import org.apache.gossip.RemoteMember;
import org.apache.gossip.crdt.Crdt;
import org.apache.gossip.event.GossipState;
import org.apache.gossip.event.data.DataEventManager;
import org.apache.gossip.event.data.UpdateNodeDataEventHandler;
import org.apache.gossip.event.data.UpdateSharedDataEventHandler;
import org.apache.gossip.model.Base;
import org.apache.gossip.model.PerNodeDataMessage;
import org.apache.gossip.model.Response;
import org.apache.gossip.model.SharedDataMessage;
import org.apache.gossip.udp.Trackable;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.*;

public class GossipCore implements GossipCoreConstants {

  class LatchAndBase {
    private final CountDownLatch latch;
    private volatile Base base;
    
    LatchAndBase(){
      latch = new CountDownLatch(1);
    }
    
  }
  public static final Logger LOGGER = Logger.getLogger(GossipCore.class);
  private final GossipManager gossipManager;
  private ConcurrentHashMap<String, LatchAndBase> requests;
  private final ConcurrentHashMap<String, ConcurrentHashMap<String, PerNodeDataMessage>> perNodeData;
  private final ConcurrentHashMap<String, SharedDataMessage> sharedData;
  private final Meter messageSerdeException;
  private final Meter transmissionException;
  private final Meter transmissionSuccess;
  private final DataEventManager eventManager;
  
  public GossipCore(GossipManager manager, MetricRegistry metrics){
    this.gossipManager = manager;
    requests = new ConcurrentHashMap<>();
    perNodeData = new ConcurrentHashMap<>();
    sharedData = new ConcurrentHashMap<>();
    eventManager = new DataEventManager(metrics);
    metrics.register(PER_NODE_DATA_SIZE, (Gauge<Integer>)() -> perNodeData.size());
    metrics.register(SHARED_DATA_SIZE, (Gauge<Integer>)() ->  sharedData.size());
    metrics.register(REQUEST_SIZE, (Gauge<Integer>)() ->  requests.size());
    messageSerdeException = metrics.meter(MESSAGE_SERDE_EXCEPTION);
    transmissionException = metrics.meter(MESSAGE_TRANSMISSION_EXCEPTION);
    transmissionSuccess = metrics.meter(MESSAGE_TRANSMISSION_SUCCESS);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void addSharedData(SharedDataMessage message) {
    while (true){
      SharedDataMessage previous = sharedData.putIfAbsent(message.getKey(), message);
      if (previous == null){
        eventManager.notifySharedData(message.getKey(), message.getPayload(), null);
        return;
      }
      if (message.getPayload() instanceof Crdt){
        SharedDataMessage merged = new SharedDataMessage();
        merged.setExpireAt(message.getExpireAt());
        merged.setKey(message.getKey());
        merged.setNodeId(message.getNodeId());
        merged.setTimestamp(message.getTimestamp());
        Crdt mergedCrdt = ((Crdt) previous.getPayload()).merge((Crdt) message.getPayload());
        merged.setPayload(mergedCrdt);
        boolean replaced = sharedData.replace(message.getKey(), previous, merged);
        if (replaced){
          if(!merged.getPayload().equals(previous.getPayload())) {
            eventManager
                    .notifySharedData(message.getKey(), merged.getPayload(), previous.getPayload());
          }
          return;
        }
      } else {
        if (previous.getTimestamp() < message.getTimestamp()){
          boolean result = sharedData.replace(message.getKey(), previous, message);
          if (result){
            eventManager.notifySharedData(message.getKey(), message.getPayload(), previous.getPayload());
            return;
          }
        } else {
          return;
        }
      }
    }
  }

  public void addPerNodeData(PerNodeDataMessage message){
    ConcurrentHashMap<String,PerNodeDataMessage> nodeMap = new ConcurrentHashMap<>();
    nodeMap.put(message.getKey(), message);
    nodeMap = perNodeData.putIfAbsent(message.getNodeId(), nodeMap);
    if (nodeMap != null){
      PerNodeDataMessage current = nodeMap.get(message.getKey());
      if (current == null){
        nodeMap.putIfAbsent(message.getKey(), message);
        eventManager.notifyPerNodeData(message.getNodeId(), message.getKey(), message.getPayload(), null);
      } else {
        if (current.getTimestamp() < message.getTimestamp()){
          nodeMap.replace(message.getKey(), current, message);
          eventManager.notifyPerNodeData(message.getNodeId(), message.getKey(), message.getPayload(),
                  current.getPayload());
        }
      }
    } else {
      eventManager.notifyPerNodeData(message.getNodeId(), message.getKey(), message.getPayload(), null);
    }
  }

  public ConcurrentHashMap<String, ConcurrentHashMap<String, PerNodeDataMessage>> getPerNodeData(){
    return perNodeData;
  }

  public ConcurrentHashMap<String, SharedDataMessage> getSharedData() {
    return sharedData;
  }

  public void shutdown(){
  }

  public void receive(Base base) {
    if (!gossipManager.getMessageHandler().invoke(this, gossipManager, base)) {
      LOGGER.warn("received message can not be handled");
    }
  }

  /**
   * Sends a blocking message.
   * todo: move functionality to TransportManager layer.
   * @param message
   * @param uri
   * @throws RuntimeException if data can not be serialized or in transmission error
   */
  private void sendInternal(Base message, URI uri) {
    byte[] json_bytes;
    try {
      json_bytes = gossipManager.getProtocolManager().write(message);
    } catch (IOException e) {
      messageSerdeException.mark();
      throw new RuntimeException(e);
    }
    try {
      gossipManager.getTransportManager().send(uri, json_bytes);
      transmissionSuccess.mark();
    } catch (IOException e) {
      transmissionException.mark();
      throw new RuntimeException(e);
    }
  }

  public Response send(Base message, URI uri){
    if (LOGGER.isDebugEnabled()){
      LOGGER.debug("Sending " + message);
      LOGGER.debug("Current request queue " + requests);
    }

    final Trackable t;
    LatchAndBase latchAndBase = null;
    if (message instanceof Trackable){
      t = (Trackable) message;
      latchAndBase = new LatchAndBase();
      requests.put(t.getUuid() + "/" + t.getUriFrom(), latchAndBase);
    } else {
      t = null;
    }
    sendInternal(message, uri);
    if (latchAndBase == null){
      return null;
    }
    
    try {
      boolean complete = latchAndBase.latch.await(1, TimeUnit.SECONDS);
      if (complete){
        return (Response) latchAndBase.base;
      } else{
        return null;
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      if (latchAndBase != null){
        requests.remove(t.getUuid() + "/" + t.getUriFrom());
      }
    }
  }

  /**
   * Sends a message across the network while blocking. Catches and ignores IOException in transmission. Used
   * when the protocol for the message is not to wait for a response
   * @param message the message to send
   * @param u the uri to send it to
   */
  public void sendOneWay(Base message, URI u) {
    try {
      sendInternal(message, u);
    } catch (RuntimeException ex) {
      LOGGER.debug("Send one way failed", ex);
    }
  }

  public void handleResponse(String k, Base v) {
    LatchAndBase latch = requests.get(k);
    latch.base = v;
    latch.latch.countDown();
  }

  /**
   * Merge lists from remote members and update heartbeats
   *
   * @param senderMember
   * @param remoteList
   *
   */
  public void mergeLists(RemoteMember senderMember, List<Member> remoteList) {
    if (LOGGER.isDebugEnabled()){
      debugState(senderMember, remoteList);
    }
    for (LocalMember i : gossipManager.getDeadMembers()) {
      if (i.getId().equals(senderMember.getId())) {
        LOGGER.debug(gossipManager.getMyself() + " contacted by dead member " + senderMember.getUri());
        i.recordHeartbeat(senderMember.getHeartbeat());
        i.setHeartbeat(senderMember.getHeartbeat());
        //TODO consider forcing an UP here
      }
    }
    for (Member remoteMember : remoteList) {
      if (remoteMember.getId().equals(gossipManager.getMyself().getId())) {
        continue;
      }
      LocalMember aNewMember = new LocalMember(remoteMember.getClusterName(),
      remoteMember.getUri(),
      remoteMember.getId(),
      remoteMember.getHeartbeat(),
      remoteMember.getProperties(),
      gossipManager.getSettings().getWindowSize(),
      gossipManager.getSettings().getMinimumSamples(),
      gossipManager.getSettings().getDistribution());
      aNewMember.recordHeartbeat(remoteMember.getHeartbeat());
      Object result = gossipManager.getMembers().putIfAbsent(aNewMember, GossipState.UP);
      if (result != null){
        for (Entry<LocalMember, GossipState> localMember : gossipManager.getMembers().entrySet()){
          if (localMember.getKey().getId().equals(remoteMember.getId())){
            localMember.getKey().recordHeartbeat(remoteMember.getHeartbeat());
            localMember.getKey().setHeartbeat(remoteMember.getHeartbeat());
            localMember.getKey().setProperties(remoteMember.getProperties());
          }
        }
      }
    }
    if (LOGGER.isDebugEnabled()){
      debugState(senderMember, remoteList);
    }
  }

  private void debugState(RemoteMember senderMember,
          List<Member> remoteList){
    LOGGER.warn(
          "-----------------------\n" +
          "Me " + gossipManager.getMyself() + "\n" +
          "Sender " + senderMember + "\n" +
          "RemoteList " + remoteList + "\n" +
          "Live " + gossipManager.getLiveMembers()+ "\n" +
          "Dead " + gossipManager.getDeadMembers()+ "\n" +
          "=======================");
  }

  @SuppressWarnings("rawtypes")
  public Crdt merge(SharedDataMessage message) {
    for (;;){
      SharedDataMessage previous = sharedData.putIfAbsent(message.getKey(), message);
      if (previous == null){
        return (Crdt) message.getPayload();
      }
      SharedDataMessage copy = new SharedDataMessage();
      copy.setExpireAt(message.getExpireAt());
      copy.setKey(message.getKey());
      copy.setNodeId(message.getNodeId());
      copy.setTimestamp(message.getTimestamp());
      @SuppressWarnings("unchecked")
      Crdt merged = ((Crdt) previous.getPayload()).merge((Crdt) message.getPayload());
      copy.setPayload(merged);
      boolean replaced = sharedData.replace(message.getKey(), previous, copy);
      if (replaced){
        return merged;
      }
    }
  }
  
  void registerPerNodeDataSubscriber(UpdateNodeDataEventHandler handler){
    eventManager.registerPerNodeDataSubscriber(handler);
  }
  
  void registerSharedDataSubscriber(UpdateSharedDataEventHandler handler){
    eventManager.registerSharedDataSubscriber(handler);
  }
  
  void unregisterPerNodeDataSubscriber(UpdateNodeDataEventHandler handler){
    eventManager.unregisterPerNodeDataSubscriber(handler);
  }
  
  void unregisterSharedDataSubscriber(UpdateSharedDataEventHandler handler){
    eventManager.unregisterSharedDataSubscriber(handler);
  }
}
