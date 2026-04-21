/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.tire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Assert;
import org.junit.Test;
import org.tron.core.capsule.utils.FastByteComparisons;
import org.tron.core.capsule.utils.RLP;
import org.tron.core.trie.TrieImpl;
import org.tron.core.trie.TrieImpl.Node;

public class TrieTest {

  private static String c = "c";
  private static String ca = "ca";
  private static String cat = "cat";
  private static String dog = "dog";
  private static String doge = "doge";
  private static String test = "test";
  private static String dude = "dude";
  private static final long SHUFFLE_SEED = 0xC0FFEEL;

  @Test
  public void test() {
    TrieImpl trie = new TrieImpl();
    trie.put(new byte[]{1}, c.getBytes());
    Assert.assertArrayEquals(trie.get(RLP.encodeInt(1)), c.getBytes());
    trie.put(new byte[]{1, 0}, ca.getBytes());
    trie.put(new byte[]{1, 1}, cat.getBytes());
    trie.put(new byte[]{1, 2}, dog.getBytes());
    trie.put(RLP.encodeInt(5), doge.getBytes());
    trie.put(RLP.encodeInt(6), doge.getBytes());
    trie.put(RLP.encodeInt(7), doge.getBytes());
    trie.put(RLP.encodeInt(11), doge.getBytes());
    trie.put(RLP.encodeInt(12), dude.getBytes());
    trie.put(RLP.encodeInt(13), test.getBytes());
    trie.delete(RLP.encodeInt(3));
    byte[] rootHash = trie.getRootHash();
    TrieImpl trieCopy = new TrieImpl(trie.getCache(), rootHash);
    Assert.assertNull(trie.prove(RLP.encodeInt(111)));
    Map<byte[], Node> map = trieCopy.prove(new byte[]{1, 1});
    boolean result = trie
        .verifyProof(trieCopy.getRootHash(), new byte[]{1, 1}, (LinkedHashMap<byte[], Node>) map);
    Assert.assertTrue(result);
    assertTrue(RLP.encodeInt(5), trieCopy);
    assertTrue(RLP.encodeInt(5), RLP.encodeInt(6), trieCopy);
    assertTrue(RLP.encodeInt(6), trieCopy);
    assertTrue(RLP.encodeInt(6), RLP.encodeInt(5), trieCopy);
    //
    trie.put(RLP.encodeInt(5), doge.getBytes());
    byte[] rootHash2 = trie.getRootHash();
    Assert.assertArrayEquals(rootHash, rootHash2);
    trieCopy = new TrieImpl(trie.getCache(), rootHash2);
    //
    assertTrue(RLP.encodeInt(5), trieCopy);
    assertTrue(RLP.encodeInt(5), RLP.encodeInt(6), trieCopy);
    assertTrue(RLP.encodeInt(6), trieCopy);
    assertTrue(RLP.encodeInt(6), RLP.encodeInt(5), trieCopy);
  }

  @Test
  public void test1() {
    TrieImpl trie = new TrieImpl();
    int n = 100;
    for (int i = 1; i < n; i++) {
      trie.put(RLP.encodeInt(i), String.valueOf(i).getBytes());
    }
    byte[] rootHash1 = trie.getRootHash();

    TrieImpl trie2 = new TrieImpl();
    for (int i = 1; i < n; i++) {
      trie2.put(RLP.encodeInt(i), String.valueOf(i).getBytes());
    }
    byte[] rootHash2 = trie2.getRootHash();
    Assert.assertArrayEquals(rootHash1, rootHash2);
  }

  @Test
  public void test2() {
    TrieImpl trie = new TrieImpl();
    int n = 100;
    for (int i = 1; i < n; i++) {
      trie.put(RLP.encodeInt(i), String.valueOf(i).getBytes());
    }
    byte[] rootHash = trie.getRootHash();
    TrieImpl trieCopy = new TrieImpl(trie.getCache(), rootHash);
    for (int i = 1; i < n; i++) {
      assertTrue(RLP.encodeInt(i), trieCopy);
    }
    for (int i = 1; i < n; i++) {
      for (int j = 1; j < n; j++) {
        if (i != j) {
          assertFalse(RLP.encodeInt(i), RLP.encodeInt(j), trieCopy);
        }
      }
    }
  }

