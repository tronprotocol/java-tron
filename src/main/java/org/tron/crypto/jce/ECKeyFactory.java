package org.tron.crypto.jce;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;

public final class ECKeyFactory {

    public static final String ALGORITHM = "EC";

    private static final String algorithmAssertionMsg =
            "Assumed the JRE supports EC key factories";

    private ECKeyFactory() {
    }

    private static class Holder {
        private static final KeyFactory INSTANCE;

        static {
            try {
                INSTANCE = KeyFactory.getInstance(ALGORITHM);
            } catch (NoSuchAlgorithmException ex) {
                throw new AssertionError(algorithmAssertionMsg, ex);
            }
        }
    }

    public static KeyFactory getInstance() {
        return Holder.INSTANCE;
    }

    public static KeyFactory getInstance(final String provider) throws
            NoSuchProviderException {
        try {
            return KeyFactory.getInstance(ALGORITHM, provider);
        } catch (NoSuchAlgorithmException ex) {
            throw new AssertionError(algorithmAssertionMsg, ex);
        }
    }

    public static KeyFactory getInstance(final Provider provider) {
        try {
            return KeyFactory.getInstance(ALGORITHM, provider);
        } catch (NoSuchAlgorithmException ex) {
            throw new AssertionError(algorithmAssertionMsg, ex);
        }
    }
}
