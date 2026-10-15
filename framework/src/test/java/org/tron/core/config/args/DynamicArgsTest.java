package org.tron.core.config.args;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.tron.common.BaseMethodTest;
import org.tron.common.TestConstants;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.net.TronNetService;
import org.tron.p2p.P2pConfig;

public class DynamicArgsTest extends BaseMethodTest {
  private DynamicArgs dynamicArgs;

  @Override
  protected void afterInit() {
    dynamicArgs = context.getBean(DynamicArgs.class);
  }

  @Test
  public void start() {
    CommonParameter parameter = Args.getInstance();
    Assert.assertEquals(TestConstants.TEST_CONF, Args.getConfigFilePath());
    Assert.assertTrue(parameter.isDynamicConfigEnable());
    Assert.assertEquals(600, parameter.getDynamicConfigCheckInterval());

    dynamicArgs.init();
    File configFile = (File) ReflectUtils.getFieldObject(dynamicArgs, "configFile");
    Assert.assertNotNull(configFile);
    Assert.assertEquals(TestConstants.TEST_CONF, configFile.getName());
    Assert.assertEquals(0, (long) ReflectUtils.getFieldObject(dynamicArgs, "lastModified"));

    TronNetService tronNetService = context.getBean(TronNetService.class);
    ReflectUtils.setFieldValue(tronNetService, "p2pConfig", new P2pConfig());
    File config = new File(Args.getConfigFilePath());
    if (!config.exists()) {
      try {
        config.createNewFile();
      } catch (Exception e) {
        return;
      }
      dynamicArgs.run();
      try {
        config.delete();
      } catch (Exception e) {
        return;
      }
    }
    try {
      dynamicArgs.reload();
    } catch (Exception e) {
      // no need to deal with
    }

    dynamicArgs.close();
  }

  @Test
  public void retainedActiveNodeStaysVisibleDuringReload() {
    InetSocketAddress retained = new InetSocketAddress("192.0.2.10", 18888);
    InetSocketAddress removed = new InetSocketAddress("192.0.2.11", 18888);
    InetSocketAddress added = new InetSocketAddress("192.0.2.12", 18888);
    List<InetSocketAddress> activeNodes =
        new CopyOnWriteArrayList<InetSocketAddress>(Arrays.asList(retained, removed)) {
          @Override
          public void clear() {
            super.clear();
            Assert.assertTrue("Retained node disappeared during reload", contains(retained));
          }

          @Override
          public boolean removeIf(Predicate<? super InetSocketAddress> filter) {
            boolean changed = super.removeIf(filter);
            Assert.assertTrue("Retained node disappeared during reload", contains(retained));
            return changed;
          }

          @Override
          public boolean add(InetSocketAddress node) {
            boolean changed = super.add(node);
            Assert.assertTrue("Retained node disappeared during reload", contains(retained));
            return changed;
          }
        };
    P2pConfig p2pConfig = new P2pConfig();
    ReflectUtils.setFieldValue(p2pConfig, "activeNodes", activeNodes);
    NodeConfig nodeConfig = new NodeConfig();
    nodeConfig.setActive(Arrays.asList("192.0.2.10:18888", "192.0.2.12:18888"));
    List<InetSocketAddress> previousActiveNodes = Args.getInstance().getActiveNodes();

    try (MockedStatic<TronNetService> netService = Mockito.mockStatic(TronNetService.class)) {
      netService.when(TronNetService::getP2pConfig).thenReturn(p2pConfig);
      ReflectUtils.invokeMethod(dynamicArgs, "updateActiveNodes",
          new Class[] {NodeConfig.class}, nodeConfig);
      Assert.assertEquals(Arrays.asList(retained, added), activeNodes);
    } finally {
      Args.getInstance().setActiveNodes(previousActiveNodes);
    }
  }
}
