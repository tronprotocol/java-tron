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
 * A {@link Bytes} value that is guaranteed to contain exactly 48 bytes.
 */
public interface Bytes48 extends Bytes {
  /** The number of bytes in this value - i.e. 48 */
  int SIZE = 48;

  /** A {@code Bytes48} containing all zero bytes */
  Bytes48 ZERO = wrap(new byte[SIZE]);

  /**
   * Wrap the provided byte array, which must be of length 48, as a {@link Bytes48}.
   *
   * <p>
   * Note that value is not copied, only wrapped, and thus any future update to {@code value} will be reflected in the
   * returned value.
   *
   * @param bytes The bytes to wrap.
   * @return A {@link Bytes48} wrapping {@code value}.
   * @throws IllegalArgumentException if {@code value.length != 48}.
   */
  static Bytes48 wrap(byte[] bytes) {
    checkNotNull(bytes);
    checkArgument(bytes.length == SIZE, "Expected %s bytes but got %s", SIZE, bytes.length);
    return wrap(bytes, 0);
  }

  /**
   * Wrap a slice/sub-part of the provided array as a {@link Bytes48}.
   *
   * <p>
   * Note that value is not copied, only wrapped, and thus any future update to {@code value} within the wrapped parts
   * will be reflected in the returned value.
   *
   * @param bytes The bytes to wrap.
   * @param offset The index (inclusive) in {@code value} of the first byte exposed by the returned value. In other
   *        words, you will have {@code wrap(value, i).get(0) == value[i]}.
   * @return A {@link Bytes48} that exposes the bytes of {@code value} from {@code offset} (inclusive) to
   *         {@code offset + 48} (exclusive).
   * @throws IndexOutOfBoundsException if {@code offset < 0 || (value.length > 0 && offset >=
   *     value.length)}.
   * @throws IllegalArgumentException if {@code length < 0 || offset + 48 > value.length}.
   */
  static Bytes48 wrap(byte[] bytes, int offset) {
    checkNotNull(bytes);
    return new ArrayWrappingBytes48(bytes, offset);
  }

  /**
   * Wrap a the provided value, which must be of size 48, as a {@link Bytes48}.
   *
   * <p>
   * Note that value is not copied, only wrapped, and thus any future update to {@code value} will be reflected in the
   * returned value.
   *
   * @param value The bytes to wrap.
   * @return A {@link Bytes48} that exposes the bytes of {@code value}.
   * @throws IllegalArgumentException if {@code value.size() != 48}.
   */
  static Bytes48 wrap(Bytes value) {
    checkNotNull(value);
    if (value instanceof Bytes48) {
      return (Bytes48) value;
    }
    checkArgument(value.size() == SIZE, "Expected %s bytes but got %s", SIZE, value.size());
    return new DelegatingBytes48(value);
  }

  /**
   * Wrap a slice/sub-part of the provided value as a {@link Bytes48}.
   *
   * <p>
   * Note that value is not copied, only wrapped, and thus any future update to {@code value} within the wrapped parts
   * will be reflected in the returned value.
   *
   * @param value The bytes to wrap.
   * @param offset The index (inclusive) in {@code value} of the first byte exposed by the returned value. In other
   *        words, you will have {@code wrap(value, i).get(0) == value.get(i)}.
   * @return A {@link Bytes48} that exposes the bytes of {@code value} from {@code offset} (inclusive) to
   *         {@code offset + 48} (exclusive).
   * @throws IndexOutOfBoundsException if {@code offset < 0 || (value.size() > 0 && offset >=
   *     value.size())}.
   * @throws IllegalArgumentException if {@code length < 0 || offset + 48 > value.size()}.
   */
  static Bytes48 wrap(Bytes value, int offset) {
    checkNotNull(value);
    if (value instanceof Bytes48) {
      return (Bytes48) value;
    }
    Bytes slice = value.slice(offset, Bytes48.SIZE);
    if (slice instanceof Bytes48) {
      return (Bytes48) slice;
    }
    return new DelegatingBytes48(Bytes48.wrap(slice));
  }

  /**
   * Left pad a {@link Bytes} value with zero bytes to create a {@link Bytes48}.
   *
   * @param value The bytes value pad.
   * @return A {@link Bytes48} that exposes the left-padded bytes of {@code value}.
   * @throws IllegalArgumentException if {@code value.size() > 48}.
   */
  static Bytes48 leftPad(Bytes value) {
    checkNotNull(value);
    if (value instanceof Bytes48) {
      return (Bytes48) value;
    }
    checkArgument(value.size() <= SIZE, "Expected at most %s bytes but got %s", SIZE, value.size());
    MutableBytes48 result = MutableBytes48.create();
    value.copyTo(result, SIZE - value.size());
    return result;
  }


