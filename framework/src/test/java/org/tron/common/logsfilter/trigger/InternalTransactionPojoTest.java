package org.tron.common.logsfilter.trigger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class InternalTransactionPojoTest {
  private InternalTransactionPojo internalTransactionPojo;

  @Before
  public void setUp() {
    internalTransactionPojo = new InternalTransactionPojo();
  }

  @Test
  public void testHash() {
    String testHash = "0x123456789abcdef0123456789abcdef0";
    internalTransactionPojo.setHash(testHash);
    assertEquals(testHash, internalTransactionPojo.getHash());
  }

  @Test
  public void testCallValue() {
    long testCallValue = 123456789L;
    internalTransactionPojo.setCallValue(testCallValue);
    assertEquals(testCallValue, internalTransactionPojo.getCallValue());
  }

  @Test
  public void testTokenInfo() {
    Map<String, Long> testTokenInfo = new HashMap<>();
    testTokenInfo.put("token1", 100L);
    testTokenInfo.put("token2", 200L);
    internalTransactionPojo.setTokenInfo(testTokenInfo);
    assertEquals(testTokenInfo, internalTransactionPojo.getTokenInfo());
  }

  @Test
  public void testTransferToAddress() {
    String testAddress = "0x0000000000000000000000000000000000000001";
    internalTransactionPojo.setTransferTo_address(testAddress);
    assertEquals(testAddress, internalTransactionPojo.getTransferTo_address());
  }

  @Test
  public void testData() {
    String testData = "0x6060604052341561000f57600080fd5b5b6040516020806101158339810160405280805"
        + "19060200190929190505050";
    internalTransactionPojo.setData(testData);
    assertEquals(testData, internalTransactionPojo.getData());
  }

  @Test
  public void testCallerAddress() {
    String testCallerAddress = "0x0000000000000000000000000000000000000002";
    internalTransactionPojo.setCaller_address(testCallerAddress);
    assertEquals(testCallerAddress, internalTransactionPojo.getCaller_address());
  }

  @Test
  public void testRejected() {
    internalTransactionPojo.setRejected(true);
    assertTrue(internalTransactionPojo.isRejected());

    internalTransactionPojo.setRejected(false);
    assertFalse(internalTransactionPojo.isRejected());
  }

  @Test
  public void testNote() {
    String testNote = "This is a test note";
    internalTransactionPojo.setNote(testNote);
    assertEquals(testNote, internalTransactionPojo.getNote());
  }

  @Test
  public void testExtra() {
    String testExtra = "extra_data_for_vote_witness";
    internalTransactionPojo.setExtra(testExtra);
    assertEquals(testExtra, internalTransactionPojo.getExtra());
  }
}
