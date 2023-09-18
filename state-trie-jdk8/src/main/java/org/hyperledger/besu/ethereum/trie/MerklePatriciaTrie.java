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
import org.hyperledger.besu.ethereum.rlp.RLP;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.hyperledger.besu.crypto.Hash.keccak256;

/** An Merkle Patricial Trie. */
public interface MerklePatriciaTrie<K, V> {

  Bytes EMPTY_TRIE_NODE = RLP.NULL;
  Bytes32 EMPTY_TRIE_NODE_HASH = keccak256(EMPTY_TRIE_NODE);

  /**
   * Returns an {@code Optional} of value mapped to the hash if it exists; otherwise empty.
   *
   * @param key The key for the value.
   * @return an {@code Optional} of value mapped to the hash if it exists; otherwise empty
   */
  Optional<V> get(K key);

  /**
   * Returns an {@code Optional} of value mapped to the given path if it exists; otherwise empty.
   *
   * @param path The path for the value.
   * @return an {@code Optional} of value mapped to the given path if it exists; otherwise empty
   */
  Optional<V> getPath(final K path);

  /**
   * Returns value and ordered proof-related nodes mapped to the hash if it exists; otherwise empty.
   *
   * @param key The key for the value.
   * @return value and ordered proof-related nodes
   */
  Proof<V> getValueWithProof(K key);

  /**
   * Updates the value mapped to the specified key, creating the mapping if one does not already
   * exist.
   *
   * @param key The key that corresponds to the value to be updated.
   * @param value The value to associate the key with.
   */
  void put(K key, V value);

  /**
   * Updates the value mapped to the specified key, creating the mapping if one does not already
   * exist.
   *
   * @param key The key that corresponds to the value to be updated.
   * @param putVisitor custom visitor for the update
   */
  void put(K key, PutVisitor<V> putVisitor);

  /**
   * Deletes the value mapped to the specified key, if such a value exists (Optional operation).
   *
   * @param key The key of the value to be deleted.
   */
  void remove(K key);

  /**
   * Deletes the node mapped to the specified path, if such a node exists (Optional operation).
   *
   * @param path of the node to be deleted.
   * @param removeVisitor custom visitor for the deletion
   */
  void removePath(K path, RemoveVisitor<V> removeVisitor);

  /**
   * Returns the KECCAK256 hash of the root node of the trie.
   *
   * @return The KECCAK256 hash of the root node of the trie.
   */
  Bytes32 getRootHash();

  /**
   * Commits any pending changes to the underlying storage.
   *
   * @param nodeUpdater used to store the node values
   */
  void commit(NodeUpdater nodeUpdater);

  /**
   * Commits any pending changes to the underlying storage.
   *
   * @param nodeUpdater used to store the node values
   * @param commitVisitor custom visitor for the commit
   */
  void commit(NodeUpdater nodeUpdater, CommitVisitor<V> commitVisitor);

  /**
   * Retrieve up to {@code limit} storage entries beginning from the first entry with hash equal to
   * or greater than {@code startKeyHash}.
   *
   * @param startKeyHash the first key hash to return.
   * @param limit the maximum number of entries to return.
   * @return the requested storage entries as a map of key hash to value.
   */
  Map<Bytes32, V> entriesFrom(Bytes32 startKeyHash, int limit);

  /**
   * Retrieve entries using a custom collector
   *
   * @param handler a custom trie collector.
   * @return the requested storage entries as a map of key hash to value.
   */
  Map<Bytes32, V> entriesFrom(final Function<Node<V>, Map<Bytes32, V>> handler);

  void visitAll(Consumer<Node<V>> nodeConsumer);

  CompletableFuture<Void> visitAll(Consumer<Node<V>> nodeConsumer, ExecutorService executorService);

  void visitLeafs(final TrieIterator.LeafHandler<V> handler);
}
