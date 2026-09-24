package org.tron.core.net.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseMethodTest;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.fetchblock.FetchBlockService;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol.Inventory.InventoryType;

public class FetchBlockServiceTest extends BaseMethodTest {

  private FetchBlockService service;
  private TronNetDelegate tronNetDelegate;

  @Override
  protected void afterInit() {
    service = context.getBean(FetchBlockService.class);
    tronNetDelegate = mock(TronNetDelegate.class);
    ReflectUtils.setFieldValue(service, "tronNetDelegate", tronNetDelegate);
  }

  /**
   * Verify that fetchBlockProcess selects the idle peer with the lowest avg latency
   * (excluding the peer we are already fetching from) and sends it a FetchInvDataMessage.
   *
   * <p>Covers the migrated getPeerLatency / shouldFetchBlock path that replaced the
   * legacy Dropwizard per-IP histogram P75 selection.
   */
  @Test
  public void testSelectLowestLatencyPeer() throws Exception {
    InetSocketAddress oldAddr = new InetSocketAddress("127.0.0.1", 10001);
    InetSocketAddress newAddr = new InetSocketAddress("127.0.0.2", 10001);

    Channel oldChannel = mock(Channel.class);
    when(oldChannel.getInetSocketAddress()).thenReturn(oldAddr);
    when(oldChannel.getInetAddress()).thenReturn(oldAddr.getAddress());
    when(oldChannel.getAvgLatency()).thenReturn(200L);
    doNothing().when(oldChannel).send(any(byte[].class));

    Channel newChannel = mock(Channel.class);
    when(newChannel.getInetSocketAddress()).thenReturn(newAddr);
    when(newChannel.getInetAddress()).thenReturn(newAddr.getAddress());
    when(newChannel.getAvgLatency()).thenReturn(50L);
    doNothing().when(newChannel).send(any(byte[].class));

    PeerConnection oldPeer = context.getBean(PeerConnection.class);
    oldPeer.setChannel(oldChannel);
    oldPeer.updateFetchLatency(200L);
    PeerConnection newPeer = context.getBean(PeerConnection.class);
    newPeer.setChannel(newChannel);
    newPeer.updateFetchLatency(50L);

    Sha256Hash hash = Sha256Hash.wrap(ByteString.copyFrom(new byte[32]));
    Item item = new Item(hash, InventoryType.BLOCK);

    // both peers advertise having the block
    oldPeer.getAdvInvReceive().put(item, System.currentTimeMillis());
    newPeer.getAdvInvReceive().put(item, System.currentTimeMillis());

    List<PeerConnection> activePeers = new ArrayList<>();
    activePeers.add(oldPeer);
    activePeers.add(newPeer);
    when(tronNetDelegate.getActivePeer()).thenReturn(activePeers);

    // seed fetchBlockInfo via reflection (private static nested class)
    Class<?> fetchBlockInfoClass = Class.forName(
        "org.tron.core.net.service.fetchblock.FetchBlockService$FetchBlockInfo");
    Constructor<?> constructor = fetchBlockInfoClass.getDeclaredConstructor(
        Sha256Hash.class, PeerConnection.class, long.class);
    constructor.setAccessible(true);
    Object fetchBlockInfo = constructor.newInstance(
        hash, oldPeer, System.currentTimeMillis());
    ReflectUtils.setFieldValue(service, "fetchBlockInfo", fetchBlockInfo);

    // invoke private fetchBlockProcess
    Method method = FetchBlockService.class.getDeclaredMethod(
        "fetchBlockProcess", fetchBlockInfoClass);
    method.setAccessible(true);
    method.invoke(service, fetchBlockInfo);

    // new peer (lowest latency) should receive the fetch request
    verify(newChannel).send(any(byte[].class));
    // old peer should not be re-requested
    verify(oldChannel, never()).send(any(byte[].class));
    // fetchBlockInfo should be cleared after successful dispatch
    Assert.assertNull(ReflectUtils.getFieldObject(service, "fetchBlockInfo"));
  }

