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

package org.apache.gossip.crdt;

import org.apache.gossip.manager.GossipManager;

import java.util.HashMap;
import java.util.Map;

public class GrowOnlyCounter implements CrdtCounter<Long, GrowOnlyCounter> {
  
  private final Map<String, Long> counters = new HashMap<>();
  
  GrowOnlyCounter(Map<String, Long> counters) {
    this.counters.putAll(counters);
  }
  
  public GrowOnlyCounter(GrowOnlyCounter growOnlyCounter, Builder builder) {
    counters.putAll(growOnlyCounter.counters);
    if (counters.containsKey(builder.myId)) {
      Long newValue = counters.get(builder.myId) + builder.counter;
      counters.replace(builder.myId, newValue);
    } else {
      counters.put(builder.myId, builder.counter);
    }
  }
  
  public GrowOnlyCounter(Builder builder) {
    counters.put(builder.myId, builder.counter);
  }
  
  public GrowOnlyCounter(GossipManager manager) {
    counters.put(manager.getMyself().getId(), 0L);
  }
  
  public GrowOnlyCounter(GrowOnlyCounter growOnlyCounter, GrowOnlyCounter other) {
    counters.putAll(growOnlyCounter.counters);
    for (Map.Entry<String, Long> entry : other.counters.entrySet()) {
      String otherKey = entry.getKey();
      Long otherValue = entry.getValue();
      
      if (counters.containsKey(otherKey)) {
        Long newValue = Math.max(counters.get(otherKey), otherValue);
        counters.replace(otherKey, newValue);
      } else {
        counters.put(otherKey, otherValue);
      }
    }
  }
  
  @Override
  public GrowOnlyCounter merge(GrowOnlyCounter other) {
    return new GrowOnlyCounter(this, other);
  }
  
  @Override
  public Long value() {
    Long globalCount = 0L;
    for (Long increment : counters.values()) {
      globalCount += increment;
    }
    return globalCount;
  }
  
  @Override
  public GrowOnlyCounter optimize() {
    return new GrowOnlyCounter(counters);
  }
  
  @Override
  public boolean equals(Object obj) {
    if (getClass() != obj.getClass())
      return false;
    GrowOnlyCounter other = (GrowOnlyCounter) obj;
    return value().longValue() == other.value().longValue();
  }
  
  @Override
  public String toString() {
    return "GrowOnlyCounter [counters= " + counters + ", Value=" + value() + "]";
  }
  
  Map<String, Long> getCounters() {
    return counters;
  }
  
  public static class Builder {
    
    private final String myId;
    
    private Long counter;
    
    public Builder(GossipManager gossipManager) {
      myId = gossipManager.getMyself().getId();
      counter = 0L;
    }
    
    public GrowOnlyCounter.Builder increment(Long count) {
      counter += count;
      return this;
    }
  }
}
