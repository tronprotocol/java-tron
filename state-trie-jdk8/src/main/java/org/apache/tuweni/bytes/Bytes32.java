/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.bytes;

import java.security.SecureRandom;
import java.util.Random;

import static org.apache.tuweni.bytes.Checks.checkArgument;
import static org.apache.tuweni.bytes.Checks.checkNotNull;

/**
 * A {@link Bytes} value that is guaranteed to contain exactly 32 bytes.
 */
public interface Bytes32 extends Bytes {
  /** The number of bytes in this value - i.e. 32 */
  int SIZE = 32;

  /** A {@code Bytes32} containing all zero bytes */
  Bytes32 ZERO = Bytes32.repeat((byte) 0);

  /**
   * Generate a bytes object filled with the same byte.
   *
   * @param b the byte to fill the Bytes with
   * @return a value filled with a fixed byte
   */
  static Bytes32 repeat(byte b) {
    return new ConstantBytes32Value(b);
  }

  /**
   * Wrap the provided byte array, which must be of length 32, as a {@link Bytes32}.
   *
   * <p>
   * Note that value is not copied, only wrapped, and thus any future update to {@code value} will be reflected in the
   * returned value.
   *
   * @param bytes The bytes to wrap.
   * @return A {@link Bytes32} wrapping {@code value}.
   * @throws IllegalArgumentException if {@code value.length != 32}.
   */
  static Bytes32 wrap(byte[] bytes) {
    checkNotNull(bytes);
    checkArgument(bytes.length == SIZE, "Expected %s bytes but got %s", SIZE, bytes.length);
    return wrap(bytes, 0);
  }

  /**
   * Wrap a slice/sub-part of the provided array as a {@link Bytes32}.
   *
   * <p>
   * Note that value is not copied, only wrapped, and thus any future update to {@code value} within the wrapped parts
   * will be reflected in the returned value.
   *
   * @param bytes The bytes to wrap.
   * @param offset The index (inclusive) in {@code value} of the first byte exposed by the returned value. In other
   *        words, you will have {@code wrap(value, i).get(0) == value[i]}.
   * @return A {@link Bytes32} that exposes the bytes of {@code value} from {@code offset} (inclusive) to
   *         {@code offset + 32} (exclusive).
   * @throws IndexOutOfBoundsException if {@code offset < 0 || (value.length > 0 && offset >=
   *     value.length)}.
   * @throws IllegalArgumentException if {@code length < 0 || offset + 32 > value.length}.
   */
  static Bytes32 wrap(byte[] bytes, int offset) {
    checkNotNull(bytes);
    return new ArrayWrappingBytes32(bytes, offset);
  }

  /**
   * Secures the provided byte array, which must be of length 32, as a {@link Bytes32}.
   *
   * @param bytes The bytes to secure.
   * @return A {@link Bytes32} securing {@code value}.
   * @throws IllegalArgumentException if {@code value.length != 32}.
   */
  static Bytes32 secure(byte[] bytes) {
    checkNotNull(bytes);
    checkArgument(bytes.length == SIZE, "Expected %s bytes but got %s", SIZE, bytes.length);
    return secure(bytes, 0);
  }

  /**
   * Secures a slice/sub-part of the provided array as a {@link Bytes32}.
   *
   * @param bytes The bytes to secure.
   * @param offset The index (inclusive) in {@code value} of the first byte exposed by the returned value. In other
   *        words, you will have {@code wrap(value, i).get(0) == value[i]}.
   * @return A {@link Bytes32} that holds securely the bytes of {@code value} from {@code offset} (inclusive) to
   *         {@code offset + 32} (exclusive).
   * @throws IndexOutOfBoundsException if {@code offset < 0 || (value.length > 0 && offset >=
   *     value.length)}.
   * @throws IllegalArgumentException if {@code length < 0 || offset + 32 > value.length}.
   */
  static Bytes32 secure(byte[] bytes, int offset) {
    checkNotNull(bytes);
    return new GuardedByteArrayBytes32(bytes, offset);
  }

