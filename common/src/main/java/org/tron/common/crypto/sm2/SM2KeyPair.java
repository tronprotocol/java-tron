package org.tron.common.crypto.sm2;

/**
 * key pair of SM2
 *
 */
public class SM2KeyPair {
    private byte[] publickey;
    private byte[] privatekey;

    public SM2KeyPair(byte[] publickey, byte[] privatekey) {
        this.publickey = publickey;
        this.privatekey = privatekey;
    }

    public byte[] getPublickey() {
        return publickey;
    }

    public byte[] getPrivatekey() {
        return privatekey;
    }

}
