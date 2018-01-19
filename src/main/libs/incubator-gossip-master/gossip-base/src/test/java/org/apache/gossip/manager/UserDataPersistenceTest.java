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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.gossip.GossipSettings;
import org.apache.gossip.model.PerNodeDataMessage;
import org.apache.gossip.model.SharedDataMessage;
import org.junit.Assert;
import org.junit.Test;

public class UserDataPersistenceTest {

  String nodeId = "1";
  
  private GossipManager sameService() throws URISyntaxException {  
    GossipSettings settings = new GossipSettings();
    settings.setTransportManagerClass("org.apache.gossip.transport.UnitTestTransportManager");
    settings.setProtocolManagerClass("org.apache.gossip.protocol.UnitTestProtocolManager");
    return GossipManagerBuilder.newBuilder()
            .cluster("a")
            .uri(new URI("udp://" + "127.0.0.1" + ":" + (29000 + 1)))
            .id(nodeId)
            .gossipSettings(settings).build();
  }
  
  @Test
  public void givenThatRingIsPersisted() throws UnknownHostException, InterruptedException, URISyntaxException {
    
    { //Create a gossip service and force it to persist its user data
      GossipManager gossipService = sameService();
      gossipService.init();
      gossipService.gossipPerNodeData(getToothpick());
      gossipService.gossipSharedData(getAnotherToothpick());
      gossipService.getUserDataState().writePerNodeToDisk();
      gossipService.getUserDataState().writeSharedToDisk();
      { //read the raw data and confirm
        ConcurrentHashMap<String, ConcurrentHashMap<String, PerNodeDataMessage>> l = gossipService.getUserDataState().readPerNodeFromDisk();
        Assert.assertEquals("red", ((AToothpick) l.get(nodeId).get("a").getPayload()).getColor());
      }
      {
        ConcurrentHashMap<String, SharedDataMessage> l = 
                gossipService.getUserDataState().readSharedDataFromDisk();
        Assert.assertEquals("blue", ((AToothpick) l.get("a").getPayload()).getColor());
      }
      gossipService.shutdown();
    }
    { //recreate the service and see that the data is read back in
      GossipManager gossipService = sameService();
      gossipService.init();
      Assert.assertEquals("red", ((AToothpick) gossipService.findPerNodeGossipData(nodeId, "a").getPayload()).getColor());
      Assert.assertEquals("blue", ((AToothpick) gossipService.findSharedGossipData("a").getPayload()).getColor());
      File f = GossipManager.buildSharedDataPath(gossipService);
      File g = GossipManager.buildPerNodeDataPath(gossipService);
      gossipService.shutdown();
      f.delete();
      g.delete();
    }
  }
  
  public PerNodeDataMessage getToothpick(){
    AToothpick a = new AToothpick();
    a.setColor("red");
    PerNodeDataMessage d = new PerNodeDataMessage();
    d.setExpireAt(Long.MAX_VALUE);
    d.setKey("a");
    d.setPayload(a);
    d.setTimestamp(System.currentTimeMillis());
    return d;
  }
  
  public SharedDataMessage getAnotherToothpick(){
    AToothpick a = new AToothpick();
    a.setColor("blue");
    SharedDataMessage d = new SharedDataMessage();
    d.setExpireAt(Long.MAX_VALUE);
    d.setKey("a");
    d.setPayload(a);
    d.setTimestamp(System.currentTimeMillis());
    return d;
  }
  
  public static class AToothpick {
    private String color;
    public AToothpick(){
      
    }
    public String getColor() {
      return color;
    }
    public void setColor(String color) {
      this.color = color;
    }
    
  }
}
