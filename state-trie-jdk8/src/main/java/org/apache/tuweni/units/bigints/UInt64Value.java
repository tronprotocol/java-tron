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

/**
 * Represents a 64-bit (8 bytes) unsigned integer value.
 *
 * <p>
 * A {@link UInt64Value} is an unsigned integer value whose value can range between 0 and 2^64-1.
 *
 * <p>
 * This interface defines operations for value types with a 64-bit precision range. The methods provided by this
 * interface take parameters of the same type (and also {@code long}. This provides type safety by ensuring calculations
 * cannot mix different {@code UInt64Value} types.
 *
 * <p>
 * Where only a pure numerical 64-bit value is required, {@link UInt64} should be used.
 *
 * <p>
 * It is strongly advised to extend {@link BaseUInt64Value} rather than implementing this interface directly. Doing so
 * provides type safety in that quantities of different units cannot be mixed accidentally.
 *
 * @param <T> The concrete type of the value.
 */
public interface UInt64Value<T extends UInt64Value<T>> extends Comparable<T> {

  /**
   * Returns true if this is 0.
   *
   * @return True if this is the value 0.
   */
  default boolean isZero() {
    return toBytes().isZero();
  }

  /**
   * Returns a value that is {@code (this + value)}.
   *
   * @param value The amount to be added to this value.
   * @return {@code this + value}
   */
  T add(T value);

  /**
   * Returns a value that is {@code (this + value)}.
   *
   * @param value the amount to be added to this value
   * @return {@code this + value}
   * @throws ArithmeticException if the result of the addition overflows
   */
  default T addExact(T value) {
    T result = add(value);
    if (compareTo(result) > 0) {
      throw new ArithmeticException("UInt64 overflow");
    }
    return result;
  }

  /**
   * Returns a value that is {@code (this + value)}.
   *
   * @param value The amount to be added to this value.
   * @return {@code this + value}
   */
  T add(long value);

  /**
   * Returns a value that is {@code (this + value)}.
   *
   * @param value the amount to be added to this value
   * @return {@code this + value}
   * @throws ArithmeticException if the result of the addition overflows
   */
  default T addExact(long value) {
    T result = add(value);
    if ((value > 0 && compareTo(result) > 0) || (value < 0 && compareTo(result) < 0)) {
      throw new ArithmeticException("UInt64 overflow");
    }
    return result;
  }

  /**
   * Returns a value equivalent to {@code ((this + value) mod modulus)}.
   *
   * @param value The amount to be added to this value.
   * @param modulus The modulus.
   * @return {@code (this + value) mod modulus}
   * @throws ArithmeticException {@code modulus} == 0.
   */
  T addMod(T value, UInt64 modulus);

  /**
   * Returns a value equivalent to {@code ((this + value) mod modulus)}.
   *
   * @param value The amount to be added to this value.
   * @param modulus The modulus.
   * @return {@code (this + value) mod modulus}
   * @throws ArithmeticException {@code modulus} == 0.
   */
  T addMod(long value, UInt64 modulus);

  /**
   * Returns a value equivalent to {@code ((this + value) mod modulus)}.
   *
   * @param value The amount to be added to this value.
   * @param modulus The modulus.
   * @return {@code (this + value) mod modulus}
   * @throws ArithmeticException {@code modulus} &le; 0.
   */
  T addMod(long value, long modulus);

  /**
   * Returns a value that is {@code (this - value)}.
   *
   * @param value The amount to be subtracted from this value.
   * @return {@code this - value}
   */
  T subtract(T value);

  /**
   * Returns a value that is {@code (this - value)}.
   *
   * @param value the amount to be subtracted to this value
   * @return {@code this - value}
   * @throws ArithmeticException if the result of the subtraction overflows
   */
  default T subtractExact(T value) {
    T result = subtract(value);
    if (compareTo(result) < 0) {
      throw new ArithmeticException("UInt64 overflow");
    }
    return result;
  }

