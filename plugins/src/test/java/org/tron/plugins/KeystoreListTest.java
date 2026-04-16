package org.tron.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.keystore.WalletUtils;
import picocli.CommandLine;

public class KeystoreListTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testListMultipleKeystores() throws Exception {
    File dir = tempFolder.newFolder("keystore");
    String password = "test123456";

    // Create 3 keystores
    for (int i = 0; i < 3; i++) {
      SignInterface key = SignUtils.getGeneratedRandomSign(
          SecureRandom.getInstance("NativePRNG"), true);
      WalletUtils.generateWalletFile(password, key, dir, false);
    }

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setOut(new PrintWriter(out));
    cmd.setErr(new PrintWriter(err));

    int exitCode = cmd.execute("keystore", "list",
        "--keystore-dir", dir.getAbsolutePath());

    assertEquals(0, exitCode);
    String output = out.toString().trim();
    assertTrue("Output should not be empty", output.length() > 0);
    // Should have 3 lines of output (one per keystore)
    String[] lines = output.split("\\n");
    assertEquals("Should list 3 keystores", 3, lines.length);
  }

  @Test
  public void testListEmptyDirectory() throws Exception {
    File dir = tempFolder.newFolder("empty");

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "list",
        "--keystore-dir", dir.getAbsolutePath());

    assertEquals(0, exitCode);
  }

  @Test
  public void testListNonExistentDirectory() throws Exception {
    File dir = new File(tempFolder.getRoot(), "nonexistent");

    CommandLine cmd = new CommandLine(new Toolkit());
    int exitCode = cmd.execute("keystore", "list",
        "--keystore-dir", dir.getAbsolutePath());

    assertEquals(0, exitCode);
  }

  @Test
  public void testListJsonOutput() throws Exception {
    File dir = tempFolder.newFolder("keystore-json");
    String password = "test123456";
    SignInterface key = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    WalletUtils.generateWalletFile(password, key, dir, false);

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setOut(new PrintWriter(out));
    cmd.setErr(new PrintWriter(err));

    int exitCode = cmd.execute("keystore", "list",
        "--keystore-dir", dir.getAbsolutePath(), "--json");

    assertEquals(0, exitCode);
    String output = out.toString().trim();
    assertTrue("Should start with keystores JSON array",
        output.startsWith("{\"keystores\":["));
    assertTrue("Should end with JSON array close",
        output.endsWith("]}"));
  }

  @Test
  public void testListSkipsNonKeystoreFiles() throws Exception {
    File dir = tempFolder.newFolder("keystore-mixed");
    String password = "test123456";

    // Create one valid keystore
    SignInterface key = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    WalletUtils.generateWalletFile(password, key, dir, false);

    // Create non-keystore files
    Files.write(new File(dir, "readme.json").toPath(),
        "{\"not\":\"a keystore\"}".getBytes(StandardCharsets.UTF_8));
    Files.write(new File(dir, "notes.txt").toPath(),
        "plain text".getBytes(StandardCharsets.UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setOut(new PrintWriter(out));
    cmd.setErr(new PrintWriter(err));

    int exitCode = cmd.execute("keystore", "list",
        "--keystore-dir", dir.getAbsolutePath());

    assertEquals(0, exitCode);
    String output = out.toString().trim();
    assertTrue("Output should not be empty", output.length() > 0);
    String[] lines = output.split("\\n");
    // Should list only the valid keystore, not the readme.json or notes.txt
    assertEquals("Should list only 1 valid keystore", 1, lines.length);
  }
}
