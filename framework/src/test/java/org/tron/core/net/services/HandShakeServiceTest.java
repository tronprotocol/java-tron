package org.tron.core.net.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.tron.core.net.message.handshake.HelloMessage.getEndpointFromNode;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.read.ListAppender;
import com.google.protobuf.ByteString;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.tron.common.TestConstants;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.PeerManagerStateResetter;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.net.P2pEventHandlerImpl;
import org.tron.core.net.TronNetService;
import org.tron.core.net.message.handshake.HelloMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.core.net.service.handshake.HandshakeService;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pService;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.discover.Node;
import org.tron.p2p.utils.NetUtil;
import org.tron.program.Version;
import org.tron.protos.Discover.Endpoint;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.HelloMessage.Builder;
import org.tron.protos.Protocol.ReasonCode;

public class HandShakeServiceTest {

  private static TronApplicationContext context;
  private PeerConnection peer;
  private static P2pEventHandlerImpl p2pEventHandler;
  private static ApplicationContext ctx;
  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @BeforeClass
  public static void init() throws Exception {
    PeerManagerStateResetter.reset();
    Args.setParam(new String[] {"--output-directory",
        temporaryFolder.newFolder().toString(), "--debug"}, TestConstants.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    p2pEventHandler = context.getBean(P2pEventHandlerImpl.class);
    ctx = (ApplicationContext) ReflectUtils.getFieldObject(p2pEventHandler, "ctx");

    TronNetService tronNetService = context.getBean(TronNetService.class);
    Parameter.p2pConfig = new P2pConfig();
    ReflectUtils.setFieldValue(tronNetService, "p2pConfig", Parameter.p2pConfig);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
  }

  @After
  public void clearPeers() {
    for (PeerConnection p : PeerManager.getPeers()) {
      PeerManager.remove(p.getChannel());
    }
  }

  @Test
  public void testOkHelloMessage()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    InetSocketAddress a1 = new InetSocketAddress("127.0.0.1", 10001);
    Channel c1 = mock(Channel.class);
    Mockito.when(c1.getInetSocketAddress()).thenReturn(a1);
    Mockito.when(c1.getInetAddress()).thenReturn(a1.getAddress());
    PeerManager.add(ctx, c1);
    peer = PeerManager.getPeers().get(0);

    Method method = p2pEventHandler.getClass()
        .getDeclaredMethod("processMessage", PeerConnection.class, byte[].class);
    method.setAccessible(true);

    //ok
    Node node = new Node(NetUtil.getNodeId(), a1.getAddress().getHostAddress(), null, a1.getPort());
    HelloMessage helloMessage = new HelloMessage(node, System.currentTimeMillis(),
        ChainBaseManager.getChainBaseManager());
    Assert.assertNotNull(helloMessage.toString());

    Assert.assertEquals(Version.getVersion(),
        new String(helloMessage.getHelloMessage().getCodeVersion().toByteArray()));
    method.invoke(p2pEventHandler, peer, helloMessage.getSendBytes());

    //dup hello message
    peer.setHelloMessageReceive(helloMessage);
    method.invoke(p2pEventHandler, peer, helloMessage.getSendBytes());

    //dup peer
    peer.setHelloMessageReceive(null);
    Mockito.when(c1.isDisconnect()).thenReturn(true);
    method.invoke(p2pEventHandler, peer, helloMessage.getSendBytes());
  }

  @Test
  public void testInvalidHelloMessage() {
    InetSocketAddress a1 = new InetSocketAddress("127.0.0.1", 10001);
    Node node = new Node(NetUtil.getNodeId(), a1.getAddress().getHostAddress(), null, a1.getPort());
    Protocol.HelloMessage.Builder builder =
        getHelloMessageBuilder(node, System.currentTimeMillis(),
            ChainBaseManager.getChainBaseManager());
    //block hash is empty
    try {
      BlockCapsule.BlockId hid = ChainBaseManager.getChainBaseManager().getHeadBlockId();
      Protocol.HelloMessage.BlockId okBlockId = Protocol.HelloMessage.BlockId.newBuilder()
          .setHash(ByteString.copyFrom(new byte[32]))
          .setNumber(hid.getNum())
          .build();
      Protocol.HelloMessage.BlockId invalidBlockId = Protocol.HelloMessage.BlockId.newBuilder()
          .setHash(ByteString.copyFrom(new byte[31]))
          .setNumber(hid.getNum())
          .build();
      builder.setHeadBlockId(invalidBlockId);
      HelloMessage helloMessage = new HelloMessage(builder.build().toByteArray());
      Assert.assertFalse(helloMessage.valid());

      builder.setHeadBlockId(okBlockId);
      builder.setGenesisBlockId(invalidBlockId);
      HelloMessage helloMessage2 = new HelloMessage(builder.build().toByteArray());
      Assert.assertFalse(helloMessage2.valid());

      builder.setGenesisBlockId(okBlockId);
      builder.setSolidBlockId(invalidBlockId);
      HelloMessage helloMessage3 = new HelloMessage(builder.build().toByteArray());
      Assert.assertFalse(helloMessage3.valid());
    } catch (Exception e) {
      Assert.fail();
    }
  }

