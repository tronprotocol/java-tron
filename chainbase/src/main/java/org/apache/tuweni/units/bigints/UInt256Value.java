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
import java.nio.ByteOrder;

import static java.nio.ByteOrder.BIG_ENDIAN;


/**
 * Represents a 256-bit (32 bytes) unsigned integer value.
 *
 * <p>
 * A {@link UInt256Value} is an unsigned integer value stored with 32 bytes, so whose value can range between 0 and
 * 2^256-1.
 *
 * <p>
 * This interface defines operations for value types with a 256-bit precision range. The methods provided by this
 * interface take parameters of the same type (and also {@code long}. This provides type safety by ensuring calculations
 * cannot mix different {@code UInt256Value} types.
 *
 * <p>
 * Where only a pure numerical 256-bit value is required, {@link UInt256} should be used.
 *
 * <p>
 * It is strongly advised to extend {@link BaseUInt256Value} rather than implementing this interface directly. Doing so
 * provides type safety in that quantities of different units cannot be mixed accidentally.
 *
 * @param <T> The concrete type of the value.
 */
public interface UInt256Value<T extends UInt256Value<T>> extends Bytes32 {

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
   * <p>
   * This notation can be used in Kotlin with the {@code +} operator.
   *
   * @param value The amount to be added to this value.
   * @return {@code this + value}
   */
  default T plus(T value) {
    return add(value);
  }

