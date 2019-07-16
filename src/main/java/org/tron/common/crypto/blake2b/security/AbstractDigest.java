package org.tron.common.crypto.blake2b.security;

import java.security.MessageDigest;
import org.tron.common.crypto.blake2b.Blake2b;

abstract class AbstractDigest extends MessageDigest {
    Blake2b instance;

    AbstractDigest(final String name, final Blake2b instance) {
        super(name);
        this.instance = instance;
    }

    protected void engineUpdate(final byte b) {
        instance.update(b);
    }

    protected void engineUpdate(byte b[], int offset, int length) {
        instance.update(b, offset, length);
    }

    protected byte[] engineDigest() {
        final byte[] ret = new byte[instance.getDigestSize()];

        instance.digest(ret, 0);

        return ret;
    }

    protected void engineReset() {
        instance.clearKey();
        instance.clearSalt();
        instance.reset();
    }
}
