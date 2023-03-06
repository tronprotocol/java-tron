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
import org.hyperledger.besu.storage.InMemoryKeyValueStorage;
import org.hyperledger.besu.storage.KeyValueStorage;
import org.hyperledger.besu.storage.KeyValueStorageTransaction;
import org.junit.Test;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TrieNodeDecoderTest {

  @Test
  public void decodeNodes() {
    final InMemoryKeyValueStorage storage = new InMemoryKeyValueStorage();

    // Build a small trie
    final MerklePatriciaTrie<Bytes, Bytes> trie =
        new StoredMerklePatriciaTrie<>(
            new BytesToByteNodeLoader(storage), Function.identity(), Function.identity());
    trie.put(Bytes.fromHexString("0x100000"), Bytes.of(1));
    trie.put(Bytes.fromHexString("0x200000"), Bytes.of(2));
    trie.put(Bytes.fromHexString("0x300000"), Bytes.of(3));

    trie.put(Bytes.fromHexString("0x110000"), Bytes.of(10));
    trie.put(Bytes.fromHexString("0x210000"), Bytes.of(20));
    // Create large leaf node that will not be inlined
    trie.put(
        Bytes.fromHexString("0x310000"),
        Bytes.fromHexString("0x11223344556677889900112233445566778899"));

    // Save nodes to storage
    final KeyValueStorageTransaction tx = storage.startTransaction();
    trie.commit((location, key, value) -> tx.put(key.toArrayUnsafe(), value.toArrayUnsafe()));
    tx.commit();

    // Get and flatten root node
    final Bytes rootNodeRlp = Bytes.wrap(storage.get(trie.getRootHash().toArrayUnsafe()).get());
    final List<Node<Bytes>> nodes = TrieNodeDecoder.decodeNodes(null, rootNodeRlp);
    // The full trie hold 10 nodes, the branch node starting with 0x3... holding 2 values will be a
    // hash
    // referenced node and so its 2 child nodes will be missing
    assertThat(nodes.size()).isEqualTo(8);

    // Collect and check values
    final List<Bytes> actualValues =
        nodes.stream()
            .filter(n -> !n.isReferencedByHash())
            .map(Node::getValue)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    assertThat(actualValues)
        .containsExactlyInAnyOrder(Bytes.of(1), Bytes.of(10), Bytes.of(2), Bytes.of(20));
  }

  @Test
  public void breadthFirstDecode_smallTrie() {
    final InMemoryKeyValueStorage storage = new InMemoryKeyValueStorage();

    // Build a small trie
    final MerklePatriciaTrie<Bytes, Bytes> trie =
        new StoredMerklePatriciaTrie<>(
            new BytesToByteNodeLoader(storage), Function.identity(), Function.identity());
    trie.put(Bytes.fromHexString("0x100000"), Bytes.of(1));
    trie.put(Bytes.fromHexString("0x200000"), Bytes.of(2));
    trie.put(Bytes.fromHexString("0x300000"), Bytes.of(3));

    trie.put(Bytes.fromHexString("0x110000"), Bytes.of(10));
    trie.put(Bytes.fromHexString("0x210000"), Bytes.of(20));
    trie.put(Bytes.fromHexString("0x310000"), Bytes.of(30));

    // Save nodes to storage
    final KeyValueStorageTransaction tx = storage.startTransaction();
    trie.commit((location, key, value) -> tx.put(key.toArrayUnsafe(), value.toArrayUnsafe()));
    tx.commit();

    // First layer should just be the root node
    final List<Node<Bytes>> depth0Nodes =
        TrieNodeDecoder.breadthFirstDecoder(
                new BytesToByteNodeLoader(storage), trie.getRootHash(), 0)
            .collect(Collectors.toList());

    assertThat(depth0Nodes.size()).isEqualTo(1);
    final Node<Bytes> rootNode = depth0Nodes.get(0);
    assertThat(rootNode.getHash()).isEqualTo(trie.getRootHash());

    // Decode first 2 levels
    final List<Node<Bytes>> depth0And1Nodes =
        (TrieNodeDecoder.breadthFirstDecoder(
                new BytesToByteNodeLoader(storage), trie.getRootHash(), 1)
            .collect(Collectors.toList()));
    final int secondLevelNodeCount = 3;
    final int expectedNodeCount = secondLevelNodeCount + 1;
    assertThat(depth0And1Nodes.size()).isEqualTo(expectedNodeCount);
    // First node should be root node
    assertThat(depth0And1Nodes.get(0).getHash()).isEqualTo(rootNode.getHash());
    // Subsequent nodes should be children of root node
    final List<Bytes32> expectedNodesHashes =
        rootNode.getChildren().stream()
            .filter(n -> !Objects.equals(n, NullNode.instance()))
            .map(Node::getHash)
            .collect(Collectors.toList());
    final List<Bytes32> actualNodeHashes =
        depth0And1Nodes.subList(1, expectedNodeCount).stream()
            .map(Node::getHash)
            .collect(Collectors.toList());
    assertThat(actualNodeHashes).isEqualTo(expectedNodesHashes);

    // Decode full trie
    final List<Node<Bytes>> allNodes =
        TrieNodeDecoder.breadthFirstDecoder(new BytesToByteNodeLoader(storage), trie.getRootHash())
            .collect(Collectors.toList());
    assertThat(allNodes.size()).isEqualTo(10);
    // Collect and check values
    final List<Bytes> actualValues =
        allNodes.stream()
            .map(Node::getValue)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    assertThat(actualValues)
        .containsExactly(
            Bytes.of(1), Bytes.of(10), Bytes.of(2), Bytes.of(20), Bytes.of(3), Bytes.of(30));
  }

  @Test
  public void breadthFirstDecode_partialTrie() {
    final InMemoryKeyValueStorage fullStorage = new InMemoryKeyValueStorage();
    final InMemoryKeyValueStorage partialStorage = new InMemoryKeyValueStorage();

    // Build a small trie
    final MerklePatriciaTrie<Bytes, Bytes> trie =
        new StoredMerklePatriciaTrie<>(
            new BytesToByteNodeLoader(fullStorage), Function.identity(), Function.identity());
    final Random random = new Random(1);
    for (int i = 0; i < 30; i++) {
      final byte[] key = new byte[4];
      final byte[] val = new byte[4];
      random.nextBytes(key);
      random.nextBytes(val);
      trie.put(Bytes.wrap(key), Bytes.wrap(val));
    }
    final KeyValueStorageTransaction tx = fullStorage.startTransaction();
    trie.commit((location, key, value) -> tx.put(key.toArrayUnsafe(), value.toArrayUnsafe()));
    tx.commit();

    // Get root node
    final Node<Bytes> rootNode =
        TrieNodeDecoder.breadthFirstDecoder(
                new BytesToByteNodeLoader(fullStorage), trie.getRootHash())
            .findFirst()
            .get();

    // Decode partially available trie
    final KeyValueStorageTransaction partialTx = partialStorage.startTransaction();
    partialTx.put(trie.getRootHash().toArrayUnsafe(), rootNode.getRlp().toArrayUnsafe());
    partialTx.commit();
    final List<Node<Bytes>> allDecodableNodes =
        TrieNodeDecoder.breadthFirstDecoder(
                new BytesToByteNodeLoader(partialStorage), trie.getRootHash())
            .collect(Collectors.toList());
    assertThat(allDecodableNodes.size()).isGreaterThanOrEqualTo(1);
    assertThat(allDecodableNodes.get(0).getHash()).isEqualTo(rootNode.getHash());
  }

  @Test
  public void breadthFirstDecode_emptyTrie() {
    final List<Node<Bytes>> result =
        TrieNodeDecoder.breadthFirstDecoder(
                (l, h) -> Optional.empty(), MerklePatriciaTrie.EMPTY_TRIE_NODE_HASH)
            .collect(Collectors.toList());
    assertThat(result.size()).isEqualTo(0);
  }

  @Test
  public void breadthFirstDecode_singleNodeTrie() {
    final InMemoryKeyValueStorage storage = new InMemoryKeyValueStorage();

    final MerklePatriciaTrie<Bytes, Bytes> trie =
        new StoredMerklePatriciaTrie<>(
            new BytesToByteNodeLoader(storage), Function.identity(), Function.identity());
    trie.put(Bytes.fromHexString("0x100000"), Bytes.of(1));

    // Save nodes to storage
    final KeyValueStorageTransaction tx = storage.startTransaction();
    trie.commit((location, key, value) -> tx.put(key.toArrayUnsafe(), value.toArrayUnsafe()));
    tx.commit();

    final List<Node<Bytes>> result =
        TrieNodeDecoder.breadthFirstDecoder(new BytesToByteNodeLoader(storage), trie.getRootHash())
            .collect(Collectors.toList());
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.get(0).getValue()).contains(Bytes.of(1));
    final Bytes actualPath = CompactEncoding.pathToBytes(result.get(0).getPath());
    assertThat(actualPath).isEqualTo(Bytes.fromHexString("0x100000"));
  }

  @Test
  public void breadthFirstDecode_unknownTrie() {

    final Bytes32 randomRootHash = Bytes32.fromHexStringLenient("0x12");
    final List<Node<Bytes>> result =
        TrieNodeDecoder.breadthFirstDecoder((l, h) -> Optional.empty(), randomRootHash)
            .collect(Collectors.toList());
    assertThat(result.size()).isEqualTo(0);
  }

  private static class BytesToByteNodeLoader implements NodeLoader {

    private final KeyValueStorage storage;

    private BytesToByteNodeLoader(final KeyValueStorage storage) {
      this.storage = storage;
    }

    @Override
    public Optional<Bytes> getNode(final Bytes location, final Bytes32 hash) {
      final byte[] value = storage.get(hash.toArrayUnsafe()).orElse(null);
      return value == null ? Optional.empty() : Optional.of(Bytes.wrap(value));
    }
  }
}
