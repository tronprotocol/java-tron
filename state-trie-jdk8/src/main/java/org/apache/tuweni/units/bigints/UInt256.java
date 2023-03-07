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
import org.apache.tuweni.bytes.MutableBytes;
import org.apache.tuweni.bytes.MutableBytes32;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * An unsigned 256-bit precision number.
 *
 * This is a raw {@link UInt256Value} - a 256-bit precision unsigned number of no particular unit.
 */
public final class UInt256 implements UInt256Value<UInt256> {
  private final static int MAX_CONSTANT = 64;
  private final static BigInteger BI_MAX_CONSTANT = BigInteger.valueOf(MAX_CONSTANT);
  private static UInt256[] CONSTANTS = new UInt256[MAX_CONSTANT + 1];
  static {
    CONSTANTS[0] = new UInt256(Bytes32.ZERO);
    for (int i = 1; i <= MAX_CONSTANT; ++i) {
      CONSTANTS[i] = new UInt256(i);
    }
  }

  /** The minimum value of a UInt256 */
  public final static UInt256 MIN_VALUE = valueOf(0);
  /** The maximum value of a UInt256 */
  public final static UInt256 MAX_VALUE = new UInt256(Bytes32.ZERO.not());
  /** The value 0 */
  public final static UInt256 ZERO = valueOf(0);
  /** The value 1 */
  public final static UInt256 ONE = valueOf(1);

  private static final int INTS_SIZE = 32 / 4;
  // The mask is used to obtain the value of an int as if it were unsigned.
  private static final long LONG_MASK = 0xFFFFFFFFL;
  private static final BigInteger P_2_256 = BigInteger.valueOf(2).pow(256);

  // The unsigned int components of the value
  private final int[] ints;
  private Integer hashCode;

  /**
   * Return a {@code UInt256} containing the specified value.
   *
   * @param value The value to create a {@code UInt256} for.
   * @return A {@code UInt256} containing the specified value.
   * @throws IllegalArgumentException If the value is negative.
   */
  public static UInt256 valueOf(long value) {
    if (value < 0) {
      throw new IllegalArgumentException("Argument must be positive");
    }
    if (value <= MAX_CONSTANT) {
      return CONSTANTS[(int) value];
    }
    return new UInt256(value);
  }

  /**
   * Return a {@link UInt256} containing the specified value.
   *
   * @param value the value to create a {@link UInt256} for
   * @return a {@link UInt256} containing the specified value
   * @throws IllegalArgumentException if the value is negative or too large to be represented as a UInt256
   */
  public static UInt256 valueOf(BigInteger value) {
    if (value.signum() < 0) {
      throw new IllegalArgumentException("Argument must be positive");
    }
    if (value.bitLength() > 256) {
      throw new IllegalArgumentException("Argument is too large to represent a UInt256");
    }
    if (value.compareTo(BI_MAX_CONSTANT) <= 0) {
      return CONSTANTS[value.intValue()];
    }
    int[] ints = new int[INTS_SIZE];
    for (int i = INTS_SIZE - 1; i >= 0; --i) {
      ints[i] = value.intValue();
      value = value.shiftRight(32);
    }
    return new UInt256(ints);
  }

