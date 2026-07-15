package org.tron.common.logsfilter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.tron.common.logsfilter.trigger.BlockLogTrigger;
import org.tron.common.logsfilter.trigger.TransactionLogTrigger;

public class EventLoaderTest {

  @Test
  public void launchNativeQueue() {
    EventPluginConfig config = new EventPluginConfig();
    config.setSendQueueLength(1000);
    config.setBindPort(5555);
    config.setUseNativeQueue(true);
    config.setPluginPath("pluginPath");
    config.setServerAddress("serverAddress");
    config.setDbConfig("dbConfig");
    assertEquals("pluginPath", config.getPluginPath());
    assertEquals("serverAddress", config.getServerAddress());
    assertEquals("dbConfig", config.getDbConfig());

    List<TriggerConfig> triggerConfigList = new ArrayList<>();

    TriggerConfig triggerConfig = new TriggerConfig();
    triggerConfig.setTriggerName("block");
    triggerConfig.setEnabled(true);
    triggerConfig.setTopic("topic");
    triggerConfig.setRedundancy(false);
    triggerConfig.setEthCompatible(false);
    triggerConfig.setSolidified(false);
    assertFalse(triggerConfig.isRedundancy());
    assertFalse(triggerConfig.isEthCompatible());
    assertFalse(triggerConfig.isSolidified());
    assertEquals("topic", triggerConfig.getTopic());
    triggerConfigList.add(triggerConfig);

    config.setTriggerConfigList(triggerConfigList);

    assertTrue(EventPluginLoader.getInstance().start(config));

    EventPluginLoader.getInstance().stopPlugin();
  }

  @Test
  public void testIsPluginVersionSupported() {
    assertEquals("3.0.0", EventPluginLoader.MIN_PLUGIN_VERSION);
    // last releases before fastjson removal — must be rejected
    assertFalse(checkVersion("1.0.0"));
    assertFalse(checkVersion("2.2.0"));
    assertFalse(checkVersion("2.9.9"));
    // 3.0.0 onward — must be accepted
    assertTrue(checkVersion("3.0.0"));
    assertTrue(checkVersion("3.1.5"));
    assertTrue(checkVersion("10.0.0"));
    // empty/null version — reject
    assertFalse(checkVersion(""));
    assertFalse(checkVersion(null));
  }

  private static boolean checkVersion(String version) {
    PluginManager pm = mock(PluginManager.class);
    PluginWrapper wrapper = mock(PluginWrapper.class);
    PluginDescriptor desc = mock(PluginDescriptor.class);
    when(pm.getPlugin("test")).thenReturn(wrapper);
    when(wrapper.getDescriptor()).thenReturn(desc);
    when(desc.getVersion()).thenReturn(version);
    return EventPluginLoader.isPluginVersionSupported(pm, "test");
  }

  @Test
  public void testBlockLogTrigger() {
    BlockLogTrigger blt = new BlockLogTrigger();
    blt.setBlockHash(blt.getBlockHash());
    blt.setBlockNumber(blt.getBlockNumber());
    blt.setTransactionSize(blt.getTransactionSize());
    blt.setLatestSolidifiedBlockNumber(blt.getLatestSolidifiedBlockNumber());
    blt.setTriggerName(blt.getTriggerName());
    blt.setTimeStamp(blt.getTimeStamp());
    blt.setTransactionList(blt.getTransactionList());
    Assert.assertNotNull(blt.toString());
  }

  @Test
  public void testTransactionLogTrigger() {
    TransactionLogTrigger tlt = new TransactionLogTrigger();
    tlt.setBlockHash(tlt.getBlockHash());
    tlt.setBlockNumber(tlt.getBlockNumber());
    tlt.setTransactionId(tlt.getTransactionId());
    tlt.setLatestSolidifiedBlockNumber(tlt.getLatestSolidifiedBlockNumber());
    tlt.setTriggerName(tlt.getTriggerName());
    tlt.setTimeStamp(tlt.getTimeStamp());
    tlt.setEnergyFee(tlt.getEnergyFee());
    tlt.setNetFee(tlt.getNetFee());
    tlt.setEnergyUsage(tlt.getEnergyUsage());
    tlt.setAssetAmount(tlt.getAssetAmount());
    tlt.setContractAddress(tlt.getContractAddress());
    tlt.setResult(tlt.getResult());
    tlt.setContractResult(tlt.getContractResult());
    tlt.setContractType(tlt.getContractType());
    tlt.setContractCallValue(tlt.getContractCallValue());
    tlt.setFromAddress(tlt.getFromAddress());
    tlt.setToAddress(tlt.getToAddress());
    tlt.setTransactionIndex(tlt.getTransactionIndex());
    tlt.setFeeLimit(tlt.getFeeLimit());
    tlt.setCumulativeEnergyUsed(tlt.getCumulativeEnergyUsed());
    tlt.setData(tlt.getData());
    tlt.setOriginEnergyUsage(tlt.getOriginEnergyUsage());
    tlt.setEnergyUsageTotal(tlt.getEnergyUsageTotal());
    tlt.setNetUsage(tlt.getNetUsage());
    tlt.setAssetName(tlt.getAssetName());
    tlt.setInternalTransactionList(tlt.getInternalTransactionList());
    tlt.setPreCumulativeLogCount(tlt.getPreCumulativeLogCount());
    tlt.setLogList(tlt.getLogList());
    tlt.setEnergyUnitPrice(tlt.getEnergyUnitPrice());
    tlt.setTimeStamp(1L);
    Assert.assertNotNull(tlt.toString());
  }
}
