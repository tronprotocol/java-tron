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
package org.apache.gossip.transport.udp;

import org.junit.Ignore;
import org.junit.Test;

public class UdpTransportIntegrationTest {
  
  // It's currently impossible to create a UdpTransportManager without bringing up an entire stack.
  // This is because AbstractTransportManager creates a PassiveGossipThread (requires GossipManager, 
  // GossipCore) and also requires those same things plus a MetricsRegistry to create the 
  // ActiveGossiper.
  // TODO: test UDPTransportManger semantics (read and write) in isolation.
  // I've written this test to indicate the direction I want things to go.
  // Uncomment/Fix it once the coupling issues are worked out.
  @Test @Ignore
  public void testRoundTrip() {
    /*
    GossipSettings settings0 = new GossipSettings();
    GossipSettings settings1 = new GossipSettings();
    UdpTransportManager mgr0 = new UdpTransportManager(settings0);
    UdpTransportManager mgr1 = new UdpTransportManager(settings1);
    
    mgr0.startEndpoint();
    mgr1.startEndpoint();
    mgr0.startActiveGossiper();
    mgr1.startActiveGossiper();
    
    // wait a little while for convergence
    // perhaps there is a Mockito Whitebox way to foce members
    
    byte[] data = new byte[] {0,1,2,3,4,5};
    Future<byte[]> someData = asyncWaitForData(mgr1);
    mgr0.send(toURI(settings1), data);

    Assert.assertEquals(data, someData.get(1000, TimeUnit.MILLISECONDS));
    
    mgr0.shutdown();
    mgr1.shutdown();
    */
  }
  
  
}
