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

import java.util.List;
import java.util.Optional;

public class StoredNode<V> implements Node<V> {
  private final StoredNodeFactory<V> nodeFactory;
  private final Bytes location;
  private final Bytes32 hash;
  private Node<V> loaded;

  StoredNode(final StoredNodeFactory<V> nodeFactory, final Bytes location, final Bytes32 hash) {
    this.nodeFactory = nodeFactory;
    this.location = location;
    this.hash = hash;
  }

  /**
   * @return True if the node needs to be persisted.
   */
  @Override
  public boolean isDirty() {
    return false;
  }

  /** Marks the node as being modified (needs to be persisted); */
  @Override
  public void markDirty() {
    throw new IllegalStateException(
        "A stored node cannot ever be dirty since it's loaded from storage");
  }

  @Override
  public boolean isHealNeeded() {
    return false;
  }

  @Override
  public void markHealNeeded() {
    throw new IllegalStateException(
        "A stored node cannot be healed since it's loaded from storage");
  }

  @Override
  public Node<V> accept(final PathNodeVisitor<V> visitor, final Bytes path) {
    final Node<V> node = load();
    return node.accept(visitor, path);
  }

  @Override
  public void accept(final NodeVisitor<V> visitor) {
    final Node<V> node = load();
    node.accept(visitor);
  }

  @Override
  public void accept(final Bytes location, final LocationNodeVisitor<V> visitor) {
    final Node<V> node = load();
    node.accept(location, visitor);
  }

  @Override
  public Bytes getPath() {
    return load().getPath();
  }

  @Override
  public Optional<Bytes> getLocation() {
    return Optional.ofNullable(location);
  }

  @Override
  public Optional<V> getValue() {
    return load().getValue();
  }

  @Override
  public List<Node<V>> getChildren() {
    return load().getChildren();
  }

  @Override
  public Bytes getRlp() {
    return load().getRlp();
  }

  @Override
  public Bytes getRlpRef() {
    // If this node was stored, then it must have a rlp larger than a hash
    return RLP.encodeOne(hash);
  }

  @Override
  public boolean isReferencedByHash() {
    // Stored nodes represent only nodes that are referenced by hash
    return true;
  }

  @Override
  public Bytes32 getHash() {
    return hash;
  }

  @Override
  public Node<V> replacePath(final Bytes path) {
    return load().replacePath(path);
  }

  private Node<V> load() {
    if (loaded == null) {
      loaded =
          nodeFactory
              .retrieve(location, hash)
              .orElseThrow(
                  () ->
                      new MerkleTrieException(
                          "Unable to load trie node value for hash "
                              + hash
                              + " location "
                              + location,
                          hash,
                          location));
    }

    return loaded;
  }

  @Override
  public void unload() {
    loaded = null;
  }

  @Override
  public String print() {
    if (loaded == null) {
      return "StoredNode:" + "\n\tRef: " + getRlpRef();
    } else {
      return load().print();
    }
  }
}
