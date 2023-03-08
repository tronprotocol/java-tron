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

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;

import java.nio.ByteBuffer;

import static java.lang.String.format;
import static org.apache.tuweni.bytes.Checks.*;

/**
 * A mutable {@link Bytes} value.
 */
public interface MutableBytes extends Bytes {

  /**
   * The empty value (with 0 bytes).
   */
  MutableBytes EMPTY = wrap(new byte[0]);

  /**
   * Create a new mutable byte value.
   *
   * @param size The size of the returned value.
   * @return A {@link MutableBytes} value.
   */
  static MutableBytes create(int size) {
    if (size == 32) {
      return MutableBytes32.create();
    }
    return new MutableArrayWrappingBytes(new byte[size]);
  }

  /**
   * Wrap a byte array in a {@link MutableBytes} value.
   *
   * @param value The value to wrap.
   * @return A {@link MutableBytes} value wrapping {@code value}.
   */
  static MutableBytes wrap(byte[] value) {
    checkNotNull(value);
    return new MutableArrayWrappingBytes(value);
  }

  /**
   * Wrap a slice of a byte array as a {@link MutableBytes} value.
   *
   * <p>
   * Note that value is not copied and thus any future update to {@code value} within the slice will be reflected in the
   * returned value.
   *
   * @param value The value to wrap.
   * @param offset The index (inclusive) in {@code value} of the first byte exposed by the returned value. In other
   *        words, you will have {@code wrap(value, o, l).get(0) == value[o]}.
   * @param length The length of the resulting value.
   * @return A {@link Bytes} value that expose the bytes of {@code value} from {@code offset} (inclusive) to
   *         {@code offset + length} (exclusive).
   * @throws IndexOutOfBoundsException if {@code offset < 0 || (value.length > 0 && offset >=
   *     value.length)}.
   * @throws IllegalArgumentException if {@code length < 0 || offset + length > value.length}.
   */
  static MutableBytes wrap(byte[] value, int offset, int length) {
    checkNotNull(value);
    if (length == 32) {
      return new MutableArrayWrappingBytes32(value, offset);
    }
    return new MutableArrayWrappingBytes(value, offset, length);
  }

  /**
   * Wrap a full Vert.x {@link Buffer} as a {@link MutableBytes} value.
   *
   * <p>
   * Note that any change to the content of the buffer may be reflected in the returned value.
   *
   * @param buffer The buffer to wrap.
   * @return A {@link MutableBytes} value.
   */
  static MutableBytes wrapBuffer(Buffer buffer) {
    checkNotNull(buffer);
    if (buffer.length() == 0) {
      return EMPTY;
    }
    return new MutableBufferWrappingBytes(buffer);
  }

  /**
   * Wrap a slice of a Vert.x {@link Buffer} as a {@link MutableBytes} value.
   *
   * <p>
   * Note that any change to the content of the buffer may be reflected in the returned value, and any change to the
   * returned value will be reflected in the buffer.
   *
   * @param buffer The buffer to wrap.
   * @param offset The offset in {@code buffer} from which to expose the bytes in the returned value. That is,
   *        {@code wrapBuffer(buffer, i, 1).get(0) == buffer.getByte(i)}.
   * @param size The size of the returned value.
   * @return A {@link MutableBytes} value.
   * @throws IndexOutOfBoundsException if {@code offset < 0 || (buffer.length() > 0 && offset >=
   *     buffer.length())}.
   * @throws IllegalArgumentException if {@code length < 0 || offset + length > buffer.length()}.
   */
  static MutableBytes wrapBuffer(Buffer buffer, int offset, int size) {
    checkNotNull(buffer);
    if (size == 0) {
      return EMPTY;
    }
    return new MutableBufferWrappingBytes(buffer, offset, size);
  }

  /**
   * Wrap a full Netty {@link ByteBuf} as a {@link MutableBytes} value.
   *
   * <p>
   * Note that any change to the content of the buffer may be reflected in the returned value.
   *
   * @param byteBuf The {@link ByteBuf} to wrap.
   * @return A {@link MutableBytes} value.
   */
  static MutableBytes wrapByteBuf(ByteBuf byteBuf) {
    checkNotNull(byteBuf);
    if (byteBuf.capacity() == 0) {
      return EMPTY;
    }
    return new MutableByteBufWrappingBytes(byteBuf);
  }

