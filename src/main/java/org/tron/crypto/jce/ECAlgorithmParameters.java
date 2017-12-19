package org.tron.crypto.jce;

import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.NoSuchAlgorithmException;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidParameterSpecException;

public final class ECAlgorithmParameters {

    public static final String ALGORITHM = "EC";
    public static final String CURVE_NAME = "secp256k1";

    private ECAlgorithmParameters() {
    }

    private static class Holder {
        private static final AlgorithmParameters INSTANCE;

        private static final ECGenParameterSpec SECP256K1_CURVE
                = new ECGenParameterSpec(CURVE_NAME);

        static {
            try {
                INSTANCE = AlgorithmParameters.getInstance(ALGORITHM);
                INSTANCE.init(SECP256K1_CURVE);
            } catch (NoSuchAlgorithmException ex) {
                throw new AssertionError(
                        "Assumed the JRE supports EC algorithm params", ex);
            } catch (InvalidParameterSpecException ex) {
                throw new AssertionError(
                        "Assumed correct key spec statically", ex);
            }
        }
    }

    public static ECParameterSpec getParameterSpec() {
        try {
            return Holder.INSTANCE.getParameterSpec(ECParameterSpec.class);
        } catch (InvalidParameterSpecException ex) {
            throw new AssertionError(
                    "Assumed correct key spec statically", ex);
        }
    }

    public static byte[] getASN1Encoding() {
        try {
            return Holder.INSTANCE.getEncoded();
        } catch (IOException ex) {
            throw new AssertionError(
                    "Assumed algo params has been initialized", ex);
        }
    }
}
