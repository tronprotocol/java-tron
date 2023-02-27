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
package org.apache.tuweni.units.bigints;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

import java.math.BigInteger;

public interface BytesUInt256Value<T extends UInt256Value<T>> extends Bytes32, UInt256Value<T> {

  @Override
  default long toLong() {
    return ((Bytes32) this).toLong();
  }

  @Override
  default String toHexString() {
    return ((Bytes32) this).toHexString();
  }

  @Override
  default String toShortHexString() {
    return ((Bytes) this).toShortHexString();
  }

  @Override
  default BigInteger toBigInteger() {
    return ((UInt256Value<T>) this).toBigInteger();
  }

  @Override
  default int numberOfLeadingZeros() {
    return ((Bytes) this).numberOfLeadingZeros();
  }

  @Override
  default boolean isZero() {
    return ((Bytes) this).isZero();
  }

  @Override
  default int bitLength() {
    return ((Bytes) this).bitLength();
  }
}