  @Test
  public void testInvalidHelloMessage2() throws Exception {
    Protocol.HelloMessage.Builder builder = getTestHelloMessageBuilder();
    Assert.assertTrue(new HelloMessage(builder.build().toByteArray()).valid());

    builder.setAddress(ByteString.copyFrom(new byte[201]));
    HelloMessage helloMessage = new HelloMessage(builder.build().toByteArray());
    Assert.assertFalse(helloMessage.valid());

    builder.setAddress(ByteString.copyFrom(new byte[200]));
    helloMessage = new HelloMessage(builder.build().toByteArray());
    Assert.assertTrue(helloMessage.valid());

    builder.setSignature(ByteString.copyFrom(new byte[201]));
    helloMessage = new HelloMessage(builder.build().toByteArray());
    Assert.assertFalse(helloMessage.valid());

    builder.setSignature(ByteString.copyFrom(new byte[200]));
    helloMessage = new HelloMessage(builder.build().toByteArray());
    Assert.assertTrue(helloMessage.valid());

    builder.setCodeVersion(ByteString.copyFrom(new byte[201]));
    helloMessage = new HelloMessage(builder.build().toByteArray());
    Assert.assertFalse(helloMessage.valid());

    builder.setCodeVersion(ByteString.copyFrom(new byte[200]));
    helloMessage = new HelloMessage(builder.build().toByteArray());
    Assert.assertTrue(helloMessage.valid());
  }


  @Test
  public void testRelayHelloMessage() throws NoSuchMethodException {
    InetSocketAddress a1 = new InetSocketAddress("127.0.0.1", 10001);
    Channel c1 = mock(Channel.class);
    Mockito.when(c1.getInetSocketAddress()).thenReturn(a1);
    Mockito.when(c1.getInetAddress()).thenReturn(a1.getAddress());
    PeerManager.add(ctx, c1);
    peer = PeerManager.getPeers().get(0);

    Method method = p2pEventHandler.getClass()
        .getDeclaredMethod("processMessage", PeerConnection.class, byte[].class);
    method.setAccessible(true);

    //address is empty
    Args.getInstance().fastForward = true;
    clearPeers();
    Node node2 = new Node(NetUtil.getNodeId(), a1.getAddress().getHostAddress(), null, 10002);
    Protocol.HelloMessage.Builder builder =
        getHelloMessageBuilder(node2, System.currentTimeMillis(),
            ChainBaseManager.getChainBaseManager());

    try {
      HelloMessage helloMessage = new HelloMessage(builder.build().toByteArray());
      method.invoke(p2pEventHandler, peer, helloMessage.getSendBytes());
    } catch (Exception e) {
      Assert.fail();
    }
    Args.getInstance().fastForward = false;
  }

