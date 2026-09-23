package org.tron.common.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.tron.common.utils.client.utils.AbiUtil.generateOccupationConstantPrivateKey;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;
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
    byte[] sha256Hash = Sha256Hash.hash(msg.getBytes());
    assertEquals(spongySha256, Hex.toHexString(sha256Hash));
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
    SignInterface sign = SignUtils.fromPrivate(Hex.decode(privString));
    String msg = "transaction raw data";
    String spongyAddress = "2e988a386a799f506693793c6a5af6b54dfaabfb";
    byte[] hash = Sha256Hash.hash(msg.getBytes());
    String sig = sign.signHash(hash);
    byte[] address = SignUtils.signatureToAddress(hash, sig);
    assertEquals(spongyAddress, Hex.toHexString(Arrays.copyOfRange(address, 1, 21)));
  }

  @Test
  public void testECSpongySignature() throws SignatureException {
    String msg = "transaction raw data";
    String spongySig = "GwYii3BGoQq3sdyWiGVv7bGCR5hJy62g+IF+1jPOSqHt"
        + "IDfuKgowhiiK7ivcqk+T7qq/hlfIjaRe+t1drFDZ+Mo=";
    String spongyAddress = "cd2a3d9f938e13cd947ec05abc7fe734df8dd826";
    byte[] hash = Sha256Hash.hash(msg.getBytes());
    byte[] address = SignUtils.signatureToAddress(hash, spongySig);
    assertEquals(spongyAddress, Hex.toHexString(Arrays.copyOfRange(address, 1, 21)));
  }

  @Test
  public void testSignToAddress() {
    String messageHash = "818e0e76976123b9b78b6076cc2b5d53e61b49ff9cf78304de688a860ce7cb95";
    String base64Sign = "G1y76mVO6TRpFwp3qOiLVzHA8uFsrDiOL7hbC2uN9qTHHiLypaW4vnQkfkoUygjo5qBd"
        + "+NlYQ/mAPVWKu6K00co=";
    try {
      SignUtils.signatureToAddress(Hex.decode(messageHash), base64Sign);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof SignatureException);
    }
  }
}
