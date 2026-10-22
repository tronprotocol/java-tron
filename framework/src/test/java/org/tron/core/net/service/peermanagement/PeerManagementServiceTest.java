package org.tron.core.net.service.peermanagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.InetAddresses;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.CommonStore;
import org.tron.core.exception.jsonrpc.JsonRpcInternalException;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.net.peer.ActivePeerInfo;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pService;
import org.tron.p2p.connection.Channel;

public class PeerManagementServiceTest {

  private static final byte[] DB_KEY_BLOCKED_IPS =
      "blocked-ips".getBytes(StandardCharsets.UTF_8);

  private static final long BLOCKED_AT_MILLIS = 1_600_000_000_000L;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private CommonStore commonStore;
  private P2pService p2pService;
  private PeerManagementService service;
  private boolean originalDynamicConfigEnable;

  @Before
  public void setUp() throws Exception {
    originalDynamicConfigEnable = Args.getInstance().isDynamicConfigEnable();
    Args.getInstance().setDynamicConfigEnable(false);
    commonStore = Mockito.mock(CommonStore.class);
    p2pService = Mockito.mock(P2pService.class);
    service = new PeerManagementService();
    setCommonStore(service);
    Mockito.when(commonStore.get(AdditionalMatchers.aryEq(DB_KEY_BLOCKED_IPS)))
        .thenReturn(new BytesCapsule(null));
  }

  @After
  public void tearDown() {
    Args.getInstance().setDynamicConfigEnable(originalDynamicConfigEnable);
  }

  @Test
  public void configureUsesEmptyBlockedIpsWhenKeyDoesNotExist() {
    P2pConfig config = new P2pConfig();

    service.configure(config);

    Assert.assertEquals(Collections.emptyList(), listedIps());
    Assert.assertEquals(Collections.emptySet(), config.getBlockedIps());
    Mockito.verify(commonStore).get(AdditionalMatchers.aryEq(DB_KEY_BLOCKED_IPS));
    Mockito.verify(commonStore, Mockito.never()).has(Mockito.any(byte[].class));
  }

  @Test
  public void initRejectsNullP2pService() {
    try {
      service.init(null);
      Assert.fail("Expected a null P2P service to be rejected");
    } catch (NullPointerException e) {
      Assert.assertEquals("p2pService must not be null", e.getMessage());
    }

    Mockito.verifyNoInteractions(commonStore);
  }

  @Test
  public void configureNormalizesAndSortsBlockedIps() throws Exception {
    P2pConfig config = new P2pConfig();
    byte[] storedValue = OBJECT_MAPPER.writeValueAsBytes(Arrays.asList(
        new BlockedIpInfo("2001:db8::2", BLOCKED_AT_MILLIS + 20),
        new BlockedIpInfo("192.0.2.2", BLOCKED_AT_MILLIS)));
    Mockito.when(commonStore.get(AdditionalMatchers.aryEq(
        DB_KEY_BLOCKED_IPS)))
        .thenReturn(new BytesCapsule(storedValue));

    service.configure(config);

    Assert.assertEquals(Arrays.asList("192.0.2.2", "2001:db8:0:0:0:0:0:2"),
        listedIps());
    Assert.assertEquals(BLOCKED_AT_MILLIS, service.listBlockedIps().get(0).getBlockedAtMillis());
    Assert.assertEquals(BLOCKED_AT_MILLIS + 20,
        service.listBlockedIps().get(1).getBlockedAtMillis());
    Assert.assertEquals(2, config.getBlockedIps().size());
    Assert.assertTrue(config.getBlockedIps().contains(InetAddress.getByName("192.0.2.2")));
    Assert.assertTrue(config.getBlockedIps().contains(InetAddress.getByName("2001:db8::2")));
  }

  @Test
  public void configureRetainsInvalidBlockedIpsAndContinuesWithEmptySnapshot() {
    P2pConfig config = new P2pConfig();
    Mockito.when(commonStore.get(AdditionalMatchers.aryEq(
        DB_KEY_BLOCKED_IPS)))
        .thenReturn(new BytesCapsule("not-json".getBytes(StandardCharsets.UTF_8)));

    service.configure(config);

    Assert.assertEquals(Collections.emptyList(), listedIps());
    Assert.assertEquals(Collections.emptySet(), config.getBlockedIps());
    Mockito.verify(commonStore, Mockito.never()).delete(Mockito.any());
    Mockito.verify(commonStore, Mockito.never()).put(Mockito.any(), Mockito.any());
  }

