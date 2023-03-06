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

import java.util.function.Consumer;

public class AllNodesVisitor<V> implements NodeVisitor<V> {

  private final Consumer<Node<V>> handler;

  AllNodesVisitor(final Consumer<Node<V>> handler) {
    this.handler = handler;
  }

  @Override
  public void visit(final ExtensionNode<V> extensionNode) {
    handler.accept(extensionNode);
    acceptAndUnload(extensionNode.getChild());
  }

  @Override
  public void visit(final BranchNode<V> branchNode) {
    handler.accept(branchNode);
    branchNode.getChildren().forEach(this::acceptAndUnload);
  }

  @Override
  public void visit(final LeafNode<V> leafNode) {
    handler.accept(leafNode);
  }

  @Override
  public void visit(final NullNode<V> nullNode) {}

  private void acceptAndUnload(final Node<V> storedNode) {
    storedNode.accept(this);
    storedNode.unload();
  }
}
