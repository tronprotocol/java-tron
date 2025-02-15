package org.tron.common.logsfilter.capsule;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.beust.jcommander.internal.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.logsfilter.trigger.ContractTrigger;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.LogInfo;

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
  public void testLogInfo() {
    logger.info("log info to string: {}, ", logInfo.toString());
    logger.info("log clone data: {}, ", logInfo.getClonedData());
    CollectionUtils.isNotEmpty(logInfo.getClonedTopics());
    CollectionUtils.isNotEmpty(logInfo.getHexTopics());
    new LogInfo(null, null, null);
  }

}
