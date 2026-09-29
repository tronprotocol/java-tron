package org.tron.core.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.google.protobuf.ByteString;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.Base58;
import org.tron.common.utils.StringUtil;
import org.tron.consensus.base.Param;
import org.tron.consensus.base.PbftInterface;
import org.tron.consensus.pbft.message.PbftMessage;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.handshake.HelloMessage;
import org.tron.core.net.messagehandler.PbftMsgHandler;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.core.net.service.handshake.HandshakeService;
import org.tron.core.net.service.statistics.PeerStatistics;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pService;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Discover;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.PBFTMessage;
import org.tron.protos.Protocol.ReasonCode;

public class Base58MessageLoggingTest {

  private final Logger logger = (Logger) LoggerFactory.getLogger("net");
  private final List<String> logMessages = new ArrayList<>();
  private Level oldLevel;
  private P2pConfig oldConfig;
  private PbftInterface oldPbftInterface;
  private AppenderBase<ILoggingEvent> appender;

  @Before
  public void setUp() {
    oldLevel = logger.getLevel();
    logger.setLevel(Level.INFO);
    appender = new AppenderBase<ILoggingEvent>() {
      @Override
      protected void append(ILoggingEvent event) {
        // Force formatting inside the receive path, as a normal logging appender does.
        logMessages.add(event.getFormattedMessage());
      }
    };
    appender.start();
    logger.addAppender(appender);
    oldConfig = Parameter.p2pConfig;
    Parameter.p2pConfig = new P2pConfig();
    Parameter.p2pConfig.setIp("127.0.0.1");
    oldPbftInterface = Param.getInstance().getPbftInterface();
    Param.getInstance().setPbftInterface(mock(PbftInterface.class));
  }

  @After
  public void tearDown() {
    logger.detachAppender(appender);
    appender.stop();
    logger.setLevel(oldLevel);
    Parameter.p2pConfig = oldConfig;
    Param.getInstance().setPbftInterface(oldPbftInterface);
  }

  @Test
  public void helloReceiveLogCannotEncodeOversizedAddressBeforeHandshakeRejection()
      throws Exception {
    for (Level level : new Level[]{Level.INFO, Level.OFF}) {
      logger.setLevel(level);
      logMessages.clear();
      PeerConnection peer = peer();
      P2pEventHandlerImpl handler = new P2pEventHandlerImpl();
      setField(handler, "handshakeService", new HandshakeService());
      Method process = P2pEventHandlerImpl.class.getDeclaredMethod("processMessage",
          PeerConnection.class, byte[].class);
      process.setAccessible(true);
      HelloMessage message = hello(16384);
      try (MockedStatic<TronNetService> network = mockStatic(TronNetService.class);
           MockedStatic<Base58> base58 = mockStatic(Base58.class)) {
        network.when(TronNetService::getP2pService).thenReturn(mock(P2pService.class));
        process.invoke(handler, peer, message.getSendBytes());
        verify(peer).disconnect(ReasonCode.INCOMPATIBLE_PROTOCOL);
        base58.verifyNoInteractions();
      }
      if (level == Level.INFO) {
        assertTrue(logMessages.stream().anyMatch(s -> s.contains(",len=16384)")));
      }
    }
  }

  @Test
  public void helloFormattingPreservesSmallInputsAndExistingLengthValidation() throws Exception {
    HelloMessage empty = hello(0);
    assertTrue(empty.valid());
    assertFalse(empty.toString().contains("address:"));
    HelloMessage normal = hello(21);
    assertTrue(normal.valid());
    assertTrue(normal.toString().contains(StringUtil.encode58Check(bytes(21).toByteArray())));
    HelloMessage unusual = hello(200);
    assertTrue(unusual.valid());
    assertTrue(unusual.toString().contains(",len=200)"));
    assertFalse(hello(201).valid());
  }

