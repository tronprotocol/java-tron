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

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.gossip.GossipSettings;
import org.apache.gossip.LocalMember;
import org.apache.gossip.Member;
import org.apache.gossip.crdt.Crdt;
import org.apache.gossip.event.GossipListener;
import org.apache.gossip.event.GossipState;
import org.apache.gossip.event.data.UpdateNodeDataEventHandler;
import org.apache.gossip.event.data.UpdateSharedDataEventHandler;
import org.apache.gossip.lock.LockManager;
import org.apache.gossip.lock.exceptions.VoteFailedException;
import org.apache.gossip.manager.handlers.MessageHandler;
import org.apache.gossip.model.PerNodeDataMessage;
import org.apache.gossip.model.SharedDataMessage;
import org.apache.gossip.protocol.ProtocolManager;
import org.apache.gossip.transport.TransportManager;
import org.apache.gossip.utils.ReflectionUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public abstract class GossipManager {

  public static final Logger LOGGER = Logger.getLogger(GossipManager.class);
  
  // this mapper is used for ring and user-data persistence only. NOT messages.
  public static final ObjectMapper metdataObjectMapper = new ObjectMapper() {
    private static final long serialVersionUID = 1L;
  {
    enableDefaultTyping();
    configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, false);
  }};

  private final ConcurrentSkipListMap<LocalMember, GossipState> members;
  private final LocalMember me;
  private final GossipSettings settings;
  private final AtomicBoolean gossipServiceRunning;
  
  private TransportManager transportManager;
  private ProtocolManager protocolManager;
  
  private final GossipCore gossipCore;
  private final DataReaper dataReaper;
  private final Clock clock;
  private final ScheduledExecutorService scheduledServiced;
  private final MetricRegistry registry;
  private final RingStatePersister ringState;
  private final UserDataPersister userDataState;
  private final GossipMemberStateRefresher memberStateRefresher;
  
  private final MessageHandler messageHandler;
  private final LockManager lockManager;

  public GossipManager(String cluster,
                       URI uri, String id, Map<String, String> properties, GossipSettings settings,
                       List<Member> gossipMembers, GossipListener listener, MetricRegistry registry,
                       MessageHandler messageHandler) {
    this.settings = settings;
    this.messageHandler = messageHandler;

    clock = new SystemClock();
    me = new LocalMember(cluster, uri, id, clock.nanoTime(), properties,
            settings.getWindowSize(), settings.getMinimumSamples(), settings.getDistribution());
    gossipCore = new GossipCore(this, registry);
    this.lockManager = new LockManager(this, settings.getLockManagerSettings(), registry);
    dataReaper = new DataReaper(gossipCore, clock);
    members = new ConcurrentSkipListMap<>();
    for (Member startupMember : gossipMembers) {
      if (!startupMember.equals(me)) {
        LocalMember member = new LocalMember(startupMember.getClusterName(),
                startupMember.getUri(), startupMember.getId(),
                clock.nanoTime(), startupMember.getProperties(), settings.getWindowSize(),
                settings.getMinimumSamples(), settings.getDistribution());
        //TODO should members start in down state?
        members.put(member, GossipState.DOWN);
      }
    }
    gossipServiceRunning = new AtomicBoolean(true);
    this.scheduledServiced = Executors.newScheduledThreadPool(1);
    this.registry = registry;
    this.ringState = new RingStatePersister(GossipManager.buildRingStatePath(this), this);
    this.userDataState = new UserDataPersister(
        gossipCore,
        GossipManager.buildPerNodeDataPath(this),
        GossipManager.buildSharedDataPath(this));
    this.memberStateRefresher = new GossipMemberStateRefresher(members, settings, listener, this::findPerNodeGossipData);
    readSavedRingState();
    readSavedDataState();
  }

  public MessageHandler getMessageHandler() {
    return messageHandler;
  }

  public ConcurrentSkipListMap<LocalMember, GossipState> getMembers() {
    return members;
  }

  public GossipSettings getSettings() {
    return settings;
  }

  /**
   * @return a read only list of members found in the DOWN state.
   */
  public List<LocalMember> getDeadMembers() {
    return Collections.unmodifiableList(
            members.entrySet()
                    .stream()
                    .filter(entry -> GossipState.DOWN.equals(entry.getValue()))
                    .map(Entry::getKey).collect(Collectors.toList()));
  }

  /**
   *
   * @return a read only list of members found in the UP state
   */
  public List<LocalMember> getLiveMembers() {
    return Collections.unmodifiableList(
            members.entrySet()
                    .stream()
                    .filter(entry -> GossipState.UP.equals(entry.getValue()))
                    .map(Entry::getKey).collect(Collectors.toList()));
  }

  public LocalMember getMyself() {
    return me;
  }

  /**
   * Starts the client. Specifically, start the various cycles for this protocol. Start the gossip
   * thread and start the receiver thread.
   */
  public void init() {
    
    // protocol manager and transport managers are specified in settings.
    // construct them here via reflection.
    
    protocolManager = ReflectionUtils.constructWithReflection(
        settings.getProtocolManagerClass(),
        new Class<?>[] { GossipSettings.class, String.class, MetricRegistry.class },
        new Object[] { settings, me.getId(), this.getRegistry() }
    );
    
    transportManager = ReflectionUtils.constructWithReflection(
        settings.getTransportManagerClass(),
        new Class<?>[] { GossipManager.class, GossipCore.class},
        new Object[] { this, gossipCore }
    );
    
    // start processing gossip messages.
    transportManager.startEndpoint();
    transportManager.startActiveGossiper();
    
    dataReaper.init();
    if (settings.isPersistRingState()) {
      scheduledServiced.scheduleAtFixedRate(ringState, 60, 60, TimeUnit.SECONDS);
    }
    if (settings.isPersistDataState()) {
      scheduledServiced.scheduleAtFixedRate(userDataState, 60, 60, TimeUnit.SECONDS);
    }
    memberStateRefresher.init();
    LOGGER.debug("The GossipManager is started.");
  }
  
  private void readSavedRingState() {
    if (settings.isPersistRingState()) {
      for (LocalMember l : ringState.readFromDisk()) {
        LocalMember member = new LocalMember(l.getClusterName(),
            l.getUri(), l.getId(),
            clock.nanoTime(), l.getProperties(), settings.getWindowSize(),
            settings.getMinimumSamples(), settings.getDistribution());
        members.putIfAbsent(member, GossipState.DOWN);
      }
    }
  }

  private void readSavedDataState() {
    if (settings.isPersistDataState()) {
      for (Entry<String, ConcurrentHashMap<String, PerNodeDataMessage>> l : userDataState.readPerNodeFromDisk().entrySet()) {
        for (Entry<String, PerNodeDataMessage> j : l.getValue().entrySet()) {
          gossipCore.addPerNodeData(j.getValue());
        }
      }
    }
    if (settings.isPersistRingState()) {
      for (Entry<String, SharedDataMessage> l : userDataState.readSharedDataFromDisk().entrySet()) {
        gossipCore.addSharedData(l.getValue());
      }
    }
  }

  /**
   * Shutdown the gossip service.
   */
  public void shutdown() {
    gossipServiceRunning.set(false);
    lockManager.shutdown();
    gossipCore.shutdown();
    transportManager.shutdown();
    dataReaper.close();
    memberStateRefresher.shutdown();
    scheduledServiced.shutdown();
    try {
      scheduledServiced.awaitTermination(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      LOGGER.error(e);
    }
    scheduledServiced.shutdownNow();
  }

  public void gossipPerNodeData(PerNodeDataMessage message){
    Objects.nonNull(message.getKey());
    Objects.nonNull(message.getTimestamp());
    Objects.nonNull(message.getPayload());
    message.setNodeId(me.getId());
    gossipCore.addPerNodeData(message);
  }

  public void gossipSharedData(SharedDataMessage message){
    Objects.nonNull(message.getKey());
    Objects.nonNull(message.getTimestamp());
    Objects.nonNull(message.getPayload());
    message.setNodeId(me.getId());
    gossipCore.addSharedData(message);
  }

  @SuppressWarnings("rawtypes")
  public Crdt findCrdt(String key){
    SharedDataMessage l = gossipCore.getSharedData().get(key);
    if (l == null){
      return null;
    }
    if (l.getExpireAt() < clock.currentTimeMillis()){
      return null;
    } else {
      return (Crdt) l.getPayload();
    }
  }

  @SuppressWarnings("rawtypes")
  public Crdt merge(SharedDataMessage message){
    Objects.nonNull(message.getKey());
    Objects.nonNull(message.getTimestamp());
    Objects.nonNull(message.getPayload());
    message.setNodeId(me.getId());
    if (! (message.getPayload() instanceof Crdt)){
      throw new IllegalArgumentException("Not a subclass of CRDT " + message.getPayload());
    }
    return gossipCore.merge(message);
  }

  public PerNodeDataMessage findPerNodeGossipData(String nodeId, String key){
    ConcurrentHashMap<String, PerNodeDataMessage> j = gossipCore.getPerNodeData().get(nodeId);
    if (j == null){
      return null;
    } else {
      PerNodeDataMessage l = j.get(key);
      if (l == null){
        return null;
      }
      if (l.getExpireAt() != null && l.getExpireAt() < clock.currentTimeMillis()) {
        return null;
      }
      return l;
    }
  }

  public SharedDataMessage findSharedGossipData(String key){
    SharedDataMessage l = gossipCore.getSharedData().get(key);
    if (l == null){
      return null;
    }
    if (l.getExpireAt() < clock.currentTimeMillis()){
      return null;
    } else {
      return l;
    }
  }

  public DataReaper getDataReaper() {
    return dataReaper;
  }

  public RingStatePersister getRingState() {
    return ringState;
  }

  public UserDataPersister getUserDataState() {
    return userDataState;
  }

  public GossipMemberStateRefresher getMemberStateRefresher() {
    return memberStateRefresher;
  }

  public Clock getClock() {
    return clock;
  }

  public MetricRegistry getRegistry() {
    return registry;
  }

  public ProtocolManager getProtocolManager() {
    return protocolManager;
  }

  public TransportManager getTransportManager() {
    return transportManager;
  }
  
  // todo: consider making these path methods part of GossipSettings
  
  public static File buildRingStatePath(GossipManager manager) {
    return new File(manager.getSettings().getPathToRingState(), "ringstate." + manager.getMyself().getClusterName() + "."
        + manager.getMyself().getId() + ".json");
  }
  
  public static File buildSharedDataPath(GossipManager manager){
    return new File(manager.getSettings().getPathToDataState(), "shareddata."
            + manager.getMyself().getClusterName() + "." + manager.getMyself().getId() + ".json");
  }
  
  public static File buildPerNodeDataPath(GossipManager manager) {
    return new File(manager.getSettings().getPathToDataState(), "pernodedata."
            + manager.getMyself().getClusterName() + "." + manager.getMyself().getId() + ".json");
  }
  
  public void registerPerNodeDataSubscriber(UpdateNodeDataEventHandler handler){
    gossipCore.registerPerNodeDataSubscriber(handler);
  }
  
  public void registerSharedDataSubscriber(UpdateSharedDataEventHandler handler){
    gossipCore.registerSharedDataSubscriber(handler);
  }
  
  public void unregisterPerNodeDataSubscriber(UpdateNodeDataEventHandler handler){
    gossipCore.unregisterPerNodeDataSubscriber(handler);
  }
  
  public void unregisterSharedDataSubscriber(UpdateSharedDataEventHandler handler){
    gossipCore.unregisterSharedDataSubscriber(handler);
  }

  public void registerGossipListener(GossipListener listener) {
    memberStateRefresher.register(listener);
  }

  /**
   * Get the lock manager specified with this GossipManager.
   * @return lock manager object.
   */
  public LockManager getLockManager() {
    return lockManager;
  }

  /**
   * Try to acquire a lock on given shared data key.
   * @param key key of tha share data object.
   * @throws VoteFailedException if the locking is failed.
   */
  public void acquireSharedDataLock(String key) throws VoteFailedException{
    lockManager.acquireSharedDataLock(key);
  }
}
