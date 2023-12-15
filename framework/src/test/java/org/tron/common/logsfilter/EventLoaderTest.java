package org.tron.common.logsfilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.logsfilter.trigger.BlockLogTrigger;
import org.tron.common.logsfilter.trigger.TransactionLogTrigger;

public class EventLoaderTest {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void launchNativeQueue() {
    EventPluginConfig config = new EventPluginConfig();
    config.setSendQueueLength(1000);
    config.setBindPort(5555);
    config.setUseNativeQueue(true);

    List<TriggerConfig> triggerConfigList = new ArrayList<>();

    TriggerConfig blockTriggerConfig = new TriggerConfig();
    blockTriggerConfig.setTriggerName("block");
    blockTriggerConfig.setEnabled(true);
    blockTriggerConfig.setTopic("block");
    triggerConfigList.add(blockTriggerConfig);

    config.setTriggerConfigList(triggerConfigList);

    Assert.assertTrue(EventPluginLoader.getInstance().start(config));

    EventPluginLoader.getInstance().stopPlugin();
  }

  @Test
  public void testPluginManager() throws IOException {
    EventPluginConfig config = new EventPluginConfig();
    config.setUseNativeQueue(false);
    config.setPluginPath(temporaryFolder.newFolder() + "/testPlugin.zip");
    createPluginInPath(config.getPluginPath());
    Assert.assertThrows(Exception.class, () -> EventPluginLoader.getInstance().start(config));
  }

  private void createPluginInPath(String pluginPath) {
    String fileName = "../testPlugin.zip";
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(
            Files.newOutputStream(Paths.get(pluginPath)))) {
      ZipEntry zipEntry = new ZipEntry(fileName);
      zipOutputStream.putNextEntry(zipEntry);
      zipOutputStream.write("".getBytes());
      zipOutputStream.closeEntry();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
