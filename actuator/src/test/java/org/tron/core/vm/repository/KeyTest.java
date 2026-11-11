package org.tron.core.vm.repository;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tron.core.vm.config.VMConfig;

public class KeyTest {

  @Before
  public void setUp() {
    setResourceOptimization(false);
  }

  @After
  public void tearDown() {
    VMConfig.clearLocalSnapshot();
  }

  @Test
  public void legacyHashIsPreservedBeforeProposal() {
    assertEquals(ArrayUtils.hashCode(new byte[0]), Key.create(null).hashCode());
    for (int length = 0; length <= 257; length++) {
      byte[] bytes = payload(length);
      assertEquals(ArrayUtils.hashCode(bytes), Key.create(bytes).hashCode());
    }
  }

  @Test
  public void keyDoesNotImplementComparable() {
    assertFalse(Comparable.class.isAssignableFrom(Key.class));
  }

  @Test
  public void proposalEnablesOptimizedHash() {
    setResourceOptimization(true);
    for (int length = 1; length <= 257; length++) {
      byte[] bytes = payload(length);
      if (Key.create(bytes).hashCode() != ArrayUtils.hashCode(bytes)) {
        return;
      }
    }
    throw new AssertionError("The enabled proposal still uses the legacy hash for every sample");
  }

  @Test
  public void hashMapRoundTripsBothModesAndPartialWords() {
    int[] lengths = {0, 1, 2, 3, 4, 7, 8, 15, 16, 20, 21, 31, 32, 33, 52, 53,
        63, 64, 65, 75, 76, 77, 78, 79, 80, 81, 127, 128, 129, 255, 256, 257, 1025};
    for (boolean enabled : new boolean[]{false, true}) {
      setResourceOptimization(enabled);
      Map<Key, Integer> map = new HashMap<>();
      for (int length : lengths) {
        Key key = Key.create(payload(length));
        map.put(key, length);
        assertEquals(key, key.clone());
        assertEquals(key.hashCode(), key.clone().hashCode());
      }
      for (int length : lengths) {
        Key again = Key.create(payload(length));
        assertEquals(Integer.valueOf(length), map.get(again));
        assertEquals(again.hashCode(), Key.create(payload(length)).hashCode());
      }
      assertEquals(Key.create(null), Key.create(new byte[0]));
      assertEquals(Integer.valueOf(0), map.get(Key.create(null)));
    }
  }

  @Test
  public void proposalFlagIsReadAtHashTimeWithoutChangingEquality() {
    byte[] bytes = payload(53);
    Key beforeProposal = Key.create(bytes);
    int legacyHash = beforeProposal.hashCode();

    setResourceOptimization(true);
    Key afterProposal = Key.create(bytes);
    int optimizedHash = afterProposal.hashCode();
    assertEquals(beforeProposal, afterProposal);
    assertEquals(afterProposal, beforeProposal);
    assertEquals(optimizedHash, beforeProposal.hashCode());
    assertEquals(beforeProposal, beforeProposal.clone());
    assertEquals(optimizedHash, beforeProposal.clone().hashCode());

    setResourceOptimization(false);
    assertEquals(legacyHash, beforeProposal.hashCode());
    assertEquals(legacyHash, afterProposal.hashCode());
    assertEquals(legacyHash, afterProposal.clone().hashCode());
    assertEquals(beforeProposal, Key.create(bytes));

    setResourceOptimization(true);
    assertEquals(optimizedHash, beforeProposal.hashCode());
    assertEquals(optimizedHash, afterProposal.hashCode());
  }

  @Test
  public void finalWordLowBitsDoNotCollapseIntoOneBucket() {
    // The old 64-bit recurrence left these bytes unmixed before truncating to the high half.
    int count = 1 << 16;
    for (int seed = 0; seed < 16; seed++) {
      TestHash hash = new TestHash(seed);
      Set<Integer> hashes = new HashSet<>();
      byte[] slot = new byte[32];
      for (int value = 0; value < count; value++) {
        slot[24] = (byte) value;
        slot[25] = (byte) (value >>> 8);
        hashes.add(hash.hash(slot));
      }
      // Hash collisions are legal; reject a collapsed family rather than require uniqueness.
      assertTrue("seed=" + seed + ", hashes=" + hashes.size(), hashes.size() > count * 99 / 100);
    }
  }

