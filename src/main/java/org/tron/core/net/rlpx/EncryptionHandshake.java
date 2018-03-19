/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.core.net.rlpx;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import org.ethereum.crypto.ECIESCoder;
import org.ethereum.crypto.ECKey;
import org.ethereum.util.ByteUtil;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.digests.KeccakDigest;
import org.spongycastle.math.ec.ECPoint;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import static org.ethereum.crypto.HashUtil.sha3;

/**
 * Created by devrandom on 2015-04-08.
 */
public class EncryptionHandshake {
    public static final int NONCE_SIZE = 32;
    public static final int MAC_SIZE = 256;
    public static final int SECRET_SIZE = 32;
    private SecureRandom random = new SecureRandom();
    private boolean isInitiator;
    private ECKey ephemeralKey;
    private ECPoint remotePublicKey;
    private ECPoint remoteEphemeralKey;
    private byte[] initiatorNonce;
    private byte[] responderNonce;
    private Secrets secrets;

    public EncryptionHandshake(ECPoint remotePublicKey) {
        this.remotePublicKey = remotePublicKey;
        ephemeralKey = new ECKey(random);
        initiatorNonce = new byte[NONCE_SIZE];
        random.nextBytes(initiatorNonce);
        isInitiator = true;
    }

    EncryptionHandshake(ECPoint remotePublicKey, ECKey ephemeralKey, byte[] initiatorNonce, byte[] responderNonce, boolean isInitiator) {
        this.remotePublicKey = remotePublicKey;
        this.ephemeralKey = ephemeralKey;
        this.initiatorNonce = initiatorNonce;
        this.responderNonce = responderNonce;
        this.isInitiator = isInitiator;
    }

    public EncryptionHandshake() {
        ephemeralKey = new ECKey(random);
        responderNonce = new byte[NONCE_SIZE];
        random.nextBytes(responderNonce);
        isInitiator = false;
    }

    /**
     * Create a handshake auth message defined by EIP-8
     *
     * @param key our private key
     */
    public AuthInitiateMessageV4 createAuthInitiateV4(ECKey key) {
        AuthInitiateMessageV4 message = new AuthInitiateMessageV4();

        BigInteger secretScalar = key.keyAgreement(remotePublicKey);
        byte[] token = ByteUtil.bigIntegerToBytes(secretScalar, NONCE_SIZE);

        byte[] nonce = initiatorNonce;
        byte[] signed = xor(token, nonce);
        message.signature = ephemeralKey.sign(signed);
        message.publicKey = key.getPubKeyPoint();
        message.nonce = initiatorNonce;
        return message;
    }

    public byte[] encryptAuthInitiateV4(AuthInitiateMessageV4 message) {

        byte[] msg = message.encode();
        byte[] padded = padEip8(msg);

        return encryptAuthEIP8(padded);
    }

