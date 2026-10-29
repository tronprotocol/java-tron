package org.tron.p2p.discover.socket;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
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
import org.tron.p2p.discover.message.kad.PingMessage;
import org.tron.p2p.discover.protocol.kad.KadService;

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