  /**
   * Return a {@link UInt256} containing the value described by the specified bytes.
   *
   * @param bytes The bytes containing a {@link UInt256}.
   * @return A {@link UInt256} containing the specified value.
   * @throws IllegalArgumentException if {@code bytes.size() > 32}.
   */
  public static UInt256 fromBytes(final Bytes bytes) {
    if (bytes instanceof UInt256) {
      return (UInt256) bytes;
    }
    if (bytes instanceof Bytes32) {
      final byte[] array = bytes.toArrayUnsafe();
      return new UInt256(
          new int[] {
              (Byte.toUnsignedInt(array[0])) << 24
                  | (Byte.toUnsignedInt(array[1]) << 16)
                  | (Byte.toUnsignedInt(array[2]) << 8)
                  | (Byte.toUnsignedInt(array[3])),
              (Byte.toUnsignedInt(array[4]) << 24)
                  | (Byte.toUnsignedInt(array[5]) << 16)
                  | (Byte.toUnsignedInt(array[6]) << 8)
                  | (Byte.toUnsignedInt(array[7])),
              (Byte.toUnsignedInt(array[8]) << 24)
                  | (Byte.toUnsignedInt(array[9]) << 16)
                  | (Byte.toUnsignedInt(array[10]) << 8)
                  | (Byte.toUnsignedInt(array[11])),
              (Byte.toUnsignedInt(array[12]) << 24)
                  | (Byte.toUnsignedInt(array[13]) << 16)
                  | (Byte.toUnsignedInt(array[14]) << 8)
                  | (Byte.toUnsignedInt(array[15])),
              (Byte.toUnsignedInt(array[16]) << 24)
                  | (Byte.toUnsignedInt(array[17]) << 16)
                  | (Byte.toUnsignedInt(array[18]) << 8)
                  | (Byte.toUnsignedInt(array[19])),
              (Byte.toUnsignedInt(array[20]) << 24)
                  | (Byte.toUnsignedInt(array[21]) << 16)
                  | (Byte.toUnsignedInt(array[22]) << 8)
                  | (Byte.toUnsignedInt(array[23])),
              (Byte.toUnsignedInt(array[24]) << 24)
                  | (Byte.toUnsignedInt(array[25]) << 16)
                  | (Byte.toUnsignedInt(array[26]) << 8)
                  | (Byte.toUnsignedInt(array[27])),
              (Byte.toUnsignedInt(array[28]) << 24)
                  | (Byte.toUnsignedInt(array[29]) << 16)
                  | (Byte.toUnsignedInt(array[30]) << 8)
                  | (Byte.toUnsignedInt(array[31]))});
    } else {
      return new UInt256(Bytes32.leftPad(bytes));
    }
  }

  /**
   * Parse a hexadecimal string into a {@link UInt256}.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x". That representation may contain
   *        less than 32 bytes, in which case the result is left padded with zeros.
   * @return The value corresponding to {@code str}.
   * @throws IllegalArgumentException if {@code str} does not correspond to a valid hexadecimal representation or
   *         contains more than 32 bytes.
   */
  public static UInt256 fromHexString(String str) {
    return new UInt256(Bytes32.fromHexStringLenient(str));
  }

  private UInt256(Bytes32 bytes) {
    this.ints = new int[INTS_SIZE];
    for (int i = 0, j = 0; i < INTS_SIZE; ++i, j += 4) {
      ints[i] = bytes.getInt(j);
    }
  }

  private UInt256(long value) {
    this.ints = new int[INTS_SIZE];
    this.ints[INTS_SIZE - 2] = (int) ((value >>> 32) & LONG_MASK);
    this.ints[INTS_SIZE - 1] = (int) (value & LONG_MASK);
  }

  private UInt256(int[] ints) {
    this.ints = ints;
  }

  @SuppressWarnings("ReferenceEquality")
  @Override
  public boolean isZero() {
    if (this == ZERO) {
      return true;
    }
    for (int i = INTS_SIZE - 1; i >= 0; --i) {
      if (this.ints[i] != 0) {
        return false;
      }
    }
    return true;
  }

  @Override
  public UInt256 add(UInt256 value) {
    if (value.isZero()) {
      return this;
    }
    if (isZero()) {
      return value;
    }
    int[] result = new int[INTS_SIZE];
    boolean constant = true;
    long sum = (this.ints[INTS_SIZE - 1] & LONG_MASK) + (value.ints[INTS_SIZE - 1] & LONG_MASK);
    result[INTS_SIZE - 1] = (int) (sum & LONG_MASK);
    if (result[INTS_SIZE - 1] < 0 || result[INTS_SIZE - 1] > MAX_CONSTANT) {
      constant = false;
    }
    for (int i = INTS_SIZE - 2; i >= 0; --i) {
      sum = (this.ints[i] & LONG_MASK) + (value.ints[i] & LONG_MASK) + (sum >>> 32);
      result[i] = (int) (sum & LONG_MASK);
      constant &= result[i] == 0;
    }
    if (constant) {
      return CONSTANTS[result[INTS_SIZE - 1]];
    }
    return new UInt256(result);
  }