  @Test
  public void pbftSignatureFailureLogIsSafeAndDisabledPbftStillReturnsEarly() throws Exception {
    PbftMessage message = pbft(Protocol.SRL.newBuilder()
        .addSrAddress(bytes(16384)).build().toByteString());
    for (boolean enabled : new boolean[]{false, true}) {
      PeerConnection peer = peer();
      Channel channel = peer.getChannel();
      P2pEventHandlerImpl handler = new P2pEventHandlerImpl();
      PbftMsgHandler pbftHandler = new PbftMsgHandler();
      TronNetDelegate delegate = mock(TronNetDelegate.class);
      when(delegate.allowPBFT()).thenReturn(enabled);
      setField(pbftHandler, "tronNetDelegate", delegate);
      setField(handler, "pbftMsgHandler", pbftHandler);
      try (MockedStatic<PeerManager> peers = mockStatic(PeerManager.class);
           MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS);
           MockedStatic<Base58> base58 = mockStatic(Base58.class)) {
        peers.when(() -> PeerManager.getPeerConnection(channel)).thenReturn(peer);
        messages.when(Message::isFilter).thenReturn(false);
        logMessages.clear();
        handler.onMessage(channel, message.getSendBytes());
        if (enabled) {
          verify(peer).disconnect(ReasonCode.BAD_PROTOCOL);
          assertTrue(logMessages.stream().anyMatch(s -> s.contains(",len=16384)")));
        } else {
          verify(peer, never()).disconnect(ReasonCode.BAD_PROTOCOL);
          assertTrue(logMessages.toString(), logMessages.isEmpty());
        }
        base58.verifyNoInteractions();
      }
    }
  }

  @Test
  public void formatsSrlListAndHandlesMalformedSrl() {
    ByteString address = bytes(21);
    PbftMessage ordinary = pbft(Protocol.SRL.newBuilder()
        .addAllSrAddress(Collections.nCopies(27, address)).build().toByteString());
    assertEquals("sr list = "
        + Collections.nCopies(27, StringUtil.encode58Check(address.toByteArray())),
        ordinary.getDataString());
    assertEquals("decode error", pbft(ByteString.copyFrom(new byte[]{-1})).getDataString());
  }

  private static PeerConnection peer() {
    PeerConnection peer = mock(PeerConnection.class);
    when(peer.getPeerStatistics()).thenReturn(new PeerStatistics());
    when(peer.getChannel()).thenReturn(mock(Channel.class));
    when(peer.getInetSocketAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 18888));
    return peer;
  }

  private static HelloMessage hello(int size) throws Exception {
    Protocol.HelloMessage.BlockId block = Protocol.HelloMessage.BlockId.newBuilder()
        .setHash(ByteString.copyFrom(new byte[32])).build();
    Protocol.HelloMessage value = Protocol.HelloMessage.newBuilder()
        .setFrom(Discover.Endpoint.newBuilder().setAddress(ByteString.copyFromUtf8("127.0.0.1"))
            .setPort(18888).setNodeId(ByteString.copyFrom(new byte[64])))
        .setGenesisBlockId(block).setSolidBlockId(block).setHeadBlockId(block)
        .setAddress(bytes(size)).build();
    return new HelloMessage(value.toByteArray());
  }

  private static PbftMessage pbft(ByteString data) {
    PbftMessage message = new PbftMessage();
    PBFTMessage value = PBFTMessage.newBuilder().setRawData(PBFTMessage.Raw.newBuilder()
        .setDataType(PBFTMessage.DataType.SRL).setMsgType(PBFTMessage.MsgType.PREPREPARE)
        .setData(data)).build();
    message.setPbftMessage(value);
    message.setData(value.toByteArray());
    message.setType(MessageTypes.PBFT_MSG.asByte());
    return message;
  }

  private static ByteString bytes(int size) {
    byte[] value = new byte[size];
    Arrays.fill(value, (byte) 0x41);
    return ByteString.copyFrom(value);
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
