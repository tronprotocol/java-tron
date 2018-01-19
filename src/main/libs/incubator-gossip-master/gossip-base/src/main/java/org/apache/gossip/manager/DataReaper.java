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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.gossip.model.PerNodeDataMessage;
import org.apache.gossip.model.SharedDataMessage;

/**
 * We wish to periodically sweep user data and remove entries past their timestamp. This
 * implementation periodically sweeps through the data and removes old entries. While it might make
 * sense to use a more specific high performance data-structure to handle eviction, keep in mind
 * that we are not looking to store a large quantity of data as we currently have to transmit this
 * data cluster wide.
 */
public class DataReaper {

  private final GossipCore gossipCore;
  private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);
  private final Clock clock;
  
  public DataReaper(GossipCore gossipCore, Clock clock){
    this.gossipCore = gossipCore;
    this.clock = clock;
  }
  
  public void init(){
    Runnable reapPerNodeData = () -> {
      runPerNodeOnce();
      runSharedOnce();
    };
    scheduledExecutor.scheduleAtFixedRate(reapPerNodeData, 0, 5, TimeUnit.SECONDS);
  }
  
  void runSharedOnce(){
    for (Entry<String, SharedDataMessage> entry : gossipCore.getSharedData().entrySet()){
      if (entry.getValue().getExpireAt() < clock.currentTimeMillis()){
        gossipCore.getSharedData().remove(entry.getKey(), entry.getValue());
      }
    }
  }
  
  void runPerNodeOnce(){
    for (Entry<String, ConcurrentHashMap<String, PerNodeDataMessage>> node : gossipCore.getPerNodeData().entrySet()){
      reapData(node.getValue());
    }
  }
  
  void reapData(ConcurrentHashMap<String, PerNodeDataMessage> concurrentHashMap){
    for (Entry<String, PerNodeDataMessage> entry : concurrentHashMap.entrySet()){
      if (entry.getValue().getExpireAt() < clock.currentTimeMillis()){
        concurrentHashMap.remove(entry.getKey(), entry.getValue());
      }
    }
  }
  
  public void close(){
    scheduledExecutor.shutdown();
    try {
      scheduledExecutor.awaitTermination(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      
    }
  }
}