  @Test
  public void testLowAndGenesisBlockNum() throws NoSuchMethodException {
    InetSocketAddress a1 = new InetSocketAddress("127.0.0.1", 10001);
    Channel c1 = mock(Channel.class);
    Mockito.when(c1.getInetSocketAddress()).thenReturn(a1);
    Mockito.when(c1.getInetAddress()).thenReturn(a1.getAddress());
    PeerManager.add(ctx, c1);
    peer = PeerManager.getPeers().get(0);

    Method method = p2pEventHandler.getClass()
        .getDeclaredMethod("processMessage", PeerConnection.class, byte[].class);
    method.setAccessible(true);

    Node node2 = new Node(NetUtil.getNodeId(), a1.getAddress().getHostAddress(), null, 10002);

    //peer's lowestBlockNum > my headBlockNum => peer is light, LIGHT_NODE_SYNC_FAIL
    Protocol.HelloMessage.Builder builder =
        getHelloMessageBuilder(node2, System.currentTimeMillis(),
            ChainBaseManager.getChainBaseManager());
    builder.setLowestBlockNum(ChainBaseManager.getChainBaseManager().getLowestBlockNum() + 1);
    try {
      HelloMessage helloMessage = new HelloMessage(builder.build().toByteArray());
      method.invoke(p2pEventHandler, peer, helloMessage.getSendBytes());
    } catch (Exception e) {
      Assert.fail();
    }

    //genesisBlock is not equal => INCOMPATIBLE_CHAIN
    builder = getHelloMessageBuilder(node2, System.currentTimeMillis(),
        ChainBaseManager.getChainBaseManager());
    BlockCapsule.BlockId gid = ChainBaseManager.getChainBaseManager().getGenesisBlockId();
    Protocol.HelloMessage.BlockId gBlockId = Protocol.HelloMessage.BlockId.newBuilder()
        .setHash(gid.getByteString())
        .setNumber(gid.getNum() + 1)
        .build();
    builder.setGenesisBlockId(gBlockId);
    try {
      HelloMessage helloMessage = new HelloMessage(builder.build().toByteArray());
      method.invoke(p2pEventHandler, peer, helloMessage.getSendBytes());
    } catch (Exception e) {
      Assert.fail();
    }

    // peer's solidityBlock <= my solidityBlock, but not contained
    // and my lowestBlockNum <= peer's solidityBlock  => FORKED
    builder = getHelloMessageBuilder(node2, System.currentTimeMillis(),
        ChainBaseManager.getChainBaseManager());

    BlockCapsule.BlockId sid = ChainBaseManager.getChainBaseManager().getSolidBlockId();

    Random gen = new Random();
    byte[] randomHash = new byte[Sha256Hash.LENGTH];
    gen.nextBytes(randomHash);

    Protocol.HelloMessage.BlockId sBlockId = Protocol.HelloMessage.BlockId.newBuilder()
        .setHash(ByteString.copyFrom(randomHash))
        .setNumber(sid.getNum())
        .build();
    builder.setSolidBlockId(sBlockId);
    try {
      HelloMessage helloMessage = new HelloMessage(builder.build().toByteArray());
      method.invoke(p2pEventHandler, peer, helloMessage.getSendBytes());
    } catch (Exception e) {
      Assert.fail();
    }

    // peer's solidityBlock <= my solidityBlock, but not contained
    // and my lowestBlockNum > peer's solidityBlock  => i am light, LIGHT_NODE_SYNC_FAIL
    ChainBaseManager.getChainBaseManager().setLowestBlockNum(2);
    try {
      HelloMessage helloMessage = new HelloMessage(builder.build().toByteArray());
      method.invoke(p2pEventHandler, peer, helloMessage.getSendBytes());
    } catch (Exception e) {
      Assert.fail();
    }
  }

  @Test
  public void testProcessHelloMessage() {
    InetSocketAddress a1 = new InetSocketAddress("127.0.0.1", 10001);
    Channel c1 = mock(Channel.class);
    Mockito.when(c1.getInetSocketAddress()).thenReturn(a1);
    Mockito.when(c1.getInetAddress()).thenReturn(a1.getAddress());
    PeerManager.add(ctx, c1);
    PeerConnection p = PeerManager.getPeers().get(0);

    try {
      Node node = new Node(NetUtil.getNodeId(), a1.getAddress().getHostAddress(),
          null, a1.getPort());
      Protocol.HelloMessage.Builder builder =
          getHelloMessageBuilder(node, System.currentTimeMillis(),
              ChainBaseManager.getChainBaseManager());
      BlockCapsule.BlockId hid = ChainBaseManager.getChainBaseManager().getHeadBlockId();
      Protocol.HelloMessage.BlockId invalidBlockId = Protocol.HelloMessage.BlockId.newBuilder()
          .setHash(ByteString.copyFrom(new byte[31]))
          .setNumber(hid.getNum())
          .build();
      builder.setHeadBlockId(invalidBlockId);

      HelloMessage helloMessage = new HelloMessage(builder.build().toByteArray());
      HandshakeService handshakeService = new HandshakeService();
      handshakeService.processHelloMessage(p, helloMessage);
    } catch (Exception e) {
      Assert.fail();
    }
  }