  @Test
  public void blockIpOverwritesInvalidStoredValueAfterStartup() throws Exception {
    P2pConfig config = new P2pConfig();
    Mockito.when(commonStore.get(AdditionalMatchers.aryEq(
        DB_KEY_BLOCKED_IPS)))
        .thenReturn(new BytesCapsule("[null]".getBytes(StandardCharsets.UTF_8)));

    service.configure(config);
    Assert.assertEquals(Collections.emptyList(), listedIps());
    Assert.assertEquals(Collections.emptySet(), config.getBlockedIps());
    Mockito.verify(commonStore, Mockito.never()).delete(Mockito.any());
    Mockito.verify(commonStore, Mockito.never()).put(Mockito.any(), Mockito.any());

    service.init(p2pService);
    PeerOperationResult result = service.blockIp("192.0.2.20");

    Assert.assertTrue(result.isSuccess());
    Assert.assertEquals(Collections.singletonList("192.0.2.20"), listedIps());
    ArgumentCaptor<BytesCapsule> stored = ArgumentCaptor.forClass(BytesCapsule.class);
    Mockito.verify(commonStore).put(AdditionalMatchers.aryEq(DB_KEY_BLOCKED_IPS),
        stored.capture());
    JsonNode entries = OBJECT_MAPPER.readTree(stored.getValue().getData());
    Assert.assertEquals(1, entries.size());
    Assert.assertEquals("192.0.2.20", entries.get(0).get("ip").asText());
    Mockito.verify(commonStore, Mockito.never()).delete(Mockito.any());
  }

  @Test
  public void configurePropagatesDatabaseReadFailureWithoutDeletingKey() {
    P2pConfig config = new P2pConfig();
    Mockito.when(commonStore.get(AdditionalMatchers.aryEq(DB_KEY_BLOCKED_IPS)))
        .thenThrow(new IllegalStateException("database unavailable"));

    try {
      service.configure(config);
      Assert.fail("Expected a CommonStore read failure to stop initialization");
    } catch (IllegalStateException e) {
      Assert.assertEquals("database unavailable", e.getMessage());
    }

    Mockito.verify(commonStore, Mockito.never()).delete(Mockito.any());
  }

  @Test
  public void configureFailureResetsReadyState() throws Exception {
    service.configure(new P2pConfig());
    service.init(p2pService);
    Mockito.when(commonStore.get(AdditionalMatchers.aryEq(DB_KEY_BLOCKED_IPS)))
        .thenThrow(new IllegalStateException("database unavailable"));

    try {
      service.configure(new P2pConfig());
      Assert.fail("Expected a CommonStore read failure");
    } catch (IllegalStateException expected) {
      Assert.assertEquals("database unavailable", expected.getMessage());
    }

    try {
      service.disconnectPeer("192.0.2.20:18888");
      Assert.fail("Expected failed configuration to leave the service unavailable");
    } catch (JsonRpcInternalException expected) {
      Assert.assertEquals("P2P service is not ready", expected.getMessage());
    }
  }

  @Test
  public void configureRetainsBlockedIpListThatExceedsLimit() throws Exception {
    stubStoredBlockedIps(buildBlockedIpJson(PeerManagementService.MAX_BLOCKED_IPS + 1));
    P2pConfig config = new P2pConfig();

    service.configure(config);

    Assert.assertEquals(Collections.emptyList(), listedIps());
    Assert.assertEquals(Collections.emptySet(), config.getBlockedIps());
    Mockito.verify(commonStore, Mockito.never()).delete(Mockito.any());
  }

  @Test
  public void addPeerAddsOnlyActiveNodeAndIsIdempotent() throws Exception {
    P2pConfig config = configureAndInit();
    InetAddress trustedAddress = InetAddress.getByName("192.0.2.10");
    config.getTrustNodes().add(trustedAddress);
    Mockito.when(p2pService.addActiveNode(Mockito.any(InetSocketAddress.class)))
        .thenReturn(true, false);

    PeerOperationResult first = service.addPeer("192.0.2.20:18888");
    PeerOperationResult second = service.addPeer("192.0.2.20:18888");

    Assert.assertTrue(first.isSuccess());
    Assert.assertTrue(first.isChanged());
    Assert.assertTrue(second.isSuccess());
    Assert.assertFalse(second.isChanged());
    Assert.assertEquals(Collections.singletonList(trustedAddress), config.getTrustNodes());
    Mockito.verify(p2pService, Mockito.times(2)).addActiveNode(
        new InetSocketAddress(InetAddress.getByName("192.0.2.20"), 18888));
  }

  @Test
  public void addPeerRejectsManuallyBlockedIpBeforeCallingLibp2p() throws Exception {
    stubStoredBlockedIps(OBJECT_MAPPER.writeValueAsString(
        Collections.singletonList(new BlockedIpInfo("192.0.2.20", BLOCKED_AT_MILLIS))));
    configureAndInit();

    PeerOperationResult result = service.addPeer("192.0.2.20:18888");

    Assert.assertFalse(result.isSuccess());
    Assert.assertFalse(result.isChanged());
    Assert.assertEquals(0, result.getDisconnectedCount());
    Assert.assertEquals("Cannot add active node because IP 192.0.2.20 is manually blocked",
        result.getErrorMessage());
    Assert.assertEquals(Collections.singletonList("192.0.2.20"), listedIps());
    Assert.assertEquals(BLOCKED_AT_MILLIS, service.listBlockedIps().get(0).getBlockedAtMillis());
    Mockito.verifyNoInteractions(p2pService);
    Mockito.verify(commonStore, Mockito.never()).put(Mockito.any(), Mockito.any());
  }

