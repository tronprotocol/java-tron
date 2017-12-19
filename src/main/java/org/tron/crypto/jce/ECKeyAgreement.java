package org.tron.crypto.jce;

import javax.crypto.KeyAgreement;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;

public final class ECKeyAgreement {

    public static final String ALGORITHM = "ECDH";

    private static final String algorithmAssertionMsg =
            "Assumed the JRE supports EC key agreement";

    private ECKeyAgreement() {
    }

    public static KeyAgreement getInstance() {
        try {
            return KeyAgreement.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException ex) {
            throw new AssertionError(algorithmAssertionMsg, ex);
        }
    }

    public static KeyAgreement getInstance(final String provider) throws
            NoSuchProviderException {
        try {
            return KeyAgreement.getInstance(ALGORITHM, provider);
        } catch (NoSuchAlgorithmException ex) {
            throw new AssertionError(algorithmAssertionMsg, ex);
        }
    }

    public static KeyAgreement getInstance(final Provider provider) {
        try {
            return KeyAgreement.getInstance(ALGORITHM, provider);
        } catch (NoSuchAlgorithmException ex) {
            throw new AssertionError(algorithmAssertionMsg, ex);
        }
    }
}