  @Override
  public UInt256 add(long value) {
    if (value == 0) {
      return this;
    }
    if (value > 0 && isZero()) {
      return UInt256.valueOf(value);
    }
    int[] result = new int[INTS_SIZE];
    boolean constant = true;
    long sum = (this.ints[INTS_SIZE - 1] & LONG_MASK) + (value & LONG_MASK);
    result[INTS_SIZE - 1] = (int) (sum & LONG_MASK);
    if (result[INTS_SIZE - 1] < 0 || result[INTS_SIZE - 1] > MAX_CONSTANT) {
      constant = false;
    }
    sum = (this.ints[INTS_SIZE - 2] & LONG_MASK) + (value >>> 32) + (sum >>> 32);
    result[INTS_SIZE - 2] = (int) (sum & LONG_MASK);
    constant &= result[INTS_SIZE - 2] == 0;
    long signExtent = (value >> 63) & LONG_MASK;
    for (int i = INTS_SIZE - 3; i >= 0; --i) {
      sum = (this.ints[i] & LONG_MASK) + signExtent + (sum >>> 32);
      result[i] = (int) (sum & LONG_MASK);
      constant &= result[i] == 0;
    }
    if (constant) {
      return CONSTANTS[result[INTS_SIZE - 1]];
    }
    return new UInt256(result);
  }

