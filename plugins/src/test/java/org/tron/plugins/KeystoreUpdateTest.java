package org.tron.plugins;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Locale;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.keystore.Credentials;
import org.tron.keystore.WalletFile;
import org.tron.keystore.WalletUtils;
import picocli.CommandLine;

public class KeystoreUpdateTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  public void testUpdatePassword() throws Exception {
    File dir = tempFolder.newFolder("keystore");
    String oldPassword = "oldpass123";
    String newPassword = "newpass456";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    byte[] originalKey = keyPair.getPrivateKey();
    String fileName = WalletUtils.generateWalletFile(oldPassword, keyPair, dir, true);

    Credentials creds = WalletUtils.loadCredentials(oldPassword,
        new File(dir, fileName), true);
    String address = creds.getAddress();

    File pwFile = tempFolder.newFile("passwords.txt");
    Files.write(pwFile.toPath(),
        (oldPassword + "\n" + newPassword).getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "update", address,
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Exit code should be 0", 0, exitCode);

    // Verify: new password works and key survives
    Credentials updated = WalletUtils.loadCredentials(newPassword,
        new File(dir, fileName), true);
    assertArrayEquals("Key must survive password change",
        originalKey, updated.getSignInterface().getPrivateKey());

    // Verify: address field preserved in keystore JSON
    WalletFile wf = MAPPER.readValue(new File(dir, fileName), WalletFile.class);
    assertEquals("Address must be preserved in updated keystore",
        address, wf.getAddress());
  }

  @Test
  public void testUpdateWrongOldPassword() throws Exception {
    File dir = tempFolder.newFolder("keystore-bad");
    String password = "correct123";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String fileName = WalletUtils.generateWalletFile(password, keyPair, dir, true);

    Credentials creds = WalletUtils.loadCredentials(password,
        new File(dir, fileName), true);
    String address = creds.getAddress();

    File pwFile = tempFolder.newFile("wrong.txt");
    Files.write(pwFile.toPath(),
        ("wrongpass1\nnewpass456").getBytes(StandardCharsets.UTF_8));

    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "update", address,
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should fail with wrong password", 1, exitCode);
    assertTrue("Error should mention decryption",
        err.toString().contains("Decryption failed"));

    // Verify: original password still works (file unchanged)
    Credentials unchanged = WalletUtils.loadCredentials(password,
        new File(dir, fileName), true);
    assertEquals(address, unchanged.getAddress());
  }

  @Test
  public void testUpdateNonExistentAddress() throws Exception {
    File dir = tempFolder.newFolder("keystore-noaddr");
    String password = "test123456";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    WalletUtils.generateWalletFile(password, keyPair, dir, true);

    File pwFile = tempFolder.newFile("pw.txt");
    Files.write(pwFile.toPath(),
        ("test123456\nnewpass789").getBytes(StandardCharsets.UTF_8));

    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "update", "TNonExistentAddress123456789",
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should fail for non-existent address", 1, exitCode);
    assertTrue("Error should mention no keystore found",
        err.toString().contains("No keystore found for address"));
  }

  @Test
  public void testUpdateNewPasswordTooShort() throws Exception {
    File dir = tempFolder.newFolder("keystore-shortpw");
    String password = "test123456";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String fileName = WalletUtils.generateWalletFile(password, keyPair, dir, true);

    Credentials creds = WalletUtils.loadCredentials(password,
        new File(dir, fileName), true);

    File pwFile = tempFolder.newFile("shortpw.txt");
    Files.write(pwFile.toPath(),
        (password + "\nabc").getBytes(StandardCharsets.UTF_8));

    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "update", creds.getAddress(),
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should fail with short new password", 1, exitCode);
    assertTrue("Error should mention password length",
        err.toString().contains("at least 6 characters"));
  }

