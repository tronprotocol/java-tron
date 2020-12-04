package org.tron.common.utils;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.tron.common.parameter.CommonParameter;

public class Sha256HashTest {

  @Test
  public void testHash() {
    //Example from https://github.com/tronprotocol/tips/blob/master/TWP-001.md
    byte[] input = ByteArray.fromHexString("A0E11973395042BA3C0B52B4CDF4E15EA77818F275");
    byte[] hash0 = Sha256Hash.hash(CommonParameter
        .getInstance().isECKeyCryptoEngine(), input);
    byte[] hash1 = Sha256Hash.hash(CommonParameter
        .getInstance().isECKeyCryptoEngine(), hash0);
    Assert.assertEquals(hash0, ByteArray
        .fromHexString("CD5D4A7E8BE869C00E17F8F7712F41DBE2DDBD4D8EC36A7280CD578863717084"));
    Assert.assertEquals(hash1, ByteArray
        .fromHexString("10AE21E887E8FE30C591A22A5F8BB20EB32B2A739486DC5F3810E00BBDB58C5C"));

  }

  @Test
  public void testMultiThreadingHash() {
    byte[] input = ByteArray.fromHexString("A0E11973395042BA3C0B52B4CDF4E15EA77818F275");
    byte[] hash = ByteArray
        .fromHexString("CD5D4A7E8BE869C00E17F8F7712F41DBE2DDBD4D8EC36A7280CD578863717084");
    AtomicLong countFailed = new AtomicLong(0);
    AtomicLong countAll = new AtomicLong(0);
    IntStream.range(0, 7).parallel().forEach(index -> {
      Thread thread =
          new Thread(() -> {
            for (int i = 0; i < 10000; i++) {
              byte[] hash0 = Sha256Hash.hash(CommonParameter.getInstance()
                  .isECKeyCryptoEngine(), input);
              countAll.incrementAndGet();
              if (!Arrays.equals(hash, hash0)) {
                countFailed.incrementAndGet();
                Assert.assertTrue(false);
              }
            }
          });
      thread.start();
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
    Assert.assertEquals(70000, countAll.get());
    Assert.assertEquals(0, countFailed.get());
  }
}