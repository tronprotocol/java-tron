/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.overlay.gossip;

import com.alibaba.fastjson.JSON;
import com.google.common.io.ByteStreams;
import org.apache.gossip.GossipSettings;
import org.apache.gossip.LocalMember;
import org.apache.gossip.Member;
import org.apache.gossip.RemoteMember;
import org.apache.gossip.manager.GossipManager;
import org.apache.gossip.manager.GossipManagerBuilder;
import org.apache.gossip.model.SharedDataMessage;
import org.tron.core.config.Configer;
import org.tron.core.net.message.Message;
import org.tron.core.net.message.MessageTypes;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LocalNode {
    private final String CLUSTER = Configer.getConf().getString("overlay.cluster");
    private final String IP = Configer.getConf().getString("overlay.ip");
    private final String PORT = Configer.getConf().getString("overlay.port");

    // is the name of the cluster
    private String cluster;

    // is a URI object containing IP/hostname and port to use on the default adapter on the
    // node's machine
    private String uri;

    // is a unique id for this node(you can use any string)
    private String id;

    private GossipManager gossipManager = null;

    private static final LocalNode INSTANCE = new LocalNode();

    private LocalNode() {
        setCluster(CLUSTER);
        setUri("udp://" + IP + PORT);
        setId(IP + PORT);

        System.out.println("cluster: " + CLUSTER + ", uri: " + "udp://" + IP + PORT + ", id: " + IP + PORT);

        initGossipManager(CLUSTER, uri, id);
        initGossipService();
    }

    public static LocalNode getInstance() {
        return INSTANCE;
    }

    private SharedDataMessage sharedNodeData(String key, String value) {
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

    private List<Member> getGossipMembers() {
        List<Member> members = loadSeedNode();

        if (members.isEmpty()) {
            return Collections
                    .singletonList(new RemoteMember(CLUSTER, URI.create("udp://127.0.0.1:10000"),
                            "127.0.0.1:10000"));
        }

        return members;
    }

    private List<Member> loadSeedNode() {
        List<Member> members = new ArrayList<>();

        InputStream is = getClass().getClassLoader().getResourceAsStream(Configer.getConf().getString("seed" +
                ".directory"));

        String json = null;

        try {
            json = new String(ByteStreams.toByteArray(is));
        } catch (IOException e) {
            e.printStackTrace();
        }

        SeedNodes seedNodes = JSON.parseObject(json, SeedNodes.class);

        for (SeedNode seedNode : seedNodes.getSeedNodes()) {
            members.add(new RemoteMember(CLUSTER, URI.create(seedNode.getUri()),
                    seedNode.getId()));
        }

        return members;
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

    public void broadcast(Message message) {
        MessageTypes type = message.getType();
        byte[] value = message.getData();

        if (gossipManager == null) {
            return;
        }

        try {
            gossipManager.gossipSharedData(sharedNodeData(type.toString(), new String(value, "ISO-8859-1")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public List<LocalMember> getLiveMembers() {
        return gossipManager.getLiveMembers();
    }

    public List<LocalMember> getDeadMembers() {
        return gossipManager.getDeadMembers();
    }
}
