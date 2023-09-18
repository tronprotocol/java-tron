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

import java.util.ArrayList;
import java.util.List;

class ProofVisitor<V> extends GetVisitor<V> implements PathNodeVisitor<V> {

  private final Node<V> rootNode;
  private final List<Node<V>> proof = new ArrayList<>();

  ProofVisitor(final Node<V> rootNode) {
    this.rootNode = rootNode;
  }

  @Override
  public Node<V> visit(final ExtensionNode<V> extensionNode, final Bytes path) {
    maybeTrackNode(extensionNode);
    return super.visit(extensionNode, path);
  }

  @Override
  public Node<V> visit(final BranchNode<V> branchNode, final Bytes path) {
    maybeTrackNode(branchNode);
    return super.visit(branchNode, path);
  }

  @Override
  public Node<V> visit(final LeafNode<V> leafNode, final Bytes path) {
    maybeTrackNode(leafNode);
    return super.visit(leafNode, path);
  }

  @Override
  public Node<V> visit(final NullNode<V> nullNode, final Bytes path) {
    return super.visit(nullNode, path);
  }

  public List<Node<V>> getProof() {
    return proof;
  }

  private void maybeTrackNode(final Node<V> node) {
    if (node.equals(rootNode) || node.isReferencedByHash()) {
      proof.add(node);
    }
  }
}
