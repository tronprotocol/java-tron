package org.web3j.crypto;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import org.junit.Assert;
import org.junit.Test;
import org.web3j.utils.Numeric;

/**
 * secp256k1 sign / recover, which Algorithm.signTree and Algorithm.verifySignature
 * use to sign and validate the DNS tree root.
 */
public class SignTest {

  private static final BigInteger PRIVATE_KEY = new BigInteger(
      "a392604efc2fad9c0b3da43b5f698a2e3f270f170d859912be0d54742275c5f6", 16);
  private static final byte[] MESSAGE = "hello world".getBytes(StandardCharsets.UTF_8);

  private static ECKeyPair keyPair() {
    return ECKeyPair.create(PRIVATE_KEY);
  }

  @Test
  public void publicKeyIsDerivedDeterministicallyFromPrivate() {
    BigInteger first = Sign.publicKeyFromPrivate(PRIVATE_KEY);
    Assert.assertEquals(first, Sign.publicKeyFromPrivate(PRIVATE_KEY));
    Assert.assertEquals(first, keyPair().getPublicKey());
    // Uncompressed key minus the 0x04 prefix is exactly 64 bytes. Compare on the
    // zero-padded form: toHexStringNoPrefix drops leading zeros, so a key whose
    // X coordinate starts with a zero nibble renders shorter than 128 chars.
    Assert.assertEquals(64, Numeric.toBytesPadded(first, 64).length);
    Assert.assertEquals(128, Numeric.toHexStringNoPrefixZeroPadded(first, 128).length());
  }

  @Test
  public void signAndRecoverRoundTrip() throws SignatureException {
    ECKeyPair pair = keyPair();
    Sign.SignatureData signature = Sign.signMessage(MESSAGE, pair);

    Assert.assertEquals(32, signature.getR().length);
    Assert.assertEquals(32, signature.getS().length);
    Assert.assertEquals(1, signature.getV().length);

    Assert.assertEquals(pair.getPublicKey(), Sign.signedMessageToKey(MESSAGE, signature));
  }

  @Test
  public void signingIsDeterministic() {
    Sign.SignatureData first = Sign.signMessage(MESSAGE, keyPair());
    Sign.SignatureData second = Sign.signMessage(MESSAGE, keyPair());
    // RFC 6979 deterministic k, so the same message and key give the same bytes.
    Assert.assertArrayEquals(first.getR(), second.getR());
    Assert.assertArrayEquals(first.getS(), second.getS());
    Assert.assertArrayEquals(first.getV(), second.getV());
  }

  @Test
  public void aDifferentMessageRecoversADifferentKeyOrFails() {
    Sign.SignatureData signature = Sign.signMessage(MESSAGE, keyPair());
    byte[] other = "goodbye world".getBytes(StandardCharsets.UTF_8);
    try {
      Assert.assertNotEquals(keyPair().getPublicKey(),
          Sign.signedMessageToKey(other, signature));
    } catch (SignatureException expected) {
      // Recovery legitimately fails for some (message, signature) pairs.
      Assert.assertNotNull(expected.getMessage());
    }
  }

  @Test
  public void prefixedSigningRoundTrips() throws SignatureException {
    ECKeyPair pair = keyPair();
    Sign.SignatureData signature = Sign.signPrefixedMessage(MESSAGE, pair);
    Assert.assertEquals(pair.getPublicKey(),
        Sign.signedPrefixedMessageToKey(MESSAGE, signature));
  }

  @Test
  public void preHashedSigningSkipsTheDigest() throws SignatureException {
    ECKeyPair pair = keyPair();
    byte[] hash = Hash.sha3(MESSAGE);
    Sign.SignatureData signature = Sign.signMessage(hash, pair, false);
    Assert.assertEquals(pair.getPublicKey(), Sign.signedMessageHashToKey(hash, signature));
  }

  @Test
  public void recoveryRejectsAnOutOfRangeHeaderByte() {
    Sign.SignatureData good = Sign.signMessage(MESSAGE, keyPair());
    Sign.SignatureData bad =
        new Sign.SignatureData((byte) 0, good.getR(), good.getS());
    try {
      Sign.signedMessageToKey(MESSAGE, bad);
      Assert.fail("expected a SignatureException");
    } catch (SignatureException expected) {
      Assert.assertTrue(expected.getMessage().contains("Header"));
    }
  }

  @Test
  public void recoverFromSignatureReturnsNullForAnImpossibleRecId() {
    ECDSASignature signature = keyPair().sign(Hash.sha3(MESSAGE));
    Assert.assertNull(Sign.recoverFromSignature(4, signature, Hash.sha3(MESSAGE)));
  }

  @Test
  public void signatureDataValueSemantics() {
    Sign.SignatureData a = Sign.signMessage(MESSAGE, keyPair());
    Sign.SignatureData b = Sign.signMessage(MESSAGE, keyPair());
    Assert.assertEquals(a, b);
    Assert.assertEquals(a.getV()[0], b.getV()[0]);
    Assert.assertEquals(a.hashCode(), b.hashCode());
    Assert.assertNotEquals(a, null);
    Assert.assertNotEquals(a, "not a signature");
    Assert.assertEquals(a, a);
  }
}
