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
import org.hyperledger.besu.ethereum.rlp.BytesValueRLPOutput;
import org.hyperledger.besu.ethereum.rlp.RLP;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hyperledger.besu.crypto.Hash.keccak256;

public class ExtensionNode<V> implements Node<V> {

  private final Optional<Bytes> location;
  private final Bytes path;
  private final Node<V> child;
  private final NodeFactory<V> nodeFactory;
  private WeakReference<Bytes> rlp;
  private SoftReference<Bytes32> hash;
  private boolean dirty = false;
  private boolean needHeal = false;

  ExtensionNode(
      final Bytes location,
      final Bytes path,
      final Node<V> child,
      final NodeFactory<V> nodeFactory) {
    assert (path.size() > 0);
    assert (path.get(path.size() - 1) != CompactEncoding.LEAF_TERMINATOR)
        : "Extension path ends in a leaf terminator";
    this.location = Optional.ofNullable(location);
    this.path = path;
    this.child = child;
    this.nodeFactory = nodeFactory;
  }

  ExtensionNode(final Bytes path, final Node<V> child, final NodeFactory<V> nodeFactory) {
    assert (path.size() > 0);
    assert (path.get(path.size() - 1) != CompactEncoding.LEAF_TERMINATOR)
        : "Extension path ends in a leaf terminator";
    this.location = Optional.empty();
    this.path = path;
    this.child = child;
    this.nodeFactory = nodeFactory;
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
  public Optional<Bytes> getLocation() {
    return location;
  }

  @Override
  public Bytes getPath() {
    return path;
  }

  @Override
  public Optional<V> getValue() {
    return Optional.empty();
  }

  @Override
  public List<Node<V>> getChildren() {
    return Collections.singletonList(child);
  }

  public Node<V> getChild() {
    return child;
  }

  @Override
  public Bytes getRlp() {
    if (rlp != null) {
      final Bytes encoded = rlp.get();
      if (encoded != null) {
        return encoded;
      }
    }
    final BytesValueRLPOutput out = new BytesValueRLPOutput();
    out.startList();
    out.writeBytes(CompactEncoding.encode(path));
    out.writeRaw(child.getRlpRef());
    out.endList();
    final Bytes encoded = out.encoded();
    rlp = new WeakReference<>(encoded);
    return encoded;
  }

  @Override
  public Bytes getRlpRef() {
    if (isReferencedByHash()) {
      return RLP.encodeOne(getHash());
    } else {
      return getRlp();
    }
  }

  @Override
  public Bytes32 getHash() {
    if (hash != null) {
      final Bytes32 hashed = hash.get();
      if (hashed != null) {
        return hashed;
      }
    }
    final Bytes rlp = getRlp();
    final Bytes32 hashed = keccak256(rlp);
    hash = new SoftReference<>(hashed);
    return hashed;
  }

  public Node<V> replaceChild(final Node<V> updatedChild) {
    // collapse this extension - if the child is a branch, it will create a new extension
    return updatedChild.replacePath(Bytes.concatenate(path, updatedChild.getPath()));
  }

  @Override
  public Node<V> replacePath(final Bytes path) {
    if (path.size() == 0) {
      return child;
    }
    return nodeFactory.createExtension(path, child);
  }

  @Override
  public String print() {
    final StringBuilder builder = new StringBuilder();
    final String childRep = getChild().print().replaceAll("\n\t", "\n\t\t");
    builder
        .append("Extension:")
        .append("\n\tRef: ")
        .append(getRlpRef())
        .append("\n\tPath: ")
        .append(CompactEncoding.encode(path))
        .append("\n\t")
        .append(childRep);
    return builder.toString();
  }

  @Override
  public boolean isDirty() {
    return dirty;
  }

  @Override
  public void markDirty() {
    dirty = true;
  }

  @Override
  public boolean isHealNeeded() {
    return needHeal;
  }

  @Override
  public void markHealNeeded() {
    this.needHeal = true;
  }
}
