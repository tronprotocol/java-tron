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
package org.hyperledger.besu.ethereum.rlp;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.MutableBytes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkState;

abstract class AbstractRLPOutput implements RLPOutput {
  /*
   * The algorithm implemented works as follows:
   *
   * Values written to the output are accumulated in the 'values' list. When a list is started, it
   * is indicated by adding a specific marker in that list (LIST_MARKER).
   * While this is gathered, we also incrementally compute the size of the payload of every list of
   * that output. Those sizes are stored in 'payloadSizes': when all the output has been added,
   * payloadSizes[i] will contain the size of the (encoded) payload of the ith list in 'values'
   * (that is, the list that starts at the ith LIST_MARKER in 'values').
   *
   * With that information gathered, encoded() can write its output in a single walk of 'values':
   * values can be encoded directly, and every time we read a list marker, we use the corresponding
   * payload size to write the proper prefix and continue.
   *
   * The main remaining aspect is how the values of 'payloadSizes' are computed. Computing the size
   * of a list without nesting inside is easy: simply add the encoded size of any newly added value
   * to the running size. The difficulty is with nesting: when we start a new list, we need to
   * track both the sizes of the previous list and the new one. To deal with that, we use the small
   * stack 'parentListStack': it stores the index in 'payloadSizes' of every currently "open" lists.
   * In other words, payloadSizes[parentListStack[stackSize - 1]] corresponds to the size of the
   * current list, the one to which newly added value are currently written (until the next call
   * to 'endList()' that is, while payloadSizes[parentListStack[stackSize - 2]] would be the size
   * of the parent list, ....
   *
   * Note that when a new value is added, we add its size only the currently running list. We should
   * add that size to that of any parent list as well, but we do so indirectly when a list is
   * finished: when 'endList()' is called, we add the size of the full list we just finished (and
   * whose size we have now completed) to its parent size.
   *
   * Side-note: this class internally and informally use "element" to refer to a non list items.
   */

  private static final Bytes LIST_MARKER = Bytes.wrap(new byte[0]);

  private final List<Bytes> values = new ArrayList<>();
  // For every value i in values, rlpEncoded.get(i) will be true only if the value stored is an
  // already encoded item.
  private final BitSet rlpEncoded = new BitSet();

  // First element is the total size of everything (the encoding may be a single non-list item, so
  // this handles that case more easily; we need that value to size out final output). Following
  // elements holds the size of the payload of the ith list in 'values'.
  private int[] payloadSizes = new int[8];
  private int listsCount = 1; // number of lists current in 'values' + 1.

  private int[] parentListStack = new int[4];
  private int stackSize = 1;

  private int currentList() {
    return parentListStack[stackSize - 1];
  }

  @Override
  public void writeBytes(final Bytes v) {
    checkState(
        stackSize > 1 || values.isEmpty(), "Terminated RLP output, cannot add more elements");
    values.add(v);
    payloadSizes[currentList()] += RLPEncodingHelpers.elementSize(v);
  }

  @Override
  public void writeRaw(final Bytes v) {
    checkState(
        stackSize > 1 || values.isEmpty(), "Terminated RLP output, cannot add more elements");
    values.add(v);
    // Mark that last value added as already encoded.
    rlpEncoded.set(values.size() - 1);
    payloadSizes[currentList()] += v.size();
  }

  @Override
  public void startList() {
    values.add(LIST_MARKER);
    ++listsCount; // we'll add a new element to payloadSizes
    ++stackSize; // and to the list stack.

    // Resize our lists if necessary.
    if (listsCount > payloadSizes.length) {
      payloadSizes = Arrays.copyOf(payloadSizes, (payloadSizes.length * 3) / 2);
    }
    if (stackSize > parentListStack.length) {
      parentListStack = Arrays.copyOf(parentListStack, (parentListStack.length * 3) / 2);
    }

    // The new current list size is store in the slot we just made room for by incrementing
    // listsCount
    parentListStack[stackSize - 1] = listsCount - 1;
  }

  @Override
  public void endList() {
    checkState(stackSize > 1, "LeaveList() called with no prior matching startList()");

    final int current = currentList();
    final int finishedListSize = RLPEncodingHelpers.listSize(payloadSizes[current]);
    --stackSize;

    // We just finished an item of our parent list, add it to that parent list size now.
    final int newCurrent = currentList();
    payloadSizes[newCurrent] += finishedListSize;
  }

  /**
   * Computes the final encoded data size.
   *
   * @return The size of the RLP-encoded data written to this output.
   * @throws IllegalStateException if some opened list haven't been closed (the output is not valid
   *     as is).
   */
  public int encodedSize() {
    checkState(stackSize == 1, "A list has been entered (startList()) but not left (endList())");
    return payloadSizes[0];
  }

  /**
   * Write the rlp encoded value to the provided {@link MutableBytes}
   *
   * @param mutableBytes the value to which the rlp-data will be written
   */
  public void writeEncoded(final MutableBytes mutableBytes) {
    // Special case where we encode only a single non-list item (note that listsCount is initially
    // set to 1, so listsCount == 1 really mean no list explicitly added to the output).
    if (listsCount == 1) {
      // writeBytes make sure we cannot have more than 1 value without a list
      assert values.size() == 1;
      final Bytes value = values.get(0);

      final int finalOffset;
      // Single non-list value.
      if (rlpEncoded.get(0)) {
        value.copyTo(mutableBytes, 0);
        finalOffset = value.size();
      } else {
        finalOffset = RLPEncodingHelpers.writeElement(value, mutableBytes, 0);
      }
      checkState(
          finalOffset == mutableBytes.size(),
          "Expected single element RLP encode to be of size %s but was of size %s.",
          mutableBytes.size(),
          finalOffset);
      return;
    }

    int offset = 0;
    int listIdx = 0;
    for (int i = 0; i < values.size(); i++) {
      final Bytes value = values.get(i);
      if (value == LIST_MARKER) {
        final int payloadSize = payloadSizes[++listIdx];
        offset = RLPEncodingHelpers.writeListHeader(payloadSize, mutableBytes, offset);
      } else if (rlpEncoded.get(i)) {
        value.copyTo(mutableBytes, offset);
        offset += value.size();
      } else {
        offset = RLPEncodingHelpers.writeElement(value, mutableBytes, offset);
      }
    }

    checkState(
        offset == mutableBytes.size(),
        "Expected RLP encoding to be of size %s but was of size %s.",
        mutableBytes.size(),
        offset);
  }

  /**
   * Check if the incoming value is 0 and writes it as 0x80, per the spec.
   *
   * @param input The value to check
   * @param writer The consumer to write the non-zero output
   */
  public void processZeroByte(final Long input, final Consumer<Long> writer) {
    // If input == 0, encode 0 value as 0x80
    if (input == 0) {
      writeRaw(Bytes.of(0x80));
    } else {
      writer.accept(input);
    }
  }
}
