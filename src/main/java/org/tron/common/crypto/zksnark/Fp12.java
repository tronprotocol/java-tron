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
 * Arithmetic in Fp_12 <br/>
 * <br/>
 *
 * "p" equals 21888242871839275222246405745257275088696311157297823662689037894645226208583, <br/>
 * elements of Fp_12 are represented with 2 elements of {@link Fp6} <br/>
 * <br/>
 *
 * Field arithmetic is ported from <a href="https://github.com/scipr-lab/libff/blob/master/libff/algebra/fields/fp12_2over3over2.tcc">libff</a>
 *
 * @author Mikhail Kalinin
 * @since 02.09.2017
 */
class Fp12 implements Field<Fp12> {

    static final Fp12 ZERO = new Fp12(Fp6.ZERO, Fp6.ZERO);
    static final Fp12 _1 = new Fp12(Fp6._1, Fp6.ZERO);

    Fp6 a;
    Fp6 b;

    Fp12 (Fp6 a, Fp6 b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public Fp12 squared() {

        Fp6 ab = a.mul(b);

        Fp6 ra = a.add(b).mul(a.add(b.mulByNonResidue())).sub(ab).sub(ab.mulByNonResidue());
        Fp6 rb = ab.add(ab);

        return new Fp12(ra, rb);
    }

    @Override
    public Fp12 dbl() {
        return null;
    }

    Fp12 mulBy024(Fp2 ell0, Fp2 ellVW, Fp2 ellVV) {

        Fp2 z0 = a.a;
        Fp2 z1 = a.b;
        Fp2 z2 = a.c;
        Fp2 z3 = b.a;
        Fp2 z4 = b.b;
        Fp2 z5 = b.c;

        Fp2 x0 = ell0;
        Fp2 x2 = ellVV;
        Fp2 x4 = ellVW;

        Fp2 t0, t1, t2, s0, t3, t4, d0, d2, d4, s1;

        d0 = z0.mul(x0);
        d2 = z2.mul(x2);
        d4 = z4.mul(x4);
        t2 = z0.add(z4);
        t1 = z0.add(z2);
        s0 = z1.add(z3).add(z5);

        // For z.a_.a_ = z0.
        s1 = z1.mul(x2);
        t3 = s1.add(d4);
        t4 = Fp6.NON_RESIDUE.mul(t3).add(d0);
        z0 = t4;

        // For z.a_.b_ = z1
        t3 = z5.mul(x4);
        s1 = s1.add(t3);
        t3 = t3.add(d2);
        t4 = Fp6.NON_RESIDUE.mul(t3);
        t3 = z1.mul(x0);
        s1 = s1.add(t3);
        t4 = t4.add(t3);
        z1 = t4;

        // For z.a_.c_ = z2
        t0 = x0.add(x2);
        t3 = t1.mul(t0).sub(d0).sub(d2);
        t4 = z3.mul(x4);
        s1 = s1.add(t4);
        t3 = t3.add(t4);

        // For z.b_.a_ = z3 (z3 needs z2)
        t0 = z2.add(z4);
        z2 = t3;
        t1 = x2.add(x4);
        t3 = t0.mul(t1).sub(d2).sub(d4);
        t4 = Fp6.NON_RESIDUE.mul(t3);
        t3 = z3.mul(x0);
        s1 = s1.add(t3);
        t4 = t4.add(t3);
        z3 = t4;

        // For z.b_.b_ = z4
        t3 = z5.mul(x2);
        s1 = s1.add(t3);
        t4 = Fp6.NON_RESIDUE.mul(t3);
        t0 = x0.add(x4);
        t3 = t2.mul(t0).sub(d0).sub(d4);
        t4 = t4.add(t3);
        z4 = t4;

        // For z.b_.c_ = z5.
        t0 = x0.add(x2).add(x4);
        t3 = s0.mul(t0).sub(s1);
        z5 = t3;

        return new Fp12(new Fp6(z0, z1, z2), new Fp6(z3, z4, z5));
    }

    @Override
    public Fp12 add(Fp12 o) {
        return new Fp12(a.add(o.a), b.add(o.b));
    }

    @Override
    public Fp12 mul(Fp12 o) {

        Fp6 a2 = o.a, b2 = o.b;
        Fp6 a1 = a,   b1 = b;

        Fp6 a1a2 = a1.mul(a2);
        Fp6 b1b2 = b1.mul(b2);

        Fp6 ra = a1a2.add(b1b2.mulByNonResidue());
        Fp6 rb = a1.add(b1).mul(a2.add(b2)).sub(a1a2).sub(b1b2);

        return new Fp12(ra, rb);
    }

    @Override
    public Fp12 sub(Fp12 o) {
        return new Fp12(a.sub(o.a), b.sub(o.b));
    }

    @Override
    public Fp12 inverse() {
        
        Fp6 t0 = a.squared();
        Fp6 t1 = b.squared();
        Fp6 t2 = t0.sub(t1.mulByNonResidue());
        Fp6 t3 = t2.inverse();

        Fp6 ra = a.mul(t3);
        Fp6 rb = b.mul(t3).negate();

        return new Fp12(ra, rb);
    }

    @Override
    public Fp12 negate() {
        return new Fp12(a.negate(), b.negate());
    }

    @Override
    public boolean isZero() {
        return this.equals(ZERO);
    }

    @Override
    public boolean isValid() {
        return a.isValid() && b.isValid();
    }

    Fp12 frobeniusMap(int power) {

        Fp6 ra = a.frobeniusMap(power);
        Fp6 rb = b.frobeniusMap(power).mul(FROBENIUS_COEFFS_B[power % 12]);

        return new Fp12(ra, rb);
    }

    Fp12 cyclotomicSquared() {
        
        Fp2 z0 = a.a;
        Fp2 z4 = a.b;
        Fp2 z3 = a.c;
        Fp2 z2 = b.a;
        Fp2 z1 = b.b;
        Fp2 z5 = b.c;

        Fp2 t0, t1, t2, t3, t4, t5, tmp;

        // t0 + t1*y = (z0 + z1*y)^2 = a^2
        tmp = z0.mul(z1);
        t0 = z0.add(z1).mul(z0.add(Fp6.NON_RESIDUE.mul(z1))).sub(tmp).sub(Fp6.NON_RESIDUE.mul(tmp));
        t1 = tmp.add(tmp);
        // t2 + t3*y = (z2 + z3*y)^2 = b^2
        tmp = z2.mul(z3);
        t2 = z2.add(z3).mul(z2.add(Fp6.NON_RESIDUE.mul(z3))).sub(tmp).sub(Fp6.NON_RESIDUE.mul(tmp));
        t3 = tmp.add(tmp);
        // t4 + t5*y = (z4 + z5*y)^2 = c^2
        tmp = z4.mul(z5);
        t4 = z4.add(z5).mul(z4.add(Fp6.NON_RESIDUE.mul(z5))).sub(tmp).sub(Fp6.NON_RESIDUE.mul(tmp));
        t5 = tmp.add(tmp);

        // for A

        // z0 = 3 * t0 - 2 * z0
        z0 = t0.sub(z0);
        z0 = z0.add(z0);
        z0 = z0.add(t0);
        // z1 = 3 * t1 + 2 * z1
        z1 = t1.add(z1);
        z1 = z1.add(z1);
        z1 = z1.add(t1);

        // for B

        // z2 = 3 * (xi * t5) + 2 * z2
        tmp = Fp6.NON_RESIDUE.mul(t5);
        z2 = tmp.add(z2);
        z2 = z2.add(z2);
        z2 = z2.add(tmp);

        // z3 = 3 * t4 - 2 * z3
        z3 = t4.sub(z3);
        z3 = z3.add(z3);
        z3 = z3.add(t4);

        // for C

        // z4 = 3 * t2 - 2 * z4
        z4 = t2.sub(z4);
        z4 = z4.add(z4);
        z4 = z4.add(t2);

        // z5 = 3 * t3 + 2 * z5
        z5 = t3.add(z5);
        z5 = z5.add(z5);
        z5 = z5.add(t3);
        
        return new Fp12(new Fp6(z0, z4, z3), new Fp6(z2, z1, z5));
    }

    Fp12 cyclotomicExp(BigInteger pow) {

        Fp12 res = _1;

        for (int i = pow.bitLength() - 1; i >=0; i--) {
            res = res.cyclotomicSquared();

            if (pow.testBit(i)) {
                res = res.mul(this);
            }
        }

        return res;
    }

    Fp12 unitaryInverse() {

        Fp6 ra = a;
        Fp6 rb = b.negate();

        return new Fp12(ra, rb);
    }

    Fp12 negExp(BigInteger exp) {
        return this.cyclotomicExp(exp).unitaryInverse();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Fp12)) return false;

