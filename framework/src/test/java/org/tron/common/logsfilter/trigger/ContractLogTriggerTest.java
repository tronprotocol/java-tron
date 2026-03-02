package org.tron.common.logsfilter.trigger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.logsfilter.capsule.RawData;
import org.tron.common.runtime.vm.DataWord;

public class ContractLogTriggerTest {
  private ContractEventTrigger mockEventTrigger;

  @Before
  public void setUp() {
    mockEventTrigger = new ContractEventTrigger();
    byte[] addressBytes = {0x01, 0x02, 0x03, 0x04};
    byte[] dataBytes = {0x10, 0x20, 0x30, 0x40};
    List<DataWord> topics = Arrays.asList(
        new DataWord("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"),
        new DataWord("0000000000000000000000000000000000000000000000000000000000000001"));

    RawData rawData = new RawData(addressBytes, topics, dataBytes);
    mockEventTrigger.setRawData(rawData);
    mockEventTrigger.setLatestSolidifiedBlockNumber(12345L);
    mockEventTrigger.setRemoved(false);
    mockEventTrigger.setUniqueId("unique-id");
    mockEventTrigger.setTransactionId("tx-id");
    mockEventTrigger.setContractAddress("contract-addr");
    mockEventTrigger.setOriginAddress("origin-addr");
    mockEventTrigger.setCreatorAddress("creator-addr");
    mockEventTrigger.setBlockNumber(67890L);
    mockEventTrigger.setTimeStamp(1622547200L);
    mockEventTrigger.setBlockHash("block-hash");
  }

  @Test
  public void testDefaultConstructor() {
    ContractLogTrigger trigger = new ContractLogTrigger();
    assertEquals(Trigger.CONTRACTLOG_TRIGGER_NAME, trigger.getTriggerName());
    assertNull(trigger.getTopicList());
    assertNull(trigger.getData());
  }

  @Test
  public void testConstructorWithEventTrigger() {
    ContractLogTrigger trigger = new ContractLogTrigger(mockEventTrigger);
    assertEquals(Trigger.CONTRACTLOG_TRIGGER_NAME, trigger.getTriggerName());
    assertEquals(mockEventTrigger.getRawData(), trigger.getRawData());
    assertEquals(mockEventTrigger.getLatestSolidifiedBlockNumber(),
        trigger.getLatestSolidifiedBlockNumber());
    assertEquals(mockEventTrigger.isRemoved(), trigger.isRemoved());
    assertEquals(mockEventTrigger.getUniqueId(), trigger.getUniqueId());
    assertEquals(mockEventTrigger.getTransactionId(), trigger.getTransactionId());
    assertEquals(mockEventTrigger.getContractAddress(), trigger.getContractAddress());
    assertEquals(mockEventTrigger.getOriginAddress(), trigger.getOriginAddress());
    assertEquals("", trigger.getCallerAddress()); // Explicitly set to empty string
    assertEquals(mockEventTrigger.getCreatorAddress(), trigger.getCreatorAddress());
    assertEquals(mockEventTrigger.getBlockNumber(), trigger.getBlockNumber());
    assertEquals(mockEventTrigger.getTimeStamp(), trigger.getTimeStamp());
    assertEquals(mockEventTrigger.getBlockHash(), trigger.getBlockHash());
  }

  @Test
  public void testSetAndGetTopicList() {
    ContractLogTrigger trigger = new ContractLogTrigger();
    List<String> topics = Arrays.asList("topic1", "topic2");
    trigger.setTopicList(topics);
    assertEquals(topics, trigger.getTopicList());
  }

  @Test
  public void testSetAndGetData() {
    ContractLogTrigger trigger = new ContractLogTrigger();
    String testData = "log data";
    trigger.setData(testData);
    assertEquals(testData, trigger.getData());
  }

}
