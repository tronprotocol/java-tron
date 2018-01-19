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
package org.apache.gossip.replication;

import org.apache.gossip.LocalMember;
import org.apache.gossip.manager.DatacenterRackAwareActiveGossiper;
import org.apache.gossip.model.SharedDataMessage;
import org.junit.Assert;
import org.junit.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(JUnitPlatform.class)
public class DataReplicationTest {
  
  @Test
  public void dataReplicateAllTest() throws URISyntaxException {
    SharedDataMessage message =  getSharedNodeData("public","public", new AllReplicable<>());
    LocalMember me = getLocalMember(new URI("udp://127.0.0.1:8001"),"1");
    LocalMember member = getLocalMember(new URI("udp://127.0.0.1:8002"),"2");
    Assert.assertEquals(true, message.getReplicable().shouldReplicate(me, member, message));
  }
  
  @Test
  public void dataReplicateNoneTest() throws URISyntaxException {
    SharedDataMessage message =  getSharedNodeData("private","private", new NotReplicable<>());
    LocalMember me = getLocalMember(new URI("udp://127.0.0.1:8001"),"1");
    LocalMember member = getLocalMember(new URI("udp://127.0.0.1:8002"),"2");
    Assert.assertEquals(false, message.getReplicable().shouldReplicate(me, member, message));
  }
  
  @Test
  public void dataReplicateWhiteListTest() throws URISyntaxException {
    List<LocalMember> memberList = new ArrayList<>();
    memberList.add(getLocalMember(new URI("udp://127.0.0.1:8001"),"1"));
    memberList.add(getLocalMember(new URI("udp://127.0.0.1:8002"),"2"));
    memberList.add(getLocalMember(new URI("udp://127.0.0.1:8003"),"3"));
    // add node 1 and 2 to the white list
    List<LocalMember> whiteList = new ArrayList<>();
    whiteList.add(memberList.get(0));
    whiteList.add(memberList.get(1));

    SharedDataMessage message = getSharedNodeData("whiteList", "Only allow some nodes",
            new WhiteListReplicable<>(whiteList));
    LocalMember me =  getLocalMember(new URI("udp://127.0.0.1:8004"),"4");
    
    // data should replicate to node 1 and 2 but not 3
    Assert.assertEquals(true,
            message.getReplicable().shouldReplicate(me, memberList.get(0), message));
    Assert.assertEquals(true,
            message.getReplicable().shouldReplicate(me, memberList.get(1), message));
    Assert.assertEquals(false,
            message.getReplicable().shouldReplicate(me, memberList.get(2), message));
  }
  
  @Test
  public void dataReplicateWhiteListNullTest() throws URISyntaxException {
    List<LocalMember> memberList = new ArrayList<>();
    memberList.add(getLocalMember(new URI("udp://127.0.0.1:8001"),"1"));
    memberList.add(getLocalMember(new URI("udp://127.0.0.1:8002"),"2"));

    SharedDataMessage message = getSharedNodeData("whiteList", "Only allow some nodes",
            new WhiteListReplicable<>(null));

    // data should not replicate if no whitelist specified
    Assert.assertEquals(false,
            message.getReplicable().shouldReplicate(memberList.get(0), memberList.get(1), message));
    Assert.assertEquals(false,
            message.getReplicable().shouldReplicate(memberList.get(1), memberList.get(0), message));

  }
  
  @Test
  public void dataReplicateBlackListTest() throws URISyntaxException {
    List<LocalMember> memberList = new ArrayList<>();
    memberList.add(getLocalMember(new URI("udp://127.0.0.1:8001"),"1"));
    memberList.add(getLocalMember(new URI("udp://127.0.0.1:8002"),"2"));
    memberList.add(getLocalMember(new URI("udp://127.0.0.1:8003"),"3"));
    // add node 1 and 2 to the black list
    List<LocalMember> blackList = new ArrayList<>();
    blackList.add(memberList.get(0));
    blackList.add(memberList.get(1));
    
    SharedDataMessage message = getSharedNodeData("blackList", "Disallow some nodes",
            new BlackListReplicable<>(blackList));
    LocalMember me = getLocalMember(new URI("udp://127.0.0.1:8004"),"4");

    // data should not replicate to node 1 and 2
    Assert.assertEquals(false,
            message.getReplicable().shouldReplicate(me, memberList.get(0), message));
    Assert.assertEquals(false,
            message.getReplicable().shouldReplicate(me, memberList.get(1), message));
    Assert.assertEquals(true,
            message.getReplicable().shouldReplicate(me, memberList.get(2), message));
  }
  
