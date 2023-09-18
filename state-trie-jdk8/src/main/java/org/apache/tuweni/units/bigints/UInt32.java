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

import java.math.BigInteger;
import java.nio.ByteOrder;

/**
 * An unsigned 32-bit precision number.
 * <p>
 * This is a raw {@link UInt32Value} - a 32-bit precision unsigned number of no particular unit.
 */
public final class UInt32 implements UInt32Value<UInt32> {
  private final static int MAX_CONSTANT = 0xff;
  private static UInt32[] CONSTANTS = new UInt32[MAX_CONSTANT + 1];

  static {
    CONSTANTS[0] = new UInt32(new byte[4]);
    for (int i = 1; i <= MAX_CONSTANT; ++i) {
      CONSTANTS[i] = new UInt32(
          new byte[] {
              (byte) ((i >> 24) & 0xff),
              (byte) ((i >> 16) & 0xff),
              (byte) ((i >> 8) & 0xff),
              (byte) ((i >> 0) & 0xff)});
    }
  }

  /**
   * The minimum value of a UInt32
   */
  public final static UInt32 MIN_VALUE = valueOf(0);
  /**
   * The maximum value of a UInt32
   */
  public final static UInt32 MAX_VALUE = create(new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff});
  /**
   * The value 0
   */
  public final static UInt32 ZERO = valueOf(0);
  /**
   * The value 1
   */
  public final static UInt32 ONE = valueOf(1);

  private static final BigInteger P_2_32 = BigInteger.valueOf(2).pow(32);

  private final Bytes value;

  /**
   * Return a {@code UInt32} containing the specified value.
   *
   * @param value The value to create a {@code UInt32} for.
   * @return A {@code UInt32} containing the specified value.
   * @throws IllegalArgumentException If the value is negative.
   */
  public static UInt32 valueOf(int value) {
    if (value < 0) {
      throw new IllegalArgumentException("Argument must be positive");
    }
    return create(value);
  }

  /**
   * Return a {@link UInt32} containing the specified value.
   *
   * @param value the value to create a {@link UInt32} for
   * @return a {@link UInt32} containing the specified value
   * @throws IllegalArgumentException if the value is negative or too large to be represented as a UInt32
   */
  public static UInt32 valueOf(BigInteger value) {
    if (value.bitLength() > 32) {
      throw new IllegalArgumentException("Argument is too large to represent a UInt32");
    }
    if (value.signum() < 0) {
      throw new IllegalArgumentException("Argument must be positive");
    }
    return create(value.toByteArray());
  }

  /**
   * Return a {@link UInt32} containing the value described by the specified bytes.
   *
   * @param bytes The bytes containing a {@link UInt32}. \ * @return A {@link UInt32} containing the specified value.
   * @throws IllegalArgumentException if {@code bytes.size() > 4}.
   */
  public static UInt32 fromBytes(Bytes bytes) {
    return fromBytes(bytes, ByteOrder.BIG_ENDIAN);
  }

  /**
   * Return a {@link UInt32} containing the value described by the specified bytes.
   *
   * @param bytes The bytes containing a {@link UInt32}.
   * @param byteOrder the byte order of the value
   * @return A {@link UInt32} containing the specified value.
   * @throws IllegalArgumentException if {@code bytes.size() > 4}.
   */
  public static UInt32 fromBytes(Bytes bytes, ByteOrder byteOrder) {
    if (bytes.size() > 4) {
      throw new IllegalArgumentException("Argument is greater than 4 bytes");
    }
    return create(byteOrder == ByteOrder.LITTLE_ENDIAN ? bytes.reverse() : bytes);
  }

  /**
   * Parse a hexadecimal string into a {@link UInt32}.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x". That representation may contain
   *        less than 8 bytes, in which case the result is left padded with zeros.
   * @return The value corresponding to {@code str}.
   * @throws IllegalArgumentException if {@code str} does not correspond to a valid hexadecimal representation or
   *         contains more than 8 bytes.
   */
  public static UInt32 fromHexString(String str) {
    return fromBytes(Bytes.fromHexStringLenient(str));
  }

  private static UInt32 create(Bytes value) {
    return create(value.toArrayUnsafe());
  }

  private static UInt32 create(byte[] value) {
    if (value.length == 4 && value[0] == 0 && value[1] == 0 && value[2] == 0) {
      return CONSTANTS[value[3] & 0xff];
    }
    if (value.length == 3) {
      value = new byte[] {0, value[0], value[1], value[2]};
    } else if (value.length == 2) {
      value = new byte[] {0, 0, value[0], value[1]};
    } else if (value.length == 1) {
      value = new byte[] {0, 0, 0, value[0]};
    } else if (value.length == 0) {
      value = new byte[4];
    }
    return new UInt32(value);
  }

  private static UInt32 create(int value) {
    if (value >= 0 && value <= MAX_CONSTANT) {
      return CONSTANTS[value];
    }
    return new UInt32(
        new byte[] {
            (byte) ((value >> 24) & 0xff),
            (byte) ((value >> 16) & 0xff),
            (byte) ((value >> 8) & 0xff),
            (byte) ((value >> 0) & 0xff)});
  }

  private UInt32(byte[] bytes) {
    this.value = Bytes.wrap(bytes);
  }

  @Override
  public boolean isZero() {
    return ZERO.equals(this);
  }

  @Override
  public UInt32 add(UInt32 value) {
    if (value.isZero()) {
      return this;
    }
    if (this.isZero()) {
      return value;
    }
    byte[] result = new byte[4];
    int carry = 0;
    for (int i = 3; i >= 0; i--) {
      int sum = (this.value.get(i) & 0xff) + (value.value.get(i) & 0xff) + carry;
      result[i] = (byte) sum;
      carry = sum >>> 8;
    }
    return create(result);
  }

  @Override
  public UInt32 add(int value) {
    if (value == 0) {
      return this;
    }
    return create(this.value.toInt() + value);
  }

  @Override
  public UInt32 addMod(UInt32 value, UInt32 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("addMod with zero modulus");
    }
    return create(toBigInteger().add(value.toBigInteger()).mod(modulus.toBigInteger()).intValue());
  }

  @Override
  public UInt32 addMod(long value, UInt32 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("addMod with zero modulus");
    }
    return create(toBigInteger().add(BigInteger.valueOf(value)).mod(modulus.toBigInteger()).intValue());
  }

  @Override
  public UInt32 addMod(long value, long modulus) {
    if (modulus == 0) {
      throw new ArithmeticException("addMod with zero modulus");
    }
    if (modulus < 0) {
      throw new ArithmeticException("addMod unsigned with negative modulus");
    }
    return create(toBigInteger().add(BigInteger.valueOf(value)).mod(BigInteger.valueOf(modulus)).intValue());
  }

  @Override
  public UInt32 subtract(UInt32 value) {
    if (value.isZero()) {
      return this;
    }

    byte[] result = new byte[4];
    int borrow = 0;
    for (int i = 3; 0 <= i; i--) {
      int i1 = this.value.get(i) & 0xff;
      int i2 = value.value.get(i) & 0xff;
      int col = i1 - i2 + borrow;
      borrow = (col >> 8);
      result[i] = (byte) (col & 0xff);
    }
    return create(result);
  }

  @Override
  public UInt32 subtract(int value) {
    return subtract(UInt32.create(value));
  }

  @Override
  public UInt32 multiply(UInt32 value) {
    return create(this.value.toInt() * value.value.toInt());
  }

  @Override
  public UInt32 multiply(int value) {
    if (value < 0) {
      throw new ArithmeticException("multiply unsigned by negative");
    }
    if (value == 0 || isZero()) {
      return ZERO;
    }
    if (value == 1) {
      return this;
    }
    return multiply(UInt32.valueOf(value));
  }

  @Override
  public UInt32 multiplyMod(UInt32 value, UInt32 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("multiplyMod with zero modulus");
    }
    if (isZero() || value.isZero()) {
      return ZERO;
    }
    if (ONE.equals(value)) {
      return mod(modulus);
    }
    return create(toBigInteger().multiply(value.toBigInteger()).mod(modulus.toBigInteger()).intValue());
  }

  @Override
  public UInt32 multiplyMod(int value, UInt32 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("multiplyMod with zero modulus");
    }
    if (value == 0 || this.isZero()) {
      return ZERO;
    }
    if (value == 1) {
      return mod(modulus);
    }
    if (value < 0) {
      throw new ArithmeticException("multiplyMod unsigned by negative");
    }
    return create(toBigInteger().multiply(BigInteger.valueOf(value)).mod(modulus.toBigInteger()).intValue());
  }

  @Override
  public UInt32 multiplyMod(int value, int modulus) {
    if (modulus == 0) {
      throw new ArithmeticException("multiplyMod with zero modulus");
    }
    if (modulus < 0) {
      throw new ArithmeticException("multiplyMod unsigned with negative modulus");
    }
    if (value == 0 || this.isZero()) {
      return ZERO;
    }
    if (value == 1) {
      return mod(modulus);
    }
    if (value < 0) {
      throw new ArithmeticException("multiplyMod unsigned by negative");
    }
    return create(toBigInteger().multiply(BigInteger.valueOf(value)).mod(BigInteger.valueOf(modulus)).intValue());
  }

  @Override
  public UInt32 divide(UInt32 value) {
    if (value.isZero()) {
      throw new ArithmeticException("divide by zero");
    }

    if (value.equals(ONE)) {
      return this;
    }
    return create(toBigInteger().divide(value.toBigInteger()).intValue());
  }

  @Override
  public UInt32 divide(int value) {
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
    return create(toBigInteger().divide(BigInteger.valueOf(value)).intValue());
  }

  @Override
  public UInt32 pow(UInt32 exponent) {
    return create(toBigInteger().modPow(exponent.toBigInteger(), P_2_32).intValue());
  }

  @Override
  public UInt32 pow(long exponent) {
    return create(toBigInteger().modPow(BigInteger.valueOf(exponent), P_2_32).intValue());
  }

  @Override
  public UInt32 mod(UInt32 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("mod by zero");
    }
    return create(Integer.remainderUnsigned(this.value.toInt(), modulus.value.toInt()));
  }

  @Override
  public UInt32 mod(int modulus) {
    if (modulus == 0) {
      throw new ArithmeticException("mod by zero");
    }
    if (modulus < 0) {
      throw new ArithmeticException("mod by negative");
    }
    return create(Integer.remainderUnsigned(this.value.toInt(), modulus));
  }

  /**
   * Return a bit-wise AND of this value and the supplied value.
   *
   * @param value the value to perform the operation with
   * @return the result of a bit-wise AND
   */
  public UInt32 and(UInt32 value) {
    if (this.isZero() || value.isZero()) {
      return ZERO;
    }
    return create(this.value.toInt() & value.value.toInt());
  }

  /**
   * Return a bit-wise AND of this value and the supplied bytes.
   *
   * @param bytes the bytes to perform the operation with
   * @return the result of a bit-wise AND
   * @throws IllegalArgumentException if more than 8 bytes are supplied
   */
  public UInt32 and(Bytes bytes) {
    if (bytes.size() > 4) {
      throw new IllegalArgumentException("and with more than 4 bytes");
    }
    if (this.isZero()) {
      return ZERO;
    }
    int value = bytes.toInt();
    if (value == 0) {
      return ZERO;
    }
    return create(this.value.toInt() & value);
  }

  /**
   * Return a bit-wise OR of this value and the supplied value.
   *
   * @param value the value to perform the operation with
   * @return the result of a bit-wise OR
   */
  public UInt32 or(UInt32 value) {
    return create(this.value.or(value.value));
  }

  /**
   * Return a bit-wise OR of this value and the supplied bytes.
   *
   * @param bytes the bytes to perform the operation with
   * @return the result of a bit-wise OR
   * @throws IllegalArgumentException if more than 8 bytes are supplied
   */
  public UInt32 or(Bytes bytes) {
    if (bytes.size() > 4) {
      throw new IllegalArgumentException("or with more than 4 bytes");
    }
    return create(this.value.or(bytes));
  }

  /**
   * Return a bit-wise XOR of this value and the supplied value.
   * <p>
   * If this value and the supplied value are different lengths, then the shorter will be zero-padded to the left.
   *
   * @param value the value to perform the operation with
   * @return the result of a bit-wise XOR
   * @throws IllegalArgumentException if more than 8 bytes are supplied
   */
  public UInt32 xor(UInt32 value) {
    return create(this.value.xor(value.value));
  }


  /**
   * Return a bit-wise XOR of this value and the supplied value.
   * <p>
   * If this value and the supplied value are different lengths, then the shorter will be zero-padded to the left.
   *
   * @param value the value to perform the operation with
   * @return the result of a bit-wise XOR
   * @throws IllegalArgumentException if more than 8 bytes are supplied
   */
  public UInt32 xor(int value) {
    return create(this.value.toInt() ^ value);
  }

  /**
   * Return a bit-wise XOR of this value and the supplied bytes.
   *
   * @param bytes the bytes to perform the operation with
   * @return the result of a bit-wise XOR
   * @throws IllegalArgumentException if more than 8 bytes are supplied
   */
  public UInt32 xor(Bytes bytes) {
    if (bytes.size() > 4) {
      throw new IllegalArgumentException("xor with more than 4 bytes");
    }
    return create(this.value.xor(bytes).toArrayUnsafe());
  }

  /**
   * Return a bit-wise NOT of this value.
   *
   * @return the result of a bit-wise NOT
   */
  public UInt32 not() {
    return create(this.value.not());
  }

  /**
   * Shift all bits in this value to the right.
   *
   * @param distance The number of bits to shift by.
   * @return A value containing the shifted bits.
   */
  public UInt32 shiftRight(int distance) {
    if (distance == 0) {
      return this;
    }
    if (distance >= 32) {
      return ZERO;
    }
    return create(this.value.shiftRight(distance));
  }

  /**
   * Shift all bits in this value to the left.
   *
   * @param distance The number of bits to shift by.
   * @return A value containing the shifted bits.
   */
  public UInt32 shiftLeft(int distance) {
    if (distance == 0) {
      return this;
    }
    if (distance >= 32) {
      return ZERO;
    }
    return create(this.value.shiftLeft(distance));
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }
    if (!(object instanceof UInt32)) {
      return false;
    }
    UInt32 other = (UInt32) object;
    return this.value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return Long.hashCode(this.value.toInt());
  }

  @Override
  public int compareTo(UInt32 other) {
    return Long.compareUnsigned(this.value.toInt(), other.value.toInt());
  }

  @Override
  public String toString() {
    return toHexString();
  }

  @Override
  public BigInteger toBigInteger() {
    return value.toUnsignedBigInteger();
  }

  @Override
  public UInt32 toUInt32() {
    return this;
  }

  @Override
  public Bytes toBytes() {
    return value;
  }

  @Override
  public Bytes toMinimalBytes() {
    return value.slice(value.numberOfLeadingZeroBytes());
  }

  @Override
  public int numberOfLeadingZeros() {
    return value.numberOfLeadingZeros();
  }

  @Override
  public int bitLength() {
    return 32 - value.numberOfLeadingZeros();
  }

  private static boolean isPowerOf2(long n) {
    assert n > 0;
    return (n & (n - 1)) == 0;
  }

  private static int log2(int v) {
    assert v > 0;
    return 63 - Long.numberOfLeadingZeros(v);
  }

}
