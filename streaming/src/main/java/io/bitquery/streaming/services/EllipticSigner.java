package io.bitquery.streaming.services;

import io.bitquery.streaming.TracerConfig;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;

public class EllipticSigner {
    private final TracerConfig config;
    private ECKey eckey;

    public EllipticSigner(TracerConfig config) {
        this.config = config;

        generateECKey();
    }

    public ECKey.ECDSASignature sign(byte[] message){
       return this.eckey.sign(message);
    }

    public String getAddress() {
        return ByteArray.toHexString(this.eckey.getAddress()).substring(2);
    }

    private void generateECKey() {
        String pk = config.getEllipticSignerPrivateKeyHex();

        // If private key is not specified, generate random one.
        if (pk.isEmpty()) {
            this.eckey = new ECKey();
            return;
        }

        this.eckey = new ECKey(ByteArray.fromHexString(pk), true);
    }

}
