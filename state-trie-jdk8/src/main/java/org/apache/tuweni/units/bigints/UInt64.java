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
import org.apache.tuweni.bytes.MutableBytes;

import java.math.BigInteger;

/**
 * An unsigned 64-bit precision number.
 *
 * This is a raw {@link UInt64Value} - a 64-bit precision unsigned number of no particular unit.
 */
public final class UInt64 implements UInt64Value<UInt64> {
  private final static int MAX_CONSTANT = 64;
  private static UInt64[] CONSTANTS = new UInt64[MAX_CONSTANT + 1];
  static {
    CONSTANTS[0] = new UInt64(0);
    for (int i = 1; i <= MAX_CONSTANT; ++i) {
      CONSTANTS[i] = new UInt64(i);
    }
  }

  /** The minimum value of a UInt64 */
  public final static UInt64 MIN_VALUE = valueOf(0);
  /** The maximum value of a UInt64 */
  public final static UInt64 MAX_VALUE = new UInt64(~0L);
  /** The value 0 */
  public final static UInt64 ZERO = valueOf(0);
  /** The value 1 */
  public final static UInt64 ONE = valueOf(1);

  private static final BigInteger P_2_64 = BigInteger.valueOf(2).pow(64);

  private final long value;

  /**
   * Return a {@code UInt64} containing the specified value.
   *
   * @param value The value to create a {@code UInt64} for.
   * @return A {@code UInt64} containing the specified value.
   * @throws IllegalArgumentException If the value is negative.
   */
  public static UInt64 valueOf(long value) {
    if (value < 0) {
      throw new IllegalArgumentException("Argument must be positive");
    }
    return create(value);
  }

  /**
   * Return a {@link UInt64} containing a random value.
   * 
   * @return a {@link UInt64} containing a random value
   */
  public static UInt64 random() {
    return UInt64.fromBytes(Bytes.random(8));
  }

  /**
   * Return a {@link UInt64} containing the specified value.
   *
   * @param value the value to create a {@link UInt64} for
   * @return a {@link UInt64} containing the specified value
   * @throws IllegalArgumentException if the value is negative or too large to be represented as a UInt64
   */
  public static UInt64 valueOf(BigInteger value) {
    if (value.signum() < 0) {
      throw new IllegalArgumentException("Argument must be positive");
    }
    if (value.bitLength() > 64) {
      throw new IllegalArgumentException("Argument is too large to represent a UInt64");
    }
    return create(value.longValue());
  }

  /**
   * Return a {@link UInt64} containing the value described by the specified bytes.
   *
   * @param bytes The bytes containing a {@link UInt64}.
   * @return A {@link UInt64} containing the specified value.
   * @throws IllegalArgumentException if {@code bytes.size() > 8}.
   */
  public static UInt64 fromBytes(Bytes bytes) {
    if (bytes.size() > 8) {
      throw new IllegalArgumentException("Argument is greater than 8 bytes");
    }
    return create(bytes.toLong());
  }

  /**
   * Parse a hexadecimal string into a {@link UInt64}.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x". That representation may contain
   *        less than 8 bytes, in which case the result is left padded with zeros.
   * @return The value corresponding to {@code str}.
   * @throws IllegalArgumentException if {@code str} does not correspond to a valid hexadecimal representation or
   *         contains more than 8 bytes.
   */
  public static UInt64 fromHexString(String str) {
    return fromBytes(Bytes.fromHexStringLenient(str));
  }

  private static UInt64 create(long value) {
    if (value >= 0 && value <= MAX_CONSTANT) {
      return CONSTANTS[(int) value];
    }
    return new UInt64(value);
  }

  private UInt64(long value) {
    this.value = value;
  }

  @Override
  public boolean isZero() {
    return this.value == 0;
  }

  @Override
  public UInt64 add(UInt64 value) {
    if (value.value == 0) {
      return this;
    }
    if (this.value == 0) {
      return value;
    }
    return create(this.value + value.value);
  }

