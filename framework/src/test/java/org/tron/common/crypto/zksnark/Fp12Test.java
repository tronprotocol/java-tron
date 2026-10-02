package org.tron.common.crypto.zksnark;

import java.math.BigInteger;
import org.junit.Assert;
import org.junit.Test;

public class Fp12Test {

  @Test
  public void testDblZero() {
    Assert.assertEquals(Fp12.ZERO, Fp12.ZERO.dbl());
  }

  @Test
  public void testDblOne() {
    Assert.assertEquals(element(2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), Fp12._1.dbl());
  }

  @Test
  public void testDblAllCoefficientsWithoutMutatingInput() {
    Fp12 value = element(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

    Fp12 doubled = value.dbl();

    Assert.assertEquals(element(2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24), doubled);
    Assert.assertNotSame(value, doubled);
    Assert.assertEquals(element(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), value);
  }

  @Test
  public void testDblReducesAllCoefficientsModuloP() {
    // Negative inputs to element() represent canonical residues near P, e.g. -1 means P - 1.
    Fp12 value = element(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12);

    Fp12 doubled = value.dbl();

    Assert.assertEquals(element(-2, -4, -6, -8, -10, -12, -14, -16, -18, -20, -22, -24),
        doubled);
    Assert.assertTrue(doubled.isValid());
    Assert.assertEquals(element(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12), value);
  }

  private static Fp12 element(long... coefficients) {
    Assert.assertEquals(12, coefficients.length);
    Fp2[] pairs = new Fp2[6];
    for (int i = 0; i < pairs.length; i++) {
      pairs[i] = new Fp2(BigInteger.valueOf(coefficients[2 * i]).mod(Params.P),
          BigInteger.valueOf(coefficients[2 * i + 1]).mod(Params.P));
    }
    return new Fp12(new Fp6(pairs[0], pairs[1], pairs[2]),
        new Fp6(pairs[3], pairs[4], pairs[5]));
  }
}