  /**
   * A 600ms seed is clamped to 500ms, and >= timeout triggers fast-switch.
   */
  @Test
  public void testSwitchOnOldPeerTimeout() throws Exception {
    InetSocketAddress oldAddr = new InetSocketAddress("127.0.0.3", 10001);
    InetSocketAddress newAddr = new InetSocketAddress("127.0.0.4", 10001);

    Channel oldChannel = mock(Channel.class);
    when(oldChannel.getInetSocketAddress()).thenReturn(oldAddr);
    when(oldChannel.getInetAddress()).thenReturn(oldAddr.getAddress());
    // seed above timeout; estimator clamps this to default fetchBlockTimeout (500)
    when(oldChannel.getAvgLatency()).thenReturn(600L);
    doNothing().when(oldChannel).send(any(byte[].class));

    Channel newChannel = mock(Channel.class);
    when(newChannel.getInetSocketAddress()).thenReturn(newAddr);
    when(newChannel.getInetAddress()).thenReturn(newAddr.getAddress());
    when(newChannel.getAvgLatency()).thenReturn(50L);
    doNothing().when(newChannel).send(any(byte[].class));

    PeerConnection oldPeer = context.getBean(PeerConnection.class);
    ReflectUtils.setFieldValue(oldPeer, "channel", oldChannel);
    oldPeer.updateFetchLatency(600L);

    PeerConnection newPeer = context.getBean(PeerConnection.class);
    newPeer.setChannel(newChannel);
    newPeer.updateFetchLatency(50L);

    Sha256Hash hash = Sha256Hash.wrap(ByteString.copyFrom(new byte[32]));
    Item item = new Item(hash, InventoryType.BLOCK);

    newPeer.getAdvInvReceive().put(item, System.currentTimeMillis());

    List<PeerConnection> activePeers = new ArrayList<>();
    activePeers.add(oldPeer);
    activePeers.add(newPeer);
    when(tronNetDelegate.getActivePeer()).thenReturn(activePeers);

    Class<?> fetchBlockInfoClass = Class.forName(
        "org.tron.core.net.service.fetchblock.FetchBlockService$FetchBlockInfo");
    Constructor<?> constructor = fetchBlockInfoClass.getDeclaredConstructor(
        Sha256Hash.class, PeerConnection.class, long.class);
    constructor.setAccessible(true);
    Object fetchBlockInfo = constructor.newInstance(
        hash, oldPeer, System.currentTimeMillis());
    ReflectUtils.setFieldValue(service, "fetchBlockInfo", fetchBlockInfo);

    Method method = FetchBlockService.class.getDeclaredMethod(
        "fetchBlockProcess", fetchBlockInfoClass);
    method.setAccessible(true);
    method.invoke(service, fetchBlockInfo);

    verify(newChannel).send(any(byte[].class));
    Assert.assertNull(ReflectUtils.getFieldObject(service, "fetchBlockInfo"));
  }

  @Test
  public void testSwitchOnHardTimeoutWhenOldPeerUnseeded() throws Exception {
    PeerConnection oldPeer = context.getBean(PeerConnection.class);
    PeerConnection candidate = context.getBean(PeerConnection.class);
    Channel oldChannel = mock(Channel.class);
    Channel candidateChannel = mock(Channel.class);
    when(oldChannel.getAvgLatency()).thenReturn(0L);
    when(candidateChannel.getAvgLatency()).thenReturn(50L);
    ReflectUtils.setFieldValue(oldPeer, "channel", oldChannel);
    ReflectUtils.setFieldValue(candidate, "channel", candidateChannel);
    candidate.updateFetchLatency(50L);
    doNothing().when(candidateChannel).send(any(byte[].class));

    Sha256Hash hash = Sha256Hash.wrap(ByteString.copyFrom(new byte[32]));
    Item item = new Item(hash, InventoryType.BLOCK);
    candidate.getAdvInvReceive().put(item, System.currentTimeMillis());
    when(tronNetDelegate.getActivePeer()).thenReturn(Arrays.asList(oldPeer, candidate));

    Class<?> fetchBlockInfoClass = Class.forName(
        "org.tron.core.net.service.fetchblock.FetchBlockService$FetchBlockInfo");
    Constructor<?> constructor = fetchBlockInfoClass.getDeclaredConstructor(
        Sha256Hash.class, PeerConnection.class, long.class);
    constructor.setAccessible(true);
    Object fetchBlockInfo = constructor.newInstance(
        hash, oldPeer, System.currentTimeMillis() - 600);
    ReflectUtils.setFieldValue(service, "fetchBlockInfo", fetchBlockInfo);
    Method method = FetchBlockService.class.getDeclaredMethod(
        "fetchBlockProcess", fetchBlockInfoClass);
    method.setAccessible(true);
    method.invoke(service, fetchBlockInfo);

    verify(candidateChannel).send(any(byte[].class));
    Assert.assertNull(ReflectUtils.getFieldObject(service, "fetchBlockInfo"));
  }