  @Test
  public void dynamicConfigRejectsAddAndRemoveWithoutParsingOrSideEffects() throws Exception {
    Args.getInstance().setDynamicConfigEnable(true);

    PeerOperationResult addResult = service.addPeer("not-an-endpoint");
    PeerOperationResult removeResult = service.removePeer("not-an-endpoint");

    Assert.assertFalse(addResult.isSuccess());
    Assert.assertFalse(addResult.isChanged());
    Assert.assertEquals(0, addResult.getDisconnectedCount());
    Assert.assertTrue(addResult.getErrorMessage().contains("node.dynamicConfig.enable"));
    Assert.assertFalse(removeResult.isSuccess());
    Assert.assertEquals(addResult.getErrorMessage(), removeResult.getErrorMessage());
    Mockito.verifyNoInteractions(p2pService);
  }

  @Test
  public void removePeerRemovesActiveNodeAndDisconnectsEndpoint() throws Exception {
    configureAndInit();
    Mockito.when(p2pService.removeActiveNode(Mockito.any(InetSocketAddress.class)))
        .thenReturn(true);
    Mockito.when(p2pService.disconnect(Mockito.any(InetSocketAddress.class))).thenReturn(1);

    PeerOperationResult result = service.removePeer("[2001:db8::20]:18888");

    Assert.assertTrue(result.isSuccess());
    Assert.assertTrue(result.isChanged());
    Assert.assertEquals(1, result.getDisconnectedCount());
    Mockito.verify(p2pService).removeActiveNode(
        new InetSocketAddress(InetAddress.getByName("2001:db8::20"), 18888));
    Mockito.verify(p2pService).disconnect(
        new InetSocketAddress(InetAddress.getByName("2001:db8::20"), 18888));
  }

  @Test
  public void disconnectPeerDoesNotModifyActiveNodes() throws Exception {
    configureAndInit();
    Mockito.when(p2pService.disconnect(Mockito.any(InetSocketAddress.class))).thenReturn(1);

    PeerOperationResult result = service.disconnectPeer("192.0.2.20:18888");

    Assert.assertTrue(result.isSuccess());
    Assert.assertFalse(result.isChanged());
    Assert.assertEquals(1, result.getDisconnectedCount());
    Mockito.verify(p2pService, Mockito.never()).removeActiveNode(Mockito.any());
  }

  @Test
  public void peerOperationsRejectDomainsAndUnbracketedIpv6() throws Exception {
    assertInvalidEndpoint("peer.example:18888");
    assertInvalidEndpoint("2001:db8::20:18888");
    assertInvalidEndpoint("192.0.2.20:0");
    assertInvalidEndpoint("192.0.2.20:+18888");
    assertInvalidEndpoint("[192.0.2.20]:18888");
  }

  @Test
  public void blockIpRejectsEndpointDomainAndOverlongInput() throws Exception {
    configureAndInit();
    assertInvalidIp("192.0.2.20:18888");
    assertInvalidIp("peer.example");
    assertInvalidIp("1111111111111111111111111111111111111111111111");
    Mockito.verify(commonStore, Mockito.never()).put(Mockito.any(), Mockito.any());
    Mockito.verifyNoInteractions(p2pService);
  }

  @Test
  public void blockAndUnblockIpTrimSurroundingWhitespace() throws Exception {
    configureAndInit();
    InetAddress address = InetAddresses.forString("192.0.2.20");

    Assert.assertTrue(service.blockIp(" 192.0.2.20 ").isChanged());
    Assert.assertEquals(Collections.singletonList("192.0.2.20"), listedIps());
    Mockito.verify(p2pService).replaceBlockedIps(Collections.singleton(address));

    Assert.assertTrue(service.unblockIp("\t192.0.2.20 ").isChanged());
    Assert.assertEquals(Collections.emptyList(), listedIps());
    Mockito.verify(p2pService).replaceBlockedIps(Collections.emptySet());
  }

  @Test
  public void scopedIpv6CannotChangeExistingBlock() throws Exception {
    stubStoredBlockedIps(OBJECT_MAPPER.writeValueAsString(
        Collections.singletonList(new BlockedIpInfo("fe80::1", BLOCKED_AT_MILLIS))));
    configureAndInit();

    for (String scopedIp : Arrays.asList("fe80::1%eth0", "fe80::1%1", "fe80::1%2")) {
      assertInvalidIp(scopedIp);
      try {
        service.unblockIp(scopedIp);
        Assert.fail("Expected invalid IP: " + scopedIp);
      } catch (JsonRpcInvalidParamsException expected) {
        Assert.assertNotNull(expected.getMessage());
      }
    }

    Assert.assertEquals(Collections.singletonList("fe80:0:0:0:0:0:0:1"), listedIps());
    Assert.assertEquals(BLOCKED_AT_MILLIS, service.listBlockedIps().get(0).getBlockedAtMillis());
    Mockito.verifyNoInteractions(p2pService);
    Mockito.verify(commonStore, Mockito.never()).put(Mockito.any(), Mockito.any());
  }

