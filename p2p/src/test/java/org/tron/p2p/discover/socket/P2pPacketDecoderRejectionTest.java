package org.tron.p2p.discover.socket;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.ByteString;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Test;
import org.tron.p2p.base.Constant;
import org.tron.p2p.discover.message.MessageType;
import org.tron.p2p.discover.message.kad.PingMessage;
import org.tron.p2p.protos.Discover;
import org.tron.p2p.utils.ByteArray;

public class P2pPacketDecoderRejectionTest {

  private static final InetSocketAddress LOCAL = new InetSocketAddress("127.0.0.1", 18888);
  private static final InetSocketAddress PEER = new InetSocketAddress("127.0.0.1", 18889);
  private final EmbeddedChannel channel = new EmbeddedChannel(new P2pPacketDecoder());

  @After
  public void tearDown() {
    channel.finishAndReleaseAll();
  }

  @Test
  public void dropsInvalidPacketsAndForwardsValidPing() throws Exception {
    byte[] ping = validPing();
    byte[] missingEndpoint = new PingMessage(Discover.PingMessage.newBuilder()
        .setTimestamp(1).build().toByteArray()).getSendData();

    assertFalse(channel.writeInbound(packet(new byte[0])));
    assertFalse(channel.writeInbound(packet(new byte[1])));
    assertFalse(channel.writeInbound(packet(new byte[Constant.MAX_DISCOVERY_PACKET_SIZE])));
    assertFalse(channel.writeInbound(packet(new byte[] {(byte) 0xff, 0})));
    assertFalse(channel.writeInbound(packet(Arrays.copyOf(ping, ping.length - 1))));
    assertFalse(channel.writeInbound(packet(missingEndpoint)));

    assertTrue(channel.writeInbound(packet(ping)));
    UdpEvent event = channel.readInbound();
    assertEquals(PEER, event.getAddress());
    assertEquals(MessageType.KAD_PING, event.getMessage().getType());
    assertArrayEquals(ping, event.getMessage().getSendData());
  }

  @Test
  public void previewAddsEllipsisOnlyBeyond128Bytes() {
    byte[] data = new byte[129];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) i;
    }
    String hex = ByteArray.toHexString(data);
    assertEquals(hex.substring(0, 254), P2pPacketDecoder.dataPreview(Arrays.copyOf(data, 127)));
    assertEquals(hex.substring(0, 256), P2pPacketDecoder.dataPreview(Arrays.copyOf(data, 128)));
    assertEquals(hex.substring(0, 256) + "...", P2pPacketDecoder.dataPreview(data));
  }

  @Test
  public void countsSuppressedSamplesPerCounter() {
    P2pPacketDecoder.DropCounter first = new P2pPacketDecoder.DropCounter();
    P2pPacketDecoder.DropCounter second = new P2pPacketDecoder.DropCounter();

    assertTrue(first.trySample());
    assertFalse(first.trySample());
    assertTrue(second.trySample());
    assertFalse(first.trySample());

    assertEquals(3, first.totalWhileEnabled);
    assertEquals(2, first.suppressed);
    assertEquals(1, second.totalWhileEnabled);
    assertEquals(0, second.suppressed);
  }

  @Test
  public void removingDecoderCancelsPeriodicSummary() {
    channel.freezeTime();
    channel.advanceTimeBy(1, TimeUnit.MINUTES);
    assertTrue(channel.runScheduledPendingTasks() > 0);
    channel.pipeline().remove(P2pPacketDecoder.class);
    assertEquals(-1L, channel.runScheduledPendingTasks());
  }

  private static byte[] validPing() throws Exception {
    Discover.Endpoint from = Discover.Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(new byte[64]))
        .setAddress(ByteString.copyFromUtf8("127.0.0.1"))
        .setPort(PEER.getPort()).build();
    return new PingMessage(Discover.PingMessage.newBuilder().setFrom(from).build()
        .toByteArray()).getSendData();
  }

  private static DatagramPacket packet(byte[] bytes) {
    return new DatagramPacket(Unpooled.wrappedBuffer(bytes), LOCAL, PEER);
  }
}
