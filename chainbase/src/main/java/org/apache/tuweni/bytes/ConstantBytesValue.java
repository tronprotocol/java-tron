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
class ConstantBytesValue extends AbstractBytes {

  private final int size;
  private final byte value;

  public ConstantBytesValue(byte b, int size) {
    this.value = b;
    this.size = size;
  }

  @Override
  public int size() {
    return this.size;
  }

  @Override
  public byte get(int i) {
    return this.value;
  }

  @Override
  public Bytes slice(int i, int length) {
    return new ConstantBytesValue(this.value, length);
  }

  @Override
  public Bytes copy() {
    return new ConstantBytesValue(this.value, this.size);
  }

  @Override
  public MutableBytes mutableCopy() {
    byte[] mutable = new byte[this.size];
    Arrays.fill(mutable, this.value);
    return new MutableArrayWrappingBytes(mutable);
  }
}
