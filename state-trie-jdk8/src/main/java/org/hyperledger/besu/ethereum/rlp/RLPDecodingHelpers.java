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

import java.util.function.IntUnaryOperator;
import java.util.function.LongUnaryOperator;

/**
 * Helper static methods to facilitate RLP decoding <b>within this package</b>. Neither this class
 * nor any of its method are meant to be exposed publicly, they are too low level.
 */
class RLPDecodingHelpers {

  /** The kind of items an RLP item can be. */
  enum Kind {
    BYTE_ELEMENT,
    SHORT_ELEMENT,
    LONG_ELEMENT,
    SHORT_LIST,
    LONG_LIST;

    static Kind of(final int prefix) {
      if (prefix <= 0x7F) {
        return Kind.BYTE_ELEMENT;
      } else if (prefix <= 0xb7) {
        return Kind.SHORT_ELEMENT;
      } else if (prefix <= 0xbf) {
        return Kind.LONG_ELEMENT;
      } else if (prefix <= 0xf7) {
        return Kind.SHORT_LIST;
      } else {
        return Kind.LONG_LIST;
      }
    }

    boolean isList() {
      switch (this) {
        case SHORT_LIST:
        case LONG_LIST:
          return true;
        default:
          return false;
      }
    }
  }

  /** Read from the provided offset a size of the provided length, assuming this is enough bytes. */
  static int extractSize(final IntUnaryOperator getter, final int offset, final int sizeLength) {
    int res = 0;
    int shift = 0;
    for (int i = 0; i < sizeLength; i++) {
      res |= (getter.applyAsInt(offset + (sizeLength - 1) - i) & 0xFF) << shift;
      shift += 8;
    }
    return res;
  }

  /** Read from the provided offset a size of the provided length, assuming this is enough bytes. */
  private static int extractSizeFromLongItem(
      final LongUnaryOperator getter, final long offset, final int sizeLength) {
    if (sizeLength > 4) {
      throw new RLPException(
          "RLP item at offset "
              + offset
              + " with size value consuming "
              + sizeLength
              + " bytes exceeds max supported size of "
              + Integer.MAX_VALUE);
    }

    long res = 0;
    int shift = 0;
    for (int i = 0; i < sizeLength; i++) {
      res |= (getter.applyAsLong(offset + (sizeLength - 1) - i) & 0xFF) << shift;
      shift += 8;
    }
    try {
      return Math.toIntExact(res);
    } catch (final ArithmeticException e) {
      throw new RLPException(
          "RLP item at offset "
              + offset
              + " with size value consuming "
              + sizeLength
              + " bytes exceeds max supported size of "
              + Integer.MAX_VALUE,
          e);
    }
  }

  static RLPElementMetadata rlpElementMetadata(
      final LongUnaryOperator byteGetter, final long size, final long elementStart) {
    final int prefix = Math.toIntExact(byteGetter.applyAsLong(elementStart)) & 0xFF;
    final Kind kind = Kind.of(prefix);
    long payloadStart = 0;
    int payloadSize = 0;

    switch (kind) {
      case BYTE_ELEMENT:
        payloadStart = elementStart;
        payloadSize = 1;
        break;
      case SHORT_ELEMENT:
        payloadStart = elementStart + 1;
        payloadSize = prefix - 0x80;
        break;
      case LONG_ELEMENT:
        final int sizeLengthElt = prefix - 0xb7;
        payloadStart = elementStart + 1 + sizeLengthElt;
        payloadSize = readLongSize(byteGetter, size, elementStart, sizeLengthElt);
        break;
      case SHORT_LIST:
        payloadStart = elementStart + 1;
        payloadSize = prefix - 0xc0;
        break;
      case LONG_LIST:
        final int sizeLengthList = prefix - 0xf7;
        payloadStart = elementStart + 1 + sizeLengthList;
        payloadSize = readLongSize(byteGetter, size, elementStart, sizeLengthList);
        break;
    }

    return new RLPElementMetadata(kind, elementStart, payloadStart, payloadSize);
  }

  /** The size of the item payload for a "long" item, given the length in bytes of the said size. */
  private static int readLongSize(
      final LongUnaryOperator byteGetter,
      final long sizeOfRlpEncodedByteString,
      final long item,
      final int sizeLength) {
    // We will read sizeLength bytes from item + 1. There must be enough bytes for this or the input
    // is corrupted.
    if (sizeOfRlpEncodedByteString - (item + 1) < sizeLength) {
      throw new CorruptedRLPInputException(
          String.format(
              "Invalid RLP item: value of size %d has not enough bytes to read the %d "
                  + "bytes payload size",
              sizeOfRlpEncodedByteString, sizeLength));
    }

    // That size (which is at least 1 byte by construction) shouldn't have leading zeros.
    if (byteGetter.applyAsLong(item + 1) == 0) {
      throw new MalformedRLPInputException("Malformed RLP item: size of payload has leading zeros");
    }

    final int res = RLPDecodingHelpers.extractSizeFromLongItem(byteGetter, item + 1, sizeLength);

    // We should not have had the size written separately if it was less than 56 bytes long.
    if (res < 56) {
      throw new MalformedRLPInputException(
          String.format("Malformed RLP item: written as a long item, but size %d < 56 bytes", res));
    }

    return res;
  }

  static class RLPElementMetadata {
    final Kind kind; // The type of rlp element
    final long elementStart; // The index at which this element starts
    final long payloadStart; // The index at which the payload of this element starts
    final int payloadSize; // The size of the paylod

    RLPElementMetadata(
        final Kind kind, final long elementStart, final long payloadStart, final int payloadSize) {
      this.kind = kind;
      this.elementStart = elementStart;
      this.payloadStart = payloadStart;
      this.payloadSize = payloadSize;
    }

    /**
     * @return the size of the byte string holding the rlp-encoded value and metadata
     */
    int getEncodedSize() {
      final long encodedSize = elementEnd() - elementStart + 1;
      try {
        return Math.toIntExact(encodedSize);
      } catch (final ArithmeticException e) {
        throw new RLPException(
            String.format(
                "RLP item exceeds max supported size of %d: %d", Integer.MAX_VALUE, encodedSize),
            e);
      }
    }

    /**
     * The index of the last byte of the rlp encoded element at startIndex
     *
     * @return the index of the last byte of the rlp encoded element at startIndex
     */
    long elementEnd() {
      return payloadStart + payloadSize - 1;
    }
  }
}
