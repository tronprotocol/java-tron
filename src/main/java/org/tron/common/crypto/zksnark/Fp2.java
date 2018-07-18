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
 * Arithmetic in F_p2 <br/>
 * <br/>
 *
 * "p" equals 21888242871839275222246405745257275088696311157297823662689037894645226208583,
 * elements of F_p2 are represented as a polynomials "a * i + b" modulo "i^2 + 1" from the ring F_p[i] <br/>
 * <br/>
 *
 * Field arithmetic is ported from <a href="https://github.com/scipr-lab/libff/blob/master/libff/algebra/fields/fp2.tcc">libff</a> <br/>
 *
 * @author Mikhail Kalinin
 * @since 01.09.2017
 */
class Fp2 implements Field<Fp2> {

    static final Fp2 ZERO = new Fp2(Fp.ZERO, Fp.ZERO);
    static final Fp2 _1  = new Fp2(Fp._1, Fp.ZERO);
    static final Fp2 NON_RESIDUE = new Fp2(BigInteger.valueOf(9), BigInteger.ONE);

    static final Fp[] FROBENIUS_COEFFS_B = new Fp[] {
            new Fp(BigInteger.ONE),
            new Fp(new BigInteger("21888242871839275222246405745257275088696311157297823662689037894645226208582"))
    };

    Fp a;
    Fp b;

    Fp2(Fp a, Fp b) {
        this.a = a;
        this.b = b;
    }

    Fp2(BigInteger a, BigInteger b) {
        this(new Fp(a), new Fp(b));
    }

    @Override
    public Fp2 squared() {

        // using Complex squaring

        Fp ab = a.mul(b);

        Fp ra = a.add(b).mul(b.mul(Fp.NON_RESIDUE).add(a))
                .sub(ab).sub(ab.mul(Fp.NON_RESIDUE)); // ra = (a + b)(a + NON_RESIDUE * b) - ab - NON_RESIDUE * b
        Fp rb = ab.dbl();

        return new Fp2(ra, rb);
    }

    @Override
    public Fp2 mul(Fp2 o) {

        Fp aa = a.mul(o.a);
        Fp bb = b.mul(o.b);

        Fp ra = bb.mul(Fp.NON_RESIDUE).add(aa);    // ra = a1 * a2 + NON_RESIDUE * b1 * b2
        Fp rb = a.add(b).mul(o.a.add(o.b)).sub(aa).sub(bb);     // rb = (a1 + b1)(a2 + b2) - a1 * a2 - b1 * b2

        return new Fp2(ra, rb);
    }

    @Override
    public Fp2 add(Fp2 o) {
        return new Fp2(a.add(o.a), b.add(o.b));
    }

    @Override
    public Fp2 sub(Fp2 o) {
        return new Fp2(a.sub(o.a), b.sub(o.b));
    }

    @Override
    public Fp2 dbl() {
        return this.add(this);
    }

    @Override
    public Fp2 inverse() {

        Fp t0 = a.squared();
        Fp t1 = b.squared();
        Fp t2 = t0.sub(Fp.NON_RESIDUE.mul(t1));
        Fp t3 = t2.inverse();

        Fp ra = a.mul(t3);          // ra = a * t3
        Fp rb = b.mul(t3).negate(); // rb = -(b * t3)

        return new Fp2(ra, rb);
    }

    @Override
    public Fp2 negate() {
        return new Fp2(a.negate(), b.negate());
    }

    @Override
    public boolean isZero() {
        return this.equals(ZERO);
    }

    @Override
    public boolean isValid() {
        return a.isValid() && b.isValid();
    }

    static Fp2 create(BigInteger aa, BigInteger bb) {

        Fp a = Fp.create(aa);
        Fp b = Fp.create(bb);

        return new Fp2(a, b);
    }

    static Fp2 create(byte[] aa, byte[] bb) {

        Fp a = Fp.create(aa);
        Fp b = Fp.create(bb);

        return new Fp2(a, b);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Fp2 fp2 = (Fp2) o;

        if (a != null ? !a.equals(fp2.a) : fp2.a != null) return false;
        return !(b != null ? !b.equals(fp2.b) : fp2.b != null);

    }

    @Override
    public int hashCode() {
        return new Integer(a.hashCode() + b.hashCode()).hashCode();
    }

    Fp2 frobeniusMap(int power) {

        Fp ra = a;
        Fp rb = FROBENIUS_COEFFS_B[power % 2].mul(b);

        return new Fp2(ra, rb);
    }

    Fp2 mulByNonResidue() {
        return NON_RESIDUE.mul(this);
    }

    @Override
    public String toString() {
        return String.format("%si + %s", a.toString(), b.toString());
    }
}
