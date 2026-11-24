package org.tron.p2p.connection.socket;

import com.google.protobuf.ByteString;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pEventHandler;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.connection.business.handshake.DisconnectCode;
import org.tron.p2p.connection.business.handshake.HandshakeService;
import org.tron.p2p.connection.message.handshake.HelloMessage;
import org.tron.p2p.protos.Connect;
import org.tron.p2p.protos.Discover;

public class P2pRecvByteBufAllocatorTest {

  private final BlockingQueue<NioSocketChannel> connections = new LinkedBlockingQueue<>();
  private final BlockingQueue<ReadEvent> reads = new LinkedBlockingQueue<>();
  private final BlockingQueue<byte[]> messages = new LinkedBlockingQueue<>();
  private final CountDownLatch inactive = new CountDownLatch(1);
  private NioEventLoopGroup group;
  private io.netty.channel.Channel server;
  private NioSocketChannel accepted;
  private Socket client;
  private P2pConfig previousConfig;
  private List<P2pEventHandler> previousHandlers;
  private Map<Byte, P2pEventHandler> previousHandlerMap;
  private Map<InetAddress, Long> previousBans;
  private Object previousHandshakeService;

  @Before
  public void setUp() throws Exception {
    previousConfig = Parameter.p2pConfig;
    previousHandlers = Parameter.handlerList;
    previousHandlerMap = Parameter.handlerMap;
    previousBans = new HashMap<>(ChannelManager.getBannedNodes().asMap());
    Parameter.p2pConfig = new P2pConfig();
    Parameter.p2pConfig.setIp("127.0.0.1");
    Parameter.p2pConfig.setIpv6("");
    Parameter.p2pConfig.setNodeID(new byte[64]);
    Parameter.handlerList = new ArrayList<>();
    Parameter.handlerMap = new HashMap<>();
    ChannelManager.getBannedNodes().invalidateAll();
    previousHandshakeService = setHandshakeService(new HandshakeService());
    Parameter.addP2pEventHandle(new P2pEventHandler() {
      {
        messageTypes = Collections.singleton((byte) 1);
      }

      @Override
      public void onMessage(Channel channel, byte[] data) {
        messages.add(data);
      }
    });
  }

  @After
  public void tearDown() throws Exception {
    try {
      if (client != null) {
        client.close();
      }
      if (accepted != null) {
        accepted.close().syncUninterruptibly();
      }
      if (server != null) {
        server.close().syncUninterruptibly();
      }
      if (group != null) {
        group.shutdownGracefully(0, 5, TimeUnit.SECONDS).syncUninterruptibly();
      }
      Field count = PendingInboundConnectionHandler.class.getDeclaredField("pendingConnections");
      count.setAccessible(true);
      Assert.assertEquals("Pending reservation leaked", 0, count.getInt(null));
      Assert.assertTrue(ChannelManager.getAllChannels().isEmpty());
    } finally {
      Parameter.p2pConfig = previousConfig;
      Parameter.handlerList = previousHandlers;
      Parameter.handlerMap = previousHandlerMap;
      ChannelManager.getBannedNodes().invalidateAll();
      ChannelManager.getBannedNodes().putAll(previousBans);
      setHandshakeService(previousHandshakeService);
    }
  }

  @Test(timeout = 20000)
  public void incompleteLengthPrefixUsesSmallReceiveBufferAndExpires() throws Exception {
    connect();

    Assert.assertEquals(1024, send(new byte[]{(byte) 0x80}).get(0).capacity);
    ByteBuf buffer = retainedFrameBuffer();
    Assert.assertEquals(1, buffer.readableBytes());
    Assert.assertEquals(1024, buffer.capacity());
    Assert.assertEquals(1, buffer.refCnt());
    Assert.assertTrue(accepted.isActive());

    awaitInactive(Parameter.HANDSHAKE_TIMEOUT_SECONDS + 2);
    Assert.assertEquals(0, buffer.refCnt());
  }

