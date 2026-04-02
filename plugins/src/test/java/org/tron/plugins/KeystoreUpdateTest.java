package org.tron.plugins;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.keystore.Credentials;
import org.tron.keystore.WalletUtils;
import picocli.CommandLine;

public class KeystoreUpdateTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testUpdatePassword() throws Exception {
    File dir = tempFolder.newFolder("keystore");
    String oldPassword = "oldpass123";
    String newPassword = "newpass456";

    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    byte[] originalKey = keyPair.getPrivateKey();
    String fileName = WalletUtils.generateWalletFile(oldPassword, keyPair, dir, true);

    // Read address from the generated file
    Credentials creds = WalletUtils.loadCredentials(oldPassword,
        new File(dir, fileName), true);
    String address = creds.getAddress();

    // Create password file with old + new passwords
    File pwFile = tempFolder.newFile("passwords.txt");
    Files.write(pwFile.toPath(),
        (oldPassword + "\n" + newPassword).getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "update", address,
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Exit code should be 0", 0, exitCode);

    // Verify: new password works
    Credentials updated = WalletUtils.loadCredentials(newPassword,
        new File(dir, fileName), true);
    assertArrayEquals("Key must survive password change",
        originalKey, updated.getSignInterface().getPrivateKey());
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

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "update", address,
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should fail with wrong password", 1, exitCode);

    // Verify: original password still works (file unchanged)
    Credentials unchanged = WalletUtils.loadCredentials(password,
        new File(dir, fileName), true);
    assertEquals(address, unchanged.getAddress());
  }

  @Test
  public void testUpdateNonExistentAddress() throws Exception {
    File dir = tempFolder.newFolder("keystore-noaddr");
    String password = "test123456";

    // Create a keystore so the dir isn't empty
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    WalletUtils.generateWalletFile(password, keyPair, dir, true);

    File pwFile = tempFolder.newFile("pw.txt");
    Files.write(pwFile.toPath(),
        ("test123456\nnewpass789").getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "update", "TNonExistentAddress123456789",
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should fail for non-existent address", 1, exitCode);
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

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "update", creds.getAddress(),
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should fail with short new password", 1, exitCode);
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

    // Password file with Windows line endings \r\n
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
}
