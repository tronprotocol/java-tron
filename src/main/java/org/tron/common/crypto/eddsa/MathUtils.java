/**
 * EdDSA-Java by str4d
 *
 * To the extent possible under law, the person who associated CC0 with
 * EdDSA-Java has waived all copyright and related or neighboring rights
 * to EdDSA-Java.
 *
 * You should have received a copy of the CC0 legalcode along with this
 * work. If not, see <https://creativecommons.org/publicdomain/zero/1.0/>.
 *
 */
package org.tron.common.crypto.eddsa;

import java.math.BigInteger;
import java.security.SecureRandom;
import org.tron.common.crypto.eddsa.math.Curve;
import org.tron.common.crypto.eddsa.math.Field;
import org.tron.common.crypto.eddsa.math.FieldElement;
import org.tron.common.crypto.eddsa.math.GroupElement;
import org.tron.common.crypto.eddsa.math.ed25519.Ed25519FieldElement;
import org.tron.common.crypto.eddsa.math.ed25519.Ed25519LittleEndianEncoding;
import org.tron.common.crypto.eddsa.spec.EdDSANamedCurveSpec;
import org.tron.common.crypto.eddsa.spec.EdDSANamedCurveTable;

/**
 * Utility class to help with calculations.
 */
public class MathUtils {
    private static final int[] exponents = {0, 26, 26 + 25, 2*26 + 25, 2*26 + 2*25, 3*26 + 2*25, 3*26 + 3*25, 4*26 + 3*25, 4*26 + 4*25, 5*26 + 4*25};
    private static final SecureRandom random = new SecureRandom();
    private static final EdDSANamedCurveSpec ed25519 = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);
    private static final Curve curve = ed25519.getCurve();
    private static final BigInteger d = new BigInteger("-121665").multiply(new BigInteger("121666").modInverse(getQ()));
    private static final BigInteger groupOrder = BigInteger.ONE.shiftLeft(252).add(new BigInteger("27742317777372353535851937790883648493"));

    /**
     * Gets q = 2^255 - 19 as BigInteger.
     */
    public static BigInteger getQ() {
        return new BigInteger("7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffed", 16);
    }

    /**
     * Gets group order = 2^252 + 27742317777372353535851937790883648493 as BigInteger.
     */
    public static BigInteger getGroupOrder() {
        return groupOrder;
    }

    /**
     * Gets the underlying finite field with q=2^255 - 19 elements.
     *
     * @return The finite field.
     */
    public static Field getField() {
        return new Field(
                256, // b
                Utils.hexToBytes("edffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff7f"), // q
                new Ed25519LittleEndianEncoding());
    }

    // region field element

    /**
     * Converts a 2^25.5 bit representation to a BigInteger.
     * <p>
     * Value: 2^exponents[0] * t[0] + 2^exponents[1] * t[1] + ... + 2^exponents[9] * t[9]
     *
     * @param t The 2^25.5 bit representation.
     * @return The BigInteger.
     */
    public static BigInteger toBigInteger(final int[] t) {
        BigInteger b = BigInteger.ZERO;
        for (int i=0; i<10; i++) {
            b = b.add(BigInteger.ONE.multiply(BigInteger.valueOf(t[i])).shiftLeft(exponents[i]));
        }

        return b;
    }

    /**
     * Converts a 2^8 bit representation to a BigInteger.
     * <p>
     * Value: bytes[0] + 2^8 * bytes[1] + ...
     *
     * @param bytes The 2^8 bit representation.
     * @return The BigInteger.
     */
    public static BigInteger toBigInteger(final byte[] bytes) {
        BigInteger b = BigInteger.ZERO;
        for (int i=0; i<bytes.length; i++) {
            b = b.add(BigInteger.ONE.multiply(BigInteger.valueOf(bytes[i] & 0xff)).shiftLeft(i * 8));
        }

        return b;
    }

    /**
     * Converts a field element to a BigInteger.
     *
     * @param f The field element.
     * @return The BigInteger.
     */
    public static BigInteger toBigInteger(final FieldElement f) {
        return toBigInteger(f.toByteArray());
    }

    /**
     * Converts a BigInteger to a field element.
     *
     * @param b The BigInteger.
     * @return The field element.
     */
    public static FieldElement toFieldElement(final BigInteger b) {
        return getField().getEncoding().decode(toByteArray(b));
    }

    /**
     * Converts a BigInteger to a little endian 32 byte representation.
     *
     * @param b The BigInteger.
     * @return The 32 byte representation.
     */
    public static byte[] toByteArray(final BigInteger b) {
        if (b.compareTo(BigInteger.ONE.shiftLeft(256)) >= 0) {
            throw new RuntimeException("only numbers < 2^256 are allowed");
        }
        final byte[] bytes = new byte[32];
        final byte[] original = b.toByteArray();

        // Although b < 2^256, original can have length > 32 with some bytes set to 0.
        final int offset = original.length > 32? original.length - 32 : 0;
        for (int i=0; i<original.length - offset; i++) {
            bytes[original.length - i - offset - 1] = original[i + offset];
        }

        return bytes;
    }

    /**
     * Reduces an integer in 2^8 bit representation modulo the group order and returns the result.
     *
     * @param bytes The integer in 2^8 bit representation.
     * @return The mod group order reduced integer.
     */
    public static byte[] reduceModGroupOrder(final byte[] bytes) {
        final BigInteger b = toBigInteger(bytes).mod(groupOrder);
        return toByteArray(b);
    }

    /**
     * Calculates (a * b + c) mod group order and returns the result.
     * <p>
     * a, b and c are given in 2^8 bit representation.
     *
     * @param a The first integer.
     * @param b The second integer.
     * @param c The third integer.
     * @return The mod group order reduced result.
     */
    public static byte[] multiplyAndAddModGroupOrder(final byte[] a, final byte[] b, final byte[] c) {
        final BigInteger result = toBigInteger(a).multiply(toBigInteger(b)).add(toBigInteger(c)).mod(groupOrder);
        return toByteArray(result);
    }

    public static byte[] getRandomByteArray(final int length) {
        final byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    /**
     * Gets a random field element where |t[i]| <= 2^24 for 0 <= i <= 9.
     *
     * @return The field element.
     */
    public static FieldElement getRandomFieldElement() {
        final int[] t = new int[10];
        for (int j=0; j<10; j++) {
            t[j] = random.nextInt(1 << 25) - (1 << 24);
        }
        return new Ed25519FieldElement(getField(), t);
    }

    // endregion

    // region group element

    /**
     * Gets a random group element in P3 representation.
     *
     * @return The group element.
     */
    public static GroupElement getRandomGroupElement() { return getRandomGroupElement(false); }

    /**
     * Gets a random group element in P3 representation, with precmp and dblPrecmp populated.
     *
     * @return The group element.
     */
    public static GroupElement getRandomGroupElement(boolean precompute) {
        final byte[] bytes = new byte[32];
        while (true) {
            try {
                random.nextBytes(bytes);
                return new GroupElement(curve, bytes, precompute);
            } catch (IllegalArgumentException e) {
                // Will fail in about 87.5%, so try again.
            }
        }
    }

    /**
     * Creates a group element from a byte array.
     * <p>
     * Bit 0 to 254 are the affine y-coordinate, bit 255 is the sign of the affine x-coordinate.
     *
     * @param bytes the byte array.
     * @return The group element.
     */
    public static GroupElement toGroupElement(final byte[] bytes) {
        final boolean shouldBeNegative = (bytes[31] >> 7) != 0;
        bytes[31] &= 0x7f;
        final BigInteger y = MathUtils.toBigInteger(bytes);

        // x = sign(x) * sqrt((y^2 - 1) / (d * y^2 + 1))
        final BigInteger u = y.multiply(y).subtract(BigInteger.ONE).mod(getQ());
        final BigInteger v = d.multiply(y).multiply(y).add(BigInteger.ONE).mod(getQ());
        final BigInteger tmp = u.multiply(v.pow(7)).modPow(BigInteger.ONE.shiftLeft(252).subtract(new BigInteger("3")), getQ()).mod(getQ());
        BigInteger x = tmp.multiply(u).multiply(v.pow(3)).mod(getQ());
        if (!v.multiply(x).multiply(x).subtract(u).mod(getQ()).equals(BigInteger.ZERO)) {
            if (!v.multiply(x).multiply(x).add(u).mod(getQ()).equals(BigInteger.ZERO)) {
                throw new IllegalArgumentException("not a valid GroupElement");
            }
            x = x.multiply(toBigInteger(curve.getI())).mod(getQ());
        }
        final boolean isNegative = x.mod(new BigInteger("2")).equals(BigInteger.ONE);
        if ((shouldBeNegative && !isNegative) || (!shouldBeNegative && isNegative)) {
            x = x.negate().mod(getQ());
        }

        return GroupElement.p3(curve, toFieldElement(x), toFieldElement(y), getField().ONE, toFieldElement(x.multiply(y).mod(getQ())));
    }

    /**
     * Converts a group element from one representation to another.
     * This method is a helper used to test various methods in GroupElement.
     *
     * @param g The group element.
     * @param repr The desired representation.
     * @return The same group element in the new representation.
     */
    public static GroupElement toRepresentation(final GroupElement g, final GroupElement.Representation repr) {
        BigInteger x;
        BigInteger y;
        final BigInteger gX = toBigInteger(g.getX().toByteArray());
        final BigInteger gY = toBigInteger(g.getY().toByteArray());
        final BigInteger gZ = toBigInteger(g.getZ().toByteArray());
        final BigInteger gT = null == g.getT()? null : toBigInteger(g.getT().toByteArray());

        // Switch to affine coordinates.
        switch (g.getRepresentation()) {
            case P2:
            case P3:
            case P3PrecomputedDouble:
                x = gX.multiply(gZ.modInverse(getQ())).mod(getQ());
                y = gY.multiply(gZ.modInverse(getQ())).mod(getQ());
                break;
            case P1P1:
                x = gX.multiply(gZ.modInverse(getQ())).mod(getQ());
                y = gY.multiply(gT.modInverse(getQ())).mod(getQ());
                break;
            case CACHED:
                x = gX.subtract(gY).multiply(gZ.multiply(new BigInteger("2")).modInverse(getQ())).mod(getQ());
                y = gX.add(gY).multiply(gZ.multiply(new BigInteger("2")).modInverse(getQ())).mod(getQ());
                break;
            case PRECOMP:
                x = gX.subtract(gY).multiply(new BigInteger("2").modInverse(getQ())).mod(getQ());
                y = gX.add(gY).multiply(new BigInteger("2").modInverse(getQ())).mod(getQ());
                break;
            default:
                throw new UnsupportedOperationException();
        }

        // Now back to the desired representation.
        switch (repr) {
            case P2:
                return GroupElement.p2(
                        curve,
                        toFieldElement(x),
                        toFieldElement(y),
                        getField().ONE);
            case P3:
                return GroupElement.p3(
                        curve,
                        toFieldElement(x),
                        toFieldElement(y),
                        getField().ONE,
                        toFieldElement(x.multiply(y).mod(getQ())), false);
            case P3PrecomputedDouble:
                return GroupElement.p3(
                        curve,
                        toFieldElement(x),
                        toFieldElement(y),
                        getField().ONE,
                        toFieldElement(x.multiply(y).mod(getQ())), true);
            case P1P1:
                return GroupElement.p1p1(
                        curve,
                        toFieldElement(x),
                        toFieldElement(y),
                        getField().ONE,
                        getField().ONE);
            case CACHED:
                return GroupElement.cached(
                        curve,
                        toFieldElement(y.add(x).mod(getQ())),
                        toFieldElement(y.subtract(x).mod(getQ())),
                        getField().ONE,
                        toFieldElement(d.multiply(new BigInteger("2")).multiply(x).multiply(y).mod(getQ())));
            case PRECOMP:
                return GroupElement.precomp(
                        curve,
                        toFieldElement(y.add(x).mod(getQ())),
                        toFieldElement(y.subtract(x).mod(getQ())),
                        toFieldElement(d.multiply(new BigInteger("2")).multiply(x).multiply(y).mod(getQ())));
            default:
                throw new UnsupportedOperationException();
        }
    }

    /**
     * Adds two group elements and returns the result in P3 representation.
     * It uses BigInteger arithmetic and the affine representation.
     * This method is a helper used to test the projective group addition formulas in GroupElement.
     *
     * @param g1 The first group element.
     * @param g2 The second group element.
     * @return The result of the addition.
     */
    public static GroupElement addGroupElements(final GroupElement g1, final GroupElement g2) {
        // Relying on a special representation of the group elements.
        if ((g1.getRepresentation() != GroupElement.Representation.P2 && g1.getRepresentation() != GroupElement.Representation.P3) ||
            (g2.getRepresentation() != GroupElement.Representation.P2 && g2.getRepresentation() != GroupElement.Representation.P3)) {
            throw new IllegalArgumentException("g1 and g2 must have representation P2 or P3");
        }

        // Projective coordinates
        final BigInteger g1X = toBigInteger(g1.getX().toByteArray());
        final BigInteger g1Y = toBigInteger(g1.getY().toByteArray());
        final BigInteger g1Z = toBigInteger(g1.getZ().toByteArray());
        final BigInteger g2X = toBigInteger(g2.getX().toByteArray());
        final BigInteger g2Y = toBigInteger(g2.getY().toByteArray());
        final BigInteger g2Z = toBigInteger(g2.getZ().toByteArray());

        // Affine coordinates
        final BigInteger g1x = g1X.multiply(g1Z.modInverse(getQ())).mod(getQ());
        final BigInteger g1y = g1Y.multiply(g1Z.modInverse(getQ())).mod(getQ());
        final BigInteger g2x = g2X.multiply(g2Z.modInverse(getQ())).mod(getQ());
        final BigInteger g2y = g2Y.multiply(g2Z.modInverse(getQ())).mod(getQ());

        // Addition formula for affine coordinates. The formula is complete in our case.
        //
        // (x3, y3) = (x1, y1) + (x2, y2) where
        //
        // x3 = (x1 * y2 + x2 * y1) / (1 + d * x1 * x2 * y1 * y2) and
        // y3 = (x1 * x2 + y1 * y2) / (1 - d * x1 * x2 * y1 * y2) and
        // d = -121665/121666
        BigInteger dx1x2y1y2 = d.multiply(g1x).multiply(g2x).multiply(g1y).multiply(g2y).mod(getQ());
        BigInteger x3 = g1x.multiply(g2y).add(g2x.multiply(g1y))
                .multiply(BigInteger.ONE.add(dx1x2y1y2).modInverse(getQ())).mod(getQ());
        BigInteger y3 = g1x.multiply(g2x).add(g1y.multiply(g2y))
                .multiply(BigInteger.ONE.subtract(dx1x2y1y2).modInverse(getQ())).mod(getQ());
        BigInteger t3 = x3.multiply(y3).mod(getQ());

        return GroupElement.p3(g1.getCurve(), toFieldElement(x3), toFieldElement(y3), getField().ONE, toFieldElement(t3));
    }

    /**
     * Doubles a group element and returns the result in P3 representation.
     * It uses BigInteger arithmetic and the affine representation.
     * This method is a helper used to test the projective group doubling formula in GroupElement.
     *
     * @param g The group element.
     * @return g+g.
     */
    public static GroupElement doubleGroupElement(final GroupElement g) {
        return addGroupElements(g, g);
    }

    /**
     * Scalar multiply the group element by the field element.
     *
     * @param g The group element.
     * @param f The field element.
     * @return The resulting group element.
     */
    public static GroupElement scalarMultiplyGroupElement(final GroupElement g, final FieldElement f) {
        final byte[] bytes = f.toByteArray();
        GroupElement h = curve.getZero(GroupElement.Representation.P3);
        for (int i=254; i>=0; i--) {
            h = doubleGroupElement(h);
            if (Utils.bit(bytes, i) == 1) {
                h = addGroupElements(h, g);
            }
        }

        return h;
    }

    /**
     * Calculates f1 * g1 + f2 * g2.
     *
     * @param g1 The first group element.
     * @param f1 The first multiplier.
     * @param g2 The second group element.
     * @param f2 The second multiplier.
     * @return The resulting group element.
     */
    public static GroupElement doubleScalarMultiplyGroupElements(
            final GroupElement g1,
            final FieldElement f1,
            final GroupElement g2,
            final FieldElement f2) {
        final GroupElement h1 = scalarMultiplyGroupElement(g1, f1);
        final GroupElement h2 = scalarMultiplyGroupElement(g2, f2);
        return addGroupElements(h1, h2);
    }

    /**
     * Negates a group element.
     *
     * @param g The group element.
     * @return The negated group element.
     */
    public static GroupElement negateGroupElement(final GroupElement g) {
        if (g.getRepresentation() != GroupElement.Representation.P3) {
            throw new IllegalArgumentException("g must have representation P3");
        }

        return GroupElement.p3(g.getCurve(), g.getX().negate(), g.getY(), g.getZ(), g.getT().negate());
    }
    // End TODO BR: Remove when finished!
}
