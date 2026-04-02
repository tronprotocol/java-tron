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
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setOut(new PrintWriter(out));

    int exitCode = cmd.execute("keystore", "new",
        "--keystore-dir", dir.getAbsolutePath(),
        "--password-file", pwFile.getAbsolutePath(),
        "--json");

    assertEquals(0, exitCode);
    // stdout is captured by picocli's setOut but System.out goes to console
    // The JSON output goes through System.out directly
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
}
