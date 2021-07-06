package org.tron.common.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Arrays;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.tron.common.crypto.sm2.SM2;

public class BouncyCastleTest {

  private String privString = "c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4";
  private BigInteger privateKey = new BigInteger(privString, 16);

  @Test
  public void testHex() {
    ECKey key = ECKey.fromPrivate(privateKey);
    byte[] address = key.getAddress();
    assertEquals("cd2a3d9f938e13cd947ec05abc7fe734df8dd826",
        Hex.toHexString(Arrays.copyOfRange(address, 1, 21)));
    assertArrayEquals(Arrays.copyOfRange(address, 1, 21),
        Hex.decode("cd2a3d9f938e13cd947ec05abc7fe734df8dd826"));
  }

  @Test
  public void testSha3Hash() {
    String msg = "transaction raw data";
    byte[] hash = Hash.sha3(msg.getBytes());
    assertEquals("429e4ce662a41be0a50e65626f0ec4c8f68d45a57fe80beebab2f82601884795",
        Hex.toHexString(hash));
  }

  @Test
  public void testECKeyAddress() {
    ECKey key = ECKey.fromPrivate(privateKey);
    byte[] address = key.getAddress();
    assertEquals("cd2a3d9f938e13cd947ec05abc7fe734df8dd826",
        Hex.toHexString(Arrays.copyOfRange(address, 1, 21)));
  }

  @Test
  public void testECKeySignature() throws SignatureException {
    SignInterface sign = SignUtils.fromPrivate(Hex.decode(privString), true);
    String msg = "transaction raw data";
    byte[] hash = Hash.sha3(msg.getBytes());
    String sig = sign.signHash(hash);
    byte[] address = SignUtils.signatureToAddress(hash, sig, true);
    assertEquals("cd2a3d9f938e13cd947ec05abc7fe734df8dd826",
        Hex.toHexString(Arrays.copyOfRange(address, 1, 21)));
  }

  @Test
  public void testSM3Hash() {
    String msg = "transaction raw data";
    SM3Digest digest = new SM3Digest();
    digest.update(msg.getBytes(), 0, msg.getBytes().length);
    byte[] hash = new byte[digest.getDigestSize()];
    digest.doFinal(hash, 0);
    assertEquals("5521fbff5abf495e6db8fb4a83ed2bf27b97197757fc5a1002a7edc58b690900",
        Hex.toHexString(hash));
  }

  @Test
  public void testSM2Address() {
    SM2 key = SM2.fromPrivate(privateKey);
    byte[] address = key.getAddress();
    assertEquals("7dc44d739a5226c0d3037bb7919f653eb2f938b9",
        Hex.toHexString(Arrays.copyOfRange(address, 1, 21)));
  }

  @Test
  public void testSM2Signature() throws SignatureException {
    SignInterface sign = SignUtils.fromPrivate(Hex.decode(privString), false);
    String msg = "transaction raw data";
    SM3Digest digest = new SM3Digest();
    digest.update(msg.getBytes(), 0, msg.getBytes().length);
    byte[] hash = new byte[digest.getDigestSize()];
    digest.doFinal(hash, 0);

    String sig = sign.signHash(hash);
    byte[] address = SignUtils.signatureToAddress(hash, sig, false);
    assertEquals("7dc44d739a5226c0d3037bb7919f653eb2f938b9",
        Hex.toHexString(Arrays.copyOfRange(address, 1, 21)));
  }
}
