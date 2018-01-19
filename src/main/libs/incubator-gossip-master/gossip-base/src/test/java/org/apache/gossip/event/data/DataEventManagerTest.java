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
package org.apache.gossip.event.data;

import com.codahale.metrics.MetricRegistry;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@RunWith(JUnitPlatform.class)
public class DataEventManagerTest {
  
  private static Semaphore semaphore;
  private String receivedNodeId;
  private String receivedKey;
  private Object receivedNewValue;
  private Object receivedOldValue;
  
  @BeforeClass
  public static void setup() {
    semaphore = new Semaphore(0);
  }
  
  @Test
  public void perNodeDataEventHandlerAddRemoveTest() {
    DataEventManager eventManager = new DataEventManager(new MetricRegistry());
    
    UpdateNodeDataEventHandler nodeDataEventHandler = (nodeId, key, oldValue, newValue) -> {
    };
    
    eventManager.registerPerNodeDataSubscriber(nodeDataEventHandler);
    Assert.assertEquals(1, eventManager.getPerNodeSubscribersSize());
    eventManager.unregisterPerNodeDataSubscriber(nodeDataEventHandler);
    Assert.assertEquals(0, eventManager.getPerNodeSubscribersSize());
  }
  
  // Test whether the per node data events are fired for matching key
  @Test
  public void perNodeDataEventHandlerTest() throws InterruptedException {
    DataEventManager eventManager = new DataEventManager(new MetricRegistry());
    resetData();

    // A new subscriber "Juliet" is like to notified when per node data change for the key "Romeo"
    UpdateNodeDataEventHandler juliet = (nodeId, key, oldValue, newValue) -> {
      if(!key.equals("Romeo")) return;
      receivedNodeId = nodeId;
      receivedKey = key;
      receivedNewValue = newValue;
      receivedOldValue = oldValue;
      semaphore.release();
    };
    // Juliet register with eventManager
    eventManager.registerPerNodeDataSubscriber(juliet);
    // Romeo is going to sleep after having dinner
    eventManager.notifyPerNodeData("Montague", "Romeo", "sleeping", "eating");
    
    // Juliet should notified
    semaphore.tryAcquire(2, TimeUnit.SECONDS);
    Assert.assertEquals("Montague", receivedNodeId);
    Assert.assertEquals("Romeo", receivedKey);
    Assert.assertEquals("sleeping", receivedNewValue);
    Assert.assertEquals("eating", receivedOldValue);
    
    eventManager.unregisterPerNodeDataSubscriber(juliet);
  }
  
  @Test
  public void sharedDataEventHandlerAddRemoveTest() {
    DataEventManager eventManager = new DataEventManager(new MetricRegistry());
    
    UpdateSharedDataEventHandler sharedDataEventHandler = (key, oldValue, newValue) -> {
    
    };
    eventManager.registerSharedDataSubscriber(sharedDataEventHandler);
    Assert.assertEquals(1, eventManager.getSharedDataSubscribersSize());
    eventManager.unregisterSharedDataSubscriber(sharedDataEventHandler);
    Assert.assertEquals(0, eventManager.getSharedDataSubscribersSize());
    
  }
  
  // Test whether the shared data events are fired
  @Test
  public void sharedDataEventHandlerTest() throws InterruptedException {
    DataEventManager eventManager = new DataEventManager(new MetricRegistry());
    resetData();
    
    // A new subscriber "Alice" is like to notified when shared data change for the key "technology"
    UpdateSharedDataEventHandler alice = (key, oldValue, newValue) -> {
      if(!key.equals("technology")) return;
      receivedKey = key;
      receivedNewValue = newValue;
      receivedOldValue = oldValue;
      semaphore.release();
    };
    // Alice register with eventManager
    eventManager.registerSharedDataSubscriber(alice);
    
    // technology key get changed
    eventManager.notifySharedData("technology", "Java has lambda", "Java is fast");
    
    // Alice should notified
    semaphore.tryAcquire(2, TimeUnit.SECONDS);
    Assert.assertEquals("technology", receivedKey);
    Assert.assertEquals("Java has lambda", receivedNewValue);
    Assert.assertEquals("Java is fast", receivedOldValue);
    
    eventManager.unregisterSharedDataSubscriber(alice);
  }
  
  // Test the MetricRegistry
  @Test
  public void metricRegistryTest() {
    MetricRegistry registry = new MetricRegistry();
    
    DataEventManager eventManager = new DataEventManager(registry);
    
    UpdateNodeDataEventHandler nodeDataEventHandler = (nodeId, key, oldValue, newValue) -> {
    };
    
    UpdateSharedDataEventHandler sharedDataEventHandler = (key, oldValue, newValue) -> {
    };
    
    eventManager.registerPerNodeDataSubscriber(nodeDataEventHandler);
    eventManager.registerSharedDataSubscriber(sharedDataEventHandler);
    
    Assert.assertEquals(1,
            registry.getGauges().get(DataEventConstants.PER_NODE_DATA_SUBSCRIBERS_SIZE).getValue());
    Assert.assertEquals(0,
            registry.getGauges().get(DataEventConstants.PER_NODE_DATA_SUBSCRIBERS_QUEUE_SIZE)
                    .getValue());
    Assert.assertEquals(1,
            registry.getGauges().get(DataEventConstants.SHARED_DATA_SUBSCRIBERS_SIZE).getValue());
    Assert.assertEquals(0,
            registry.getGauges().get(DataEventConstants.SHARED_DATA_SUBSCRIBERS_QUEUE_SIZE)
                    .getValue());
    
  }
  
  private void resetData() {
    receivedNodeId = null;
    receivedKey = null;
    receivedNewValue = null;
    receivedOldValue = null;
  }
  
}
