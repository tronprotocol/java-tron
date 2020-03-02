package org.tron.common.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.SignatureException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.spongycastle.crypto.digests.SM3Digest;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.sm2.SM2;
import org.tron.common.crypto.sm2.SM2Signer;
import org.tron.core.Wallet;

@Slf4j
public class SM2KeyTest {

  //private String IDa = "ALICE123@YAHOO.COM";
  private static BigInteger SM2_N = new BigInteger("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFF7203DF6"
      + "B21C6052B53BBF40939D54123", 16);
  private String privString = "128B2FA8BD433C6C068C8D803DFF79792A519A55171B1B650C23661D15897263";
  private BigInteger privateKey = new BigInteger(privString, 16);
  private String pubString = "04d5548c7825cbb56150a3506cd57464af8a1ae0519dfaf3c58221dc810caf28d"
      + "d921073768fe3d59ce54e79a49445cf73fed23086537027264d168946d479533e";
  private String compressedPubString =
      "02d5548c7825cbb56150a3506cd57464af8a1ae0519dfaf3c58221dc810caf28dd";
  private byte[] pubKey = Hex.decode(pubString);
  private byte[] compressedPubKey = Hex.decode(compressedPubString);
  private String address = "62e49e4c2f4e3c0653a02f8859c1e6991b759e87";

  @Test
  public void testHashCode() {
    assertEquals(1126288006, SM2.fromPrivate(privateKey).hashCode());
  }

  @Test
  public void testSM2() {
    SM2 key = new SM2();
    assertTrue(key.isPubKeyCanonical());
    assertNotNull(key.getPubKey());
    assertNotNull(key.getPrivKeyBytes());
    logger.info(Hex.toHexString(key.getPrivKeyBytes()) + " :Generated privkey");
    logger.info(Hex.toHexString(key.getPubKey()) + " :Generated pubkey");
    logger.info("private key in bigInteger form: " + key.getPrivKey());
  }

