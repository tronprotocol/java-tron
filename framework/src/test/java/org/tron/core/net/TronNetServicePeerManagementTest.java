package org.tron.core.net;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.SeedNode;
import org.tron.core.exception.TronError;
import org.tron.core.net.messagehandler.TransactionsMsgHandler;
import org.tron.core.net.peer.PeerManager;
import org.tron.core.net.peer.PeerStatusCheck;
import org.tron.core.net.service.adv.AdvService;
import org.tron.core.net.service.effective.EffectiveCheckService;
import org.tron.core.net.service.effective.ResilienceService;
import org.tron.core.net.service.fetchblock.FetchBlockService;
import org.tron.core.net.service.nodepersist.NodePersistService;
import org.tron.core.net.service.peermanagement.PeerManagementService;
import org.tron.core.net.service.relay.RelayService;
import org.tron.core.net.service.statistics.TronStatsManager;
import org.tron.core.net.service.sync.SyncService;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pService;

public class TronNetServicePeerManagementTest {

  @Test
  public void peerManagementLifecycleFollowsP2pLifecycle() throws Exception {
    TestContext context = new TestContext();
    try (MockedStatic<PeerManager> peerManager = Mockito.mockStatic(PeerManager.class)) {
      context.service.start();

      InOrder startOrder = Mockito.inOrder(context.peerManagementService, context.p2pService,
          context.effectiveCheckService);
      startOrder.verify(context.peerManagementService)
          .configure(context.p2pConfig, context.p2pService);
      startOrder.verify(context.p2pService).start(context.p2pConfig);
      startOrder.verify(context.p2pService).register(Mockito.any(P2pEventHandlerImpl.class));
      startOrder.verify(context.effectiveCheckService).init();
      startOrder.verify(context.peerManagementService).init();
      Assert.assertSame(context.p2pConfig, TronNetService.getP2pConfig());

      context.service.close();

      InOrder stopOrder = Mockito.inOrder(context.peerManagementService, context.p2pService);
      stopOrder.verify(context.peerManagementService).close();
      stopOrder.verify(context.p2pService).close();
      peerManager.verify(PeerManager::init);
      peerManager.verify(PeerManager::close);
    } finally {
      context.restoreStatics();
    }
  }

  @Test
  public void failedP2pStartLeavesPeerManagementUnavailable() throws Exception {
    TestContext context = new TestContext();
    Mockito.doThrow(new IllegalStateException("start failed"))
        .when(context.p2pService).start(context.p2pConfig);
    try {
      context.service.start();
      Assert.fail("Expected network startup to fail");
    } catch (TronError expected) {
      Assert.assertNotNull(expected.getCause());
    } finally {
      context.restoreStatics();
    }

    Mockito.verify(context.peerManagementService)
        .configure(context.p2pConfig, context.p2pService);
    Mockito.verify(context.peerManagementService, Mockito.never()).close();
    Mockito.verify(context.peerManagementService, Mockito.never()).init();
  }

  private static class TestContext {

    private final TronNetService service = new TronNetService();
    private final P2pConfig p2pConfig = new P2pConfig();
    private final P2pService p2pService = Mockito.mock(P2pService.class);
    private final PeerManagementService peerManagementService =
        Mockito.mock(PeerManagementService.class);
    private final EffectiveCheckService effectiveCheckService =
        Mockito.mock(EffectiveCheckService.class);
    private final P2pService originalP2pService;
    private final P2pConfig originalP2pConfig;

    private TestContext() throws Exception {
      originalP2pService = (P2pService) getStaticField("p2pService");
      originalP2pConfig = (P2pConfig) getStaticField("p2pConfig");
      setStaticField("p2pService", p2pService);

      CommonParameter parameter = Mockito.mock(CommonParameter.class);
      SeedNode seedNode = new SeedNode();
      seedNode.setAddressList(new ArrayList<>());
      p2pConfig.setIp("192.0.2.1");
      Mockito.when(parameter.getP2pConfig()).thenReturn(p2pConfig);
      Mockito.when(parameter.getSeedNode()).thenReturn(seedNode);
      Mockito.when(parameter.getActiveNodes()).thenReturn(Collections.emptyList());
      Mockito.when(parameter.getPassiveNodes()).thenReturn(Collections.emptyList());
      Mockito.when(parameter.getFastForwardNodes()).thenReturn(Collections.emptyList());
      Mockito.when(parameter.getDnsTreeUrls()).thenReturn(Collections.emptyList());

      NodePersistService nodePersistService = Mockito.mock(NodePersistService.class);
      Mockito.when(nodePersistService.dbRead()).thenReturn(Collections.emptyList());
      setField("parameter", parameter);
      setField("nodePersistService", nodePersistService);
      setField("peerManagementService", peerManagementService);
      setField("p2pEventHandler", Mockito.mock(P2pEventHandlerImpl.class));
      setField("advService", Mockito.mock(AdvService.class));
      setField("syncService", Mockito.mock(SyncService.class));
      setField("peerStatusCheck", Mockito.mock(PeerStatusCheck.class));
      setField("resilienceService", Mockito.mock(ResilienceService.class));
      setField("transactionsMsgHandler", Mockito.mock(TransactionsMsgHandler.class));
      setField("fetchBlockService", Mockito.mock(FetchBlockService.class));
      setField("tronStatsManager", Mockito.mock(TronStatsManager.class));
      setField("relayService", Mockito.mock(RelayService.class));
      setField("effectiveCheckService", effectiveCheckService);
    }

    private void restoreStatics() throws Exception {
      setStaticField("p2pService", originalP2pService);
      setStaticField("p2pConfig", originalP2pConfig);
    }

    private void setField(String name, Object value) throws Exception {
      Field field = TronNetService.class.getDeclaredField(name);
      field.setAccessible(true);
      field.set(service, value);
    }

    private static Object getStaticField(String name) throws Exception {
      Field field = TronNetService.class.getDeclaredField(name);
      field.setAccessible(true);
      return field.get(null);
    }

    private static void setStaticField(String name, Object value) throws Exception {
      Field field = TronNetService.class.getDeclaredField(name);
      field.setAccessible(true);
      field.set(null, value);
    }
  }
}