  /*
   * Verifies that TrieImpl root hash is insertion-order-independent even when
   * the same key is put more than once (idempotent put).
   *
   * Covers both known-failing sequences (regression) and a seeded random
   * shuffle. Previously flaky due to a correctness bug in TrieImpl.insert():
   * commonPrefix.isEmpty() was checked before commonPrefix.equals(k), causing
   * KVNode("", v_old) to be incorrectly replaced with BranchNode{terminal:v_new}
   * on a duplicate put of a fully-split key — this is the actual root-hash
   * corruption. A separate, non-correctness optimization in
   * kvNodeSetValueOrNode() additionally short-circuits same-value writes to
   * avoid unnecessary dirty marking / hash recomputation.
   */
  @Test
  public void testOrder() {
    TrieImpl trie = new TrieImpl();
    int n = 100;
    List<Integer> value = new ArrayList<>();
    for (int i = 1; i < n; i++) {
      value.add(i);
      trie.put(RLP.encodeInt(i), String.valueOf(i).getBytes());
    }
    trie.put(RLP.encodeInt(10), String.valueOf(10).getBytes());
    value.add(10);
    byte[] rootHash1 = trie.getRootHash();
    TrieImpl baseline = new TrieImpl();
    for (int i = 1; i < n; i++) {
      baseline.put(RLP.encodeInt(i), String.valueOf(i).getBytes());
    }
    Assert.assertArrayEquals(baseline.getRootHash(), rootHash1);
    Collections.shuffle(value, new Random(SHUFFLE_SEED));
    assertTrieRootHash(rootHash1, value);
    String[] sequences = {
        "95,10,66,10,67,2,98,31,85,89,81,96,19,68,44,49,43,40,62,87,4,38,17,18,8,"
            + "74,28,51,3,41,99,80,70,61,26,34,86,15,33,52,25,92,77,11,39,88,46,84,7,48,"
            + "82,91,16,56,90,65,30,53,47,14,32,79,1,42,45,29,13,22,5,23,59,97,12,20,37,"
            + "54,64,57,78,6,27,50,58,93,83,76,94,72,69,60,75,55,35,63,21,71,24,73,36,9",
        "42,10,78,80,37,10,55,20,58,8,47,84,52,22,27,79,19,34,3,69,49,74,97,81,39,"
            + "4,48,11,68,30,60,98,73,33,86,36,67,94,92,43,88,23,40,28,18,46,50,45,21,14,"
            + "26,24,66,32,71,91,5,95,59,51,38,29,12,41,75,89,16,15,87,85,77,17,96,63,7,"
            + "57,54,35,61,83,31,2,72,90,53,9,44,56,6,1,70,64,25,82,62,99,13,93,76,65",
        "74,83,94,10,28,91,10,29,20,58,2,5,36,41,12,27,19,48,80,38,33,15,46,32,64,"
            + "13,95,1,7,42,26,90,31,77,34,60,56,44,17,23,52,39,87,35,22,37,14,67,86,4,"
            + "93,68,45,71,97,18,98,73,75,53,51,57,72,9,96,78,40,66,92,30,81,50,6,59,61,"
            + "8,65,76,69,16,11,88,25,89,3,54,49,43,62,24,21,82,70,47,84,55,79,99,63,85",
        "99,35,66,10,78,29,70,46,75,10,23,61,60,7,25,20,31,37,52,77,80,11,34,89,65,"
            + "88,28,64,43,81,92,87,72,40,38,67,54,26,73,15,8,90,63,21,49,1,85,17,74,97,"
            + "91,16,36,6,2,56,94,3,62,95,32,58,39,51,14,59,27,96,83,50,86,84,48,19,24,"
            + "82,5,41,13,33,18,44,79,42,68,4,57,45,76,55,9,69,93,12,53,98,22,30,47,71",
        "27,47,18,78,87,10,98,20,45,33,10,46,56,5,24,39,11,40,14,73,66,76,96,44,42,"
            + "53,69,50,61,29,94,55,35,72,99,43,57,91,85,9,48,86,32,92,64,97,67,75,7,58,"
            + "34,4,88,63,70,80,83,82,22,30,84,60,36,54,62,28,21,38,51,25,81,41,52,15,"
            + "77,93,89,13,95,3,49,31,17,59,26,2,23,12,71,16,90,79,68,6,1,37,74,65,19,8",
        "80,60,17,71,92,47,52,10,61,10,97,44,57,45,86,55,96,34,27,77,50,91,32,24,8,"
            + "67,33,94,19,5,4,37,70,63,13,68,69,85,29,49,23,76,40,81,99,15,73,41,12,83,"
            + "93,64,1,79,58,89,88,21,53,6,39,95,74,22,9,78,46,18,11,54,30,90,31,98,36,"
            + "38,75,48,25,72,28,14,66,26,56,3,16,43,62,82,59,87,84,35,2,7,20,42,51,65",
        "94,73,70,10,36,10,50,54,89,37,20,95,82,47,6,32,12,39,80,65,41,44,13,86,27,"
            + "66,49,30,58,51,21,59,56,16,5,38,81,90,67,11,35,55,14,97,79,29,75,57,24,"
            + "43,92,78,71,93,85,72,18,52,28,87,31,83,9,99,46,17,25,42,96,15,8,22,45,76,"
            + "77,7,91,53,1,4,3,84,62,40,60,61,19,98,63,2,88,26,68,33,64,23,34,74,69,48",
        "64,73,78,46,10,37,10,20,19,94,56,57,69,31,82,54,96,4,87,59,30,84,9,23,76,"
            + "2,72,36,71,40,24,49,44,95,98,16,35,45,77,67,80,33,32,29,91,53,39,14,52,"
            + "81,13,25,90,79,28,61,26,83,62,41,34,43,86,66,50,58,21,22,7,38,74,42,48,"
            + "93,55,68,51,89,12,88,60,6,92,99,18,65,15,8,63,17,1,85,70,75,3,27,97,11,"
            + "47,5",
        "10,78,26,27,10,56,24,38,70,23,48,21,77,97,83,20,67,74,29,36,15,16,6,19,90,"
            + "88,1,13,93,25,11,79,52,61,84,40,99,12,81,98,2,58,54,66,7,9,31,30,60,47,"
            + "63,75,44,34,86,37,57,76,5,72,94,14,95,55,51,18,82,3,89,46,33,69,59,96,"
            + "17,41,92,53,87,71,8,80,28,73,85,39,32,45,4,22,35,43,65,62,50,49,91,64,"
            + "68,42",
        "10,10,97,52,89,91,66,28,59,60,58,76,17,67,44,79,88,7,48,50,61,70,39,75,95,"
            + "69,38,55,98,37,25,84,49,35,85,72,29,83,74,99,21,53,32,81,73,16,19,6,92,"
            + "12,96,46,40,14,47,15,27,36,78,82,3,2,8,26,20,33,57,63,65,77,54,1,64,34,"
            + "5,4,18,13,30,9,43,93,90,80,62,11,42,45,51,41,86,94,24,71,22,56,23,31,"
            + "87,68"
    };
    for (String sequence : sequences) {
      assertTrieRootHash(rootHash1, parseSeq(sequence));
    }
  }

