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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DelegateMutableBytes32Test {

  @Test
  void failsWhenWrappingArraySmallerThan32() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> MutableBytes32.wrap(MutableBytes.wrap(new byte[31])));
    assertEquals("Expected 32 bytes but got 31", exception.getMessage());
  }

  @Test
  void failsWhenWrappingArrayLargerThan32() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> MutableBytes32.wrap(MutableBytes.wrap(new byte[33])));
    assertEquals("Expected 32 bytes but got 33", exception.getMessage());
  }

  @Test
  void testSize() {
    assertEquals(32, DelegatingMutableBytes32.delegateTo(MutableBytes32.create()).size());
  }

  @Test
  void testCopy() {
    Bytes bytes = DelegatingMutableBytes32.delegateTo(MutableBytes32.create()).copy();
    assertEquals(bytes, bytes.copy());
    assertEquals(bytes, bytes.mutableCopy());
  }

  @Test
  void testSlice() {
    Bytes bytes = DelegatingMutableBytes32.delegateTo(MutableBytes32.create()).copy();
    assertEquals(Bytes.wrap(new byte[] {bytes.get(2), bytes.get(3), bytes.get(4)}), bytes.slice(2, 3));
  }
}