  @Test
  public void testInvalidHelloLogsHashLengths() throws Exception {
    int largeHashLength = Parameter.MAX_MESSAGE_LENGTH - 1024;
    Endpoint validEndpoint = endpoint("127.0.0.1", "", 18888);
    for (int[] lengths : new int[][] {
        {32, 32, 32}, {0, 32, 32}, {32, 31, 32}, {32, 32, 33}, {0, 31, 33},
        {largeHashLength, 32, 32}, {32, largeHashLength, 32}, {32, 32, largeHashLength}}) {
      // The oversized signature also exercises logging when all three hashes are 32 bytes.
      assertInvalidHelloLog(lengths[0], lengths[1], lengths[2], validEndpoint, 201, true);
    }
  }

  @Test
  public void testInvalidHelloLogsEndpointValidity() throws Exception {
    for (Endpoint invalidEndpoint : new Endpoint[] {
        endpoint("127.0.0.1", "", 0), endpoint("127.0.0.1", "", 65536),
        Endpoint.getDefaultInstance(), endpoint("", "", 18888),
        endpoint("hello.invalid", "", 18888), endpoint("127.0.0.1", "2001:db8:::1", 18888),
        endpoint("127.0.0.1\nforged", "", 18888),
        endpoint(StringUtils.repeat('a', Parameter.MAX_MESSAGE_LENGTH - 1024), "", 18888),
        endpoint("127.0.0.1", StringUtils.repeat('a', 201), 18888)}) {
      // Keep all other HELLO fields valid so the endpoint alone causes rejection.
      assertInvalidHelloLog(32, 32, 32, invalidEndpoint, 0, false);
    }
    assertInvalidHelloLog(32, 32, 32, endpoint("", "2001:db8::1", 18888), 201, true);
  }

  @Test(timeout = 5000)
  public void testInvalidHelloLogsNodeIdLengths() throws Exception {
    for (int length : new int[] {0, 63, 65}) {
      Endpoint invalidEndpoint = endpoint("127.0.0.1", "", 18888).toBuilder()
          .setNodeId(ByteString.copyFrom(new byte[length])).build();
      assertInvalidHelloLog(32, 32, 32, invalidEndpoint, 0, false);
    }
  }