  /**
   * Returns a value that is {@code (this - value)}.
   *
   * @param value The amount to be subtracted from this value.
   * @return {@code this - value}
   */
  T subtract(long value);

  /**
   * Returns a value that is {@code (this - value)}.
   *
   * @param value the amount to be subtracted to this value
   * @return {@code this - value}
   * @throws ArithmeticException if the result of the subtraction overflows
   */
  default T subtractExact(long value) {
    T result = subtract(value);
    if ((value > 0 && compareTo(result) < 0) || (value < 0 && compareTo(result) > 0)) {
      throw new ArithmeticException("UInt64 overflow");
    }
    return result;
  }

  /**
   * Returns a value that is {@code (this * value)}.
   *
   * @param value The amount to multiply this value by.
   * @return {@code this * value}
   */
  T multiply(T value);

  /**
   * Returns a value that is {@code (this * value)}.
   *
   * @param value The amount to multiply this value by.
   * @return {@code this * value}
   * @throws ArithmeticException {@code value} &lt; 0.
   */
  T multiply(long value);

  /**
   * Returns a value that is {@code ((this * value) mod modulus)}.
   *
   * @param value The amount to multiply this value by.
   * @param modulus The modulus.
   * @return {@code (this * value) mod modulus}
   * @throws ArithmeticException {@code value} &lt; 0 or {@code modulus} == 0.
   */
  T multiplyMod(T value, UInt64 modulus);

  /**
   * Returns a value that is {@code ((this * value) mod modulus)}.
   *
   * @param value The amount to multiply this value by.
   * @param modulus The modulus.
   * @return {@code (this * value) mod modulus}
   * @throws ArithmeticException {@code value} &lt; 0 or {@code modulus} == 0.
   */
  T multiplyMod(long value, UInt64 modulus);

  /**
   * Returns a value that is {@code ((this * value) mod modulus)}.
   *
   * @param value The amount to multiply this value by.
   * @param modulus The modulus.
   * @return {@code (this * value) mod modulus}
   * @throws ArithmeticException {@code value} &lt; 0 or {@code modulus} &le; 0.
   */
  T multiplyMod(long value, long modulus);

  /**
   * Returns a value that is {@code (this / value)}.
   *
   * @param value The amount to divide this value by.
   * @return {@code this / value}
   * @throws ArithmeticException {@code value} == 0.
   */
  T divide(T value);

  /**
   * Returns a value that is {@code (this / value)}.
   *
   * @param value The amount to divide this value by.
   * @return {@code this / value}
   * @throws ArithmeticException {@code value} &le; 0.
   */
  T divide(long value);

  /**
   * Returns a value that is {@code (this<sup>exponent</sup> mod 2<sup>64</sup>)}
   *
   * <p>
   * This calculates an exponentiation over the modulus of {@code 2^64}.
   *
   * <p>
   * Note that {@code exponent} is an {@link UInt64} rather than of the type {@code T}.
   *
   * @param exponent The exponent to which this value is to be raised.
   * @return {@code this<sup>exponent</sup> mod 2<sup>64</sup>}
   */
  T pow(UInt64 exponent);

  /**
   * Returns a value that is {@code (this<sup>exponent</sup> mod 2<sup>64</sup>)}
   *
   * <p>
   * This calculates an exponentiation over the modulus of {@code 2^64}.
   *
   * @param exponent The exponent to which this value is to be raised.
   * @return {@code this<sup>exponent</sup> mod 2<sup>64</sup>}
   */
  T pow(long exponent);

  /**
   * Returns a value that is {@code (this mod modulus)}.
   *
   * @param modulus The modulus.
   * @return {@code this mod modulus}.
   * @throws ArithmeticException {@code modulus} == 0.
   */
  T mod(UInt64 modulus);

  /**
   * Returns a value that is {@code (this mod modulus)}.
   *
   * @param modulus The modulus.
   * @return {@code this mod modulus}.
   * @throws ArithmeticException {@code modulus} &le; 0.
   */
  T mod(long modulus);