        Fp12 fp12 = (Fp12) o;

        if (a != null ? !a.equals(fp12.a) : fp12.a != null) return false;
        return !(b != null ? !b.equals(fp12.b) : fp12.b != null);

    }

    @Override
    public String toString() {
        return String.format(
                "Fp12 (%s; %s)\n" +
                "     (%s; %s)\n" +
                "     (%s; %s)\n" +
                "     (%s; %s)\n" +
                "     (%s; %s)\n" +
                "     (%s; %s)\n",

                a.a.a, a.a.b,
                a.b.a, a.b.b,
                a.c.a, a.c.b,
                b.a.a, b.a.b,
                b.b.a, b.b.b,
                b.c.a, b.c.b
        );
    }

    static final Fp2[] FROBENIUS_COEFFS_B = new Fp2[] {

            new Fp2(BigInteger.ONE,
                    BigInteger.ZERO),

            new Fp2(new BigInteger("8376118865763821496583973867626364092589906065868298776909617916018768340080"),
                    new BigInteger("16469823323077808223889137241176536799009286646108169935659301613961712198316")),

            new Fp2(new BigInteger("21888242871839275220042445260109153167277707414472061641714758635765020556617"),
                    BigInteger.ZERO),

            new Fp2(new BigInteger("11697423496358154304825782922584725312912383441159505038794027105778954184319"),
                    new BigInteger("303847389135065887422783454877609941456349188919719272345083954437860409601")),

            new Fp2(new BigInteger("21888242871839275220042445260109153167277707414472061641714758635765020556616"),
                    BigInteger.ZERO),

            new Fp2(new BigInteger("3321304630594332808241809054958361220322477375291206261884409189760185844239"),
                    new BigInteger("5722266937896532885780051958958348231143373700109372999374820235121374419868")),

            new Fp2(new BigInteger("21888242871839275222246405745257275088696311157297823662689037894645226208582"),
                    BigInteger.ZERO),

            new Fp2(new BigInteger("13512124006075453725662431877630910996106405091429524885779419978626457868503"),
                    new BigInteger("5418419548761466998357268504080738289687024511189653727029736280683514010267")),

            new Fp2(new BigInteger("2203960485148121921418603742825762020974279258880205651966"),
                    BigInteger.ZERO),

            new Fp2(new BigInteger("10190819375481120917420622822672549775783927716138318623895010788866272024264"),
                    new BigInteger("21584395482704209334823622290379665147239961968378104390343953940207365798982")),

            new Fp2(new BigInteger("2203960485148121921418603742825762020974279258880205651967"),
                    BigInteger.ZERO),

            new Fp2(new BigInteger("18566938241244942414004596690298913868373833782006617400804628704885040364344"),
                    new BigInteger("16165975933942742336466353786298926857552937457188450663314217659523851788715"))
    };
}
