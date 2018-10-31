package org.tron.common.crypto.blake2b.security;

import org.tron.common.crypto.blake2b.Blake2b;

public class Blake2b384Digest extends AbstractDigest implements Cloneable {
    private static final int DIGEST_SIZE = 384;

    public Blake2b384Digest() {
        super(Blake2b.BLAKE2_B_384, new Blake2b(DIGEST_SIZE));
    }

    public Object clone() throws CloneNotSupportedException {
        final Blake2b384Digest clone = (Blake2b384Digest) super.clone();

        clone.instance = new Blake2b(instance);

        return clone;
    }


}
