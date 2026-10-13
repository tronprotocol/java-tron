package org.tron.common.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.tron.common.utils.client.utils.AbiUtil.generateOccupationConstantPrivateKey;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.core.Wallet;

/**
 * The reason the test case uses the private key plaintext is to ensure that,
 * after the ECkey tool or algorithm is upgraded,
 * the upgraded differences can be verified.
 */
@Slf4j
public class ECKeyTest {

  // For safety reasons, test with a placeholder private key
  private String privString = generateOccupationConstantPrivateKey();
  private BigInteger privateKey = new BigInteger(privString, 16);

  private String pubString = "04e90c7d3640a1568839c31b70a893ab6714ef8415b9de90cedfc1c8f353a6983e62"
      + "5529392df7fa514bdd65a2003f6619567d79bee89830e63e932dbd42362d34";
  private String compressedPubString =
      "02e90c7d3640a1568839c31b70a893ab6714ef8415b9de90cedfc1c8f353a6983e";
  private byte[] pubKey = Hex.decode(pubString);
  private byte[] compressedPubKey = Hex.decode(compressedPubString);
  private String address = "2e988a386a799f506693793c6a5af6b54dfaabfb";
  String eventSign = "eventBytesL(address,bytes,bytes32,uint256,string)";

  @Test
  public void testSha3() {
    assertNotEquals(Hash.sha3(eventSign.getBytes()).length, 0);
  }

  @Test
  public void testHashCode() {
    assertEquals(-827927068, ECKey.fromPrivate(privateKey).hashCode());
  }

  @Test
  public void testECKey() {
    ECKey key = new ECKey();
    assertTrue(key.isPubKeyCanonical());
    assertNotNull(key.getPubKey());
    assertNotNull(key.getPrivKeyBytes());
    logger.info(Hex.toHexString(key.getPrivKeyBytes()) + " :Generated privkey");
    logger.info(Hex.toHexString(key.getPubKey()) + " :Generated pubkey");
  }

  @Test
  public void testFromPrivateKey() {
    ECKey key = ECKey.fromPrivate(privateKey);
    assertTrue(key.isPubKeyCanonical());
    assertTrue(key.hasPrivKey());
    assertArrayEquals(pubKey, key.getPubKey());

    assertThrows(IllegalArgumentException.class,
        () -> ECKey.fromPrivate((byte[]) null));
    assertThrows(IllegalArgumentException.class,
        () -> ECKey.fromPrivate(new byte[0]));
  }

  @Test
  public void testValidatePrivateKey() {
    assertTrue(ECKey.isValidPrivateKey(privateKey));
    assertTrue(ECKey.isValidPrivateKey(Hex.decode(privString)));
    assertFalse(ECKey.isValidPrivateKey((BigInteger) null));
    assertFalse(ECKey.isValidPrivateKey((byte[]) null));
    assertFalse(ECKey.isValidPrivateKey(new byte[0]));
    assertFalse(ECKey.isValidPrivateKey(new byte[33]));
    assertFalse(ECKey.isValidPrivateKey(BigInteger.ZERO));
    assertFalse(ECKey.isValidPrivateKey(ECKey.CURVE.getN()));

    BigInteger highBitPrivateKey = ECKey.CURVE.getN().subtract(BigInteger.ONE);
    byte[] signPaddedPrivateKey = highBitPrivateKey.toByteArray();
    assertEquals(33, signPaddedPrivateKey.length);
    assertEquals(0, signPaddedPrivateKey[0]);
    assertTrue(ECKey.isValidPrivateKey(signPaddedPrivateKey));
    assertEquals(highBitPrivateKey, ECKey.fromPrivate(signPaddedPrivateKey).getPrivKey());

    byte[] redundantSignPaddedPrivateKey = new byte[33];
    redundantSignPaddedPrivateKey[32] = 1;
    assertFalse(ECKey.isValidPrivateKey(redundantSignPaddedPrivateKey));

    byte[] nonZeroLeadingPrivateKey = Arrays.copyOf(signPaddedPrivateKey,
        signPaddedPrivateKey.length);
    nonZeroLeadingPrivateKey[0] = 1;
    assertFalse(ECKey.isValidPrivateKey(nonZeroLeadingPrivateKey));

    byte[] doubleSignPaddedPrivateKey = new byte[34];
    System.arraycopy(signPaddedPrivateKey, 0, doubleSignPaddedPrivateKey, 1,
        signPaddedPrivateKey.length);
    assertFalse(ECKey.isValidPrivateKey(doubleSignPaddedPrivateKey));

    assertThrows(IllegalArgumentException.class,
        () -> ECKey.fromPrivate(BigInteger.ZERO));
    assertThrows(IllegalArgumentException.class,
        () -> ECKey.fromPrivate(ECKey.CURVE.getN()));
    assertThrows(IllegalArgumentException.class,
        () -> ECKey.fromPrivate(new byte[1024 * 1024]));
    assertThrows(IllegalArgumentException.class,
        () -> ECKey.publicKeyFromPrivate(null, false));
  }

