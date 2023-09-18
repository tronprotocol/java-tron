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
 * Represents a 32-bit (8 bytes) unsigned integer value.
 *
 * <p>
 * A {@link UInt32Value} is an unsigned integer value whose value can range between 0 and 2^32-1.
 *
 * <p>
 * This interface defines operations for value types with a 32-bit precision range. The methods provided by this
 * interface take parameters of the same type (and also {@code long}. This provides type safety by ensuring calculations
 * cannot mix different {@code UInt32Value} types.
 *
 * <p>
 * Where only a pure numerical 32-bit value is required, {@link UInt32} should be used.
 *
 * <p>
 * It is strongly advised to extend {@link BaseUInt32Value} rather than implementing this interface directly. Doing so
 * provides type safety in that quantities of different units cannot be mixed accidentally.
 *
 * @param <T> The concrete type of the value.
 */
public interface UInt32Value<T extends UInt32Value<T>> extends Comparable<T> {

  /**
   * Returns true is this is 0.
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
      throw new ArithmeticException("UInt32 overflow");
    }
    return result;
  }

  /**
   * Returns a value that is {@code (this + value)}.
   *
   * @param value The amount to be added to this value.
   * @return {@code this + value}
   */
  T add(int value);

  /**
   * Returns a value that is {@code (this + value)}.
   *
   * @param value the amount to be added to this value
   * @return {@code this + value}
   * @throws ArithmeticException if the result of the addition overflows
   */
  default T addExact(int value) {
    T result = add(value);
    if ((value > 0 && compareTo(result) > 0) || (value < 0 && compareTo(result) < 0)) {
      throw new ArithmeticException("UInt32 overflow");
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
  T addMod(T value, UInt32 modulus);

  /**
   * Returns a value equivalent to {@code ((this + value) mod modulus)}.
   *
   * @param value The amount to be added to this value.
   * @param modulus The modulus.
   * @return {@code (this + value) mod modulus}
   * @throws ArithmeticException {@code modulus} == 0.
   */
  T addMod(long value, UInt32 modulus);

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
      throw new ArithmeticException("UInt32 overflow");
    }
    return result;
  }

  /**
   * Returns a value that is {@code (this - value)}.
   *
   * @param value The amount to be subtracted from this value.
   * @return {@code this - value}
   */
  T subtract(int value);

  /**
   * Returns a value that is {@code (this - value)}.
   *
   * @param value the amount to be subtracted to this value
   * @return {@code this - value}
   * @throws ArithmeticException if the result of the subtraction overflows
   */
  default T subtractExact(int value) {
    T result = subtract(value);
    if ((value > 0 && compareTo(result) < 0) || (value < 0 && compareTo(result) > 0)) {
      throw new ArithmeticException("UInt32 overflow");
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
  T multiply(int value);

  /**
   * Returns a value that is {@code ((this * value) mod modulus)}.
   *
   * @param value The amount to multiply this value by.
   * @param modulus The modulus.
   * @return {@code (this * value) mod modulus}
   * @throws ArithmeticException {@code value} &lt; 0 or {@code modulus} == 0.
   */
  T multiplyMod(T value, UInt32 modulus);

  /**
   * Returns a value that is {@code ((this * value) mod modulus)}.
   *
   * @param value The amount to multiply this value by.
   * @param modulus The modulus.
   * @return {@code (this * value) mod modulus}
   * @throws ArithmeticException {@code value} &lt; 0 or {@code modulus} == 0.
   */
  T multiplyMod(int value, UInt32 modulus);

  /**
   * Returns a value that is {@code ((this * value) mod modulus)}.
   *
   * @param value The amount to multiply this value by.
   * @param modulus The modulus.
   * @return {@code (this * value) mod modulus}
   * @throws ArithmeticException {@code value} &lt; 0 or {@code modulus} &le; 0.
   */
  T multiplyMod(int value, int modulus);

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
  T divide(int value);

  /**
   * Returns a value that is {@code (this<sup>exponent</sup> mod 2<sup>32</sup>)}
   *
   * <p>
   * This calculates an exponentiation over the modulus of {@code 2^32}.
   *
   * <p>
   * Note that {@code exponent} is an {@link UInt32} rather than of the type {@code T}.
   *
   * @param exponent The exponent to which this value is to be raised.
   * @return {@code this<sup>exponent</sup> mod 2<sup>32</sup>}
   */
  T pow(UInt32 exponent);

  /**
   * Returns a value that is {@code (this<sup>exponent</sup> mod 2<sup>32</sup>)}
   *
   * <p>
   * This calculates an exponentiation over the modulus of {@code 2^32}.
   *
   * @param exponent The exponent to which this value is to be raised.
   * @return {@code this<sup>exponent</sup> mod 2<sup>32</sup>}
   */
  T pow(long exponent);

  /**
   * Returns a value that is {@code (this mod modulus)}.
   *
   * @param modulus The modulus.
   * @return {@code this mod modulus}.
   * @throws ArithmeticException {@code modulus} == 0.
   */
  T mod(UInt32 modulus);

  /**
   * Returns a value that is {@code (this mod modulus)}.
   *
   * @param modulus The modulus.
   * @return {@code this mod modulus}.
   * @throws ArithmeticException {@code modulus} &le; 0.
   */
  T mod(int modulus);

  /**
   * Returns true if this value fits an int.
   *
   * @return True if this value fits a java {@code int} (i.e. is less or equal to {@code Integer.MAX_VALUE}).
   */
  default boolean fitsInt() {
    return !UInt32.MAX_VALUE.equals(this);
  }

  /**
   * Returns this value as an int.
   * 
   * @return This value as a java {@code int} assuming it is small enough to fit an {@code int}.
   * @throws ArithmeticException If the value does not fit an {@code int}, that is if {@code
   *     !fitsInt()}.
   */
  default int intValue() {
    return toBigInteger().intValueExact();
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
    return toBigInteger().longValueExact();
  }

  /**
   * Provides this value as a BigInteger.
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
   * Returns this value represented as a minimal hexadecimal string (without any leading zero).
   * 
   * @return This value represented as a minimal hexadecimal string (without any leading zero).
   */
  default String toShortHexString() {
    return toBytes().toShortHexString();
  }

  /**
   * Convert this value to a {@link UInt32}.
   *
   * @return This value as a {@link UInt32}.
   */
  UInt32 toUInt32();

  /**
   * Provides the value as bytes.
   * 
   * @return The value as bytes.
   */
  Bytes toBytes();

  /**
   * Provides the value as bytes without any leading zero bytes
   * 
   * @return The value as bytes without any leading zero bytes.
   */
  Bytes toMinimalBytes();

  /**
   * Provides the number of zero bits preceding the highest-order ("leftmost") one-bit
   * 
   * @return the number of zero bits preceding the highest-order ("leftmost") one-bit in the binary representation of
   *         this value, or 32 if the value is equal to zero.
   */
  default int numberOfLeadingZeros() {
    return toBytes().numberOfLeadingZeros();
  }

  /**
   * Provides the number of bits following and including the highest-order ("leftmost") one-bit
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
