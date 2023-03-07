/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.ethereum.trie;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.storage.KeyValueStorage;
import org.hyperledger.besu.storage.RocksDBConfiguration;
import org.hyperledger.besu.storage.RocksDBConfigurationBuilder;
import org.hyperledger.besu.storage.RocksDBKeyValueStorage;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

public class StoredMerklePatriciaTrieTest extends AbstractMerklePatriciaTrieTest {
  private KeyValueStorage keyValueStore;
  private MerkleStorage merkleStorage;
  private Function<String, Bytes> valueSerializer;
  private Function<Bytes, String> valueDeserializer;
  @Rule
  public final TemporaryFolder folder = new TemporaryFolder();

  protected KeyValueStorage createStore() {
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

  @Override
  protected MerklePatriciaTrie<Bytes, String> createTrie() {
    keyValueStore = createStore();
    merkleStorage = new KeyValueMerkleStorage(keyValueStore);
    valueSerializer =
        value -> (value != null) ? Bytes.wrap(value.getBytes(StandardCharsets.UTF_8)) : null;
    valueDeserializer = bytes -> new String(bytes.toArrayUnsafe(), StandardCharsets.UTF_8);
    return new StoredMerklePatriciaTrie<>(merkleStorage::get, valueSerializer, valueDeserializer);
  }

  @Test
  public void putEmpty() {
    final Bytes key0 = Bytes.of(1, 9, 8, 9);
    // Push some values into the trie and commit changes so nodes are persisted
    final String value0 = "";
    trie.put(key0, value0);
    // put data into pendingUpdates
    trie.commit(merkleStorage::put);
    assertThatRuntimeException().isThrownBy(() -> trie.get(key0))
            .withMessageContaining("leaf has null value");
  }

  @Test
  public void canReloadTrieFromHash() {
    final Bytes key1 = Bytes.of(1, 5, 8, 9);
    final Bytes key2 = Bytes.of(1, 6, 1, 2);
    final Bytes key3 = Bytes.of(1, 6, 1, 3);

    // Push some values into the trie and commit changes so nodes are persisted
    final String value1 = "value1";
    trie.put(key1, value1);
    final Bytes32 hash1 = trie.getRootHash();
    // put data into pendingUpdates
    trie.commit(merkleStorage::put);

    final String value2 = "value2";
    trie.put(key2, value2);
    final String value3 = "value3";
    trie.put(key3, value3);
    final Bytes32 hash2 = trie.getRootHash();
    // put data into pendingUpdates
    trie.commit(merkleStorage::put);

    final String value4 = "value4";
    trie.put(key1, value4);
    final Bytes32 hash3 = trie.getRootHash();
    // put data into pendingUpdates
    trie.commit(merkleStorage::put);

    // Check the root hashes for 3 tries are all distinct
    assertThat(hash1).isNotEqualTo(hash2);
    assertThat(hash1).isNotEqualTo(hash3);
    assertThat(hash2).isNotEqualTo(hash3);
    // And that we can retrieve the last value we set for key1
    assertThat(trie.get(key1)).isEqualTo(Optional.of("value4"));

    // Create new tries from root hashes and check that we find expected values
    trie =
        new StoredMerklePatriciaTrie<>(
            merkleStorage::get, hash1, valueSerializer, valueDeserializer);
    assertThat(trie.get(key1)).isEqualTo(Optional.of("value1"));
    assertThat(trie.get(key2)).isEqualTo(Optional.empty());
    assertThat(trie.get(key3)).isEqualTo(Optional.empty());

    trie =
        new StoredMerklePatriciaTrie<>(
            merkleStorage::get, hash2, valueSerializer, valueDeserializer);
    assertThat(trie.get(key1)).isEqualTo(Optional.of("value1"));
    assertThat(trie.get(key2)).isEqualTo(Optional.of("value2"));
    assertThat(trie.get(key3)).isEqualTo(Optional.of("value3"));

    trie =
        new StoredMerklePatriciaTrie<>(
            merkleStorage::get, hash3, valueSerializer, valueDeserializer);
    assertThat(trie.get(key1)).isEqualTo(Optional.of("value4"));
    assertThat(trie.get(key2)).isEqualTo(Optional.of("value2"));
    assertThat(trie.get(key3)).isEqualTo(Optional.of("value3"));

    // Commit changes to storage, and create new tries from roothash and new storage instance
    merkleStorage.commit();
    final MerkleStorage newMerkleStorage = new KeyValueMerkleStorage(keyValueStore);
    trie =
        new StoredMerklePatriciaTrie<>(
            newMerkleStorage::get, hash1, valueSerializer, valueDeserializer);
    assertThat(trie.get(key1)).isEqualTo(Optional.of("value1"));
    assertThat(trie.get(key2)).isEqualTo(Optional.empty());
    assertThat(trie.get(key3)).isEqualTo(Optional.empty());

    trie =
        new StoredMerklePatriciaTrie<>(
            newMerkleStorage::get, hash2, valueSerializer, valueDeserializer);
    assertThat(trie.get(key1)).isEqualTo(Optional.of("value1"));
    assertThat(trie.get(key2)).isEqualTo(Optional.of("value2"));
    assertThat(trie.get(key3)).isEqualTo(Optional.of("value3"));

    trie =
        new StoredMerklePatriciaTrie<>(
            newMerkleStorage::get, hash3, valueSerializer, valueDeserializer);
    assertThat(trie.get(key1)).isEqualTo(Optional.of("value4"));
    assertThat(trie.get(key2)).isEqualTo(Optional.of("value2"));
    assertThat(trie.get(key3)).isEqualTo(Optional.of("value3"));
    final Bytes key4 = Bytes.of(1,3,4,6,7,9);
    final Bytes key5 = Bytes.of(1,3,4,6,3,9);
    final Bytes key6 = Bytes.of(1,3,4,6,8,9);
    final Bytes key7= Bytes.of(2);
    final Bytes key8= Bytes32.random();
    final Bytes key9= Bytes.wrap(key8, key7);
    trie.put(key4, "value5");
    trie.put(key5, "value6");
    trie.put(key6, "value7");
    trie.put(key5, "value8");
    trie.put(key7, "value9");
    trie.put(key8, "value10");
    trie.put(key9, "value11");
    Random r = new SecureRandom();
    List<Bytes> rl = new ArrayList<>();
    for (int i =1 ; i<= 1000; i++) {
      byte[] array = new byte[i%256];
      r.nextBytes(array);
      Bytes bytes = Bytes.wrap(array);
      rl.add(bytes);
      trie.put(bytes, UUID.randomUUID().toString());
    }
    rl.addAll(Arrays.asList(key1,key2,key3,key4,key5,key6,key7, key8, key9));
    trie.commit(merkleStorage::put);
    merkleStorage.commit();

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
    assertThat(trie.get(key7)).isEqualTo(Optional.of("value9"));
    assertThat(keys.size()).isEqualTo(rl.size());
    assertThat(keys).isEqualTo(rl);
  }
}
