/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.ethereum.rlp;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

import java.math.BigInteger;

/** An {@link RLPInput} that reads RLP encoded data from a {@link Bytes}. */
public class BytesValueRLPInput extends AbstractRLPInput {

  // The RLP encoded data.
  private final Bytes value;

  public BytesValueRLPInput(final Bytes value, final boolean lenient) {
    this(value, lenient, true);
  }

  public BytesValueRLPInput(
      final Bytes value, final boolean lenient, final boolean shouldFitExactly) {
    super(lenient);
    this.value = value;
    init(value.size(), shouldFitExactly);
  }

  @Override
  protected byte inputByte(final long offset) {
    return value.get((int) offset);
  }

  @Override
  protected Bytes inputSlice(final long offset, final int length) {
    return value.slice(Math.toIntExact(offset), length);
  }

  @Override
  protected Bytes32 inputSlice32(final long offset) {
    return Bytes32.wrap(inputSlice(offset, 32));
  }

  @Override
  protected String inputHex(final long offset, final int length) {
    return value.slice(Math.toIntExact(offset), length).toString().substring(2);
  }

  @Override
  protected BigInteger getUnsignedBigInteger(final long offset, final int length) {
    return value.slice(Math.toIntExact(offset), length).toUnsignedBigInteger();
  }

  @Override
  protected int getInt(final long offset) {
    return value.getInt(Math.toIntExact(offset));
  }

  @Override
  protected long getLong(final long offset) {
    return value.getLong(Math.toIntExact(offset));
  }

  @Override
  public Bytes raw() {
    return value;
  }
}
