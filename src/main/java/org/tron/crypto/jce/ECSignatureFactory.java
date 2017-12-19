package org.tron.crypto.jce;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Signature;

public final class ECSignatureFactory {

    public static final String RAW_ALGORITHM = "NONEwithECDSA";

    private static final String rawAlgorithmAssertionMsg =
            "Assumed the JRE supports NONEwithECDSA signatures";

    private ECSignatureFactory() {
    }

    public static Signature getRawInstance() {
        try {
            return Signature.getInstance(RAW_ALGORITHM);
        } catch (NoSuchAlgorithmException ex) {
            throw new AssertionError(rawAlgorithmAssertionMsg, ex);
        }
    }

    public static Signature getRawInstance(final String provider) throws
            NoSuchProviderException {
        try {
            return Signature.getInstance(RAW_ALGORITHM, provider);
        } catch (NoSuchAlgorithmException ex) {
            throw new AssertionError(rawAlgorithmAssertionMsg, ex);
        }
    }

    public static Signature getRawInstance(final Provider provider) {
        try {
            return Signature.getInstance(RAW_ALGORITHM, provider);
        } catch (NoSuchAlgorithmException ex) {
            throw new AssertionError(rawAlgorithmAssertionMsg, ex);
        }
    }
}
