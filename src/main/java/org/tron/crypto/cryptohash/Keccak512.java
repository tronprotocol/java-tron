package org.tron.crypto.cryptohash;

public class Keccak512 extends KeccakCore {

    /**
     * Create the engine.
     */
    public Keccak512() {
        super("tron-keccak-512");
    }

    public Digest copy() {
        return copyState(new Keccak512());
    }

    public int engineGetDigestLength() {
        return 64;
    }

    @Override
    protected byte[] engineDigest() {
        return null;
    }

    @Override
    protected void engineUpdate(byte input) {
    }

    @Override
    protected void engineUpdate(byte[] input, int offset, int len) {
    }
}