  @Test
  public void blockIpRejectsOnlyNewEntriesWhenLimitIsReached() throws Exception {
    stubStoredBlockedIps(buildBlockedIpJson(PeerManagementService.MAX_BLOCKED_IPS));
    configureAndInit();
    JsonNode original = OBJECT_MAPPER.valueToTree(service.listBlockedIps());

    PeerOperationResult rejected = service.blockIp("192.0.2.20");
    PeerOperationResult repeated = service.blockIp("2001:db8::1");

    Assert.assertFalse(rejected.isSuccess());
    Assert.assertFalse(rejected.isChanged());
    Assert.assertEquals(0, rejected.getDisconnectedCount());
    Assert.assertEquals("Blocked IP limit of " + PeerManagementService.MAX_BLOCKED_IPS
        + " has been reached", rejected.getErrorMessage());
    Assert.assertTrue(repeated.isSuccess());
    Assert.assertFalse(repeated.isChanged());
    Assert.assertEquals(0, repeated.getDisconnectedCount());
    Assert.assertEquals("", repeated.getErrorMessage());
    Assert.assertEquals(original, OBJECT_MAPPER.valueToTree(service.listBlockedIps()));
    Mockito.verify(commonStore, Mockito.never()).put(Mockito.any(), Mockito.any());
    Mockito.verifyNoInteractions(p2pService);
  }

  @Test
  public void blockIpPersistsBeforeApplyingAndPublishesSnapshot() throws Exception {
    configureAndInit();
    Mockito.when(p2pService.replaceBlockedIps(Mockito.anySet())).thenReturn(2);

    long before = System.currentTimeMillis();
    PeerOperationResult result = service.blockIp("2001:db8::20");
    long after = System.currentTimeMillis();

    Assert.assertTrue(result.isSuccess());
    Assert.assertTrue(result.isChanged());
    Assert.assertEquals(2, result.getDisconnectedCount());
    Assert.assertEquals("", result.getErrorMessage());
    Assert.assertEquals(Collections.singletonList("2001:db8:0:0:0:0:0:20"),
        listedIps());

    ArgumentCaptor<BytesCapsule> capsuleCaptor = ArgumentCaptor.forClass(BytesCapsule.class);
    InOrder inOrder = Mockito.inOrder(p2pService, commonStore);
    inOrder.verify(commonStore).put(AdditionalMatchers.aryEq(
        DB_KEY_BLOCKED_IPS), capsuleCaptor.capture());
    inOrder.verify(p2pService).replaceBlockedIps(Mockito.argThat(
        addresses -> addresses.contains(InetAddresses.forString("2001:db8::20"))));
    JsonNode stored = new ObjectMapper().readTree(capsuleCaptor.getValue().getData());
    Assert.assertEquals("2001:db8:0:0:0:0:0:20", stored.get(0).get("ip").asText());
    Assert.assertTrue(stored.get(0).get("blockedAtMillis").isIntegralNumber());
    long blockedAtMillis = stored.get(0).get("blockedAtMillis").asLong();
    Assert.assertTrue(blockedAtMillis >= before);
    Assert.assertTrue(blockedAtMillis <= after);
    Assert.assertEquals(stored, OBJECT_MAPPER.valueToTree(service.listBlockedIps()));
  }

  @Test
  public void repeatedBlockAndUnblockAreIdempotent() throws Exception {
    stubStoredBlockedIps(OBJECT_MAPPER.writeValueAsString(
        Collections.singletonList(new BlockedIpInfo("192.0.2.20", BLOCKED_AT_MILLIS))));
    configureAndInit();

    PeerOperationResult blockResult = service.blockIp("192.0.2.20");
    Assert.assertEquals(BLOCKED_AT_MILLIS, service.listBlockedIps().get(0).getBlockedAtMillis());
    PeerOperationResult firstUnblock = service.unblockIp("192.0.2.20");
    PeerOperationResult secondUnblock = service.unblockIp("192.0.2.20");

    Assert.assertFalse(blockResult.isChanged());
    Assert.assertTrue(firstUnblock.isChanged());
    Assert.assertFalse(secondUnblock.isChanged());
    Assert.assertEquals("", blockResult.getErrorMessage());
    Assert.assertEquals("", firstUnblock.getErrorMessage());
    Assert.assertEquals("", secondUnblock.getErrorMessage());
    Assert.assertEquals(Collections.emptyList(), listedIps());
    Mockito.verify(commonStore, Mockito.times(1)).put(Mockito.any(), Mockito.any());
    Mockito.verify(p2pService, Mockito.times(1)).replaceBlockedIps(Collections.emptySet());
  }

