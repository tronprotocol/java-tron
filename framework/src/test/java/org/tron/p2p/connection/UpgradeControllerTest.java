package org.tron.p2p.connection;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.upgrade.UpgradeController;
import org.tron.p2p.exception.P2pException;

public class UpgradeControllerTest {

  @Before
  public void setUp() {
    Parameter.version = 1;
  }

  @Test
  public void testCodeSendDataNoCompressVersion0() throws IOException {
    byte[] data = new byte[]{0x01, 0x02, 0x03};
    // version 0 should not compress
    byte[] result = UpgradeController.codeSendData(0, data);
    Assert.assertArrayEquals(data, result);
  }

  @Test
  public void testCodeSendDataCompressVersion1() throws IOException {
    byte[] data = new byte[]{0x01, 0x02, 0x03};
    byte[] result = UpgradeController.codeSendData(1, data);
    // result should be different from input when compressed
    Assert.assertNotNull(result);
    Assert.assertTrue(result.length > 0);
  }

  @Test
  public void testDecodeReceiveDataNoCompressVersion0()
      throws P2pException, IOException {
    byte[] data = new byte[]{0x01, 0x02, 0x03};
    byte[] result = UpgradeController.decodeReceiveData(0, data);
    Assert.assertArrayEquals(data, result);
  }

  @Test
  public void testCodeAndDecodeRoundTrip() throws P2pException, IOException {
    byte[] original = new byte[]{0x0A, 0x0B, 0x0C, 0x0D};
    byte[] encoded = UpgradeController.codeSendData(1, original);
    byte[] decoded = UpgradeController.decodeReceiveData(1, encoded);
    Assert.assertArrayEquals(original, decoded);
  }

  @Test
  public void testDecodeReceiveDataBadData() throws Exception {
    // Construct bytes that will fail protobuf parsing or snappy decompression
    // Use a byte array that looks like a valid protobuf CompressMessage
    // but has corrupted snappy data
    byte[] badData = new byte[]{
        0x08, 0x01,  // field 1 (type), value 1 (snappy)
        0x12, 0x03,  // field 2 (data), length 3
        0x01, 0x02, 0x03  // invalid snappy data
    };
    try {
      UpgradeController.decodeReceiveData(1, badData);
      Assert.fail("Expected exception for bad snappy data");
    } catch (Exception e) {
      // Expected: IOException from Snappy or P2pException
      Assert.assertTrue(
          e instanceof IOException || e instanceof P2pException);
    }
  }

  @Test
  public void testNoCompressWhenParameterVersion0() throws IOException {
    Parameter.version = 0;
    byte[] data = new byte[]{0x01, 0x02, 0x03};
    byte[] result = UpgradeController.codeSendData(1, data);
    Assert.assertArrayEquals(data, result);
    Parameter.version = 1;
  }
}
