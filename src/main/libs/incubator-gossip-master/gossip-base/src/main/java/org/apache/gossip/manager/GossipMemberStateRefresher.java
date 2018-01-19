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

import org.apache.gossip.GossipSettings;
import org.apache.gossip.LocalMember;
import org.apache.gossip.event.GossipListener;
import org.apache.gossip.event.GossipState;
import org.apache.gossip.model.PerNodeDataMessage;
import org.apache.gossip.model.ShutdownMessage;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.function.BiFunction;

public class GossipMemberStateRefresher {
  public static final Logger LOGGER = Logger.getLogger(GossipMemberStateRefresher.class);

  private final Map<LocalMember, GossipState> members;
  private final GossipSettings settings;
  private final List<GossipListener> listeners = new CopyOnWriteArrayList<>();
  private final Clock clock;
  private final BiFunction<String, String, PerNodeDataMessage> findPerNodeGossipData;
  private final ExecutorService listenerExecutor;
  private final ScheduledExecutorService scheduledExecutor;
  private final BlockingQueue<Runnable> workQueue;

  public GossipMemberStateRefresher(Map<LocalMember, GossipState> members, GossipSettings settings,
                                    GossipListener listener,
                                    BiFunction<String, String, PerNodeDataMessage> findPerNodeGossipData) {
    this.members = members;
    this.settings = settings;
    listeners.add(listener);
    this.findPerNodeGossipData = findPerNodeGossipData;
    clock = new SystemClock();
    workQueue = new ArrayBlockingQueue<>(1024);
    listenerExecutor = new ThreadPoolExecutor(1, 20, 1, TimeUnit.SECONDS, workQueue,
            new ThreadPoolExecutor.DiscardOldestPolicy());
    scheduledExecutor = Executors.newScheduledThreadPool(1);
  }

  public void init() {
    scheduledExecutor.scheduleAtFixedRate(() -> run(), 0, 100, TimeUnit.MILLISECONDS);
  }

  public void run() {
    try {
      runOnce();
    } catch (RuntimeException ex) {
      LOGGER.warn("scheduled state had exception", ex);
    }
  }

  public void runOnce() {
    for (Entry<LocalMember, GossipState> entry : members.entrySet()) {
      boolean userDown = processOptimisticShutdown(entry);
      if (userDown)
        continue;

      Double phiMeasure = entry.getKey().detect(clock.nanoTime());
      GossipState requiredState;

      if (phiMeasure != null) {
        requiredState = calcRequiredState(phiMeasure);
      } else {
        requiredState = calcRequiredStateCleanupInterval(entry.getKey(), entry.getValue());
      }

      if (entry.getValue() != requiredState) {
        members.put(entry.getKey(), requiredState);
        /* Call listeners asynchronously */
        for (GossipListener listener: listeners)
          listenerExecutor.execute(() -> listener.gossipEvent(entry.getKey(), requiredState));
      }
    }
  }

  public GossipState calcRequiredState(Double phiMeasure) {
    if (phiMeasure > settings.getConvictThreshold())
      return GossipState.DOWN;
    else
      return GossipState.UP;
  }

  public GossipState calcRequiredStateCleanupInterval(LocalMember member, GossipState state) {
    long now = clock.nanoTime();
    long nowInMillis = TimeUnit.MILLISECONDS.convert(now, TimeUnit.NANOSECONDS);
    if (nowInMillis - settings.getCleanupInterval() > member.getHeartbeat()) {
      return GossipState.DOWN;
    } else {
      return state;
    }
  }

  /**
   * If we have a special key the per-node data that means that the node has sent us
   * a pre-emptive shutdown message. We process this so node is seen down sooner
   *
   * @param l member to consider
   * @return true if node forced down
   */
  public boolean processOptimisticShutdown(Entry<LocalMember, GossipState> l) {
    PerNodeDataMessage m = findPerNodeGossipData.apply(l.getKey().getId(), ShutdownMessage.PER_NODE_KEY);
    if (m == null) {
      return false;
    }
    ShutdownMessage s = (ShutdownMessage) m.getPayload();
    if (s.getShutdownAtNanos() > l.getKey().getHeartbeat()) {
      members.put(l.getKey(), GossipState.DOWN);
      if (l.getValue() == GossipState.UP) {
        for (GossipListener listener: listeners)
          listenerExecutor.execute(() -> listener.gossipEvent(l.getKey(), GossipState.DOWN));
      }
      return true;
    }
    return false;
  }

  public void register(GossipListener listener) {
    listeners.add(listener);
  }

  public void shutdown() {
    scheduledExecutor.shutdown();
    try {
      scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      LOGGER.debug("Issue during shutdown", e);
    }
    listenerExecutor.shutdown();
    try {
      listenerExecutor.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      LOGGER.debug("Issue during shutdown", e);
    }
    listenerExecutor.shutdownNow();
  }
}