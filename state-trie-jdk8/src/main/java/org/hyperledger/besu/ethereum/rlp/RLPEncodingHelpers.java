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

/**
 * Helper static methods to facilitate RLP encoding <b>within this package</b>. Neither this class
 * nor any of its method are meant to be exposed publicly, they are too low level.
 */
class RLPEncodingHelpers {
  private RLPEncodingHelpers() {}

  static boolean isSingleRLPByte(final Bytes value) {
    return value.size() == 1 && value.get(0) >= 0;
  }

  static boolean isShortElement(final Bytes value) {
    return value.size() <= 55;
  }

  static boolean isShortList(final int payloadSize) {
    return payloadSize <= 55;
  }

  /** The encoded size of the provided value. */
  static int elementSize(final Bytes value) {
    if (isSingleRLPByte(value)) return 1;

    if (isShortElement(value)) return 1 + value.size();

    return 1 + sizeLength(value.size()) + value.size();
  }

  /** The encoded size of a list given the encoded size of its payload. */
  static int listSize(final int payloadSize) {
    int size = 1 + payloadSize;
    if (!isShortList(payloadSize)) size += sizeLength(payloadSize);
    return size;
  }

  /**
   * Writes the result of encoding the provided value to the provided destination (which must be big
   * enough).
   */
  static int writeElement(final Bytes value, final MutableBytes dest, final int destOffset) {
    final int size = value.size();
    if (isSingleRLPByte(value)) {
      dest.set(destOffset, value.get(0));
      return destOffset + 1;
    }

    if (isShortElement(value)) {
      dest.set(destOffset, (byte) (0x80 + size));
      value.copyTo(dest, destOffset + 1);
      return destOffset + 1 + size;
    }

    final int offset = writeLongMetadata(0xb7, size, dest, destOffset);
    value.copyTo(dest, offset);
    return offset + size;
  }

  /**
   * Writes the encoded header of a list provided its encoded payload size to the provided
   * destination (which must be big enough).
   */
  static int writeListHeader(final int payloadSize, final MutableBytes dest, final int destOffset) {
    if (isShortList(payloadSize)) {
      dest.set(destOffset, (byte) (0xc0 + payloadSize));
      return destOffset + 1;
    }

    return writeLongMetadata(0xf7, payloadSize, dest, destOffset);
  }

  private static int writeLongMetadata(
      final int baseCode, final int size, final MutableBytes dest, final int destOffset) {
    final int sizeLength = sizeLength(size);
    dest.set(destOffset, (byte) (baseCode + sizeLength));
    int shift = 0;
    for (int i = 0; i < sizeLength; i++) {
      dest.set(destOffset + sizeLength - i, (byte) (size >> shift));
      shift += 8;
    }
    return destOffset + 1 + sizeLength;
  }

  private static int sizeLength(final int size) {
    final int zeros = Integer.numberOfLeadingZeros(size);
    return 4 - (zeros / 8);
  }
}
