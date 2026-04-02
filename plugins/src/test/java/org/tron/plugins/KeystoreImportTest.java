package org.tron.plugins;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.common.utils.ByteArray;
import org.tron.keystore.Credentials;
import org.tron.keystore.WalletUtils;
import picocli.CommandLine;

public class KeystoreImportTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testImportWithKeyFileAndPasswordFile() throws Exception {
    File dir = tempFolder.newFolder("keystore");

    // Generate a known private key
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String privateKeyHex = ByteArray.toHexString(keyPair.getPrivateKey());
    String expectedAddress = Credentials.create(keyPair).getAddress();

    File keyFile = tempFolder.newFile("private.key");
    Files.write(keyFile.toPath(), privateKeyHex.getBytes(StandardCharsets.UTF_8));

    File pwFile = tempFolder.newFile("password.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "import",
        "--keystore-dir", dir.getAbsolutePath(),
        "--key-file", keyFile.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Exit code should be 0", 0, exitCode);

    File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
    assertNotNull(files);
    assertEquals(1, files.length);

    // Verify roundtrip: decrypt should recover the same private key
    Credentials creds = WalletUtils.loadCredentials("test123456", files[0], true);
    assertEquals("Address must match", expectedAddress, creds.getAddress());
    assertArrayEquals("Private key must survive import roundtrip",
        keyPair.getPrivateKey(), creds.getSignInterface().getPrivateKey());
  }

  @Test
  public void testImportInvalidKeyTooShort() throws Exception {
    File dir = tempFolder.newFolder("keystore-bad");
    File keyFile = tempFolder.newFile("bad.key");
    Files.write(keyFile.toPath(), "abcdef1234".getBytes(StandardCharsets.UTF_8));

    File pwFile = tempFolder.newFile("pw.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "import",
        "--keystore-dir", dir.getAbsolutePath(),
        "--key-file", keyFile.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should fail with invalid key", 1, exitCode);
  }

  @Test
  public void testImportInvalidKeyNonHex() throws Exception {
    File dir = tempFolder.newFolder("keystore-hex");
    File keyFile = tempFolder.newFile("nonhex.key");
    Files.write(keyFile.toPath(),
        "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz"
            .getBytes(StandardCharsets.UTF_8));

    File pwFile = tempFolder.newFile("pw.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "import",
        "--keystore-dir", dir.getAbsolutePath(),
        "--key-file", keyFile.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should fail with non-hex key", 1, exitCode);
  }

  @Test
  public void testImportNoTtyNoKeyFile() throws Exception {
    File dir = tempFolder.newFolder("keystore-notty");
    File pwFile = tempFolder.newFile("pw2.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    // No --key-file and System.console() is null in CI
    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "import",
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should fail when no TTY and no --key-file", 1, exitCode);
  }

  @Test
  public void testImportWithSm2() throws Exception {
    File dir = tempFolder.newFolder("keystore-sm2");
    // SM2 uses same 32-byte private key format
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), false);
    String privateKeyHex = ByteArray.toHexString(keyPair.getPrivateKey());

    File keyFile = tempFolder.newFile("sm2.key");
    Files.write(keyFile.toPath(), privateKeyHex.getBytes(StandardCharsets.UTF_8));
    File pwFile = tempFolder.newFile("pw-sm2.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "import",
        "--keystore-dir", dir.getAbsolutePath(),
        "--key-file", keyFile.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath(),
        "--sm2");

    assertEquals("SM2 import should succeed", 0, exitCode);
    File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
    assertNotNull(files);
    assertEquals(1, files.length);

    // Verify SM2 keystore can be decrypted
    Credentials creds = WalletUtils.loadCredentials("test123456", files[0], false);
    assertArrayEquals("SM2 key must survive import roundtrip",
        keyPair.getPrivateKey(), creds.getSignInterface().getPrivateKey());
  }

  @Test
  public void testImportKeyFileWithWhitespace() throws Exception {
    File dir = tempFolder.newFolder("keystore-ws");
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String privateKeyHex = ByteArray.toHexString(keyPair.getPrivateKey());

    // Key file with leading/trailing whitespace and newlines
    File keyFile = tempFolder.newFile("ws.key");
    Files.write(keyFile.toPath(),
        ("  " + privateKeyHex + "  \n\n").getBytes(StandardCharsets.UTF_8));
    File pwFile = tempFolder.newFile("pw-ws.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "import",
        "--keystore-dir", dir.getAbsolutePath(),
        "--key-file", keyFile.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Import with whitespace-padded key should succeed", 0, exitCode);

    File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
    assertNotNull(files);
    assertEquals(1, files.length);
    Credentials creds = WalletUtils.loadCredentials("test123456", files[0], true);
    assertArrayEquals("Key must survive whitespace-trimmed import",
        keyPair.getPrivateKey(), creds.getSignInterface().getPrivateKey());
  }

  @Test
  public void testImportDuplicateAddress() throws Exception {
    File dir = tempFolder.newFolder("keystore-dup");
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String privateKeyHex = ByteArray.toHexString(keyPair.getPrivateKey());

    File keyFile = tempFolder.newFile("dup.key");
    Files.write(keyFile.toPath(), privateKeyHex.getBytes(StandardCharsets.UTF_8));
    File pwFile = tempFolder.newFile("pw-dup.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    // Import same key twice
    CommandLine cmd1 = new CommandLine(new Toolkit());
    assertEquals(0, cmd1.execute("keystore", "import",
        "--keystore-dir", dir.getAbsolutePath(),
        "--key-file", keyFile.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath()));

    CommandLine cmd2 = new CommandLine(new Toolkit());
    assertEquals(0, cmd2.execute("keystore", "import",
        "--keystore-dir", dir.getAbsolutePath(),
        "--key-file", keyFile.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath()));

    // Should create two separate files (timestamped names differ)
    File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
    assertNotNull(files);
    assertEquals("Duplicate import should create 2 separate files", 2, files.length);
  }
}
