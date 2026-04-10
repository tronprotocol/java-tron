package org.tron.p2p.discover.socket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Constant;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.message.MessageType;
import org.tron.p2p.discover.message.kad.PingMessage;
import org.tron.p2p.protos.Discover;

public class P2pPacketDecoderTest {

  private static P2pPacketDecoder decoder;
  private static ChannelHandlerContext ctx;
  private static InetSocketAddress senderAddress;

  @BeforeClass
  public static void init() {
    Parameter.p2pConfig = new P2pConfig();
    decoder = new P2pPacketDecoder();
    ctx = Mockito.mock(ChannelHandlerContext.class);
    Channel channel = Mockito.mock(Channel.class);
    Mockito.when(ctx.channel()).thenReturn(channel);
    Mockito.when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 9999));
    senderAddress = new InetSocketAddress("192.168.1.100", 18888);
  }

  @Test
  public void testDecodeValidPingMessage() throws Exception {
    byte[] nodeId1 = new byte[Constant.NODE_ID_LEN];
    Arrays.fill(nodeId1, (byte) 0x01);
    Node fromNode = new Node(nodeId1, "192.168.1.1", null, 18888);

    byte[] nodeId2 = new byte[Constant.NODE_ID_LEN];
    Arrays.fill(nodeId2, (byte) 0x02);
    Node toNode = new Node(nodeId2, "192.168.1.2", null, 18889);

    PingMessage ping = new PingMessage(fromNode, toNode);
    byte[] sendData = ping.getSendData();

    ByteBuf buf = Unpooled.wrappedBuffer(sendData);
    DatagramPacket packet = new DatagramPacket(buf, senderAddress, senderAddress);

    List<Object> out = new ArrayList<>();
    decoder.decode(ctx, packet, out);

    Assert.assertEquals(1, out.size());
    Assert.assertTrue(out.get(0) instanceof UdpEvent);
    UdpEvent event = (UdpEvent) out.get(0);
    Assert.assertEquals(MessageType.KAD_PING, event.getMessage().getType());
    Assert.assertEquals(senderAddress, event.getAddress());
  }

  @Test
  public void testDecodeTooShortPacket() throws Exception {
    // Length <= 1 should be dropped
    ByteBuf buf = Unpooled.wrappedBuffer(new byte[] {0x01});
    DatagramPacket packet = new DatagramPacket(buf, senderAddress, senderAddress);

    List<Object> out = new ArrayList<>();
    decoder.decode(ctx, packet, out);

    Assert.assertTrue(out.isEmpty());
  }

  @Test
  public void testDecodeEmptyPacket() throws Exception {
    ByteBuf buf = Unpooled.buffer(0);
    DatagramPacket packet = new DatagramPacket(buf, senderAddress, senderAddress);

    List<Object> out = new ArrayList<>();
    decoder.decode(ctx, packet, out);

    Assert.assertTrue(out.isEmpty());
  }

  @Test
  public void testDecodeTooLargePacket() throws Exception {
    // Length >= 2048 should be dropped
    byte[] largeData = new byte[2048];
    ByteBuf buf = Unpooled.wrappedBuffer(largeData);
    DatagramPacket packet = new DatagramPacket(buf, senderAddress, senderAddress);

    List<Object> out = new ArrayList<>();
    decoder.decode(ctx, packet, out);

    Assert.assertTrue(out.isEmpty());
  }

  @Test
  public void testDecodeUnknownMessageType() throws Exception {
    // Unknown type byte followed by some data
    byte[] data = new byte[] {(byte) 0xFF, 0x01, 0x02, 0x03};
    ByteBuf buf = Unpooled.wrappedBuffer(data);
    DatagramPacket packet = new DatagramPacket(buf, senderAddress, senderAddress);

    List<Object> out = new ArrayList<>();
    decoder.decode(ctx, packet, out);

    // P2pException should be caught internally, no output
    Assert.assertTrue(out.isEmpty());
  }

  @Test
  public void testDecodeInvalidProtobufData() throws Exception {
    // Valid type byte but invalid protobuf payload
    byte[] data = new byte[20];
    data[0] = MessageType.KAD_PING.getType();
    // Fill rest with garbage
    for (int i = 1; i < data.length; i++) {
      data[i] = (byte) (0xAB + i);
    }
    ByteBuf buf = Unpooled.wrappedBuffer(data);
    DatagramPacket packet = new DatagramPacket(buf, senderAddress, senderAddress);

    List<Object> out = new ArrayList<>();
    decoder.decode(ctx, packet, out);

    // Should be caught by one of the exception handlers, no output
    Assert.assertTrue(out.isEmpty());
  }

  @Test
  public void testDecodeBadMessage() throws Exception {
    // Create a PingMessage with an invalid from node (will fail valid() check)
    // Build protobuf manually with empty nodeId
    Discover.Endpoint emptyEndpoint =
        Discover.Endpoint.newBuilder()
            .setPort(18888)
            .build();

    Discover.PingMessage pingProto =
        Discover.PingMessage.newBuilder()
            .setVersion(1)
            .setFrom(emptyEndpoint)
            .setTo(emptyEndpoint)
            .setTimestamp(System.currentTimeMillis())
            .build();

    byte[] payload = pingProto.toByteArray();
    byte[] sendData = new byte[payload.length + 1];
    sendData[0] = MessageType.KAD_PING.getType();
    System.arraycopy(payload, 0, sendData, 1, payload.length);

    ByteBuf buf = Unpooled.wrappedBuffer(sendData);
    DatagramPacket packet = new DatagramPacket(buf, senderAddress, senderAddress);

    List<Object> out = new ArrayList<>();
    decoder.decode(ctx, packet, out);

    // BAD_MESSAGE exception caught, no output
    Assert.assertTrue(out.isEmpty());
  }
}