  /**
   * Wrap a slice of a Netty {@link ByteBuf} as a {@link MutableBytes} value.
   *
   * <p>
   * Note that any change to the content of the buffer may be reflected in the returned value, and any change to the
   * returned value will be reflected in the buffer.
   *
   * @param byteBuf The {@link ByteBuf} to wrap.
   * @param offset The offset in {@code byteBuf} from which to expose the bytes in the returned value. That is,
   *        {@code wrapByteBuf(byteBuf, i, 1).get(0) == byteBuf.getByte(i)}.
   * @param size The size of the returned value.
   * @return A {@link MutableBytes} value.
   * @throws IndexOutOfBoundsException if {@code offset < 0 || (byteBuf.capacity() > 0 && offset >=
   *     byteBuf.capacity())}.
   * @throws IllegalArgumentException if {@code length < 0 || offset + length > byteBuf.capacity()}.
   */
  static MutableBytes wrapByteBuf(ByteBuf byteBuf, int offset, int size) {
    checkNotNull(byteBuf);
    if (size == 0) {
      return EMPTY;
    }
    return new MutableByteBufWrappingBytes(byteBuf, offset, size);
  }

  /**
   * Wrap a full Java NIO {@link ByteBuffer} as a {@link MutableBytes} value.
   *
   * <p>
   * Note that any change to the content of the buffer may be reflected in the returned value.
   *
   * @param byteBuffer The {@link ByteBuffer} to wrap.
   * @return A {@link MutableBytes} value.
   */
  static MutableBytes wrapByteBuffer(ByteBuffer byteBuffer) {
    checkNotNull(byteBuffer);
    if (byteBuffer.limit() == 0) {
      return EMPTY;
    }
    return new MutableByteBufferWrappingBytes(byteBuffer);
  }

  /**
   * Wrap a slice of a Java NIO {@link ByteBuffer} as a {@link MutableBytes} value.
   *
   * <p>
   * Note that any change to the content of the buffer may be reflected in the returned value, and any change to the
   * returned value will be reflected in the buffer.
   *
   * @param byteBuffer The {@link ByteBuffer} to wrap.
   * @param offset The offset in {@code byteBuffer} from which to expose the bytes in the returned value. That is,
   *        {@code wrapByteBuffer(byteBuffer, i, 1).get(0) == byteBuffer.getByte(i)}.
   * @param size The size of the returned value.
   * @return A {@link MutableBytes} value.
   * @throws IndexOutOfBoundsException if {@code offset < 0 || (byteBuffer.limit() > 0 && offset >=
   *     byteBuffer.limit())}.
   * @throws IllegalArgumentException if {@code length < 0 || offset + length > byteBuffer.limit()}.
   */
  static MutableBytes wrapByteBuffer(ByteBuffer byteBuffer, int offset, int size) {
    checkNotNull(byteBuffer);
    if (size == 0) {
      return EMPTY;
    }
    return new MutableByteBufferWrappingBytes(byteBuffer, offset, size);
  }

  /**
   * Create a value that contains the specified bytes in their specified order.
   *
   * @param bytes The bytes that must compose the returned value.
   * @return A value containing the specified bytes.
   */
  static MutableBytes of(byte... bytes) {
    return wrap(bytes);
  }

  /**
   * Create a value that contains the specified bytes in their specified order.
   *
   * @param bytes The bytes.
   * @return A value containing bytes are the one from {@code bytes}.
   * @throws IllegalArgumentException if any of the specified would be truncated when storing as a byte.
   */
  static MutableBytes of(int... bytes) {
    byte[] result = new byte[bytes.length];
    for (int i = 0; i < bytes.length; i++) {
      int b = bytes[i];
      checkArgument(b == (((byte) b) & 0xff), "%sth value %s does not fit a byte", i + 1, b);
      result[i] = (byte) b;
    }
    return wrap(result);
  }

  /**
   * Set a byte in this value.
   *
   * @param i The index of the byte to set.
   * @param b The value to set that byte to.
   * @throws IndexOutOfBoundsException if {@code i < 0} or {i >= size()}.
   */
  void set(int i, byte b);

