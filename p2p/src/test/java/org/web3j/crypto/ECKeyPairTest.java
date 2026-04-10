package org.web3j.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.web3j.utils.Numeric;

public class ECKeyPairTest {

  // Well-known test private key (from Ethereum docs / web3j tests)
  private static final String PRIVATE_KEY_HEX =
      "a392604efc2fad9c0b3da43b5f698a2e3f270f170d859912be0d54742275c5f6";
  private static final BigInteger PRIVATE_KEY = new BigInteger(PRIVATE_KEY_HEX, 16);

  @BeforeClass
  public static void setUp() {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  // --- create from BigInteger ---

  @Test
  public void testCreateFromBigInteger() {
    ECKeyPair keyPair = ECKeyPair.create(PRIVATE_KEY);
    assertEquals(PRIVATE_KEY, keyPair.getPrivateKey());
    assertNotNull(keyPair.getPublicKey());
    // Public key should be 512 bits (64 bytes) for secp256k1 uncompressed without prefix
    assertTrue(keyPair.getPublicKey().bitLength() > 0);
  }

  // --- create from byte[] ---

  @Test
  public void testCreateFromBytes() {
    byte[] privateKeyBytes = Numeric.hexStringToByteArray(PRIVATE_KEY_HEX);
    ECKeyPair keyPair = ECKeyPair.create(privateKeyBytes);
    assertEquals(PRIVATE_KEY, keyPair.getPrivateKey());
    assertNotNull(keyPair.getPublicKey());
  }

  // --- create from JCA KeyPair ---

  @Test
  public void testCreateFromJcaKeyPair() throws Exception {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", "BC");
    keyGen.initialize(new ECGenParameterSpec("secp256k1"));
    KeyPair jcaKeyPair = keyGen.generateKeyPair();
    ECKeyPair ecKeyPair = ECKeyPair.create(jcaKeyPair);
    assertNotNull(ecKeyPair.getPrivateKey());
    assertNotNull(ecKeyPair.getPublicKey());
  }

  // --- deterministic key derivation ---

  @Test
  public void testSamePrivateKeyGivesSamePublicKey() {
    ECKeyPair kp1 = ECKeyPair.create(PRIVATE_KEY);
    ECKeyPair kp2 = ECKeyPair.create(PRIVATE_KEY);
    assertEquals(kp1.getPublicKey(), kp2.getPublicKey());
  }

  // --- sign ---

  @Test
  public void testSign() {
    ECKeyPair keyPair = ECKeyPair.create(PRIVATE_KEY);
    byte[] hash = Hash.sha3("test message".getBytes());
    ECDSASignature sig = keyPair.sign(hash);
    assertNotNull(sig);
    assertNotNull(sig.r);
    assertNotNull(sig.s);
    assertTrue(sig.r.signum() > 0);
    assertTrue(sig.s.signum() > 0);
    // Signature should be canonical
    assertTrue(sig.isCanonical());
  }

  @Test
  public void testSignAndRecover() throws Exception {
    ECKeyPair keyPair = ECKeyPair.create(PRIVATE_KEY);
    byte[] message = "test recovery".getBytes();
    Sign.SignatureData sigData = Sign.signMessage(message, keyPair);
    BigInteger recoveredKey = Sign.signedMessageToKey(message, sigData);
    assertEquals(keyPair.getPublicKey(), recoveredKey);
  }

  // --- equals and hashCode ---

  @Test
  public void testEqualsAndHashCode() {
    ECKeyPair kp1 = ECKeyPair.create(PRIVATE_KEY);
    ECKeyPair kp2 = ECKeyPair.create(PRIVATE_KEY);
    assertEquals(kp1, kp2);
    assertEquals(kp1.hashCode(), kp2.hashCode());
  }

  @Test
  public void testEqualsSameInstance() {
    ECKeyPair kp = ECKeyPair.create(PRIVATE_KEY);
    assertTrue(kp.equals(kp));
  }

  @Test
  public void testNotEqualsNull() {
    ECKeyPair kp = ECKeyPair.create(PRIVATE_KEY);
    assertFalse(kp.equals(null));
  }

  @Test
  public void testNotEqualsDifferentClass() {
    ECKeyPair kp = ECKeyPair.create(PRIVATE_KEY);
    assertFalse(kp.equals("not a key pair"));
  }

  @Test
  public void testNotEqualsDifferentKey() {
    ECKeyPair kp1 = ECKeyPair.create(PRIVATE_KEY);
    ECKeyPair kp2 = ECKeyPair.create(BigInteger.valueOf(12345));
    assertNotEquals(kp1, kp2);
  }
}
