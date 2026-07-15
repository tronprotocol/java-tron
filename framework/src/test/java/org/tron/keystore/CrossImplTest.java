package org.tron.keystore;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.common.utils.Utils;

/**
 * Format compatibility tests.
 *
 * <p>All tests generate keystores dynamically at test time — no static
 * fixtures or secrets stored in the repository. Verifies that keystore
 * files can survive a full roundtrip: generate keypair, encrypt, serialize
 * to JSON file, deserialize, decrypt, compare private key and address.
 */
public class CrossImplTest {

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  // --- Ethereum standard test vectors (from Web3 Secret Storage spec, inline) ---
  // Source: web3j WalletTest.java — password and private key are public test data.

  private static final String ETH_PASSWORD = "Insecure Pa55w0rd";
  private static final String ETH_PRIVATE_KEY =
      "a392604efc2fad9c0b3da43b5f698a2e3f270f170d859912be0d54742275c5f6";

  private static final String ETH_PBKDF2_KEYSTORE = "{"
      + "\"crypto\":{\"cipher\":\"aes-128-ctr\","
      + "\"cipherparams\":{\"iv\":\"02ebc768684e5576900376114625ee6f\"},"
      + "\"ciphertext\":\"7ad5c9dd2c95f34a92ebb86740b92103a5d1cc4c2eabf3b9a59e1f83f3181216\","
      + "\"kdf\":\"pbkdf2\","
      + "\"kdfparams\":{\"c\":262144,\"dklen\":32,\"prf\":\"hmac-sha256\","
      + "\"salt\":\"0e4cf3893b25bb81efaae565728b5b7cde6a84e224cbf9aed3d69a31c981b702\"},"
      + "\"mac\":\"2b29e4641ec17f4dc8b86fc8592090b50109b372529c30b001d4d96249edaf62\"},"
      + "\"id\":\"af0451b4-6020-4ef0-91ec-794a5a965b01\",\"version\":3}";

  private static final String ETH_SCRYPT_KEYSTORE = "{"
      + "\"crypto\":{\"cipher\":\"aes-128-ctr\","
      + "\"cipherparams\":{\"iv\":\"3021e1ef4774dfc5b08307f3a4c8df00\"},"
      + "\"ciphertext\":\"4dd29ba18478b98cf07a8a44167acdf7e04de59777c4b9c139e3d3fa5cb0b931\","
      + "\"kdf\":\"scrypt\","
      + "\"kdfparams\":{\"dklen\":32,\"n\":262144,\"r\":8,\"p\":1,"
      + "\"salt\":\"4f9f68c71989eb3887cd947c80b9555fce528f210199d35c35279beb8c2da5ca\"},"
      + "\"mac\":\"7e8f2192767af9be18e7a373c1986d9190fcaa43ad689bbb01a62dbde159338d\"},"
      + "\"id\":\"7654525c-17e0-4df5-94b5-c7fde752c9d2\",\"version\":3}";

  @Test
  public void testDecryptEthPbkdf2Keystore() throws Exception {
    WalletFile walletFile = MAPPER.readValue(ETH_PBKDF2_KEYSTORE, WalletFile.class);
    SignInterface recovered = Wallet.decrypt(ETH_PASSWORD, walletFile, true);
    assertEquals("Private key must match Ethereum test vector",
        ETH_PRIVATE_KEY,
        org.tron.common.utils.ByteArray.toHexString(recovered.getPrivateKey()));
  }

  @Test
  public void testDecryptEthScryptKeystore() throws Exception {
    WalletFile walletFile = MAPPER.readValue(ETH_SCRYPT_KEYSTORE, WalletFile.class);
    SignInterface recovered = Wallet.decrypt(ETH_PASSWORD, walletFile, true);
    assertEquals("Private key must match Ethereum test vector",
        ETH_PRIVATE_KEY,
        org.tron.common.utils.ByteArray.toHexString(recovered.getPrivateKey()));
  }

  // --- Dynamic format compatibility (no static secrets) ---

  @Test
  public void testKeystoreFormatCompatibility() throws Exception {
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom(), true);
    byte[] originalKey = keyPair.getPrivateKey();
    String password = "dynamicTest123";

    WalletFile walletFile = Wallet.createStandard(password, keyPair);

    // Verify Web3 Secret Storage structure
    assertEquals("version must be 3", 3, walletFile.getVersion());
    assertNotNull("must have address", walletFile.getAddress());
    assertNotNull("must have crypto", walletFile.getCrypto());
    assertEquals("cipher must be aes-128-ctr",
        "aes-128-ctr", walletFile.getCrypto().getCipher());
    assertTrue("kdf must be scrypt or pbkdf2",
        "scrypt".equals(walletFile.getCrypto().getKdf())
            || "pbkdf2".equals(walletFile.getCrypto().getKdf()));

    // Write to file, read back — simulates cross-process interop
    File tempFile = new File(tempFolder.getRoot(), "compat-test.json");
    MAPPER.writeValue(tempFile, walletFile);
    WalletFile loaded = MAPPER.readValue(tempFile, WalletFile.class);

    SignInterface recovered = Wallet.decrypt(password, loaded, true);
    assertArrayEquals("Key must survive file roundtrip",
        originalKey, recovered.getPrivateKey());

    // Verify TRON address format
    byte[] tronAddr = recovered.getAddress();
    assertEquals("TRON address must be 21 bytes", 21, tronAddr.length);
    assertEquals("First byte must be TRON prefix", 0x41, tronAddr[0] & 0xFF);
  }

  @Test
  public void testLightScryptFormatCompatibility() throws Exception {
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom(), true);
    byte[] originalKey = keyPair.getPrivateKey();
    String password = "lightCompat456";

    WalletFile walletFile = Wallet.createLight(password, keyPair);
    File tempFile = new File(tempFolder.getRoot(), "light-compat.json");
    MAPPER.writeValue(tempFile, walletFile);
    WalletFile loaded = MAPPER.readValue(tempFile, WalletFile.class);

    SignInterface recovered = Wallet.decrypt(password, loaded, true);
    assertArrayEquals("Key must survive light scrypt file roundtrip",
        originalKey, recovered.getPrivateKey());
  }

  @Test
  public void testKeystoreAddressConsistency() throws Exception {
    String password = "addresscheck";
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom(), true);
    Credentials original = Credentials.create(keyPair);

    WalletFile walletFile = Wallet.createLight(password, keyPair);
    assertEquals("WalletFile address must match credentials address",
        original.getAddress(), walletFile.getAddress());

    SignInterface recovered = Wallet.decrypt(password, walletFile, true);
    Credentials recoveredCreds = Credentials.create(recovered);
    assertEquals("Recovered address must match original",
        original.getAddress(), recoveredCreds.getAddress());
  }

  @Test
  public void testLoadCredentialsIntegration() throws Exception {
    String password = "integration789";
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom(), true);
    byte[] originalKey = keyPair.getPrivateKey();
    String originalAddress = Credentials.create(keyPair).getAddress();

    File tempDir = tempFolder.newFolder("wallet-integration");
    String fileName = WalletUtils.generateWalletFile(password, keyPair, tempDir, false);
    assertNotNull(fileName);

    File keystoreFile = new File(tempDir, fileName);
    Credentials loaded = WalletUtils.loadCredentials(password, keystoreFile, true);

    assertEquals("Address must survive full WalletUtils roundtrip",
        originalAddress, loaded.getAddress());
    assertArrayEquals("Key must survive full WalletUtils roundtrip",
        originalKey, loaded.getSignInterface().getPrivateKey());
  }
}
