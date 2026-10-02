package org.tron.core.net.messagehandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.prometheus.client.CollectorRegistry;
import java.net.InetSocketAddress;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.exception.P2pException;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.adv.AdvService;
import org.tron.core.net.service.fetchblock.FetchBlockService;
import org.tron.core.net.service.relay.RelayService;
import org.tron.core.net.service.sync.SyncService;
import org.tron.core.services.WitnessProductBlockService;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol.Inventory.InventoryType;

/**
 * Focused unit tests for the tron:block_already_known counter semantics.
 *
 * <p>The counter only counts adv block responses that were matched to an outstanding
 * adv request (the request entry is consumed exactly once) AND whose exact block id
 * was already known before the response was processed. A height comparison is never
 * used, because an unknown fork block can sit below head and must not count.
 *
 * <p>Pure unit tests (no Spring context): every BlockMsgHandler dependency is mocked
 * and the counter value is read back from the default Prometheus registry as a delta,
 * so leftover increments from earlier tests in the same JVM cannot interfere.
 */
public class BlockAlreadyKnownCounterTest {

  private static final long HEAD_NUM = 100L;
  private static final String SAMPLE_NAME = MetricKeys.Counter.BLOCK_ALREADY_KNOWN + "_total";

  private BlockMsgHandler handler;
  private TronNetDelegate delegate;
  private PeerConnection peer;
  private boolean metricsEnabledBefore;

  @Before
  public void setUp() {
    metricsEnabledBefore = CommonParameter.getInstance().isMetricsPrometheusEnable();
    CommonParameter.getInstance().setMetricsPrometheusEnable(true);
    delegate = mock(TronNetDelegate.class);
    handler = new BlockMsgHandler();
    ReflectUtils.setFieldValue(handler, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(handler, "advService", mock(AdvService.class));
    ReflectUtils.setFieldValue(handler, "relayService", mock(RelayService.class));
    ReflectUtils.setFieldValue(handler, "syncService", mock(SyncService.class));
    ReflectUtils.setFieldValue(handler, "fetchBlockService", mock(FetchBlockService.class));
    ReflectUtils.setFieldValue(handler, "witnessProductBlockService",
        mock(WitnessProductBlockService.class));
    // production default; pinned so a leaked fast-forward flag from another test
    // class in the same JVM cannot skip the no-request validation below
    ReflectUtils.setFieldValue(handler, "fastForward", false);

    peer = new PeerConnection();
    Channel channel = mock(Channel.class);
    InetSocketAddress address = new InetSocketAddress("127.0.0.1", 18888);
    when(channel.getInetSocketAddress()).thenReturn(address);
    when(channel.getInetAddress()).thenReturn(address.getAddress());
    ReflectUtils.setFieldValue(peer, "channel", channel);
  }

  @After
  public void tearDown() {
    CommonParameter.getInstance().setMetricsPrometheusEnable(metricsEnabledBefore);
  }

  @Test
  public void testMatchedRequestAlreadyKnownBlockIncrements() throws P2pException {
    when(delegate.containBlock(any(BlockId.class))).thenReturn(true);
    when(delegate.validBlock(any(BlockCapsule.class))).thenReturn(true);
    when(delegate.getHeadBlockId()).thenReturn(new BlockId(Sha256Hash.ZERO_HASH, HEAD_NUM));

    BlockMessage msg = newBlockMessage(1);
    double before = sample();
    request(msg);
    handler.processMessage(peer, msg);
    assertEquals(before + 1, sample(), 0.0);

    // each matched delivery of an already-known id counts exactly once
    double beforeSecond = sample();
    request(msg);
    handler.processMessage(peer, msg);
    assertEquals(beforeSecond + 1, sample(), 0.0);
  }

  @Test
  public void testMatchedRequestUnknownForkBelowHeadNotIncremented() throws P2pException {
    // An unknown fork below head must not count. Only the block's own id is stubbed
    // "unknown" while its parent is "known", so processing passes the unlink guard and
    // reaches the low-height branch (num < head) without ever delegating: under the old
    // height-based implementation that branch counted, so this test fails on it.
    BlockMessage msg = newBlockMessage(1);
    assertNotEquals(msg.getBlockId(), msg.getBlockCapsule().getParentBlockId());
    when(delegate.containBlock(msg.getBlockId())).thenReturn(false);
    when(delegate.containBlock(msg.getBlockCapsule().getParentBlockId())).thenReturn(true);
    when(delegate.validBlock(any(BlockCapsule.class))).thenReturn(true);
    when(delegate.getHeadBlockId()).thenReturn(new BlockId(Sha256Hash.ZERO_HASH, HEAD_NUM));

    double before = sample();
    request(msg);
    handler.processMessage(peer, msg);
    assertEquals(before, sample(), 0.0);
    // the response-time exact-id sample ran and found the block unknown
    verify(delegate).containBlock(msg.getBlockId());
    // the unlink guard really did check the parent before the low-height branch ...
    verify(delegate).containBlock(msg.getBlockCapsule().getParentBlockId());
    // ... which returned without delegating to block processing
    verify(delegate, never()).processBlock(any(BlockCapsule.class), anyBoolean());
  }

  @Test
  public void testNoRequestKnownBlockNotIncremented() throws Exception {
    when(delegate.containBlock(any(BlockId.class))).thenReturn(true);

    double before = sample();
    try {
      handler.processMessage(peer, newBlockMessage(1));
      fail("expected P2pException for a block with no matching request");
    } catch (P2pException e) {
      assertEquals("no request", e.getMessage());
    }
    assertEquals(before, sample(), 0.0);
  }

  @Test
  public void testMatchedRequestHeadDuplicateIncrements() throws P2pException {
    // re-delivery of the current head block id counts: the id is exactly known even
    // though it is not below head.
    when(delegate.containBlock(any(BlockId.class))).thenReturn(true);
    when(delegate.validBlock(any(BlockCapsule.class))).thenReturn(true);
    when(delegate.getHeadBlockId()).thenReturn(new BlockId(Sha256Hash.ZERO_HASH, HEAD_NUM));
    when(delegate.getActivePeer()).thenReturn(Collections.emptyList());

    BlockMessage msg = newBlockMessage(HEAD_NUM);
    double before = sample();
    request(msg);
    handler.processMessage(peer, msg);
    assertEquals(before + 1, sample(), 0.0);
  }

  private BlockMessage newBlockMessage(long number) {
    BlockCapsule capsule = new BlockCapsule(number, Sha256Hash.ZERO_HASH,
        System.currentTimeMillis() - 60_000L, Sha256Hash.ZERO_HASH.getByteString());
    return new BlockMessage(capsule);
  }

  private void request(BlockMessage msg) {
    peer.getAdvInvRequest()
        .put(new Item(msg.getBlockId(), InventoryType.BLOCK), System.currentTimeMillis());
  }

  private double sample() {
    Double value = CollectorRegistry.defaultRegistry.getSampleValue(SAMPLE_NAME);
    return value == null ? 0 : value;
  }
}