  /**
   * Returns a value that is {@code (this + value)}.
   *
   * <p>
   * This notation can be used in Kotlin with the {@code +} operator.
   *
   * @param value The amount to be added to this value.
   * @return {@code this + value}
   */
  default T plus(long value) {
    return add(value);
  }

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
      throw new ArithmeticException("UInt256 overflow");
    }
    return result;
  }

  /**
   * Returns a value that is {@code (this + value)}, or MAX_VALUE if it overflows.
   * 
   * @param value the amount to be added to this value
   * @return {@code this + value} or UInt256.MAX
   */
  default T addSafe(T value) {
    T result = add(value);
    if (compareTo(result) > 0) {
      return max();
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
      throw new ArithmeticException("UInt256 overflow");
    }
    return result;
  }

  /**
   * Returns a value that is {@code (this + value)}, or MAX_VALUE if it overflows.
   *
   * @param value the amount to be added to this value
   * @return {@code this + value} or UInt256.MAX
   */
  default T addSafe(long value) {
    T result = add(value);
    if ((value > 0 && compareTo(result) > 0) || (value < 0 && compareTo(result) < 0)) {
      return max();
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
  T addMod(T value, UInt256 modulus);

  /**
   * Returns a value equivalent to {@code ((this + value) mod modulus)}.
   *
   * @param value The amount to be added to this value.
   * @param modulus The modulus.
   * @return {@code (this + value) mod modulus}
   * @throws ArithmeticException {@code modulus} == 0.
   */
  T addMod(long value, UInt256 modulus);

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
   * @throws ArithmeticException if the result of the addition overflows
   */
  default T subtractExact(T value) {
    T result = subtract(value);
    if (compareTo(result) < 0) {
      throw new ArithmeticException("UInt256 overflow");
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
   * @throws ArithmeticException if the result of the addition overflows
   */
  default T subtractExact(long value) {
    T result = subtract(value);
    if ((value > 0 && compareTo(result) < 0) || (value < 0 && compareTo(result) > 0)) {
      throw new ArithmeticException("UInt256 overflow");
    }
    return result;
  }

  /**
   * Return the max value for this type.
   *
   * <p>
   * The default implementation of this method returns a value obtained from calling the concrete type constructor with
   * an argument of {@link UInt256#MAX_VALUE}. Most implementations will want to override this method to instead return
   * a static constant.
   *
   * @return The max value for this type.
   */
  public T max();

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
  T multiplyMod(T value, UInt256 modulus);

  /**
   * Returns a value that is {@code ((this * value) mod modulus)}.
   *
   * @param value The amount to multiply this value by.
   * @param modulus The modulus.
   * @return {@code (this * value) mod modulus}
   * @throws ArithmeticException {@code value} &lt; 0 or {@code modulus} == 0.
   */
  T multiplyMod(long value, UInt256 modulus);

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
   * Returns a value that is {@code ceiling(this / value)}.
   *
   * @param value The amount to divide this value by.
   * @return {@code this / value + ( this % value == 0 ? 0 : 1)}
   * @throws ArithmeticException {@code value} == 0.
   */
  T divideCeil(T value);

  /**
   * Returns a value that is {@code ceiling(this / value)}.
   *
   * @param value The amount to divide this value by.
   * @return {@code this / value + ( this % value == 0 ? 0 : 1)}
   * @throws ArithmeticException {@code value} == 0.
   */
  T divideCeil(long value);

  /**
   * Returns a value that is {@code (this<sup>exponent</sup> mod 2<sup>256</sup>)}
   *
   * <p>
   * This calculates an exponentiation over the modulus of {@code 2^256}.
   *
   * <p>
   * Note that {@code exponent} is an {@link UInt256} rather than of the type {@code T}.
   *
   * @param exponent The exponent to which this value is to be raised.
   * @return {@code this<sup>exponent</sup> mod 2<sup>256</sup>}
   */
  T pow(UInt256 exponent);

  /**
   * Returns a value that is {@code (this<sup>exponent</sup> mod 2<sup>256</sup>)}
   *
   * <p>
   * This calculates an exponentiation over the modulus of {@code 2^256}.
   *
   * @param exponent The exponent to which this value is to be raised.
   * @return {@code this<sup>exponent</sup> mod 2<sup>256</sup>}
   */
  T pow(long exponent);

  /**
   * Returns a value that is {@code (this mod modulus)}.
   *
   * @param modulus The modulus.
   * @return {@code this mod modulus}.
   * @throws ArithmeticException {@code modulus} == 0.
   */
  T mod(UInt256 modulus);

  /**
   * Returns a value that is {@code (this mod modulus)}.
   *
   * @param modulus The modulus.
   * @return {@code this mod modulus}.
   * @throws ArithmeticException {@code modulus} &le; 0.
   */
  T mod(long modulus);

  /**
   * Returns a value that is {@code (this mod modulus)}, or 0 if modulus is 0.
   *
   * @param modulus The modulus.
   * @return {@code this mod modulus}.
   */
  T mod0(UInt256 modulus);

  /**
   * Returns a value that is {@code (this mod modulus)}, or 0 if modulus is 0.
   *
   * @param modulus The modulus.
   * @return {@code this mod modulus}.
   */
  T mod0(long modulus);

  /**
   * Returns true if the value can fit in an int.
   *
   * @return True if this value fits a java {@code int} (i.e. is less or equal to {@code Integer.MAX_VALUE}).
   */
  default boolean fitsInt() {
    return fitsInt(ByteOrder.BIG_ENDIAN);
  }

  /**
   * Returns true if the value can fit in an int according to the byte order.
   *
   * @param order the byte order, little or big endian
   * @return True if this value fits a java {@code int} (i.e. is less or equal to {@code Integer.MAX_VALUE}).
   */
  default boolean fitsInt(ByteOrder order) {
    if (order == ByteOrder.BIG_ENDIAN) {
      // Ints are 4 bytes, so anything but the 4 last bytes must be zeroes
      for (int i = 0; i < Bytes32.SIZE - 4; i++) {
        if (get(i) != 0)
          return false;
      }
      // Lastly, the left-most byte of the int must not start with a 1.
      return get(Bytes32.SIZE - 4) >= 0;
    } else {
      // Ints are 4 bytes, so only the 4 first bytes must not be zeroes
      for (int i = 4; i < Bytes32.SIZE - 4; i++) {
        if (get(i) != 0)
          return false;
      }
      // Lastly, the right-most byte of the int must not start with a 1.
      return get(3) >= 0;
    }
  }

  /**
   * Provides this value as an int.
   *
   * @return This value as a java {@code int} assuming it is small enough to fit an {@code int}.
   * @throws ArithmeticException If the value does not fit an {@code int}, that is if {@code !fitsInt()}.
   */
  default int intValue() {
    if (!fitsInt()) {
      throw new ArithmeticException("Value does not fit a 4 byte int");
    }
    return getInt(Bytes32.SIZE - 4);
  }

  @Override
  default int toInt(ByteOrder order) {
    if (!fitsInt(order)) {
      throw new ArithmeticException("Value does not fit a 4 byte int");
    }
    if (order == ByteOrder.BIG_ENDIAN) {
      return getInt(Bytes32.SIZE - 4, order);
    } else {
      return getInt(0, order);
    }
  }

  @Override
  default long toLong(ByteOrder order) {
    if (!fitsLong(order)) {
      throw new ArithmeticException("Value does not fit a 8 byte long");
    }
    if (order == ByteOrder.BIG_ENDIAN) {
      return getLong(Bytes32.SIZE - 8, order);
    } else {
      return getLong(0, order);
    }
  }

  /**
   * Returns true if the value can fit in a long.
   *
   * @return True if this value fits a java {@code long} (i.e. is less or equal to {@code Long.MAX_VALUE}).
   */
  default boolean fitsLong() {
    return fitsLong(ByteOrder.BIG_ENDIAN);
  }

  /**
   * Returns true if the value can fit in a long.
   *
   * @param order byte order, little or big endian
   * @return True if this value fits a java {@code long} (i.e. is less or equal to {@code Long.MAX_VALUE}).
   */
  default boolean fitsLong(ByteOrder order) {
    if (order == ByteOrder.BIG_ENDIAN) {
      // Longs are 8 bytes, so anything but the 8 last bytes must be zeroes
      for (int i = 0; i < Bytes32.SIZE - 8; i++) {
        if (get(i) != 0)
          return false;
      }
      // Lastly, the left-most byte of the long must not start with a 1.
      return get(Bytes32.SIZE - 8) >= 0;
    } else {
      // Longs are 8 bytes, so only the 8 first bytes may not be zeroes
      for (int i = 8; i < Bytes32.SIZE; i++) {
        if (get(i) != 0)
          return false;
      }
      // Lastly, the left-most byte of the long must not start with a 1.
      return get(7) >= 0;
    }
  }

  /**
   * Convert this value to a {@link UInt256}.
   *
   * @return This value as a {@link UInt256}.
   */
  UInt256 toUInt256();

  /**
   * Provides the value as bytes.
   * 
   * @return The value as bytes.
   */
  Bytes32 toBytes();

  /**
   * Provides the value as bytes without any leading zero bytes.
   * 
   * @return The value as bytes without any leading zero bytes.
   */
  Bytes toMinimalBytes();

  /**
   * Returns true if this value is greater than the other one
   * 
   * @param other the other value being compared
   * @return true if this value is greater than the other one, false otherwise
   */
  default boolean greaterThan(UInt256Value<T> other) {
    return compareTo(other) > 0;
  }

  /**
   * Returns true if this value is greater or equal than the other one
   * 
   * @param other the other value being compared
   * @return true if this value is greater or equal than the other one, false otherwise
   */
  default boolean greaterOrEqualThan(UInt256Value<T> other) {
    return compareTo(other) >= 0;
  }

  /**
   * Returns true if this value is less than the other one
   * 
   * @param other the other value being compared
   * @return true if this value is less than the other one, false otherwise
   */
  default boolean lessThan(UInt256Value<T> other) {
    return compareTo(other) < 0;
  }

  /**
   * Returns true if this value is less or equal than the other one
   * 
   * @param other the other value being compared
   * @return true if this value is less or equal than the other one, false otherwise
   */
  default boolean lessOrEqualThan(UInt256Value<T> other) {
    return compareTo(other) <= 0;
  }

  /**
   * Returns the decimal representation of this value as a String.
   *
   * @return the decimal representation of this value as a String.
   */
  default String toDecimalString() {
    return toBigInteger().toString(10);
  }

  /**
   * The BigInteger corresponding to interpreting these bytes as a two's-complement signed integer.
   *
   * @return A {@link BigInteger} corresponding to interpreting these bytes as a two's-complement signed integer.
   */
  default BigInteger toSignedBigInteger() {
    return toSignedBigInteger(BIG_ENDIAN);
  }

  /**
   * The BigInteger corresponding to interpreting these bytes as a two's-complement signed integer.
   *
   * @param order The byte-order for decoding the integer.
   * @return A {@link BigInteger} corresponding to interpreting these bytes as a two's-complement signed integer.
   */
  default BigInteger toSignedBigInteger(ByteOrder order) {
    if (size() == 0) {
      return BigInteger.ZERO;
    }
    return new BigInteger((order == BIG_ENDIAN) ? toArrayUnsafe() : reverse().toArrayUnsafe());
  }

  /**
   * The BigInteger corresponding to interpreting these bytes as an unsigned integer.
   *
   * @return A positive (or zero) {@link BigInteger} corresponding to interpreting these bytes as an unsigned integer.
   */
  @Override
  default BigInteger toBigInteger() {
    return toUnsignedBigInteger();
  }

  /**
   * The BigInteger corresponding to interpreting these bytes as an unsigned integer.
   *
   * @param order The byte-order for decoding the integer.
   * @return A positive (or zero) {@link BigInteger} corresponding to interpreting these bytes as an unsigned integer.
   */
  @Override
  default BigInteger toBigInteger(ByteOrder order) {
    return toUnsignedBigInteger(order);
  }
}
