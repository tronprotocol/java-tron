package org.tron.core.net;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.ChainBaseManager;
import org.tron.core.ChainBaseManager.NodeType;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.PbftMessageFactory;
import org.tron.core.net.message.TronMessageFactory;
import org.tron.core.net.message.base.DisconnectMessage;
import org.tron.core.net.message.handshake.HelloMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.core.net.service.handshake.HandshakeService;
import org.tron.core.net.service.statistics.NodeStatistics;
import org.tron.core.net.service.statistics.PeerStatistics;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.discover.Node;
import org.tron.protos.Protocol.ReasonCode;

public class P2pHelloGateTest {

  private final P2pEventHandlerImpl handler = new P2pEventHandlerImpl();
  private final Channel channel = mock(Channel.class);
  private final PeerConnection peer = mock(PeerConnection.class);
  private final HandshakeService handshakeService = mock(HandshakeService.class);
  private MockedStatic<PeerManager> peerManager;

  @Before
  public void setUp() {
    ReflectUtils.setFieldValue(handler, "handshakeService", handshakeService);
    peerManager = Mockito.mockStatic(PeerManager.class);
    peerManager.when(() -> PeerManager.getPeerConnection(channel)).thenReturn(peer);
  }

  @After
  public void tearDown() {
    peerManager.close();
  }

  @Test
  public void testRejectMessagesBeforeHelloWithoutParsing() {
    try (MockedStatic<TronMessageFactory> tronFactory =
             Mockito.mockStatic(TronMessageFactory.class);
         MockedStatic<PbftMessageFactory> pbftFactory =
             Mockito.mockStatic(PbftMessageFactory.class)) {
      int rejected = 0;
      for (MessageTypes type : MessageTypes.values()) {
        if (type != MessageTypes.P2P_HELLO && type != MessageTypes.P2P_DISCONNECT) {
          handler.onMessage(channel, new byte[] {type.asByte()});
          rejected++;
        }
      }
      verify(peer, times(rejected)).disconnect(ReasonCode.BAD_PROTOCOL);
      tronFactory.verifyNoInteractions();
      pbftFactory.verifyNoInteractions();
    }
  }

  @Test
  public void testRejectEmptyFrames() {
    handler.onMessage(channel, new byte[0]);
    handler.onMessage(channel, null);
    verify(peer, times(2)).disconnect(ReasonCode.BAD_PROTOCOL);
  }

  @Test
  public void testHelloIsDispatchedBeforeHandshake() throws Exception {
    ChainBaseManager chain = mock(ChainBaseManager.class);
    when(chain.getGenesisBlockId()).thenReturn(new BlockId());
    when(chain.getSolidBlockId()).thenReturn(new BlockId());
    when(chain.getHeadBlockId()).thenReturn(new BlockId());
    when(chain.getNodeType()).thenReturn(NodeType.FULL);
    HelloMessage hello = new HelloMessage(
        new Node(new byte[64], "127.0.0.1", null, 18888), 0, chain);
    when(peer.getPeerStatistics()).thenReturn(new PeerStatistics());

    handler.onMessage(channel, hello.getSendBytes());

    verify(handshakeService).processHelloMessage(eq(peer), any(HelloMessage.class));
    verify(peer, never()).disconnect(any());
  }

  @Test
  public void testDisconnectIsDispatchedBeforeHandshake() {
    NodeStatistics stats = mock(NodeStatistics.class);
    when(peer.getPeerStatistics()).thenReturn(new PeerStatistics());
    when(peer.getP2pRateLimiter()).thenReturn(new P2pRateLimiter());
    when(peer.getChannel()).thenReturn(channel);
    when(peer.getNodeStatistics()).thenReturn(stats);

    handler.onMessage(channel, new DisconnectMessage(ReasonCode.PEER_QUITING).getSendBytes());

    verify(channel).close();
    verify(stats).nodeDisconnectedRemote(ReasonCode.PEER_QUITING);
    verify(peer, never()).disconnect(any());
  }
}
