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
import org.apache.tuweni.bytes.Bytes48;

import java.math.BigInteger;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Base class for {@link UInt384Value}.
 *
 * <p>
 * This class is abstract as it is not meant to be used directly, but it has no abstract methods. As mentioned in
 * {@link UInt384Value}, this is used to create strongly-typed type aliases of {@link UInt384}. In other words, this
 * allow to "tag" numbers with the unit of what they represent for the type-system, which can help clarity, but also
 * forbid mixing numbers that are mean to be of different units (the strongly-typed part).
 *
 * <p>
 * This class implements {@link UInt384Value}, but also adds a few operations that take a {@link UInt384} directly, for
 * instance {@link #multiply(UInt384)}. The rational is that multiplying a given quantity of something by a "raw" number
 * is always meaningful, and return a new quantity of the same thing.
 *
 * @param <T> The concrete type of the value.
 */
public abstract class BaseUInt384Value<T extends UInt384Value<T>> implements UInt384Value<T> {

  private final UInt384 value;
  private final Function<UInt384, T> ctor;

  /**
   * @param value The value to instantiate this {@code UInt384Value} with.
   * @param ctor A constructor for the concrete type.
   */
  protected BaseUInt384Value(UInt384 value, Function<UInt384, T> ctor) {
    requireNonNull(value);
    requireNonNull(ctor);
    this.value = value;
    this.ctor = ctor;
  }

  /**
   * @param value An unsigned value to instantiate this {@code UInt384Value} with.
   * @param ctor A constructor for the concrete type.
   */
  protected BaseUInt384Value(long value, Function<UInt384, T> ctor) {
    requireNonNull(ctor);
    this.value = UInt384.valueOf(value);
    this.ctor = ctor;
  }

  /**
   * @param value An unsigned value to instantiate this {@code UInt384Value} with.
   * @param ctor A constructor for the concrete type.
   */
  protected BaseUInt384Value(BigInteger value, Function<UInt384, T> ctor) {
    requireNonNull(value);
    requireNonNull(ctor);
    this.value = UInt384.valueOf(value);
    this.ctor = ctor;
  }

  /**
   * Return a copy of this value, or itself if immutable.
   *
   * <p>
   * The default implementation of this method returns a copy using the constructor for the concrete type and the bytes
   * returned from {@link #toBytes()}. Most implementations will want to override this method to instead return
   * {@code this}.
   *
   * @return A copy of this value, or itself if immutable.
   */
  protected T copy() {
    return ctor.apply(value);
  }

  /**
   * Return the zero value for this type.
   *
   * <p>
   * The default implementation of this method returns a value obtained from calling the concrete type constructor with
   * an argument of {@link Bytes48#ZERO}. Most implementations will want to override this method to instead return a
   * static constant.
   *
   * @return The zero value for this type.
   */
  protected T zero() {
    return ctor.apply(UInt384.ZERO);
  }

  @Override
  public T add(T value) {
    return add(value.toUInt384());
  }

  /**
   * Returns a value that is {@code (this + value)}.
   *
   * @param value The amount to be added to this value.
   * @return {@code this + value}
   */
  public T add(UInt384 value) {
    if (value.isZero()) {
      return copy();
    }
    return ctor.apply(this.value.add(value));
  }

  @Override
  public T add(long value) {
    if (value == 0) {
      return copy();
    }
    return ctor.apply(this.value.add(value));
  }

  @Override
  public T addMod(T value, UInt384 modulus) {
    return addMod(value.toUInt384(), modulus);
  }

  /**
   * Returns a value equivalent to {@code ((this + value) mod modulus)}.
   *
   * @param value The amount to be added to this value.
   * @param modulus The modulus.
   * @return {@code (this + value) mod modulus}
   * @throws ArithmeticException {@code modulus} == 0.
   */
  public T addMod(UInt384 value, UInt384 modulus) {
    return ctor.apply(this.value.addMod(value, modulus));
  }

  @Override
  public T addMod(long value, UInt384 modulus) {
    return ctor.apply(this.value.addMod(value, modulus));
  }

  @Override
  public T addMod(long value, long modulus) {
    return ctor.apply(this.value.addMod(value, modulus));
  }

  @Override
  public T subtract(T value) {
    return subtract(value.toUInt384());
  }

  /**
   * Returns a value that is {@code (this - value)}.
   *
   * @param value The amount to be subtracted from this value.
   * @return {@code this - value}
   */
  public T subtract(UInt384 value) {
    if (value.isZero()) {
      return copy();
    }
    return ctor.apply(this.value.subtract(value));
  }

  @Override
  public T subtract(long value) {
    if (value == 0) {
      return copy();
    }
    return ctor.apply(this.value.subtract(value));
  }

  @Override
  public T multiply(T value) {
    return multiply(value.toUInt384());
  }

  /**
   * Returns a value that is {@code (this * value)}.
   *
   * @param value The amount to multiply this value by.
   * @return {@code this * value}
   */
  public T multiply(UInt384 value) {
    if (isZero() || value.isZero()) {
      return zero();
    }
    if (value.equals(UInt384.ONE)) {
      return copy();
    }
    return ctor.apply(this.value.multiply(value));
  }

  @Override
  public T multiply(long value) {
    if (value == 0 || isZero()) {
      return zero();
    }
    if (value == 1) {
      return copy();
    }
    return ctor.apply(this.value.multiply(value));
  }

  @Override
  public T multiplyMod(T value, UInt384 modulus) {
    return multiplyMod(value.toUInt384(), modulus);
  }

  /**
   * Returns a value that is {@code ((this * value) mod modulus)}.
   *
   * @param value The amount to multiply this value by.
   * @param modulus The modulus.
   * @return {@code (this * value) mod modulus}
   * @throws ArithmeticException {@code value} &lt; 0 or {@code modulus} == 0.
   */
  public T multiplyMod(UInt384 value, UInt384 modulus) {
    return ctor.apply(this.value.multiplyMod(value, modulus));
  }

  @Override
  public T multiplyMod(long value, UInt384 modulus) {
    return ctor.apply(this.value.multiplyMod(value, modulus));
  }

  @Override
  public T multiplyMod(long value, long modulus) {
    return ctor.apply(this.value.multiplyMod(value, modulus));
  }

  @Override
  public T divide(T value) {
    return divide(value.toUInt384());
  }

  /**
   * Returns a value that is {@code (this / value)}.
   *
   * @param value The amount to divide this value by.
   * @return {@code this / value}
   * @throws ArithmeticException {@code value} == 0.
   */
  public T divide(UInt384 value) {
    return ctor.apply(this.value.divide(value));
  }

  @Override
  public T divide(long value) {
    return ctor.apply(this.value.divide(value));
  }

  @Override
  public T pow(UInt384 exponent) {
    return ctor.apply(this.value.pow(exponent));
  }

  @Override
  public T pow(long exponent) {
    return ctor.apply(this.value.pow(exponent));
  }

  @Override
  public T mod(UInt384 modulus) {
    return ctor.apply(this.value.mod(modulus));
  }

  @Override
  public T mod(long modulus) {
    return ctor.apply(this.value.mod(modulus));
  }

  @Override
  public int compareTo(T other) {
    return compareTo(other.toUInt384());
  }

  /**
   * Compare two {@link UInt384} values.
   *
   * @param other The value to compare to.
   * @return A negative integer, zero, or a positive integer as this value is less than, equal to, or greater than the
   *         specified value.
   */
  public int compareTo(UInt384 other) {
    return this.value.compareTo(other);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof UInt384Value)) {
      return false;
    }
    UInt384Value<?> other = (UInt384Value<?>) obj;
    return this.value.equals(other.toUInt384());
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public String toString() {
    return value.toString();
  }

  @Override
  public UInt384 toUInt384() {
    return value;
  }

  @Override
  public Bytes48 toBytes() {
    return value.toBytes();
  }

  @Override
  public Bytes toMinimalBytes() {
    return value.toMinimalBytes();
  }
}
