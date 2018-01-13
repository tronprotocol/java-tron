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

package org.tron.gossip.example;

import static org.tron.core.Constant.TOPIC_BLOCK;
import static org.tron.core.Constant.TOPIC_TRANSACTION;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.tron.gossip.GossipSettings;
import org.tron.gossip.Member;
import org.tron.gossip.RemoteMember;
import org.tron.gossip.crdt.OrSet;
import org.tron.gossip.manager.GossipManager;
import org.tron.gossip.manager.GossipManagerBuilder;
import org.tron.gossip.model.SharedDataMessage;
import org.tron.overlay.Net;
import org.tron.overlay.message.Message;
import org.tron.overlay.message.Type;

public class StandNode implements Net {
  public static final String INDEX_KEY_FOR_SET = "block";

  // is the name of the cluster
  private String cluster;

  // is a URI object containing IP/hostname and port to use on the default adapter on the
  // node's machine
  private String uri;

  // is a unique id for this node(you can use any string)
  private String id;

  private GossipManager gossipManager = null;

  public StandNode(String cluster, String uri, String id) {
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

  private List<Member> getGossipMembers() {
    return Collections.singletonList(new RemoteMember(cluster, URI.create("udp://localhost:10000"), "0"));
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

    getGossipManager().gossipSharedData(StandNode.sharedNodeData(topic, value));
  }

  @Override
  public void deliver(Message message) {

  }
}
