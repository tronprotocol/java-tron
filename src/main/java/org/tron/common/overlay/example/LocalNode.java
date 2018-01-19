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

package org.tron.common.overlay.example;

import org.apache.gossip.GossipSettings;
import org.apache.gossip.LocalMember;
import org.apache.gossip.Member;
import org.apache.gossip.RemoteMember;
import org.apache.gossip.crdt.OrSet;
import org.apache.gossip.manager.GossipManager;
import org.apache.gossip.manager.GossipManagerBuilder;
import org.apache.gossip.model.SharedDataMessage;
import org.tron.common.overlay.Net;
import org.tron.common.overlay.message.Message;
import org.tron.common.overlay.message.Type;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.tron.core.Constant.TOPIC_BLOCK;
import static org.tron.core.Constant.TOPIC_TRANSACTION;

public class LocalNode implements Net {
    public static final String INDEX_KEY_FOR_SET = "block";

    // is the name of the cluster
    private String cluster;

    // is a URI object containing IP/hostname and port to use on the default adapter on the
    // node's machine
    private String uri;

    // is a unique id for this node(you can use any string)
    private String id;

    private GossipManager gossipManager = null;

    public LocalNode(String cluster, String uri, String id) {
        setCluster(cluster);
        setUri(uri);
        setId(id);

        initGossipManager(cluster, uri, id);
        initGossipService();
    }

    public static void addData(String val, GossipManager gossipService) {
        SharedDataMessage m = new SharedDataMessage();
        m.setExpireAt(Long.MAX_VALUE);
        m.setKey(INDEX_KEY_FOR_SET);
        m.setPayload(new OrSet<String>(val));
        m.setTimestamp(System.currentTimeMillis());
        gossipService.merge(m);
    }

    public static SharedDataMessage sharedNodeData(String key, String value) {
        SharedDataMessage g = new SharedDataMessage();
        g.setExpireAt(Long.MAX_VALUE);
        g.setKey(key);
        g.setPayload(value);
        g.setTimestamp(System.currentTimeMillis());
        return g;
    }

    public void initGossipManager(String cluster, String uri, String id) {
        GossipSettings s = new GossipSettings();
        s.setWindowSize(1000);
        s.setGossipInterval(100);
        GossipManager gossipService = GossipManagerBuilder.newBuilder().cluster(cluster)
                .uri(URI.create(uri)).id(id)
                .gossipMembers(getGossipMembers())
                .gossipSettings(s).build();
        setGossipManager(gossipService);
    }

    public void initGossipService() {
        gossipManager.init();
    }

    public void printLiveMembers() {
        List<LocalMember> members = gossipManager.getLiveMembers();
        if (members.isEmpty()) {
            System.out.println("Live: (none)");
            return;
        }
        System.out.println("Live: " + members.get(0));
        for (int i = 1; i < members.size(); i++) {
            System.out.println("    : " + members.get(i));
        }
    }

    public void printDeadMambers() {
        List<LocalMember> members = gossipManager.getDeadMembers();
        if (members.isEmpty()) {
            System.out.println("Dead: (none)");
            return;
        }
        System.out.println("Dead: " + members.get(0));
        for (int i = 1; i < members.size(); i++) {
            System.out.println("    : " + members.get(i));
        }
    }

    private List<Member> getGossipMembers() {
        return Collections
                .singletonList(new RemoteMember(cluster, URI.create("udp://192.168.0.102:10000"),
                        "192.168.0.102:10000"));
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public GossipManager getGossipManager() {
        return gossipManager;
    }

    public void setGossipManager(GossipManager gossipManager) {
        this.gossipManager = gossipManager;
    }

    @Override
    public void broadcast(Message message) {
        String topic = "";
        String value = message.getMessage();

        if (message.getType() == Type.BLOCK) {
            topic = TOPIC_BLOCK;
        } else if (message.getType() == Type.TRANSACTION) {
            topic = TOPIC_TRANSACTION;
        }

        getGossipManager().gossipSharedData(LocalNode.sharedNodeData(topic, value));
    }

    @Override
    public void deliver(Message message) {

    }
}