  private void assertInvalidHelloLog(int genesisLength, int solidLength, int headLength,
      Endpoint endpoint, int signatureLength, boolean expectedEndpointValid) throws Exception {
    Protocol.HelloMessage proto = Protocol.HelloMessage.newBuilder()
        .setFrom(endpoint)
        .setGenesisBlockId(blockId(genesisLength, (byte) 0x11))
        .setSolidBlockId(blockId(solidLength, (byte) 0x22))
        .setHeadBlockId(blockId(headLength, (byte) 0x33))
        .setSignature(ByteString.copyFrom(new byte[signatureLength]))
        .build();
    Assert.assertTrue(proto.getSerializedSize() + 1 < Parameter.MAX_MESSAGE_LENGTH);
    HelloMessage hello = Mockito.spy(new HelloMessage(proto.toByteArray()));
    Mockito.doThrow(new AssertionError("Invalid HELLO reached Node construction"))
        .when(hello).getFrom();
    Assert.assertEquals(expectedEndpointValid, hello.validEndPoint());
    Assert.assertFalse(hello.valid());

    PeerConnection testPeer = mock(PeerConnection.class);
    when(testPeer.getChannel()).thenReturn(mock(Channel.class));
    when(testPeer.getInetSocketAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 18888));
    P2pService p2p = mock(P2pService.class);
    Logger logger = (Logger) LoggerFactory.getLogger("net");
    Level originalLevel = logger.getLevel();
    boolean originalAdditive = logger.isAdditive();
    List<Appender<ILoggingEvent>> originalAppenders = new ArrayList<>();
    Iterator<Appender<ILoggingEvent>> iterator = logger.iteratorForAppenders();
    while (iterator.hasNext()) {
      originalAppenders.add(iterator.next());
    }
    ListAppender<ILoggingEvent> sink = new ListAppender<>();
    // The shared application context can also log from background threads.
    sink.list = new CopyOnWriteArrayList<>();
    sink.setContext(logger.getLoggerContext());
    sink.start();
    try (MockedStatic<TronNetService> netService = Mockito.mockStatic(TronNetService.class)) {
      netService.when(TronNetService::getP2pService).thenReturn(p2p);
      for (Appender<ILoggingEvent> appender : originalAppenders) {
        logger.detachAppender(appender);
      }
      logger.setAdditive(false);
      logger.setLevel(Level.WARN);
      logger.addAppender(sink);

      new HandshakeService().processHelloMessage(testPeer, hello);

      verify(hello, never()).getFrom();
      Mockito.verifyNoInteractions(p2p);
      verify(testPeer).disconnect(ReasonCode.INCOMPATIBLE_PROTOCOL);
      verify(testPeer, never()).setHelloMessageReceive(any());
      verify(testPeer, never()).onConnect();
      List<ILoggingEvent> warnings = sink.list.stream()
          .filter(event -> event.getMessage()
              .startsWith("Peer {} invalid hello message parameters"))
          .collect(Collectors.toList());
      Assert.assertEquals(1, warnings.size());
      ILoggingEvent event = warnings.get(0);
      Assert.assertEquals(Level.WARN, event.getLevel());
      String text = event.getFormattedMessage();
      Assert.assertTrue("Invalid HELLO warning must remain bounded", text.length() < 512);
      Assert.assertEquals("Peer /127.0.0.1:18888 invalid hello message parameters, "
          + "genesisHashLength: " + genesisLength + ", solidHashLength: " + solidLength
          + ", headHashLength: " + headLength + ", address: 0, sig: " + signatureLength
          + ", codeVersion: 0, endpointValid: " + expectedEndpointValid, text);
    } finally {
      logger.detachAppender(sink);
      sink.stop();
      for (Appender<ILoggingEvent> appender : originalAppenders) {
        logger.addAppender(appender);
      }
      logger.setLevel(originalLevel);
      logger.setAdditive(originalAdditive);
    }
  }

  private static Endpoint endpoint(String ipv4, String ipv6, int port) {
    return Endpoint.newBuilder().setNodeId(ByteString.copyFrom(new byte[64]))
        .setAddress(ByteString.copyFromUtf8(ipv4)).setAddressIpv6(ByteString.copyFromUtf8(ipv6))
        .setPort(port).build();
  }

  private static Protocol.HelloMessage.BlockId blockId(int length, byte value) {
    byte[] hash = new byte[length];
    Arrays.fill(hash, value);
    return Protocol.HelloMessage.BlockId.newBuilder().setHash(ByteString.copyFrom(hash)).build();
  }

  private Protocol.HelloMessage.Builder getHelloMessageBuilder(Node from, long timestamp,
      ChainBaseManager chainBaseManager) {
    Endpoint fromEndpoint = getEndpointFromNode(from);

    BlockCapsule.BlockId gid = chainBaseManager.getGenesisBlockId();
    Protocol.HelloMessage.BlockId gBlockId = Protocol.HelloMessage.BlockId.newBuilder()
        .setHash(gid.getByteString())
        .setNumber(gid.getNum())
        .build();

    BlockCapsule.BlockId sid = chainBaseManager.getSolidBlockId();
    Protocol.HelloMessage.BlockId sBlockId = Protocol.HelloMessage.BlockId.newBuilder()
        .setHash(sid.getByteString())
        .setNumber(sid.getNum())
        .build();

    BlockCapsule.BlockId hid = chainBaseManager.getHeadBlockId();
    Protocol.HelloMessage.BlockId hBlockId = Protocol.HelloMessage.BlockId.newBuilder()
        .setHash(hid.getByteString())
        .setNumber(hid.getNum())
        .build();
    Builder builder = Protocol.HelloMessage.newBuilder();
    builder.setFrom(fromEndpoint);
    builder.setVersion(Args.getInstance().getNodeP2pVersion());
    builder.setTimestamp(timestamp);
    builder.setGenesisBlockId(gBlockId);
    builder.setSolidBlockId(sBlockId);
    builder.setHeadBlockId(hBlockId);
    builder.setNodeType(chainBaseManager.getNodeType().getType());
    builder.setLowestBlockNum(chainBaseManager.isLiteNode()
        ? chainBaseManager.getLowestBlockNum() : 0);

    return builder;
  }

  private Protocol.HelloMessage.Builder getTestHelloMessageBuilder() {
    InetSocketAddress a1 = new InetSocketAddress("127.0.0.1", 10001);
    Node node = new Node(NetUtil.getNodeId(), a1.getAddress().getHostAddress(), null, a1.getPort());
    Protocol.HelloMessage.Builder builder =
        getHelloMessageBuilder(node, System.currentTimeMillis(),
            ChainBaseManager.getChainBaseManager());
    return builder;
  }
}