  @Test
  public void evenHighBitFlipsDoNotCancelAcrossWords() {
    for (int seed = 0; seed < 16; seed++) {
      TestHash hash = new TestHash(seed);
      Set<Integer> hashes = new HashSet<>();
      for (int mask = 0; mask < 16; mask++) {
        if ((Integer.bitCount(mask) & 1) != 0) {
          continue;
        }
        byte[] slot = new byte[32];
        for (int word = 0; word < 4; word++) {
          if ((mask & (1 << word)) != 0) {
            slot[word * 8 + 7] = (byte) 0x80;
          }
        }
        hashes.add(hash.hash(slot));
      }
      // All eight slots used to collide for every odd 64-bit coefficient set.
      assertTrue("seed=" + seed, hashes.size() > 1);
    }
  }

  @Test
  public void everyWordAndTailAffectsHash() {
    for (int seed = 0; seed < 4; seed++) {
      TestHash hash = new TestHash(seed);
      for (int length : new int[]{16, 19, 21, 23, 32, 42, 43, 46, 53, 65, 75, 76, 79, 80}) {
        for (int offset = 0; offset < length; offset += 4) {
          byte[] bytes = payload(length);
          Set<Integer> hashes = new HashSet<>();
          for (int value = 0; value < 256; value++) {
            bytes[offset] = (byte) value;
            hashes.add(hash.hash(bytes));
          }
          assertTrue("seed=" + seed + ", length=" + length + ", offset=" + offset,
              hashes.size() > 250);
        }
      }
    }
  }

  @Test
  public void lengthSeparatesZeroPaddedKeys() {
    for (int seed = 0; seed < 16; seed++) {
      TestHash hash = new TestHash(seed);
      Set<Integer> hashes = new HashSet<>();
      for (int length = 1; length <= 129; length++) {
        byte[] bytes = new byte[length];
        bytes[0] = 1;
        hashes.add(hash.hash(bytes));
      }
      assertTrue("seed=" + seed, hashes.size() > 125);
    }
  }

  @Test
  public void longKeysUseLegacyHashAndIncludeTheWholeTail() {
    setResourceOptimization(true);
    TestHash hash = new TestHash(42);
    for (int length : new int[]{81, 82, 83, 84, 128, 129, 257, 1025, 4097}) {
      byte[] bytes = payload(length);
      Key key = Key.create(bytes);
      int original = key.hashCode();
      assertEquals(ArrayUtils.hashCode(bytes), original);
      assertEquals(original, hash.hash(bytes));
      bytes[length - 1] ^= 1;
      Key changedTail = Key.create(bytes);
      assertEquals(ArrayUtils.hashCode(bytes), changedTail.hashCode());
      assertNotEquals(original, changedTail.hashCode());
      assertEquals(original, key.hashCode());
    }
  }

  @Test
  public void transientStorageChildCommitAndDiscardWorkInBothModes() {
    byte[] address = payload(32);
    for (boolean enabled : new boolean[]{false, true}) {
      setResourceOptimization(enabled);
      Repository root = new RepositoryImpl(null, null);
      Repository child = root.newRepositoryChild();
      for (int value = 0; value < 512; value++) {
        byte[] slot = new byte[32];
        slot[24] = (byte) value;
        slot[25] = (byte) (value >>> 8);
        byte[] data = payload(32);
        data[0] = (byte) value;
        data[1] = (byte) (value >>> 8);
        child.updateTransientStorageValue(address, slot, data);
        assertArrayEquals(data, child.getTransientStorageValue(address, slot));
        assertNull(root.getTransientStorageValue(address, slot));
      }
      child.commit();
      for (int value = 0; value < 512; value++) {
        byte[] slot = new byte[32];
        slot[24] = (byte) value;
        slot[25] = (byte) (value >>> 8);
        byte[] data = payload(32);
        data[0] = (byte) value;
        data[1] = (byte) (value >>> 8);
        assertArrayEquals(data, root.getTransientStorageValue(address, slot));
        Repository discarded = root.newRepositoryChild();
        discarded.updateTransientStorageValue(address, slot, new byte[32]);
        assertArrayEquals(data, root.getTransientStorageValue(address, slot));
      }
    }
  }

  private static void setResourceOptimization(boolean enabled) {
    VMConfig.Snapshot snapshot = new VMConfig.Snapshot();
    snapshot.allowTvmResourceOptimization = enabled;
    VMConfig.setLocalSnapshot(snapshot);
  }

  private static class TestHash {

    private final long[] seeds = new long[20];
    private final long lengthSeed;

    private TestHash(long seed) {
      Random random = new Random(seed);
      lengthSeed = random.nextLong();
      for (int i = 0; i < seeds.length; i++) {
        seeds[i] = random.nextLong();
      }
    }

    private int hash(byte[] bytes) {
      return Key.optimizedHash(bytes, seeds, lengthSeed);
    }
  }

  private static byte[] payload(int length) {
    byte[] data = new byte[length];
    for (int i = 0; i < length; i++) {
      data[i] = (byte) (i * 17 + 3);
    }
    return data;
  }
}
