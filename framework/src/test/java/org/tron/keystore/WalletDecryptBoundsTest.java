package org.tron.keystore;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.common.utils.Utils;
import org.tron.core.exception.CipherException;

/**
 * Verifies that untrusted keystore KDF cost parameters are bounded before
 * any key-derivation work happens. A hostile keystore previously drove
 * Bouncy Castle into {@code NegativeArraySizeException} (huge scrypt n),
 * a ~4 GiB allocation / OOM (n=2^22, r=8), {@code ArithmeticException}
 * (r*n overflow), or a multi-hour pbkdf2 CPU hang. All of these must now
 * be rejected by {@link Wallet#validate(WalletFile)} (which is called at
 * the top of {@link Wallet#decrypt}), never reaching the KDF engine.
 *
 * <p>All rejection tests are fast by construction: validation performs no
 * KDF execution, and no test ever invokes decrypt with scrypt parameters
 * that pass the bounds checks (except the deliberately corrupted
 * low-cost keystore used for the CipherException wrap test).
 */
public class WalletDecryptBoundsTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private static final String SCRYPT_TEMPLATE =
      "{\"version\":3,\"address\":\"addr\",\"id\":\"test\",\"crypto\":{"
          + "\"cipher\":\"aes-128-ctr\",\"ciphertext\":\"00\","
          + "\"cipherparams\":{\"iv\":\"00\"},\"mac\":\"00\",\"kdf\":\"scrypt\","
          + "\"kdfparams\":{\"dklen\":%d,\"n\":%d,\"r\":%d,\"p\":%d,\"salt\":\"00\"}}}";

  private static final String PBKDF2_TEMPLATE =
      "{\"version\":3,\"address\":\"addr\",\"id\":\"test\",\"crypto\":{"
          + "\"cipher\":\"aes-128-ctr\",\"ciphertext\":\"00\","
          + "\"cipherparams\":{\"iv\":\"00\"},\"mac\":\"00\",\"kdf\":\"pbkdf2\","
          + "\"kdfparams\":{\"dklen\":%d,\"c\":%d,\"prf\":\"hmac-sha256\",\"salt\":\"00\"}}}";

  private static WalletFile parseKeystore(String json) throws Exception {
    return OBJECT_MAPPER.readValue(json, WalletFile.class);
  }

  private static CipherException validateFailure(String json) throws Exception {
    WalletFile walletFile = parseKeystore(json);
    return assertThrows(CipherException.class, () -> Wallet.validate(walletFile));
  }

  // ---------- hostile scrypt n ----------

  @Test
  public void testValidateRejectsScryptNNegativeArrayCase() throws Exception {
    // n=2^23 previously crashed inside SCrypt.generate with
    // NegativeArraySizeException; must be rejected before the KDF runs.
    CipherException err = validateFailure(String.format(SCRYPT_TEMPLATE,
        32, 1 << 23, 8, 1));
    assertTrue(err.getMessage(), err.getMessage().contains("n"));
  }

  @Test
  public void testValidateRejectsScryptNOomCase() throws Exception {
    // n=2^22, r=8 requests 128*r*n = 4 GiB and previously died with OOM;
    // validation must reject it (fast, no allocation).
    long start = System.nanoTime();
    CipherException err = validateFailure(String.format(SCRYPT_TEMPLATE,
        32, 1 << 22, 8, 1));
    assertTrue(err.getMessage(), err.getMessage().contains("n"));
    // Rejection is pure validation: must be far cheaper than any KDF run.
    assertTrue("rejection must happen before allocation",
        System.nanoTime() - start < 5_000_000_000L);
  }

  @Test
  public void testValidateRejectsScryptNBelowFloor() throws Exception {
    CipherException err = validateFailure(String.format(SCRYPT_TEMPLATE,
        32, 1 << 11, 8, 1));
    assertTrue(err.getMessage(), err.getMessage().contains("Scrypt n"));
  }

  @Test
  public void testValidateRejectsScryptNAboveCap() throws Exception {
    CipherException err = validateFailure(String.format(SCRYPT_TEMPLATE,
        32, 1 << 21, 8, 1));
    assertTrue(err.getMessage(), err.getMessage().contains("Scrypt n"));
  }

  @Test
  public void testValidateRejectsScryptNNotPowerOfTwo() throws Exception {
    CipherException err = validateFailure(String.format(SCRYPT_TEMPLATE,
        32, 12345, 8, 1));
    assertTrue(err.getMessage(), err.getMessage().contains("Scrypt n"));
  }

  // ---------- hostile scrypt r / p / dklen ----------

  @Test
  public void testValidateRejectsScryptRArithmeticCase() throws Exception {
    // r=2^24 with n*r overflow previously crashed inside
    // SCrypt.generate with ArithmeticException.
    CipherException err = validateFailure(String.format(SCRYPT_TEMPLATE,
        32, 1 << 13, 1 << 24, 1));
    assertTrue(err.getMessage(), err.getMessage().contains("Scrypt r"));
  }

  @Test
  public void testValidateRejectsScryptPAboveCap() throws Exception {
    CipherException err = validateFailure(String.format(SCRYPT_TEMPLATE,
        32, 1 << 13, 8, 9));
    assertTrue(err.getMessage(), err.getMessage().contains("Scrypt p"));
  }

  @Test
  public void testValidateRejectsOversizedDklen() throws Exception {
    CipherException err = validateFailure(String.format(SCRYPT_TEMPLATE,
        4096, 1 << 13, 8, 1));
    assertTrue(err.getMessage(), err.getMessage().contains("Scrypt dklen"));
  }

  @Test
  public void testValidateRejectsScryptDklenBelowMacFloor() throws Exception {
    // dklen=16 passes the old < 1 sanity floor but is too short for
    // generateMac (needs derivedKey[16..32)) — validation must reject it
    // before any KDF runs, otherwise a raw ArrayIndexOutOfBoundsException
    // escapes decrypt().
    CipherException err = validateFailure(String.format(SCRYPT_TEMPLATE,
        16, 1 << 12, 1, 1));
    assertTrue(err.getMessage(), err.getMessage().contains("Scrypt dklen"));
    assertTrue(err.getMessage(), err.getMessage().contains("[32"));
  }

  // ---------- missing kdfparams ----------
  //
  // Note: a keystore JSON that omits "kdfparams" entirely fails at
  // deserialization (Jackson external type id), which is already a safe
  // rejection. The validation chokepoint itself is exercised here via
  // programmatically built WalletFile objects with a null kdfparams.

  private static WalletFile walletWithKdfparams(
      String kdf, WalletFile.KdfParams kdfParams) {
    WalletFile walletFile = new WalletFile();
    walletFile.setVersion(3);
    walletFile.setAddress("addr");
    WalletFile.Crypto crypto = new WalletFile.Crypto();
    crypto.setCipher("aes-128-ctr");
    crypto.setCiphertext("00");
    WalletFile.CipherParams cipherParams = new WalletFile.CipherParams();
    cipherParams.setIv("00");
    crypto.setCipherparams(cipherParams);
    crypto.setMac("00");
    crypto.setKdf(kdf);
    crypto.setKdfparams(kdfParams);
    walletFile.setCrypto(crypto);
    return walletFile;
  }

  @Test
  public void testValidateToleratesMissingScryptKdfparams() {
    // Structural stubs without kdfparams are tolerated (KeystoreCliUtilsTest
    // fixture contract); decrypt() still fails safely because the
    // ClassCastException (RuntimeException) is wrapped into CipherException.
    WalletFile walletFile = walletWithKdfparams("scrypt", null);
    try {
      Wallet.validate(walletFile);
    } catch (CipherException e) {
      fail("missing kdfparams must be tolerated, got: " + e.getMessage());
    }
    assertThrows(CipherException.class,
        () -> Wallet.decrypt("password123", walletFile, true));
  }

  @Test
  public void testValidateToleratesMissingPbkdf2Kdfparams() {
    WalletFile walletFile = walletWithKdfparams("pbkdf2", null);
    try {
      Wallet.validate(walletFile);
    } catch (CipherException e) {
      fail("missing kdfparams must be tolerated, got: " + e.getMessage());
    }
    assertThrows(CipherException.class,
        () -> Wallet.decrypt("password123", walletFile, true));
  }

  // ---------- hostile pbkdf2 ----------

  @Test
  public void testValidateRejectsPbkdf2MaxIterations() throws Exception {
    // c=2147483647 is a multi-hour CPU hang; must be rejected up front.
    CipherException err = validateFailure(String.format(PBKDF2_TEMPLATE,
        32, Integer.MAX_VALUE));
    assertTrue(err.getMessage(), err.getMessage().contains("Pbkdf2 iteration count"));
  }

  @Test
  public void testValidateRejectsPbkdf2OversizedDklen() throws Exception {
    CipherException err = validateFailure(String.format(PBKDF2_TEMPLATE,
        4096, 262144));
    assertTrue(err.getMessage(), err.getMessage().contains("Pbkdf2 dklen"));
  }

  // ---------- behavior freeze: legitimate keystores still pass ----------

  @Test
  public void testValidateAcceptsStandardDefaults() throws Exception {
    // Wallet.createStandard: n=2^18, r=8, p=1, dklen=32.
    WalletFile standard = parseKeystore(String.format(SCRYPT_TEMPLATE,
        32, 1 << 18, 8, 1));
    Wallet.validate(standard);
    assertTrue(Wallet.isValidKeystoreFile(standard));

    // Wallet.createLight: n=2^12, r=8, p=6, dklen=32.
    WalletFile light = parseKeystore(String.format(SCRYPT_TEMPLATE,
        32, 1 << 12, 8, 6));
    Wallet.validate(light);
    assertTrue(Wallet.isValidKeystoreFile(light));
  }

  @Test
  public void testValidateAcceptsUpperBoundaryParams() throws Exception {
    // n=2^20, r=8, p=1 sits exactly at the cap (128*r*n = 1 GiB) and is a
    // sane (if heavy) generator choice: validation must accept it. The
    // test only asserts validation — the actual scrypt run is never
    // executed here.
    WalletFile boundary = parseKeystore(String.format(SCRYPT_TEMPLATE,
        128, 1 << 20, 8, 1));
    Wallet.validate(boundary);
    assertTrue(Wallet.isValidKeystoreFile(boundary));
  }

  @Test
  public void testValidateAcceptsPbkdf2Defaults() throws Exception {
    WalletFile walletFile = parseKeystore(String.format(PBKDF2_TEMPLATE,
        32, 262144));
    Wallet.validate(walletFile);
    assertTrue(Wallet.isValidKeystoreFile(walletFile));
  }

  // ---------- decrypt wrap: in-bounds but corrupted keystore ----------

  @Test
  public void testDecryptCorruptedKeystoreThrowsCipherException() throws Exception {
    // In-bounds low-cost scrypt params (fast), but tampered MAC:
    // decrypt must fail with a clean CipherException, never leak a raw
    // RuntimeException stack.
    String json = String.format(SCRYPT_TEMPLATE, 32, 1 << 12, 1, 1)
        .replace("\"mac\":\"00\"",
            "\"mac\":\"0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20\"");
    WalletFile walletFile = parseKeystore(json);
    Wallet.validate(walletFile);

    CipherException err = assertThrows(CipherException.class,
        () -> Wallet.decrypt("password123", walletFile, true));
    assertEquals("Invalid password provided", err.getMessage());
  }

  // ---------- create(): KDF params validated symmetric with decrypt() ----------

  @Test
  public void testCreateRejectsOutOfBoundsKdfParams() {
    // p=16 exceeds the SCRYPT_P_MAX=8 bound that validate()/decrypt() enforce,
    // so create(n=2^18, p=16) would previously produce a wallet file that only
    // this class would later refuse to decrypt. Must throw before any KDF run.
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom(), true);

    CipherException err = assertThrows(CipherException.class,
        () -> Wallet.create("password123", keyPair, 1 << 18, 16));
    assertTrue(err.getMessage(), err.getMessage().contains("Scrypt p"));
  }

  @Test(timeout = 60000)
  public void testCreateAcceptsInBoundsKdfParams() throws Exception {
    // n=2^12 (4096), p=1 is well within the enforced bounds; create must
    // succeed and the resulting file must pass validate() and decrypt().
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom(), true);
    byte[] originalKey = keyPair.getPrivateKey();

    WalletFile walletFile = Wallet.create("password123", keyPair, 1 << 12, 1);
    Wallet.validate(walletFile);

    SignInterface recovered = Wallet.decrypt("password123", walletFile, true);
    assertArrayEquals(originalKey, recovered.getPrivateKey());
  }
}
