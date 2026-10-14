package org.tron.p2p.connection.business.upgrade;

import java.util.Arrays;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.exception.P2pException;
import org.tron.p2p.exception.P2pException.TypeEnum;

/**
 * Compression is negotiated per peer: it applies only when both this node's
 * Parameter.version and the peer's advertised version are at least 1.
 */
public class UpgradeControllerTest {

  private static final byte[] COMPRESSIBLE = new byte[4096];
  private int savedVersion;

  @Before
  public void setUp() {
    savedVersion = Parameter.version;
    Arrays.fill(COMPRESSIBLE, (byte) 'a');
  }

  @After
  public void tearDown() {
    Parameter.version = savedVersion;
  }

  @Test
  public void legacyPeerGetsUntouchedBytes() throws Exception {
    Parameter.version = 1;
    byte[] encoded = UpgradeController.codeSendData(0, COMPRESSIBLE);
    Assert.assertSame(COMPRESSIBLE, encoded);
    Assert.assertSame(COMPRESSIBLE, UpgradeController.decodeReceiveData(0, COMPRESSIBLE));
  }

  @Test
  public void legacyLocalVersionGetsUntouchedBytes() throws Exception {
    Parameter.version = 0;
    Assert.assertSame(COMPRESSIBLE, UpgradeController.codeSendData(1, COMPRESSIBLE));
    Assert.assertSame(COMPRESSIBLE, UpgradeController.decodeReceiveData(1, COMPRESSIBLE));
  }

  @Test
  public void upgradedPeerRoundTripsThroughCompression() throws Exception {
    Parameter.version = 1;
    byte[] encoded = UpgradeController.codeSendData(1, COMPRESSIBLE);
    // Highly repetitive input must actually shrink, otherwise the wrapper is
    // adding framing for nothing.
    Assert.assertTrue(encoded.length < COMPRESSIBLE.length);
    Assert.assertArrayEquals(COMPRESSIBLE, UpgradeController.decodeReceiveData(1, encoded));
  }

  @Test
  public void incompressiblePayloadStillRoundTrips() throws Exception {
    Parameter.version = 1;
    byte[] tiny = new byte[] {1, 2, 3};
    byte[] encoded = UpgradeController.codeSendData(1, tiny);
    Assert.assertArrayEquals(tiny, UpgradeController.decodeReceiveData(1, encoded));
  }

  @Test
  public void malformedFrameBecomesAParseFailure() {
    Parameter.version = 1;
    try {
      UpgradeController.decodeReceiveData(1, new byte[] {(byte) 0xFF, (byte) 0xFF, 0x7F});
      Assert.fail("expected a P2pException");
    } catch (P2pException e) {
      Assert.assertEquals(TypeEnum.PARSE_MESSAGE_FAILED, e.getType());
    } catch (Exception e) {
      Assert.fail("expected a P2pException, got " + e);
    }
  }
}
