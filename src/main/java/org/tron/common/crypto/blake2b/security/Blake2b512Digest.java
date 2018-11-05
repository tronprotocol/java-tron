package org.tron.common.crypto.blake2b.security;

import org.tron.common.crypto.blake2b.Blake2b;

public class Blake2b512Digest extends AbstractDigest implements Cloneable {
    private static final int DIGEST_SIZE = 512;

    public Blake2b512Digest() {
        super(Blake2b.BLAKE2_B_512, new Blake2b(DIGEST_SIZE));
    }

    public Object clone() throws CloneNotSupportedException {
        final Blake2b512Digest clone = (Blake2b512Digest) super.clone();

        clone.instance = new Blake2b(instance);

        return clone;
    }


}
