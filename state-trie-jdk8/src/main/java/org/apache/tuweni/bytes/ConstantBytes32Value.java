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

import java.util.Arrays;

/**
 * A Bytes value with just one constant value throughout. Ideal to avoid allocating large byte arrays filled with the
 * same byte.
 */
class ConstantBytes32Value extends AbstractBytes implements Bytes32 {

  private final byte value;

  public ConstantBytes32Value(byte b) {
    this.value = b;
  }

  @Override
  public int size() {
    return 32;
  }

  @Override
  public byte get(int i) {
    return this.value;
  }

  @Override
  public Bytes slice(int i, int length) {
    if (length == 32) {
      return this;
    }
    return new ConstantBytesValue(this.value, length);
  }

  @Override
  public Bytes32 copy() {
    return new ConstantBytes32Value(this.value);
  }

  @Override
  public MutableBytes32 mutableCopy() {
    byte[] mutable = new byte[32];
    Arrays.fill(mutable, this.value);
    return new MutableArrayWrappingBytes32(mutable);
  }
}