  /**
   * Right pad a {@link Bytes} value with zero bytes to create a {@link Bytes48}.
   *
   * @param value The bytes value pad.
   * @return A {@link Bytes48} that exposes the rightw-padded bytes of {@code value}.
   * @throws IllegalArgumentException if {@code value.size() > 48}.
   */
  static Bytes48 rightPad(Bytes value) {
    checkNotNull(value);
    if (value instanceof Bytes48) {
      return (Bytes48) value;
    }
    checkArgument(value.size() <= SIZE, "Expected at most %s bytes but got %s", SIZE, value.size());
    MutableBytes48 result = MutableBytes48.create();
    value.copyTo(result, 0);
    return result;
  }

  /**
   * Parse a hexadecimal string into a {@link Bytes48}.
   *
   * <p>
   * This method is lenient in that {@code str} may of an odd length, in which case it will behave exactly as if it had
   * an additional 0 in front.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x". That representation may contain
   *        less than 48 bytes, in which case the result is left padded with zeros (see {@link #fromHexStringStrict} if
   *        this is not what you want).
   * @return The value corresponding to {@code str}.
   * @throws IllegalArgumentException if {@code str} does not correspond to a valid hexadecimal representation or
   *         contains more than 48 bytes.
   */
  static Bytes48 fromHexStringLenient(CharSequence str) {
    checkNotNull(str);
    return wrap(BytesValues.fromRawHexString(str, SIZE, true));
  }

  /**
   * Parse a hexadecimal string into a {@link Bytes48}.
   *
   * <p>
   * This method is strict in that {@code str} must of an even length.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x". That representation may contain
   *        less than 48 bytes, in which case the result is left padded with zeros (see {@link #fromHexStringStrict} if
   *        this is not what you want).
   * @return The value corresponding to {@code str}.
   * @throws IllegalArgumentException if {@code str} does not correspond to a valid hexadecimal representation, is of an
   *         odd length, or contains more than 48 bytes.
   */
  static Bytes48 fromHexString(CharSequence str) {
    checkNotNull(str);
    return wrap(BytesValues.fromRawHexString(str, SIZE, false));
  }

  /**
   * Generate random bytes.
   *
   * @return A value containing random bytes.
   */
  static Bytes48 random() {
    return random(new SecureRandom());
  }

  /**
   * Generate random bytes.
   *
   * @param generator The generator for random bytes.
   * @return A value containing random bytes.
   */
  static Bytes48 random(Random generator) {
    byte[] array = new byte[48];
    generator.nextBytes(array);
    return wrap(array);
  }

  /**
   * Parse a hexadecimal string into a {@link Bytes48}.
   *
   * <p>
   * This method is extra strict in that {@code str} must of an even length and the provided representation must have
   * exactly 48 bytes.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x".
   * @return The value corresponding to {@code str}.
   * @throws IllegalArgumentException if {@code str} does not correspond to a valid hexadecimal representation, is of an
   *         odd length or does not contain exactly 48 bytes.
   */
  static Bytes48 fromHexStringStrict(CharSequence str) {
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
  default Bytes48 and(Bytes48 other) {
    return and(other, MutableBytes48.create());
  }

  /**
   * Return a bit-wise OR of these bytes and the supplied bytes.
   *
   * @param other The bytes to perform the operation with.
   * @return The result of a bit-wise OR.
   */
  default Bytes48 or(Bytes48 other) {
    return or(other, MutableBytes48.create());
  }

  /**
   * Return a bit-wise XOR of these bytes and the supplied bytes.
   *
   * @param other The bytes to perform the operation with.
   * @return The result of a bit-wise XOR.
   */
  default Bytes48 xor(Bytes48 other) {
    return xor(other, MutableBytes48.create());
  }

  @Override
  default Bytes48 not() {
    return not(MutableBytes48.create());
  }

  @Override
  default Bytes48 shiftRight(int distance) {
    return shiftRight(distance, MutableBytes48.create());
  }

  @Override
  default Bytes48 shiftLeft(int distance) {
    return shiftLeft(distance, MutableBytes48.create());
  }

  @Override
  Bytes48 copy();

  @Override
  MutableBytes48 mutableCopy();
}
