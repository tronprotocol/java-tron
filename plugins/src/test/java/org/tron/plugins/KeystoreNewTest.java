package org.tron.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.keystore.Credentials;
import org.tron.keystore.WalletUtils;
import picocli.CommandLine;

public class KeystoreNewTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testNewKeystoreWithPasswordFile() throws Exception {
    File dir = tempFolder.newFolder("keystore");
    File pwFile = tempFolder.newFile("password.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setOut(new PrintWriter(out));
    cmd.setErr(new PrintWriter(err));

    int exitCode = cmd.execute("keystore", "new",
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Exit code should be 0", 0, exitCode);

    File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
    assertNotNull(files);
    assertEquals("Should create exactly one keystore file", 1, files.length);

    // Verify the file is a valid keystore
    Credentials creds = WalletUtils.loadCredentials("test123456", files[0], true);
    assertNotNull(creds.getAddress());
    assertTrue(creds.getAddress().startsWith("T"));
  }

  @Test
  public void testNewKeystoreJsonOutput() throws Exception {
    File dir = tempFolder.newFolder("keystore-json");
    File pwFile = tempFolder.newFile("password-json.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setOut(new PrintWriter(out));
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "new",
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath(),
        "--json");

    assertEquals(0, exitCode);
    String output = out.toString().trim();
    assertTrue("JSON output should contain address",
        output.contains("\"address\""));
    assertTrue("JSON output should contain file",
        output.contains("\"file\""));
  }

  @Test
  public void testNewKeystoreInvalidPassword() throws Exception {
    File dir = tempFolder.newFolder("keystore-bad");
    File pwFile = tempFolder.newFile("short.txt");
    Files.write(pwFile.toPath(), "abc".getBytes(StandardCharsets.UTF_8));

    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "new",
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should fail with short password", 1, exitCode);
    assertTrue("Error should mention password length",
        err.toString().contains("at least 6 characters"));
  }

  @Test
  public void testNewKeystoreCustomDir() throws Exception {
    File dir = new File(tempFolder.getRoot(), "custom/nested/dir");
    File pwFile = tempFolder.newFile("pw.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "new",
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals(0, exitCode);
    assertTrue("Custom dir should be created", dir.exists());
    File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
    assertNotNull(files);
    assertEquals(1, files.length);
  }

  @Test
  public void testNewKeystoreNoTtyNoPasswordFile() throws Exception {
    // In CI/test environment, System.console() is null.
    // Without --password-file, should fail with exit code 1.
    File dir = tempFolder.newFolder("keystore-notty");

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "new",
        "--keystore-dir", dir.getAbsolutePath());

    assertEquals("Should fail when no TTY and no --password-file", 1, exitCode);
  }

  @Test
  public void testNewKeystoreEmptyPassword() throws Exception {
    File dir = tempFolder.newFolder("keystore-empty");
    File pwFile = tempFolder.newFile("empty.txt");
    Files.write(pwFile.toPath(), "".getBytes(StandardCharsets.UTF_8));

    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "new",
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should fail with empty password", 1, exitCode);
    assertTrue("Error should mention password length",
        err.toString().contains("at least 6 characters"));
  }

  @Test
  public void testNewKeystoreWithSm2() throws Exception {
    File dir = tempFolder.newFolder("keystore-sm2");
    File pwFile = tempFolder.newFile("pw-sm2.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "new",
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath(),
        "--sm2");

    assertEquals("SM2 keystore creation should succeed", 0, exitCode);
    File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
    assertNotNull(files);
    assertEquals(1, files.length);

    // Verify SM2 keystore can be decrypted with ecKey=false
    org.tron.keystore.Credentials creds =
        org.tron.keystore.WalletUtils.loadCredentials("test123456", files[0], false);
    assertNotNull(creds.getAddress());
  }

  @Test
  public void testNewKeystoreSpecialCharPassword() throws Exception {
    File dir = tempFolder.newFolder("keystore-special");
    File pwFile = tempFolder.newFile("pw-special.txt");
    String password = "p@$$w0rd!#%^&*()_+-=[]{}";
    Files.write(pwFile.toPath(), password.getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "new",
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals(0, exitCode);
    File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
    assertNotNull(files);
    assertEquals(1, files.length);

    // Verify can decrypt with same special-char password
    Credentials creds = WalletUtils.loadCredentials(password, files[0], true);
    assertNotNull(creds.getAddress());
  }

  @Test
  public void testNewKeystorePasswordFileNotFound() throws Exception {
    File dir = tempFolder.newFolder("keystore-nopw");

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "new",
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", "/tmp/nonexistent-pw.txt");

    assertEquals("Should fail when password file not found", 1, exitCode);
  }

  @Test
  public void testNewKeystoreDirIsFile() throws Exception {
    File notADir = tempFolder.newFile("not-a-dir");
    File pwFile = tempFolder.newFile("pw-dir.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "new",
        "--keystore-dir", notADir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should fail when dir is a file", 1, exitCode);
  }

  @Test
  public void testNewKeystorePasswordFileTooLarge() throws Exception {
    File dir = tempFolder.newFolder("keystore-bigpw");
    File pwFile = tempFolder.newFile("bigpw.txt");
    byte[] bigContent = new byte[1025];
    java.util.Arrays.fill(bigContent, (byte) 'a');
    Files.write(pwFile.toPath(), bigContent);

    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "new",
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should fail with large password file", 1, exitCode);
    assertTrue("Error should mention file too large",
        err.toString().contains("too large"));
  }

  @Test
  public void testNewKeystorePasswordFileWithBom() throws Exception {
    File dir = tempFolder.newFolder("keystore-bom");
    File pwFile = tempFolder.newFile("bom.txt");
    Files.write(pwFile.toPath(),
        ("\uFEFF" + "test123456").getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "new",
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should succeed with BOM password file", 0, exitCode);
    File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
    assertNotNull(files);
    assertEquals(1, files.length);
  }

  @Test
  public void testNewKeystoreFilePermissions() throws Exception {
    String os = System.getProperty("os.name").toLowerCase();
    org.junit.Assume.assumeTrue("POSIX permissions test, skip on Windows",
        !os.contains("win"));

    File dir = tempFolder.newFolder("keystore-perms");
    File pwFile = tempFolder.newFile("pw-perms.txt");
    Files.write(pwFile.toPath(), "test123456".getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "new",
        "--keystore-dir", dir.getAbsolutePath(),
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
  public void testNewKeystoreRejectsMultiLinePasswordFile() throws Exception {
    // Regression: a user might accidentally point --password-file at a
    // `keystore update` two-line file (old\nnew). Without the guard the
    // literal "old\nnew" becomes the password and neither line alone can
    // unlock it later. new/import must reject multi-line files.
    File dir = tempFolder.newFolder("keystore-multi");
    File pwFile = tempFolder.newFile("multi-line.txt");
    Files.write(pwFile.toPath(),
        "oldpass123\nnewpass456".getBytes(StandardCharsets.UTF_8));

    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "new",
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should reject multi-line password file", 1, exitCode);
    assertTrue("Error must explain the multi-line rejection, got: " + err.toString(),
        err.toString().contains("multiple lines"));
    // No keystore created
    File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
    assertTrue("No keystore should have been created",
        files == null || files.length == 0);
  }
}