  /**
   * Returns true if this value fits an int.
   *
   * @return True if this value fits a java {@code int} (i.e. is less or equal to {@code Integer.MAX_VALUE}).
   */
  default boolean fitsInt() {
    // Ints are 4 bytes, so anything but the 4 last bytes must be zeroes
    Bytes bytes = toBytes();
    for (int i = 0; i < 8 - 4; i++) {
      if (bytes.get(i) != 0)
        return false;
    }
    // Lastly, the left-most byte of the int must not start with a 1.
    return bytes.get(4) >= 0;
  }

  /**
   * Returns this value as an int.
   *
   * @return This value as a java {@code int} assuming it is small enough to fit an {@code int}.
   * @throws ArithmeticException If the value does not fit an {@code int}, that is if {@code
   *     !fitsInt()}.
   */
  default int intValue() {
    if (!fitsInt()) {
      throw new ArithmeticException("Value does not fit a 4 byte int");
    }
    return toBytes().getInt(4);
  }

  /**
   * Returns true if this value fits a long.
   *
   * @return True if this value fits a java {@code long} (i.e. is less or equal to {@code Long.MAX_VALUE}).
   */
  default boolean fitsLong() {
    return true;
  }

  /**
   * Returns this value as a long.
   *
   * @return This value as a java {@code long} assuming it is small enough to fit a {@code long}.
   * @throws ArithmeticException If the value does not fit a {@code long}, that is if {@code
   *     !fitsLong()}.
   */
  default long toLong() {
    if (!fitsLong()) {
      throw new ArithmeticException("Value does not fit a 8 byte long");
    }
    return toBytes().getLong(0);
  }

  /**
   * Returns this value as a BigInteger
   * 
   * @return This value as a {@link BigInteger}.
   */
  default BigInteger toBigInteger() {
    return toBytes().toUnsignedBigInteger();
  }

  /**
   * This value represented as an hexadecimal string.
   *
   * <p>
   * Note that this representation includes all the 8 underlying bytes, no matter what the integer actually represents
   * (in other words, it can have many leading zeros). For a shorter representation that don't include leading zeros,
   * use {@link #toShortHexString}.
   *
   * @return This value represented as an hexadecimal string.
   */
  default String toHexString() {
    return toBytes().toHexString();
  }

  /**
   * Returns this value represented as a minimal hexadecimal string (without any leading zero)
   * 
   * @return This value represented as a minimal hexadecimal string (without any leading zero).
   */
  default String toShortHexString() {
    return toBytes().toShortHexString();
  }

  /**
   * Convert this value to a {@link UInt64}.
   *
   * @return This value as a {@link UInt64}.
   */
  UInt64 toUInt64();

  /**
   * Returns the value as bytes.
   * 
   * @return The value as bytes.
   */
  Bytes toBytes();

  /**
   * Returns the value as bytes without any leading zero bytes.
   * 
   * @return The value as bytes without any leading zero bytes.
   */
  Bytes toMinimalBytes();

  /**
   * Returns the number of zero bits preceding the highest-order ("leftmost") one-bit
   * 
   * @return the number of zero bits preceding the highest-order ("leftmost") one-bit in the binary representation of
   *         this value, or 64 if the value is equal to zero.
   */
  default int numberOfLeadingZeros() {
    return toBytes().numberOfLeadingZeros();
  }

  /**
   * Returns the number of bits following and including the highest-order ("leftmost") one-bit
   * 
   * @return The number of bits following and including the highest-order ("leftmost") one-bit in the binary
   *         representation of this value, or zero if all bits are zero.
   */
  default int bitLength() {
    return toBytes().bitLength();
  }

  /**
   * Returns the decimal representation of this value as a String.
   *
   * @return the decimal representation of this value as a String.
   */
  default String toDecimalString() {
    return toBigInteger().toString(10);
  }
}
