package org.web3j.crypto;

import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import org.junit.Test;
import org.web3j.utils.Numeric;

/**
 * Hash is on the DNS tree signing path (Algorithm.signTree / verifySignature),
 * so its digests need to stay byte-exact. Vectors are the published ones for
 * each algorithm.
 */
public class HashTest {

  private static final byte[] EMPTY = new byte[0];

  @Test
  public void sha3OfEmptyInput() {
    Assert.assertEquals(
        "0xc5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470",
        Numeric.toHexString(Hash.sha3(EMPTY)));
  }

  @Test
  public void sha3StringMatchesKnownVector() {
    Assert.assertEquals(
        "0x47173285a8d7341e5e972fc677286384f802f8ef42a5ec5f03bbfa254cb01fad",
        Hash.sha3String("hello world"));
  }

  @Test
  public void sha3OverARange() {
    byte[] input = "hello world".getBytes(StandardCharsets.UTF_8);
    byte[] whole = Hash.sha3(input);
    byte[] range = Hash.sha3(input, 0, input.length);
    Assert.assertArrayEquals(whole, range);
    // A narrower window must produce a different digest.
    Assert.assertNotEquals(Numeric.toHexString(whole),
        Numeric.toHexString(Hash.sha3(input, 0, 5)));
  }

  @Test
  public void sha3OnHexStringRoundTrips() {
    byte[] input = "hello world".getBytes(StandardCharsets.UTF_8);
    Assert.assertEquals(Numeric.toHexString(Hash.sha3(input)),
        Hash.sha3(Numeric.toHexString(input)));
  }

  @Test
  public void sha256MatchesKnownVector() {
    Assert.assertEquals(
        "0xe3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        Numeric.toHexString(Hash.sha256(EMPTY)));
  }

  @Test
  public void hashDispatchesByAlgorithmName() {
    Assert.assertArrayEquals(Hash.sha256(EMPTY), Hash.hash(EMPTY, "SHA-256"));
    // The name is upper-cased before lookup, so a lowercase name resolves too.
    Assert.assertArrayEquals(Hash.sha256(EMPTY), Hash.hash(EMPTY, "sha-256"));
  }

  @Test(expected = RuntimeException.class)
  public void hashRejectsUnknownAlgorithm() {
    Hash.hash(EMPTY, "NOT-A-REAL-DIGEST");
  }

  @Test
  public void sha256hash160ProducesTwentyBytes() {
    byte[] out = Hash.sha256hash160("hello world".getBytes(StandardCharsets.UTF_8));
    Assert.assertEquals(20, out.length);
    Assert.assertArrayEquals(out,
        Hash.sha256hash160("hello world".getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  public void hmacSha512ProducesSixtyFourBytes() {
    byte[] key = "key".getBytes(StandardCharsets.UTF_8);
    byte[] out = Hash.hmacSha512(key, "message".getBytes(StandardCharsets.UTF_8));
    Assert.assertEquals(64, out.length);
    Assert.assertArrayEquals(out,
        Hash.hmacSha512(key, "message".getBytes(StandardCharsets.UTF_8)));
    Assert.assertFalse(java.util.Arrays.equals(out,
        Hash.hmacSha512("other".getBytes(StandardCharsets.UTF_8),
            "message".getBytes(StandardCharsets.UTF_8))));
  }

  @Test
  public void blake2b256ProducesThirtyTwoBytes() {
    byte[] out = Hash.blake2b256("hello world".getBytes(StandardCharsets.UTF_8));
    Assert.assertEquals(32, out.length);
    Assert.assertArrayEquals(out,
        Hash.blake2b256("hello world".getBytes(StandardCharsets.UTF_8)));
  }
}
