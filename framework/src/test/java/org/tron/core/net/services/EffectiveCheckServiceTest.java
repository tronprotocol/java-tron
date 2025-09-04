package org.tron.core.net.services;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.core.net.TronNetService;
import org.tron.core.net.service.effective.EffectiveCheckService;
import org.tron.p2p.P2pConfig;

public class EffectiveCheckServiceTest extends BaseTest {

  @Resource
  private EffectiveCheckService service;
  @Resource
  private TronNetService tronNetService;

  @BeforeClass
  public static void init() {
    Args.setParam(new String[] {"--output-directory", dbPath(), "--debug"},
        Constant.TEST_CONF);
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
    ReflectUtils.setFieldValue(tronNetService, "p2pConfig", p2pConfig);
    TronNetService.getP2pService().start(p2pConfig);

    service.triggerNext();
    Assert.assertNull(service.getCur());

    ReflectUtils.invokeMethod(service, "resetCount");
    InetSocketAddress cur = new InetSocketAddress("192.168.0.1", port);
    service.setCur(cur);
    service.onDisconnect(cur);
  }
}
