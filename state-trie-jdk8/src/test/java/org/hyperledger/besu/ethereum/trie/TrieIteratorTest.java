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
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.crypto.Hash;
import org.hyperledger.besu.ethereum.trie.TrieIterator.LeafHandler;
import org.hyperledger.besu.ethereum.trie.TrieIterator.State;
import org.junit.Test;
import org.mockito.InOrder;

import java.nio.charset.StandardCharsets;
import java.util.NavigableSet;
import java.util.Random;
import java.util.TreeSet;

import static org.hyperledger.besu.ethereum.trie.CompactEncoding.bytesToPath;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class TrieIteratorTest {

  private static final Bytes32 KEY_HASH1 =
      Bytes32.fromHexString("0x5555555555555555555555555555555555555555555555555555555555555555");
  private static final Bytes32 KEY_HASH2 =
      Bytes32.fromHexString("0x5555555555555555555555555555555555555555555555555555555555555556");
  private static final Bytes PATH1 = bytesToPath(KEY_HASH1);
  private static final Bytes PATH2 = bytesToPath(KEY_HASH2);

  @SuppressWarnings("unchecked")
  private final LeafHandler<String> leafHandler = mock(LeafHandler.class);

  private final DefaultNodeFactory<String> nodeFactory =
      new DefaultNodeFactory<>(this::valueSerializer);
  private final TrieIterator<String> iterator = new TrieIterator<>(leafHandler, false);

  private Bytes valueSerializer(final String value) {
    return Bytes.wrap(value.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  public void shouldCallLeafHandlerWhenRootNodeIsALeaf() {
    final Node<String> leaf = nodeFactory.createLeaf(bytesToPath(KEY_HASH1), "Leaf");
    leaf.accept(iterator, PATH1);

    verify(leafHandler).onLeaf(KEY_HASH1, leaf);
  }

  @Test
  public void shouldNotNotifyLeafHandlerOfNullNodes() {
    NullNode.<String>instance().accept(iterator, PATH1);

    verifyNoInteractions(leafHandler);
  }

  @Test
  public void shouldConcatenatePathAndVisitChildOfExtensionNode() {
    final Node<String> leaf = nodeFactory.createLeaf(PATH1.slice(10), "Leaf");
    final Node<String> extension = nodeFactory.createExtension(PATH1.slice(0, 10), leaf);
    extension.accept(iterator, PATH1);
    verify(leafHandler).onLeaf(KEY_HASH1, leaf);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldVisitEachChildOfABranchNode() {
    when(leafHandler.onLeaf(any(Bytes32.class), any(Node.class))).thenReturn(State.CONTINUE);
    final Node<String> root =
        NullNode.<String>instance()
            .accept(new PutVisitor<>(nodeFactory, "Leaf 1"), PATH1)
            .accept(new PutVisitor<>(nodeFactory, "Leaf 2"), PATH2);
    root.accept(iterator, PATH1);

    final InOrder inOrder = inOrder(leafHandler);
    inOrder.verify(leafHandler).onLeaf(eq(KEY_HASH1), any(Node.class));
    inOrder.verify(leafHandler).onLeaf(eq(KEY_HASH2), any(Node.class));
    verifyNoMoreInteractions(leafHandler);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldStopIteratingChildrenOfBranchWhenLeafHandlerReturnsStop() {
    when(leafHandler.onLeaf(any(Bytes32.class), any(Node.class))).thenReturn(State.STOP);
    final Node<String> root =
        NullNode.<String>instance()
            .accept(new PutVisitor<>(nodeFactory, "Leaf 1"), PATH1)
            .accept(new PutVisitor<>(nodeFactory, "Leaf 2"), PATH2);
    root.accept(iterator, PATH1);

    verify(leafHandler).onLeaf(eq(KEY_HASH1), any(Node.class));
    verifyNoMoreInteractions(leafHandler);
  }

  @Test
  @SuppressWarnings({"unchecked", "MathAbsoluteRandom"})
  public void shouldIterateArbitraryStructureAccurately() {
    Node<String> root = NullNode.instance();
    final NavigableSet<Bytes32> expectedKeyHashes = new TreeSet<>();
    final Random random = new Random(-5407159858935967790L);
    Bytes32 startAtHash = Bytes32.ZERO;
    Bytes32 stopAtHash = Bytes32.ZERO;
    final int totalNodes = Math.abs(random.nextInt(1000));
    final int startNodeNumber = random.nextInt(Math.max(1, totalNodes - 1));
    final int stopNodeNumber = random.nextInt(Math.max(1, totalNodes - 1));
    for (int i = 0; i < totalNodes; i++) {
      final Bytes32 keyHash =
          Hash.keccak256(UInt256.valueOf(Math.abs(random.nextInt(Integer.MAX_VALUE))));
      root = root.accept(new PutVisitor<>(nodeFactory, "Value"), bytesToPath(keyHash));
      expectedKeyHashes.add(keyHash);
      if (i == startNodeNumber) {
        startAtHash = keyHash;
      } else if (i == stopNodeNumber) {
        stopAtHash = keyHash;
      }
    }

    final Bytes32 actualStopAtHash =
        stopAtHash.compareTo(startAtHash) >= 0 ? stopAtHash : startAtHash;
    when(leafHandler.onLeaf(any(Bytes32.class), any(Node.class))).thenReturn(State.CONTINUE);
    when(leafHandler.onLeaf(eq(actualStopAtHash), any(Node.class))).thenReturn(State.STOP);
    root.accept(iterator, bytesToPath(startAtHash));
    final InOrder inOrder = inOrder(leafHandler);
    expectedKeyHashes
        .subSet(startAtHash, true, actualStopAtHash, true)
        .forEach(keyHash -> inOrder.verify(leafHandler).onLeaf(eq(keyHash), any(Node.class)));
    verifyNoMoreInteractions(leafHandler);
  }
}
