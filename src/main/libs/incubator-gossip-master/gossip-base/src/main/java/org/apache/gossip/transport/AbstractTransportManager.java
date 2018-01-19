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
package org.apache.gossip.transport;

import com.codahale.metrics.MetricRegistry;
import org.apache.gossip.manager.AbstractActiveGossiper;
import org.apache.gossip.manager.GossipCore;
import org.apache.gossip.manager.GossipManager;
import org.apache.gossip.utils.ReflectionUtils;
import org.apache.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manage the protcol threads (active and passive gossipers).
 */
public abstract class AbstractTransportManager implements TransportManager {
  
  public static final Logger LOGGER = Logger.getLogger(AbstractTransportManager.class);
  
  private final ExecutorService gossipThreadExecutor;
  private final AbstractActiveGossiper activeGossipThread;
  protected final GossipManager gossipManager;
  protected final GossipCore gossipCore;
  
  public AbstractTransportManager(GossipManager gossipManager, GossipCore gossipCore) {
    this.gossipManager = gossipManager;
    this.gossipCore = gossipCore;
    gossipThreadExecutor = Executors.newCachedThreadPool();
    activeGossipThread = ReflectionUtils.constructWithReflection(
      gossipManager.getSettings().getActiveGossipClass(),
        new Class<?>[]{
            GossipManager.class, GossipCore.class, MetricRegistry.class
        },
        new Object[]{
            gossipManager, gossipCore, gossipManager.getRegistry()
        });
  }

  // shut down threads etc.
  @Override
  public void shutdown() {
    gossipThreadExecutor.shutdown();
    if (activeGossipThread != null) {
      activeGossipThread.shutdown();
    }
    try {
      boolean result = gossipThreadExecutor.awaitTermination(10, TimeUnit.MILLISECONDS);
      if (!result) {
        // common when blocking patterns are used to read data from a socket.
        LOGGER.warn("executor shutdown timed out");
      }
    } catch (InterruptedException e) {
      LOGGER.error(e);
    }
    gossipThreadExecutor.shutdownNow();
  }

  @Override
  public void startActiveGossiper() {
    activeGossipThread.init();
  }

  @Override
  public abstract void startEndpoint();
}
