package org.tron.core.capsule.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Random;
import org.bouncycastle.util.BigIntegers;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.utils.Value;

public class RLPListTest {

  @Test
  public void testRecursivePrint() {
    RLPItem element = new RLPItem("rlpItem".getBytes());
    Assert.assertEquals(new String(element.getRLPData()), "rlpItem");
    RLPList.recursivePrint(element);
    RLPList rlpList = new RLPList();
    rlpList.add(new RLPItem("rlpItem0".getBytes()));
    RLPList.recursivePrint(rlpList);
    Assert.assertThrows(RuntimeException.class, () -> RLPList.recursivePrint(null));
  }

  @Test
  public void testGetRLPData() {
    RLPList rlpList = new RLPList();
    rlpList.setRLPData("rlpData".getBytes());
    Assert.assertEquals(new String(rlpList.getRLPData()), "rlpData");
  }

  @Test
  public void testToBytes()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method = RLP.class.getDeclaredMethod("toBytes", Object.class);
    method.setAccessible(true);

    byte[] aBytes = new byte[10];
    byte[] bBytes = (byte[]) method.invoke(RLP.class, aBytes);
    Assert.assertArrayEquals(aBytes, bBytes);

    int i = new Random().nextInt();
    byte[] cBytes = BigIntegers.asUnsignedByteArray(BigInteger.valueOf(i));
    byte[] dBytes = (byte[]) method.invoke(RLP.class, i);
    Assert.assertArrayEquals(cBytes, dBytes);

    long j = new Random().nextInt();
    byte[] eBytes = BigIntegers.asUnsignedByteArray(BigInteger.valueOf(j));
    byte[] fBytes = (byte[]) method.invoke(RLP.class, j);
    Assert.assertArrayEquals(eBytes, fBytes);

    String test = "testA";
    byte[] gBytes = test.getBytes();
    byte[] hBytes = (byte[]) method.invoke(RLP.class, test);
    Assert.assertArrayEquals(gBytes, hBytes);

    BigInteger bigInteger = BigInteger.valueOf(100);
    byte[] iBytes = BigIntegers.asUnsignedByteArray(bigInteger);
    byte[] jBytes = (byte[]) method.invoke(RLP.class, bigInteger);
    Assert.assertArrayEquals(iBytes, jBytes);

    Value v = new Value(new byte[0]);
    byte[] kBytes = v.asBytes();
    byte[] lBytes = (byte[]) method.invoke(RLP.class, v);
    Assert.assertArrayEquals(kBytes, lBytes);

    char c = 'a';
    try {
      method.invoke(RLP.class, c);
      Assert.fail();
    } catch (Exception e) {
      Assert.assertTrue(true);
    }
  }

  @Test
  public void testEncode() {
    byte[] aBytes = RLP.encode(new byte[1]);
    Assert.assertEquals(1, aBytes.length);
  }
}
