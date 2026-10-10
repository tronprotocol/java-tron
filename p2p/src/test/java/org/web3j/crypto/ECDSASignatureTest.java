package org.web3j.crypto;

import java.math.BigInteger;
import org.junit.Assert;
import org.junit.Test;

public class ECDSASignatureTest {

  @Test
  public void lowSValueIsAlreadyCanonical() {
    ECDSASignature low = new ECDSASignature(BigInteger.ONE, BigInteger.TEN);
    Assert.assertTrue(low.isCanonical());
    // Nothing to adjust, so the same instance comes back.
    Assert.assertSame(low, low.toCanonicalised());
  }

  @Test
  public void halfCurveOrderIsStillCanonical() {
    ECDSASignature edge = new ECDSASignature(BigInteger.ONE, Sign.HALF_CURVE_ORDER);
    Assert.assertTrue(edge.isCanonical());
    Assert.assertSame(edge, edge.toCanonicalised());
  }

  @Test
  public void highSValueIsFlippedIntoTheLowerHalf() {
    BigInteger highS = Sign.HALF_CURVE_ORDER.add(BigInteger.ONE);
    ECDSASignature high = new ECDSASignature(BigInteger.ONE, highS);
    Assert.assertFalse(high.isCanonical());

    ECDSASignature canonical = high.toCanonicalised();
    Assert.assertNotSame(high, canonical);
    Assert.assertTrue(canonical.isCanonical());
    Assert.assertEquals(BigInteger.ONE, canonical.r);
    Assert.assertEquals(Sign.CURVE.getN().subtract(highS), canonical.s);
    // Canonicalising twice is a no-op.
    Assert.assertSame(canonical, canonical.toCanonicalised());
  }
}
