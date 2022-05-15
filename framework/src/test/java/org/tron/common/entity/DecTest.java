package org.tron.common.entity;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.stream.Stream;

public class DecTest {


    // Showcase that different orders of operations causes different results.
    @Test
    public void TestOperationOrders() {
        Dec n1 = Dec.newDec(10);
        Dec n2 = Dec.newDec(1000000010);
        Assert.assertEquals(n1.mul(n2).quo(n2), Dec.newDec(10));
        Assert.assertNotEquals(n1.mul(n2).quo(n2), n1.quo(n2).mul(n2));
    }

    @Test
    public void TestDecCeil() {
        Stream.of(
                // 0.001 => 1.0
                new Pair<>(Dec.newDecWithPrec(1000000000000000L, Dec.precision), Dec.newDec(1)),
                // -0.001 => 0.0
                new Pair<>(Dec.newDecWithPrec(-1000000000000000L, Dec.precision), Dec.zeroDec()),
                // 0.0 => 0.0
                new Pair<>(Dec.zeroDec(), Dec.zeroDec()),
                // 0.9 => 1.0
                new Pair<>(Dec.newDecWithPrec(900000000000000000L, Dec.precision), Dec.newDec(1)),
                // 4.001 => 5.0
                new Pair<>(Dec.newDecWithPrec(4001000000000000000L, Dec.precision), Dec.newDec(5)),
                // -4.001 => -4.0
                new Pair<>(Dec.newDecWithPrec(-4001000000000000000L, Dec.precision), Dec.newDec(-4)),
                // 4.7 => 5.0
                new Pair<>(Dec.newDecWithPrec(4700000000000000000L, Dec.precision), Dec.newDec(5)),
                // -4.7 => -4.0
                new Pair<>(Dec.newDecWithPrec(-4700000000000000000L, Dec.precision), Dec.newDec(-4))
        ).forEach(e -> Assert.assertEquals(e.v, e.k.ceil()));
    }

    @Test
    public void TestPower() {
        Stream.of(
                // 1.0 ^ (10) => 1.0
                new Pair<>(Dec.oneDec(), BigInteger.valueOf(10), Dec.oneDec()),
                // 0.5 ^ 2 => 0.25
                new Pair<>(Dec.newDecWithPrec(5, 1), BigInteger.valueOf(2),
                        Dec.newDecWithPrec(25, 2)),
                // 0.2 ^ 2 => 0.04
                new Pair<>(Dec.newDecWithPrec(2, 1), BigInteger.valueOf(2),
                        Dec.newDecWithPrec(4, 2)),
                // 3 ^ 3 => 27
                new Pair<>(Dec.newDec(3), BigInteger.valueOf(3), Dec.newDec(27)),
                // -3 ^ 4 = 81
                new Pair<>(Dec.newDec(-3), BigInteger.valueOf(4), Dec.newDec(81)),
                //// 1.414213562373095049 ^ 2 = 2
                new Pair<>(Dec.newDecWithPrec(1414213562373095049L, 18), BigInteger.valueOf(2),
                        Dec.newDec(2))
        ).forEach(tc -> Assert.assertTrue(tc.s.sub(tc.k.power(tc.v)).abs().lte(Dec.smallestDec())));
    }

