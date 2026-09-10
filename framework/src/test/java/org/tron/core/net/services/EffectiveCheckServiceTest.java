package org.tron.core.net.services;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import javax.annotation.Resource;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.config.args.Args;
import org.tron.core.net.TronNetService;
import org.tron.core.net.service.effective.EffectiveCheckService;
import org.tron.p2p.P2pConfig;

public class EffectiveCheckServiceTest extends BaseTest {

  private P2pConfig savedP2pConfig;
  private boolean p2pStarted;

  @After
  public void closeP2p() {
    if (p2pStarted) {
      try {
        TronNetService.getP2pService().close();
      } finally {
        ReflectUtils.setFieldValue(tronNetService, "p2pConfig", savedP2pConfig);
      }
    }
  }

  @Resource
  private EffectiveCheckService service;
  @Resource
  private TronNetService tronNetService;

  @BeforeClass
  public static void init() {
    Args.setParam(new String[] {"--output-directory", dbPath(), "--debug"},
        TestConstants.TEST_CONF);
  }

  @Test
  public void testNoIpv4() throws Exception {
    Method privateMethod = tronNetService.getClass()
        .getDeclaredMethod("updateConfig", P2pConfig.class);
    privateMethod.setAccessible(true);
    P2pConfig config = new P2pConfig();
    config.setIp(null);
    P2pConfig newConfig = (P2pConfig) privateMethod.invoke(tronNetService, config);
    Assert.assertNotNull(newConfig.getIp());
  }

  @Test
  public void testFind() {
    int port = PublicMethod.chooseRandomPort();
    P2pConfig p2pConfig = new P2pConfig();
    p2pConfig.setIp("127.0.0.1");
    p2pConfig.setPort(port);
    savedP2pConfig = TronNetService.getP2pConfig();
    ReflectUtils.setFieldValue(tronNetService, "p2pConfig", p2pConfig);
    p2pStarted = true;
    TronNetService.getP2pService().start(p2pConfig);

    ScheduledExecutorService executor = Mockito.mock(ScheduledExecutorService.class);
    Mockito.when(executor.submit(Mockito.any(Runnable.class)))
        .thenReturn(CompletableFuture.completedFuture(null));
    ReflectUtils.setFieldValue(service, "executor", executor);
    service.triggerNext();
    ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
    Mockito.verify(executor).submit(task.capture());
    task.getValue().run();
    Assert.assertNull(service.getCur());

    ReflectUtils.invokeMethod(service, "resetCount");
    InetSocketAddress cur = new InetSocketAddress("192.168.0.1", port);
    service.setCur(cur);
    service.onDisconnect(cur);
  }
}
