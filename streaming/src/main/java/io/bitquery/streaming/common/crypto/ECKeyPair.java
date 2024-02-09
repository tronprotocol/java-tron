package io.bitquery.streaming.common.crypto;

import io.bitquery.streaming.common.utils.Numeric;
import lombok.Getter;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

public class ECKeyPair {
    @Getter
    private final BigInteger privateKey;
    @Getter
    private final BigInteger publicKey;

    public ECKeyPair(BigInteger privateKey, BigInteger publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public ECDSASignature sign(byte[] message) {
        ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        ECPrivateKeyParameters privKey = new ECPrivateKeyParameters(this.privateKey, Sign.CURVE);
        signer.init(true, privKey);
        BigInteger[] components = signer.generateSignature(message);
        return (new ECDSASignature(components[0], components[1])).toCanonicalised();
    }

    public String getAddress() {
        String pubKey = Numeric.toHexStringWithPrefixZeroPadded(publicKey, 128);
        String publicKeyNoPrefix = Numeric.cleanHexPrefix(pubKey);
//        if (publicKeyNoPrefix.length() < 128) {
//            String var10000 = Strings.zeros(128 - publicKeyNoPrefix.length());
//            publicKeyNoPrefix = var10000 + publicKeyNoPrefix;
//        }

        String hash = Hash.sha3(publicKeyNoPrefix);
        return hash.substring(hash.length() - 40);
    }


    public static ECKeyPair create(KeyPair keyPair) {
        BCECPrivateKey privateKey = (BCECPrivateKey)keyPair.getPrivate();
        BCECPublicKey publicKey = (BCECPublicKey)keyPair.getPublic();
        BigInteger privateKeyValue = privateKey.getD();
        byte[] publicKeyBytes = publicKey.getQ().getEncoded(false);
        BigInteger publicKeyValue = new BigInteger(1, Arrays.copyOfRange(publicKeyBytes, 1, publicKeyBytes.length));
        return new ECKeyPair(privateKeyValue, publicKeyValue);
    }

    public static ECKeyPair create(byte[] privateKey) {
        return create(Numeric.toBigInt(privateKey));
    }

    public static ECKeyPair create(BigInteger privateKey) {
        return new ECKeyPair(privateKey, Sign.publicKeyFromPrivate(privateKey));
    }

    public static ECKeyPair createEcKeyPair() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        KeyPair keyPair = createSecp256k1KeyPair(new SecureRandom());
        return ECKeyPair.create(keyPair);
    }

    static KeyPair createSecp256k1KeyPair(SecureRandom random) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
        ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("secp256k1");
        if (random != null) {
            keyPairGenerator.initialize(ecGenParameterSpec, random);
        } else {
            keyPairGenerator.initialize(ecGenParameterSpec);
        }

        return keyPairGenerator.generateKeyPair();
    }
}
