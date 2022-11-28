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

import org.apache.commons.collections4.set.ListOrderedSet;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.bouncycastle.util.Arrays;
import org.hyperledger.besu.ethereum.trie.BranchNode;
import org.hyperledger.besu.ethereum.trie.CompactEncoding;
import org.hyperledger.besu.ethereum.trie.LeafNode;
import org.hyperledger.besu.ethereum.trie.MerkleStorage;
import org.hyperledger.besu.storage.KeyValueStorage;
import org.hyperledger.besu.storage.RocksDBConfiguration;
import org.hyperledger.besu.storage.RocksDBConfigurationBuilder;
import org.hyperledger.besu.storage.RocksDBKeyValueStorage;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.utils.ByteArray;
import org.tron.core.state.WorldStateCallBackUtils;
import org.tron.core.state.trie.TrieImpl2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

public class Trie2Test {

  @Rule
  public final TemporaryFolder folder = new TemporaryFolder();

  private static final Bytes c = Bytes.wrap("c".getBytes(StandardCharsets.UTF_8));
  private static final Bytes ca = Bytes.wrap("ca".getBytes(StandardCharsets.UTF_8));
  private static final Bytes cat = Bytes.wrap("cat".getBytes(StandardCharsets.UTF_8));
  private static final Bytes dog = Bytes.wrap("dog".getBytes(StandardCharsets.UTF_8));
  private static final Bytes doge = Bytes.wrap("doge".getBytes(StandardCharsets.UTF_8));
  private static final Bytes test = Bytes.wrap("test".getBytes(StandardCharsets.UTF_8));
  private static final Bytes dude = Bytes.wrap("dude".getBytes(StandardCharsets.UTF_8));

