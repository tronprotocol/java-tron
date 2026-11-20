package org.tron.core.config.args;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.tron.core.config.Configuration;
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
  public void reloadPeersIgnoresInvalidRpcSettings() {
    String configPath = Args.getConfigFilePath();
    Config config = ConfigFactory.parseString("node.rpc.maxMessageSize = -1")
        .withFallback(peerConfig("192.0.2.10:18888", "192.0.2.20:18888"));
    P2pConfig p2pConfig = new P2pConfig();

    try (MockedStatic<Configuration> configuration = Mockito.mockStatic(Configuration.class);
         MockedStatic<TronNetService> netService = Mockito.mockStatic(TronNetService.class)) {
      configuration.when(() -> Configuration.getByFileName(configPath)).thenReturn(config);
      netService.when(TronNetService::getP2pConfig).thenReturn(p2pConfig);

      dynamicArgs.reload();

      assertPeers(p2pConfig, "192.0.2.10", "192.0.2.20");
    }
  }

  @Test
  public void malformedPassivePeerPreservesStateAndCorrectedReloadSucceeds() {
    String configPath = Args.getConfigFilePath();
    Config initial = peerConfig("192.0.2.10:18888", "192.0.2.20:18888");
    Config malformed = peerConfig("192.0.2.11:18888", "192.0.2.21:not-a-port");
    Config corrected = peerConfig("192.0.2.11:18888", "192.0.2.21:18888");
    P2pConfig p2pConfig = new P2pConfig();

    try (MockedStatic<Configuration> configuration = Mockito.mockStatic(Configuration.class);
         MockedStatic<TronNetService> netService = Mockito.mockStatic(TronNetService.class)) {
      configuration.when(() -> Configuration.getByFileName(configPath))
          .thenReturn(initial, malformed, corrected);
      netService.when(TronNetService::getP2pConfig).thenReturn(p2pConfig);

      dynamicArgs.reload();
      assertPeers(p2pConfig, "192.0.2.10", "192.0.2.20");

      Assert.assertThrows(IllegalArgumentException.class, dynamicArgs::reload);
      assertPeers(p2pConfig, "192.0.2.10", "192.0.2.20");

      dynamicArgs.reload();
      assertPeers(p2pConfig, "192.0.2.11", "192.0.2.21");
    }
  }

  private static Config peerConfig(String active, String passive) {
    return ConfigFactory.parseString("node.active = [\"" + active + "\"]\n"
        + "node.passive = [\"" + passive + "\"]")
        .withFallback(ConfigFactory.defaultReference());
  }

  private static void assertPeers(P2pConfig p2pConfig, String activeIp, String passiveIp) {
    InetSocketAddress active = new InetSocketAddress(activeIp, 18888);
    InetSocketAddress passive = new InetSocketAddress(passiveIp, 18888);
    CommonParameter parameter = Args.getInstance();
    Assert.assertEquals(Collections.singletonList(active), parameter.getActiveNodes());
    Assert.assertEquals(Collections.singletonList(passive.getAddress()),
        parameter.getPassiveNodes());
    Assert.assertEquals(Collections.singletonList(active), p2pConfig.getActiveNodes());
    List<InetAddress> trustNodes = new ArrayList<>(Arrays.asList(passive.getAddress(),
        active.getAddress()));
    parameter.getFastForwardNodes().forEach(node -> trustNodes.add(node.getAddress()));
    Assert.assertEquals(trustNodes, p2pConfig.getTrustNodes());
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
    List<InetSocketAddress> previousActiveNodes = Args.getInstance().getActiveNodes();

    try (MockedStatic<TronNetService> netService = Mockito.mockStatic(TronNetService.class)) {
      netService.when(TronNetService::getP2pConfig).thenReturn(p2pConfig);
      ReflectUtils.invokeMethod(dynamicArgs, "updateActiveNodes",
          new Class[] {List.class}, Arrays.asList(retained, added));
      Assert.assertEquals(Arrays.asList(retained, added), activeNodes);
    } finally {
      Args.getInstance().setActiveNodes(previousActiveNodes);
    }
  }
}
