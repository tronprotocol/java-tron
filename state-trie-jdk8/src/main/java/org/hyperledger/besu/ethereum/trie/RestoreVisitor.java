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
import java.util.function.Function;

public class RestoreVisitor<V> implements PathNodeVisitor<V> {

  private final NodeFactory<V> nodeFactory;
  private final V value;
  private final NodeVisitor<V> persistVisitor;

  public RestoreVisitor(
      final Function<V, Bytes> valueSerializer,
      final V value,
      final NodeVisitor<V> persistVisitor) {
    this.nodeFactory = new DefaultNodeFactory<>(valueSerializer);
    this.value = value;
    this.persistVisitor = persistVisitor;
  }

  @Override
  public Node<V> visit(final ExtensionNode<V> extensionNode, final Bytes path) {
    final Bytes extensionPath = extensionNode.getPath();

    final int commonPathLength = extensionPath.commonPrefixLength(path);
    assert commonPathLength < path.size()
        : "Visiting path doesn't end with a non-matching terminator";

    if (commonPathLength == extensionPath.size()) {
      final Node<V> newChild = extensionNode.getChild().accept(this, path.slice(commonPathLength));
      return extensionNode.replaceChild(newChild);
    }

    // path diverges before the end of the extension - create a new branch

    final byte leafIndex = path.get(commonPathLength);
    final Bytes leafPath = path.slice(commonPathLength + 1);

    final byte extensionIndex = extensionPath.get(commonPathLength);
    final Node<V> updatedExtension =
        extensionNode.replacePath(extensionPath.slice(commonPathLength + 1));
    final Node<V> leaf = nodeFactory.createLeaf(leafPath, value);
    final Node<V> branch =
        nodeFactory.createBranch(leafIndex, leaf, extensionIndex, updatedExtension);

    if (commonPathLength > 0) {
      return nodeFactory.createExtension(extensionPath.slice(0, commonPathLength), branch);
    } else {
      return branch;
    }
  }

  @Override
  public Node<V> visit(final BranchNode<V> branchNode, final Bytes path) {
    assert path.size() > 0 : "Visiting path doesn't end with a non-matching terminator";
    BranchNode<V> workingNode = branchNode;

    final byte childIndex = path.get(0);
    if (childIndex == CompactEncoding.LEAF_TERMINATOR) {
      return workingNode.replaceValue(value);
    }

    for (byte i = 0; i < childIndex; i++) {
      workingNode = persistNode(workingNode, i);
    }

    final Node<V> updatedChild = workingNode.child(childIndex).accept(this, path.slice(1));
    return workingNode.replaceChild(childIndex, updatedChild);
  }

  private BranchNode<V> persistNode(final BranchNode<V> parent, final byte index) {
    final Node<V> child = parent.getChildren().get(index);
    if (!(child instanceof StoredNode)) {
      child.accept(persistVisitor);
      final PersistedNode<V> persistedNode =
          new PersistedNode<>(null, child.getHash(), child.getRlpRef());
      return (BranchNode<V>) parent.replaceChild(index, persistedNode);
    } else {
      return parent;
    }
  }

  @Override
  public Node<V> visit(final LeafNode<V> leafNode, final Bytes path) {
    final Bytes leafPath = leafNode.getPath();
    final int commonPathLength = leafPath.commonPrefixLength(path);

    // Check if the current leaf node should be replaced
    if (commonPathLength == leafPath.size() && commonPathLength == path.size()) {
      return nodeFactory.createLeaf(leafPath, value);
    }

    assert commonPathLength < leafPath.size() && commonPathLength < path.size()
        : "Should not have consumed non-matching terminator";

    // The current leaf path must be split to accommodate the new value.

    final byte newLeafIndex = path.get(commonPathLength);
    final Bytes newLeafPath = path.slice(commonPathLength + 1);

    final byte updatedLeafIndex = leafPath.get(commonPathLength);

    final Node<V> updatedLeaf = leafNode.replacePath(leafPath.slice(commonPathLength + 1));
    final Node<V> leaf = nodeFactory.createLeaf(newLeafPath, value);
    final Node<V> branch =
        nodeFactory.createBranch(updatedLeafIndex, updatedLeaf, newLeafIndex, leaf);
    if (commonPathLength > 0) {
      return nodeFactory.createExtension(leafPath.slice(0, commonPathLength), branch);
    } else {
      return branch;
    }
  }

  @Override
  public Node<V> visit(final NullNode<V> nullNode, final Bytes path) {
    return nodeFactory.createLeaf(path, value);
  }

  static class PersistedNode<V> implements Node<V> {
    private final Bytes path;
    private final Bytes32 hash;
    private final Bytes refRlp;

    PersistedNode(final Bytes path, final Bytes32 hash, final Bytes refRlp) {
      this.path = path;
      this.hash = hash;
      this.refRlp = refRlp;
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
      throw new UnsupportedOperationException(
          "A persisted node cannot ever be dirty since it's loaded from storage");
    }

    @Override
    public boolean isHealNeeded() {
      return false;
    }

    @Override
    public void markHealNeeded() {
      throw new UnsupportedOperationException(
          "A persisted node cannot be healed since it's loaded from storage");
    }

    @Override
    public Node<V> accept(final PathNodeVisitor<V> visitor, final Bytes path) {
      // do nothing
      return this;
    }

    @Override
    public void accept(final NodeVisitor<V> visitor) {
      // do nothing
    }

    @Override
    public void accept(final Bytes location, final LocationNodeVisitor<V> visitor) {
      // do nothing
    }

    @Override
    public Bytes getPath() {
      return path;
    }

    @Override
    public Optional<V> getValue() {
      throw new UnsupportedOperationException(
          "A persisted node cannot have a value, as it's already been restored.");
    }

    @Override
    public List<Node<V>> getChildren() {
      return Collections.emptyList();
    }

    @Override
    public Bytes getRlp() {
      throw new UnsupportedOperationException(
          "A persisted node cannot have rlp, as it's already been restored.");
    }

    @Override
    public Bytes getRlpRef() {
      return refRlp;
    }

    @Override
    public boolean isReferencedByHash() {
      // Persisted nodes represent only nodes that are referenced by hash
      return true;
    }

    @Override
    public Bytes32 getHash() {
      return hash;
    }

    @Override
    public Node<V> replacePath(final Bytes path) {
      throw new UnsupportedOperationException(
          "A persisted node cannot be replaced, as it's already been restored.");
    }

    @Override
    public void unload() {
      throw new UnsupportedOperationException(
          "A persisted node cannot be unloaded, as it's already been restored.");
    }

    @Override
    public String print() {
      return "PersistedNode:" + "\n\tPath: " + getPath() + "\n\tHash: " + getHash();
    }
  }
}