  @Test(timeout = 10000)
  public void incompletePayloadUsesSmallReceiveBuffer() throws Exception {
    assertIncompletePayloadUsesSmallReceiveBuffer();
  }

  @Test(timeout = 10000)
  public void trustedConnectionsAlsoUseSmallReceiveBuffers() throws Exception {
    Parameter.p2pConfig.getTrustNodes().add(InetAddress.getByName("127.0.0.1"));
    assertIncompletePayloadUsesSmallReceiveBuffer();
  }

  private void assertIncompletePayloadUsesSmallReceiveBuffer() throws Exception {
    connect();

    Assert.assertEquals(1024, send(new byte[]{(byte) 0x80, 4, 1}).get(0).capacity);
    ByteBuf buffer = retainedFrameBuffer();
    Assert.assertEquals(3, buffer.readableBytes());
    Assert.assertEquals(1024, buffer.capacity());
    Assert.assertTrue(accepted.isActive());

    send(new byte[]{1});
    Assert.assertSame(buffer, retainedFrameBuffer());
    Assert.assertEquals(4, buffer.readableBytes());
    accepted.close().sync();
    awaitInactive(3);
    Assert.assertEquals(0, buffer.refCnt());
  }

  @Test(timeout = 10000)
  public void fragmentedFrameNearLimitKeepsCumulationSmall() throws Exception {
    connect();
    // A padded five-byte varint declaring 1023 bytes, followed by an incomplete payload.
    send(new byte[]{(byte) 0xff, (byte) 0x87, (byte) 0x80, (byte) 0x80});
    for (ReadEvent read : send(new byte[1023])) {
      Assert.assertEquals(1024, read.capacity);
    }

    ByteBuf buffer = retainedFrameBuffer();
    Assert.assertEquals(1027, buffer.readableBytes());
    // Merging fragments can grow beyond one read buffer, including varint overhead.
    Assert.assertTrue(buffer.capacity() <= 4096);
    Assert.assertTrue(accepted.isActive());
    accepted.close().sync();
    awaitInactive(3);
    Assert.assertEquals(0, buffer.refCnt());
  }

  @Test(timeout = 10000)
  public void oversizedDeclarationStillClosesAndReleases() throws Exception {
    connect();
    send(new byte[]{(byte) 0x80});
    ByteBuf buffer = retainedFrameBuffer();

    send(new byte[]{8});

    awaitInactive(3);
    Assert.assertEquals(0, buffer.refCnt());
  }

  @Test(timeout = 10000)
  public void cachedReceiveHandleGrowsAfterHelloAndAcceptsLargerFrames() throws Exception {
    connect();
    byte[] hello = frame(hello());
    send(Arrays.copyOf(hello, 1));
    RecvByteBufAllocator.Handle handle = accepted.eventLoop().submit(
        () -> accepted.unsafe().recvBufAllocHandle()).get();
    Assert.assertEquals(1024, handle.guess());

    byte[] payload = new byte[4096];
    payload[0] = 1;
    ByteArrayOutputStream batch = new ByteArrayOutputStream();
    batch.write(hello, 1, hello.length - 1);
    batch.write(frame(payload));

    List<ReadEvent> batchReads = send(batch.toByteArray());

    Assert.assertArrayEquals(payload, messages.poll(3, TimeUnit.SECONDS));
    Assert.assertEquals(1024, batchReads.get(0).capacity);
    Assert.assertTrue(batchReads.stream().anyMatch(read -> read.capacity == 256 * 1024));
    Assert.assertSame(handle, accepted.eventLoop().submit(
        () -> accepted.unsafe().recvBufAllocHandle()).get());
    Assert.assertEquals(256 * 1024, handle.guess());
    Assert.assertEquals(1, ChannelManager.getChannelCount());
    Assert.assertTrue(ChannelManager.getAllChannels().get(0).isFinishHandshake());
    Assert.assertNull(accepted.pipeline().get(PendingInboundConnectionHandler.class));
    Assert.assertTrue(accepted.isActive());
  }