    public AuthInitiateMessageV4 decryptAuthInitiateV4(byte[] in, ECKey myKey) throws InvalidCipherTextException {
        try {

            byte[] prefix = new byte[2];
            System.arraycopy(in, 0, prefix, 0, 2);
            short size = ByteUtil.bigEndianToShort(prefix, 0);
            byte[] ciphertext = new byte[size];
            System.arraycopy(in, 2, ciphertext, 0, size);

            byte[] plaintext = ECIESCoder.decrypt(myKey.getPrivKey(), ciphertext, prefix);

            return AuthInitiateMessageV4.decode(plaintext);
        } catch (InvalidCipherTextException e) {
            throw e;
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public byte[] encryptAuthResponseV4(AuthResponseMessageV4 message) {

        byte[] msg = message.encode();
        byte[] padded = padEip8(msg);

        return encryptAuthEIP8(padded);
    }

    public AuthResponseMessageV4 decryptAuthResponseV4(byte[] in, ECKey myKey) {
        try {

            byte[] prefix = new byte[2];
            System.arraycopy(in, 0, prefix, 0, 2);
            short size = ByteUtil.bigEndianToShort(prefix, 0);
            byte[] ciphertext = new byte[size];
            System.arraycopy(in, 2, ciphertext, 0, size);

            byte[] plaintext = ECIESCoder.decrypt(myKey.getPrivKey(), ciphertext, prefix);

            return AuthResponseMessageV4.decode(plaintext);
        } catch (IOException | InvalidCipherTextException e) {
            throw Throwables.propagate(e);
        }
    }

    AuthResponseMessageV4 makeAuthInitiateV4(AuthInitiateMessageV4 initiate, ECKey key) {
        initiatorNonce = initiate.nonce;
        remotePublicKey = initiate.publicKey;

        BigInteger secretScalar = key.keyAgreement(remotePublicKey);

        byte[] token = ByteUtil.bigIntegerToBytes(secretScalar, NONCE_SIZE);
        byte[] signed = xor(token, initiatorNonce);

        ECKey ephemeral = ECKey.recoverFromSignature(recIdFromSignatureV(initiate.signature.v),
                initiate.signature, signed);
        if (ephemeral == null) {
            throw new RuntimeException("failed to recover signatue from message");
        }
        remoteEphemeralKey = ephemeral.getPubKeyPoint();
        AuthResponseMessageV4 response = new AuthResponseMessageV4();
        response.ephemeralPublicKey = ephemeralKey.getPubKeyPoint();
        response.nonce = responderNonce;
        return response;
    }

    public AuthResponseMessageV4 handleAuthResponseV4(ECKey myKey, byte[] initiatePacket, byte[] responsePacket) {
        AuthResponseMessageV4 response = decryptAuthResponseV4(responsePacket, myKey);
        remoteEphemeralKey = response.ephemeralPublicKey;
        responderNonce = response.nonce;
        agreeSecret(initiatePacket, responsePacket);
        return response;
    }

    byte[] encryptAuthEIP8(byte[] msg) {

        short size = (short) (msg.length + ECIESCoder.getOverhead());
        byte[] prefix = ByteUtil.shortToBytes(size);
        byte[] encrypted = ECIESCoder.encrypt(remotePublicKey, msg, prefix);

        byte[] out = new byte[prefix.length + encrypted.length];
        int offset = 0;
        System.arraycopy(prefix, 0, out, offset, prefix.length);
        offset += prefix.length;
        System.arraycopy(encrypted, 0, out, offset, encrypted.length);

        return out;
    }

    /**
     * Create a handshake auth message
     *
     * @param token previous token if we had a previous session
     * @param key our private key
     */
    public AuthInitiateMessage createAuthInitiate(@Nullable byte[] token, ECKey key) {
        AuthInitiateMessage message = new AuthInitiateMessage();
        boolean isToken;
        if (token == null) {
            isToken = false;
            BigInteger secretScalar = key.keyAgreement(remotePublicKey);
            token = ByteUtil.bigIntegerToBytes(secretScalar, NONCE_SIZE);
        } else {
            isToken = true;
        }

        byte[] nonce = initiatorNonce;
        byte[] signed = xor(token, nonce);
        message.signature = ephemeralKey.sign(signed);
        message.isTokenUsed = isToken;
        message.ephemeralPublicHash = sha3(ephemeralKey.getPubKey(), 1, 64);
        message.publicKey = key.getPubKeyPoint();
        message.nonce = initiatorNonce;
        return message;
    }

    private static byte[] xor(byte[] b1, byte[] b2) {
        Preconditions.checkArgument(b1.length == b2.length);
        byte[] out = new byte[b1.length];
        for (int i = 0; i < b1.length; i++) {
            out[i] = (byte) (b1[i] ^ b2[i]);
        }
        return out;
    }

    public byte[] encryptAuthMessage(AuthInitiateMessage message) {
        return ECIESCoder.encrypt(remotePublicKey, message.encode());
    }

    public byte[] encryptAuthResponse(AuthResponseMessage message) {
        return ECIESCoder.encrypt(remotePublicKey, message.encode());
    }

    public AuthResponseMessage decryptAuthResponse(byte[] ciphertext, ECKey myKey) {
        try {
            byte[] plaintext = ECIESCoder.decrypt(myKey.getPrivKey(), ciphertext);
            return AuthResponseMessage.decode(plaintext);
        } catch (IOException | InvalidCipherTextException e) {
            throw Throwables.propagate(e);
        }
    }

    public AuthInitiateMessage decryptAuthInitiate(byte[] ciphertext, ECKey myKey) throws InvalidCipherTextException {
        try {
            byte[] plaintext = ECIESCoder.decrypt(myKey.getPrivKey(), ciphertext);
            return AuthInitiateMessage.decode(plaintext);
        } catch (InvalidCipherTextException e) {
            throw e;
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public AuthResponseMessage handleAuthResponse(ECKey myKey, byte[] initiatePacket, byte[] responsePacket) {
        AuthResponseMessage response = decryptAuthResponse(responsePacket, myKey);
        remoteEphemeralKey = response.ephemeralPublicKey;
        responderNonce = response.nonce;
        agreeSecret(initiatePacket, responsePacket);
        return response;
    }

    void agreeSecret(byte[] initiatePacket, byte[] responsePacket) {
        BigInteger secretScalar = ephemeralKey.keyAgreement(remoteEphemeralKey);
        byte[] agreedSecret = ByteUtil.bigIntegerToBytes(secretScalar, SECRET_SIZE);
        byte[] sharedSecret = sha3(agreedSecret, sha3(responderNonce, initiatorNonce));
        byte[] aesSecret = sha3(agreedSecret, sharedSecret);
        secrets = new Secrets();
        secrets.aes = aesSecret;
        secrets.mac = sha3(agreedSecret, aesSecret);
        secrets.token = sha3(sharedSecret);
//        System.out.println("mac " + Hex.toHexString(secrets.mac));
//        System.out.println("aes " + Hex.toHexString(secrets.aes));
//        System.out.println("shared " + Hex.toHexString(sharedSecret));
//        System.out.println("ecdhe " + Hex.toHexString(agreedSecret));

        KeccakDigest mac1 = new KeccakDigest(MAC_SIZE);
        mac1.update(xor(secrets.mac, responderNonce), 0, secrets.mac.length);
        byte[] buf = new byte[32];
        new KeccakDigest(mac1).doFinal(buf, 0);
        mac1.update(initiatePacket, 0, initiatePacket.length);
        new KeccakDigest(mac1).doFinal(buf, 0);
        KeccakDigest mac2 = new KeccakDigest(MAC_SIZE);
        mac2.update(xor(secrets.mac, initiatorNonce), 0, secrets.mac.length);
        new KeccakDigest(mac2).doFinal(buf, 0);
        mac2.update(responsePacket, 0, responsePacket.length);
        new KeccakDigest(mac2).doFinal(buf, 0);
        if (isInitiator) {
            secrets.egressMac = mac1;
            secrets.ingressMac = mac2;
        } else {
            secrets.egressMac = mac2;
            secrets.ingressMac = mac1;
        }
    }

    public byte[] handleAuthInitiate(byte[] initiatePacket, ECKey key) throws InvalidCipherTextException {
        AuthResponseMessage response = makeAuthInitiate(initiatePacket, key);
        byte[] responsePacket = encryptAuthResponse(response);
        agreeSecret(initiatePacket, responsePacket);
        return responsePacket;
    }

    AuthResponseMessage makeAuthInitiate(byte[] initiatePacket, ECKey key) throws InvalidCipherTextException {
        AuthInitiateMessage initiate = decryptAuthInitiate(initiatePacket, key);
        return makeAuthInitiate(initiate, key);
    }

    AuthResponseMessage makeAuthInitiate(AuthInitiateMessage initiate, ECKey key) {
        initiatorNonce = initiate.nonce;
        remotePublicKey = initiate.publicKey;
        BigInteger secretScalar = key.keyAgreement(remotePublicKey);
        byte[] token = ByteUtil.bigIntegerToBytes(secretScalar, NONCE_SIZE);
        byte[] signed = xor(token, initiatorNonce);

        ECKey ephemeral = ECKey.recoverFromSignature(recIdFromSignatureV(initiate.signature.v),
                initiate.signature, signed);
        if (ephemeral == null) {
            throw new RuntimeException("failed to recover signatue from message");
        }
        remoteEphemeralKey = ephemeral.getPubKeyPoint();
        AuthResponseMessage response = new AuthResponseMessage();
        response.isTokenUsed = initiate.isTokenUsed;
        response.ephemeralPublicKey = ephemeralKey.getPubKeyPoint();
        response.nonce = responderNonce;
        return response;
    }

    /**
     * Pads messages with junk data,
     * pad data length is random value satisfying 100 < len < 300.
     * It's necessary to make messages described by EIP-8 distinguishable from pre-EIP-8 msgs
     *
     * @param msg message to pad
     * @return padded message
     */
    private byte[] padEip8(byte[] msg) {

        byte[] paddedMessage = new byte[msg.length + random.nextInt(200) + 100];
        random.nextBytes(paddedMessage);
        System.arraycopy(msg, 0, paddedMessage, 0, msg.length);

        return paddedMessage;
    }

    static public byte recIdFromSignatureV(int v) {
        if (v >= 31) {
            // compressed
            v -= 4;
        }
        return (byte)(v - 27);
    }

    public Secrets getSecrets() {
        return secrets;
    }

    public ECPoint getRemotePublicKey() {
        return remotePublicKey;
    }

    public static class Secrets {
        byte[] aes;
        byte[] mac;
        byte[] token;
        KeccakDigest egressMac;
        KeccakDigest ingressMac;

        public byte[] getAes() {
            return aes;
        }

        public byte[] getMac() {
            return mac;
        }

        public byte[] getToken() {
            return token;
        }

        public KeccakDigest getIngressMac() {
            return ingressMac;
        }

        public KeccakDigest getEgressMac() {
            return egressMac;
        }
    }

    public boolean isInitiator() {
        return isInitiator;
    }
}