  @Test
  public void blockDatabaseFailureLeavesJavaSnapshotUnchangedAndAllowsRetry() throws Exception {
    configureAndInit();
    Mockito.doThrow(new IllegalStateException("write failed"))
        .doNothing()
        .when(commonStore).put(Mockito.any(), Mockito.any());

    try {
      service.blockIp("192.0.2.20");
      Assert.fail("Expected persistence failure");
    } catch (JsonRpcInternalException e) {
      Assert.assertEquals("Failed to persist blocked IP snapshot", e.getMessage());
    }
    Assert.assertEquals(Collections.emptyList(), listedIps());
    Mockito.verifyNoInteractions(p2pService);

    long before = System.currentTimeMillis();
    PeerOperationResult retryResult = service.blockIp("192.0.2.20");
    long after = System.currentTimeMillis();

    Assert.assertTrue(retryResult.isSuccess());
    Assert.assertTrue(retryResult.isChanged());
    Assert.assertEquals(Collections.singletonList("192.0.2.20"), listedIps());
    long blockedAtMillis = service.listBlockedIps().get(0).getBlockedAtMillis();
    Assert.assertTrue(blockedAtMillis >= before);
    Assert.assertTrue(blockedAtMillis <= after);
    Mockito.verify(p2pService, Mockito.times(1)).replaceBlockedIps(Mockito.argThat(
        addresses -> addresses.contains(InetAddresses.forString("192.0.2.20"))));
    ArgumentCaptor<BytesCapsule> stored = ArgumentCaptor.forClass(BytesCapsule.class);
    Mockito.verify(commonStore, Mockito.times(2))
        .put(AdditionalMatchers.aryEq(DB_KEY_BLOCKED_IPS), stored.capture());
    Assert.assertEquals(OBJECT_MAPPER.readTree(stored.getValue().getData()),
        OBJECT_MAPPER.valueToTree(service.listBlockedIps()));
  }

  @Test
  public void unblockDatabaseFailureKeepsJavaSnapshotAndAllowsRetry() throws Exception {
    stubStoredBlockedIps(OBJECT_MAPPER.writeValueAsString(
        Collections.singletonList(new BlockedIpInfo("192.0.2.20", BLOCKED_AT_MILLIS))));
    configureAndInit();
    Mockito.doThrow(new IllegalStateException("write failed"))
        .doNothing()
        .when(commonStore).put(Mockito.any(), Mockito.any());

    try {
      service.unblockIp("192.0.2.20");
      Assert.fail("Expected persistence failure");
    } catch (JsonRpcInternalException e) {
      Assert.assertEquals("Failed to persist blocked IP snapshot", e.getMessage());
    }
    Assert.assertEquals(Collections.singletonList("192.0.2.20"),
        listedIps());
    Assert.assertEquals(BLOCKED_AT_MILLIS, service.listBlockedIps().get(0).getBlockedAtMillis());
    Mockito.verifyNoInteractions(p2pService);

    PeerOperationResult retryResult = service.unblockIp("192.0.2.20");

    Assert.assertTrue(retryResult.isSuccess());
    Assert.assertTrue(retryResult.isChanged());
    Assert.assertEquals(Collections.emptyList(), listedIps());
    Mockito.verify(p2pService, Mockito.times(1)).replaceBlockedIps(Collections.emptySet());
    Mockito.verify(commonStore, Mockito.times(2)).put(Mockito.any(), Mockito.any());
  }

  @Test
  public void libp2pFailureAfterPersistDoesNotPublishJavaSnapshot() throws Exception {
    configureAndInit();
    Mockito.doThrow(new IllegalStateException("apply failed")).when(p2pService)
        .replaceBlockedIps(Mockito.anySet());

    try {
      service.blockIp("192.0.2.20");
      Assert.fail("Expected libp2p update failure");
    } catch (JsonRpcInternalException e) {
      Assert.assertEquals("Failed to apply blocked IP snapshot", e.getMessage());
    }
    Mockito.verify(commonStore).put(AdditionalMatchers.aryEq(DB_KEY_BLOCKED_IPS),
        Mockito.any());
    Assert.assertEquals(Collections.emptyList(), listedIps());
  }

  @Test(timeout = 5_000)
  public void concurrentBlockRequestsPublishACompleteCombinedSnapshot() throws Exception {
    configureAndInit();
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch start = new CountDownLatch(1);
    try {
      Future<PeerOperationResult> first = executor.submit(() -> {
        start.await();
        return service.blockIp("192.0.2.20");
      });
      Future<PeerOperationResult> second = executor.submit(() -> {
        start.await();
        return service.blockIp("192.0.2.21");
      });

      long before = System.currentTimeMillis();
      start.countDown();
      Assert.assertTrue(first.get().isChanged());
      Assert.assertTrue(second.get().isChanged());
      long after = System.currentTimeMillis();
      Assert.assertEquals(Arrays.asList("192.0.2.20", "192.0.2.21"),
          listedIps());

      ArgumentCaptor<BytesCapsule> values = ArgumentCaptor.forClass(BytesCapsule.class);
      Mockito.verify(commonStore, Mockito.times(2))
          .put(AdditionalMatchers.aryEq(DB_KEY_BLOCKED_IPS), values.capture());
      List<BytesCapsule> storedValues = values.getAllValues();
      JsonNode finalStoredValue = new ObjectMapper()
          .readTree(storedValues.get(storedValues.size() - 1).getData());
      Assert.assertEquals(2, finalStoredValue.size());
      Assert.assertEquals(finalStoredValue, OBJECT_MAPPER.valueToTree(service.listBlockedIps()));
      for (BlockedIpInfo entry : service.listBlockedIps()) {
        Assert.assertTrue(entry.getBlockedAtMillis() >= before);
        Assert.assertTrue(entry.getBlockedAtMillis() <= after);
      }
      Mockito.verify(p2pService, Mockito.times(2)).replaceBlockedIps(Mockito.anySet());
    } finally {
      executor.shutdownNow();
      executor.awaitTermination(1, TimeUnit.SECONDS);
    }
  }