  @Test
  public void dataReplicateBlackListNullTest() throws URISyntaxException {
    
    List<LocalMember> memberList = new ArrayList<>();
    memberList.add(getLocalMember(new URI("udp://127.0.0.1:8001"),"1"));
    memberList.add(getLocalMember(new URI("udp://127.0.0.1:8002"),"2"));
    
    SharedDataMessage message = getSharedNodeData("blackList", "Disallow some nodes",
            new BlackListReplicable<>(null));
    
    // data should replicate if no blacklist specified
    Assert.assertEquals(true,
            message.getReplicable().shouldReplicate(memberList.get(0), memberList.get(1), message));
    Assert.assertEquals(true,
            message.getReplicable().shouldReplicate(memberList.get(1), memberList.get(0), message));
  }
  
  @Test
  public void dataReplicateDataCenterTest() throws URISyntaxException {
    
    List<LocalMember> memberListDc1 = new ArrayList<>();
    List<LocalMember> memberListDc2 = new ArrayList<>();

    memberListDc1
            .add(getLocalMemberDc(new URI("udp://10.0.0.1:8000"), "1", "DataCenter1", "Rack1"));
    memberListDc1
            .add(getLocalMemberDc(new URI("udp://10.0.0.2:8000"), "2", "DataCenter1", "Rack2"));
    memberListDc2
            .add(getLocalMemberDc(new URI("udp://10.0.1.1:8000"), "11", "DataCenter2", "Rack1"));
    memberListDc2
            .add(getLocalMemberDc(new URI("udp://10.0.1.2:8000"), "12", "DataCenter2", "Rack2"));

    SharedDataMessage message = getSharedNodeData("datacenter1", "I am in data center 1 rack 1",
            new DataCenterReplicable<>());

    // data should replicate in data center 1
    Assert.assertEquals(true, message.getReplicable()
            .shouldReplicate(memberListDc1.get(0), memberListDc1.get(1), message));
    Assert.assertEquals(true, message.getReplicable()
            .shouldReplicate(memberListDc2.get(0), memberListDc2.get(1), message));
    
    // data should not replicate to data center 2
    Assert.assertEquals(false, message.getReplicable()
            .shouldReplicate(memberListDc1.get(0), memberListDc2.get(0), message));
    Assert.assertEquals(false, message.getReplicable()
            .shouldReplicate(memberListDc1.get(1), memberListDc2.get(1), message));
  }
  
  @Test
  public void dataReplicateDataCenterUnknownDataCenterTest() throws URISyntaxException {
    
    List<LocalMember> memberListDc1 = new ArrayList<>();
    memberListDc1
            .add(getLocalMemberDc(new URI("udp://10.0.0.1:8000"), "1", "DataCenter1", "Rack1"));

    Map<String, String> properties = new HashMap<>();
    LocalMember unknownDc = new LocalMember("cluster1", new URI("udp://10.0.1.2:8000"), "12", 0,
            properties, 1, 0, "");
    
    SharedDataMessage message = getSharedNodeData("datacenter1","I am in data center 1 rack 1", new DataCenterReplicable<>());

    // data should not replicate from dc1 to unknown node
    Assert.assertEquals(false, message.getReplicable()
            .shouldReplicate(memberListDc1.get(0), unknownDc, message));
    // data can replicate from unknown node to dc
    Assert.assertEquals(true, message.getReplicable()
            .shouldReplicate(unknownDc, memberListDc1.get(0), message));

  }

  private static SharedDataMessage getSharedNodeData(String key, String value,
          Replicable<SharedDataMessage> replicable) {
    SharedDataMessage g = new SharedDataMessage();
    g.setExpireAt(Long.MAX_VALUE);
    g.setKey(key);
    g.setPayload(value);
    g.setTimestamp(System.currentTimeMillis());
    g.setReplicable(replicable);
    return g;
  }

  private static LocalMember getLocalMember(URI uri, String id){
    return new LocalMember("cluster1", uri, id, 0, null, 1, 0, "");
  }

  private static LocalMember getLocalMemberDc(URI uri, String id, String dataCenter, String rack){
    Map<String, String> props = new HashMap<>();
    props.put(DatacenterRackAwareActiveGossiper.DATACENTER, dataCenter);
    props.put(DatacenterRackAwareActiveGossiper.RACK, rack);
    return new LocalMember("cluster1", uri, id, 0, props, 1, 0, "");
  }
}