  /**
   * Set a byte in this value.
   *
   * @param offset The offset of the bytes to set.
   * @param bytes The value to set bytes to.
   * @throws IndexOutOfBoundsException if {@code i < 0} or {i >= size()}.
   */
  default void set(int offset, Bytes bytes) {
    for (int i = 0; i < bytes.size(); i++) {
      set(offset + i, bytes.get(i));
    }
  }

  /**
   * Set the 4 bytes starting at the specified index to the specified integer value.
   *
   * @param i The index, which must less than or equal to {@code size() - 4}.
   * @param value The integer value.
   * @throws IndexOutOfBoundsException if {@code i < 0} or {@code i > size() - 4}.
   */
  default void setInt(int i, int value) {
    int size = size();
    checkElementIndex(i, size);
    if (i > (size - 4)) {
      throw new IndexOutOfBoundsException(
          format("Value of size %s has not enough bytes to write a 4 bytes int from index %s", size, i));
    }

    set(i++, (byte) (value >>> 24));
    set(i++, (byte) ((value >>> 16) & 0xFF));
    set(i++, (byte) ((value >>> 8) & 0xFF));
    set(i, (byte) (value & 0xFF));
  }

  /**
   * Set the 8 bytes starting at the specified index to the specified long value.
   *
   * @param i The index, which must less than or equal to {@code size() - 8}.
   * @param value The long value.
   * @throws IndexOutOfBoundsException if {@code i < 0} or {@code i > size() - 8}.
   */
  default void setLong(int i, long value) {
    int size = size();
    checkElementIndex(i, size);
    if (i > (size - 8)) {
      throw new IndexOutOfBoundsException(
          format("Value of size %s has not enough bytes to write a 8 bytes long from index %s", size, i));
    }

    set(i++, (byte) (value >>> 56));
    set(i++, (byte) ((value >>> 48) & 0xFF));
    set(i++, (byte) ((value >>> 40) & 0xFF));
    set(i++, (byte) ((value >>> 32) & 0xFF));
    set(i++, (byte) ((value >>> 24) & 0xFF));
    set(i++, (byte) ((value >>> 16) & 0xFF));
    set(i++, (byte) ((value >>> 8) & 0xFF));
    set(i, (byte) (value & 0xFF));
  }

  /**
   * Increments the value of the bytes by 1, treating the value as big endian.
   *
   * If incrementing overflows the value then all bits flip, i.e. incrementing 0xFFFF will return 0x0000.
   *
   * @return this value
   */
  default MutableBytes increment() {
    for (int i = size() - 1; i >= 0; --i) {
      if (get(i) == (byte) 0xFF) {
        set(i, (byte) 0x00);
      } else {
        byte currentValue = get(i);
        set(i, ++currentValue);
        break;
      }
    }
    return this;
  }

  /**
   * Decrements the value of the bytes by 1, treating the value as big endian.
   *
   * If decrementing underflows the value then all bits flip, i.e. decrementing 0x0000 will return 0xFFFF.
   *
   * @return this value
   */
  default MutableBytes decrement() {
    for (int i = size() - 1; i >= 0; --i) {
      if (get(i) == (byte) 0x00) {
        set(i, (byte) 0xFF);
      } else {
        byte currentValue = get(i);
        set(i, --currentValue);
        break;
      }
    }
    return this;
  }

  /**
   * Create a mutable slice of the bytes of this value.
   *
   * <p>
   * Note: the resulting slice is only a view over the original value. Holding a reference to the returned slice may
   * hold more memory than the slide represents. Use {@link #copy} on the returned slice to avoid this.
   *
   * @param i The start index for the slice.
   * @param length The length of the resulting value.
   * @return A new mutable view over the bytes of this value from index {@code i} (included) to index {@code i + length}
   *         (excluded).
   * @throws IllegalArgumentException if {@code length < 0}.
   * @throws IndexOutOfBoundsException if {@code i < 0} or {i >= size()} or {i + length > size()} .
   */
  MutableBytes mutableSlice(int i, int length);

  /**
   * Fill all the bytes of this value with the specified byte.
   *
   * @param b The byte to use to fill the value.
   */
  default void fill(byte b) {
    int size = size();
    for (int i = 0; i < size; i++) {
      set(i, b);
    }
  }

  /**
   * Set all bytes in this value to 0.
   */
  default void clear() {
    fill((byte) 0);
  }
}
