package org.tron.core.net.services;

import java.io.File;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.net.TronNetService;
import org.tron.core.net.service.effective.EffectiveCheckService;
import org.tron.p2p.P2pConfig;

public class EffectiveCheckServiceTest {

  protected TronApplicationContext context;
  private EffectiveCheckService service;
  private String dbPath = "output-effective-service-test";

  @Before
  public void init() {
    Args.setParam(new String[] {"--output-directory", dbPath, "--debug"},
        Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    service = context.getBean(EffectiveCheckService.class);
  }

  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void testNoIpv4() throws Exception {
    TronNetService tronNetService = context.getBean(TronNetService.class);
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
    TronNetService tronNetService = context.getBean(TronNetService.class);
    P2pConfig p2pConfig = new P2pConfig();
    p2pConfig.setIp("127.0.0.1");
    p2pConfig.setPort(34567);
    ReflectUtils.setFieldValue(tronNetService, "p2pConfig", p2pConfig);
    TronNetService.getP2pService().start(p2pConfig);

    service.triggerNext();
    Assert.assertNull(service.getCur());

    ReflectUtils.invokeMethod(service, "resetCount");
    InetSocketAddress cur = new InetSocketAddress("192.168.0.1", 34567);
    service.setCur(cur);
    service.onDisconnect(cur);
  }
}