  @Test
  public void writeOperationsFailClearlyBeforeP2pIsReady() throws Exception {
    service.configure(new P2pConfig());

    try {
      service.disconnectPeer("192.0.2.20:18888");
      Assert.fail("Expected the operation to require a ready P2P service");
    } catch (JsonRpcInternalException e) {
      Assert.assertEquals("P2P service is not ready", e.getMessage());
    }
  }

  @Test
  public void closeMakesWriteOperationsUnavailable() throws Exception {
    configureAndInit();

    service.close();

    try {
      service.disconnectPeer("192.0.2.20:18888");
      Assert.fail("Expected the closed service to reject write operations");
    } catch (JsonRpcInternalException e) {
      Assert.assertEquals("P2P service is not ready", e.getMessage());
    }
    Mockito.verifyNoInteractions(p2pService);
  }

  @Test
  public void listActivePeersUsesStructuredPeerSnapshotsAndCurrentCounts() {
    PeerConnection activePeer = mockPeer(true, true, "192.0.2.20:18888", 42L);
    PeerConnection passivePeer = mockPeer(false, false, "192.0.2.21:18888", 84L);

    try (MockedStatic<PeerManager> peerManager = Mockito.mockStatic(PeerManager.class)) {
      peerManager.when(PeerManager::getPeers).thenReturn(Arrays.asList(activePeer, passivePeer));

      ActivePeerListResult result = service.listActivePeers();

      Assert.assertEquals(2, result.getAllCount());
      Assert.assertEquals(1, result.getActiveCount());
      Assert.assertEquals(1, result.getPassiveCount());
      Assert.assertEquals(1, result.getEffectiveCount());
      Assert.assertEquals(2, result.getPeers().size());
      Assert.assertEquals("192.0.2.20:18888", result.getPeers().get(0).getRemoteAddress());
      Assert.assertEquals(42L, result.getPeers().get(0).getAverageLatencyMillis());
      JsonNode serialized = OBJECT_MAPPER.valueToTree(result);
      Assert.assertFalse(serialized.has("validCount"));
      Assert.assertEquals(1, serialized.path("effectiveCount").asInt());
    }
  }

  @Test
  public void listActivePeersHandlesEmptyPeerSet() {
    try (MockedStatic<PeerManager> peerManager = Mockito.mockStatic(PeerManager.class)) {
      peerManager.when(PeerManager::getPeers).thenReturn(Collections.emptyList());

      ActivePeerListResult result = service.listActivePeers();

      Assert.assertEquals(0, result.getAllCount());
      Assert.assertEquals(0, result.getActiveCount());
      Assert.assertEquals(0, result.getPassiveCount());
      Assert.assertEquals(0, result.getEffectiveCount());
      Assert.assertEquals(Collections.emptyList(), result.getPeers());
    }
  }

  @Test
  public void listActivePeersCountsEffectivePeersFromReturnedSnapshots() {
    PeerConnection peer = mockPeer(true, false, "192.0.2.20:18888", 42L);
    // The live state can differ from the snapshot returned to the caller.
    Mockito.when(peer.isSyncFinish()).thenReturn(true);
    Mockito.when(peer.getActivePeerInfo().isNeedSyncFromPeer()).thenReturn(false);
    Mockito.when(peer.getActivePeerInfo().isNeedSyncFromUs()).thenReturn(true);

    try (MockedStatic<PeerManager> peerManager = Mockito.mockStatic(PeerManager.class)) {
      peerManager.when(PeerManager::getPeers).thenReturn(Collections.singletonList(peer));

      ActivePeerListResult result = service.listActivePeers();

      Assert.assertEquals(0, result.getEffectiveCount());
      Assert.assertTrue(result.getPeers().get(0).isNeedSyncFromUs());
    }
  }

