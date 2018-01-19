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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.gossip.LocalMember;
import org.apache.gossip.manager.GossipManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PNCounterTest {

  private List<GossipManager> mockManagers;

  @Before
  public void setupMocks() {
    GossipManager manager1 = mock(GossipManager.class);
    LocalMember mockMember1 = mock(LocalMember.class);
    when(mockMember1.getId()).thenReturn("x");
    when(manager1.getMyself()).thenReturn(mockMember1);

    GossipManager manager2 = mock(GossipManager.class);
    LocalMember mockMember2 = mock(LocalMember.class);
    when(mockMember2.getId()).thenReturn("y");
    when(manager2.getMyself()).thenReturn(mockMember2);

    GossipManager manager3 = mock(GossipManager.class);
    LocalMember mockMember3 = mock(LocalMember.class);
    when(mockMember3.getId()).thenReturn("z");
    when(manager3.getMyself()).thenReturn(mockMember3);

    mockManagers = new ArrayList<GossipManager>();
    mockManagers.add(manager1);
    mockManagers.add(manager2);
    mockManagers.add(manager3);
  }

  @Test
  public void existanceTest() {
    PNCounter counter = new PNCounter(mockManagers.get(0));
    Assert.assertEquals(0, (long) counter.value());
  }

  @Test
  public void localOperationTest() {
    PNCounter counter = new PNCounter(mockManagers.get(0));
    Assert.assertEquals(0, (long) counter.value());

    counter = new PNCounter(counter, new PNCounter.Builder(mockManagers.get(0)).increment(5L));
    Assert.assertEquals(5, (long) counter.value());

    counter = new PNCounter(counter, new PNCounter.Builder(mockManagers.get(0)).increment(4L));
    Assert.assertEquals(9, (long) counter.value());

    counter = new PNCounter(counter, new PNCounter.Builder(mockManagers.get(0)).decrement(3L));
    Assert.assertEquals(6, (long) counter.value());

    counter = new PNCounter(counter, new PNCounter.Builder(mockManagers.get(0)).decrement(12L));
    Assert.assertEquals(-6, (long) counter.value());
  }

  @Test
  public void oddballLocalOperationTest() {
    PNCounter counter = new PNCounter(mockManagers.get(0));
    Assert.assertEquals(0, (long) counter.value());

    counter = new PNCounter(counter, new PNCounter.Builder(mockManagers.get(0)).increment(-5L));
    Assert.assertEquals(-5, (long) counter.value());

    counter = new PNCounter(counter, new PNCounter.Builder(mockManagers.get(0)).increment(4L));
    Assert.assertEquals(-1, (long) counter.value());

    counter = new PNCounter(counter, new PNCounter.Builder(mockManagers.get(0)).decrement(-3L));
    Assert.assertEquals(2, (long) counter.value());

    counter = new PNCounter(counter, new PNCounter.Builder(mockManagers.get(0)).decrement(-12L));
    Assert.assertEquals(14, (long) counter.value());
  }

  @Test
  public void networkLikeOperations() {
    PNCounter counter1 = new PNCounter(mockManagers.get(0));
    PNCounter counter2 = new PNCounter(mockManagers.get(1));
    PNCounter counter3 = new PNCounter(mockManagers.get(2));

    Assert.assertEquals(0, (long) counter1.value());
    Assert.assertEquals(0, (long) counter2.value());
    Assert.assertEquals(0, (long) counter3.value());

    counter1 = new PNCounter(counter1, new PNCounter.Builder(mockManagers.get(0)).increment(3L));
    Assert.assertEquals(3, (long) counter1.value());

    counter2 = new PNCounter(counter2, new PNCounter.Builder(mockManagers.get(1)).increment(5L));
    Assert.assertEquals(5, (long) counter2.value());

    counter3 = new PNCounter(counter3, new PNCounter.Builder(mockManagers.get(2)).decrement(7L));
    Assert.assertEquals(-7, (long) counter3.value());

    // 2 becomes 2 and 1
    counter2 = counter2.merge(counter1);
    Assert.assertEquals(8, (long) counter2.value());

    // 3 becomes 3 and 1
    counter3 = counter3.merge(counter1);
    Assert.assertEquals(-4, (long) counter3.value());

    // 3 becomes all
    counter3 = counter3.merge(counter2);
    Assert.assertEquals(1, (long) counter3.value());

    // 2 becomes all - different order
    counter2 = counter2.merge(counter3);
    Assert.assertEquals(1, (long) counter3.value());
  }

}
