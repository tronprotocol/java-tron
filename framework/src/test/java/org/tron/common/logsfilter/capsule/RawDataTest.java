package org.tron.common.logsfilter.capsule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.tron.common.runtime.vm.DataWord;

public class RawDataTest {
  @Test
  public void testRawDataConstructor() {
    byte[] addressBytes = {0x01, 0x02, 0x03, 0x04};
    byte[] dataBytes = {0x10, 0x20, 0x30, 0x40};
    List<DataWord> topics = Arrays.asList(
        new DataWord("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"),
        new DataWord("0000000000000000000000000000000000000000000000000000000000000001"));

    RawData rawData = new RawData(addressBytes, topics, dataBytes);

    assertEquals("01020304", rawData.getAddress());
    assertEquals(topics, rawData.getTopics());
    assertEquals("10203040", rawData.getData());

    rawData = new RawData(null, null, null);
    assertEquals("", rawData.getAddress());
    assertTrue(rawData.getTopics().isEmpty());
    assertEquals("", rawData.getData());
    assertNotNull(rawData.toString());
  }
}
