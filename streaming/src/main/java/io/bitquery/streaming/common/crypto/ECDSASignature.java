package io.bitquery.streaming.common.crypto;

import io.bitquery.streaming.common.utils.ByteUtil;

import java.math.BigInteger;

public class ECDSASignature {
    public final BigInteger r;
    public final BigInteger s;

    public ECDSASignature(BigInteger r, BigInteger s) {
        this.r = r;
        this.s = s;
    }

    public byte[] toByteArray() {
        return ByteUtil.merge(
                ByteUtil.bigIntegerToBytes(this.r, 32),
                ByteUtil.bigIntegerToBytes(this.s, 32));
    }

    public boolean isCanonical() {
        return this.s.compareTo(Sign.HALF_CURVE_ORDER) <= 0;
    }

    public ECDSASignature toCanonicalised() {
        return !this.isCanonical() ? new ECDSASignature(this.r, Sign.CURVE.getN().subtract(this.s)) : this;
    }
}

