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

import org.apache.gossip.manager.Clock;
import org.apache.gossip.manager.SystemClock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
  Last write wins CrdtSet
  Each operation has timestamp: when you add or remove SystemClock is used to get current time in nanoseconds.
  When all add/remove operations are within the only node LWWSet is guaranteed to work like a Set.
  If you have multiple nodes with ideally synchronized clocks:
    You will observe operations on all machines later than on the initiator, but the last operations on cluster will win.
  If you have some significant clock drift you will suffer from data loss.

  Read more: https://github.com/aphyr/meangirls#lww-element-set

  You can view examples of usage in tests:
  LwwSetTest - unit tests
  DataTest - integration test with 2 nodes, LWWSet was serialized/deserialized, sent between nodes, merged
*/

public class LwwSet<ElementType> implements CrdtAddRemoveSet<ElementType, Set<ElementType>, LwwSet<ElementType>> {
  static private Clock clock = new SystemClock();

  private final Map<ElementType, Timestamps> struct;

  static class Timestamps {
    private final long latestAdd;
    private final long latestRemove;

    Timestamps(){
      latestAdd = 0;
      latestRemove = 0;
    }

    Timestamps(long add, long remove){
      latestAdd = add;
      latestRemove = remove;
    }

    long getLatestAdd(){
      return latestAdd;
    }

    long getLatestRemove(){
      return latestRemove;
    }

    // consider element present when addTime >= removeTime, so we prefer add to remove
    boolean isPresent(){
      return latestAdd >= latestRemove;
    }

    Timestamps updateAdd(){
      return new Timestamps(clock.nanoTime(), latestRemove);
    }

    Timestamps updateRemove(){
      return new Timestamps(latestAdd, clock.nanoTime());
    }

    Timestamps merge(Timestamps other){
      if (other == null){
        return this;
      }
      return new Timestamps(Math.max(latestAdd, other.latestAdd), Math.max(latestRemove, other.latestRemove));
    }
  }


  public LwwSet(){
    struct = new HashMap<>();
  }

  @SafeVarargs
  public LwwSet(ElementType... elements){
    this(new HashSet<>(Arrays.asList(elements)));
  }

  public LwwSet(Set<ElementType> set){
    struct = new HashMap<>();
    for (ElementType e : set){
      struct.put(e, new Timestamps().updateAdd());
    }
  }

  public LwwSet(LwwSet<ElementType> first, LwwSet<ElementType> second){
    Function<ElementType, Timestamps> timestampsFor = p -> {
      Timestamps firstTs = first.struct.get(p);
      Timestamps secondTs = second.struct.get(p);
      if (firstTs == null){
        return secondTs;
      }
      return firstTs.merge(secondTs);
    };
    struct = Stream.concat(first.struct.keySet().stream(), second.struct.keySet().stream())
        .distinct().collect(Collectors.toMap(p -> p, timestampsFor));
  }

  public LwwSet<ElementType> add(ElementType e){
    return this.merge(new LwwSet<>(e));
  }

  // for serialization
  LwwSet(Map<ElementType, Timestamps> struct){
    this.struct = struct;
  }

  Map<ElementType, Timestamps> getStruct(){
    return struct;
  }


  public LwwSet<ElementType> remove(ElementType e){
    Timestamps eTimestamps = struct.get(e);
    if (eTimestamps == null || !eTimestamps.isPresent()){
      return this;
    }
    Map<ElementType, Timestamps> changeMap = new HashMap<>();
    changeMap.put(e, eTimestamps.updateRemove());
    return this.merge(new LwwSet<>(changeMap));
  }

  @Override
  public LwwSet<ElementType> merge(LwwSet<ElementType> other){
    return new LwwSet<>(this, other);
  }

  @Override
  public Set<ElementType> value(){
    return struct.entrySet().stream()
        .filter(entry -> entry.getValue().isPresent())
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }

  @Override
  public LwwSet<ElementType> optimize(){
    return this;
  }

  @Override
  public boolean equals(Object obj){
    return this == obj || (obj != null && getClass() == obj.getClass() && value().equals(((LwwSet) obj).value()));
  }
}