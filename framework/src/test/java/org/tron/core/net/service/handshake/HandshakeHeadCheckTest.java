package org.tron.core.net.service.handshake;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.ChainBaseManager.NodeType;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.net.TronNetService;
import org.tron.core.net.message.handshake.HelloMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.core.net.service.effective.EffectiveCheckService;
import org.tron.core.net.service.relay.RelayService;
import org.tron.p2p.P2pService;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.discover.Node;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.ReasonCode;

public class HandshakeHeadCheckTest {

  private HandshakeService service;
  private ChainBaseManager chain;
  private PeerConnection peer;
  private Channel channel;
  private EffectiveCheckService effectiveCheckService;
  private MockedStatic<TronNetService> netService;
  private MockedStatic<PeerManager> peerManager;
  private BlockId genesis;
  private BlockId solid;
  private BlockId localHead;

  @Before
  public void setUp() {
    service = new HandshakeService();
    chain = mock(ChainBaseManager.class);
    peer = mock(PeerConnection.class);
    channel = mock(Channel.class);
    effectiveCheckService = mock(EffectiveCheckService.class);
    RelayService relayService = mock(RelayService.class);
    ReflectUtils.setFieldValue(service, "chainBaseManager", chain);
    ReflectUtils.setFieldValue(service, "relayService", relayService);
    ReflectUtils.setFieldValue(service, "effectiveCheckService", effectiveCheckService);
    when(peer.getChannel()).thenReturn(channel);
    when(peer.getInetSocketAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 18888));
    when(relayService.checkHelloMessage(any(), any())).thenReturn(true);

    genesis = blockId(0, 1);
    solid = blockId(80, 3);
    localHead = blockId(100, 2);
    when(chain.getGenesisBlockId()).thenReturn(genesis);
    when(chain.getSolidBlockId()).thenReturn(solid);
    when(chain.getHeadBlockId()).thenReturn(localHead);
    when(chain.getHeadBlockNum()).thenReturn(100L);
    when(chain.getLowestBlockNum()).thenReturn(10L);
    when(chain.getNodeType()).thenReturn(NodeType.FULL);
    when(chain.containBlockInMainChain(genesis)).thenReturn(true);
    when(chain.containBlockInMainChain(solid)).thenReturn(true);
    when(chain.containBlockInMainChain(localHead)).thenReturn(true);

