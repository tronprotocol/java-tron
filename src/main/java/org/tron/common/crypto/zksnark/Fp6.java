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
 * Arithmetic in Fp_6 <br/>
 * <br/>
 *
 * "p" equals 21888242871839275222246405745257275088696311157297823662689037894645226208583, <br/>
 * elements of Fp_6 are represented with 3 elements of {@link Fp2} <br/>
 * <br/>
 *
 * Field arithmetic is ported from <a href="https://github.com/scipr-lab/libff/blob/master/libff/algebra/fields/fp6_3over2.tcc">libff</a>
 *
 * @author Mikhail Kalinin
 * @since 05.09.2017
 */
class Fp6 implements Field<Fp6> {

    static final Fp6 ZERO = new Fp6(Fp2.ZERO, Fp2.ZERO, Fp2.ZERO);
    static final Fp6 _1 = new Fp6(Fp2._1, Fp2.ZERO, Fp2.ZERO);
    static final Fp2 NON_RESIDUE = new Fp2(BigInteger.valueOf(9), BigInteger.ONE);

    Fp2 a;
    Fp2 b;
    Fp2 c;

    Fp6(Fp2 a, Fp2 b, Fp2 c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    @Override
    public Fp6 squared() {

        Fp2 s0 = a.squared();
        Fp2 ab = a.mul(b);
        Fp2 s1 = ab.dbl();
        Fp2 s2 = a.sub(b).add(c).squared();
        Fp2 bc = b.mul(c);
        Fp2 s3 = bc.dbl();
        Fp2 s4 = c.squared();

        Fp2 ra = s0.add(s3.mulByNonResidue());
        Fp2 rb = s1.add(s4.mulByNonResidue());
        Fp2 rc = s1.add(s2).add(s3).sub(s0).sub(s4);

        return new Fp6(ra, rb, rc);
    }

    @Override
    public Fp6 dbl() {
        return this.add(this);
    }

    @Override
    public Fp6 mul(Fp6 o) {

        Fp2 a1 = a,   b1 = b,   c1 = c;
        Fp2 a2 = o.a, b2 = o.b, c2 = o.c;

        Fp2 a1a2 = a1.mul(a2);
        Fp2 b1b2 = b1.mul(b2);
        Fp2 c1c2 = c1.mul(c2);

        Fp2 ra = a1a2.add(b1.add(c1).mul(b2.add(c2)).sub(b1b2).sub(c1c2).mulByNonResidue());
        Fp2 rb = a1.add(b1).mul(a2.add(b2)).sub(a1a2).sub(b1b2).add(c1c2.mulByNonResidue());
        Fp2 rc = a1.add(c1).mul(a2.add(c2)).sub(a1a2).add(b1b2).sub(c1c2);

        return new Fp6(ra, rb, rc);
    }

    Fp6 mul(Fp2 o) {

        Fp2 ra = a.mul(o);
        Fp2 rb = b.mul(o);
        Fp2 rc = c.mul(o);

        return new Fp6(ra, rb, rc);
    }

    Fp6 mulByNonResidue() {

        Fp2 ra = NON_RESIDUE.mul(c);
        Fp2 rb = a;
        Fp2 rc = b;

        return new Fp6(ra, rb, rc);
    }

    @Override
    public Fp6 add(Fp6 o) {

        Fp2 ra = a.add(o.a);
        Fp2 rb = b.add(o.b);
        Fp2 rc = c.add(o.c);

        return new Fp6(ra, rb, rc);
    }

    @Override
    public Fp6 sub(Fp6 o) {

        Fp2 ra = a.sub(o.a);
        Fp2 rb = b.sub(o.b);
        Fp2 rc = c.sub(o.c);

        return new Fp6(ra, rb, rc);
    }

    @Override
    public Fp6 inverse() {

        /* From "High-Speed Software Implementation of the Optimal Ate Pairing over Barreto-Naehrig Curves"; Algorithm 17 */

        Fp2 t0 = a.squared();
        Fp2 t1 = b.squared();
        Fp2 t2 = c.squared();
        Fp2 t3 = a.mul(b);
        Fp2 t4 = a.mul(c);
        Fp2 t5 = b.mul(c);
        Fp2 c0 = t0.sub(t5.mulByNonResidue());
        Fp2 c1 = t2.mulByNonResidue().sub(t3);
        Fp2 c2 = t1.sub(t4); // typo in paper referenced above. should be "-" as per Scott, but is "*"
        Fp2 t6 = a.mul(c0).add((c.mul(c1).add(b.mul(c2))).mulByNonResidue()).inverse();

        Fp2 ra = t6.mul(c0);
        Fp2 rb = t6.mul(c1);
        Fp2 rc = t6.mul(c2);

        return new Fp6(ra, rb, rc);
    }

    @Override
    public Fp6 negate() {
        return new Fp6(a.negate(), b.negate(), c.negate());
    }

    @Override
    public boolean isZero() {
        return this.equals(ZERO);
    }

    @Override
    public boolean isValid() {
        return a.isValid() && b.isValid() && c.isValid();
    }

    Fp6 frobeniusMap(int power) {

        Fp2 ra = a.frobeniusMap(power);
        Fp2 rb = FROBENIUS_COEFFS_B[power % 6].mul(b.frobeniusMap(power));
        Fp2 rc = FROBENIUS_COEFFS_C[power % 6].mul(c.frobeniusMap(power));

        return new Fp6(ra, rb, rc);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o.getClass() == getClass())) return false;

