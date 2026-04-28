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
  public void testImportDuplicateAddressBlocked() throws Exception {
    File dir = tempFolder.newFolder("keystore-dup");
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String privateKeyHex = ByteArray.toHexString(keyPair.getPrivateKey());

    File keyFile = tempFolder.newFile("dup.key");
    Files.write(keyFile.toPath(), privateKeyHex.getBytes(StandardCharsets.UTF_8));
    File pwFile = tempFolder.newFile("pw-dup.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    // First import succeeds
    CommandLine cmd1 = new CommandLine(new Toolkit());
    assertEquals(0, cmd1.execute("keystore", "import",
        "--keystore-dir", dir.getAbsolutePath(),
        "--key-file", keyFile.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath()));

    // Second import of same key is blocked
    java.io.StringWriter err = new java.io.StringWriter();
    CommandLine cmd2 = new CommandLine(new Toolkit());
    cmd2.setErr(new java.io.PrintWriter(err));
    assertEquals("Duplicate import should be blocked", 1,
        cmd2.execute("keystore", "import",
            "--keystore-dir", dir.getAbsolutePath(),
            "--key-file", keyFile.getAbsolutePath(),
            "--password-file", pwFile.getAbsolutePath()));
    assertTrue("Error should mention already exists",
        err.toString().contains("already exists"));

    File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
    assertNotNull(files);
    assertEquals("Should still have only 1 keystore", 1, files.length);
  }

  @Test
  public void testImportDuplicateAddressWithForce() throws Exception {
    File dir = tempFolder.newFolder("keystore-force");
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String privateKeyHex = ByteArray.toHexString(keyPair.getPrivateKey());

    File keyFile = tempFolder.newFile("force.key");
    Files.write(keyFile.toPath(), privateKeyHex.getBytes(StandardCharsets.UTF_8));
    File pwFile = tempFolder.newFile("pw-force.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    // First import
    CommandLine cmd1 = new CommandLine(new Toolkit());
    assertEquals(0, cmd1.execute("keystore", "import",
        "--keystore-dir", dir.getAbsolutePath(),
        "--key-file", keyFile.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath()));

    // Second import with --force succeeds
    CommandLine cmd2 = new CommandLine(new Toolkit());
    assertEquals(0, cmd2.execute("keystore", "import",
        "--keystore-dir", dir.getAbsolutePath(),
        "--key-file", keyFile.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath(),
        "--force"));

    File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
    assertNotNull(files);
    assertEquals("Force import should create 2 files", 2, files.length);
  }

  @Test
  public void testImportKeyFileNotFound() throws Exception {
    File dir = tempFolder.newFolder("keystore-nokey");
    File pwFile = tempFolder.newFile("pw-nokey.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "import",
        "--keystore-dir", dir.getAbsolutePath(),
        "--key-file", "/tmp/nonexistent-key-file.txt",
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should fail when key file not found", 1, exitCode);
  }

  @Test
  public void testImportWith0xPrefix() throws Exception {
    File dir = tempFolder.newFolder("keystore-0x");
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String privateKeyHex = ByteArray.toHexString(keyPair.getPrivateKey());
    String expectedAddress = Credentials.create(keyPair).getAddress();

    File keyFile = tempFolder.newFile("0x.key");
    Files.write(keyFile.toPath(),
        ("0x" + privateKeyHex).getBytes(StandardCharsets.UTF_8));
    File pwFile = tempFolder.newFile("pw-0x.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "import",
        "--keystore-dir", dir.getAbsolutePath(),
        "--key-file", keyFile.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Import with 0x prefix should succeed", 0, exitCode);
    File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
    assertNotNull(files);
    assertEquals(1, files.length);
    Credentials creds = WalletUtils.loadCredentials("test123456", files[0], true);
    assertEquals("Address must match", expectedAddress, creds.getAddress());
  }

  @Test
  public void testImportWith0XUppercasePrefix() throws Exception {
    File dir = tempFolder.newFolder("keystore-0X");
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String privateKeyHex = ByteArray.toHexString(keyPair.getPrivateKey());

    File keyFile = tempFolder.newFile("0X.key");
    Files.write(keyFile.toPath(),
        ("0X" + privateKeyHex).getBytes(StandardCharsets.UTF_8));
    File pwFile = tempFolder.newFile("pw-0X.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "import",
        "--keystore-dir", dir.getAbsolutePath(),
        "--key-file", keyFile.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Import with 0X prefix should succeed", 0, exitCode);
  }

  @Test
  public void testImportWarnsOnCorruptedFile() throws Exception {
    File dir = tempFolder.newFolder("keystore-corrupt");
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String privateKeyHex = ByteArray.toHexString(keyPair.getPrivateKey());

    // Create a corrupted JSON in the keystore dir
    Files.write(new File(dir, "corrupted.json").toPath(),
        "not valid json{{{".getBytes(StandardCharsets.UTF_8));

    File keyFile = tempFolder.newFile("warn.key");
    Files.write(keyFile.toPath(), privateKeyHex.getBytes(StandardCharsets.UTF_8));
    File pwFile = tempFolder.newFile("pw-warn.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    java.io.StringWriter out = new java.io.StringWriter();
    java.io.StringWriter err = new java.io.StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setOut(new java.io.PrintWriter(out));
    cmd.setErr(new java.io.PrintWriter(err));
    int exitCode = cmd.execute("keystore", "import",
        "--keystore-dir", dir.getAbsolutePath(),
        "--key-file", keyFile.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals(0, exitCode);
    String errOutput = err.toString();
    assertTrue("Should warn about corrupted file",
        errOutput.contains("Warning: skipping unreadable file: corrupted.json"));
  }

  @Test
  public void testImportKeystoreFilePermissions() throws Exception {
    String os = System.getProperty("os.name").toLowerCase();
    org.junit.Assume.assumeTrue("POSIX permissions test, skip on Windows",
        !os.contains("win"));

    File dir = tempFolder.newFolder("keystore-perms");
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String privateKeyHex = ByteArray.toHexString(keyPair.getPrivateKey());

    File keyFile = tempFolder.newFile("perm.key");
    Files.write(keyFile.toPath(), privateKeyHex.getBytes(StandardCharsets.UTF_8));
    File pwFile = tempFolder.newFile("pw-perm.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "import",
        "--keystore-dir", dir.getAbsolutePath(),
        "--key-file", keyFile.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals(0, exitCode);
    File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
    assertNotNull(files);
    assertEquals(1, files.length);

    java.util.Set<java.nio.file.attribute.PosixFilePermission> perms =
        Files.getPosixFilePermissions(files[0].toPath());
    assertEquals("Keystore file should have owner-only permissions (rw-------)",
        java.util.EnumSet.of(
            java.nio.file.attribute.PosixFilePermission.OWNER_READ,
            java.nio.file.attribute.PosixFilePermission.OWNER_WRITE),
        perms);
  }

  @Test
  public void testImportRefusesSymlinkKeyFile() throws Exception {
    org.junit.Assume.assumeTrue("Symlinks only tested on POSIX",
        !System.getProperty("os.name").toLowerCase().contains("win"));

    File dir = tempFolder.newFolder("keystore-symlink");
    // Create a real key file and a symlink pointing to it
    File target = tempFolder.newFile("real.key");
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    Files.write(target.toPath(),
        ByteArray.toHexString(keyPair.getPrivateKey()).getBytes(StandardCharsets.UTF_8));

    File symlink = new File(tempFolder.getRoot(), "symlink.key");
    Files.createSymbolicLink(symlink.toPath(), target.toPath());

    File pwFile = tempFolder.newFile("pw-symlink.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    java.io.StringWriter err = new java.io.StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new java.io.PrintWriter(err));
    int exitCode = cmd.execute("keystore", "import",
        "--keystore-dir", dir.getAbsolutePath(),
        "--key-file", symlink.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Must refuse symlinked key file", 1, exitCode);
    assertTrue("Expected symlink-refusal error, got: " + err.toString(),
        err.toString().contains("Refusing to follow symbolic link"));
  }

  @Test
  public void testImportRefusesSymlinkPasswordFile() throws Exception {
    org.junit.Assume.assumeTrue("Symlinks only tested on POSIX",
        !System.getProperty("os.name").toLowerCase().contains("win"));

    File dir = tempFolder.newFolder("keystore-pwsymlink");
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    File keyFile = tempFolder.newFile("sym-pw.key");
    Files.write(keyFile.toPath(),
        ByteArray.toHexString(keyPair.getPrivateKey()).getBytes(StandardCharsets.UTF_8));

    File realPwFile = tempFolder.newFile("real-pw.txt");
    Files.write(realPwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));
    File pwSymlink = new File(tempFolder.getRoot(), "pw-symlink.txt");
    Files.createSymbolicLink(pwSymlink.toPath(), realPwFile.toPath());

    java.io.StringWriter err = new java.io.StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new java.io.PrintWriter(err));
    int exitCode = cmd.execute("keystore", "import",
        "--keystore-dir", dir.getAbsolutePath(),
        "--key-file", keyFile.getAbsolutePath(),
        "--password-file", pwSymlink.getAbsolutePath());

    assertEquals("Must refuse symlinked password file", 1, exitCode);
    assertTrue("Expected symlink-refusal error, got: " + err.toString(),
        err.toString().contains("Refusing to follow symbolic link"));
  }

  @Test
  public void testImportDuplicateCheckSkipsInvalidVersion() throws Exception {
    File dir = tempFolder.newFolder("keystore-badver");
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String privateKeyHex = ByteArray.toHexString(keyPair.getPrivateKey());
    String address = Credentials.create(keyPair).getAddress();

    // Create a JSON with correct address but wrong version — should NOT count as duplicate
    String fakeKeystore = "{\"address\":\"" + address
        + "\",\"version\":2,\"crypto\":{\"cipher\":\"aes-128-ctr\"}}";
    Files.write(new File(dir, "fake.json").toPath(),
        fakeKeystore.getBytes(StandardCharsets.UTF_8));

    File keyFile = tempFolder.newFile("ver.key");
    Files.write(keyFile.toPath(), privateKeyHex.getBytes(StandardCharsets.UTF_8));
    File pwFile = tempFolder.newFile("pw-ver.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "import",
        "--keystore-dir", dir.getAbsolutePath(),
        "--key-file", keyFile.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Import should succeed — invalid-version file is not a real duplicate", 0,
        exitCode);
  }

  @Test
  public void testImportDuplicateScanSkipsSymlinkedEntry() throws Exception {
    org.junit.Assume.assumeTrue("Symlinks only tested on POSIX",
        !System.getProperty("os.name").toLowerCase().contains("win"));

    File dir = tempFolder.newFolder("keystore-dup-symlink");

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String privateKeyHex = ByteArray.toHexString(keyPair.getPrivateKey());

    File target = tempFolder.newFile("outside.json");
    Files.write(target.toPath(),
        "{\"not\":\"a keystore\"}".getBytes(StandardCharsets.UTF_8));
    File symlink = new File(dir, "evil.json");
    Files.createSymbolicLink(symlink.toPath(), target.toPath());

    File keyFile = tempFolder.newFile("dup-sym.key");
    Files.write(keyFile.toPath(),
        privateKeyHex.getBytes(StandardCharsets.UTF_8));
    File pwFile = tempFolder.newFile("pw-dup-sym.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    java.io.StringWriter err = new java.io.StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new java.io.PrintWriter(err));
    int exitCode = cmd.execute("keystore", "import",
        "--keystore-dir", dir.getAbsolutePath(),
        "--key-file", keyFile.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Import should succeed with symlink present", 0, exitCode);
    assertTrue("Duplicate scan must warn about the symlinked entry, got: "
            + err.toString(),
        err.toString().contains("Warning: skipping symbolic link: evil.json"));
  }

  @Test
  public void testImportRejectsMultiLinePasswordFile() throws Exception {
    // Regression: a user might accidentally point --password-file at a
    // `keystore update` two-line file; without the guard that literal
    // "old\nnew" becomes the password.
    File dir = tempFolder.newFolder("keystore-multi-pw");
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String privateKeyHex = ByteArray.toHexString(keyPair.getPrivateKey());

    File keyFile = tempFolder.newFile("multi.key");
    Files.write(keyFile.toPath(), privateKeyHex.getBytes(StandardCharsets.UTF_8));
    File pwFile = tempFolder.newFile("multi-pw.txt");
    Files.write(pwFile.toPath(),
        "oldpass123\nnewpass456".getBytes(StandardCharsets.UTF_8));

    java.io.StringWriter err = new java.io.StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new java.io.PrintWriter(err));
    int exitCode = cmd.execute("keystore", "import",
        "--keystore-dir", dir.getAbsolutePath(),
        "--key-file", keyFile.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should reject multi-line password file", 1, exitCode);
    assertTrue("Error must explain the multi-line rejection, got: " + err.toString(),
        err.toString().contains("multiple lines"));
    File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
    assertTrue("No keystore should have been created",
        files == null || files.length == 0);
  }
}