    @Test
    public void TestApproxRoot() {
        Stream.of(
                // 1.0 ^ (0.1) => 1.0
                new Pair<>(Dec.oneDec(), 10, Dec.oneDec()),
                // 0.25 ^ (0.5) => 0.5
                new Pair<>(Dec.newDecWithPrec(25, 2), 2, Dec.newDecWithPrec(5, 1)),
                // 0.04 ^ (0.5) => 0.2
                new Pair<>(Dec.newDecWithPrec(4, 2), 2, Dec.newDecWithPrec(2, 1)),
                // 27 ^ (1/3) => 3
                new Pair<>(Dec.newDec(27), 3, Dec.newDec(3)),
                // -81 ^ (0.25) => -3
                new Pair<>(Dec.newDec(-81), 4, Dec.newDec(-3)),
                // 2 ^ (0.5) => 1.414213562373095049
                new Pair<>(Dec.newDec(2), 2,
                        Dec.newDecWithPrec(1414213562373095049L, 18)),
                // 1.005 ^ (1/31536000) ≈ 1.00000000016
                new Pair<>(Dec.newDecWithPrec(1005, 3), 31536000, Dec.newDec("1.000000000158153904")),
                // 1e-18 ^ (0.5) => 1e-9
                new Pair<>(Dec.smallestDec(), 2, Dec.newDecWithPrec(1, 9)),
                // 1e-18 ^ (1/3) => 1e-6
                new Pair<>(Dec.smallestDec(), 3, Dec.newDec("0.000000999999999997")),
                // 1e-8 ^ (1/3) ≈ 0.00215443469
                new Pair<>(Dec.newDecWithPrec(1, 8), 3, Dec.newDec("0.002154434690031900"))
        ).forEach(tc -> Assert.assertTrue(tc.s.sub(tc.k.approxRoot(tc.v)).abs().lte(Dec.smallestDec())));

        // In the case of 1e-8 ^ (1/3), the result repeats every 5 iterations starting from iteration 24
        // (i.e. 24, 29, 34, ... give the same result) and never converges enough. The maximum number of
        // iterations (100) causes the result at iteration 100 to be returned, regardless of convergence.

    }

    @Test
    public void TestApproxSqrt() {
        Stream.of(
                // 1.0 => 1.0
                new Pair<>(Dec.oneDec(), Dec.oneDec()),
                // 0.25 => 0.5
                new Pair<>(Dec.newDecWithPrec(25, 2), Dec.newDecWithPrec(5, 1)),
                // 0.09 => 0.3
                new Pair<>(Dec.newDecWithPrec(9, 2), Dec.newDecWithPrec(3, 1)),
                // 9 => 3
                new Pair<>(Dec.newDec(9), Dec.newDec(3)),
                // -9 => -3
                new Pair<>(Dec.newDec(-9), Dec.newDec(-3)),
                // 2 => 1.414213562373095049
                new Pair<>(Dec.newDec(2),
                        Dec.newDecWithPrec(1414213562373095049L, 18)))
                .forEach(tc ->
                        Assert.assertEquals(tc.v, tc.k.approxSqrt()
                        ));

    }


    @Test
    public void TestDecMulInt() {

        Stream.of(
                new Pair<>(Dec.newDec(10), 2, Dec.newDec(20)),
                new Pair<>(Dec.newDec(1000000), 100, Dec.newDec(100000000)),
                new Pair<>(Dec.newDecWithPrec(1, 1), 10, Dec.newDec(1)),
                new Pair<>(Dec.newDecWithPrec(1, 5), 20, Dec.newDecWithPrec(2, 4))
        ).forEach(e -> Assert.assertEquals(e.k.mul(e.v), e.s));
    }

    @Test
    public void TestStringOverflow() {
        // two random 64 bit primes
        Dec dec1 = Dec.newDec("51643150036226787134389711697696177267");
        Dec dec2 = Dec.newDec("-31798496660535729618459429845579852627");
        Dec dec3 = dec1.add(dec2);
        Assert.assertEquals(
                "19844653375691057515930281852116324640", dec3.toString());
    }

    @Test
    public void TestTruncate() {

        Stream.of(
                new Pair<>(Dec.newDec("0"), 0),
                new Pair<>(Dec.newDec("0.25"), 0),
                new Pair<>(Dec.newDec("0.75"), 0),
                new Pair<>(Dec.newDec("1"), 1),
                new Pair<>(Dec.newDec("1.5"), 1),
                new Pair<>(Dec.newDec("7.5"), 7),
                new Pair<>(Dec.newDec("7.6"), 7),
                new Pair<>(Dec.newDec("7.4"), 7),
                new Pair<>(Dec.newDec("100.1"), 100),
                new Pair<>(Dec.newDec("1000.1"), 1000)
        ).forEach(e -> {
            Assert.assertEquals(-1 * e.v, e.k.neg().truncateLong());
            Assert.assertEquals(e.v.longValue(), e.k.truncateLong());
        });
    }


