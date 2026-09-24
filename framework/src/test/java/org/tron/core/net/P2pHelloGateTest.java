package org.tron.core.net;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.tron.common.utils.Sha256Hash;
import org.tron.consensus.pbft.message.PbftMessage;
import org.tron.core.ChainBaseManager;
import org.tron.core.ChainBaseManager.NodeType;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.PbftMessageFactory;
import org.tron.core.net.message.TronMessageFactory;
import org.tron.core.net.message.adv.InventoryMessage;
import org.tron.core.net.message.base.DisconnectMessage;
import org.tron.core.net.message.handshake.HelloMessage;
import org.tron.core.net.message.keepalive.PingMessage;
import org.tron.core.net.messagehandler.BlockMsgHandler;
import org.tron.core.net.messagehandler.ChainInventoryMsgHandler;
import org.tron.core.net.messagehandler.FetchInvDataMsgHandler;
import org.tron.core.net.messagehandler.InventoryMsgHandler;
import org.tron.core.net.messagehandler.PbftDataSyncHandler;
import org.tron.core.net.messagehandler.PbftMsgHandler;
import org.tron.core.net.messagehandler.SyncBlockChainMsgHandler;
import org.tron.core.net.messagehandler.TransactionsMsgHandler;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.core.net.service.handshake.HandshakeService;
import org.tron.core.net.service.keepalive.KeepAliveService;
import org.tron.core.net.service.statistics.NodeStatistics;
import org.tron.core.net.service.statistics.PeerStatistics;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.discover.Node;
import org.tron.protos.Protocol.Inventory.InventoryType;
import org.tron.protos.Protocol.ReasonCode;

public class P2pHelloGateTest {

  @InjectMocks
  private P2pEventHandlerImpl handler;
  @Mock
  private HandshakeService handshakeService;
  @Mock
  private KeepAliveService keepAliveService;
  @Mock
  private SyncBlockChainMsgHandler syncBlockChainMsgHandler;
  @Mock
  private ChainInventoryMsgHandler chainInventoryMsgHandler;
  @Mock
  private InventoryMsgHandler inventoryMsgHandler;
  @Mock
  private FetchInvDataMsgHandler fetchInvDataMsgHandler;
  @Mock
  private BlockMsgHandler blockMsgHandler;
  @Mock
  private TransactionsMsgHandler transactionsMsgHandler;
  @Mock
  private PbftDataSyncHandler pbftDataSyncHandler;
  @Mock
  private PbftMsgHandler pbftMsgHandler;
  @Mock
  private PeerConnection peer;
  @Mock
  private Channel channel;

  private AutoCloseable mocks;
  private MockedStatic<PeerManager> peerManager;

  @Before
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    peerManager = Mockito.mockStatic(PeerManager.class);
    peerManager.when(() -> PeerManager.getPeerConnection(channel)).thenReturn(peer);
  }

  @After
  public void tearDown() throws Exception {
    peerManager.close();
    mocks.close();
  }

  @Test
  public void testRejectAllOtherMessageTypesBeforeParsing() {
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
    verify(peer, never()).getPeerStatistics();
    verifyNoInteractions(handshakeService, keepAliveService, syncBlockChainMsgHandler,
        chainInventoryMsgHandler, inventoryMsgHandler, fetchInvDataMsgHandler, blockMsgHandler,
        transactionsMsgHandler, pbftDataSyncHandler, pbftMsgHandler);
  }

  @Test
  public void testEmptyFramesAreRejected() {
    handler.onMessage(channel, new byte[0]);
    handler.onMessage(channel, null);
    verify(peer, times(2)).disconnect(ReasonCode.BAD_PROTOCOL);
  }

  @Test
  public void testUnknownPeerIsIgnored() {
    peerManager.when(() -> PeerManager.getPeerConnection(channel)).thenReturn(null);
    handler.onMessage(channel, new PingMessage().getSendBytes());
    verifyNoInteractions(peer, keepAliveService);
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

  @Test
  public void testKeepAliveIsDispatchedAfterHello() throws Exception {
    when(peer.getHelloMessageReceive()).thenReturn(mock(HelloMessage.class));
    when(peer.getPeerStatistics()).thenReturn(new PeerStatistics());

    handler.onMessage(channel, new PingMessage().getSendBytes());

    verify(keepAliveService).processMessage(eq(peer), any(PingMessage.class));
    verify(peer, never()).disconnect(any());
  }

  @Test
  public void testInventoryIsDispatchedAfterHello() throws Exception {
    when(peer.getHelloMessageReceive()).thenReturn(mock(HelloMessage.class));
    when(peer.getPeerStatistics()).thenReturn(new PeerStatistics());
    InventoryMessage inventory = new InventoryMessage(
        Collections.singletonList(Sha256Hash.ZERO_HASH), InventoryType.BLOCK);

    handler.onMessage(channel, inventory.getSendBytes());

    verify(inventoryMsgHandler).processMessage(eq(peer), any(InventoryMessage.class));
    verify(peer, never()).disconnect(any());
  }

  @Test
  public void testPbftIsDispatchedAfterHello() throws Exception {
    when(peer.getHelloMessageReceive()).thenReturn(mock(HelloMessage.class));
    byte[] data = new byte[] {MessageTypes.PBFT_MSG.asByte()};
    PbftMessage message = mock(PbftMessage.class);
    try (MockedStatic<PbftMessageFactory> factory = Mockito.mockStatic(PbftMessageFactory.class)) {
      factory.when(() -> PbftMessageFactory.create(data)).thenReturn(message);
      handler.onMessage(channel, data);
      verify(pbftMsgHandler).processMessage(peer, message);
      verify(peer, never()).disconnect(any());
    }
  }
}
