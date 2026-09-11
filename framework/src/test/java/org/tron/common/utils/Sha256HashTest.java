package org.tron.common.utils;

import static java.nio.file.Files.createTempFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.parameter.CommonParameter;

public class Sha256HashTest {

  @Test
  public void testHash() throws IOException {
    //Example from https://github.com/tronprotocol/tips/blob/master/TWP-001.md
    byte[] input = ByteArray.fromHexString("A0E11973395042BA3C0B52B4CDF4E15EA77818F275");
    byte[] hash0 = Sha256Hash.hash(CommonParameter
        .getInstance().isECKeyCryptoEngine(), input);
    byte[] hash1 = Sha256Hash.hash(CommonParameter
        .getInstance().isECKeyCryptoEngine(), hash0);
    assertEquals(Arrays.toString(hash0), Arrays.toString(ByteArray
        .fromHexString("CD5D4A7E8BE869C00E17F8F7712F41DBE2DDBD4D8EC36A7280CD578863717084")));
    assertEquals(Arrays.toString(hash1), Arrays.toString(ByteArray
        .fromHexString("10AE21E887E8FE30C591A22A5F8BB20EB32B2A739486DC5F3810E00BBDB58C5C")));

    Sha256Hash sha256Hash = new Sha256Hash(1, new byte[32]);
    assertNotNull(sha256Hash.toBigInteger());

    Sha256Hash.create(true, ("byte1-1").getBytes(StandardCharsets.UTF_8));
    File testfile = createTempFile("testfile", ".txt").toFile();
    Sha256Hash.of(true, testfile);
    Sha256Hash.createDouble(true, new byte[0]);
    Sha256Hash.twiceOf(true, new byte[0]);
    Sha256Hash.hashTwice(true, new byte[0]);
    Sha256Hash.hashTwice(false, new byte[0]);
    Sha256Hash.hashTwice(true, new byte[0], 0, 0);
    Sha256Hash.hashTwice(false, new byte[0], 0, 0);
    Sha256Hash.hash(false, new byte[0], 0, 0);
    Sha256Hash.hashTwice(true, new byte[0], 0, 0, new byte[0], 0, 0);
    Sha256Hash.hashTwice(false, new byte[0], 0, 0, new byte[0], 0, 0);
    assertTrue(testfile.delete());



  }

  @Test
  public void testMultiThreadingHash() throws Exception {
    byte[] input = ByteArray.fromHexString("A0E11973395042BA3C0B52B4CDF4E15EA77818F275");
    byte[] hash = ByteArray
        .fromHexString("CD5D4A7E8BE869C00E17F8F7712F41DBE2DDBD4D8EC36A7280CD578863717084");
    int workerCount = 7;
    CountDownLatch ready = new CountDownLatch(workerCount);
    ExecutorService executor = Executors.newFixedThreadPool(workerCount);
    List<Future<Integer>> futures = new ArrayList<>();
    try {
      for (int worker = 0; worker < workerCount; worker++) {
        futures.add(executor.submit(() -> {
          ready.countDown();
          assertTrue("Hash workers did not start", ready.await(10, TimeUnit.SECONDS));
          for (int i = 0; i < 10_000; i++) {
            if (Thread.currentThread().isInterrupted()) {
              throw new InterruptedException("Hash worker cancelled");
            }
            Assert.assertArrayEquals(hash, Sha256Hash.hash(true, input));
          }
          return 10_000;
        }));
      }
      int completed = 0;
      for (Future<Integer> future : futures) {
        completed += future.get(30, TimeUnit.SECONDS);
      }
      assertEquals(70_000, completed);
    } finally {
      executor.shutdownNow();
      assertTrue("Hash workers did not stop", executor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }
}