  @Test
  public void testFromPrivateKey() {
    SM2 key = SM2.fromPrivate(privateKey);
    assertTrue(key.isPubKeyCanonical());
    assertTrue(key.hasPrivKey());
    assertArrayEquals(pubKey, key.getPubKey());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPrivatePublicKeyBytesNoArg() {
    new SM2((BigInteger) null, null);
    fail("Expecting an IllegalArgumentException for using only null-parameters");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidPrivateKey() throws Exception {
    new SM2(
        KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate(),
        SM2.fromPublicOnly(pubKey).getPubKeyPoint());
    fail("Expecting an IllegalArgumentException for using an non EC private key");
  }

  @Test
  public void testIsPubKeyOnly() {
    SM2 key = SM2.fromPublicOnly(pubKey);
    assertTrue(key.isPubKeyCanonical());
    assertTrue(key.isPubKeyOnly());
    assertArrayEquals(key.getPubKey(), pubKey);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSignIncorrectInputSize() {
    SM2 key = new SM2();
    String message = "The quick brown fox jumps over the lazy dog.";
    SM2.SM2Signature sig = key.sign(message.getBytes());
    fail("Expecting an IllegalArgumentException for a non 32-byte input");
  }

  @Test(expected = SignatureException.class)
  public void testBadBase64Sig() throws SignatureException {
    byte[] messageHash = new byte[32];
    SM2.signatureToKey(messageHash, "This is not valid Base64!");
    fail("Expecting a SignatureException for invalid Base64");
  }

  @Test(expected = SignatureException.class)
  public void testInvalidSignatureLength() throws SignatureException {
    byte[] messageHash = new byte[32];
    SM2.signatureToKey(messageHash, "abcdefg");
    fail("Expecting a SignatureException for invalid signature length");
  }

  @Test
  public void testSM3Hash() {
    //SM2 key = SM2.fromPrivate(privateKey);
    SM2 key = SM2.fromPublicOnly(pubKey);
    SM2Signer signer = key.getSM2SignerForHash();
    String message = "message digest";
    byte[] hash = signer.generateSM3Hash(message.getBytes());
    assertEquals("299C7DDB0D8DD2A85381BACBB92F738F390210A493A144C78E18C67B430DA882",
        Hex.toHexString(hash).toUpperCase());
  }


  @Test
  public void testSignatureToKeyBytes() throws SignatureException {
    SM2 key = SM2.fromPrivate(privateKey);
    byte[] hash = Hex.decode("B524F552CD82B8B028476E005C377FB"
        + "19A87E6FC682D48BB5D42E3D9B9EFFE76");
    SM2.SM2Signature sign = key.sign(hash);
    byte[] pubKeys = SM2.signatureToKeyBytes(hash, sign);
    //        System.out.println(Hex.toHexString(pubKeys));
    //        System.out.println(Hex.toHexString(pubKey));
    assertEquals(Hex.toHexString(pubKey), Hex.toHexString(pubKeys));
  }

  @Test
  public void testSignatureToKeyBytes2() throws SignatureException {
    SM2 key = SM2.fromPrivate(privateKey);
    byte[] hash = Hex.decode("B524F552CD82B8B028476E005C377FB"
        + "19A87E6FC682D48BB5D42E3D9B9EFFE76");
    SM2.SM2Signature sign = key.sign(hash);
    byte[] pubKeys = SM2.signatureToKeyBytes(hash, sign);
    assertArrayEquals(pubKeys, key.getPubKey());
  }

  @Test
  public void testSignatureToAddress() throws SignatureException {
    SM2 key = SM2.fromPrivate(privateKey);
    byte[] hash = Hex.decode("B524F552CD82B8B028476E005C377FB"
        + "19A87E6FC682D48BB5D42E3D9B9EFFE76");
    SM2.SM2Signature sign = key.sign(hash);
    byte[] addr = SM2.signatureToAddress(hash, sign);
    addr = Arrays.copyOfRange(addr, 1, addr.length);
    //        System.out.println(Hex.toHexString(addr));
    //        System.out.println(address);
    assertEquals(address, Hex.toHexString(addr));
  }

  @Test
  public void testPublicKeyFromPrivate() {
    byte[] pubFromPriv = SM2.publicKeyFromPrivate(privateKey, false);
    assertArrayEquals(pubKey, pubFromPriv);
  }

  @Test
  public void testPublicKeyFromPrivateCompressed() {
    byte[] pubFromPriv = SM2.publicKeyFromPrivate(privateKey, true);
    //System.out.println(Hex.toHexString(pubFromPriv));
    assertArrayEquals(compressedPubKey, pubFromPriv);
  }

  @Test
  public void testGetAddress() {
    SM2 key = SM2.fromPublicOnly(pubKey);
    // Addresses are prefixed with a constant.
    byte[] prefixedAddress = key.getAddress();
    byte[] unprefixedAddress = Arrays.copyOfRange(key.getAddress(), 1, prefixedAddress.length);
    //System.out.println(Hex.toHexString(unprefixedAddress));
    assertArrayEquals(Hex.decode(address), unprefixedAddress);
    assertEquals(Wallet.getAddressPreFixByte(), prefixedAddress[0]);
  }

  @Test
  public void testGetAddressFromPrivateKey() {
    SM2 key = SM2.fromPrivate(privateKey);
    // Addresses are prefixed with a constant.
    byte[] prefixedAddress = key.getAddress();
    byte[] unprefixedAddress = Arrays.copyOfRange(key.getAddress(), 1, prefixedAddress.length);
    assertArrayEquals(Hex.decode(address), unprefixedAddress);
    assertEquals(Wallet.getAddressPreFixByte(), prefixedAddress[0]);
  }

  @Test
  public void testToString() {
    SM2 key = SM2.fromPrivate(BigInteger.TEN); // An example private key.
    assertEquals("pub:04d3f94862519621c121666061f65c3e32b2d0d065"
        + "cd219e3284a04814db5227564b9030cf676f6a742ebd57d146dca"
        + "428f6b743f64d1482d147d46fb2bab82a14", key.toString());
  }

  @Test
  public void testIsPubKeyCanonicalCorrect() {
    // Test correct prefix 4, right length 65
    byte[] canonicalPubkey1 = new byte[65];
    canonicalPubkey1[0] = 0x04;
    assertTrue(SM2.isPubKeyCanonical(canonicalPubkey1));
    // Test correct prefix 2, right length 33
    byte[] canonicalPubkey2 = new byte[33];
    canonicalPubkey2[0] = 0x02;
    assertTrue(SM2.isPubKeyCanonical(canonicalPubkey2));
    // Test correct prefix 3, right length 33
    byte[] canonicalPubkey3 = new byte[33];
    canonicalPubkey3[0] = 0x03;
    assertTrue(SM2.isPubKeyCanonical(canonicalPubkey3));
  }

  @Test
  public void testIsPubKeyCanonicalWrongLength() {
    // Test correct prefix 4, but wrong length !65
    byte[] nonCanonicalPubkey1 = new byte[64];
    nonCanonicalPubkey1[0] = 0x04;
    assertFalse(SM2.isPubKeyCanonical(nonCanonicalPubkey1));
    // Test correct prefix 2, but wrong length !33
    byte[] nonCanonicalPubkey2 = new byte[32];
    nonCanonicalPubkey2[0] = 0x02;
    assertFalse(SM2.isPubKeyCanonical(nonCanonicalPubkey2));
    // Test correct prefix 3, but wrong length !33
    byte[] nonCanonicalPubkey3 = new byte[32];
    nonCanonicalPubkey3[0] = 0x03;
    assertFalse(SM2.isPubKeyCanonical(nonCanonicalPubkey3));
  }

  @Test
  public void testIsPubKeyCanonicalWrongPrefix() {
    // Test wrong prefix 4, right length 65
    byte[] nonCanonicalPubkey4 = new byte[65];
    assertFalse(SM2.isPubKeyCanonical(nonCanonicalPubkey4));
    // Test wrong prefix 2, right length 33
    byte[] nonCanonicalPubkey5 = new byte[33];
    assertFalse(SM2.isPubKeyCanonical(nonCanonicalPubkey5));
    // Test wrong prefix 3, right length 33
    byte[] nonCanonicalPubkey6 = new byte[33];
    assertFalse(SM2.isPubKeyCanonical(nonCanonicalPubkey6));
  }

  @Test
  public void testGetPrivKeyBytes() {
    SM2 key = new SM2();
    assertNotNull(key.getPrivKeyBytes());
    assertEquals(32, key.getPrivKeyBytes().length);
  }

  @Test
  public void testEqualsObject() {
    SM2 key0 = new SM2();
    SM2 key1 = SM2.fromPrivate(privateKey);
    SM2 key2 = SM2.fromPrivate(privateKey);

    assertFalse(key0.equals(key1));
    assertTrue(key1.equals(key1));
    assertTrue(key1.equals(key2));
  }

  //  @Test
  //  public void decryptAECSIC() {
  //    SM2 key = SM2.fromPrivate(
  //            Hex.decode("abb51256c1324a1350598653f46aa3ad693ac3cf5d05f36eba3f495a1f51590f"));
  //    byte[] payload = key.decryptAES(Hex.decode("84a727bc81fa4b13947dc9728b88fd08"));
  //    System.out.println(Hex.toHexString(payload));
  //  }

  @Test
  public void testNodeId() {
    SM2 key = SM2.fromPublicOnly(pubKey);

    assertEquals(key, SM2.fromNodeId(key.getNodeId()));
  }

  @Test
  public void testSM3() {
    String message = "F4A38489E32B45B6F876E3AC2168CA392362DC8F23459C1D1146F"
        + "C3DBFB7BC9A6D65737361676520646967657374";
    SM3Digest digest = new SM3Digest();
    byte[] msg = Hex.decode(message);
    digest.update(msg, 0, msg.length);

    byte[] eHash = new byte[digest.getDigestSize()];

    digest.doFinal(eHash, 0);

    assertEquals("b524f552cd82b8b028476e005c377fb19a87e6fc682d48bb5d42e3d9b9effe76",
        Hex.toHexString(eHash));
  }

}