  @Override
  public UInt64 add(long value) {
    if (value == 0) {
      return this;
    }
    return create(this.value + value);
  }

  @Override
  public UInt64 addMod(UInt64 value, UInt64 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("addMod with zero modulus");
    }
    return create(toBigInteger().add(value.toBigInteger()).mod(modulus.toBigInteger()).longValue());
  }

  @Override
  public UInt64 addMod(long value, UInt64 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("addMod with zero modulus");
    }
    return create(toBigInteger().add(BigInteger.valueOf(value)).mod(modulus.toBigInteger()).longValue());
  }

  @Override
  public UInt64 addMod(long value, long modulus) {
    if (modulus == 0) {
      throw new ArithmeticException("addMod with zero modulus");
    }
    if (modulus < 0) {
      throw new ArithmeticException("addMod unsigned with negative modulus");
    }
    return create(toBigInteger().add(BigInteger.valueOf(value)).mod(BigInteger.valueOf(modulus)).longValue());
  }

  @Override
  public UInt64 subtract(UInt64 value) {
    if (value.isZero()) {
      return this;
    }
    return create(this.value - value.value);
  }

  @Override
  public UInt64 subtract(long value) {
    return add(-value);
  }

  @Override
  public UInt64 multiply(UInt64 value) {
    if (this.value == 0 || value.value == 0) {
      return ZERO;
    }
    if (value.value == 1) {
      return this;
    }
    return create(this.value * value.value);
  }

  @Override
  public UInt64 multiply(long value) {
    if (value < 0) {
      throw new ArithmeticException("multiply unsigned by negative");
    }
    if (value == 0 || this.value == 0) {
      return ZERO;
    }
    if (value == 1) {
      return this;
    }
    return create(this.value * value);
  }

  @Override
  public UInt64 multiplyMod(UInt64 value, UInt64 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("multiplyMod with zero modulus");
    }
    if (this.value == 0 || value.value == 0) {
      return ZERO;
    }
    if (value.value == 1) {
      return mod(modulus);
    }
    return create(toBigInteger().multiply(value.toBigInteger()).mod(modulus.toBigInteger()).longValue());
  }

  @Override
  public UInt64 multiplyMod(long value, UInt64 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("multiplyMod with zero modulus");
    }
    if (value == 0 || this.value == 0) {
      return ZERO;
    }
    if (value == 1) {
      return mod(modulus);
    }
    if (value < 0) {
      throw new ArithmeticException("multiplyMod unsigned by negative");
    }
    return create(toBigInteger().multiply(BigInteger.valueOf(value)).mod(modulus.toBigInteger()).longValue());
  }

  @Override
  public UInt64 multiplyMod(long value, long modulus) {
    if (modulus == 0) {
      throw new ArithmeticException("multiplyMod with zero modulus");
    }
    if (modulus < 0) {
      throw new ArithmeticException("multiplyMod unsigned with negative modulus");
    }
    if (value == 0 || this.value == 0) {
      return ZERO;
    }
    if (value == 1) {
      return mod(modulus);
    }
    if (value < 0) {
      throw new ArithmeticException("multiplyMod unsigned by negative");
    }
    return create(toBigInteger().multiply(BigInteger.valueOf(value)).mod(BigInteger.valueOf(modulus)).longValue());
  }

  @Override
  public UInt64 divide(UInt64 value) {
    if (value.value == 0) {
      throw new ArithmeticException("divide by zero");
    }
    if (value.value == 1) {
      return this;
    }
    return create(toBigInteger().divide(value.toBigInteger()).longValue());
  }

  @Override
  public UInt64 divide(long value) {
    if (value == 0) {
      throw new ArithmeticException("divide by zero");
    }
    if (value < 0) {
      throw new ArithmeticException("divide unsigned by negative");
    }
    if (value == 1) {
      return this;
    }
    if (isPowerOf2(value)) {
      return shiftRight(log2(value));
    }
    return create(toBigInteger().divide(BigInteger.valueOf(value)).longValue());
  }

  @Override
  public UInt64 pow(UInt64 exponent) {
    return create(toBigInteger().modPow(exponent.toBigInteger(), P_2_64).longValue());
  }

  @Override
  public UInt64 pow(long exponent) {
    return create(toBigInteger().modPow(BigInteger.valueOf(exponent), P_2_64).longValue());
  }

  @Override
  public UInt64 mod(UInt64 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("mod by zero");
    }
    return create(toBigInteger().mod(modulus.toBigInteger()).longValue());
  }

  @Override
  public UInt64 mod(long modulus) {
    if (modulus == 0) {
      throw new ArithmeticException("mod by zero");
    }
    if (modulus < 0) {
      throw new ArithmeticException("mod by negative");
    }
    return create(this.value % modulus);
  }

  /**
   * Return a bit-wise AND of this value and the supplied value.
   *
   * @param value the value to perform the operation with
   * @return the result of a bit-wise AND
   */
  public UInt64 and(UInt64 value) {
    if (this.value == 0 || value.value == 0) {
      return ZERO;
    }
    return create(this.value & value.value);
  }

  /**
   * Return a bit-wise AND of this value and the supplied bytes.
   *
   * @param bytes the bytes to perform the operation with
   * @return the result of a bit-wise AND
   * @throws IllegalArgumentException if more than 8 bytes are supplied
   */
  public UInt64 and(Bytes bytes) {
    if (bytes.size() > 8) {
      throw new IllegalArgumentException("and with more than 8 bytes");
    }
    if (this.value == 0) {
      return ZERO;
    }
    long value = bytes.toLong();
    if (value == 0) {
      return ZERO;
    }
    return create(this.value & value);
  }

  /**
   * Return a bit-wise OR of this value and the supplied value.
   *
   * @param value the value to perform the operation with
   * @return the result of a bit-wise OR
   */
  public UInt64 or(UInt64 value) {
    return create(this.value | value.value);
  }

  /**
   * Return a bit-wise OR of this value and the supplied bytes.
   *
   * @param bytes the bytes to perform the operation with
   * @return the result of a bit-wise OR
   * @throws IllegalArgumentException if more than 8 bytes are supplied
   */
  public UInt64 or(Bytes bytes) {
    if (bytes.size() > 8) {
      throw new IllegalArgumentException("or with more than 8 bytes");
    }
    return create(this.value | bytes.toLong());
  }

  /**
   * Return a bit-wise XOR of this value and the supplied value.
   *
   * If this value and the supplied value are different lengths, then the shorter will be zero-padded to the left.
   *
   * @param value the value to perform the operation with
   * @return the result of a bit-wise XOR
   * @throws IllegalArgumentException if more than 8 bytes are supplied
   */
  public UInt64 xor(UInt64 value) {
    return create(this.value ^ value.value);
  }

  /**
   * Return a bit-wise XOR of this value and the supplied bytes.
   *
   * @param bytes the bytes to perform the operation with
   * @return the result of a bit-wise XOR
   * @throws IllegalArgumentException if more than 8 bytes are supplied
   */
  public UInt64 xor(Bytes bytes) {
    if (bytes.size() > 8) {
      throw new IllegalArgumentException("xor with more than 8 bytes");
    }
    return create(this.value ^ bytes.toLong());
  }

  /**
   * Return a bit-wise NOT of this value.
   *
   * @return the result of a bit-wise NOT
   */
  public UInt64 not() {
    return create(~this.value);
  }

  /**
   * Shift all bits in this value to the right.
   *
   * @param distance The number of bits to shift by.
   * @return A value containing the shifted bits.
   */
  public UInt64 shiftRight(int distance) {
    if (distance == 0) {
      return this;
    }
    if (distance >= 64) {
      return ZERO;
    }
    return create(this.value >>> distance);
  }

  /**
   * Shift all bits in this value to the left.
   *
   * @param distance The number of bits to shift by.
   * @return A value containing the shifted bits.
   */
  public UInt64 shiftLeft(int distance) {
    if (distance == 0) {
      return this;
    }
    if (distance >= 64) {
      return ZERO;
    }
    return create(this.value << distance);
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }
    if (!(object instanceof UInt64)) {
      return false;
    }
    UInt64 other = (UInt64) object;
    return this.value == other.value;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(this.value);
  }

  @Override
  public int compareTo(UInt64 other) {
    return Long.compareUnsigned(this.value, other.value);
  }

  @Override
  public boolean fitsInt() {
    return this.value >= 0 && this.value <= Integer.MAX_VALUE;
  }

  @Override
  public int intValue() {
    if (!fitsInt()) {
      throw new ArithmeticException("Value does not fit a 4 byte int");
    }
    return (int) this.value;
  }

  @Override
  public boolean fitsLong() {
    return this.value >= 0;
  }

  @Override
  public long toLong() {
    if (!fitsLong()) {
      throw new ArithmeticException("Value does not fit a 8 byte long");
    }
    return this.value;
  }

  @Override
  public String toString() {
    return toBigInteger().toString();
  }

  @Override
  public BigInteger toBigInteger() {
    byte[] mag = new byte[8];
    mag[0] = (byte) ((this.value >>> 56) & 0xFF);
    mag[1] = (byte) ((this.value >>> 48) & 0xFF);
    mag[2] = (byte) ((this.value >>> 40) & 0xFF);
    mag[3] = (byte) ((this.value >>> 32) & 0xFF);
    mag[4] = (byte) ((this.value >>> 24) & 0xFF);
    mag[5] = (byte) ((this.value >>> 16) & 0xFF);
    mag[6] = (byte) ((this.value >>> 8) & 0xFF);
    mag[7] = (byte) (this.value & 0xFF);
    return new BigInteger(1, mag);
  }

  @Override
  public UInt64 toUInt64() {
    return this;
  }

  @Override
  public Bytes toBytes() {
    MutableBytes bytes = MutableBytes.create(8);
    bytes.setLong(0, this.value);
    return bytes;
  }

  @Override
  public Bytes toMinimalBytes() {
    int requiredBytes = 8 - (Long.numberOfLeadingZeros(this.value) / 8);
    MutableBytes bytes = MutableBytes.create(requiredBytes);
    int j = 0;
    switch (requiredBytes) {
      case 8:
        bytes.set(j++, (byte) (this.value >>> 56));
        // fall through
      case 7:
        bytes.set(j++, (byte) ((this.value >>> 48) & 0xFF));
        // fall through
      case 6:
        bytes.set(j++, (byte) ((this.value >>> 40) & 0xFF));
        // fall through
      case 5:
        bytes.set(j++, (byte) ((this.value >>> 32) & 0xFF));
        // fall through
      case 4:
        bytes.set(j++, (byte) ((this.value >>> 24) & 0xFF));
        // fall through
      case 3:
        bytes.set(j++, (byte) ((this.value >>> 16) & 0xFF));
        // fall through
      case 2:
        bytes.set(j++, (byte) ((this.value >>> 8) & 0xFF));
        // fall through
      case 1:
        bytes.set(j, (byte) (this.value & 0xFF));
    }
    return bytes;
  }

  @Override
  public int numberOfLeadingZeros() {
    return Long.numberOfLeadingZeros(this.value);
  }

  @Override
  public int bitLength() {
    return 64 - Long.numberOfLeadingZeros(this.value);
  }

  private static boolean isPowerOf2(long n) {
    assert n > 0;
    return (n & (n - 1)) == 0;
  }

  private static int log2(long v) {
    assert v > 0;
    return 63 - Long.numberOfLeadingZeros(v);
  }

}
