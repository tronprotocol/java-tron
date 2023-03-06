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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class NullNode<V> implements Node<V> {
  @SuppressWarnings("rawtypes")
  private static final NullNode instance = new NullNode();

  protected NullNode() {}

  @SuppressWarnings("unchecked")
  public static <V> NullNode<V> instance() {
    return instance;
  }

  @Override
  public Node<V> accept(final PathNodeVisitor<V> visitor, final Bytes path) {
    return visitor.visit(this, path);
  }

  @Override
  public void accept(final NodeVisitor<V> visitor) {
    visitor.visit(this);
  }

  @Override
  public void accept(final Bytes location, final LocationNodeVisitor<V> visitor) {
    visitor.visit(location, this);
  }

  @Override
  public Bytes getPath() {
    return Bytes.EMPTY;
  }

  @Override
  public Optional<V> getValue() {
    return Optional.empty();
  }

  @Override
  public List<Node<V>> getChildren() {
    return Collections.emptyList();
  }

  @Override
  public Bytes getRlp() {
    return MerklePatriciaTrie.EMPTY_TRIE_NODE;
  }

  @Override
  public Bytes getRlpRef() {
    return MerklePatriciaTrie.EMPTY_TRIE_NODE;
  }

  @Override
  public Bytes32 getHash() {
    return MerklePatriciaTrie.EMPTY_TRIE_NODE_HASH;
  }

  @Override
  public Node<V> replacePath(final Bytes path) {
    return this;
  }

  @Override
  public String print() {
    return "[NULL]";
  }

  @Override
  public boolean isDirty() {
    return false;
  }

  @Override
  public void markDirty() {
    // do nothing
  }

  @Override
  public boolean isHealNeeded() {
    return false;
  }

  @Override
  public void markHealNeeded() {
    // do nothing
  }
}
