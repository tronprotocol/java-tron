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

import static org.apache.tuweni.bytes.Checks.checkNotNull;

/**
 * A mutable {@link Bytes32}, that is a mutable {@link Bytes} value of exactly 32 bytes.
 */
public interface MutableBytes32 extends MutableBytes, Bytes32 {

  /**
   * Create a new mutable 32 bytes value.
   *
   * @return A newly allocated {@link MutableBytes} value.
   */
  static MutableBytes32 create() {
    return new MutableArrayWrappingBytes32(new byte[SIZE]);
  }

  /**
   * Wrap a 32 bytes array as a mutable 32 bytes value.
   *
   * @param value The value to wrap.
   * @return A {@link MutableBytes32} wrapping {@code value}.
   * @throws IllegalArgumentException if {@code value.length != 32}.
   */
  static MutableBytes32 wrap(byte[] value) {
    checkNotNull(value);
    return new MutableArrayWrappingBytes32(value);
  }

  /**
   * Wrap a the provided array as a {@link MutableBytes32}.
   *
   * <p>
   * Note that value is not copied, only wrapped, and thus any future update to {@code value} within the wrapped parts
   * will be reflected in the returned value.
   *
   * @param value The bytes to wrap.
   * @param offset The index (inclusive) in {@code value} of the first byte exposed by the returned value. In other
   *        words, you will have {@code wrap(value, i).get(0) == value[i]}.
   * @return A {@link MutableBytes32} that exposes the bytes of {@code value} from {@code offset} (inclusive) to
   *         {@code offset + 32} (exclusive).
   * @throws IndexOutOfBoundsException if {@code offset < 0 || (value.length > 0 && offset >=
   *     value.length)}.
   * @throws IllegalArgumentException if {@code length < 0 || offset + 32 > value.length}.
   */
  static MutableBytes32 wrap(byte[] value, int offset) {
    checkNotNull(value);
    return new MutableArrayWrappingBytes32(value, offset);
  }

  /**
   * Wrap a the provided value, which must be of size 32, as a {@link MutableBytes32}.
   *
   * <p>
   * Note that value is not copied, only wrapped, and thus any future update to {@code value} will be reflected in the
   * returned value.
   *
   * @param value The bytes to wrap.
   * @return A {@link MutableBytes32} that exposes the bytes of {@code value}.
   * @throws IllegalArgumentException if {@code value.size() != 32}.
   */
  static MutableBytes32 wrap(MutableBytes value) {
    checkNotNull(value);
    if (value instanceof MutableBytes32) {
      return (MutableBytes32) value;
    }
    return DelegatingMutableBytes32.delegateTo(value);
  }

  /**
   * Wrap a slice/sub-part of the provided value as a {@link MutableBytes32}.
   *
   * <p>
   * Note that the value is not copied, and thus any future update to {@code value} within the wrapped parts will be
   * reflected in the returned value.
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
  static MutableBytes32 wrap(MutableBytes value, int offset) {
    checkNotNull(value);
    if (value instanceof MutableBytes32) {
      return (MutableBytes32) value;
    }
    MutableBytes slice = value.mutableSlice(offset, Bytes32.SIZE);
    if (slice instanceof MutableBytes32) {
      return (MutableBytes32) slice;
    }
    return DelegatingMutableBytes32.delegateTo(slice);
  }
}