    @Test
    public void TestBankerRoundChop() {
        Stream.of(
                new Pair<>(Dec.newDec("0.25"), 0),
                new Pair<>(Dec.newDec("0"), 0),
                new Pair<>(Dec.newDec("1"), 1),
                new Pair<>(Dec.newDec("0.75"), 1),
                new Pair<>(Dec.newDec("0.5"), 0),
                new Pair<>(Dec.newDec("7.5"), 8),
                new Pair<>(Dec.newDec("1.5"), 2),
                new Pair<>(Dec.newDec("2.5"), 2),
                new Pair<>(Dec.newDec("0.545"), 1),
                new Pair<>(Dec.newDec("1.545"), 2)
        ).forEach(e -> {
            Assert.assertEquals(-1 * e.v, e.k.neg().roundLong());
            Assert.assertEquals(e.v.longValue(), e.k.roundLong());
        });

    }


    @Test
    public void TestArithmetic() {
        //  d1         d2         MUL    MulTruncate    QUO    QUORoundUp QUOTrunctate  ADD         SUB
        Stream.of(
                new Arithmetic(Dec.newDec(0), Dec.newDec(0), Dec.newDec(0), Dec.newDec(0), Dec.newDec(0),
                        Dec.newDec(0), Dec.newDec(0), Dec.newDec(0), Dec.newDec(0)),
                new Arithmetic(Dec.newDec(1), Dec.newDec(0), Dec.newDec(0), Dec.newDec(0), Dec.newDec(0),
                        Dec.newDec(0), Dec.newDec(0), Dec.newDec(1), Dec.newDec(1)),
                new Arithmetic(Dec.newDec(0), Dec.newDec(1), Dec.newDec(0), Dec.newDec(0), Dec.newDec(0),
                        Dec.newDec(0), Dec.newDec(0), Dec.newDec(1), Dec.newDec(-1)),
                new Arithmetic(Dec.newDec(0), Dec.newDec(-1), Dec.newDec(0), Dec.newDec(0), Dec.newDec(0),
                        Dec.newDec(0), Dec.newDec(0), Dec.newDec(-1), Dec.newDec(1)),
                new Arithmetic(Dec.newDec(-1), Dec.newDec(0), Dec.newDec(0), Dec.newDec(0), Dec.newDec(0),
                        Dec.newDec(0), Dec.newDec(0), Dec.newDec(-1), Dec.newDec(-1)),
                new Arithmetic(Dec.newDec(1), Dec.newDec(1), Dec.newDec(1), Dec.newDec(1), Dec.newDec(1),
                        Dec.newDec(1), Dec.newDec(1), Dec.newDec(2), Dec.newDec(0)),
                new Arithmetic(Dec.newDec(-1), Dec.newDec(-1), Dec.newDec(1), Dec.newDec(1), Dec.newDec(1),
                        Dec.newDec(1), Dec.newDec(1), Dec.newDec(-2), Dec.newDec(0)),
                new Arithmetic(Dec.newDec(1), Dec.newDec(-1), Dec.newDec(-1), Dec.newDec(-1), Dec.newDec(-1),
                        Dec.newDec(-1), Dec.newDec(-1), Dec.newDec(0), Dec.newDec(2)),
                new Arithmetic(Dec.newDec(-1), Dec.newDec(1), Dec.newDec(-1), Dec.newDec(-1), Dec.newDec(-1),
                        Dec.newDec(-1), Dec.newDec(-1), Dec.newDec(0), Dec.newDec(-2)),
                new Arithmetic(Dec.newDec(3), Dec.newDec(7), Dec.newDec(21), Dec.newDec(21),
                        Dec.newDecWithPrec(428571428571428571L, 18),
                        Dec.newDecWithPrec(428571428571428572L, 18),
                        Dec.newDecWithPrec(428571428571428571L, 18), Dec.newDec(10), Dec.newDec(-4)),
                new Arithmetic(Dec.newDec(2), Dec.newDec(4), Dec.newDec(8), Dec.newDec(8),
                        Dec.newDecWithPrec(5, 1), Dec.newDecWithPrec(5, 1),
                        Dec.newDecWithPrec(5, 1), Dec.newDec(6), Dec.newDec(-2)),
                new Arithmetic(Dec.newDec(100), Dec.newDec(100), Dec.newDec(10000), Dec.newDec(10000),
                        Dec.newDec(1), Dec.newDec(1), Dec.newDec(1), Dec.newDec(200), Dec.newDec(0)),
                new Arithmetic(Dec.newDecWithPrec(15, 1), Dec.newDecWithPrec(15, 1),
                        Dec.newDecWithPrec(225, 2), Dec.newDecWithPrec(225, 2), Dec.newDec(1),
                        Dec.newDec(1), Dec.newDec(1), Dec.newDec(3), Dec.newDec(0)),
                new Arithmetic(Dec.newDecWithPrec(3333, 4), Dec.newDecWithPrec(333, 4),
                        Dec.newDecWithPrec(1109889, 8), Dec.newDecWithPrec(1109889, 8),
                        Dec.newDec("10.009009009009009009"), Dec.newDec("10.009009009009009010"),
                        Dec.newDec("10.009009009009009009"), Dec.newDecWithPrec(3666, 4),
                        Dec.newDecWithPrec(3, 1)))
                .forEach(tc -> {
                    Dec resAdd = tc.d1.add(tc.d2);
                    Dec resSub = tc.d1.sub(tc.d2);
                    Dec resMul = tc.d1.mul(tc.d2);
                    Dec resMulTruncate = tc.d1.mulTruncate(tc.d2);
                    Assert.assertEquals(tc.expAdd, resAdd);
                    Assert.assertEquals(tc.expSub, resSub);
                    Assert.assertEquals(tc.expMul, resMul);
                    Assert.assertEquals(tc.expMulTruncate, resMulTruncate);

                    if (tc.d2.isZero()) { // panic for divide by zero
                        try {
                            tc.d1.quo(tc.d2);
                        } catch (RuntimeException e) {
                            Assert.assertEquals("BigInteger divide by zero", e.getMessage());
                        }

                    } else {
                        Dec resQuo = tc.d1.quo(tc.d2);
                        Dec resQuoRoundUp = tc.d1.quoRoundUp(tc.d2);

                        Dec resQuoTruncate = tc.d1.quoTruncate(tc.d2);
                        Assert.assertEquals(tc.expQuo, resQuo);
                        Assert.assertEquals(tc.expQuoRoundUp, resQuoRoundUp);
                        Assert.assertEquals(tc.expQuoTruncate, resQuoTruncate);
                    }
                });

    }

