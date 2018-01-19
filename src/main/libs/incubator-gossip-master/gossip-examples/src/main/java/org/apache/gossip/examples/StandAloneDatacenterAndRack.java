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

package org.apache.gossip.examples;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.gossip.GossipSettings;
import org.apache.gossip.RemoteMember;
import org.apache.gossip.manager.DatacenterRackAwareActiveGossiper;
import org.apache.gossip.manager.GossipManager;
import org.apache.gossip.manager.GossipManagerBuilder;

public class StandAloneDatacenterAndRack extends StandAloneExampleBase {

  public static void main(String[] args) throws InterruptedException, IOException {
    StandAloneDatacenterAndRack example = new StandAloneDatacenterAndRack(args);
    boolean willRead = true;
    example.exec(willRead);
  }

  StandAloneDatacenterAndRack(String[] args) {
    args = super.checkArgsForClearFlag(args);
    initGossipManager(args);
  }

  void initGossipManager(String[] args) {
    GossipSettings s = new GossipSettings();
    s.setWindowSize(1000);
    s.setGossipInterval(100);
    s.setActiveGossipClass(DatacenterRackAwareActiveGossiper.class.getName());
    Map<String, String> gossipProps = new HashMap<>();
    gossipProps.put("sameRackGossipIntervalMs", "2000");
    gossipProps.put("differentDatacenterGossipIntervalMs", "10000");
    s.setActiveGossipProperties(gossipProps);
    Map<String, String> props = new HashMap<>();
    props.put(DatacenterRackAwareActiveGossiper.DATACENTER, args[4]);
    props.put(DatacenterRackAwareActiveGossiper.RACK, args[5]);
    GossipManager manager = GossipManagerBuilder.newBuilder().cluster("mycluster")
            .uri(URI.create(args[0])).id(args[1]).gossipSettings(s)
            .gossipMembers(
                    Arrays.asList(new RemoteMember("mycluster", URI.create(args[2]), args[3])))
            .properties(props).build();
    manager.init();
    setGossipService(manager);
  }

  @Override
  void printValues(GossipManager gossipService) {
    return;
  }

}