  private static List<Integer> parseSeq(String csv) {
    String[] parts = csv.split(",");
    List<Integer> result = new ArrayList<>(parts.length);
    for (String p : parts) {
      result.add(Integer.parseInt(p));
    }
    return result;
  }

  private static void assertTrieRootHash(byte[] rootHash1, List<Integer> value) {
    TrieImpl trie2 = new TrieImpl();
    for (int i : value) {
      trie2.put(RLP.encodeInt(i), String.valueOf(i).getBytes());
    }
    byte[] rootHash2 = trie2.getRootHash();
    Assert.assertArrayEquals(rootHash1, rootHash2);
  }

  @Test
  public void testDeleteDirtyPropagation() {
    TrieImpl trie = new TrieImpl();
    byte[] key1 = new byte[]{0x01, 0x00};
    byte[] key2 = new byte[]{0x01, 0x01};
    byte[] key3 = new byte[]{0x01, 0x02};
    trie.put(key1, "a".getBytes());
    trie.put(key2, "b".getBytes());
    trie.put(key3, "c".getBytes());
    byte[] hashBefore = trie.getRootHash();
    trie.delete(key3);
    byte[] hashAfterDelete = trie.getRootHash();
    Assert.assertFalse("root hash must change after delete",
        Arrays.equals(hashBefore, hashAfterDelete));
    trie.put(key3, "c".getBytes());
    byte[] hashAfterReinsert = trie.getRootHash();
    Assert.assertArrayEquals("root hash must match original after re-insert",
        hashBefore, hashAfterReinsert);
  }

  /*
   * Same as testOrder but without duplicate keys — verifies insertion-order
   * independence for the normal (non-buggy) case.
   */
  @Test
  public void testOrderNoDuplicate() {
    TrieImpl trie = new TrieImpl();
    int n = 100;
    List<Integer> value = new ArrayList<>();
    for (int i = 1; i < n; i++) {
      value.add(i);
      trie.put(RLP.encodeInt(i), String.valueOf(i).getBytes());
    }
    byte[] rootHash1 = trie.getRootHash();
    Collections.shuffle(value, new Random(42));
    TrieImpl trie2 = new TrieImpl();
    for (int i : value) {
      trie2.put(RLP.encodeInt(i), String.valueOf(i).getBytes());
    }
    byte[] rootHash2 = trie2.getRootHash();
    Assert.assertArrayEquals(rootHash1, rootHash2);
  }

  private void assertTrue(byte[] key, TrieImpl trieCopy) {
    Assert.assertTrue(trieCopy.verifyProof(trieCopy.getRootHash(), key, trieCopy.prove(key)));
  }

  private void assertTrue(byte[] key1, byte[] key2, TrieImpl trieCopy) {
    Assert.assertTrue(trieCopy.verifyProof(trieCopy.getRootHash(), key2, trieCopy.prove(key1)));
  }

  private void assertFalse(byte[] key1, byte[] key2, TrieImpl trieCopy) {
    Assert.assertFalse(trieCopy.verifyProof(trieCopy.getRootHash(), key2, trieCopy.prove(key1)));
  }

  @Test
  public void testFastByteComparisons() {
    byte[] test1 = new byte[] {0x00, 0x00, 0x01, 0x02, 0x03, 0x04};
    byte[] test2 = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04};
    Assert.assertEquals(0, FastByteComparisons.compareTo(test1, 1, 5, test2, 0, 5));
  }

}
