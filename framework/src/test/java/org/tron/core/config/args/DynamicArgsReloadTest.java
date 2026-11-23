package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.Configuration;
import org.tron.core.exception.TronError;
import org.tron.core.net.TronNetService;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.utils.NetUtil;

public class DynamicArgsReloadTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private final CommonParameter parameter = new CommonParameter();
  private P2pConfig p2pConfig;
  private CommonParameter previousParameter;
  private String previousConfigPath;
  private Object previousConfig;
  private Path configFile;
  private DynamicArgs dynamicArgs;
  private Runnable reloadTask;
  private long initialTimestamp;

  @Before
  public void setUp() throws Exception {
    previousParameter = Args.getInstance();
    previousConfigPath = Args.getConfigFilePath();
    previousConfig = ReflectionTestUtils.getField(Configuration.class, "config");

    configFile = temp.newFile("dynamic.conf").toPath();
    initialTimestamp = writeConfig("", "192.0.2.1", "192.0.2.2", 1000);

    parameter.setDynamicConfigEnable(true);
    parameter.setDynamicConfigCheckInterval(1);
    parameter.setNodeLanIp("127.0.0.1");
    parameter.setNodeExternalIp("127.0.0.1");
    parameter.fastForwardNodes = Collections.emptyList();
    parameter.setActiveNodes(Collections.singletonList(activeNode("192.0.2.1")));
    parameter.setPassiveNodes(Collections.singletonList(InetAddress.getByName("192.0.2.2")));
    try (MockedStatic<NetUtil> netUtil = mockStatic(NetUtil.class)) {
      p2pConfig = new P2pConfig();
    }
    p2pConfig.setActiveNodes(parameter.getActiveNodes());
    p2pConfig.getTrustNodes().addAll(parameter.getPassiveNodes());
    p2pConfig.getTrustNodes().add(activeNode("192.0.2.1").getAddress());

    ReflectionTestUtils.setField(CommonParameter.class, "PARAMETER", parameter);
    ReflectionTestUtils.setField(Args.class, "configFilePath", configFile.toString());

    ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
    try (MockedStatic<ExecutorServiceManager> executors =
        mockStatic(ExecutorServiceManager.class)) {
      executors.when(
          () -> ExecutorServiceManager.newSingleThreadScheduledExecutor("dynamic-reload"))
          .thenReturn(scheduler);
      dynamicArgs = new DynamicArgs();
      dynamicArgs.init();
    }
    ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
    verify(scheduler).scheduleWithFixedDelay(task.capture(), anyLong(), anyLong(),
        any(TimeUnit.class));
    reloadTask = task.getValue();
  }

  @After
  public void restoreConfiguration() {
    ReflectionTestUtils.setField(CommonParameter.class, "PARAMETER", previousParameter);
    ReflectionTestUtils.setField(Args.class, "configFilePath", previousConfigPath);
    ReflectionTestUtils.setField(Configuration.class, "config", previousConfig);
  }

  @Test
  public void reloadsNewerFileAfterInvalidPeerAddresses() throws Exception {
    assertRecoversAfterInvalidConfig(
        "node.active = [\"192.0.2.3:not-a-port\"]",
        "node.passive = [\"192.0.2.4:not-a-port\"]");
  }

  @Test
  public void reloadsNewerFileAfterWrongPeerConfigType() throws Exception {
    assertRecoversAfterInvalidConfig("node.active = true", "node.passive = true");
  }

  @Test
  public void reloadsNewerFileAfterNonParameterTronError() throws Exception {
    assertReloadsNewerFileAfterError(
        new TronError("Unexpected failure", TronError.ErrCode.DB_FLUSH));
  }

  @Test
  public void reloadsNewerFileAfterOtherError() throws Exception {
    assertReloadsNewerFileAfterError(new AssertionError("Unexpected failure"));
  }

  private void assertReloadsNewerFileAfterError(Error failure) throws Exception {
    long editedTimestamp = writeConfig("", "192.0.2.3", "192.0.2.4", initialTimestamp + 2000);
    try (MockedStatic<TronNetService> netService = mockStatic(TronNetService.class)) {
      netService.when(TronNetService::getP2pConfig).thenReturn(p2pConfig);
      try (MockedStatic<Configuration> configuration = mockStatic(Configuration.class)) {
        configuration.when(() -> Configuration.getByFileName(configFile.toString()))
            .thenThrow(failure);
        reloadTask.run();
        configuration.verify(() -> Configuration.getByFileName(configFile.toString()));
      }
      assertEquals(editedTimestamp, lastModified());
      assertNodes("192.0.2.1", "192.0.2.2");

      long newerTimestamp = writeConfig("", "192.0.2.5", "192.0.2.6", editedTimestamp + 2000);
      reloadTask.run();
      assertEquals(newerTimestamp, lastModified());
      assertNodes("192.0.2.5", "192.0.2.6");
    }
  }

  private void assertRecoversAfterInvalidConfig(String... invalidSettings) throws Exception {
    long editedTimestamp = initialTimestamp;
    int expectedLoads = 0;
    try (MockedStatic<TronNetService> netService = mockStatic(TronNetService.class);
         MockedStatic<Configuration> configuration =
             mockStatic(Configuration.class, CALLS_REAL_METHODS)) {
      netService.when(TronNetService::getP2pConfig).thenReturn(p2pConfig);
      for (String invalidSetting : invalidSettings) {
        editedTimestamp = writeConfig(invalidSetting, "192.0.2.3", "192.0.2.4",
            editedTimestamp + 2000);
        reloadTask.run();
        assertEquals(invalidSetting, editedTimestamp, lastModified());
        assertNodes("192.0.2.1", "192.0.2.2");

        reloadTask.run();
        configuration.verify(() -> Configuration.getByFileName(configFile.toString()),
            times(++expectedLoads));
      }
      long correctedTimestamp = writeConfig("", "192.0.2.3", "192.0.2.4", editedTimestamp + 2000);
      reloadTask.run();
      assertEquals(correctedTimestamp, lastModified());
      assertNodes("192.0.2.3", "192.0.2.4");
    }
  }

  private long writeConfig(String setting, String activeIp, String passiveIp, long timestamp)
      throws Exception {
    String content = "node.active = [\"" + activeIp + ":18888\"]\n"
        + "node.passive = [\"" + passiveIp + ":18888\"]\n" + setting + "\n";
    Files.write(configFile, content.getBytes(StandardCharsets.UTF_8));
    Files.setLastModifiedTime(configFile, FileTime.fromMillis(timestamp));
    return configFile.toFile().lastModified();
  }

  private long lastModified() {
    return (long) ReflectionTestUtils.getField(dynamicArgs, "lastModified");
  }

  private void assertNodes(String activeIp, String passiveIp) throws Exception {
    InetSocketAddress active = activeNode(activeIp);
    InetAddress passive = InetAddress.getByName(passiveIp);
    assertEquals(Collections.singletonList(active), parameter.getActiveNodes());
    assertEquals(Collections.singletonList(passive), parameter.getPassiveNodes());
    assertEquals(Collections.singletonList(active), p2pConfig.getActiveNodes());
    assertEquals(Arrays.asList(passive, active.getAddress()), p2pConfig.getTrustNodes());
  }

  private static InetSocketAddress activeNode(String ip) {
    return new InetSocketAddress(ip, 18888);
  }
}
