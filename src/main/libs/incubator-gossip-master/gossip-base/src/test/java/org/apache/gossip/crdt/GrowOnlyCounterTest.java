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

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class GrowOnlyCounterTest {
  
  @Test
  public void mergeTest() {
    
    Map<String, Long> node1Counter = new HashMap<>();
    node1Counter.put("1", 3L);
    Map<String, Long> node2Counter = new HashMap<>();
    node2Counter.put("2", 1L);
    Map<String, Long> node3Counter = new HashMap<>();
    node3Counter.put("3", 2L);
    
    GrowOnlyCounter gCounter1 = new GrowOnlyCounter(node1Counter);
    GrowOnlyCounter gCounter2 = new GrowOnlyCounter(node2Counter);
    GrowOnlyCounter gCounter3 = new GrowOnlyCounter(node3Counter);
    
    // After node 2 receive from node 1
    gCounter2 = gCounter2.merge(gCounter1);
    Assert.assertEquals(4, (long) gCounter2.value());
    
    // After node 3 receive from node 1
    gCounter3 = gCounter3.merge(gCounter1);
    Assert.assertEquals(5, (long) gCounter3.value());
    
    // After node 3 receive from node 2
    gCounter3 = gCounter3.merge(gCounter2);
    Assert.assertEquals(6, (long) gCounter3.value());
  }
}