        Fp6 fp6 = (Fp6) o;

        if (a != null ? !a.equals(fp6.a) : fp6.a != null) return false;
        if (b != null ? !b.equals(fp6.b) : fp6.b != null) return false;
        return !(c != null ? !c.equals(fp6.c) : fp6.c != null);
    }

    @Override
    public int hashCode() {
        return new Integer(a.hashCode() + b.hashCode() + c.hashCode()).hashCode();
    }

    static final Fp2[] FROBENIUS_COEFFS_B = {

            new Fp2(BigInteger.ONE,
                    BigInteger.ZERO),

            new Fp2(new BigInteger("21575463638280843010398324269430826099269044274347216827212613867836435027261"),
                    new BigInteger("10307601595873709700152284273816112264069230130616436755625194854815875713954")),

            new Fp2(new BigInteger("21888242871839275220042445260109153167277707414472061641714758635765020556616"),
                    BigInteger.ZERO),

            new Fp2(new BigInteger("3772000881919853776433695186713858239009073593817195771773381919316419345261"),
                    new BigInteger("2236595495967245188281701248203181795121068902605861227855261137820944008926")),

            new Fp2(new BigInteger("2203960485148121921418603742825762020974279258880205651966"),
                    BigInteger.ZERO),

            new Fp2(new BigInteger("18429021223477853657660792034369865839114504446431234726392080002137598044644"),
                    new BigInteger("9344045779998320333812420223237981029506012124075525679208581902008406485703"))
    };

    static final Fp2[] FROBENIUS_COEFFS_C = {

            new Fp2(BigInteger.ONE,
                    BigInteger.ZERO),

            new Fp2(new BigInteger("2581911344467009335267311115468803099551665605076196740867805258568234346338"),
                    new BigInteger("19937756971775647987995932169929341994314640652964949448313374472400716661030")),

            new Fp2(new BigInteger("2203960485148121921418603742825762020974279258880205651966"),
                    BigInteger.ZERO),

            new Fp2(new BigInteger("5324479202449903542726783395506214481928257762400643279780343368557297135718"),
                    new BigInteger("16208900380737693084919495127334387981393726419856888799917914180988844123039")),

            new Fp2(new BigInteger("21888242871839275220042445260109153167277707414472061641714758635765020556616"),
                    BigInteger.ZERO),

            new Fp2(new BigInteger("13981852324922362344252311234282257507216387789820983642040889267519694726527"),
                    new BigInteger("7629828391165209371577384193250820201684255241773809077146787135900891633097"))
    };
}
