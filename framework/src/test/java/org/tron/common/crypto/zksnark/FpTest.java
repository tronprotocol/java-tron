package org.tron.common.crypto.zksnark;

import java.math.BigInteger;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public class FpTest {

  @Test
  public void testFieldInitializationWithoutCurveParameters() throws Exception {
    for (Class<?> field : new Class<?>[]{Fp.class, Fp2.class, Fp6.class, Fp12.class}) {
      assertInitializesWithoutCurveParameters(field.getName());
    }
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorRejectsNull() {
    new Fp((BigInteger) null);
  }

  @Test(expected = NullPointerException.class)
  public void testFactoryRejectsNull() {
    Fp.create((BigInteger) null);
  }

  @Test
  public void testEquals() {
    Fp one = Fp.create(BigInteger.ONE);
    Fp sameValue = new Fp(BigInteger.ONE);
    Fp differentValue = Fp.create(BigInteger.valueOf(2));

    Assert.assertTrue(one.equals(one));
    Assert.assertTrue(one.equals(sameValue));
    Assert.assertTrue(sameValue.equals(one));
    Assert.assertEquals(one.hashCode(), sameValue.hashCode());
    Assert.assertFalse(one.equals(differentValue));
    Assert.assertFalse(differentValue.equals(one));
    Assert.assertFalse(one.equals(null));
    Assert.assertFalse(one.equals(BigInteger.ONE));
  }

  @Test
  public void testRejectsNegativeValues() {
    Assert.assertFalse(new Fp(BigInteger.valueOf(-1)).isValid());
    Assert.assertFalse(Fp.create(BigInteger.valueOf(-1)).isValid());
    Assert.assertFalse(Fp.create(Params.P.negate()).isValid());
  }

  @Test
  public void testValidRange() {
    Assert.assertTrue(Fp.create(BigInteger.ZERO).isValid());
    Assert.assertTrue(Fp.create(BigInteger.ONE).isValid());
    Assert.assertTrue(Fp.create(Params.P.subtract(BigInteger.ONE)).isValid());
    Assert.assertFalse(Fp.create(Params.P).isValid());
    Assert.assertFalse(Fp.create(Params.P.add(BigInteger.ONE)).isValid());
  }

  @Test
  public void testRejectsNegativeExtensionCoefficients() {
    Assert.assertFalse(Fp2.create(BigInteger.valueOf(-1), BigInteger.ZERO).isValid());
    Assert.assertFalse(Fp2.create(BigInteger.ZERO, BigInteger.valueOf(-1)).isValid());
  }

  @Test
  public void testUnsignedByteInput() {
    Fp value = Fp.create(new byte[]{(byte) 0xff});

    Assert.assertEquals(Fp.create(BigInteger.valueOf(255)), value);
    Assert.assertTrue(value.isValid());

    byte[] maxWord = new byte[32];
    Arrays.fill(maxWord, (byte) 0xff);
    Assert.assertFalse(Fp.create(maxWord).isValid());
  }

  @Test
  public void testRejectsOutOfRangeCoordinates() {
    byte[] one = BigInteger.ONE.toByteArray();
    byte[] two = BigInteger.valueOf(2).toByteArray();
    byte[] outOfRange = Params.P.toByteArray();

    Assert.assertNotNull(BN128Fp.create(one, two));
    Assert.assertNull(BN128Fp.create(outOfRange, two));
    Assert.assertNull(BN128Fp.create(one, outOfRange));
  }

  private void assertInitializesWithoutCurveParameters(String name) throws Exception {
    URL location = Fp.class.getProtectionDomain().getCodeSource().getLocation();
    // A fresh loader exercises first use even if another test already initialized these classes.
    // Reject Params to detect the dependency that caused the initialization cycle.
    try (URLClassLoader loader = new URLClassLoader(new URL[]{location}, null) {
      @Override
      protected Class<?> loadClass(String className, boolean resolve)
          throws ClassNotFoundException {
        if (className.equals(Params.class.getName())) {
          throw new ClassNotFoundException("Field initialization must not load Params");
        }
        return super.loadClass(className, resolve);
      }
    }) {
      Assert.assertNotNull(Class.forName(name, true, loader));
    }
  }
}
