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

    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    java.io.PrintStream originalOut = System.out;
    System.setOut(new java.io.PrintStream(baos));
    try {
      CommandLine cmd = new CommandLine(new Toolkit());
      int exitCode = cmd.execute("keystore", "new",
          "--keystore-dir", dir.getAbsolutePath(),
          "--password-file", pwFile.getAbsolutePath(),
          "--json");

      assertEquals(0, exitCode);
      String output = baos.toString(StandardCharsets.UTF_8.name()).trim();
      assertTrue("JSON output should contain address",
          output.contains("\"address\""));
      assertTrue("JSON output should contain file",
          output.contains("\"file\""));
    } finally {
      System.setOut(originalOut);
    }
  }

  @Test
  public void testNewKeystoreInvalidPassword() throws Exception {
    File dir = tempFolder.newFolder("keystore-bad");
    File pwFile = tempFolder.newFile("short.txt");
    Files.write(pwFile.toPath(), "abc".getBytes(StandardCharsets.UTF_8));

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "new",
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should fail with short password", 1, exitCode);
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

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "new",
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath());

    assertEquals("Should fail with empty password", 1, exitCode);
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
}