  private KeyValueStorage createStore() {
    try {
      return new RocksDBKeyValueStorage(config());
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  private RocksDBConfiguration config() throws IOException {
    return new RocksDBConfigurationBuilder().databaseDir(folder.newFolder().toPath()).build();
  }

  @Test
  public void test() {
    TrieImpl2 trie = new TrieImpl2(createStore());
    trie.put(Bytes.of(1), c);
    Assert.assertEquals(trie.get(new byte[]{1}), c);
    trie.put(Bytes.of(1, 0), ca);
    String hash1 = ByteArray.toHexString(trie.getRootHash());
    trie.commit();
    trie.put(new byte[]{1, 1}, cat);
    String hash2 = ByteArray.toHexString(trie.getRootHash());
    trie.commit();
    Assert.assertNotEquals(hash1, hash2);
    trie.put(new byte[]{1, 2}, dog);
    String hash3 = ByteArray.toHexString(trie.getRootHash());
    trie.commit();
    trie.put(Bytes.of(5), doge);
    String hash4 = ByteArray.toHexString(trie.getRootHash());
    trie.commit();
    Assert.assertNotEquals(hash3, hash4);
    trie.put(Bytes.of(6).toArrayUnsafe(), doge);
    String hash5 = ByteArray.toHexString(trie.getRootHash());
    trie.commit();
    Assert.assertNotEquals(hash4, hash5);
    trie.put(Bytes.of(7).toArrayUnsafe(), doge);
    trie.put(Bytes.of(11), doge);
    trie.put(Bytes.of(12).toArrayUnsafe(), dude);
    trie.put(Bytes.of(13).toArrayUnsafe(), test);
    String hash9 = ByteArray.toHexString(trie.getRootHash());
    trie.commit();
    Assert.assertEquals(trie.get(new byte[]{1, 0}), ca);
    trie.delete(Bytes.of(13).toArrayUnsafe());
    String hash10 = ByteArray.toHexString(trie.getRootHash());
    trie.commit();
    Assert.assertNotEquals(hash9, hash10);
    byte[] rootHash = trie.getRootHash();
    trie.flush();
    TrieImpl2 trieCopy = new TrieImpl2(trie.getMerkleStorage(),
            Bytes32.wrap(ByteArray.fromHexString(hash3)));
    Assert.assertNull(trieCopy.get(Bytes.of(5).toArrayUnsafe()));
    Assert.assertEquals(trieCopy.get(new byte[]{1, 2}), dog);
    trieCopy.put(Bytes.of(5).toArrayUnsafe(), doge);
    trieCopy.put(new byte[]{1, 2}, cat);
    trieCopy.commit();
    byte[] rootHash2 = trieCopy.getRootHash();
    Assert.assertFalse(Arrays.areEqual(rootHash, rootHash2));
    Assert.assertEquals(trieCopy.get(Bytes.of(5).toArrayUnsafe()), doge);
    Assert.assertEquals(trieCopy.get(new byte[]{1, 2}), cat);
    Assert.assertEquals(trie.get(new byte[]{1, 2}), dog);
  }

  @Test
  public void canReloadTrieFromHash() {
    final Bytes key1 = Bytes.of(1, 5, 8, 9);
    final Bytes key2 = Bytes.of(1, 6, 1, 2);
    final Bytes key3 = Bytes.of(1, 6, 1, 3);

    TrieImpl2 trie = new TrieImpl2(createStore());

    // Push some values into the trie and commit changes so nodes are persisted
    final Bytes value1 = Bytes.of("value1".getBytes(StandardCharsets.UTF_8));
    trie.put(key1, value1);
    final byte[] hash1 = trie.getRootHash();
    // put data into pendingUpdates
    trie.commit();

    final  Bytes value2 = Bytes.wrap("value2".getBytes(StandardCharsets.UTF_8));
    trie.put(key2, value2);
    final  Bytes value3 = Bytes.wrap("value3".getBytes(StandardCharsets.UTF_8));
    trie.put(key3, value3);
    final byte[] hash2 = trie.getRootHash();
    // put data into pendingUpdates
    trie.commit();

    final Bytes value4 = Bytes.wrap("value4".getBytes(StandardCharsets.UTF_8));
    trie.put(key1, value4);
    trie.put(Bytes.of(1,2,3,4,5), UInt256.ZERO);
    final byte[] hash3 = trie.getRootHash();
    // put data into pendingUpdates
     trie.commit();

    // Check the root hashes for 3 tries are all distinct
    Assert.assertNotEquals(Bytes.of(hash1),Bytes.of(hash2));
    Assert.assertNotEquals(Bytes.of(hash1),Bytes.of(hash3));
    Assert.assertNotEquals(Bytes.of(hash2),Bytes.of(hash3));

    // And that we can retrieve the last value we set for key1
    Assert.assertEquals(trie.get(key1), value4);

    // Create new tries from root hashes and check that we find expected values
    trie = new TrieImpl2(trie.getMerkleStorage(), Bytes32.wrap(hash1));
    Assert.assertEquals(trie.get(key1),value1);
    Assert.assertNull(trie.get(key2));
    Assert.assertNull(trie.get(key3));

    trie = new TrieImpl2(trie.getMerkleStorage(), Bytes32.wrap(hash2));
    Assert.assertEquals(trie.get(key1),value1);
    Assert.assertEquals(trie.get(key2),value2);
    Assert.assertEquals(trie.get(key3),value3);

    trie = new TrieImpl2(trie.getMerkleStorage(), Bytes32.wrap(hash3));
    Assert.assertEquals(trie.get(key1),value4);
    Assert.assertEquals(trie.get(key2),value2);
    Assert.assertEquals(trie.get(key3),value3);

    // Commit changes to storage, and create new tries from roothash and new storage instance
    trie.flush();
    final MerkleStorage newMerkleStorage = trie.getMerkleStorage();
    trie = new TrieImpl2(newMerkleStorage,Bytes32.wrap(hash1));
    Assert.assertEquals(trie.get(key1),value1);
    Assert.assertNull(trie.get(key2));
    Assert.assertNull(trie.get(key3));

    trie = new TrieImpl2(newMerkleStorage,Bytes32.wrap(hash2));
    Assert.assertEquals(trie.get(key1),value1);
    Assert.assertEquals(trie.get(key2),value2);
    Assert.assertEquals(trie.get(key3),value3);

    trie = new TrieImpl2(newMerkleStorage, Bytes32.wrap(hash3));
    Assert.assertEquals(trie.get(key1),value4);
    Assert.assertEquals(trie.get(key2),value2);
    Assert.assertEquals(trie.get(key3),value3);
    Assert.assertEquals(trie.get(Bytes.of(1,2,3,4,5)), UInt256.ZERO);
    final byte[] key4 = Bytes.of(1,3,4,6,7,9).toArray();
    final byte[] key5 = Bytes.of(1,3,4,6,3,9).toArray();
    final byte[] key6 = Bytes.of(1,3,4,6,8,9).toArray();
    final Bytes key7 = Bytes.of(2);
    final Bytes key8= Bytes32.random();
    final byte[] key9= Bytes.wrap(key8, key7).toArray();
    trie.put(key4, Bytes.of("value5".getBytes(StandardCharsets.UTF_8)));
    trie.put(key5, Bytes.of("value6".getBytes(StandardCharsets.UTF_8)));
    trie.put(key6, Bytes.of("value7".getBytes(StandardCharsets.UTF_8)));
    trie.put(key5, Bytes.of("value8".getBytes(StandardCharsets.UTF_8)));
    trie.put(key7, Bytes.of("value9".getBytes(StandardCharsets.UTF_8)));
    trie.put(key8, Bytes.of("value10".getBytes(StandardCharsets.UTF_8)));
    trie.put(key9, Bytes.of("value11".getBytes(StandardCharsets.UTF_8)));
    Random r = new SecureRandom();
    List<Bytes> rl = new ArrayList<>();
    for (int i =1 ; i<= 1000; i++) {
      byte[] array = new byte[i%256];
      r.nextBytes(array);
      Bytes bytes = Bytes.wrap(array);
      rl.add(bytes);
      trie.put(bytes, Bytes32.random());
    }
    rl.addAll(java.util.Arrays.asList(Bytes.wrap(key1),Bytes.of(1,2,3,4,5)
            ,Bytes.wrap(key2),Bytes.wrap(key3), Bytes.wrap(key4),
            Bytes.wrap(key5),
            Bytes.wrap(key6), key7, key8, Bytes.wrap(key9)));
    trie.commit();
    trie.flush();

    List<Bytes> keys = new ArrayList<>();
    trie.visitAll((N) -> {
      if (N instanceof BranchNode && N.getValue().isPresent()) {
        Bytes k = CompactEncoding.pathToBytes(
                Bytes.concatenate(N.getLocation().orElse(Bytes.EMPTY),
                        Bytes.of(CompactEncoding.LEAF_TERMINATOR)));
        keys.add(k);
      }

      if (N instanceof LeafNode) {
        Bytes k = CompactEncoding.pathToBytes(
                Bytes.concatenate(N.getLocation().orElse(Bytes.EMPTY),
                        N.getPath()));
        keys.add(k);
      }
    });

    Collections.sort(keys);
    Collections.sort(rl);
    rl = rl.stream().distinct().collect(Collectors.toList());
    Assert.assertEquals(trie.get(key7),
            Bytes.of("value9".getBytes(StandardCharsets.UTF_8)));
    Assert.assertEquals(keys.size(), rl.size());
    Assert.assertEquals(keys, rl);
  }

  @Test
  public void test1() {
    TrieImpl2 trie = new TrieImpl2();
    int n = 100;
    for (int i = 1; i < n; i++) {
      trie.put(Bytes.of(i), Bytes.of(i));
    }
    byte[] rootHash1 = trie.getRootHash();

    TrieImpl2 trie2 = new TrieImpl2();
    for (int i = 1; i < n; i++) {
      trie2.put(Bytes.of(i), Bytes.of(i));
    }
    byte[] rootHash2 = trie2.getRootHash();
    Assert.assertArrayEquals(rootHash1, rootHash2);
  }

  @Test
  public void test2() {
    TrieImpl2 trie = new TrieImpl2();
    int n = 100;
    for (int i = 1; i < n; i++) {
      trie.put(Bytes.of(i),  Bytes.of(i));
    }
    trie.commit();
    trie.flush();
    byte[] rootHash = trie.getRootHash();
    TrieImpl2 trieCopy = new TrieImpl2(trie.getMerkleStorage(), Bytes32.wrap(rootHash));
    for (int i = 1; i < n; i++) {
      Assert.assertEquals(trieCopy.get(Bytes.of(i)), Bytes.of(i));
    }
    for (int i = 1; i < n; i++) {
      for (int j = 1; j < n; j++) {
        if (i != j) {
          Assert.assertNotEquals(trieCopy.get(Bytes.of(i)), trieCopy.get(Bytes.of(j)));
        }
      }
    }
  }

  @Test
  public void testOrder() {
    TrieImpl2 trie = new TrieImpl2();
    int n = 100;
    List<Integer> value = new ArrayList<>();
    for (int i = 1; i < n; i++) {
      value.add(i);
      trie.put(Bytes.of(i), Bytes.of(i));
    }
    trie.put(Bytes.of(10), Bytes.of(10));
    value.add(10);
    trie.commit();
    trie.flush();
    byte[] rootHash1 = trie.getRootHash();
    Collections.shuffle(value);
    TrieImpl2 trie2 = new TrieImpl2();
    for (int i : value) {
      trie2.put(Bytes.of(i), Bytes.of(i));
    }
    trie2.commit();
    trie2.flush();
    byte[] rootHash2 = trie2.getRootHash();
    Assert.assertArrayEquals(rootHash1, rootHash2);
  }

  @Test
  public void testOrder2() {
    Set<WorldStateCallBackUtils.TrieEntry> trieEntryList = new ListOrderedSet<>();
    trieEntryList.add(WorldStateCallBackUtils.TrieEntry.build(Bytes.of(1),Bytes.of(1)));
    trieEntryList.add(WorldStateCallBackUtils.TrieEntry.build(Bytes.of(2),Bytes.of(2)));
    trieEntryList.add(WorldStateCallBackUtils.TrieEntry.build(Bytes.of(3),Bytes.of(3)));
    trieEntryList.add(WorldStateCallBackUtils.TrieEntry.build(Bytes.of(1),Bytes.of(11)));
    trieEntryList.add(WorldStateCallBackUtils.TrieEntry.build(Bytes.of(3),Bytes.of(32)));
    trieEntryList.forEach(e -> System.out.println(e.getKey() + ", " + e.getData()));
  }

}
