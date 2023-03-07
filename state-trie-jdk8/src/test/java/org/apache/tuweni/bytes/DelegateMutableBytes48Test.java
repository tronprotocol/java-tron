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

class DelegateMutableBytes48Test {

  @Test
  void failsWhenWrappingArraySmallerThan48() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> MutableBytes48.wrap(MutableBytes.wrap(new byte[47])));
    assertEquals("Expected 48 bytes but got 47", exception.getMessage());
  }

  @Test
  void failsWhenWrappingArrayLargerThan48() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> MutableBytes48.wrap(MutableBytes.wrap(new byte[49])));
    assertEquals("Expected 48 bytes but got 49", exception.getMessage());
  }

  @Test
  void testSize() {
    assertEquals(48, DelegatingMutableBytes48.delegateTo(MutableBytes48.create()).size());
  }

  @Test
  void testCopy() {
    Bytes bytes = DelegatingMutableBytes48.delegateTo(MutableBytes48.create()).copy();
    assertEquals(bytes, bytes.copy());
    assertEquals(bytes, bytes.mutableCopy());
  }

  @Test
  void testSlice() {
    Bytes bytes = DelegatingMutableBytes48.delegateTo(MutableBytes48.create()).copy();
    assertEquals(Bytes.wrap(new byte[] {bytes.get(2), bytes.get(3), bytes.get(4)}), bytes.slice(2, 3));
  }
}