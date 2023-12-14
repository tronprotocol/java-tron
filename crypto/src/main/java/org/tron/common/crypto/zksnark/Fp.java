/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.common.crypto.zksnark;

import java.math.BigInteger;

/**
 * Arithmetic in F_p, p = 21888242871839275222246405745257275088696311157297823662689037894645226208583
 * This class stores elements of F_p in the Montgomery form: a*r mod p.
 * 
 * @author Mikhail Kalinin
 * @since 01.09.2017
 */
public class Fp implements Field<Fp> {
  /**
   * "p" field parameter of F_p, F_p2, F_p6 and F_p12
   */
  protected static final BigInteger P = new BigInteger(
      "21888242871839275222246405745257275088696311157297823662689037894645226208583");

  /**
   * This value is equal to 2^256. It should be greater than {@link #P} and coprime to it.
   * Field elements are represented in Montgomery form as a*{@link #REDUCER} mod {@link #P}. 
   * This specific value of {@link #REDUCER} is selected to facilitate efficient division 
   * by {@link #REDUCER} through simple shifting.
   * This field is not used in the code but can be helpful for understanding
   */
  @SuppressWarnings("unused")
  private static final BigInteger REDUCER = new BigInteger(
    "115792089237316195423570985008687907853269984665640564039457584007913129639936");

  /**
   * The number of bits in the {@link #REDUCER} value.
   */
  private static final int REDUCER_BITS = 256;

  /**
   * A precomputed value of {@link #REDUCER}^2 mod {@link #P}.
   */
  private static final BigInteger REDUCER_SQUARED = new BigInteger(
    "3096616502983703923843567936837374451735540968419076528771170197431451843209");

  /**
   * A precomputed value of {@link #REDUCER}^3 mod {@link #P}.
   */
  private static final BigInteger REDUCER_CUBED = new BigInteger(
    "14921786541159648185948152738563080959093619838510245177710943249661917737183");

  /**
   * A precomputed value of -{@link #P}^{-1} mod {@link #REDUCER}.
   */
  private static final BigInteger FACTOR = new BigInteger(
    "111032442853175714102588374283752698368366046808579839647964533820976443843465");

  /**
   * The MASK value is set to 2^256 - 1 and is utilized to replace the operation % 2^256
   * with a bitwise AND using this value. This choice ensures that only the lower 256 bits
   * of a result are retained, effectively simulating the modulus operation.
   */
  private static final BigInteger MASK = new BigInteger(
    "115792089237316195423570985008687907853269984665640564039457584007913129639935");

  protected static final Fp ZERO = Fp.create(BigInteger.ZERO);
  protected static final Fp _1 = Fp.create(BigInteger.ONE);
  protected static final Fp NON_RESIDUE = Fp.create(new BigInteger(
      "21888242871839275222246405745257275088696311157297823662689037894645226208582"));

  protected static final Fp _2_INV = Fp.create(BigInteger.valueOf(2).modInverse(P));

  BigInteger v;

  Fp(BigInteger v) {
    this.v = v;
  }

  static Fp create(byte[] v) {
    BigInteger value = new BigInteger(1, v);
    if (value.compareTo(P) >= 0) {
      // Only the values less than P are valid
      return null;
    }
    return new Fp(toMontgomery(value));
  }

  static Fp create(BigInteger v) {
    if (v.compareTo(P) >= 0) {
      // Only the values less than P are valid
      return null;
    }
    return new Fp(toMontgomery(v));
  }

  @Override
  public Fp add(Fp o) {
    BigInteger r = v.add(o.v);
    return new Fp(r.compareTo(P) < 0 ? r : r.subtract(P));
  }

  @Override
  public Fp mul(Fp o) {
    return new Fp(redc(v.multiply(o.v)));
  }

  @Override
  public Fp sub(Fp o) {
    BigInteger r = v.subtract(o.v);
    return new Fp(r.compareTo(BigInteger.ZERO) < 0 ? r.add(P) : r);
  }

  @Override
  public Fp squared() {
    return new Fp(redc(v.multiply(v)));
  }

  @Override
  public Fp dbl() {
    return add(this);
  }

  @Override
  public Fp inverse() {
    return new Fp(redc(v.modInverse(P).multiply(REDUCER_CUBED)));
  }

  @Override
  public Fp negate() {
    return new Fp(v.negate().mod(P));
  }

  @Override
  public boolean isZero() {
    return v.compareTo(BigInteger.ZERO) == 0;
  }

  Fp2 mul(Fp2 o) {
    return new Fp2(o.a.mul(this), o.b.mul(this));
  }

  public byte[] bytes() {
    return fromMontgomery(v).toByteArray();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Fp fp = (Fp) o;

    return !(v != null ? v.compareTo(fp.v) != 0 : fp.v != null);
  }

  @Override
  public int hashCode() {
    return v.hashCode();
  }

  @Override
  public String toString() {
    return v.toString();
  }

  /**
   * Converts a value in normal representation to Montgomery form.
   * 
   * @param n value in normal form
   * @return value in Montgomery form
   */
  private static BigInteger toMontgomery(BigInteger n) {
    return redc(n.multiply(REDUCER_SQUARED));
  }

  /**
   * Converts a value in Montgomery form to a normal representation.
   * 
   * @param n value in Montgomery form
   * @return value in normal form
   */
  private static BigInteger fromMontgomery(BigInteger n) {
    return redc(n);
  }

  /**
   * Montgomery reduction; given a value x, computes x*{@link #REDUCER}^{-1} mod {@link #P}
   * 
   * @param x value to reduce
   * @return x*{@link #REDUCER}^{-1} mod {@link #P}
   */
  private static BigInteger redc(BigInteger x)  {
    BigInteger temp = x.multiply(FACTOR).and(MASK);
    BigInteger reduced = temp.multiply(P).add(x).shiftRight(REDUCER_BITS);
    return reduced.compareTo(P) < 0 ? reduced : reduced.subtract(P);
  }
}
