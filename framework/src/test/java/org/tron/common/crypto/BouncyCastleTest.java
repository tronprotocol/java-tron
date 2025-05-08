package org.tron.common.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.tron.common.utils.client.utils.AbiUtil.generateOccupationConstantPrivateKey;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Arrays;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.crypto.sm2.SM2;
import org.tron.common.utils.Sha256Hash;

/**
 * The reason the test case uses the private key plaintext is to ensure that,
 * after the ECkey tool or algorithm is upgraded,
 * the upgraded differences can be verified.
 */
public class BouncyCastleTest {

  // For safety reasons, test with a placeholder private key
  private final String privString = generateOccupationConstantPrivateKey();
  private final BigInteger privateKey = new BigInteger(privString, 16);

  @Test
  public void testHex() {
    String spongyAddress = "2e988a386a799f506693793c6a5af6b54dfaabfb";
    ECKey key = ECKey.fromPrivate(privateKey);
    byte[] address = key.getAddress();
    assertEquals(spongyAddress,
        Hex.toHexString(Arrays.copyOfRange(address, 1, 21)));
    assertArrayEquals(Arrays.copyOfRange(address, 1, 21),
        Hex.decode(spongyAddress));
  }

  @Test
  public void testSha256Hash() {
    String msg = "transaction raw data";
    String spongySha256 = "da36dc042630f1aa810171d1fc4db7771a9f12b585848b0fed6caf5c7bd06531";
    String spongySm3 = "5521fbff5abf495e6db8fb4a83ed2bf27b97197757fc5a1002a7edc58b690900";
    byte[] sha256Hash = Sha256Hash.hash(true, msg.getBytes());
    assertEquals(spongySha256, Hex.toHexString(sha256Hash));
    byte[] sm3Hash = Sha256Hash.hash(false, msg.getBytes());
    assertEquals(spongySm3, Hex.toHexString(sm3Hash));
  }

  @Test
  public void testSha3Hash() {
    String msg = "transaction raw data";
    String spongyHash = "429e4ce662a41be0a50e65626f0ec4c8f68d45a57fe80beebab2f82601884795";
    byte[] hash = Hash.sha3(msg.getBytes());
    assertEquals(spongyHash, Hex.toHexString(hash));
  }

  @Test
  public void testECKeyAddress() {
    String spongyPubkey = "04e90c7d3640a1568839c31b70a893ab6714ef8415b9de90cedfc1c8f353a6983e625529"
        + "392df7fa514bdd65a2003f6619567d79bee89830e63e932dbd42362d34";
    String spongyAddress = "2e988a386a799f506693793c6a5af6b54dfaabfb";
    ECKey key = ECKey.fromPrivate(privateKey);
    byte[] pubkey = key.getPubKey();
    assertEquals(spongyPubkey, Hex.toHexString(pubkey));
    byte[] address = key.getAddress();
    assertEquals(spongyAddress,
        Hex.toHexString(Arrays.copyOfRange(address, 1, 21)));
  }

  @Test
  public void testECKeySignature() throws SignatureException {
    SignInterface sign = SignUtils.fromPrivate(Hex.decode(privString), true);
    String msg = "transaction raw data";
    String spongyAddress = "2e988a386a799f506693793c6a5af6b54dfaabfb";
    byte[] hash = Sha256Hash.hash(true, msg.getBytes());
    String sig = sign.signHash(hash);
    byte[] address = SignUtils.signatureToAddress(hash, sig, true);
    assertEquals(spongyAddress, Hex.toHexString(Arrays.copyOfRange(address, 1, 21)));
  }

  @Test
  public void testECSpongySignature() throws SignatureException {
    String msg = "transaction raw data";
    String spongySig = "GwYii3BGoQq3sdyWiGVv7bGCR5hJy62g+IF+1jPOSqHt"
        + "IDfuKgowhiiK7ivcqk+T7qq/hlfIjaRe+t1drFDZ+Mo=";
    String spongyAddress = "cd2a3d9f938e13cd947ec05abc7fe734df8dd826";
    byte[] hash = Sha256Hash.hash(true, msg.getBytes());
    byte[] address = SignUtils.signatureToAddress(hash, spongySig, true);
    assertEquals(spongyAddress, Hex.toHexString(Arrays.copyOfRange(address, 1, 21)));
  }

  @Test
  public void testSM3Hash() {
    String msg = "transaction raw data";
    String spongyHash = "5521fbff5abf495e6db8fb4a83ed2bf27b97197757fc5a1002a7edc58b690900";
    SM3Digest digest = new SM3Digest();
    digest.update(msg.getBytes(), 0, msg.getBytes().length);
    byte[] hash = new byte[digest.getDigestSize()];
    digest.doFinal(hash, 0);
    assertEquals(spongyHash, Hex.toHexString(hash));
  }

  @Test
  public void testSM2Address() {
    String spongyPublickey = "04dc3547dbbc4c90a9cde599848e26cb145e805b3d11daaf9daae0680d9c6824058ac"
        + "35ddecb12f3a8bbc3104a2b91a2b7d04851d773d9b4ab8d5e0359243c8628";
    String spongyAddress = "6cb22f88564bdd61eb4cdb36215add53bc702ff1";
    SM2 key = SM2.fromPrivate(privateKey);
    assertEquals(spongyPublickey, Hex.toHexString(key.getPubKey()));
    byte[] address = key.getAddress();
    assertEquals(spongyAddress, Hex.toHexString(Arrays.copyOfRange(address, 1, 21)));
  }

  @Test
  public void testSM2Signature() throws SignatureException {
    SignInterface sign = SignUtils.fromPrivate(Hex.decode(privString), false);
    String msg = "transaction raw data";
    String spongyAddress = "6cb22f88564bdd61eb4cdb36215add53bc702ff1";
    byte[] hash = Sha256Hash.hash(false, msg.getBytes());
    String sig = sign.signHash(hash);
    byte[] address = SignUtils.signatureToAddress(hash, sig, false);
    assertEquals(spongyAddress, Hex.toHexString(Arrays.copyOfRange(address, 1, 21)));
  }

  @Test
  public void testSM2SpongySignature() throws SignatureException {
    String msg = "transaction raw data";
    String spongySig = "HOoyvBLOJ+dKReQdAc6W/ffRi/KmVntco0+xgzmFItEExq/fHF"
        + "veCe0GoCJUBdyHyUFjwn+a18ibtGJcHxnvLj0=";
    String spongyAddress = "7dc44d739a5226c0d3037bb7919f653eb2f938b9";
    byte[] hash = Sha256Hash.hash(false, msg.getBytes());
    byte[] address = SignUtils.signatureToAddress(hash, spongySig, false);
    assertEquals(spongyAddress, Hex.toHexString(Arrays.copyOfRange(address, 1, 21)));
  }

  @Test
  public void testSignToAddress() {
    String messageHash = "818e0e76976123b9b78b6076cc2b5d53e61b49ff9cf78304de688a860ce7cb95";
    String base64Sign = "G1y76mVO6TRpFwp3qOiLVzHA8uFsrDiOL7hbC2uN9qTHHiLypaW4vnQkfkoUygjo5qBd"
        + "+NlYQ/mAPVWKu6K00co=";
    try {
      SignUtils.signatureToAddress(Hex.decode(messageHash), base64Sign, Boolean.TRUE);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof SignatureException);
    }
    try {
      SignUtils.signatureToAddress(Hex.decode(messageHash), base64Sign, Boolean.FALSE);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof SignatureException);
    }
  }
}
