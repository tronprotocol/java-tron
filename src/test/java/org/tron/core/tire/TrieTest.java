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

import static org.tron.common.crypto.Hash.EMPTY_TRIE_HASH;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.utils.ByteArrayWrapper;
import org.tron.core.capsule.utils.RLP;
import org.tron.core.db2.common.ConcurrentHashDB;
import org.tron.core.db2.common.DB;
import org.tron.core.trie.TrieImpl;

public class TrieTest {

  private static final Logger logger = LoggerFactory.getLogger("test");

  private static String LONG_STRING = "1234567890abcdefghijklmnopqrstuvwxxzABCEFGHIJKLMNOPQRSTUVWXYZ";
  private static String ROOT_HASH_EMPTY = Hex.toHexString(EMPTY_TRIE_HASH);

  private static String c = "c";
  private static String ca = "ca";
  private static String cat = "cat";
  private static String dog = "dog";
  private static String doge = "doge";
  private static String test = "test";
  private static String dude = "dude";


  //    public TrieCache mockDb = new TrieCache();
//    public TrieCache mockDb_2 = new TrieCache();
  public DB mockDb = new ConcurrentHashDB();

//      ROOT: [ '\x16', A ]
//      A: [ '', '', '', '', B, '', '', '', C, '', '', '', '', '', '', '', '' ]
//      B: [ '\x00\x6f', D ]
//      D: [ '', '', '', '', '', '', E, '', '', '', '', '', '', '', '', '', 'verb' ]
//      E: [ '\x17', F ]
//      F: [ '', '', '', '', '', '', G, '', '', '', '', '', '', '', '', '', 'puppy' ]
//      G: [ '\x35', 'coin' ]
//      C: [ '\x20\x6f\x72\x73\x65', 'stallion' ]

  @After
  public void closeMockDb() throws IOException {
  }

  @Test
  public void test() throws UnsupportedEncodingException {
    TrieImpl trie = new TrieImpl();
    trie.put(new byte[]{1}, c.getBytes());
    Assert.assertTrue(Arrays.areEqual(trie.get(RLP.encodeInt(1)), c.getBytes()));
    trie.put(new byte[]{1,0}, ca.getBytes());
    trie.put(new byte[]{1,1}, cat.getBytes());
    trie.put(new byte[]{1,2}, dog.getBytes());
    trie.put(RLP.encodeInt(5), doge.getBytes());
    trie.put(RLP.encodeInt(6), doge.getBytes());
    trie.put(RLP.encodeInt(7), doge.getBytes());
    trie.put(RLP.encodeInt(11), doge.getBytes());
    System.out.println(Sha256Hash.of(trie.getRootHash()).toString());
    trie.put(RLP.encodeInt(5), dude.getBytes());
    System.out.println(trie.dumpTrie());
    System.out.println(Sha256Hash.of(trie.getRootHash()).toString());
    trie.delete(RLP.encodeInt(3));
    System.out.println(trie.dumpTrie());
    System.out.println(Sha256Hash.of(trie.getRootHash()).toString());
    getReferencedTrieNodes(trie, RLP.encodeInt(1));
  }

  public Set<ByteArrayWrapper> getReferencedTrieNodes(TrieImpl trie, byte[] keyV) {
    final Set<ByteArrayWrapper> ret = new HashSet<>();
    trie.scanTree(new TrieImpl.ScanAction() {
      @Override
      public void doOnNode(byte[] hash, TrieImpl.Node node) {
        ret.add(new ByteArrayWrapper(hash));
      }

      @Override
      public void doOnValue(byte[] nodeHash, TrieImpl.Node node, byte[] key, byte[] value) {
//        if (Arrays.areEqual(key, keyV)) {
//          ret.add(new ByteArrayWrapper( node.encode()));
//        }
      }
    });
    System.out.println("getReferencedTrieNodes: " + ret);
    return ret;
  }

}