  @Test
  public void testRejectInvalidPublicKey() {
    assertTrue(ECKey.isValidPublicKey(pubKey));
    assertTrue(ECKey.isValidPublicKey(compressedPubKey));
    assertFalse(ECKey.isValidPublicKey(null));
    assertFalse(ECKey.isValidPublicKey(new byte[0]));
    assertFalse(ECKey.isValidPublicKey(new byte[]{0}));
    assertFalse(ECKey.isValidPublicKey(new byte[66]));

    byte[] oversizedPublicKey = new byte[1024 * 1024];
    oversizedPublicKey[0] = 0x04;
    assertFalse(ECKey.isValidPublicKey(oversizedPublicKey));

    assertThrows(IllegalArgumentException.class,
        () -> ECKey.fromPublicOnly((byte[]) null));
    assertThrows(IllegalArgumentException.class,
        () -> ECKey.fromPublicOnly(ECKey.CURVE.getCurve().getInfinity()));
    assertThrows(IllegalArgumentException.class,
        () -> ECKey.fromPublicOnly(
            ECKey.CURVE.getCurve().createPoint(BigInteger.ZERO, BigInteger.ZERO)));
    assertThrows(IllegalArgumentException.class,
        () -> ECKey.fromPublicOnly(SECNamedCurves.getByName("secp256r1").getG()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPrivatePublicKeyBytesNoArg() {
    new ECKey((BigInteger) null, null);
    fail("Expecting an IllegalArgumentException for using only null-parameters");
  }

  @Test
  public void testRejectMismatchedKeyPair() {
    BigInteger otherPrivateKey = privateKey.add(BigInteger.ONE);
    ECKey otherKey = ECKey.fromPrivate(otherPrivateKey);

    assertThrows(IllegalArgumentException.class,
        () -> new ECKey(privateKey, otherKey.getPubKeyPoint()));
  }

  @Test
  public void testAcceptMatchingKeyPair() {
    ECKey key = new ECKey(privateKey, ECKey.CURVE.getG().multiply(privateKey));

    assertArrayEquals(pubKey, key.getPubKey());
    assertTrue(key.hasPrivKey());
  }

  @Test
  public void testIsPubKeyOnly() {
    ECKey key = ECKey.fromPublicOnly(pubKey);
    assertTrue(key.isPubKeyCanonical());
    assertTrue(key.isPubKeyOnly());
    assertArrayEquals(key.getPubKey(), pubKey);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSignIncorrectInputSize() {
    ECKey key = new ECKey();
    String message = "The quick brown fox jumps over the lazy dog.";
    ECDSASignature sig = key.doSign(message.getBytes());
    fail("Expecting an IllegalArgumentException for a non 32-byte input");
  }

  @Test(expected = SignatureException.class)
  public void testBadBase64Sig() throws SignatureException {
    byte[] messageHash = new byte[32];
    ECKey.signatureToKey(messageHash, "This is not valid Base64!");
    fail("Expecting a SignatureException for invalid Base64");
  }

  @Test(expected = SignatureException.class)
  public void testInvalidSignatureLength() throws SignatureException {
    byte[] messageHash = new byte[32];
    ECKey.signatureToKey(messageHash, "abcdefg");
    fail("Expecting a SignatureException for invalid signature length");
  }

  @Test
  public void testPublicKeyFromPrivate() {
    byte[] pubFromPriv = ECKey.publicKeyFromPrivate(privateKey, false);
    assertArrayEquals(pubKey, pubFromPriv);
  }

  @Test
  public void testPublicKeyFromPrivateCompressed() {
    byte[] pubFromPriv = ECKey.publicKeyFromPrivate(privateKey, true);
    assertArrayEquals(compressedPubKey, pubFromPriv);
  }

  @Test
  public void testGetAddress() {
    ECKey key = ECKey.fromPublicOnly(pubKey);
    // Addresses are prefixed with a constant.
    byte[] prefixedAddress = key.getAddress();
    byte[] unprefixedAddress = Arrays.copyOfRange(key.getAddress(), 1, prefixedAddress.length);
    assertArrayEquals(Hex.decode(address), unprefixedAddress);
    assertEquals(Wallet.getAddressPreFixByte(), prefixedAddress[0]);
  }

  @Test
  public void testGetAddressFromPrivateKey() {
    ECKey key = ECKey.fromPrivate(privateKey);
    // Addresses are prefixed with a constant.
    byte[] prefixedAddress = key.getAddress();
    byte[] unprefixedAddress = Arrays.copyOfRange(key.getAddress(), 1, prefixedAddress.length);
    assertArrayEquals(Hex.decode(address), unprefixedAddress);
    assertEquals(Wallet.getAddressPreFixByte(), prefixedAddress[0]);
  }

  @Test
  public void testToString() {
    ECKey key = ECKey.fromPrivate(BigInteger.TEN); // An example private key.
    assertEquals("pub:04a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7893aba42"
        + "5419bc27a3b6c7e693a24c696f794c2ed877a1593cbee53b037368d7", key.toString());
  }

  @Test
  public void testIsPubKeyCanonicalCorect() {
    // Test correct prefix 4, right length 65
    byte[] canonicalPubkey1 = new byte[65];
    canonicalPubkey1[0] = 0x04;
    assertTrue(ECKey.isPubKeyCanonical(canonicalPubkey1));
    // Test correct prefix 2, right length 33
    byte[] canonicalPubkey2 = new byte[33];
    canonicalPubkey2[0] = 0x02;
    assertTrue(ECKey.isPubKeyCanonical(canonicalPubkey2));
    // Test correct prefix 3, right length 33
    byte[] canonicalPubkey3 = new byte[33];
    canonicalPubkey3[0] = 0x03;
    assertTrue(ECKey.isPubKeyCanonical(canonicalPubkey3));
  }

  @Test
  public void testIsPubKeyCanonicalWrongLength() {
    // Test correct prefix 4, but wrong length !65
    byte[] nonCanonicalPubkey1 = new byte[64];
    nonCanonicalPubkey1[0] = 0x04;
    assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey1));
    // Test correct prefix 2, but wrong length !33
    byte[] nonCanonicalPubkey2 = new byte[32];
    nonCanonicalPubkey2[0] = 0x02;
    assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey2));
    // Test correct prefix 3, but wrong length !33
    byte[] nonCanonicalPubkey3 = new byte[32];
    nonCanonicalPubkey3[0] = 0x03;
    assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey3));
  }

  @Test
  public void testIsPubKeyCanonicalWrongPrefix() {
    // Test wrong prefix 4, right length 65
    byte[] nonCanonicalPubkey4 = new byte[65];
    assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey4));
    // Test wrong prefix 2, right length 33
    byte[] nonCanonicalPubkey5 = new byte[33];
    assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey5));
    // Test wrong prefix 3, right length 33
    byte[] nonCanonicalPubkey6 = new byte[33];
    assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey6));
    assertFalse(ECKey.isPubKeyCanonical(null));
    assertFalse(ECKey.isPubKeyCanonical(new byte[0]));
  }

  @Test
  public void testGetPrivKeyBytes() {
    ECKey key = new ECKey();
    assertNotNull(key.getPrivKeyBytes());
    assertEquals(32, key.getPrivKeyBytes().length);
  }

  @Test
  public void testEqualsObject() {
    ECKey key0 = new ECKey();
    ECKey key1 = ECKey.fromPrivate(privateKey);
    ECKey key2 = ECKey.fromPrivate(privateKey);
    ECKey publicOnlyKey = ECKey.fromPublicOnly(key1.getPubKey());

    assertFalse(key0.equals(key1));
    assertTrue(key1.equals(key1));
    assertTrue(key1.equals(key2));
    assertTrue(key1.equals(publicOnlyKey));
    assertTrue(publicOnlyKey.equals(key1));
    assertEquals(key1.hashCode(), publicOnlyKey.hashCode());
  }

  @Test
  public void testDefensiveCopy() {
    ECKey key = ECKey.fromPrivate(privateKey);
    byte[] expectedAddress = Arrays.copyOf(key.getAddress(), key.getAddress().length);
    byte[] returnedAddress = key.getAddress();
    returnedAddress[0] ^= 1;
    assertArrayEquals(expectedAddress, key.getAddress());

    byte[] expectedNodeId = Arrays.copyOf(key.getNodeId(), key.getNodeId().length);
    byte[] returnedNodeId = key.getNodeId();
    returnedNodeId[0] ^= 1;
    assertArrayEquals(expectedNodeId, key.getNodeId());
  }


  @Test
  public void testNodeId() {
    ECKey key = ECKey.fromPublicOnly(pubKey);

    assertEquals(key, ECKey.fromNodeId(key.getNodeId()));
  }
}
