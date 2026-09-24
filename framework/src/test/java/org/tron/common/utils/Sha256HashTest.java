package org.tron.common.utils;

import static java.nio.file.Files.createTempFile;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import org.junit.Assert;
import org.junit.Test;

public class Sha256HashTest {

  // ECKey vectors captured from 4d6c24085ab151d8c7ef821ff0e7ad2b7f168733,
  // using the old hashTwice(true, ...) overloads. Expected values must stay literal.
  private static final byte[] HASH_INPUT =
      ByteArray.fromHexString("ff00112233445566778899aabbccddeeffee");

  @Test
  public void testHash() throws IOException {
    //Example from https://github.com/tronprotocol/tips/blob/master/TWP-001.md
    byte[] input = ByteArray.fromHexString("A0E11973395042BA3C0B52B4CDF4E15EA77818F275");
    byte[] hash0 = Sha256Hash.hash(input);
    byte[] hash1 = Sha256Hash.hash(hash0);
    assertEquals(Arrays.toString(hash0), Arrays.toString(ByteArray
        .fromHexString("CD5D4A7E8BE869C00E17F8F7712F41DBE2DDBD4D8EC36A7280CD578863717084")));
    assertEquals(Arrays.toString(hash1), Arrays.toString(ByteArray
        .fromHexString("10AE21E887E8FE30C591A22A5F8BB20EB32B2A739486DC5F3810E00BBDB58C5C")));

    Sha256Hash sha256Hash = new Sha256Hash(1, new byte[32]);
    assertNotNull(sha256Hash.toBigInteger());

    Sha256Hash.create(("byte1-1").getBytes(StandardCharsets.UTF_8));
    File testfile = createTempFile("testfile", ".txt").toFile();
    Sha256Hash.of(testfile);
    assertTrue(testfile.delete());
  }

  @Test
  public void testHashTwiceEmpty() {
    byte[] expected = ByteArray.fromHexString(
        "5df6e0e2761359d30a8275058e299fcc0381534545f55cf43e41983f5d4c9456");
    assertArrayEquals(expected, Sha256Hash.hashTwice(new byte[0]));
    assertArrayEquals(expected, Sha256Hash.hashTwice(new byte[0], 0, 0));
    assertArrayEquals(expected,
        Sha256Hash.hashTwice(new byte[0], 0, 0, new byte[0], 0, 0));
    assertArrayEquals(expected, Sha256Hash.createDouble(new byte[0]).getBytes());
    assertArrayEquals(expected, Sha256Hash.twiceOf(new byte[0]).getBytes());
    assertArrayEquals(ByteArray.fromHexString(
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),
        Sha256Hash.hash(new byte[0], 0, 0));
  }

  @Test
  public void testHashTwiceVector() {
    assertArrayEquals(ByteArray.fromHexString(
        "67c6edabfc5925c15fa2ab4dbc835c92f2f47a6aeb715d56fc1b19e096d18c67"),
        Sha256Hash.hashTwice(HASH_INPUT));
  }

  @Test
  public void testHashTwiceSliceVector() {
    // Hash only 112233445566778899, excluding bytes on both sides of the range.
    assertArrayEquals(ByteArray.fromHexString(
        "7a489568b05b35bcb012cf017a812486bc03dc63af12d7b37c89b212402d1370"),
        Sha256Hash.hashTwice(HASH_INPUT, 2, 9));
  }

  @Test
  public void testHashTwiceTwoSlicesVector() {
    byte[] second = ByteArray.fromHexString("dd102030405060708090aabbcc");
    // Hash 112233445566778899 || 3040506070, preserving range and input order.
    assertArrayEquals(ByteArray.fromHexString(
        "1fa4c3ffcb0fe12aaf5d87443b445637e2796cc35171e749fafc6d9c726a6c87"),
        Sha256Hash.hashTwice(HASH_INPUT, 2, 9, second, 3, 5));
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
              byte[] hash0 = Sha256Hash.hash(input);
              countAll.incrementAndGet();
              if (!Arrays.equals(hash, hash0)) {
                countFailed.incrementAndGet();
                Assert.fail();
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
    assertEquals(70000, countAll.get());
    assertEquals(0, countFailed.get());
  }
}