    P2pService p2p = mock(P2pService.class);
    netService = Mockito.mockStatic(TronNetService.class);
    netService.when(TronNetService::getP2pService).thenReturn(p2p);
    peerManager = Mockito.mockStatic(PeerManager.class);
  }

  @After
  public void tearDown() {
    peerManager.close();
    netService.close();
  }

  @Test
  public void testAcceptUnknownHeadAtLocalHeight() {
    BlockId unknown = blockId(100, 4);
    HelloMessage hello = hello(unknown);
    Assert.assertTrue(hello.valid());
    Assert.assertEquals(chain.getSolidBlockId(), hello.getSolidBlockId());
    Assert.assertNotEquals(localHead, unknown);
    Assert.assertFalse(chain.containBlockInMainChain(unknown));

    service.processHelloMessage(peer, hello);

    assertAccepted(hello);
  }

  @Test
  public void testAcceptCachedForkHeadAtLocalHeight() {
    BlockId forkHead = blockId(100, 12);
    when(chain.containBlock(forkHead)).thenReturn(true);
    Assert.assertTrue(chain.containBlock(forkHead));
    Assert.assertFalse(chain.containBlockInMainChain(forkHead));
    HelloMessage hello = hello(forkHead);

    service.processHelloMessage(peer, hello);

    assertAccepted(hello);
  }

  @Test
  public void testAcceptMainChainHeadAtLocalHeight() {
    HelloMessage hello = hello(localHead);

    service.processHelloMessage(peer, hello);

    assertAccepted(hello);
  }

  @Test
  public void testAcceptOlderMainChainHead() {
    BlockId older = blockId(90, 5);
    when(chain.containBlockInMainChain(older)).thenReturn(true);
    HelloMessage hello = hello(older);

    service.processHelloMessage(peer, hello);

    assertAccepted(hello);
  }

  @Test
  public void testAcceptForkHeadWhenSolidBlockMatches() {
    BlockId forkHead = blockId(90, 6);
    HelloMessage hello = hello(forkHead);
    Assert.assertEquals(chain.getSolidBlockId(), hello.getSolidBlockId());
    // Keep the connection available so a short-lived fork can converge through sync/broadcast.
    service.processHelloMessage(peer, hello);

    verify(chain).containBlockInMainChain(solid);
    assertAccepted(hello);
  }

  @Test
  public void testAcceptOlderForkHeadBelowLocalSolid() {
    when(chain.getSolidBlockId()).thenReturn(blockId(95, 13));
    HelloMessage hello = hello(blockId(90, 7));
    Assert.assertTrue(hello.getSolidBlockId().getNum() < hello.getHeadBlockId().getNum());
    Assert.assertTrue(hello.getHeadBlockId().getNum() < chain.getSolidBlockId().getNum());

    service.processHelloMessage(peer, hello);

    verify(chain).containBlockInMainChain(solid);
    assertAccepted(hello);
  }

  @Test
  public void testAcceptKnownHeadAtLowestRetainedHeight() {
    when(chain.getLowestBlockNum()).thenReturn(solid.getNum());
    HelloMessage hello = hello(solid);

    service.processHelloMessage(peer, hello);

    verify(chain).containBlockInMainChain(solid);
    assertAccepted(hello);
  }

  @Test
  public void testHigherHeadContinuesToSync() {
    BlockId higher = blockId(101, 8);
    HelloMessage hello = hello(higher);

    service.processHelloMessage(peer, hello);

    verify(chain, never()).containBlockInMainChain(higher);
    assertAccepted(hello);
  }

  @Test
  public void testUnverifiableSolidBelowRetainedHistoryIsRejected() {
    BlockId prunedSolid = blockId(9, 9);
    HelloMessage hello = hello(blockId(90, 14));
    hello.setHelloMessage(hello.getInstance().toBuilder()
        .setSolidBlockId(protoBlockId(prunedSolid)).build());

    service.processHelloMessage(peer, hello);

    verify(chain).containBlockInMainChain(prunedSolid);
    assertRejected(ReasonCode.LIGHT_NODE_SYNC_FAIL);
  }

  @Test
  public void testGenesisMismatchIsStillRejected() {
    HelloMessage hello = hello(localHead);
    hello.setHelloMessage(hello.getInstance().toBuilder()
        .setGenesisBlockId(protoBlockId(blockId(0, 10))).build());

    service.processHelloMessage(peer, hello);

    assertRejected(ReasonCode.INCOMPATIBLE_CHAIN);
  }

  @Test
  public void testVersionMismatchIsStillRejected() {
    HelloMessage hello = hello(localHead);
    hello.setHelloMessage(hello.getInstance().toBuilder()
        .setVersion(hello.getVersion() + 1).build());

    service.processHelloMessage(peer, hello);

    assertRejected(ReasonCode.INCOMPATIBLE_VERSION);
  }

  @Test
  public void testEffectiveCheckStillRejectsOlderKnownHead() {
    BlockId older = blockId(90, 5);
    when(chain.containBlockInMainChain(older)).thenReturn(true);
    InetSocketAddress address = peer.getInetSocketAddress();
    when(effectiveCheckService.getCur()).thenReturn(address);

    service.processHelloMessage(peer, hello(older));

    assertRejected(ReasonCode.BELOW_THAN_ME);
  }

  @Test
  public void testSolidForkIsStillRejected() {
    BlockId differentSolid = blockId(80, 11);
    HelloMessage hello = hello(localHead);
    hello.setHelloMessage(hello.getInstance().toBuilder()
        .setSolidBlockId(protoBlockId(differentSolid)).build());

    service.processHelloMessage(peer, hello);

    verify(chain).containBlockInMainChain(differentSolid);
    assertRejected(ReasonCode.FORKED);
  }

  @Test
  public void testDuplicateHelloIsStillRejected() {
    HelloMessage hello = hello(localHead);
    when(peer.getHelloMessageReceive()).thenReturn(hello);

    service.processHelloMessage(peer, hello);

    assertRejected(ReasonCode.BAD_PROTOCOL);
  }

  private HelloMessage hello(BlockId head) {
    HelloMessage hello = new HelloMessage(
        new Node(new byte[64], "127.0.0.1", null, 18888), 0, chain);
    hello.setHelloMessage(hello.getInstance().toBuilder()
        .setSolidBlockId(protoBlockId(solid))
        .setHeadBlockId(protoBlockId(head)).build());
    return hello;
  }

  private static BlockId blockId(long number, int seed) {
    return new BlockId(Sha256Hash.of(true, new byte[] {(byte) seed}), number);
  }

  private static Protocol.HelloMessage.BlockId protoBlockId(BlockId id) {
    return Protocol.HelloMessage.BlockId.newBuilder()
        .setNumber(id.getNum()).setHash(id.getByteString()).build();
  }

  private void assertAccepted(HelloMessage hello) {
    verify(peer).setHelloMessageReceive(hello);
    verify(peer).onConnect();
    verify(peer, never()).disconnect(any());
    peerManager.verify(PeerManager::sortPeers);
  }

  private void assertRejected(ReasonCode reason) {
    verify(peer).disconnect(reason);
    verify(peer, never()).setHelloMessageReceive(any());
    verify(peer, never()).onConnect();
    peerManager.verifyNoInteractions();
  }
}
