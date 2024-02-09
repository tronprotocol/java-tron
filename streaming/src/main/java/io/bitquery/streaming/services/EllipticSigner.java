package io.bitquery.streaming.services;

import io.bitquery.streaming.TracerConfig;
import io.bitquery.streaming.common.crypto.ECKeyPair;
import io.bitquery.streaming.common.utils.ByteArray;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

@Slf4j(topic = "streaming")
public class EllipticSigner {
    private ECKeyPair ecKeyPair;

    @Getter
    private String address;

    public EllipticSigner(TracerConfig config) {
        try {
            generateECKey(config);
        } catch (Exception e) {
            logger.error("EllipticSigner initialization failed", e);
            System.exit(1);
        }

        this.address = ecKeyPair.getAddress();
    }

    public byte[] sign(byte[] message) {
        return ecKeyPair.sign(message).toByteArray();
    }


    private void generateECKey(TracerConfig config) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        String pk = config.getEllipticSignerPrivateKeyHex();

        // If private key is not specified, generate random one.
        if (pk.isEmpty()) {
            this.ecKeyPair = ECKeyPair.createEcKeyPair();
            return;
        }


        this.ecKeyPair = ECKeyPair.create(ByteArray.fromHexString(pk));
    }
}
