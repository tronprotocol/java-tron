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

import static org.apache.tuweni.bytes.Checks.checkArgument;
import static org.apache.tuweni.bytes.Checks.checkElementIndex;

class ByteBufWrappingBytes extends AbstractBytes {

  protected final ByteBuf byteBuf;

  ByteBufWrappingBytes(ByteBuf byteBuf) {
    this.byteBuf = byteBuf;
  }

  ByteBufWrappingBytes(ByteBuf byteBuf, int offset, int length) {
    checkArgument(length >= 0, "Invalid negative length");
    int bufferLength = byteBuf.capacity();
    checkElementIndex(offset, bufferLength + 1);
    checkArgument(
        offset + length <= bufferLength,
        "Provided length %s is too big: the buffer has size %s and has only %s bytes from %s",
        length,
        bufferLength,
        bufferLength - offset,
        offset);

    if (offset == 0 && length == bufferLength) {
      this.byteBuf = byteBuf;
    } else {
      this.byteBuf = byteBuf.slice(offset, length);
    }
  }

  @Override
  public int size() {
    return byteBuf.capacity();
  }

  @Override
  public byte get(int i) {
    return byteBuf.getByte(i);
  }

  @Override
  public int getInt(int i) {
    return byteBuf.getInt(i);
  }

  @Override
  public long getLong(int i) {
    return byteBuf.getLong(i);
  }

  @Override
  public Bytes slice(int i, int length) {
    int size = byteBuf.capacity();
    if (i == 0 && length == size) {
      return this;
    }
    if (length == 0) {
      return Bytes.EMPTY;
    }

    checkElementIndex(i, size);
    checkArgument(
        i + length <= size,
        "Provided length %s is too big: the value has size %s and has only %s bytes from %s",
        length,
        size,
        size - i,
        i);

    return new ByteBufWrappingBytes(byteBuf.slice(i, length));
  }

  // MUST be overridden by mutable implementations
  @Override
  public Bytes copy() {
    return Bytes.wrap(toArray());
  }

  @Override
  public MutableBytes mutableCopy() {
    return MutableBytes.wrap(toArray());
  }

  @Override
  public void appendTo(Buffer buffer) {
    buffer.appendBuffer(Buffer.buffer(this.byteBuf));
  }

  @Override
  public byte[] toArray() {
    int size = byteBuf.capacity();
    byte[] array = new byte[size];
    byteBuf.getBytes(0, array);
    return array;
  }
}