  /**
   * Wrap a the provided value, which must be of size 32, as a {@link Bytes32}.
   *
   * <p>
   * Note that value is not copied, only wrapped, and thus any future update to {@code value} will be reflected in the
   * returned value.
   *
   * @param value The bytes to wrap.
   * @return A {@link Bytes32} that exposes the bytes of {@code value}.
   * @throws IllegalArgumentException if {@code value.size() != 32}.
   */
  static Bytes32 wrap(Bytes value) {
    checkNotNull(value);
    if (value instanceof Bytes32) {
      return (Bytes32) value;
    }
    checkArgument(value.size() == SIZE, "Expected %s bytes but got %s", SIZE, value.size());
    return new DelegatingBytes32(value);
  }

  /**
   * Wrap a slice/sub-part of the provided value as a {@link Bytes32}.
   *
   * <p>
   * Note that value is not copied, only wrapped, and thus any future update to {@code value} within the wrapped parts
   * will be reflected in the returned value.
   *
   * @param value The bytes to wrap.
   * @param offset The index (inclusive) in {@code value} of the first byte exposed by the returned value. In other
   *        words, you will have {@code wrap(value, i).get(0) == value.get(i)}.
   * @return A {@link Bytes32} that exposes the bytes of {@code value} from {@code offset} (inclusive) to
   *         {@code offset + 32} (exclusive).
   * @throws IndexOutOfBoundsException if {@code offset < 0 || (value.size() > 0 && offset >=
   *     value.size())}.
   * @throws IllegalArgumentException if {@code length < 0 || offset + 32 > value.size()}.
   */
  static Bytes32 wrap(Bytes value, int offset) {
    checkNotNull(value);
    Bytes slice = value.slice(offset, Bytes32.SIZE);
    if (slice instanceof Bytes32) {
      return (Bytes32) slice;
    }
    return new DelegatingBytes32(slice);
  }

  /**
   * Left pad a {@link Bytes} value with a fill byte to create a {@link Bytes32}.
   *
   * @param value The bytes value pad.
   * @param fill the byte to fill with
   * @return A {@link Bytes32} that exposes the left-padded bytes of {@code value}.
   * @throws IllegalArgumentException if {@code value.size() > 32}.
   */
  static Bytes32 leftPad(Bytes value, byte fill) {
    checkNotNull(value);
    if (value instanceof Bytes32) {
      return (Bytes32) value;
    }
    checkArgument(value.size() <= SIZE, "Expected at most %s bytes but got %s", SIZE, value.size());
    MutableBytes32 result = MutableBytes32.create();
    result.fill(fill);
    value.copyTo(result, SIZE - value.size());
    return result;
  }

  /**
   * Left pad a {@link Bytes} value with zero bytes to create a {@link Bytes32}.
   *
   * @param value The bytes value pad.
   * @return A {@link Bytes32} that exposes the left-padded bytes of {@code value}.
   * @throws IllegalArgumentException if {@code value.size() > 32}.
   */
  static Bytes32 leftPad(Bytes value) {
    checkNotNull(value);
    if (value instanceof Bytes32) {
      return (Bytes32) value;
    }
    checkArgument(value.size() <= SIZE, "Expected at most %s bytes but got %s", SIZE, value.size());
    MutableBytes32 result = MutableBytes32.create();
    value.copyTo(result, SIZE - value.size());
    return result;
  }

  /**
   * Right pad a {@link Bytes} value with zero bytes to create a {@link Bytes32}.
   *
   * @param value The bytes value pad.
   * @return A {@link Bytes32} that exposes the rightw-padded bytes of {@code value}.
   * @throws IllegalArgumentException if {@code value.size() > 32}.
   */
  static Bytes32 rightPad(Bytes value) {
    checkNotNull(value);
    if (value instanceof Bytes32) {
      return (Bytes32) value;
    }
    checkArgument(value.size() <= SIZE, "Expected at most %s bytes but got %s", SIZE, value.size());
    MutableBytes32 result = MutableBytes32.create();
    value.copyTo(result, 0);
    return result;
  }