  /**
   * Old peer unsampled: getFetchLatency() falls back to its channel avgLatency (0), while
   * the candidate's first real sample 999 clamps to 500. shouldFetchBlock: left time
   * 0 - 0 = 0, so 500 < 0 * 0.5 = 0 is false — no failover while the fetch is within timeout.
   */
  @Test
  public void testNoSwitchWhenOldPeerLatencyUnknown() throws Exception {
    InetSocketAddress oldAddr = new InetSocketAddress("127.0.0.5", 10001);
    InetSocketAddress newAddr = new InetSocketAddress("127.0.0.6", 10001);

    Channel oldChannel = mock(Channel.class);
    when(oldChannel.getInetSocketAddress()).thenReturn(oldAddr);
    when(oldChannel.getInetAddress()).thenReturn(oldAddr.getAddress());
    // old peer latency unknown
    when(oldChannel.getAvgLatency()).thenReturn(0L);
    doNothing().when(oldChannel).send(any(byte[].class));

    Channel newChannel = mock(Channel.class);
    when(newChannel.getInetSocketAddress()).thenReturn(newAddr);
    when(newChannel.getInetAddress()).thenReturn(newAddr.getAddress());
    when(newChannel.getAvgLatency()).thenReturn(50L);
    doNothing().when(newChannel).send(any(byte[].class));

    PeerConnection oldPeer = context.getBean(PeerConnection.class);
    oldPeer.setChannel(oldChannel);

    PeerConnection newPeer = context.getBean(PeerConnection.class);
    newPeer.setChannel(newChannel);
    // first real sample replaces the unsampled state, then clamps 999 to 500
    newPeer.updateFetchLatency(999L);

    Sha256Hash hash = Sha256Hash.wrap(ByteString.copyFrom(new byte[32]));
    Item item = new Item(hash, InventoryType.BLOCK);

    oldPeer.getAdvInvReceive().put(item, System.currentTimeMillis());
    newPeer.getAdvInvReceive().put(item, System.currentTimeMillis());

    List<PeerConnection> activePeers = new ArrayList<>();
    activePeers.add(oldPeer);
    activePeers.add(newPeer);
    when(tronNetDelegate.getActivePeer()).thenReturn(activePeers);

    Class<?> fetchBlockInfoClass = Class.forName(
        "org.tron.core.net.service.fetchblock.FetchBlockService$FetchBlockInfo");
    Constructor<?> constructor = fetchBlockInfoClass.getDeclaredConstructor(
        Sha256Hash.class, PeerConnection.class, long.class);
    constructor.setAccessible(true);
    Object fetchBlockInfo = constructor.newInstance(
        hash, oldPeer, System.currentTimeMillis());
    ReflectUtils.setFieldValue(service, "fetchBlockInfo", fetchBlockInfo);

    Method method = FetchBlockService.class.getDeclaredMethod(
        "fetchBlockProcess", fetchBlockInfoClass);
    method.setAccessible(true);
    method.invoke(service, fetchBlockInfo);

    // no failover: candidate peer must not receive a fetch request
    verify(newChannel, never()).send(any(byte[].class));
    // in-flight fetchBlockInfo stays pending
    Assert.assertNotNull(ReflectUtils.getFieldObject(service, "fetchBlockInfo"));
  }