  private void connect() throws Exception {
    group = new NioEventLoopGroup(1);
    server = new ServerBootstrap().group(group).channel(NioServerSocketChannel.class)
        .childHandler(new ChannelInitializer<NioSocketChannel>() {
          @Override
          protected void initChannel(NioSocketChannel socket) {
            new P2pChannelInitializer("", false, false).initChannel(socket);
            socket.pipeline().addBefore("protoDecode", "captureReceiveCapacity",
                new ChannelInboundHandlerAdapter() {
                  @Override
                  public void channelRead(ChannelHandlerContext ctx, Object msg) {
                    ByteBuf buffer = (ByteBuf) msg;
                    ReadEvent read = new ReadEvent(buffer.capacity(), buffer.readableBytes());
                    ctx.fireChannelRead(msg);
                    // Observe the real receive allocation without retaining the buffer.
                    reads.add(read);
                  }

                  @Override
                  public void channelInactive(ChannelHandlerContext ctx) {
                    ctx.fireChannelInactive();
                    // closeFuture can complete before the decoder releases its cumulation.
                    inactive.countDown();
                  }
                });
            connections.add(socket);
          }
        }).bind("127.0.0.1", 0).sync().channel();
    client = new Socket();
    client.connect(server.localAddress(), 3000);
    accepted = connections.poll(5, TimeUnit.SECONDS);
    Assert.assertNotNull("Server did not initialize the connection", accepted);
    accepted.eventLoop().submit(() -> { }).sync();
  }

  private List<ReadEvent> send(byte[] bytes) throws Exception {
    client.getOutputStream().write(bytes);
    client.getOutputStream().flush();
    List<ReadEvent> result = new ArrayList<>();
    int received = 0;
    while (received < bytes.length) {
      ReadEvent read = reads.poll(3, TimeUnit.SECONDS);
      Assert.assertNotNull("Socket did not read all the supplied bytes", read);
      received += read.length;
      result.add(read);
    }
    Assert.assertEquals(bytes.length, received);
    accepted.eventLoop().submit(() -> { }).sync();
    return result;
  }

  private ByteBuf retainedFrameBuffer() throws Exception {
    Field field = ByteToMessageDecoder.class.getDeclaredField("cumulation");
    field.setAccessible(true);
    return accepted.eventLoop().submit(() -> (ByteBuf) field.get(
        accepted.pipeline().get(P2pProtobufVarint32FrameDecoder.class))).get();
  }

  private void awaitInactive(int timeoutSeconds) throws Exception {
    Assert.assertTrue("Decoder did not finish closing",
        inactive.await(timeoutSeconds, TimeUnit.SECONDS));
    Assert.assertFalse(accepted.isActive());
  }

  private byte[] hello() throws Exception {
    byte[] id = new byte[64];
    id[0] = 1;
    Discover.Endpoint endpoint = Discover.Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(id))
        .setAddress(ByteString.copyFromUtf8("127.0.0.1"))
        .setPort(18888)
        .build();
    return new HelloMessage(Connect.HelloMessage.newBuilder()
        .setFrom(endpoint)
        .setNetworkId(Parameter.p2pConfig.getNetworkId())
        .setVersion(0)
        .setCode(DisconnectCode.NORMAL.getValue())
        .setTimestamp(System.currentTimeMillis())
        .build().toByteArray()).getSendData();
  }

  private byte[] frame(byte[] payload) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    int length = payload.length;
    while ((length & ~0x7f) != 0) {
      bytes.write((length & 0x7f) | 0x80);
      length >>>= 7;
    }
    bytes.write(length);
    bytes.write(payload, 0, payload.length);
    return bytes.toByteArray();
  }

  private Object setHandshakeService(Object service) throws Exception {
    Field field = ChannelManager.class.getDeclaredField("handshakeService");
    field.setAccessible(true);
    Object previous = field.get(null);
    field.set(null, service);
    return previous;
  }

  private static class ReadEvent {
    private final int capacity;
    private final int length;

    private ReadEvent(int capacity, int length) {
      this.capacity = capacity;
      this.length = length;
    }
  }
}