  /**
   * Parse a hexadecimal string into a {@link Bytes32}.
   *
   * <p>
   * This method is lenient in that {@code str} may of an odd length, in which case it will behave exactly as if it had
   * an additional 0 in front.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x". That representation may contain
   *        less than 32 bytes, in which case the result is left padded with zeros (see {@link #fromHexStringStrict} if
   *        this is not what you want).
   * @return The value corresponding to {@code str}.
   * @throws IllegalArgumentException if {@code str} does not correspond to a valid hexadecimal representation or
   *         contains more than 32 bytes.
   */
  static Bytes32 fromHexStringLenient(CharSequence str) {
    checkNotNull(str);
    return wrap(BytesValues.fromRawHexString(str, SIZE, true));
  }

  /**
   * Parse a hexadecimal string into a {@link Bytes32}.
   *
   * <p>
   * This method is strict in that {@code str} must of an even length.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x". That representation may contain
   *        less than 32 bytes, in which case the result is left padded with zeros (see {@link #fromHexStringStrict} if
   *        this is not what you want).
   * @return The value corresponding to {@code str}.
   * @throws IllegalArgumentException if {@code str} does not correspond to a valid hexadecimal representation, is of an
   *         odd length, or contains more than 32 bytes.
   */
  static Bytes32 fromHexString(CharSequence str) {
    checkNotNull(str);
    return wrap(BytesValues.fromRawHexString(str, SIZE, false));
  }

  /**
   * Generate random bytes.
   *
   * @return A value containing random bytes.
   */
  static Bytes32 random() {
    return random(new SecureRandom());
  }

  /**
   * Generate random bytes.
   *
   * @param generator The generator for random bytes.
   * @return A value containing random bytes.
   */
  static Bytes32 random(Random generator) {
    byte[] array = new byte[32];
    generator.nextBytes(array);
    return wrap(array);
  }

  /**
   * Parse a hexadecimal string into a {@link Bytes32}.
   *
   * <p>
   * This method is extra strict in that {@code str} must of an even length and the provided representation must have
   * exactly 32 bytes.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x".
   * @return The value corresponding to {@code str}.
   * @throws IllegalArgumentException if {@code str} does not correspond to a valid hexadecimal representation, is of an
   *         odd length or does not contain exactly 32 bytes.
   */
  static Bytes32 fromHexStringStrict(CharSequence str) {
    checkNotNull(str);
    return wrap(BytesValues.fromRawHexString(str, -1, false));
  }

  @Override
  default int size() {
    return SIZE;
  }

  /**
   * Return a bit-wise AND of these bytes and the supplied bytes.
   *
   * @param other The bytes to perform the operation with.
   * @return The result of a bit-wise AND.
   */
  default Bytes32 and(Bytes32 other) {
    return and(other, MutableBytes32.create());
  }

  /**
   * Return a bit-wise OR of these bytes and the supplied bytes.
   *
   * @param other The bytes to perform the operation with.
   * @return The result of a bit-wise OR.
   */
  default Bytes32 or(Bytes32 other) {
    return or(other, MutableBytes32.create());
  }

  /**
   * Return a bit-wise XOR of these bytes and the supplied bytes.
   *
   * @param other The bytes to perform the operation with.
   * @return The result of a bit-wise XOR.
   */
  default Bytes32 xor(Bytes32 other) {
    return xor(other, MutableBytes32.create());
  }

  @Override
  default Bytes32 not() {
    return not(MutableBytes32.create());
  }

  @Override
  default Bytes32 shiftRight(int distance) {
    return shiftRight(distance, MutableBytes32.create());
  }

  @Override
  default Bytes32 shiftLeft(int distance) {
    return shiftLeft(distance, MutableBytes32.create());
  }

  @Override
  Bytes32 copy();

  @Override
  MutableBytes32 mutableCopy();
}