  @Test
  public void repeatedBlockPreservesCreationTimeAndOldQueryResults() throws Exception {
    stubStoredBlockedIps(OBJECT_MAPPER.writeValueAsString(
        Collections.singletonList(new BlockedIpInfo("2001:db8::20", BLOCKED_AT_MILLIS))));
    configureAndInit();
    List<BlockedIpInfo> original = service.listBlockedIps();

    PeerOperationResult repeated = service.blockIp("2001:db8:0:0:0:0:0:20");

    Assert.assertTrue(repeated.isSuccess());
    Assert.assertFalse(repeated.isChanged());
    Assert.assertEquals(BLOCKED_AT_MILLIS, service.listBlockedIps().get(0).getBlockedAtMillis());
    Mockito.verify(commonStore, Mockito.never()).put(Mockito.any(), Mockito.any());
    Mockito.verify(p2pService, Mockito.never()).replaceBlockedIps(Mockito.anySet());

    service.unblockIp("2001:db8::20");
    long before = System.currentTimeMillis();
    service.blockIp("2001:db8::20");
    long after = System.currentTimeMillis();

    long blockedAtMillis = service.listBlockedIps().get(0).getBlockedAtMillis();
    Assert.assertTrue(blockedAtMillis >= before);
    Assert.assertTrue(blockedAtMillis <= after);
    Assert.assertNotEquals(BLOCKED_AT_MILLIS, blockedAtMillis);
    Assert.assertEquals(BLOCKED_AT_MILLIS, original.get(0).getBlockedAtMillis());
    original.clear();
    Assert.assertEquals(1, service.listBlockedIps().size());
  }

  @Test
  public void restartRestoresPersistedCreationTime() throws Exception {
    stubStoredBlockedIps(OBJECT_MAPPER.writeValueAsString(
        Collections.singletonList(new BlockedIpInfo("192.0.2.20", BLOCKED_AT_MILLIS))));
    configureAndInit();
    service.blockIp("192.0.2.21");
    ArgumentCaptor<BytesCapsule> stored = ArgumentCaptor.forClass(BytesCapsule.class);
    Mockito.verify(commonStore).put(AdditionalMatchers.aryEq(DB_KEY_BLOCKED_IPS), stored.capture());
    Mockito.when(commonStore.get(AdditionalMatchers.aryEq(DB_KEY_BLOCKED_IPS)))
        .thenReturn(new BytesCapsule(stored.getValue().getData()));
    PeerManagementService restarted = new PeerManagementService();
    setCommonStore(restarted);
    P2pConfig config = new P2pConfig();

    restarted.configure(config);
    restarted.init(p2pService);

    Assert.assertEquals(2, restarted.listBlockedIps().size());
    Assert.assertEquals("192.0.2.20", restarted.listBlockedIps().get(0).getIp());
    Assert.assertEquals(BLOCKED_AT_MILLIS, restarted.listBlockedIps().get(0).getBlockedAtMillis());
    Assert.assertEquals(OBJECT_MAPPER.readTree(stored.getValue().getData()),
        OBJECT_MAPPER.valueToTree(restarted.listBlockedIps()));
    Assert.assertEquals(2, config.getBlockedIps().size());
    Assert.assertTrue(config.getBlockedIps().containsAll(Arrays.asList(
        InetAddresses.forString("192.0.2.20"), InetAddresses.forString("192.0.2.21"))));
    Assert.assertFalse(restarted.blockIp("192.0.2.20").isChanged());
    Assert.assertEquals(BLOCKED_AT_MILLIS, restarted.listBlockedIps().get(0).getBlockedAtMillis());
    Mockito.verify(commonStore, Mockito.times(1)).put(Mockito.any(), Mockito.any());
  }

  @Test
  public void blockRecordsCurrentEpochMillis() throws Exception {
    configureAndInit();
    long before = System.currentTimeMillis();

    service.blockIp("192.0.2.20");

    long blockedAtMillis = service.listBlockedIps().get(0).getBlockedAtMillis();
    Assert.assertTrue(blockedAtMillis >= before);
    Assert.assertTrue(blockedAtMillis <= System.currentTimeMillis());
  }

  @Test
  public void invalidIpRecordsAreRejectedWithoutPublishingPartialSnapshot() {
    for (String entry : Arrays.asList("null", "42", "\"192.0.2.20\"", "{}",
        "{\"ip\":null,\"blockedAtMillis\":1000}",
        "{\"ip\":1234,\"blockedAtMillis\":1000}",
        "{\"ip\":\"peer.example\",\"blockedAtMillis\":1000}",
        "{\"ip\":\"fe80::1%eth0\",\"blockedAtMillis\":1000}",
        "{\"ip\":\"fe80::1%1\",\"blockedAtMillis\":1000}")) {
      Mockito.clearInvocations(commonStore);
      stubStoredBlockedIps("[{\"ip\":\"192.0.2.21\",\"blockedAtMillis\":1000}," + entry + "]");
      P2pConfig config = new P2pConfig();

      service.configure(config);

      Assert.assertTrue(entry, service.listBlockedIps().isEmpty());
      Assert.assertTrue(entry, config.getBlockedIps().isEmpty());
      Mockito.verify(commonStore, Mockito.never()).delete(Mockito.any());
    }
  }

