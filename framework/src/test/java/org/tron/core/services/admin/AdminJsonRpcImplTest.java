package org.tron.core.services.admin;

import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.net.service.peermanagement.ActivePeerListResult;
import org.tron.core.net.service.peermanagement.BlockedIpInfo;
import org.tron.core.net.service.peermanagement.PeerManagementService;
import org.tron.core.net.service.peermanagement.PeerOperationResult;
import org.tron.core.services.jsonrpc.ApiLogCapture;

public class AdminJsonRpcImplTest {

  private PeerManagementService peerManagementService;
  private AdminJsonRpcImpl adminJsonRpc;

  @Before
  public void setUp() {
    peerManagementService = Mockito.mock(PeerManagementService.class);
    adminJsonRpc = new AdminJsonRpcImpl(peerManagementService);
  }

  @Test
  public void peerManagementMethodsDelegateToSharedService() throws Exception {
    PeerOperationResult peerResult = new PeerOperationResult(true, true, 1, "");
    PeerOperationResult blockedIpResult = new PeerOperationResult(true, true, 2, "");
    ActivePeerListResult activePeers = new ActivePeerListResult(0, 0, 0, 0,
        java.util.Collections.emptyList());
    Mockito.when(peerManagementService.addPeer("192.0.2.20:18888")).thenReturn(peerResult);
    Mockito.when(peerManagementService.removePeer("192.0.2.20:18888")).thenReturn(peerResult);
    Mockito.when(peerManagementService.disconnectPeer("192.0.2.20:18888"))
        .thenReturn(peerResult);
    Mockito.when(peerManagementService.listActivePeers()).thenReturn(activePeers);
    Mockito.when(peerManagementService.blockIp("192.0.2.20")).thenReturn(blockedIpResult);
    Mockito.when(peerManagementService.unblockIp("192.0.2.20")).thenReturn(blockedIpResult);
    List<BlockedIpInfo> blockedIps = Collections.singletonList(
        new BlockedIpInfo("192.0.2.20", 1_790_000_000_000L));
    Mockito.when(peerManagementService.listBlockedIps()).thenReturn(blockedIps);

    Assert.assertSame(peerResult, adminJsonRpc.addPeer("192.0.2.20:18888"));
    Assert.assertSame(peerResult, adminJsonRpc.removePeer("192.0.2.20:18888"));
    Assert.assertSame(peerResult, adminJsonRpc.disconnectPeer("192.0.2.20:18888"));
    Assert.assertSame(activePeers, adminJsonRpc.listActivePeers());
    Assert.assertSame(blockedIpResult, adminJsonRpc.blockIp("192.0.2.20"));
    Assert.assertSame(blockedIpResult, adminJsonRpc.unblockIp("192.0.2.20"));
    Assert.assertSame(blockedIps, adminJsonRpc.listBlockedIps());
  }

  @Test
  public void writeCallsLogMethodAndTargetWhileQueriesDoNot() throws Exception {
    String endpoint = "192.0.2.20:18888";
    String ip = "2001:db8::1";

    try (ApiLogCapture logs = new ApiLogCapture()) {
      adminJsonRpc.addPeer(endpoint);
      adminJsonRpc.removePeer(endpoint);
      adminJsonRpc.disconnectPeer(endpoint);
      adminJsonRpc.blockIp(ip);
      adminJsonRpc.unblockIp(ip);
      adminJsonRpc.listActivePeers();
      adminJsonRpc.listBlockedIps();

      List<ILoggingEvent> events = logs.events();
      String[] expected = {
          "admin_addPeer target=" + endpoint,
          "admin_removePeer target=" + endpoint,
          "admin_disconnectPeer target=" + endpoint,
          "admin_blockIp target=" + ip,
          "admin_unblockIp target=" + ip
      };
      Assert.assertEquals(expected.length, events.size());
      for (int i = 0; i < expected.length; i++) {
        Assert.assertTrue(events.get(i).getFormattedMessage().contains(expected[i]));
      }
    }
  }

  @Test
  public void rejectedWriteDoesNotLogUnsafeArgumentOrException() throws Exception {
    String unsafe = "192.0.2.20:18888\nforged=secret";
    Mockito.when(peerManagementService.addPeer(unsafe))
        .thenThrow(new JsonRpcInvalidParamsException("sensitive-detail"));

    try (ApiLogCapture logs = new ApiLogCapture()) {
      try {
        adminJsonRpc.addPeer(unsafe);
        Assert.fail("Expected invalid parameters");
      } catch (JsonRpcInvalidParamsException expected) {
        // The original JSON-RPC exception remains client-visible.
      }

      List<ILoggingEvent> events = logs.events();
      Assert.assertEquals(1, events.size());
      Assert.assertTrue(events.get(0).getFormattedMessage().contains(
          "method=admin_addPeer target=omitted"));
      Assert.assertFalse(events.get(0).getFormattedMessage().contains("forged"));
      Assert.assertFalse(events.get(0).getFormattedMessage().contains("sensitive-detail"));
      Assert.assertNull(events.get(0).getThrowableProxy());
    }
  }
}