  @Test
  public void testUpdateWithWindowsLineEndings() throws Exception {
    File dir = tempFolder.newFolder("keystore-crlf");
    String oldPassword = "oldpass123";
    String newPassword = "newpass456";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    byte[] originalKey = keyPair.getPrivateKey();
    String fileName = WalletUtils.generateWalletFile(oldPassword, keyPair, dir, true);
    Credentials creds = WalletUtils.loadCredentials(oldPassword,
        new File(dir, fileName), true);

    File pwFile = tempFolder.newFile("crlf.txt");
    Files.write(pwFile.toPath(),
        (oldPassword + "\r\n" + newPassword + "\r\n").getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "update", creds.getAddress(),
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Update with CRLF password file should succeed", 0, exitCode);

    Credentials updated = WalletUtils.loadCredentials(newPassword,
        new File(dir, fileName), true);
    assertArrayEquals("Key must survive update with CRLF passwords",
        originalKey, updated.getSignInterface().getPrivateKey());
  }

  @Test
  public void testUpdateJsonOutput() throws Exception {
    File dir = tempFolder.newFolder("keystore-json");
    String oldPassword = "oldpass123";
    String newPassword = "newpass456";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String fileName = WalletUtils.generateWalletFile(oldPassword, keyPair, dir, true);
    Credentials creds = WalletUtils.loadCredentials(oldPassword,
        new File(dir, fileName), true);

    File pwFile = tempFolder.newFile("pw-json.txt");
    Files.write(pwFile.toPath(),
        (oldPassword + "\n" + newPassword).getBytes(StandardCharsets.UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setOut(new PrintWriter(out));
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "update", creds.getAddress(),
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath(),
        "--json");

    assertEquals(0, exitCode);
    String output = out.toString().trim();
    assertTrue("JSON should contain address",
        output.contains("\"address\""));
    assertTrue("JSON should contain status updated",
        output.contains("\"updated\""));
    assertTrue("JSON should contain file",
        output.contains("\"file\""));
  }

  @Test
  public void testUpdateWarnsOnCorruptedFile() throws Exception {
    File dir = tempFolder.newFolder("keystore-corrupt");
    String password = "test123456";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String fileName = WalletUtils.generateWalletFile(password, keyPair, dir, true);
    Credentials creds = WalletUtils.loadCredentials(password,
        new File(dir, fileName), true);

    Files.write(new File(dir, "corrupted.json").toPath(),
        "not valid json{{{".getBytes(StandardCharsets.UTF_8));

    File pwFile = tempFolder.newFile("pw-corrupt.txt");
    Files.write(pwFile.toPath(),
        (password + "\nnewpass789").getBytes(StandardCharsets.UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setOut(new PrintWriter(out));
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "update", creds.getAddress(),
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals(0, exitCode);
    assertTrue("Should warn about corrupted file",
        err.toString().contains("Warning: skipping unreadable file: corrupted.json"));
  }

  @Test
  public void testUpdatePasswordFileOnlyOneLine() throws Exception {
    File dir = tempFolder.newFolder("keystore-1line");
    String password = "test123456";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String fileName = WalletUtils.generateWalletFile(password, keyPair, dir, true);
    Credentials creds = WalletUtils.loadCredentials(password,
        new File(dir, fileName), true);

    File pwFile = tempFolder.newFile("oneline.txt");
    Files.write(pwFile.toPath(),
        "onlyoldpassword".getBytes(StandardCharsets.UTF_8));

    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "update", creds.getAddress(),
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should fail with single-line password file", 1, exitCode);
    assertTrue("Error should mention exactly two lines",
        err.toString().contains("exactly two lines"));
  }

  @Test
  public void testUpdatePasswordFileThreeLines() throws Exception {
    // Regression: a three-line password file (e.g. someone confusingly added a
    // confirm line, or pointed at the wrong file) must be rejected, not have
    // the third line silently discarded. The original keystore must remain
    // decryptable with the old password.
    File dir = tempFolder.newFolder("keystore-3line");
    String oldPassword = "oldpass123";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    byte[] originalKey = keyPair.getPrivateKey();
    String fileName = WalletUtils.generateWalletFile(oldPassword, keyPair, dir, true);
    Credentials creds = WalletUtils.loadCredentials(oldPassword,
        new File(dir, fileName), true);

    // Snapshot the keystore bytes so we can verify the file is untouched.
    byte[] beforeBytes = Files.readAllBytes(new File(dir, fileName).toPath());

    File pwFile = tempFolder.newFile("threeline.txt");
    Files.write(pwFile.toPath(),
        (oldPassword + "\nnewpass456\nnewpass456").getBytes(StandardCharsets.UTF_8));

    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "update", creds.getAddress(),
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should fail with three-line password file", 1, exitCode);
    assertTrue("Error should mention exactly two lines, got: " + err.toString(),
        err.toString().contains("exactly two lines"));

    // Verify: keystore file is byte-for-byte unchanged
    byte[] afterBytes = Files.readAllBytes(new File(dir, fileName).toPath());
    assertArrayEquals("Keystore file must not be modified on rejection",
        beforeBytes, afterBytes);

    // Verify: original password still decrypts the keystore
    Credentials unchanged = WalletUtils.loadCredentials(oldPassword,
        new File(dir, fileName), true);
    assertArrayEquals("Original key must still be recoverable with old password",
        originalKey, unchanged.getSignInterface().getPrivateKey());
  }

  @Test
  public void testUpdateNoTtyNoPasswordFile() throws Exception {
    File dir = tempFolder.newFolder("keystore-notty");
    String password = "test123456";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String fileName = WalletUtils.generateWalletFile(password, keyPair, dir, true);
    Credentials creds = WalletUtils.loadCredentials(password,
        new File(dir, fileName), true);

    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "update", creds.getAddress(),
        "--keystore-dir", dir.getAbsolutePath());

    assertEquals("Should fail when no TTY and no --password-file", 1, exitCode);
    assertTrue("Error should mention no terminal",
        err.toString().contains("No interactive terminal"));
  }

  @Test
  public void testUpdatePasswordFileNotFound() throws Exception {
    File dir = tempFolder.newFolder("keystore-nopwf");
    String password = "test123456";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String fileName = WalletUtils.generateWalletFile(password, keyPair, dir, true);
    Credentials creds = WalletUtils.loadCredentials(password,
        new File(dir, fileName), true);

    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "update", creds.getAddress(),
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", "/tmp/nonexistent-pw-update.txt");

    assertEquals("Should fail when password file not found", 1, exitCode);
    assertTrue("Error should mention file not found",
        err.toString().contains("Password file not found"));
  }

  @Test
  public void testUpdateSm2Keystore() throws Exception {
    File dir = tempFolder.newFolder("keystore-sm2");
    String oldPassword = "oldpass123";
    String newPassword = "newpass456";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), false);
    byte[] originalKey = keyPair.getPrivateKey();
    String fileName = WalletUtils.generateWalletFile(oldPassword, keyPair, dir, true);
    Credentials creds = WalletUtils.loadCredentials(oldPassword,
        new File(dir, fileName), false);

    File pwFile = tempFolder.newFile("pw-sm2.txt");
    Files.write(pwFile.toPath(),
        (oldPassword + "\n" + newPassword).getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "update", creds.getAddress(),
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath(),
        "--sm2");

    assertEquals("SM2 keystore update should succeed", 0, exitCode);

    Credentials updated = WalletUtils.loadCredentials(newPassword,
        new File(dir, fileName), false);
    assertArrayEquals("SM2 key must survive password change",
        originalKey, updated.getSignInterface().getPrivateKey());
  }

  @Test
  public void testUpdateMultipleKeystoresSameAddress() throws Exception {
    File dir = tempFolder.newFolder("keystore-multi");
    String password = "test123456";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String address = Credentials.create(keyPair).getAddress();

    // Create two keystores for the same address via direct API
    WalletUtils.generateWalletFile(password, keyPair, dir, true);
    // Small delay to get different filename timestamps
    Thread.sleep(50);
    WalletUtils.generateWalletFile(password, keyPair, dir, true);

    File pwFile = tempFolder.newFile("pw-multi.txt");
    Files.write(pwFile.toPath(),
        (password + "\nnewpass789").getBytes(StandardCharsets.UTF_8));

    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "update", address,
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should fail with multiple keystores for same address", 1, exitCode);
    assertTrue("Error should mention multiple keystores",
        err.toString().contains("Multiple keystores found"));
    assertTrue("Error should mention remove duplicates",
        err.toString().contains("remove duplicates"));
  }

  @Test
  public void testUpdatePasswordFileTooLarge() throws Exception {
    File dir = tempFolder.newFolder("keystore-bigpw");
    String password = "test123456";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String fileName = WalletUtils.generateWalletFile(password, keyPair, dir, true);
    Credentials creds = WalletUtils.loadCredentials(password,
        new File(dir, fileName), true);

    // Create a password file > 1KB
    File pwFile = tempFolder.newFile("bigpw.txt");
    byte[] bigContent = new byte[1025];
    java.util.Arrays.fill(bigContent, (byte) 'a');
    Files.write(pwFile.toPath(), bigContent);

    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "update", creds.getAddress(),
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should fail with large password file", 1, exitCode);
    assertTrue("Error should mention file too large",
        err.toString().contains("too large"));
  }

  @Test
  public void testUpdatePasswordFileWithBom() throws Exception {
    File dir = tempFolder.newFolder("keystore-bom");
    String oldPassword = "oldpass123";
    String newPassword = "newpass456";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    byte[] originalKey = keyPair.getPrivateKey();
    String fileName = WalletUtils.generateWalletFile(oldPassword, keyPair, dir, true);
    Credentials creds = WalletUtils.loadCredentials(oldPassword,
        new File(dir, fileName), true);

    // Password file with UTF-8 BOM
    File pwFile = tempFolder.newFile("bom.txt");
    Files.write(pwFile.toPath(),
        ("\uFEFF" + oldPassword + "\n" + newPassword).getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "update", creds.getAddress(),
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Update with BOM password file should succeed", 0, exitCode);

    Credentials updated = WalletUtils.loadCredentials(newPassword,
        new File(dir, fileName), true);
    assertArrayEquals("Key must survive update with BOM password file",
        originalKey, updated.getSignInterface().getPrivateKey());
  }

  @Test
  public void testUpdateNonExistentKeystoreDir() throws Exception {
    File dir = new File(tempFolder.getRoot(), "does-not-exist");

    File pwFile = tempFolder.newFile("pw-nodir.txt");
    Files.write(pwFile.toPath(),
        ("oldpass123\nnewpass456").getBytes(StandardCharsets.UTF_8));

    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "update", "TSomeAddress",
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals(1, exitCode);
    assertTrue("Error should mention no keystore found",
        err.toString().contains("No keystore found for address"));
  }

  @Test
  public void testUpdateKeystoreDirIsFile() throws Exception {
    File notADir = tempFolder.newFile("not-a-dir");

    File pwFile = tempFolder.newFile("pw-notdir.txt");
    Files.write(pwFile.toPath(),
        ("oldpass123\nnewpass456").getBytes(StandardCharsets.UTF_8));

    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "update", "TSomeAddress",
        "--keystore-dir", notADir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals(1, exitCode);
    assertTrue("Error should mention no keystore found",
        err.toString().contains("No keystore found for address"));
  }

  @Test
  public void testUpdateWithOldMacLineEndings() throws Exception {
    File dir = tempFolder.newFolder("keystore-cr");
    String oldPassword = "oldpass123";
    String newPassword = "newpass456";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    byte[] originalKey = keyPair.getPrivateKey();
    String fileName = WalletUtils.generateWalletFile(oldPassword, keyPair, dir, true);
    Credentials creds = WalletUtils.loadCredentials(oldPassword,
        new File(dir, fileName), true);

    // Password file with old Mac line endings (\r only)
    File pwFile = tempFolder.newFile("cr.txt");
    Files.write(pwFile.toPath(),
        (oldPassword + "\r" + newPassword + "\r").getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "update", creds.getAddress(),
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Update with old Mac CR line endings should succeed", 0, exitCode);

    Credentials updated = WalletUtils.loadCredentials(newPassword,
        new File(dir, fileName), true);
    assertArrayEquals("Key must survive update with CR passwords",
        originalKey, updated.getSignInterface().getPrivateKey());
  }

  @Test
  public void testUpdateSkipsInvalidVersionKeystores() throws Exception {
    File dir = tempFolder.newFolder("keystore-badver");
    String password = "test123456";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String address = Credentials.create(keyPair).getAddress();

    // Create a JSON file with correct address but wrong version
    String fakeKeystore = "{\"address\":\"" + address
        + "\",\"version\":2,\"crypto\":{\"cipher\":\"aes-128-ctr\"}}";
    Files.write(new File(dir, "fake.json").toPath(),
        fakeKeystore.getBytes(StandardCharsets.UTF_8));

    File pwFile = tempFolder.newFile("pw-badver.txt");
    Files.write(pwFile.toPath(),
        (password + "\nnewpass789").getBytes(StandardCharsets.UTF_8));

    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "update", address,
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should not find keystore with wrong version", 1, exitCode);
    assertTrue("Error should mention no keystore found",
        err.toString().contains("No keystore found"));
  }

  @Test
  public void testUpdateRejectsTamperedAddressKeystore() throws Exception {
    File dir = tempFolder.newFolder("keystore-tampered");
    String password = "test123456";

    // Create a real keystore, then tamper with the address field to simulate
    // a spoofed keystore that claims a different address than its encrypted key.
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String fileName = WalletUtils.generateWalletFile(password, keyPair, dir, true);
    File keystoreFile = new File(dir, fileName);

    String realAddress = Credentials.create(keyPair).getAddress();
    String spoofedAddress = "TSpoofedAddressXXXXXXXXXXXXXXXXXXXX";

    com.fasterxml.jackson.databind.ObjectMapper mapper =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature
                .FAIL_ON_UNKNOWN_PROPERTIES, false);
    org.tron.keystore.WalletFile wf = mapper.readValue(keystoreFile,
        org.tron.keystore.WalletFile.class);
    wf.setAddress(spoofedAddress);
    mapper.writeValue(keystoreFile, wf);

    File pwFile = tempFolder.newFile("pw-tampered.txt");
    Files.write(pwFile.toPath(),
        (password + "\nnewpass789").getBytes(StandardCharsets.UTF_8));

    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "update", spoofedAddress,
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should fail decryption on tampered address", 1, exitCode);
    assertTrue("Error should mention address mismatch, got: " + err.toString(),
        err.toString().contains("address mismatch"));
  }

  @Test
  public void testUpdatePreservesCorrectDerivedAddress() throws Exception {
    // After update, the keystore's address field should be the derived address,
    // not carried over from the original JSON (defense-in-depth against any
    // residual spoofed address that somehow passed decryption).
    File dir = tempFolder.newFolder("keystore-derived");
    String oldPassword = "oldpass123";
    String newPassword = "newpass456";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String fileName = WalletUtils.generateWalletFile(oldPassword, keyPair, dir, true);
    String originalAddress = Credentials.create(keyPair).getAddress();

    File pwFile = tempFolder.newFile("pw-derived.txt");
    Files.write(pwFile.toPath(),
        (oldPassword + "\n" + newPassword).getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "update", originalAddress,
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals(0, exitCode);

    // Verify updated file has the derived address
    com.fasterxml.jackson.databind.ObjectMapper mapper =
        new com.fasterxml.jackson.databind.ObjectMapper();
    org.tron.keystore.WalletFile wf = mapper.readValue(new File(dir, fileName),
        org.tron.keystore.WalletFile.class);
    assertEquals("Updated keystore address must match derived address",
        originalAddress, wf.getAddress());
  }

  @Test
  public void testUpdateNarrowsLoosePermissionsTo0600() throws Exception {
    // Adversarial test: pre-loosen the keystore to 0644, then verify that
    // update writes the file back with 0600. This exercises the temp-file
    // + atomic-rename path rather than merely preserving existing perms.
    String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
    org.junit.Assume.assumeTrue("POSIX permissions test, skip on Windows",
        !os.contains("win"));

    File dir = tempFolder.newFolder("keystore-perms");
    String oldPassword = "oldpass123";
    String newPassword = "newpass456";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String fileName = WalletUtils.generateWalletFile(oldPassword, keyPair, dir, true);
    Credentials creds = WalletUtils.loadCredentials(oldPassword,
        new File(dir, fileName), true);

    // Deliberately loosen to 0644 before update
    java.nio.file.Path keystorePath = new File(dir, fileName).toPath();
    java.nio.file.Files.setPosixFilePermissions(keystorePath,
        java.util.EnumSet.of(
            java.nio.file.attribute.PosixFilePermission.OWNER_READ,
            java.nio.file.attribute.PosixFilePermission.OWNER_WRITE,
            java.nio.file.attribute.PosixFilePermission.GROUP_READ,
            java.nio.file.attribute.PosixFilePermission.OTHERS_READ));

    File pwFile = tempFolder.newFile("pw-perms.txt");
    Files.write(pwFile.toPath(),
        (oldPassword + "\n" + newPassword).getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "update", creds.getAddress(),
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals(0, exitCode);

    // Verify the updated keystore file is now owner-only (0600), not 0644
    java.util.Set<java.nio.file.attribute.PosixFilePermission> perms =
        java.nio.file.Files.getPosixFilePermissions(keystorePath);
    assertEquals("Updated keystore must be narrowed to owner-only (rw-------)",
        java.util.EnumSet.of(
            java.nio.file.attribute.PosixFilePermission.OWNER_READ,
            java.nio.file.attribute.PosixFilePermission.OWNER_WRITE),
        perms);
  }

  @Test
  public void testUpdateLegacyTipFiresWhenPasswordHasWhitespace() throws Exception {
    // The legacy-truncation tip should fire when the entered old password
    // contains whitespace and decryption fails — the scenario that actually
    // matches the legacy bug.
    File dir = tempFolder.newFolder("keystore-tip-ws");
    String realPassword = "realpass123";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String fileName = WalletUtils.generateWalletFile(realPassword, keyPair, dir, true);
    Credentials creds = WalletUtils.loadCredentials(realPassword,
        new File(dir, fileName), true);

    // Password with internal whitespace that is NOT the real password
    File pwFile = tempFolder.newFile("pw-ws.txt");
    Files.write(pwFile.toPath(),
        ("correct horse battery staple\nnewpass789").getBytes(StandardCharsets.UTF_8));

    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "update", creds.getAddress(),
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals(1, exitCode);
    assertTrue("Legacy-truncation tip should fire for whitespace password, got: "
            + err.toString(),
        err.toString().contains("first whitespace-separated word"));
  }

  @Test
  public void testUpdateLegacyTipSuppressedWhenPasswordHasNoWhitespace() throws Exception {
    // For the common "wrong password" case (no whitespace), the legacy tip
    // would be noise — it should be suppressed.
    File dir = tempFolder.newFolder("keystore-tip-nows");
    String realPassword = "realpass123";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String fileName = WalletUtils.generateWalletFile(realPassword, keyPair, dir, true);
    Credentials creds = WalletUtils.loadCredentials(realPassword,
        new File(dir, fileName), true);

    // Wrong password with no whitespace
    File pwFile = tempFolder.newFile("pw-nows.txt");
    Files.write(pwFile.toPath(),
        ("wrongpassword\nnewpass789").getBytes(StandardCharsets.UTF_8));

    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "update", creds.getAddress(),
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals(1, exitCode);
    assertTrue("Decryption failure must still be reported",
        err.toString().contains("Decryption failed"));
    assertFalse("Legacy-truncation tip should NOT fire for whitespace-free password",
        err.toString().contains("first whitespace-separated word"));
  }

  @Test
  public void testUpdateScanSkipsSymlinkedEntry() throws Exception {
    org.junit.Assume.assumeTrue("Symlinks only tested on POSIX",
        !System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win"));

    File dir = tempFolder.newFolder("keystore-update-symlink");
    String oldPassword = "oldpass123";
    String newPassword = "newpass456";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    String fileName = WalletUtils.generateWalletFile(oldPassword, keyPair, dir, true);
    Credentials creds = WalletUtils.loadCredentials(oldPassword,
        new File(dir, fileName), true);

    File target = tempFolder.newFile("outside.json");
    Files.write(target.toPath(),
        "{\"not\":\"a keystore\"}".getBytes(StandardCharsets.UTF_8));
    File symlink = new File(dir, "evil.json");
    Files.createSymbolicLink(symlink.toPath(), target.toPath());

    File pwFile = tempFolder.newFile("pw-update-sym.txt");
    Files.write(pwFile.toPath(),
        (oldPassword + "\n" + newPassword).getBytes(StandardCharsets.UTF_8));

    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "update", creds.getAddress(),
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Update should succeed; symlinked entry must not break scan", 0, exitCode);
    assertTrue("Scan must warn about the symlinked entry, got: " + err.toString(),
        err.toString().contains("Warning: skipping symbolic link: evil.json"));
  }
}