  /**
   * Both peers are unsampled, so both reads fall back to their channel avgLatency
   * (old = 200, candidate = 0). The candidate wins min() and shouldFetchBlock:
   * left time 200 - 0 = 200, so 0 < 200 * 0.5 = 100 holds — failover happens.
   */
  @Test
  public void testSwitchWhenBothPeersUnseededReadsChannelFallback() throws Exception {
    InetSocketAddress oldAddr = new InetSocketAddress("127.0.0.7", 10001);
    InetSocketAddress newAddr = new InetSocketAddress("127.0.0.8", 10001);

    Channel oldChannel = mock(Channel.class);
    when(oldChannel.getInetSocketAddress()).thenReturn(oldAddr);
    when(oldChannel.getInetAddress()).thenReturn(oldAddr.getAddress());
    when(oldChannel.getAvgLatency()).thenReturn(200L);
    doNothing().when(oldChannel).send(any(byte[].class));

    Channel newChannel = mock(Channel.class);
    when(newChannel.getInetSocketAddress()).thenReturn(newAddr);
    when(newChannel.getInetAddress()).thenReturn(newAddr.getAddress());
    // candidate unsampled: read falls back to its channel avgLatency (0)
    when(newChannel.getAvgLatency()).thenReturn(0L);
    doNothing().when(newChannel).send(any(byte[].class));

    PeerConnection oldPeer = context.getBean(PeerConnection.class);
    oldPeer.setChannel(oldChannel);
    PeerConnection newPeer = context.getBean(PeerConnection.class);
    newPeer.setChannel(newChannel);

    Sha256Hash hash = Sha256Hash.wrap(ByteString.copyFrom(new byte[32]));
    Item item = new Item(hash, InventoryType.BLOCK);

    oldPeer.getAdvInvReceive().put(item, System.currentTimeMillis());
    newPeer.getAdvInvReceive().put(item, System.currentTimeMillis());

    List<PeerConnection> activePeers = new ArrayList<>();
    activePeers.add(oldPeer);
    activePeers.add(newPeer);
    when(tronNetDelegate.getActivePeer()).thenReturn(activePeers);

    Class<?> fetchBlockInfoClass = Class.forName(
        "org.tron.core.net.service.fetchblock.FetchBlockService$FetchBlockInfo");
    Constructor<?> constructor = fetchBlockInfoClass.getDeclaredConstructor(
        Sha256Hash.class, PeerConnection.class, long.class);
    constructor.setAccessible(true);
    Object fetchBlockInfo = constructor.newInstance(
        hash, oldPeer, System.currentTimeMillis());
    ReflectUtils.setFieldValue(service, "fetchBlockInfo", fetchBlockInfo);

    Method method = FetchBlockService.class.getDeclaredMethod(
        "fetchBlockProcess", fetchBlockInfoClass);
    method.setAccessible(true);
    method.invoke(service, fetchBlockInfo);

    // failover: unsampled candidate reads channel fallback 0 and wins the comparison
    verify(newChannel).send(any(byte[].class));
    Assert.assertNull(ReflectUtils.getFieldObject(service, "fetchBlockInfo"));
  }

  /**
   * Old peer seeded at 200; the unsampled candidate reads its channel fallback (0), wins
   * min() and the left-time comparison (0 < 200 * 0.5) — failover to the candidate.
   */
  @Test
  public void testSwitchToUnseededCandidateWhenOldPeerSeeded() throws Exception {
    PeerConnection oldPeer = context.getBean(PeerConnection.class);
    PeerConnection candidate = context.getBean(PeerConnection.class);
    Channel oldChannel = mock(Channel.class);
    Channel candidateChannel = mock(Channel.class);
    when(oldChannel.getAvgLatency()).thenReturn(200L);
    when(candidateChannel.getAvgLatency()).thenReturn(0L);
    ReflectUtils.setFieldValue(oldPeer, "channel", oldChannel);
    oldPeer.updateFetchLatency(200L);
    ReflectUtils.setFieldValue(candidate, "channel", candidateChannel);
    doNothing().when(candidateChannel).send(any(byte[].class));

    Sha256Hash hash = Sha256Hash.wrap(ByteString.copyFrom(new byte[32]));
    Item item = new Item(hash, InventoryType.BLOCK);
    candidate.getAdvInvReceive().put(item, System.currentTimeMillis());
    when(tronNetDelegate.getActivePeer()).thenReturn(Arrays.asList(oldPeer, candidate));

    Class<?> infoClass = Class.forName(
        "org.tron.core.net.service.fetchblock.FetchBlockService$FetchBlockInfo");
    Constructor<?> constructor = infoClass.getDeclaredConstructor(
        Sha256Hash.class, PeerConnection.class, long.class);
    constructor.setAccessible(true);
    Object info = constructor.newInstance(hash, oldPeer, System.currentTimeMillis());
    ReflectUtils.setFieldValue(service, "fetchBlockInfo", info);
    Method method = FetchBlockService.class.getDeclaredMethod("fetchBlockProcess", infoClass);
    method.setAccessible(true);
    method.invoke(service, info);

    verify(candidateChannel).send(any(byte[].class));
    Assert.assertNull(ReflectUtils.getFieldObject(service, "fetchBlockInfo"));
  }

  /**
   * The first real fetch sample directly replaces the unsampled state (no blending with
   * the channel prior, isomorphic to RFC 6298 SRTT initialization); the clamp still
   * applies, so 999 saturates to the 500ms bound instead of the old blended (123*9+999)/10.
   */
  @Test
  public void testFirstFetchLatencySampleReplacesSeed() {
    Channel channel = mock(Channel.class);
    when(channel.getAvgLatency()).thenReturn(123L);
    PeerConnection peer = new PeerConnection();
    ReflectUtils.setFieldValue(peer, "channel", channel);

    // First real sample replaces the unsampled state; channel prior (123) is not blended.
    peer.updateFetchLatency(999L);

    Assert.assertEquals(500L, peer.getFetchLatency());
  }

