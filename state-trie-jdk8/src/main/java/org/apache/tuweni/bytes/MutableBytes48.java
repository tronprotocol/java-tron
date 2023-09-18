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
 * A mutable {@link Bytes48}, that is a mutable {@link Bytes} value of exactly 48 bytes.
 */
public interface MutableBytes48 extends MutableBytes, Bytes48 {

  /**
   * Create a new mutable 48 bytes value.
   *
   * @return A newly allocated {@link MutableBytes} value.
   */
  static MutableBytes48 create() {
    return new MutableArrayWrappingBytes48(new byte[SIZE]);
  }

  /**
   * Wrap a 48 bytes array as a mutable 48 bytes value.
   *
   * @param value The value to wrap.
   * @return A {@link MutableBytes48} wrapping {@code value}.
   * @throws IllegalArgumentException if {@code value.length != 48}.
   */
  static MutableBytes48 wrap(byte[] value) {
    checkNotNull(value);
    return new MutableArrayWrappingBytes48(value);
  }

  /**
   * Wrap a the provided array as a {@link MutableBytes48}.
   *
   * <p>
   * Note that value is not copied, only wrapped, and thus any future update to {@code value} within the wrapped parts
   * will be reflected in the returned value.
   *
   * @param value The bytes to wrap.
   * @param offset The index (inclusive) in {@code value} of the first byte exposed by the returned value. In other
   *        words, you will have {@code wrap(value, i).get(0) == value[i]}.
   * @return A {@link MutableBytes48} that exposes the bytes of {@code value} from {@code offset} (inclusive) to
   *         {@code offset + 48} (exclusive).
   * @throws IndexOutOfBoundsException if {@code offset < 0 || (value.length > 0 && offset >=
   *     value.length)}.
   * @throws IllegalArgumentException if {@code length < 0 || offset + 48 > value.length}.
   */
  static MutableBytes48 wrap(byte[] value, int offset) {
    checkNotNull(value);
    return new MutableArrayWrappingBytes48(value, offset);
  }

  /**
   * Wrap a the provided value, which must be of size 48, as a {@link MutableBytes48}.
   *
   * <p>
   * Note that value is not copied, only wrapped, and thus any future update to {@code value} will be reflected in the
   * returned value.
   *
   * @param value The bytes to wrap.
   * @return A {@link MutableBytes48} that exposes the bytes of {@code value}.
   * @throws IllegalArgumentException if {@code value.size() != 48}.
   */
  static MutableBytes48 wrap(MutableBytes value) {
    checkNotNull(value);
    if (value instanceof MutableBytes48) {
      return (MutableBytes48) value;
    }
    return DelegatingMutableBytes48.delegateTo(value);
  }

  /**
   * Wrap a slice/sub-part of the provided value as a {@link MutableBytes48}.
   *
   * <p>
   * Note that the value is not copied, and thus any future update to {@code value} within the wrapped parts will be
   * reflected in the returned value.
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
  static MutableBytes48 wrap(MutableBytes value, int offset) {
    checkNotNull(value);
    if (value instanceof MutableBytes48) {
      return (MutableBytes48) value;
    }
    MutableBytes slice = value.mutableSlice(offset, Bytes48.SIZE);
    if (slice instanceof MutableBytes48) {
      return (MutableBytes48) slice;
    }
    return DelegatingMutableBytes48.delegateTo(slice);
  }
}