  @Override
  public UInt256 addMod(UInt256 value, UInt256 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("addMod with zero modulus");
    }
    return UInt256
        .valueOf(toUnsignedBigInteger().add(value.toUnsignedBigInteger()).mod(modulus.toUnsignedBigInteger()));
  }

  @Override
  public UInt256 addMod(long value, UInt256 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("addMod with zero modulus");
    }
    return UInt256.valueOf(toUnsignedBigInteger().add(BigInteger.valueOf(value)).mod(modulus.toUnsignedBigInteger()));
  }

  @Override
  public UInt256 addMod(long value, long modulus) {
    if (modulus == 0) {
      throw new ArithmeticException("addMod with zero modulus");
    }
    if (modulus < 0) {
      throw new ArithmeticException("addMod unsigned with negative modulus");
    }
    return UInt256.valueOf(toUnsignedBigInteger().add(BigInteger.valueOf(value)).mod(BigInteger.valueOf(modulus)));
  }

  @Override
  public UInt256 subtract(UInt256 value) {
    if (value.isZero()) {
      return this;
    }

    int[] result = new int[INTS_SIZE];
    boolean constant = true;
    long sum = (this.ints[INTS_SIZE - 1] & LONG_MASK) + ((~value.ints[INTS_SIZE - 1]) & LONG_MASK) + 1;
    result[INTS_SIZE - 1] = (int) (sum & LONG_MASK);
    if (result[INTS_SIZE - 1] < 0 || result[INTS_SIZE - 1] > MAX_CONSTANT) {
      constant = false;
    }
    for (int i = INTS_SIZE - 2; i >= 0; --i) {
      sum = (this.ints[i] & LONG_MASK) + ((~value.ints[i]) & LONG_MASK) + (sum >>> 32);
      result[i] = (int) (sum & LONG_MASK);
      constant &= result[i] == 0;
    }
    if (constant) {
      return CONSTANTS[result[INTS_SIZE - 1]];
    }
    return new UInt256(result);
  }

  @Override
  public UInt256 subtract(long value) {
    return add(-value);
  }

  @Override
  public UInt256 multiply(UInt256 value) {
    if (isZero() || value.isZero()) {
      return ZERO;
    }
    if (value.equals(UInt256.ONE)) {
      return this;
    }
    if (this.equals(UInt256.ONE)) {
      return value;
    }
    return multiply(this.ints, value.ints);
  }

  private static UInt256 multiply(int[] x, int[] y) {
    int[] result = new int[INTS_SIZE + INTS_SIZE];

    long carry = 0;
    for (int j = INTS_SIZE - 1, k = INTS_SIZE + INTS_SIZE - 1; j >= 0; j--, k--) {
      long product = (y[j] & LONG_MASK) * (x[INTS_SIZE - 1] & LONG_MASK) + carry;
      result[k] = (int) product;
      carry = product >>> 32;
    }
    result[INTS_SIZE - 1] = (int) carry;

    for (int i = INTS_SIZE - 2; i >= 0; i--) {
      carry = 0;
      for (int j = INTS_SIZE - 1, k = INTS_SIZE + i; j >= 0; j--, k--) {
        long product = (y[j] & LONG_MASK) * (x[i] & LONG_MASK) + (result[k] & LONG_MASK) + carry;

        result[k] = (int) product;
        carry = product >>> 32;
      }
      result[i] = (int) carry;
    }

    boolean constant = true;
    for (int i = INTS_SIZE; i < (INTS_SIZE + INTS_SIZE) - 1; ++i) {
      constant &= (result[i] == 0);
    }
    if (constant && result[INTS_SIZE + INTS_SIZE - 1] >= 0 && result[INTS_SIZE + INTS_SIZE - 1] <= MAX_CONSTANT) {
      return CONSTANTS[result[INTS_SIZE + INTS_SIZE - 1]];
    }
    return new UInt256(Arrays.copyOfRange(result, INTS_SIZE, INTS_SIZE + INTS_SIZE));
  }

  @Override
  public UInt256 multiply(long value) {
    if (value == 0 || isZero()) {
      return ZERO;
    }
    if (value == 1) {
      return this;
    }
    if (value < 0) {
      throw new ArithmeticException("multiply unsigned by negative");
    }
    UInt256 other = new UInt256(value);
    if (this.equals(UInt256.ONE)) {
      return other;
    }
    return multiply(this.ints, other.ints);
  }

  @Override
  public UInt256 multiplyMod(UInt256 value, UInt256 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("multiplyMod with zero modulus");
    }
    if (isZero() || value.isZero()) {
      return ZERO;
    }
    if (value.equals(UInt256.ONE)) {
      return mod(modulus);
    }
    return UInt256
        .valueOf(toUnsignedBigInteger().multiply(value.toUnsignedBigInteger()).mod(modulus.toUnsignedBigInteger()));
  }

  @Override
  public UInt256 multiplyMod(long value, UInt256 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("multiplyMod with zero modulus");
    }
    if (value == 0 || isZero()) {
      return ZERO;
    }
    if (value == 1) {
      return mod(modulus);
    }
    if (value < 0) {
      throw new ArithmeticException("multiplyMod unsigned by negative");
    }
    return UInt256
        .valueOf(toUnsignedBigInteger().multiply(BigInteger.valueOf(value)).mod(modulus.toUnsignedBigInteger()));
  }

  @Override
  public UInt256 multiplyMod(long value, long modulus) {
    if (modulus == 0) {
      throw new ArithmeticException("multiplyMod with zero modulus");
    }
    if (modulus < 0) {
      throw new ArithmeticException("multiplyMod unsigned with negative modulus");
    }
    if (value == 0 || isZero()) {
      return ZERO;
    }
    if (value == 1) {
      return mod(modulus);
    }
    if (value < 0) {
      throw new ArithmeticException("multiplyMod unsigned by negative");
    }
    return UInt256.valueOf(toUnsignedBigInteger().multiply(BigInteger.valueOf(value)).mod(BigInteger.valueOf(modulus)));
  }

  @Override
  public UInt256 divide(UInt256 value) {
    if (value.isZero()) {
      throw new ArithmeticException("divide by zero");
    }
    if (value.equals(UInt256.ONE)) {
      return this;
    }
    return UInt256.valueOf(toUnsignedBigInteger().divide(value.toUnsignedBigInteger()));
  }

  @Override
  public UInt256 divide(long value) {
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
    return UInt256.valueOf(toUnsignedBigInteger().divide(BigInteger.valueOf(value)));
  }

  public UInt256 sdiv0(UInt256 divisor) {
    if (divisor.isZero()) {
      return UInt256.ZERO;
    } else {
      BigInteger result = this.toSignedBigInteger().divide(divisor.toSignedBigInteger());
      Bytes resultBytes = Bytes.wrap(result.toByteArray());
      if (resultBytes.size() > 32) {
        resultBytes = resultBytes.slice(resultBytes.size() - 32, 32);
      }
      return UInt256.fromBytes(Bytes32.leftPad(resultBytes, result.signum() < 0 ? (byte) 0xFF : 0x00));
    }
  }

  @Override
  public UInt256 divideCeil(UInt256 value) {
    return this.divide(value).add(this.mod(value).isZero() ? 0 : 1);
  }

  @Override
  public UInt256 divideCeil(long value) {
    return this.divide(value).add(this.mod(value).isZero() ? 0 : 1);
  }

  @Override
  public UInt256 pow(UInt256 exponent) {
    return UInt256.valueOf(toUnsignedBigInteger().modPow(exponent.toUnsignedBigInteger(), P_2_256));
  }

  @Override
  public UInt256 pow(long exponent) {
    return UInt256.valueOf(toUnsignedBigInteger().modPow(BigInteger.valueOf(exponent), P_2_256));
  }

  @Override
  public UInt256 mod(UInt256 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("mod by zero");
    }
    return UInt256.valueOf(toUnsignedBigInteger().mod(modulus.toUnsignedBigInteger()));
  }

  @Override
  public UInt256 mod(long modulus) {
    if (modulus == 0) {
      throw new ArithmeticException("mod by zero");
    }
    if (modulus < 0) {
      throw new ArithmeticException("mod by negative");
    }
    if (isPowerOf2(modulus)) {
      int log2 = log2(modulus);
      int d = log2 / 32;
      int s = log2 % 32;
      assert (d == 0 || d == 1);

      int[] result = new int[INTS_SIZE];
      // Mask the byte at d to only include the s right-most bits
      result[INTS_SIZE - 1 - d] = this.ints[INTS_SIZE - 1 - d] & ~(0xFFFFFFFF << s);
      if (d != 0) {
        result[INTS_SIZE - 1] = this.ints[INTS_SIZE - 1];
      }
      return new UInt256(result);
    }
    return UInt256.valueOf(toUnsignedBigInteger().mod(BigInteger.valueOf(modulus)));
  }

  @Override
  public UInt256 mod0(UInt256 modulus) {
    if (modulus.equals(UInt256.ZERO)) {
      return UInt256.ZERO;
    }
    return mod(modulus);
  }

  /**
   * Returns a value that is the {@code (this signed mod modulus)}, or 0 if modulus is 0.
   *
   * @param modulus The modulus.
   * @return {@code this signed mod modulus}.
   */
  public UInt256 smod0(UInt256 modulus) {
    if (modulus.equals(UInt256.ZERO)) {
      return UInt256.ZERO;
    }

    BigInteger bi = this.toSignedBigInteger();
    BigInteger result = bi.abs().mod(modulus.toSignedBigInteger().abs());

    if (bi.signum() < 0) {
      result = result.negate();
    }

    Bytes resultBytes = Bytes.wrap(result.toByteArray());
    if (resultBytes.size() > 32) {
      resultBytes = resultBytes.slice(resultBytes.size() - 32, 32);
    }

    return UInt256.fromBytes(Bytes32.leftPad(resultBytes, result.signum() < 0 ? (byte) 0xFF : 0x00));
  }

  @Override
  public UInt256 mod0(long modulus) {
    if (modulus == 0) {
      return UInt256.ZERO;
    }
    if (modulus < 0) {
      throw new ArithmeticException("mod by negative");
    }
    return mod(modulus);
  }

  /**
   * Return a bit-wise AND of this value and the supplied value.
   *
   * @param value the value to perform the operation with
   * @return the result of a bit-wise AND
   */
  public UInt256 and(UInt256 value) {
    int[] result = new int[INTS_SIZE];
    for (int i = INTS_SIZE - 1; i >= 0; --i) {
      result[i] = this.ints[i] & value.ints[i];
    }
    return new UInt256(result);
  }

  /**
   * Return a bit-wise AND of this value and the supplied bytes.
   *
   * @param bytes the bytes to perform the operation with
   * @return the result of a bit-wise AND
   */
  @Override
  public UInt256 and(Bytes32 bytes) {
    int[] result = new int[INTS_SIZE];
    for (int i = INTS_SIZE - 1, j = 28; i >= 0; --i, j -= 4) {
      int other = ((int) bytes.get(j) & 0xFF) << 24;
      other |= ((int) bytes.get(j + 1) & 0xFF) << 16;
      other |= ((int) bytes.get(i + 2) & 0xFF) << 8;
      other |= ((int) bytes.get(i + 3) & 0xFF);
      result[i] = this.ints[i] & other;
    }
    return new UInt256(result);
  }

  /**
   * Return a bit-wise OR of this value and the supplied value.
   *
   * @param value the value to perform the operation with
   * @return the result of a bit-wise OR
   */
  public UInt256 or(UInt256 value) {
    int[] result = new int[INTS_SIZE];
    for (int i = INTS_SIZE - 1; i >= 0; --i) {
      result[i] = this.ints[i] | value.ints[i];
    }
    return new UInt256(result);
  }

  /**
   * Return a bit-wise OR of this value and the supplied bytes.
   *
   * @param bytes the bytes to perform the operation with
   * @return the result of a bit-wise OR
   */
  @Override
  public UInt256 or(Bytes32 bytes) {
    int[] result = new int[INTS_SIZE];
    for (int i = INTS_SIZE - 1, j = 28; i >= 0; --i, j -= 4) {
      result[i] = this.ints[i] | (((int) bytes.get(j) & 0xFF) << 24);
      result[i] |= ((int) bytes.get(j + 1) & 0xFF) << 16;
      result[i] |= ((int) bytes.get(j + 2) & 0xFF) << 8;
      result[i] |= ((int) bytes.get(j + 3) & 0xFF);
    }
    return new UInt256(result);
  }

  /**
   * Return a bit-wise XOR of this value and the supplied value.
   *
   * @param value the value to perform the operation with
   * @return the result of a bit-wise XOR
   */
  public UInt256 xor(UInt256 value) {
    int[] result = new int[INTS_SIZE];
    for (int i = INTS_SIZE - 1; i >= 0; --i) {
      result[i] = this.ints[i] ^ value.ints[i];
    }
    return new UInt256(result);
  }

  /**
   * Return a bit-wise XOR of this value and the supplied bytes.
   *
   * @param bytes the bytes to perform the operation with
   * @return the result of a bit-wise XOR
   */
  @Override
  public UInt256 xor(Bytes32 bytes) {
    int[] result = new int[INTS_SIZE];
    for (int i = INTS_SIZE - 1, j = 28; i >= 0; --i, j -= 4) {
      result[i] = this.ints[i] ^ (((int) bytes.get(j) & 0xFF) << 24);
      result[i] ^= ((int) bytes.get(j + 1) & 0xFF) << 16;
      result[i] ^= ((int) bytes.get(j + 2) & 0xFF) << 8;
      result[i] ^= ((int) bytes.get(j + 3) & 0xFF);
    }
    return new UInt256(result);
  }

  /**
   * Return a bit-wise NOT of this value.
   *
   * @return the result of a bit-wise NOT
   */
  @Override
  public UInt256 not() {
    int[] result = new int[INTS_SIZE];
    for (int i = INTS_SIZE - 1; i >= 0; --i) {
      result[i] = ~(this.ints[i]);
    }
    return new UInt256(result);
  }

  /**
   * Shift all bits in this value to the right.
   *
   * @param distance The number of bits to shift by.
   * @return A value containing the shifted bits.
   */
  @Override
  public UInt256 shiftRight(int distance) {
    if (distance == 0) {
      return this;
    }
    if (distance >= 256) {
      return ZERO;
    }
    int[] result = new int[INTS_SIZE];
    int d = distance / 32;
    int s = distance % 32;

    int resIdx = INTS_SIZE;
    if (s == 0) {
      for (int i = INTS_SIZE - d; i > 0;) {
        result[--resIdx] = this.ints[--i];
      }
    } else {
      for (int i = INTS_SIZE - 1 - d; i >= 0; i--) {
        int leftSide = this.ints[i] >>> s;
        int rightSide = (i == 0) ? 0 : this.ints[i - 1] << (32 - s);
        result[--resIdx] = (leftSide | rightSide);
      }
    }
    return new UInt256(result);
  }

  /**
   * Shift all bits in this value to the left.
   *
   * @param distance The number of bits to shift by.
   * @return A value containing the shifted bits.
   */
  @Override
  public UInt256 shiftLeft(int distance) {
    if (distance == 0) {
      return this;
    }
    if (distance >= 256) {
      return ZERO;
    }
    int[] result = new int[INTS_SIZE];
    int d = distance / 32;
    int s = distance % 32;

    int resIdx = 0;
    if (s == 0) {
      for (int i = d; i < INTS_SIZE;) {
        result[resIdx++] = this.ints[i++];
      }
    } else {
      for (int i = d; i < INTS_SIZE; ++i) {
        int leftSide = this.ints[i] << s;
        int rightSide = (i == INTS_SIZE - 1) ? 0 : (this.ints[i + 1] >>> (32 - s));
        result[resIdx++] = (leftSide | rightSide);
      }
    }
    return new UInt256(result);
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object == this) {
      return true;
    }
    if (object instanceof UInt256) {
      UInt256 other = (UInt256) object;
      for (int i = 0; i < INTS_SIZE; ++i) {
        if (this.ints[i] != other.ints[i]) {
          return false;
        }
      }
      return true;
    }
    if (object instanceof Bytes) {
      Bytes other = (Bytes) object;
      if (this.size() != other.size()) {
        return false;
      }

      for (int i = 0; i < size(); i++) {
        if (this.get(i) != other.get(i)) {
          return false;
        }
      }

      return true;
    }
    return false;
  }

  int computeHashcode() {
    int result = 1;
    for (int i = 0; i < size(); i++) {
      result = 31 * result + get(i);
    }
    return result;
  }

  @Override
  public int hashCode() {
    if (this.hashCode == null) {
      this.hashCode = computeHashcode();
    }
    return this.hashCode;
  }

  @Override
  public boolean fitsInt() {
    for (int i = 0; i < INTS_SIZE - 1; i++) {
      if (this.ints[i] != 0) {
        return false;
      }
    }
    // Lastly, the left-most byte of the int must not start with a 1.
    return this.ints[INTS_SIZE - 1] >= 0;
  }

  @Override
  public int intValue() {
    if (!fitsInt()) {
      throw new ArithmeticException("Value does not fit a 4 byte int");
    }
    return this.ints[INTS_SIZE - 1];
  }

  @Override
  public boolean fitsLong() {
    for (int i = 0; i < INTS_SIZE - 2; i++) {
      if (this.ints[i] != 0) {
        return false;
      }
    }
    // Lastly, the left-most byte of the int must not start with a 1.
    return this.ints[INTS_SIZE - 2] >= 0;
  }

  @Override
  public byte get(int i) {
    int whichInt = i / 4;
    int whichIndex = 3 - i % 4;
    return (byte) ((this.ints[whichInt] >> (8 * whichIndex)) & 0xFF);
  }

  @Override
  public Bytes32 copy() {
    return toBytes();
  }

  @Override
  public long toLong() {
    if (!fitsLong()) {
      throw new ArithmeticException("Value does not fit a 8 byte long");
    }
    return (((long) this.ints[INTS_SIZE - 2]) << 32) | (((long) (this.ints[INTS_SIZE - 1])) & LONG_MASK);
  }

  @Override
  public String toString() {
    return toHexString();
  }

  @Override
  public UInt256 toUInt256() {
    return this;
  }

  private byte[] toByteArray() {
    return new byte[] {
        (byte) (ints[0] >> 24),
        (byte) (ints[0] >> 16),
        (byte) (ints[0] >> 8),
        (byte) (ints[0]),
        (byte) (ints[1] >> 24),
        (byte) (ints[1] >> 16),
        (byte) (ints[1] >> 8),
        (byte) (ints[1]),
        (byte) (ints[2] >> 24),
        (byte) (ints[2] >> 16),
        (byte) (ints[2] >> 8),
        (byte) (ints[2]),
        (byte) (ints[3] >> 24),
        (byte) (ints[3] >> 16),
        (byte) (ints[3] >> 8),
        (byte) (ints[3]),
        (byte) (ints[4] >> 24),
        (byte) (ints[4] >> 16),
        (byte) (ints[4] >> 8),
        (byte) (ints[4]),
        (byte) (ints[5] >> 24),
        (byte) (ints[5] >> 16),
        (byte) (ints[5] >> 8),
        (byte) (ints[5]),
        (byte) (ints[6] >> 24),
        (byte) (ints[6] >> 16),
        (byte) (ints[6] >> 8),
        (byte) (ints[6]),
        (byte) (ints[7] >> 24),
        (byte) (ints[7] >> 16),
        (byte) (ints[7] >> 8),
        (byte) (ints[7])};
  }

  @Override
  public Bytes slice(int i, int length) {
    return toBytes().slice(i, length);
  }

  @Override
  public MutableBytes32 mutableCopy() {
    return MutableBytes32.wrap(toByteArray());
  }

  @Override
  public Bytes32 toBytes() {
    return Bytes32.wrap(toByteArray());
  }

  @Override
  public Bytes toMinimalBytes() {
    int i = 0;
    while (i < INTS_SIZE && this.ints[i] == 0) {
      ++i;
    }
    if (i == INTS_SIZE) {
      return Bytes.EMPTY;
    }
    int firstIntBytes = 4 - (Integer.numberOfLeadingZeros(this.ints[i]) / 8);
    int totalBytes = firstIntBytes + ((INTS_SIZE - (i + 1)) * 4);
    MutableBytes bytes = MutableBytes.create(totalBytes);
    int j = 0;
    switch (firstIntBytes) {
      case 4:
        bytes.set(j++, (byte) (this.ints[i] >>> 24));
        // fall through
      case 3:
        bytes.set(j++, (byte) ((this.ints[i] >>> 16) & 0xFF));
        // fall through
      case 2:
        bytes.set(j++, (byte) ((this.ints[i] >>> 8) & 0xFF));
        // fall through
      case 1:
        bytes.set(j++, (byte) (this.ints[i] & 0xFF));
    }
    ++i;
    for (; i < INTS_SIZE; ++i, j += 4) {
      bytes.setInt(j, this.ints[i]);
    }
    return bytes;
  }

  @Override
  public int numberOfLeadingZeros() {
    for (int i = 0; i < INTS_SIZE; i++) {
      if (this.ints[i] == 0) {
        continue;
      }
      return (i * 32) + Integer.numberOfLeadingZeros(this.ints[i]);
    }
    return 256;
  }

  @Override
  public int bitLength() {
    for (int i = 0; i < INTS_SIZE; i++) {
      if (this.ints[i] == 0) {
        continue;
      }
      return (INTS_SIZE * 32) - (i * 32) - Integer.numberOfLeadingZeros(this.ints[i]);
    }
    return 0;
  }

  @Override
  public UInt256 max() {
    return UInt256.MAX_VALUE;
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
