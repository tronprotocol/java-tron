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

import java.util.function.Consumer;

import static java.lang.String.format;

/** Static methods to work with RLP encoding/decoding. */
public abstract class RLP {
  private RLP() {}

  /** The RLP encoding of a single empty value, also known as RLP null. */
  public static final Bytes NULL = encodeOne(Bytes.EMPTY);

  public static final Bytes EMPTY_LIST;

  // RLP encoding requires payloads to be less thatn 2^64 bytes in length
  // As a result, the longest RLP strings will have a prefix composed of 1 byte encoding the type
  // of string followed by at most 8 bytes describing the length of the string
  public static final int MAX_PREFIX_SIZE = 9;

  static {
    final BytesValueRLPOutput out = new BytesValueRLPOutput();
    out.startList();
    out.endList();
    EMPTY_LIST = out.encoded();
  }

  /**
   * Creates a new {@link RLPInput} suitable for decoding the provided RLP encoded value.
   *
   * <p>The created input is strict, in that exceptions will be thrown for any malformed input,
   * either by this method or by future reads from the returned input.
   *
   * @param encoded The RLP encoded data for which to create a {@link RLPInput}.
   * @return A newly created {@link RLPInput} to decode {@code encoded}.
   * @throws MalformedRLPInputException if {@code encoded} doesn't contain a single RLP encoded item
   *     (item that can be a list itself). Note that more deeply nested corruption/malformation of
   *     the input will not be detected by this method call, but will be later when the input is
   *     read.
   */
  public static RLPInput input(final Bytes encoded) {
    return input(encoded, false);
  }

  public static RLPInput input(final Bytes encoded, final boolean lenient) {
    return new BytesValueRLPInput(encoded, lenient);
  }

  /**
   * Creates a {@link RLPOutput}, pass it to the provided consumer for writing, and then return the
   * RLP encoded result of that writing.
   *
   * <p>This method is a convenience method that is mostly meant for use with class that have a
   * method to write to an {@link RLPOutput}. For instance:
   *
   * <pre>{@code
   * class Foo {
   *   public void writeTo(RLPOutput out) {
   *     //... write some data to out ...
   *   }
   * }
   *
   * Foo f = ...;
   * // RLP encode f
   * Bytes encoded = RLPs.encode(f::writeTo);
   * }</pre>
   *
   * @param writer A method that given an {@link RLPOutput}, writes some data to it.
   * @return The RLP encoding of the data written by {@code writer}.
   */
  public static Bytes encode(final Consumer<RLPOutput> writer) {
    final BytesValueRLPOutput out = new BytesValueRLPOutput();
    writer.accept(out);
    return out.encoded();
  }

  /**
   * Encodes a single binary value into RLP.
   *
   * <p>This is equivalent (but possibly more efficient) to:
   *
   * <pre>
   * {
   *   &#64;code
   *   BytesValueRLPOutput out = new BytesValueRLPOutput();
   *   out.writeBytes(value);
   *   return out.encoded();
   * }
   * </pre>
   *
   * <p>So note in particular that the value is encoded as is (and so not as a scalar in
   * particular).
   *
   * @param value The value to encode.
   * @return The RLP encoding containing only {@code value}.
   */
  public static Bytes encodeOne(final Bytes value) {
    if (RLPEncodingHelpers.isSingleRLPByte(value)) return value;

    final MutableBytes res = MutableBytes.create(RLPEncodingHelpers.elementSize(value));
    RLPEncodingHelpers.writeElement(value, res, 0);
    return res;
  }

  /**
   * Decodes an RLP-encoded value assuming it contains a single non-list item.
   *
   * <p>This is equivalent (but possibly more efficient) to:
   *
   * <pre>{@code
   * return input(value).readBytes();
   * }</pre>
   *
   * <p>So note in particular that the value is decoded as is (and so not as a scalar in
   * particular).
   *
   * @param encodedValue The encoded RLP value.
   * @return The single value encoded in {@code encodedValue}.
   * @throws RLPException if {@code encodedValue} is not a valid RLP encoding or if it does not
   *     contains a single non-list item.
   */
  public static Bytes decodeOne(final Bytes encodedValue) {
    if (encodedValue.size() == 0) {
      throw new RLPException("Invalid empty input for RLP decoding");
    }

    final int prefix = encodedValue.get(0) & 0xFF;
    final RLPDecodingHelpers.Kind kind = RLPDecodingHelpers.Kind.of(prefix);
    if (kind.isList()) {
      throw new RLPException(format("Invalid input: value %s is an RLP list", encodedValue));
    }

    if (kind == RLPDecodingHelpers.Kind.BYTE_ELEMENT) {
      return encodedValue;
    }

    final int offset;
    final int size;
    if (kind == RLPDecodingHelpers.Kind.SHORT_ELEMENT) {
      offset = 1;
      size = prefix - 0x80;
    } else {
      final int sizeLength = prefix - 0xb7;
      if (1 + sizeLength > encodedValue.size()) {
        throw new RLPException(
            format(
                "Malformed RLP input: not enough bytes to read size of "
                    + "long item in %s: expected %d bytes but only %d",
                encodedValue, sizeLength + 1, encodedValue.size()));
      }
      offset = 1 + sizeLength;
      size = RLPDecodingHelpers.extractSize((index) -> encodedValue.get(index), 1, sizeLength);
    }
    if (offset + size != encodedValue.size()) {
      throw new RLPException(
          format(
              "Malformed RLP input: %s should be of size %d according to "
                  + "prefix byte but of size %d",
              encodedValue, offset + size, encodedValue.size()));
    }
    return encodedValue.slice(offset, size);
  }

  /**
   * Validates that the provided value is a valid RLP encoding.
   *
   * @param encodedValue The value to check.
   * @throws RLPException if {@code encodedValue} is not a valid RLP encoding.
   */
  public static void validate(final Bytes encodedValue) {
    final RLPInput in = input(encodedValue);
    while (!in.isDone()) {
      if (in.nextIsList()) {
        in.enterList();
      } else if (in.isEndOfCurrentList()) {
        in.leaveList();
      } else {
        // Skip does as much validation as can be done in general, without allocating anything.
        in.skipNext();
      }
    }
  }

  /**
   * Given a {@link Bytes} containing rlp-encoded data, determines the full length of the encoded
   * value (including the prefix) by inspecting the prefixed metadata.
   *
   * @param value the rlp-encoded byte string
   * @return the length of the encoded data, according to the prefixed metadata
   */
  public static int calculateSize(final Bytes value) {
    return RLPDecodingHelpers.rlpElementMetadata((index) -> value.get((int) index), value.size(), 0)
        .getEncodedSize();
  }
}