    @Test
    public void TestEqualities() {
        Stream.of(
                new Equalities(Dec.newDec(0), Dec.newDec(0), false, false, true),
                new Equalities(Dec.newDecWithPrec(0, 2), Dec.newDecWithPrec(0, 4),
                        false, false, true),
                new Equalities(Dec.newDecWithPrec(100, 0), Dec.newDecWithPrec(100, 0),
                        false, false, true),
                new Equalities(Dec.newDecWithPrec(-100, 0), Dec.newDecWithPrec(-100, 0),
                        false, false, true),
                new Equalities(Dec.newDecWithPrec(-1, 1), Dec.newDecWithPrec(-1, 1),
                        false, false, true),
                new Equalities(Dec.newDecWithPrec(3333, 3), Dec.newDecWithPrec(3333, 3),
                        false, false, true),

                new Equalities(Dec.newDecWithPrec(0, 0), Dec.newDecWithPrec(3333, 3),
                        false, true, false),
                new Equalities(Dec.newDecWithPrec(0, 0), Dec.newDecWithPrec(100, 0),
                        false, true, false),
                new Equalities(Dec.newDecWithPrec(-1, 0), Dec.newDecWithPrec(3333, 3),
                        false, true, false),
                new Equalities(Dec.newDecWithPrec(-1, 0), Dec.newDecWithPrec(100, 0),
                        false, true, false),
                new Equalities(Dec.newDecWithPrec(1111, 3), Dec.newDecWithPrec(100, 0),
                        false, true, false),
                new Equalities(Dec.newDecWithPrec(1111, 3), Dec.newDecWithPrec(3333, 3),
                        false, true, false),
                new Equalities(Dec.newDecWithPrec(-3333, 3), Dec.newDecWithPrec(-1111, 3),
                        false, true, false),

                new Equalities(Dec.newDecWithPrec(3333, 3), Dec.newDecWithPrec(0, 0),
                        true, false, false),
                new Equalities(Dec.newDecWithPrec(100, 0), Dec.newDecWithPrec(0, 0),
                        true, false, false),
                new Equalities(Dec.newDecWithPrec(3333, 3), Dec.newDecWithPrec(-1, 0),
                        true, false, false),
                new Equalities(Dec.newDecWithPrec(100, 0), Dec.newDecWithPrec(-1, 0),
                        true, false, false),
                new Equalities(Dec.newDecWithPrec(100, 0), Dec.newDecWithPrec(1111, 3),
                        true, false, false),
                new Equalities(Dec.newDecWithPrec(3333, 3), Dec.newDecWithPrec(1111, 3),
                        true, false, false),
                new Equalities(Dec.newDecWithPrec(-1111, 3), Dec.newDecWithPrec(-3333, 3),
                        true, false, false)
        ).forEach(tc -> {
            Assert.assertEquals(tc.gt, tc.d1.gt(tc.d2));
            Assert.assertEquals(tc.lt, tc.d1.lt(tc.d2));
            Assert.assertEquals(tc.eq, tc.d1.equal(tc.d2));
        });

    }

