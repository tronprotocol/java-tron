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
 * Implementation of Barreto–Naehrig curve defined over abstract finite field. This curve is one of the keys to zkSNARKs. <br/>
 * This specific curve was introduced in
 * <a href="https://github.com/scipr-lab/libff#elliptic-curve-choices">libff</a>
 * and used by a proving system in
 * <a href="https://github.com/zcash/zcash/wiki/specification#zcash-protocol">ZCash protocol</a> <br/>
 * <br/>
 *
 * Curve equation: <br/>
 * Y^2 = X^3 + b, where "b" is a constant number belonging to corresponding specific field <br/>
 * Point at infinity is encoded as <code>(0, 0, 0)</code> <br/>
 * <br/>
 *
 * This curve has embedding degree 12 with respect to "r" (see {@link Params#R}), which means that "r" is a multiple of "p ^ 12 - 1",
 * this condition is important for pairing operation implemented in {@link PairingCheck}<br/>
 * <br/>
 *
 * Code of curve arithmetic has been ported from
 * <a href="https://github.com/scipr-lab/libff/blob/master/libff/algebra/curves/alt_bn128/alt_bn128_g1.cpp">libff</a> <br/>
 * <br/>
 *
 * Current implementation uses Jacobian coordinate system as
 * <a href="https://github.com/scipr-lab/libff/blob/master/libff/algebra/curves/alt_bn128/alt_bn128_g1.cpp">libff</a> does,
 * use {@link #toEthNotation()} to convert Jacobian coords to Ethereum encoding <br/>
 *
 * @author Mikhail Kalinin
 * @since 05.09.2017
 */
public abstract class BN128<T extends Field<T>> {

    protected T x;
    protected T y;
    protected T z;

    protected BN128(T x, T y, T z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Point at infinity in Ethereum notation: should return (0; 0; 0),
     * {@link #isZero()} method called for that point, also, returns {@code true}
     */
    abstract protected BN128<T> zero();
    abstract protected BN128<T> instance(T x, T y, T z);
    abstract protected T b();
    abstract protected T one();

    /**
     * Transforms given Jacobian to affine coordinates and then creates a point
     */
    public BN128<T> toAffine() {

        if (isZero()) {
            BN128<T> zero = zero();
            return instance(zero.x, one(), zero.z); // (0; 1; 0)
        }

        T zInv = z.inverse();
        T zInv2 = zInv.squared();
        T zInv3 = zInv2.mul(zInv);

        T ax = x.mul(zInv2);
        T ay = y.mul(zInv3);

        return instance(ax, ay, one());
    }

    /**
     * Runs affine transformation and encodes point at infinity as (0; 0; 0)
     */
    public BN128<T> toEthNotation() {
        BN128<T> affine = toAffine();

        // affine zero is (0; 1; 0), convert to Ethereum zero: (0; 0; 0)
        if (affine.isZero()) {
            return zero();
        } else {
            return affine;
        }
    }

    protected boolean isOnCurve() {

        if (isZero()) return true;

        T z6 = z.squared().mul(z).squared();

        T left  = y.squared();                          // y^2
        T right = x.squared().mul(x).add(b().mul(z6));  // x^3 + b * z^6
        return left.equals(right);
    }

    public BN128<T> add(BN128<T> o) {

        if (this.isZero()) return o; // 0 + P = P
        if (o.isZero()) return this; // P + 0 = P

        T x1 = this.x, y1 = this.y, z1 = this.z;
        T x2 = o.x,    y2 = o.y,    z2 = o.z;

        // ported code is started from here
        // next calculations are done in Jacobian coordinates

        T z1z1 = z1.squared();
        T z2z2 = z2.squared();

        T u1 = x1.mul(z2z2);
        T u2 = x2.mul(z1z1);

        T z1Cubed = z1.mul(z1z1);
        T z2Cubed = z2.mul(z2z2);

        T s1 = y1.mul(z2Cubed);      // s1 = y1 * Z2^3
        T s2 = y2.mul(z1Cubed);      // s2 = y2 * Z1^3

        if (u1.equals(u2) && s1.equals(s2)) {
            return dbl(); // P + P = 2P
        }

        T h = u2.sub(u1);          // h = u2 - u1
        T i = h.dbl().squared();   // i = (2 * h)^2
        T j = h.mul(i);            // j = h * i
        T r = s2.sub(s1).dbl();    // r = 2 * (s2 - s1)
        T v = u1.mul(i);           // v = u1 * i
        T zz = z1.add(z2).squared()
                .sub(z1.squared()).sub(z2.squared());

        T x3 = r.squared().sub(j).sub(v.dbl());        // x3 = r^2 - j - 2 * v
        T y3 = v.sub(x3).mul(r).sub(s1.mul(j).dbl());  // y3 = r * (v - x3) - 2 * (s1 * j)
        T z3 = zz.mul(h); // z3 = ((z1+z2)^2 - z1^2 - z2^2) * h = zz * h

        return instance(x3, y3, z3);
    }

    public BN128<T> mul(BigInteger s) {

        if (s.compareTo(BigInteger.ZERO) == 0) // P * 0 = 0
            return zero();

        if (isZero()) return this; // 0 * s = 0

        BN128<T> res = zero();

        for (int i = s.bitLength() - 1; i >= 0; i--) {

            res = res.dbl();

            if (s.testBit(i)) {
                res = res.add(this);
            }
        }

        return res;
    }

    private BN128<T> dbl() {

        if (isZero()) return this;

        // ported code is started from here
        // next calculations are done in Jacobian coordinates with z = 1

        T a = x.squared();     // a = x^2
        T b = y.squared();     // b = y^2
        T c = b.squared();     // c = b^2
        T d = x.add(b).squared().sub(a).sub(c);
        d = d.add(d);                              // d = 2 * ((x + b)^2 - a - c)
        T e = a.add(a).add(a);  // e = 3 * a
        T f = e.squared();     // f = e^2

        T x3 = f.sub(d.add(d)); // rx = f - 2 * d
        T y3 = e.mul(d.sub(x3)).sub(c.dbl().dbl().dbl()); // ry = e * (d - rx) - 8 * c
        T z3 = y.mul(z).dbl(); // z3 = 2 * y * z

        return instance(x3, y3, z3);
    }

    public T x() {
        return x;
    }

    public T y() {
        return y;
    }

    public boolean isZero() {
        return z.isZero();
    }

    protected boolean isValid() {

        // check whether coordinates belongs to the Field
        if (!x.isValid() || !y.isValid() || !z.isValid()) {
            return false;
        }

        // check whether point is on the curve
        if (!isOnCurve()) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return String.format("(%s; %s; %s)", x.toString(), y.toString(), z.toString());
    }

    @Override
    @SuppressWarnings("all")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BN128)) return false;

        BN128<?> bn128 = (BN128<?>) o;

        if (x != null ? !x.equals(bn128.x) : bn128.x != null) return false;
        if (y != null ? !y.equals(bn128.y) : bn128.y != null) return false;
        return !(z != null ? !z.equals(bn128.z) : bn128.z != null);
    }
}
