package org.tron.crypto.jce;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.Provider;
import java.security.Security;

public final class TronCastleProvider {

    private static class Holder {
        private static final Provider INSTANCE;

        static {
            Provider p = Security.getProvider("SC");

            INSTANCE = (p != null) ? p : new BouncyCastleProvider();

            INSTANCE.put("MessageDigest.TRON-KECCAK-256", "org.tron.crypto" +
                    ".cryptohash.Keccak256");
            INSTANCE.put("MessageDigest.TRON-KECCAK-512", "org.tron.crypto" +
                    ".cryptohash.Keccak512");
        }
    }

    public static Provider getInstance() {
        return Holder.INSTANCE;
    }
}
