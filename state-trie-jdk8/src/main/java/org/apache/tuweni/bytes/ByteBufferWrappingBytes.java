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

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.apache.tuweni.bytes.Checks.checkArgument;
import static org.apache.tuweni.bytes.Checks.checkElementIndex;

class ByteBufferWrappingBytes extends AbstractBytes {

  protected final ByteBuffer byteBuffer;
  protected final int offset;
  protected final int length;

  ByteBufferWrappingBytes(ByteBuffer byteBuffer) {
    this(byteBuffer, 0, byteBuffer.limit());
  }

  ByteBufferWrappingBytes(ByteBuffer byteBuffer, int offset, int length) {
    checkArgument(length >= 0, "Invalid negative length");
    int bufferLength = byteBuffer.capacity();
    if (bufferLength > 0) {
      checkElementIndex(offset, bufferLength);
    }
    checkArgument(
        offset + length <= bufferLength,
        "Provided length %s is too big: the value has only %s bytes from offset %s",
        length,
        bufferLength - offset,
        offset);

    this.byteBuffer = byteBuffer;
    this.offset = offset;
    this.length = length;
  }

  @Override
  public int size() {
    return length;
  }

  @Override
  public int getInt(int i) {
    return byteBuffer.getInt(offset + i);
  }

  @Override
  public long getLong(int i) {
    return byteBuffer.getLong(offset + i);
  }

  @Override
  public byte get(int i) {
    return byteBuffer.get(offset + i);
  }

  @Override
  public Bytes slice(int i, int length) {
    if (i == 0 && length == this.length) {
      return this;
    }
    if (length == 0) {
      return Bytes.EMPTY;
    }

    checkElementIndex(i, this.length);
    checkArgument(
        i + length <= this.length,
        "Provided length %s is too big: the value has size %s and has only %s bytes from %s",
        length,
        this.length,
        this.length - i,
        i);

    if (length == 32) {
      return new ByteBufferWrappingBytes32(byteBuffer, offset + i, length);
    }

    return new ByteBufferWrappingBytes(byteBuffer, offset + i, length);
  }

  // MUST be overridden by mutable implementations
  @Override
  public Bytes copy() {
    if (offset == 0 && length == byteBuffer.limit()) {
      return this;
    }
    return new ArrayWrappingBytes(toArray());
  }

  @Override
  public MutableBytes mutableCopy() {
    return new MutableArrayWrappingBytes(toArray());
  }

  @Override
  public void appendTo(ByteBuffer byteBuffer) {
    byteBuffer.put(this.byteBuffer);
  }

  @Override
  public byte[] toArray() {
    if (!byteBuffer.hasArray()) {
      return super.toArray();
    }
    int arrayOffset = byteBuffer.arrayOffset();
    return Arrays.copyOfRange(byteBuffer.array(), arrayOffset + offset, arrayOffset + offset + length);
  }

  @Override
  public byte[] toArrayUnsafe() {
    if (!byteBuffer.hasArray()) {
      return toArray();
    }
    byte[] array = byteBuffer.array();
    if (array.length != length || byteBuffer.arrayOffset() != 0) {
      return toArray();
    }
    return array;
  }
}
