package org.web3j.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.web3j.utils.Numeric;

public class HashTest {

  // --- sha3 (Keccak-256) with known test vectors ---

  @Test
  public void testSha3EmptyBytes() {
    // Keccak-256 of empty input
    byte[] result = Hash.sha3(new byte[0]);
    String hex = Numeric.toHexStringNoPrefix(result);
    assertEquals("c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470", hex);
  }

  @Test
  public void testSha3KnownInput() {
    // Keccak-256("testing")
    byte[] input = "testing".getBytes(StandardCharsets.UTF_8);
    byte[] result = Hash.sha3(input);
    String hex = Numeric.toHexStringNoPrefix(result);
    assertEquals("5f16f4c7f149ac4f9510d9cf8cf384038ad348b3bcdc01915f95de12df9d1b02", hex);
  }

  @Test
  public void testSha3WithOffsetAndLength() {
    byte[] input = "XXtestingYY".getBytes(StandardCharsets.UTF_8);
    // hash only "testing" portion (offset=2, length=7)
    byte[] result = Hash.sha3(input, 2, 7);
    byte[] expected = Hash.sha3("testing".getBytes(StandardCharsets.UTF_8));
    assertArrayEquals(expected, result);
  }

  @Test
  public void testSha3HexString() {
    // sha3 from hex string: keccak256(0x68656c6c6f) = keccak256("hello")
    String hexInput = "68656c6c6f";
    String result = Hash.sha3(hexInput);
    byte[] directResult = Hash.sha3("hello".getBytes(StandardCharsets.UTF_8));
    assertEquals(Numeric.toHexString(directResult), result);
  }

  @Test
  public void testSha3HexStringWithPrefix() {
    String result = Hash.sha3("0x68656c6c6f");
    byte[] directResult = Hash.sha3("hello".getBytes(StandardCharsets.UTF_8));
    assertEquals(Numeric.toHexString(directResult), result);
  }

  // --- sha3String ---

  @Test
  public void testSha3String() {
    String result = Hash.sha3String("hello");
    byte[] directResult = Hash.sha3("hello".getBytes(StandardCharsets.UTF_8));
    assertEquals(Numeric.toHexString(directResult), result);
  }

  // --- hash (generic MessageDigest) ---

  @Test
  public void testHashSha256() {
    // SHA-256("hello") known vector
    byte[] input = "hello".getBytes(StandardCharsets.UTF_8);
    byte[] result = Hash.hash(input, "SHA-256");
    String hex = Numeric.toHexStringNoPrefix(result);
    assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", hex);
  }

  @Test(expected = RuntimeException.class)
  public void testHashInvalidAlgorithm() {
    Hash.hash("test".getBytes(StandardCharsets.UTF_8), "NONEXISTENT");
  }

  // --- sha256 ---

  @Test
  public void testSha256() {
    byte[] input = "hello".getBytes(StandardCharsets.UTF_8);
    byte[] result = Hash.sha256(input);
    String hex = Numeric.toHexStringNoPrefix(result);
    assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", hex);
  }

  // --- hmacSha512 ---

  @Test
  public void testHmacSha512() {
    byte[] key = "key".getBytes(StandardCharsets.UTF_8);
    byte[] data = "data".getBytes(StandardCharsets.UTF_8);
    byte[] result = Hash.hmacSha512(key, data);
    assertEquals(64, result.length);
    assertNotNull(result);
  }

  // --- sha256hash160 ---

  @Test
  public void testSha256hash160() {
    byte[] input = "hello".getBytes(StandardCharsets.UTF_8);
    byte[] result = Hash.sha256hash160(input);
    assertEquals(20, result.length);
  }

  // --- blake2b256 ---

  @Test
  public void testBlake2b256() {
    byte[] input = "hello".getBytes(StandardCharsets.UTF_8);
    byte[] result = Hash.blake2b256(input);
    assertEquals(32, result.length);
    assertNotNull(result);
  }

  @Test
  public void testBlake2b256Empty() {
    byte[] result = Hash.blake2b256(new byte[0]);
    assertEquals(32, result.length);
  }
}