  @Test
  public void testFetchLatencyUsesEwma() {
    Channel channel = mock(Channel.class);
    when(channel.getAvgLatency()).thenReturn(100L);
    PeerConnection peer = new PeerConnection();
    ReflectUtils.setFieldValue(peer, "channel", channel);

    // first real sample initializes directly: clamp(100) = 100
    peer.updateFetchLatency(100L);
    // second sample onwards: EWMA alpha = 0.1 → (100 * 9 + 200) / 10 = 110
    peer.updateFetchLatency(200L);

    Assert.assertEquals(110L, peer.getFetchLatency());
  }

  /**
   * Degradation: seeded at 100 (direct replacement), then ten 500ms samples. With
   * alpha = 0.1 and integer division the estimate rises monotonically while converging
   * smoothly and never exceeds the 500ms clamp bound. Hand-computed sequence:
   * 140, 176, 208, 237, 263, 286, 307, 326, 343, 358.
   */
  @Test
  public void testFetchLatencyEwmaDegradationConverges() {
    Channel channel = mock(Channel.class);
    when(channel.getAvgLatency()).thenReturn(0L);
    PeerConnection peer = new PeerConnection();
    ReflectUtils.setFieldValue(peer, "channel", channel);

    // first real sample initializes directly: clamp(100) = 100
    peer.updateFetchLatency(100L);
    long previous = peer.getFetchLatency();
    long[] expected = {140, 176, 208, 237, 263, 286, 307, 326, 343, 358};
    for (long expectedValue : expected) {
      peer.updateFetchLatency(500L);
      long current = peer.getFetchLatency();
      Assert.assertEquals(expectedValue, current);
      Assert.assertTrue(current > previous);
      Assert.assertTrue(current <= 500L);
      previous = current;
    }
  }

  /**
   * Recovery: continuing from the degradation endpoint (358), ten 100ms samples pull the
   * estimate back down monotonically and smoothly. Hand-computed sequence:
   * 332, 308, 287, 268, 251, 235, 221, 208, 197, 187.
   */
  @Test
  public void testFetchLatencyEwmaRecoveryConverges() {
    Channel channel = mock(Channel.class);
    when(channel.getAvgLatency()).thenReturn(0L);
    PeerConnection peer = new PeerConnection();
    ReflectUtils.setFieldValue(peer, "channel", channel);

    // replay the degradation phase to reach its endpoint
    peer.updateFetchLatency(100L);
    for (int i = 0; i < 10; i++) {
      peer.updateFetchLatency(500L);
    }
    Assert.assertEquals(358L, peer.getFetchLatency());

    long previous = peer.getFetchLatency();
    long[] expected = {332, 308, 287, 268, 251, 235, 221, 208, 197, 187};
    for (long expectedValue : expected) {
      peer.updateFetchLatency(100L);
      long current = peer.getFetchLatency();
      Assert.assertEquals(expectedValue, current);
      Assert.assertTrue(current < previous);
      previous = current;
    }
  }

  @Test
  public void testFetchLatencyIsClamped() {
    Channel channel = mock(Channel.class);
    when(channel.getAvgLatency()).thenReturn(100L);
    PeerConnection peer = new PeerConnection();
    ReflectUtils.setFieldValue(peer, "channel", channel);

    // first real sample initializes directly: clamp(100) = 100
    peer.updateFetchLatency(100L);
    // EWMA: (100 * 9 + 9999) / 10 = 1089, then clamped to the 500ms bound
    peer.updateFetchLatency(9999L);

    Assert.assertEquals(500L, peer.getFetchLatency());
  }

  @Test
  public void testFetchLatencyIsIsolatedAcrossConnections() {
    Channel firstChannel = mock(Channel.class);
    when(firstChannel.getAvgLatency()).thenReturn(100L);
    PeerConnection first = new PeerConnection();
    ReflectUtils.setFieldValue(first, "channel", firstChannel);
    first.updateFetchLatency(9999L);

    Channel secondChannel = mock(Channel.class);
    when(secondChannel.getAvgLatency()).thenReturn(50L);
    PeerConnection second = new PeerConnection();
    ReflectUtils.setFieldValue(second, "channel", secondChannel);

    // the fresh connection is unsampled: it reads its own channel fallback (50),
    // not the first connection's estimate (500) and not a cross-connection value
    Assert.assertEquals(50L, second.getFetchLatency());
  }
}
