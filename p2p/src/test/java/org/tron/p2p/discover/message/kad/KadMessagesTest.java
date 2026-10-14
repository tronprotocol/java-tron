package org.tron.p2p.discover.message.kad;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.message.MessageType;
import org.tron.p2p.protos.Discover;
import org.tron.p2p.protos.Discover.Endpoint;
import org.tron.p2p.utils.ByteArray;
import org.tron.p2p.utils.NetUtil;

/**
 * Round-trips every kad discovery message through its own wire bytes, which is
 * the path a remote datagram takes: build -> toByteArray -> parse.
 */
public class KadMessagesTest {

  private P2pConfig saved;
  private Node from;
  private Node to;

  @Before
  public void setUp() {
    saved = Parameter.p2pConfig;
    P2pConfig config = new P2pConfig();
    config.setNetworkId(11111);
    Parameter.p2pConfig = config;
    from = new Node(NetUtil.getNodeId(), "127.0.0.1", null, 18888, 18888);
    to = new Node(NetUtil.getNodeId(), "127.0.0.2", null, 18889, 18889);
  }

  @After
  public void tearDown() {
    Parameter.p2pConfig = saved;
  }

  @Test
  public void pingRoundTrip() throws Exception {
    PingMessage sent = new PingMessage(from, to);
    Assert.assertEquals(MessageType.KAD_PING, sent.getType());
    Assert.assertTrue(sent.valid());

    PingMessage parsed = new PingMessage(sent.getData());
    Assert.assertEquals(11111, parsed.getNetworkId());
    Assert.assertArrayEquals(from.getId(), parsed.getFrom().getId());
    Assert.assertEquals("127.0.0.1", parsed.getFrom().getHostV4());
    Assert.assertEquals(18888, parsed.getFrom().getPort());
    Assert.assertEquals("127.0.0.2", parsed.getTo().getHostV4());
    Assert.assertEquals(sent.getTimestamp(), parsed.getTimestamp());
    Assert.assertTrue(parsed.toString().startsWith("[pingMessage"));
  }

  @Test
  public void pongRoundTrip() throws Exception {
    PongMessage sent = new PongMessage(from);
    Assert.assertEquals(MessageType.KAD_PONG, sent.getType());
    Assert.assertTrue(sent.valid());

    PongMessage parsed = new PongMessage(sent.getData());
    Assert.assertEquals(11111, parsed.getNetworkId());
    Assert.assertArrayEquals(from.getId(), parsed.getFrom().getId());
    Assert.assertEquals(sent.getTimestamp(), parsed.getTimestamp());
    Assert.assertNotNull(parsed.toString());
  }

  @Test
  public void findNodeRoundTrip() throws Exception {
    byte[] target = NetUtil.getNodeId();
    FindNodeMessage sent = new FindNodeMessage(from, target);
    Assert.assertEquals(MessageType.KAD_FIND_NODE, sent.getType());
    Assert.assertTrue(sent.valid());

    FindNodeMessage parsed = new FindNodeMessage(sent.getData());
    Assert.assertArrayEquals(target, parsed.getTargetId());
    Assert.assertArrayEquals(from.getId(), parsed.getFrom().getId());
    Assert.assertEquals(sent.getTimestamp(), parsed.getTimestamp());
    Assert.assertNotNull(parsed.toString());
  }

  @Test
  public void neighboursRoundTrip() throws Exception {
    List<Node> neighbours = new ArrayList<>();
    neighbours.add(to);
    neighbours.add(new Node(NetUtil.getNodeId(), "127.0.0.3", null, 18890, 18890));

    NeighborsMessage sent = new NeighborsMessage(from, neighbours, 42L);
    Assert.assertEquals(MessageType.KAD_NEIGHBORS, sent.getType());
    Assert.assertTrue(sent.valid());

    NeighborsMessage parsed = new NeighborsMessage(sent.getData());
    Assert.assertEquals(2, parsed.getNodes().size());
    Assert.assertArrayEquals(from.getId(), parsed.getFrom().getId());
    Assert.assertEquals(sent.getTimestamp(), parsed.getTimestamp());
    Assert.assertNotNull(parsed.toString());
  }

  @Test
  public void neighboursWithNoNodesIsStillValid() throws Exception {
    NeighborsMessage sent =
        new NeighborsMessage(from, new ArrayList<Node>(), 1L);
    NeighborsMessage parsed = new NeighborsMessage(sent.getData());
    Assert.assertTrue(parsed.getNodes().isEmpty());
    Assert.assertTrue(parsed.valid());
  }

  @Test
  public void endpointCarriesIpv6AndOmitsEmptyFields() {
    Node dual = new Node(NetUtil.getNodeId(), "127.0.0.1", "::1", 18888, 18888);
    Endpoint endpoint = KadMessage.getEndpointFromNode(dual);
    Assert.assertEquals(18888, endpoint.getPort());
    Assert.assertFalse(endpoint.getNodeId().isEmpty());
    Assert.assertFalse(endpoint.getAddress().isEmpty());
    Assert.assertFalse(endpoint.getAddressIpv6().isEmpty());

    Node v4Only = new Node(NetUtil.getNodeId(), "127.0.0.1", null, 18888, 18888);
    Assert.assertTrue(KadMessage.getEndpointFromNode(v4Only).getAddressIpv6().isEmpty());
  }

  @Test
  public void aMessageWithNoAddressIsRejected() throws Exception {
    // valid() delegates to NetUtil.validNode, which requires a routable host.
    // An endpoint carrying only a node id and a port is the shape a malformed
    // NEIGHBOURS entry takes, and it must not pass.
    Discover.PongMessage wire = Discover.PongMessage.newBuilder()
        .setFrom(Endpoint.newBuilder()
            .setNodeId(ByteString.copyFrom(NetUtil.getNodeId()))
            .setPort(18888)
            .build())
        .setEcho(1)
        .setTimestamp(System.currentTimeMillis())
        .build();

    PongMessage parsed = new PongMessage(wire.toByteArray());
    Assert.assertFalse(parsed.valid());
  }

  @Test
  public void aMessageWithNoNodeIdIsRejected() throws Exception {
    Discover.PongMessage wire = Discover.PongMessage.newBuilder()
        .setFrom(Endpoint.newBuilder()
            .setAddress(ByteString.copyFrom(ByteArray.fromString("127.0.0.1")))
            .setPort(18888)
            .build())
        .setEcho(1)
        .setTimestamp(System.currentTimeMillis())
        .build();

    PongMessage parsed = new PongMessage(wire.toByteArray());
    Assert.assertFalse(parsed.valid());
  }
}
