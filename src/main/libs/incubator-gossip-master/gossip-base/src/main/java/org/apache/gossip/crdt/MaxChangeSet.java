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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
  Max Change Set CrdtSet. Value which has changed the most wins.
  You cannot delete an element which is not present, and cannot add an element which is already present.
   MC-sets are compact and do the right thing when changes to elements are infrequent compared to the gossiping period.

  Read more: https://github.com/aphyr/meangirls#max-change-sets
  You can view examples of usage in tests:
  MaxChangeSetTest - unit tests
  DataTest - integration test with 2 nodes, MaxChangeSet was serialized/deserialized, sent between nodes, merged
*/

public class MaxChangeSet<ElementType> implements CrdtAddRemoveSet<ElementType, Set<ElementType>, MaxChangeSet<ElementType>> {
  private final Map<ElementType, Integer> struct;

  public MaxChangeSet(){
    struct = new HashMap<>();
  }

  @SafeVarargs
  public MaxChangeSet(ElementType... elements){
    this(new HashSet<>(Arrays.asList(elements)));
  }

  public MaxChangeSet(Set<ElementType> set){
    struct = new HashMap<>();
    for (ElementType e : set){
      struct.put(e, 1);
    }
  }

  public MaxChangeSet(MaxChangeSet<ElementType> first, MaxChangeSet<ElementType> second){
    Function<ElementType, Integer> valueFor = element ->
        Math.max(first.struct.getOrDefault(element, 0), second.struct.getOrDefault(element, 0));
    struct = Stream.concat(first.struct.keySet().stream(), second.struct.keySet().stream())
        .distinct().collect(Collectors.toMap(p -> p, valueFor));
  }

  // for serialization
  MaxChangeSet(Map<ElementType, Integer> struct){
    this.struct = struct;
  }

  Map<ElementType, Integer> getStruct(){
    return struct;
  }

  private MaxChangeSet<ElementType> increment(ElementType e){
    Map<ElementType, Integer> changeMap = new HashMap<>();
    changeMap.put(e, struct.getOrDefault(e, 0) + 1);
    return this.merge(new MaxChangeSet<>(changeMap));
  }

  public MaxChangeSet<ElementType> add(ElementType e){
    if (struct.getOrDefault(e, 0) % 2 == 1){
      return this;
    }
    return increment(e);
  }

  public MaxChangeSet<ElementType> remove(ElementType e){
    if (struct.getOrDefault(e, 0) % 2 == 0){
      return this;
    }
    return increment(e);
  }

  @Override
  public MaxChangeSet<ElementType> merge(MaxChangeSet<ElementType> other){
    return new MaxChangeSet<>(this, other);
  }

  @Override
  public Set<ElementType> value(){
    return struct.entrySet().stream()
        .filter(entry -> (entry.getValue() % 2 == 1))
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }

  @Override
  public MaxChangeSet<ElementType> optimize(){
    return this;
  }

  @Override
  public boolean equals(Object obj){
    return this == obj || (obj != null && getClass() == obj.getClass() && value().equals(((MaxChangeSet) obj).value()));
  }
}