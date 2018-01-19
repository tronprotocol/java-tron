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
import java.net.URI;

import org.apache.gossip.GossipSettings;
import org.apache.gossip.model.PerNodeDataMessage;
import org.apache.gossip.model.SharedDataMessage;
import org.junit.Assert;
import org.junit.Test;

import io.teknek.tunit.TUnit;

public class DataReaperTest {

  private final MetricRegistry registry = new MetricRegistry();
  String myId = "4";
  String key = "key";
  String value = "a";
  
  @Test
  public void testReaperOneShot() {
    GossipSettings settings = new GossipSettings();
    settings.setPersistRingState(false);
    settings.setPersistDataState(false);
    settings.setTransportManagerClass("org.apache.gossip.transport.UnitTestTransportManager");
    settings.setProtocolManagerClass("org.apache.gossip.protocol.UnitTestProtocolManager");
    GossipManager gm = GossipManagerBuilder.newBuilder().cluster("abc").gossipSettings(settings)
            .id(myId).uri(URI.create("udp://localhost:6000")).registry(registry).build();
    gm.init();
    gm.gossipPerNodeData(perNodeDatum(key, value));
    gm.gossipSharedData(sharedDatum(key, value));
    assertDataIsAtCorrectValue(gm);
    gm.getDataReaper().runPerNodeOnce();
    gm.getDataReaper().runSharedOnce();
    assertDataIsRemoved(gm);
    gm.shutdown();
  }

  private void assertDataIsAtCorrectValue(GossipManager gm){
    Assert.assertEquals(value, gm.findPerNodeGossipData(myId, key).getPayload());
    Assert.assertEquals(1, registry.getGauges().get(GossipCoreConstants.PER_NODE_DATA_SIZE).getValue());
    Assert.assertEquals(value, gm.findSharedGossipData(key).getPayload());
    Assert.assertEquals(1, registry.getGauges().get(GossipCoreConstants.SHARED_DATA_SIZE).getValue());
  }
  
  private void assertDataIsRemoved(GossipManager gm){
    TUnit.assertThat(() -> gm.findPerNodeGossipData(myId, key)).equals(null);
    TUnit.assertThat(() -> gm.findSharedGossipData(key)).equals(null);
  }
  
  private PerNodeDataMessage perNodeDatum(String key, String value) {
    PerNodeDataMessage m = new PerNodeDataMessage();
    m.setExpireAt(System.currentTimeMillis() + 5L);
    m.setKey(key);
    m.setPayload(value);
    m.setTimestamp(System.currentTimeMillis());
    return m;
  }
  
  private SharedDataMessage sharedDatum(String key, String value) {
    SharedDataMessage m = new SharedDataMessage();
    m.setExpireAt(System.currentTimeMillis() + 5L);
    m.setKey(key);
    m.setPayload(value);
    m.setTimestamp(System.currentTimeMillis());
    return m;
  }
  
  @Test
  public void testHigherTimestampWins() {
    String myId = "4";
    String key = "key";
    String value = "a";
    GossipSettings settings = new GossipSettings();
    settings.setTransportManagerClass("org.apache.gossip.transport.UnitTestTransportManager");
    settings.setProtocolManagerClass("org.apache.gossip.protocol.UnitTestProtocolManager");
    GossipManager gm = GossipManagerBuilder.newBuilder().cluster("abc").gossipSettings(settings)
            .id(myId).uri(URI.create("udp://localhost:7000")).registry(registry).build();
    gm.init();
    PerNodeDataMessage before = perNodeDatum(key, value);
    PerNodeDataMessage after = perNodeDatum(key, "b");
    after.setTimestamp(after.getTimestamp() - 1);
    gm.gossipPerNodeData(before);
    Assert.assertEquals(value, gm.findPerNodeGossipData(myId, key).getPayload());
    gm.gossipPerNodeData(after);
    Assert.assertEquals(value, gm.findPerNodeGossipData(myId, key).getPayload());
    gm.shutdown();
  }

}
