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

import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class MutableBytesTest {

  @Test
  void testMutableBytesWrap() {
    MutableBytes b = MutableBytes.wrap(Bytes.fromHexString("deadbeef").toArrayUnsafe(), 1, 3);
    assertEquals(Bytes.fromHexString("adbeef"), b);
  }

  @Test
  void testClear() {
    MutableBytes b = MutableBytes.wrap(Bytes.fromHexString("deadbeef").toArrayUnsafe());
    b.clear();
    assertEquals(Bytes.fromHexString("00000000"), b);
  }

  @Test
  void testFill() {
    MutableBytes b = MutableBytes.create(2);
    b.fill((byte) 34);
    assertEquals(Bytes.fromHexString("0x2222"), b);
  }

  @Test
  void testDecrementAndIncrement() {
    MutableBytes b = MutableBytes.create(2);
    b.increment();
    assertEquals(Bytes.fromHexString("0x0001"), b);
    b.decrement();
    assertEquals(Bytes.fromHexString("0x0000"), b);

    b.fill((byte) 0xFF);
    b.decrement();
    assertEquals(Bytes.fromHexString("0xFFFE"), b);

    b = MutableBytes.wrap(Bytes.fromHexString("0x00FF").toArrayUnsafe());
    b.increment();
    assertEquals(Bytes.fromHexString("0x0100"), b);
  }

  @Test
  void setLong() {
    MutableBytes b = MutableBytes.create(8);
    b.setLong(0, 256);
    assertEquals(Bytes.fromHexString("0x0000000000000100"), b);
  }

  @Test
  void setInt() {
    MutableBytes b = MutableBytes.create(4);
    b.setInt(0, 256);
    assertEquals(Bytes.fromHexString("0x00000100"), b);
  }

  @Test
  void setIntOverflow() {
    MutableBytes b = MutableBytes.create(2);
    assertThrows(IndexOutOfBoundsException.class, () -> {
      b.setInt(0, 18096);
    });
  }

  @Test
  void setLongOverflow() {
    MutableBytes b = MutableBytes.create(6);
    assertThrows(IndexOutOfBoundsException.class, () -> {
      b.setLong(0, Long.MAX_VALUE);
    });
  }

  @Test
  void wrap32() {
    MutableBytes b = MutableBytes.create(32);
    assertTrue(b instanceof MutableBytes32);
    b = MutableBytes.wrap(Bytes.random(36).toArrayUnsafe(), 4, 32);
    assertTrue(b instanceof MutableBytes32);
  }

  @Test
  void wrapEmpty() {
    MutableBytes b = MutableBytes.wrapBuffer(Buffer.buffer());
    assertSame(MutableBytes.EMPTY, b);
    b = MutableBytes.wrapByteBuf(Unpooled.buffer(0));
    assertSame(MutableBytes.EMPTY, b);
    b = MutableBytes.wrapBuffer(Buffer.buffer(), 3, 0);
    assertSame(MutableBytes.EMPTY, b);
    b = MutableBytes.wrapByteBuf(Unpooled.buffer(), 4, 0);
    assertSame(MutableBytes.EMPTY, b);
  }

  @Test
  void testHashcodeUpdates() {
    MutableBytes dest = MutableBytes.create(32);
    int hashCode = dest.hashCode();
    dest.set(1, (byte) 123);
    assertNotEquals(hashCode, dest.hashCode());
  }

  @Test
  void testHashcodeUpdatesBuffer() {
    MutableBytes dest = MutableBytes.wrapBuffer(Buffer.buffer(new byte[4]));
    int hashCode = dest.hashCode();
    dest.set(1, (byte) 123);
    assertNotEquals(hashCode, dest.hashCode());
  }

  @Test
  void testHashcodeUpdatesByteBuffer() {
    MutableBytes dest = MutableBytes.wrapByteBuffer(ByteBuffer.wrap(new byte[4]));
    int hashCode = dest.hashCode();
    dest.set(1, (byte) 123);
    assertNotEquals(hashCode, dest.hashCode());
  }

  @Test
  void testHashcodeUpdatesByteBuf() {
    MutableBytes dest = MutableBytes.wrapByteBuf(Unpooled.buffer(4));
    int hashCode = dest.hashCode();
    dest.set(1, (byte) 123);
    assertNotEquals(hashCode, dest.hashCode());
  }
}