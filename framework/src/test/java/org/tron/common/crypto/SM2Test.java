package org.tron.common.crypto;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.asn1.sec.SECNamedCurves;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.sm2.SM2;
import org.tron.common.crypto.sm2.SM2KeyPair;

import java.math.BigInteger;
import java.util.Enumeration;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * SM2 test
 *
 */
public class SM2Test {
    private SM2 sm2;
    private SM2KeyPair sm2KeyPair;

    @Before
    public void setup() {
        System.out.println("before test");
        System.out.println("set up ....");
        sm2 = new SM2();
        sm2KeyPair = sm2.generateKeyPair();
    }
    @After
    public void finalize() {
        System.out.println("finalize..");
    }


    @Test
    public void generateKeyPair() {
        System.out.println("--------------sm2 key pair generation test--------------");
        System.out.println("generate the sm2 public key: " + Hex.toHexString(sm2KeyPair.getPublickey()));
        System.out.println("generate the sm2 private key:" + Hex.toHexString(sm2KeyPair.getPrivatekey()));
    }

    @Test
    public void SignTest() throws Exception {
        byte[] content = genByteArray(32);
        byte[] privateKey = sm2KeyPair.getPrivatekey();
        byte[] publicKey = sm2KeyPair.getPublickey();
        BigInteger[] signature = sm2.sign(privateKey, content);
        boolean result = sm2.verify(publicKey,signature,content);
        System.out.println("validation results: " + result);
    }

    @Test
    public void SignNegtiveTest() throws Exception {
        byte[] content = genByteArray(32);
        byte[] privateKey = sm2KeyPair.getPrivatekey();
        byte[] publicKey = sm2KeyPair.getPublickey();
        BigInteger[] signature = sm2.sign(privateKey, content);
        //BigInteger P = new BigInteger("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFF",16);
        BigInteger N = new BigInteger("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFF7203DF6B21C6052B53BBF40939D54123",16);
        BigInteger sig1 = signature[1].negate().mod(N);
        System.out.println(sig1);
        BigInteger sig2 = N.subtract(signature[1]);
        System.out.println(sig2);
        signature[1] = sig1;
        boolean result = sm2.verify(publicKey,signature,content);
        System.out.println("validation results: " + result);
    }

    /**
     * generate the random byte array
     *
     * @param size
     * @return
     */
    public static byte[] genByteArray(int size) {
        byte[] rdBytes = new byte[size];
        Random random = new Random();
        random.nextBytes(rdBytes);
        return rdBytes;
    }

    @Test
    public void invalidParamSignTest() {
        byte[] testData = null;
        byte[] zeroData = new byte[0];
        byte[] privateKey = sm2KeyPair.getPrivatekey();
        try {
            sm2.sign(privateKey,null);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("plaintext is null",e.getMessage());
        }
        try {
            sm2.sign(zeroData,null);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("the length of private is 0",e.getMessage());
        }
        try {
            sm2.sign(null, genByteArray(12));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("private key is null", e.getMessage());
        }
    }

    @Test
    public void invalidParamVerify() {
        byte[] testData = null;
        byte[] zeroData = new byte[0];
        byte[] publicKey = sm2KeyPair.getPublickey();

        try {
            sm2.verify(null, null, null);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("public key is null", e.getMessage());
        }
        try {
            sm2.verify(publicKey, null, null);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("signValue is null", e.getMessage());
        }
    }
    @Test
    public void diffSizeDataSignWithVerity() throws Exception {
        int[] testData = {1, 16, 32, 64, 128, 256, 512, 1024, 2048, 1048576, 2097152};
        for (int i = 0; i < testData.length; i++) {
            byte[] content = genByteArray(testData[i]);
            byte[] privateKey = sm2KeyPair.getPrivatekey();
            byte[] publicKey = sm2KeyPair.getPublickey();
            long t1 = System.currentTimeMillis();
            BigInteger[] signature = sm2.sign(privateKey,content);
            sm2.verify(publicKey,signature,content);
            long t2 = System.currentTimeMillis();
            System.out.println(String.format("%s byte data expand the time %s:ms",testData[i],t2-t1));
        }
    }

    @Test
    public void scTest(){
        //BouncyCastleProvider cp = new BouncyCastleProvider();
        Set s = BouncyCastleProvider.CONFIGURATION.getAcceptableNamedCurves();
        System.out.println(s);
        Enumeration e =  SECNamedCurves.getNames();
        while (e.hasMoreElements()) {
            System.out.println(e.nextElement());
        }
    }


}
