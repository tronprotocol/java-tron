package org.tron.p2p.discover.socket;

import com.google.protobuf.ByteString;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.message.Message;
import org.tron.p2p.discover.message.MessageType;
import org.tron.p2p.discover.message.kad.FindNodeMessage;
import org.tron.p2p.discover.message.kad.KadMessage;
import org.tron.p2p.discover.message.kad.NeighborsMessage;
import org.tron.p2p.discover.message.kad.PingMessage;
import org.tron.p2p.discover.protocol.kad.KadService;
import org.tron.p2p.discover.protocol.kad.NodeHandler;
import org.tron.p2p.protos.Discover;

public class MessageHandlerTest {

  private static P2pConfig config;
  private P2pConfig previousConfig;
  private KadService service;
  private EmbeddedChannel pipeline;
  private final List<UdpEvent> outbound = new ArrayList<>();

  @BeforeClass
  public static void createConfig() {
    config = new P2pConfig();
    config.setIp("127.0.0.1");
    byte[] homeId = new byte[64];
    homeId[0] = 1;
    config.setNodeID(homeId);
  }

  @Before
  public void setUp() {
    previousConfig = Parameter.p2pConfig;
    Parameter.p2pConfig = config;
    config.setIp("127.0.0.1");
    config.setIpv6(null);
    config.setDiscoverEnable(false);
  }

  @After
  public void tearDown() {
    try {
      if (pipeline != null) {
        pipeline.finishAndReleaseAll();
      }
    } finally {
      if (service != null) {
        service.close();
      }
      Parameter.p2pConfig = previousConfig;
    }
  }

  @Test
  public void unsupportedIpv6PingIsDroppedAndIpv4StillWorks() throws Exception {
    startPipeline(new KadService());
    Node from = new Node(new byte[64], "", "::1", 18888);

    receive(new PingMessage(from, service.getPublicHomeNode()), "::1");

    assertDroppedAndIpv4StillWorks();
  }

  @Test
  public void unsupportedIpv6FindNodeIsDroppedAndIpv4StillWorks() throws Exception {
    startPipeline(new KadService());
    Node from = new Node(new byte[64], "", "::1", 18888);

    receive(new FindNodeMessage(from, new byte[64]), "::1");

    assertDroppedAndIpv4StillWorks();
  }

  @Test
  public void ipv6PingAndFindNodeStillWorkWhenEnabled() throws Exception {
    config.setIpv6("::1");
    startPipeline(new KadService());
    Node from = new Node(new byte[64], "", "::1", 18888);

    receive(new PingMessage(from, service.getPublicHomeNode()), "::1");
    receive(new FindNodeMessage(from, new byte[64]), "::1");

    Assert.assertEquals(1, service.getAllNodes().size());
    assertReply(MessageType.KAD_PONG, "::1");
    assertReply(MessageType.KAD_NEIGHBORS, "::1");
  }

  @Test
  public void messageFailureDoesNotCloseChannelOrPreventNextMessage() throws Exception {
    AtomicBoolean failFirst = new AtomicBoolean(true);
    startPipeline(new KadService() {
      @Override
      public void handleEvent(UdpEvent event) {
        if (failFirst.getAndSet(false)) {
          throw new IllegalArgumentException("Injected per-message failure");
        }
        super.handleEvent(event);
      }
    });
    Node from = new Node(new byte[64], "127.0.0.2", "", 18888);
    PingMessage ping = new PingMessage(from, service.getPublicHomeNode());

    receive(ping, "127.0.0.2");
    Assert.assertTrue(outbound.isEmpty());
    receive(ping, "127.0.0.2");

    assertReply(MessageType.KAD_PONG, "127.0.0.2");
  }

  @Test
  public void channelFailureStillClosesChannel() {
    startPipeline(new KadService());

    pipeline.pipeline().fireExceptionCaught(new IOException("Injected channel failure"));

    Assert.assertFalse(pipeline.isOpen());
  }

  @Test
  public void neighborResponsesFitPacketBudget() throws Exception {
    String longIpv6 = "2606:4700:1234:5678:9abc:def0:1234:5678";
    config.setIp("192.168.100.200");
    config.setIpv6(longIpv6);
    startPipeline(new KadService());

    for (String ipv6 : new String[]{null, "2001:db8::1", longIpv6}) {
      assertNeighborsFit(neighbors(ipv6), Long.MAX_VALUE);
    }
    assertNeighborsFit(neighbors(longIpv6, null), Long.MAX_VALUE);
    assertNeighborsFit(Collections.emptyList(), 1L);
  }

  @Test
  public void scopedIpv6NeighborsRequireValidIpv4() throws Exception {
    Node from = new Node(new byte[64], "192.0.2.1", null, 18888);
    for (String zone : new String[]{"2147483647", "eth0"}) {
      for (String ipv4 : new String[]{"192.0.2.2", "", "invalid"}) {
        Discover.Endpoint neighbor = Discover.Endpoint.newBuilder()
            .setNodeId(ByteString.copyFrom(new byte[64]))
            .setAddress(ByteString.copyFromUtf8(ipv4))
            .setAddressIpv6(ByteString.copyFromUtf8("fe80::1%" + zone))
            .setPort(18888).build();
        Discover.Neighbours payload = Discover.Neighbours.newBuilder()
            .setFrom(KadMessage.getEndpointFromNode(from)).addNeighbours(neighbor)
            .setTimestamp(1L).build();

        List<Object> decoded = decode(new NeighborsMessage(payload.toByteArray()).getSendData());

        if (ipv4.equals("192.0.2.2")) {
          Assert.assertEquals(1, decoded.size());
          NeighborsMessage reply = (NeighborsMessage) ((UdpEvent) decoded.get(0)).getMessage();
          Node node = reply.getNodes().get(0);
          Assert.assertEquals(ipv4, node.getHostV4());
          Assert.assertNull(node.getHostV6());
        } else {
          Assert.assertTrue(decoded.isEmpty());
        }
      }
    }
  }

