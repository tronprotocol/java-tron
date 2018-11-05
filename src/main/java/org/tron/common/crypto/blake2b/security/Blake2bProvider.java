package org.tron.common.crypto.blake2b.security;

import java.security.Provider;

public class Blake2bProvider extends Provider {
    public Blake2bProvider() {
        super("BLAKE2B", 1.0, "BLAKE2B provider");

        put("MessageDigest.BLAKE2B-160", Blake2b160Digest.class.getName());
        put("MessageDigest.BLAKE2B-256", Blake2b256Digest.class.getName());
        put("MessageDigest.BLAKE2B-384", Blake2b384Digest.class.getName());
        put("MessageDigest.BLAKE2B-512", Blake2b512Digest.class.getName());
    }
}