    @Test
    public void TestDecString() {
        Stream.of(
                new Pair<>(Dec.newDec(0), "0"),
                new Pair<>(Dec.newDec(1), "1"),
                new Pair<>(Dec.newDec(10), "10"),
                new Pair<>(Dec.newDec(12340), "12340"),
                new Pair<>(Dec.newDecWithPrec(12340, 4), "1.234"),
                new Pair<>(Dec.newDecWithPrec(12340, 5), "0.1234"),
                new Pair<>(Dec.newDecWithPrec(1234007806, 13), "0.0001234007806"),
                new Pair<>(Dec.newDecWithPrec(1009009009009009009L, 17), "10.09009009009009009")
        ).forEach(e -> {
            Assert.assertEquals(e.v, e.k.toString());
            Assert.assertEquals((e.k.equals(Dec.zeroDec()) ? "" : "-") + e.v,
                    e.k.neg().toString());
        });
    }

    @Test
    public void TestDecFloat64() {
        Stream.of(
                new Pair<>(Dec.newDec(0), 0.000000000000000000),
                new Pair<>(Dec.newDec(1), 1.000000000000000000),
                new Pair<>(Dec.newDec(10), 10.000000000000000000),
                new Pair<>(Dec.newDec(12340), 12340.000000000000000000),
                new Pair<>(Dec.newDecWithPrec(12340, 4), 1.234000000000000000),
                new Pair<>(Dec.newDecWithPrec(12340, 5), 0.123400000000000000),
                new Pair<>(Dec.newDecWithPrec(12340, 8), 0.000123400000000000),
                new Pair<>(Dec.newDecWithPrec(1009009009009009009L, 17), 10.090090090090090090)
        ).forEach(e -> Assert.assertEquals(Double.compare(e.v, e.k.Double()), 0));
    }


