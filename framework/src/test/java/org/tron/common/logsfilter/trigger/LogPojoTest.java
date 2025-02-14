package org.tron.common.logsfilter.trigger;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class LogPojoTest {

  private LogPojo logPojo;

  @Before
  public void setUp() {
    logPojo = new LogPojo();
  }

  @Test
  public void testAddress() {
    String testAddress = "123 Test Address";
    logPojo.setAddress(testAddress);
    assertEquals(testAddress, logPojo.getAddress());
  }

  @Test
  public void testBlockHash() {
    String testBlockHash = "abcdef1234567890abcdef1234567890abcdef12";
    logPojo.setBlockHash(testBlockHash);
    assertEquals(testBlockHash, logPojo.getBlockHash());
  }

  @Test
  public void testBlockNumber() {
    long testBlockNumber = 1234567L;
    logPojo.setBlockNumber(testBlockNumber);
    assertEquals(testBlockNumber, logPojo.getBlockNumber());
  }

  @Test
  public void testData() {
    String testData = "Some data here";
    logPojo.setData(testData);
    assertEquals(testData, logPojo.getData());
  }

  @Test
  public void testLogIndex() {
    long testLogIndex = 5L;
    logPojo.setLogIndex(testLogIndex);
    assertEquals(testLogIndex, logPojo.getLogIndex());
  }

  @Test
  public void testTopicList() {
    List<String> testTopicList = Arrays.asList("topic1", "topic2", "topic3");
    logPojo.setTopicList(testTopicList);
    assertEquals(testTopicList, logPojo.getTopicList());
  }

  @Test
  public void testTransactionHash() {
    String testTransactionHash = "abcdef1234567890abcdef1234567890abcdef12";
    logPojo.setTransactionHash(testTransactionHash);
    assertEquals(testTransactionHash, logPojo.getTransactionHash());
  }

  @Test
  public void testTransactionIndex() {
    long testTransactionIndex = 3L;
    logPojo.setTransactionIndex(testTransactionIndex);
    assertEquals(testTransactionIndex, logPojo.getTransactionIndex());
  }
}
