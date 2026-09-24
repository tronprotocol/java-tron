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
    localHead = blockId(100, 2);
    when(chain.getGenesisBlockId()).thenReturn(genesis);
    when(chain.getSolidBlockId()).thenReturn(blockId(80, 3));
    when(chain.getHeadBlockId()).thenReturn(localHead);
    when(chain.getHeadBlockNum()).thenReturn(100L);
    when(chain.getLowestBlockNum()).thenReturn(10L);
    when(chain.getNodeType()).thenReturn(NodeType.FULL);
    when(chain.containBlockInMainChain(genesis)).thenReturn(true);
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
  public void testRejectUnknownHeadAtLocalHeight() {
    BlockId unknown = blockId(100, 4);
    HelloMessage hello = hello(unknown);
    Assert.assertTrue(hello.valid());

    service.processHelloMessage(peer, hello);

    verify(chain).containBlockInMainChain(unknown);
    assertRejected(ReasonCode.FORKED);
  }

  @Test
  public void testAcceptMainChainHeadAtLocalHeight() {
    HelloMessage hello = hello(localHead);

    service.processHelloMessage(peer, hello);

    verify(chain).containBlockInMainChain(localHead);
    assertAccepted(hello);
  }

  @Test
  public void testAcceptOlderMainChainHead() {
    BlockId older = blockId(90, 5);
    when(chain.containBlockInMainChain(older)).thenReturn(true);
    HelloMessage hello = hello(older);

    service.processHelloMessage(peer, hello);

    verify(chain).containBlockInMainChain(older);
    assertAccepted(hello);
  }

  @Test
  public void testRejectForkHeadEvenWhenSolidBlockMatches() {
    BlockId forkHead = blockId(90, 6);
    HelloMessage hello = hello(forkHead);
    // A common solid block does not make a different, locally verifiable head acceptable.
    service.processHelloMessage(peer, hello);

    verify(chain).containBlockInMainChain(genesis);
    verify(chain).containBlockInMainChain(forkHead);
    assertRejected(ReasonCode.FORKED);
  }

  @Test
  public void testCheckHeadAtLowestRetainedHeight() {
    BlockId boundary = blockId(10, 7);

    service.processHelloMessage(peer, hello(boundary));

    verify(chain).containBlockInMainChain(boundary);
    assertRejected(ReasonCode.FORKED);
  }

  @Test
  public void testAcceptKnownHeadAtLowestRetainedHeight() {
    BlockId boundary = blockId(10, 7);
    when(chain.containBlockInMainChain(boundary)).thenReturn(true);
    HelloMessage hello = hello(boundary);

    service.processHelloMessage(peer, hello);

    verify(chain).containBlockInMainChain(boundary);
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
  public void testPrunedHeadContinuesToSync() {
    BlockId pruned = blockId(9, 9);
    HelloMessage hello = hello(pruned);

    service.processHelloMessage(peer, hello);

    verify(chain, never()).containBlockInMainChain(pruned);
    assertAccepted(hello);
  }

  @Test
  public void testFullNodeChecksUnknownHeadAtHeightZero() {
    when(chain.getLowestBlockNum()).thenReturn(0L);
    BlockId unknown = blockId(0, 10);

    service.processHelloMessage(peer, hello(unknown));

    verify(chain).containBlockInMainChain(unknown);
    assertRejected(ReasonCode.FORKED);
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
  public void testSolidForkIsRejectedBeforeHeadCheck() {
    BlockId differentSolid = blockId(80, 11);
    HelloMessage hello = hello(localHead);
    hello.setHelloMessage(hello.getInstance().toBuilder()
        .setSolidBlockId(protoBlockId(differentSolid)).build());

    service.processHelloMessage(peer, hello);

    verify(chain, never()).containBlockInMainChain(localHead);
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
        .setSolidBlockId(protoBlockId(genesis))
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
