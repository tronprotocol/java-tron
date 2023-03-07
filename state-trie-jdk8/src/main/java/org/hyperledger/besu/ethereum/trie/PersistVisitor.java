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
 *
 */

package org.hyperledger.besu.ethereum.trie;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

import java.util.function.BiConsumer;

public class PersistVisitor<V> implements NodeVisitor<V> {

  private int branchNodeCount = 0;
  private int extensionNodeCount = 0;
  private int leafNodeCount = 0;

  private final BiConsumer<Bytes32, Bytes> writer;

  public PersistVisitor(final BiConsumer<Bytes32, Bytes> writer) {
    this.writer = writer;
  }

  public Node<V> initialRoot() {
    return NullNode.instance();
  }

  public void persist(final Node<V> root) {
    if (root instanceof BranchNode) {
      visit((BranchNode<V>) root);
    } else if (root instanceof ExtensionNode) {
      visit((ExtensionNode<V>) root);
    } else if (root instanceof LeafNode) {
      visit((LeafNode<V>) root);
    } else if (root instanceof NullNode) {
      visit((NullNode<V>) root);
    }
  }

  @Override
  public void visit(final BranchNode<V> branchNode) {
    writer.accept(branchNode.getHash(), branchNode.getRlp());
    branchNodeCount++;
    branchNode.getChildren().forEach(node -> node.accept(this));
  }

  @Override
  public void visit(final ExtensionNode<V> extensionNode) {
    writer.accept(extensionNode.getHash(), extensionNode.getRlp());
    extensionNodeCount++;
    extensionNode.getChild().accept(this);
  }

  @Override
  public void visit(final LeafNode<V> leafNode) {
    writer.accept(leafNode.getHash(), leafNode.getRlp());
    leafNodeCount++;
  }

  @Override
  public void visit(final NullNode<V> nullNode) {}

  public int getBranchNodeCount() {
    return branchNodeCount;
  }

  public int getExtensionNodeCount() {
    return extensionNodeCount;
  }

  public int getLeafNodeCount() {
    return leafNodeCount;
  }
}
