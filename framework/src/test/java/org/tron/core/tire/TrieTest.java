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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.util.Arrays;
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

  @Test
  public void test() {
    TrieImpl trie = new TrieImpl();
    trie.put(new byte[]{1}, c.getBytes());
    Assert.assertTrue(Arrays.areEqual(trie.get(RLP.encodeInt(1)), c.getBytes()));
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
    System.out.println(trieCopy.prove(RLP.encodeInt(5)));
    System.out.println(trieCopy.prove(RLP.encodeInt(6)));
    assertTrue(RLP.encodeInt(5), trieCopy);
    assertTrue(RLP.encodeInt(5), RLP.encodeInt(6), trieCopy);
    assertTrue(RLP.encodeInt(6), trieCopy);
    assertTrue(RLP.encodeInt(6), RLP.encodeInt(5), trieCopy);
    //
    trie.put(RLP.encodeInt(5), doge.getBytes());
    byte[] rootHash2 = trie.getRootHash();
    Assert.assertFalse(Arrays.areEqual(rootHash, rootHash2));
    trieCopy = new TrieImpl(trie.getCache(), rootHash2);
    //
    assertTrue(RLP.encodeInt(5), trieCopy);
    assertFalse(RLP.encodeInt(5), RLP.encodeInt(6), trieCopy);
    assertTrue(RLP.encodeInt(6), trieCopy);
    assertFalse(RLP.encodeInt(6), RLP.encodeInt(5), trieCopy);
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
    Assert.assertTrue(Arrays.areEqual(rootHash1, rootHash2));
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
    Collections.shuffle(value);
    TrieImpl trie2 = new TrieImpl();
    for (int i : value) {
      trie2.put(RLP.encodeInt(i), String.valueOf(i).getBytes());
    }
    byte[] rootHash2 = trie2.getRootHash();
    Assert.assertTrue(java.util.Arrays.equals(rootHash1, rootHash2));
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

}
