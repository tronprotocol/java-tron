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
    // Each line should contain a T-address and a .json filename
    for (String line : lines) {
      assertTrue("Each line should contain an address starting with T",
          line.trim().startsWith("T"));
      assertTrue("Each line should reference a .json file",
          line.contains(".json"));
    }
  }

  @Test
  public void testListEmptyDirectory() throws Exception {
    File dir = tempFolder.newFolder("empty");

    StringWriter out = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setOut(new PrintWriter(out));
    int exitCode = cmd.execute("keystore", "list",
        "--keystore-dir", dir.getAbsolutePath());

    assertEquals(0, exitCode);
    assertTrue("Should print no-keystores message",
        out.toString().contains("No keystores found"));
  }

  @Test
  public void testListNonExistentDirectory() throws Exception {
    File dir = new File(tempFolder.getRoot(), "nonexistent");

    StringWriter out = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setOut(new PrintWriter(out));
    int exitCode = cmd.execute("keystore", "list",
        "--keystore-dir", dir.getAbsolutePath());

    assertEquals(0, exitCode);
    assertTrue("Should print no-keystores message",
        out.toString().contains("No keystores found"));
  }

  @Test
  public void testListEmptyDirectoryJsonOutput() throws Exception {
    File dir = tempFolder.newFolder("empty-json");

    StringWriter out = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setOut(new PrintWriter(out));
    int exitCode = cmd.execute("keystore", "list",
        "--keystore-dir", dir.getAbsolutePath(), "--json");

    assertEquals(0, exitCode);
    String output = out.toString().trim();
    assertTrue("Empty dir JSON should have empty keystores array",
        output.contains("{\"keystores\":[]}"));
  }

  @Test
  public void testListNonExistentDirectoryJsonOutput() throws Exception {
    File dir = new File(tempFolder.getRoot(), "nonexistent-json");

    StringWriter out = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setOut(new PrintWriter(out));
    int exitCode = cmd.execute("keystore", "list",
        "--keystore-dir", dir.getAbsolutePath(), "--json");

    assertEquals(0, exitCode);
    String output = out.toString().trim();
    assertTrue("Non-existent dir JSON should have empty keystores array",
        output.contains("{\"keystores\":[]}"));
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

  @Test
  public void testListWarnsOnCorruptedJsonFiles() throws Exception {
    File dir = tempFolder.newFolder("keystore-corrupt");
    String password = "test123456";

    // Create one valid keystore
    SignInterface key = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    WalletUtils.generateWalletFile(password, key, dir, false);

    // Create a corrupted JSON file
    Files.write(new File(dir, "corrupted.json").toPath(),
        "not valid json{{{".getBytes(StandardCharsets.UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setOut(new PrintWriter(out));
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "list",
        "--keystore-dir", dir.getAbsolutePath());

    assertEquals(0, exitCode);
    String errOutput = err.toString();
    assertTrue("Should warn about corrupted file",
        errOutput.contains("Warning: skipping unreadable file: corrupted.json"));

    // Valid keystore should still be listed
    String output = out.toString().trim();
    assertTrue("Should still list the valid keystore", output.length() > 0);
  }

  @Test
  public void testListSkipsInvalidVersionKeystores() throws Exception {
    File dir = tempFolder.newFolder("keystore-version");
    String password = "test123456";

    // Create one valid keystore
    SignInterface key = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    WalletUtils.generateWalletFile(password, key, dir, false);

    // Create a JSON with address and crypto but wrong version
    String fakeV2 = "{\"address\":\"TFakeAddress\",\"version\":2,"
        + "\"crypto\":{\"cipher\":\"aes-128-ctr\"}}";
    Files.write(new File(dir, "v2-keystore.json").toPath(),
        fakeV2.getBytes(StandardCharsets.UTF_8));

    // Create a JSON with address but null crypto
    String noCrypto = "{\"address\":\"TFakeAddress2\",\"version\":3}";
    Files.write(new File(dir, "no-crypto.json").toPath(),
        noCrypto.getBytes(StandardCharsets.UTF_8));

    StringWriter out = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setOut(new PrintWriter(out));
    int exitCode = cmd.execute("keystore", "list",
        "--keystore-dir", dir.getAbsolutePath());

    assertEquals(0, exitCode);
    String output = out.toString().trim();
    String[] lines = output.split("\\n");
    assertEquals("Should list only the valid v3 keystore, not v2 or no-crypto",
        1, lines.length);
  }

  @Test
  public void testListSkipsSymlinkedKeystoreFile() throws Exception {
    org.junit.Assume.assumeTrue("Symlinks only tested on POSIX",
        !System.getProperty("os.name").toLowerCase().contains("win"));

    File dir = tempFolder.newFolder("keystore-symlink-scan");
    String password = "test123456";

    SignInterface key = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    WalletUtils.generateWalletFile(password, key, dir, false);

    // A JSON file elsewhere (simulates "target we should not be tricked
    // into reading") — placed outside the keystore dir.
    File target = tempFolder.newFile("outside.json");
    Files.write(target.toPath(),
        "{\"secret\":\"should not appear in list output\"}"
            .getBytes(StandardCharsets.UTF_8));

    // Plant a .json symlink in the keystore dir
    File symlink = new File(dir, "evil.json");
    Files.createSymbolicLink(symlink.toPath(), target.toPath());

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    CommandLine cmd = new CommandLine(new Toolkit());
    cmd.setOut(new PrintWriter(out));
    cmd.setErr(new PrintWriter(err));
    int exitCode = cmd.execute("keystore", "list",
        "--keystore-dir", dir.getAbsolutePath());

    assertEquals(0, exitCode);
    assertTrue("Should warn about symbolic link, got err: " + err.toString(),
        err.toString().contains("Warning: skipping symbolic link: evil.json"));
    String output = out.toString().trim();
    String[] lines = output.split("\\n");
    assertEquals("Should list only the real keystore", 1, lines.length);
  }
}