  @Test
  public void neighborPacketLimitIsExclusive() throws Exception {
    String ipv6 = "2001:db8:1234:5678:9abc:0:0:1";
    config.setIp("192.168.100.200");
    config.setIpv6(ipv6);
    startPipeline(new KadService());
    List<Node> neighbors = neighbors(ipv6);

    // Five- and six-byte timestamps produce exactly 2047- and 2048-byte packets.
    for (long sequence : new long[]{1L << 28, 1L << 35}) {
      byte[] wire = new NeighborsMessage(service.getPublicHomeNode(), neighbors, sequence)
          .getSendData();
      Assert.assertEquals(sequence == (1L << 28) ? 2047 : 2048, wire.length);
      Assert.assertEquals(wire.length < 2048 ? 1 : 0, decode(wire).size());
      assertNeighborsFit(neighbors, sequence);
    }
  }

  private void assertNeighborsFit(List<Node> neighbors, long sequence) throws Exception {
    Node from = service.getPublicHomeNode();
    byte[] original = new NeighborsMessage(from, neighbors, sequence).getSendData();
    NodeHandler handler = service.getNodeHandler(new Node(new byte[64], "127.0.0.2", null,
        18888));
    outbound.clear();

    handler.sendNeighbours(Collections.unmodifiableList(neighbors), sequence);

    List<Message> replies = new ArrayList<>();
    outbound.stream().map(UdpEvent::getMessage)
        .filter(message -> message.getType() == MessageType.KAD_NEIGHBORS)
        .forEach(replies::add);
    Assert.assertEquals(1, replies.size());
    byte[] wire = replies.get(0).getSendData();
    Assert.assertTrue(wire.length < 2048);
    List<Object> decoded = decode(wire);
    Assert.assertEquals(1, decoded.size());
    NeighborsMessage reply = (NeighborsMessage) ((UdpEvent) decoded.get(0)).getMessage();
    Assert.assertEquals(sequence, reply.getTimestamp());
    Assert.assertEquals(KadMessage.getEndpointFromNode(from),
        KadMessage.getEndpointFromNode(reply.getFrom()));
    List<Node> returned = reply.getNodes();
    for (int i = 0; i < returned.size(); i++) {
      Assert.assertEquals(KadMessage.getEndpointFromNode(neighbors.get(i)),
          KadMessage.getEndpointFromNode(returned.get(i)));
    }
    if (original.length < 2048) {
      Assert.assertArrayEquals(original, wire);
    } else {
      Assert.assertTrue(returned.size() < neighbors.size());
      Assert.assertTrue(new NeighborsMessage(from, neighbors.subList(0, returned.size() + 1),
          sequence).getSendData().length >= 2048);
    }
  }

  private static List<Node> neighbors(String... ipv6s) {
    List<Node> nodes = new ArrayList<>();
    for (int i = 0; i < 16; i++) {
      byte[] id = new byte[64];
      id[63] = (byte) (i + 1);
      nodes.add(new Node(id, "192.168.100." + (100 + i), ipv6s[i % ipv6s.length], 18888));
    }
    return nodes;
  }

  private static List<Object> decode(byte[] wire) throws Exception {
    List<Object> decoded = new ArrayList<>();
    InetSocketAddress address = new InetSocketAddress("127.0.0.1", 18888);
    DatagramPacket packet = new DatagramPacket(Unpooled.wrappedBuffer(wire), address, address);
    try {
      new P2pPacketDecoder().decode(null, packet, decoded);
    } finally {
      packet.release();
    }
    return decoded;
  }

  private void startPipeline(KadService kadService) {
    service = kadService;
    service.init();
    config.setDiscoverEnable(true);
    service.setMessageSender(outbound::add);
    pipeline = new EmbeddedChannel(new P2pPacketDecoder(), new MessageHandler(null, service));
  }

  private void receive(Message message, String senderIp) throws Exception {
    byte[] wire = message.getSendData();
    // The triggering IPv6 messages pass wire validation even when local IPv6 is disabled.
    Message.parse(wire);
    pipeline.writeInbound(new DatagramPacket(Unpooled.wrappedBuffer(wire),
        new InetSocketAddress("127.0.0.1", 18888), new InetSocketAddress(senderIp, 18888)));
    pipeline.checkException();
    Assert.assertTrue("One discovery message must not close the shared channel", pipeline.isOpen());
  }

  private void assertDroppedAndIpv4StillWorks() throws Exception {
    Assert.assertTrue(outbound.isEmpty());
    Assert.assertTrue(service.getAllNodes().isEmpty());
    Node from = new Node(new byte[64], "127.0.0.2", "", 18888);

    receive(new PingMessage(from, service.getPublicHomeNode()), "127.0.0.2");
    receive(new FindNodeMessage(from, new byte[64]), "127.0.0.2");

    Assert.assertEquals(1, service.getAllNodes().size());
    assertReply(MessageType.KAD_PONG, "127.0.0.2");
    assertReply(MessageType.KAD_NEIGHBORS, "127.0.0.2");
  }

  private void assertReply(MessageType type, String recipientIp) {
    InetSocketAddress recipient = new InetSocketAddress(recipientIp, 18888);
    Assert.assertTrue("Missing " + type + " reply", outbound.stream().anyMatch(event ->
        event.getMessage().getType() == type && recipient.equals(event.getAddress())));
    outbound.forEach(event -> Assert.assertNotNull(event.getAddress()));
  }
}
