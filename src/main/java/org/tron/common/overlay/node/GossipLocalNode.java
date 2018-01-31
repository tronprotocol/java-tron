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
import io.scalecube.transport.Address;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.config.Configer;
import org.tron.core.net.message.BlockInventoryMessage;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.FetchBlocksMessage;
import org.tron.core.net.message.Message;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.SyncBlockChainMessage;
import org.tron.core.net.message.TransactionMessage;
import org.tron.core.net.peer.PeerConnection;

public class GossipLocalNode implements LocalNode {

  private static final Logger logger = LoggerFactory.getLogger("GossipLocalNode");

  private final int PORT = Configer.getConf().getInt("overlay.port");

  private Cluster cluster = null;

  private static final GossipLocalNode INSTANCE = new GossipLocalNode();

  private GossipLocalNode() {
    ClusterConfig config = ClusterConfig.builder()
        .seedMembers(getAddresses())
        .portAutoIncrement(false)
        .port(PORT)
        .build();

    cluster = Cluster.joinAwait(config);
  }

  public static GossipLocalNode getInstance() {
    return INSTANCE;
  }

  private List<Address> getAddresses() {
    List<Address> addresses = loadSeedNode();

    if (addresses.isEmpty()) {

    }

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

  public void sendMessage(Member member, Message message) {
    MessageTypes type = message.getType();
    byte[] value = message.getData();

    if (cluster == null) {
      return;
    }

    try {
      io.scalecube.transport.Message msg = io.scalecube.transport.Message.builder()
          .data(new String(value, "ISO-8859-1"))
          .header("type", type.toString())
          .build();

      logger.info("send message to member");
      cluster.send(member, msg);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

  }

  public Cluster getCluster() {
    return cluster;
  }

  public void setCluster(Cluster cluster) {
    this.cluster = cluster;
  }

  @Override
  public Collection<Member> getMembers() {
    Collection<Member> members = cluster.otherMembers();
    return members;
  }

  @Override
  public void start(PeerConnection peer) {
    logger.info("listener message");
    cluster.listen().subscribe(msg -> {
      byte[] newValueBytes = null;
      String key = "";
      try {
        key = msg.header("type");
        newValueBytes = msg.data().toString().getBytes("ISO-8859-1");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }

      //todo
      //set msg type from key, instance msg object
      //set a trx msg first here
      Message message = getMessageByKey(key, newValueBytes, cluster.member(msg.sender()).get());

      peer.onMessage(peer, message);

    });
  }

  private Message getMessageByKey(String key, byte[] content, Member member) {
    Message message = null;

    switch (MessageTypes.valueOf(key)) {
      case BLOCK:
        message = new BlockMessage(content);
        break;
      case TRX:
        message = new TransactionMessage(content);
        break;
      case SYNC_BLOCK_CHAIN:
        message = new SyncBlockChainMessage(content, member);
        break;
      case FETCH_BLOCKS:
        message = new FetchBlocksMessage(content, member);
        break;
      case BLOCK_INVENTORY:
        message = new BlockInventoryMessage(content, member);
        break;
      default:
        try {
          throw new IllegalArgumentException("No such message");
        } catch (IllegalArgumentException e) {
          e.printStackTrace();
        }
    }

    return message;
  }
}
