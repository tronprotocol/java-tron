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

package org.tron.common.overlay.node;

import com.alibaba.fastjson.JSON;
import com.google.common.io.ByteStreams;
import io.scalecube.cluster.Cluster;
import io.scalecube.cluster.ClusterConfig;
import io.scalecube.cluster.Member;
import io.scalecube.cluster.membership.MembershipEvent.Type;
import io.scalecube.transport.Address;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.config.Configer;
import org.tron.core.net.message.Message;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerConnectionDelegate;

public class GossipLocalNode implements LocalNode {

  private static final Logger logger = LoggerFactory.getLogger("GossipLocalNode");

  private final int port = Configer.getConf().getInt("overlay.port");

  private Cluster cluster = null;

  private PeerConnectionDelegate peerDel;

  private static final GossipLocalNode INSTANCE = new GossipLocalNode();

  public HashMap<Integer, PeerConnection> listPeer = new HashMap<>();

  private ExecutorService executors;

  private GossipLocalNode() {
    ClusterConfig config = ClusterConfig.builder()
        .seedMembers(getAddresses())
        .portAutoIncrement(false)
        .port(port)
        .build();

    cluster = Cluster.joinAwait(config);

    for (Member member : cluster.otherMembers()) {
      listPeer.put(member.hashCode(), new PeerConnection(this.cluster, member));
    }

    cluster.listenMembership()
        .subscribe(event -> {
          if (event.type() == Type.REMOVED) {
            listPeer.remove(event.oldMember().hashCode());
          } else {
            listPeer.put(event.newMember().hashCode(),
                new PeerConnection(this.cluster, event.newMember()));
          }
        });
  }


  @Override
  public void broadcast(Message message) {
    MessageTypes type = message.getType();
    byte[] value = message.getData();

    if (cluster == null) {
      return;
    }

    cluster.otherMembers().forEach(member -> {
      try {
        io.scalecube.transport.Message msg = io.scalecube.transport.Message.builder()
            .data(new String(value, "ISO-8859-1"))
            .header("type", type.toString())
            .build();

        logger.info("broadcast other members");
        cluster.send(member, msg);
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
    });
  }

  @Override
  public void start() {
    logger.info("listener message");

    executors = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(1000), new ThreadPoolExecutor.CallerRunsPolicy());

    cluster.listen().subscribe(msg -> {
      executors.submit(new StartWorker(msg, peerDel, listPeer, cluster));
    });
  }

  public void stop() {
    cluster.shutdown();
  }


  public void setPeerDel(PeerConnectionDelegate peerDel) {
    this.peerDel = peerDel;
  }

  public static GossipLocalNode getInstance() {
    return INSTANCE;
  }

  private List<Address> getAddresses() {
    List<Address> addresses = loadSeedNode();
    return addresses;
  }

  private List<Address> loadSeedNode() {
    List<Address> addresses = new ArrayList<>();

    String jsonFile = Configer.getConf().getString("seed.directory");

    if (jsonFile.trim().isEmpty()) {
      return addresses;
    }

    InputStream is = getClass().getClassLoader()
        .getResourceAsStream(jsonFile);

    if (is == null) {
      return addresses;
    }

    String json = null;

    try {
      json = new String(ByteStreams.toByteArray(is));

      SeedNodes seedNodes = JSON.parseObject(json, SeedNodes.class);

      for (SeedNode seedNode : seedNodes.getSeedNodes()) {
        addresses.add(Address.create(seedNode.getIp(), seedNode.getPort()));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return addresses;
  }
}