  @Test
  public void configureKeepsBlockedIpsWhenTimestampsAreUnavailable() throws Exception {
    stubStoredBlockedIps("[{\"ip\":\"192.0.2.20\"},"
        + "{\"ip\":\"192.0.2.21\",\"blockedAtMillis\":\"unavailable\"}]");
    P2pConfig config = new P2pConfig();

    service.configure(config);

    Assert.assertEquals(Arrays.asList("192.0.2.20", "192.0.2.21"), listedIps());
    Assert.assertEquals(0, service.listBlockedIps().get(0).getBlockedAtMillis());
    Assert.assertEquals(0, service.listBlockedIps().get(1).getBlockedAtMillis());
    Assert.assertTrue(config.getBlockedIps().contains(InetAddresses.forString("192.0.2.20")));
    Assert.assertTrue(config.getBlockedIps().contains(InetAddresses.forString("192.0.2.21")));
    Mockito.verify(commonStore, Mockito.never()).put(Mockito.any(), Mockito.any());
    Mockito.verify(commonStore, Mockito.never()).delete(Mockito.any());
  }

  @Test
  public void maximumBlocklistWithLongIpv6AddressesFitsAndReloads() throws Exception {
    List<BlockedIpInfo> entries = new ArrayList<>();
    for (int i = 0; i < PeerManagementService.MAX_BLOCKED_IPS - 1; i++) {
      entries.add(new BlockedIpInfo("ffff:ffff:ffff:ffff:ffff:ffff:ffff:"
          + Integer.toHexString(i), Long.MAX_VALUE));
    }
    stubStoredBlockedIps(OBJECT_MAPPER.writeValueAsString(entries));
    configureAndInit();

    Assert.assertTrue(service.blockIp("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff").isChanged());

    ArgumentCaptor<BytesCapsule> stored = ArgumentCaptor.forClass(BytesCapsule.class);
    Mockito.verify(commonStore).put(AdditionalMatchers.aryEq(DB_KEY_BLOCKED_IPS), stored.capture());
    byte[] value = stored.getValue().getData();
    Assert.assertTrue(value.length <= 1024 * 1024);
    Mockito.when(commonStore.get(AdditionalMatchers.aryEq(DB_KEY_BLOCKED_IPS)))
        .thenReturn(new BytesCapsule(value));
    P2pConfig reloaded = new P2pConfig();
    service.configure(reloaded);

    Assert.assertEquals(PeerManagementService.MAX_BLOCKED_IPS, service.listBlockedIps().size());
    Assert.assertEquals(PeerManagementService.MAX_BLOCKED_IPS, reloaded.getBlockedIps().size());
    Assert.assertEquals(OBJECT_MAPPER.readTree(value),
        OBJECT_MAPPER.valueToTree(service.listBlockedIps()));
    Mockito.verify(commonStore, Mockito.never()).delete(Mockito.any());
  }

  private void setCommonStore(PeerManagementService instance) throws Exception {
    Field field = PeerManagementService.class.getDeclaredField("commonStore");
    field.setAccessible(true);
    field.set(instance, commonStore);
  }

  private P2pConfig configureAndInit() {
    P2pConfig config = new P2pConfig();
    service.configure(config);
    service.init(p2pService);
    return config;
  }

  private void stubStoredBlockedIps(String value) {
    Mockito.when(commonStore.get(AdditionalMatchers.aryEq(
        DB_KEY_BLOCKED_IPS)))
        .thenReturn(new BytesCapsule(value.getBytes(StandardCharsets.UTF_8)));
  }

  private void assertInvalidEndpoint(String endpoint) throws Exception {
    try {
      service.addPeer(endpoint);
      Assert.fail("Expected invalid endpoint: " + endpoint);
    } catch (JsonRpcInvalidParamsException expected) {
      Assert.assertNotNull(expected.getMessage());
    }
  }

  private void assertInvalidIp(String ip) throws Exception {
    try {
      service.blockIp(ip);
      Assert.fail("Expected invalid IP: " + ip);
    } catch (JsonRpcInvalidParamsException expected) {
      Assert.assertNotNull(expected.getMessage());
    }
  }

  private List<String> listedIps() {
    return service.listBlockedIps().stream().map(BlockedIpInfo::getIp).collect(Collectors.toList());
  }

  private String buildBlockedIpJson(int size) throws Exception {
    List<BlockedIpInfo> entries = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      entries.add(new BlockedIpInfo("2001:db8::" + Integer.toHexString(i), BLOCKED_AT_MILLIS));
    }
    return OBJECT_MAPPER.writeValueAsString(entries);
  }

  private PeerConnection mockPeer(boolean active, boolean syncFinished, String remoteAddress,
      long averageLatencyMillis) {
    PeerConnection peer = Mockito.mock(PeerConnection.class);
    Channel channel = Mockito.mock(Channel.class);
    ActivePeerInfo peerInfo = Mockito.mock(ActivePeerInfo.class);
    Mockito.when(channel.isActive()).thenReturn(active);
    Mockito.when(peer.getChannel()).thenReturn(channel);
    Mockito.when(peerInfo.isNeedSyncFromPeer()).thenReturn(!syncFinished);
    Mockito.when(peer.getActivePeerInfo()).thenReturn(peerInfo);
    Mockito.when(peerInfo.getRemoteAddress()).thenReturn(remoteAddress);
    Mockito.when(peerInfo.getAverageLatencyMillis()).thenReturn(averageLatencyMillis);
    return peer;
  }

}