    @Test
    public void TestNewDecFromStr() {
        BigInteger largeBigInt = new BigInteger("3144605511029693144278234343371835", 10);
        Stream.of(
                new Pair<>("", true, Dec.zeroDec()),
                new Pair<>("0.-75", true, Dec.zeroDec()),
                new Pair<>("0", false, Dec.newDec(0)),
                new Pair<>("1", false, Dec.newDec(1)),
                new Pair<>("1.1", false, Dec.newDecWithPrec(11, 1)),
                new Pair<>("0.75", false, Dec.newDecWithPrec(75, 2)),
                new Pair<>("0.8", false, Dec.newDecWithPrec(8, 1)),
                new Pair<>("0.11111", false, Dec.newDecWithPrec(11111, 5)),
                new Pair<>("314460551102969.3144278234343371835",
                        true, Dec.newDec(3141203149163817869L)),
                new Pair<>("314460551102969314427823434337.1835718092488231350",
                        true, Dec.newDecWithPrec(largeBigInt, 4)),
                new Pair<>("314460551102969314427823434337.1835",
                        false, Dec.newDecWithPrec(largeBigInt, 4)),
                new Pair<>(".", true, Dec.zeroDec()),
                new Pair<>(".0", true, Dec.newDec(0)),
                new Pair<>("1.", true, Dec.newDec(1)),
                new Pair<>("foobar", true, Dec.zeroDec()),
                new Pair<>("0.foobar", true, Dec.zeroDec()),
                new Pair<>("0.foobar.", true, Dec.zeroDec()),
                new Pair<>(
                        "88888888888888888888888888888888888888888888888888888888888888888888844444440",
                        true, Dec.zeroDec())
        ).forEach(tc -> {
            RuntimeException err = null;
            Dec res = null;
            try {
                res = Dec.newDec(tc.k);
            } catch (RuntimeException e) {
                err = e;
            }

            if (tc.v) {
                Assert.assertNotNull(err);
            } else {
                Assert.assertNull(err);
                Assert.assertEquals(res, tc.s);
            }

            // negative tc
            err = null;
            res = null;
            try {
                res = Dec.newDec("-" + tc.k);
            } catch (RuntimeException e) {
                err = e;
            }
            if (tc.v) {
                Assert.assertNotNull(err);
            } else {
                Assert.assertNull(err);
                Dec exp = tc.s.mul(Dec.newDec(-1));
                Assert.assertEquals(res, exp);
            }
        });

    }


    public static class Pair<K, V, S> {

        private K k;
        private V v;
        private S s;

        Pair(K key, V value) {
            this.k = key;
            this.v = value;
        }

        Pair(K k, V v, S s) {
            this.k = k;
            this.v = v;
            this.s = s;
        }

    }

    public static class Arithmetic {

        private final Dec d1;
        private final Dec d2;
        private final Dec expMul;
        private final Dec expMulTruncate;
        private final Dec expQuo;
        private final Dec expQuoRoundUp;
        private final Dec expQuoTruncate;
        private final Dec expAdd;
        private final Dec expSub;

        Arithmetic(Dec d1, Dec d2, Dec expMul,
                   Dec expMulTruncate, Dec expQuo,
                   Dec expQuoRoundUp, Dec expQuoTruncate,
                   Dec expAdd, Dec expSub) {
            this.d1 = d1;
            this.d2 = d2;
            this.expMul = expMul;
            this.expMulTruncate = expMulTruncate;
            this.expQuo = expQuo;
            this.expQuoRoundUp = expQuoRoundUp;
            this.expQuoTruncate = expQuoTruncate;
            this.expAdd = expAdd;
            this.expSub = expSub;
        }

    }

    static class Equalities {

        private final Dec d1;
        private final Dec d2;
        private final boolean gt;
        private final boolean lt;
        private final boolean eq;

        Equalities(Dec d1, Dec d2, boolean gt, boolean lt, boolean eq) {
            this.d1 = d1;
            this.d2 = d2;
            this.gt = gt;
            this.lt = lt;
            this.eq = eq;
        }

    }
}
