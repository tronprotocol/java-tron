package org.tron.common.logsfilter.capsule;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.beust.jcommander.internal.Lists;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;
import org.tron.common.logsfilter.trigger.ContractTrigger;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.core.config.args.Args;

@Slf4j
public class ContractTriggerCapsuleTest {

  private ContractTriggerCapsule capsule;

  private LogInfo logInfo;

  @Before
  public void setUp() {
    ContractTrigger contractTrigger = new ContractTrigger();
    contractTrigger.setBlockNumber(0L);
    contractTrigger.setRemoved(false);
    logInfo = new LogInfo(bytesToAddress(new byte[] {0x11}),
        newArrayList(new DataWord()), new byte[0]);
    contractTrigger.setLogInfo(logInfo);
    contractTrigger.setRawData(new RawData(null, null, null));
    contractTrigger.setAbi(contractTrigger.getAbi());
    capsule = new ContractTriggerCapsule(contractTrigger);

  }

  private byte[] bytesToAddress(byte[] address) {
    byte[] data = new byte[20];
    System.arraycopy(address, 0, data, 20 - address.length, address.length);
    return data;
  }

  @Test
  public void testSetAndGetContractTrigger() {
    capsule.setContractTrigger(capsule.getContractTrigger());
    capsule.setBlockHash("e58f33f9baf9305dc6f82b9f1934ea8f0ade2defb951258d50167028c780351f");
    capsule.setLatestSolidifiedBlockNumber(0);
    assertEquals(0, capsule.getContractTrigger().getLatestSolidifiedBlockNumber());
    assertEquals("e58f33f9baf9305dc6f82b9f1934ea8f0ade2defb951258d50167028c780351f",
        capsule.getContractTrigger().getBlockHash());
    try {
      capsule.processTrigger();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
    }
  }

  @Test
  public void testRemovedTriggerNotWrittenToSolidityMap() throws Exception {
    Args.getSolidityContractLogTriggerMap().clear();
    Args.getSolidityContractEventTriggerMap().clear();

    EventPluginLoader mockLoader = mock(EventPluginLoader.class);
    when(mockLoader.isSolidityLogTriggerEnable()).thenReturn(true);
    when(mockLoader.isSolidityEventTriggerEnable()).thenReturn(false);
    when(mockLoader.isContractLogTriggerEnable()).thenReturn(false);
    when(mockLoader.isContractEventTriggerEnable()).thenReturn(false);
    when(mockLoader.isSolidityLogTriggerRedundancy()).thenReturn(false);
    when(mockLoader.isContractLogTriggerRedundancy()).thenReturn(false);

    Field instanceField = EventPluginLoader.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    EventPluginLoader originalInstance = (EventPluginLoader) instanceField.get(null);
    instanceField.set(null, mockLoader);

    try {
      ContractLogTrigger trigger = new ContractLogTrigger();
      trigger.setRemoved(true);
      trigger.setBlockNumber(100L);
      trigger.setTransactionId("abc");
      trigger.setContractAddress("0x01");
      LogInfo logInfo = new LogInfo(new byte[0], new ArrayList<>(), new byte[0]);
      trigger.setLogInfo(logInfo);

      ContractTriggerCapsule capsule = new ContractTriggerCapsule(trigger);
      capsule.processTrigger();

      assertTrue(Args.getSolidityContractLogTriggerMap().isEmpty());
      assertTrue(Args.getSolidityContractEventTriggerMap().isEmpty());
    } finally {
      instanceField.set(null, originalInstance);
      Args.getSolidityContractLogTriggerMap().clear();
      Args.getSolidityContractEventTriggerMap().clear();
    }
  }

  @Test
  public void testLogInfo() {
    logger.info("log info to string: {}, ", logInfo.toString());
    logger.info("log clone data: {}, ", logInfo.getClonedData());
    CollectionUtils.isNotEmpty(logInfo.getClonedTopics());
    CollectionUtils.isNotEmpty(logInfo.getHexTopics());
    new LogInfo(null, null, null);
  }

}
