package org.tron.keystore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.common.utils.Utils;

/**
 * Verifies that {@link WalletUtils#generateWalletFile} and
 * {@link WalletUtils#writeWalletFile} produce keystore files with
 * owner-only permissions (0600) atomically, leaving no temp files behind.
 *
 * <p>Tests use light scrypt (useFullScrypt=false) where possible because
 * they validate filesystem behavior, not the KDF parameters.
 */
public class WalletUtilsWriteTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private static WalletFile lightWalletFile(String password) throws Exception {
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom(), true);
    return Wallet.createLight(password, keyPair);
  }

  @Test
  public void testGenerateWalletFileCreatesOwnerOnlyFile() throws Exception {
    Assume.assumeTrue("POSIX permissions test",
        !System.getProperty("os.name").toLowerCase().contains("win"));

    File dir = tempFolder.newFolder("gen-perms");
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom(), true);

    String fileName = WalletUtils.generateWalletFile("password123", keyPair, dir, false);

    File created = new File(dir, fileName);
    assertTrue(created.exists());

    Set<PosixFilePermission> perms = Files.getPosixFilePermissions(created.toPath());
    assertEquals("Keystore must have owner-only permissions (rw-------)",
        EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
        perms);
  }

  @Test
  public void testGenerateWalletFileLeavesNoTempFile() throws Exception {
    File dir = tempFolder.newFolder("gen-no-temp");
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom(), true);

    WalletUtils.generateWalletFile("password123", keyPair, dir, false);

    File[] tempFiles = dir.listFiles((d, name) -> name.startsWith("keystore-")
        && name.endsWith(".tmp"));
    assertNotNull(tempFiles);
    assertEquals("No temp files should remain after generation", 0, tempFiles.length);
  }

  @Test
  public void testGenerateWalletFileLightScrypt() throws Exception {
    File dir = tempFolder.newFolder("gen-light");
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom(), true);

    String fileName = WalletUtils.generateWalletFile("password123", keyPair, dir, false);
    assertNotNull(fileName);
    assertTrue(fileName.endsWith(".json"));
    assertTrue(new File(dir, fileName).exists());
  }

  @Test
  public void testWriteWalletFileOwnerOnly() throws Exception {
    Assume.assumeTrue("POSIX permissions test",
        !System.getProperty("os.name").toLowerCase().contains("win"));

    File dir = tempFolder.newFolder("write-perms");
    WalletFile wf = lightWalletFile("password123");
    File destination = new File(dir, "out.json");

    WalletUtils.writeWalletFile(wf, destination);

    assertTrue(destination.exists());
    Set<PosixFilePermission> perms = Files.getPosixFilePermissions(destination.toPath());
    assertEquals("Keystore must have owner-only permissions (rw-------)",
        EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
        perms);
  }

  @Test
  public void testWriteWalletFileReplacesExisting() throws Exception {
    File dir = tempFolder.newFolder("write-replace");
    WalletFile wf1 = lightWalletFile("password123");
    WalletFile wf2 = lightWalletFile("password123");
    File destination = new File(dir, "out.json");

    WalletUtils.writeWalletFile(wf1, destination);
    WalletUtils.writeWalletFile(wf2, destination);

    assertTrue("Destination exists after replace", destination.exists());
    WalletFile reread = new com.fasterxml.jackson.databind.ObjectMapper()
        .readValue(destination, WalletFile.class);
    assertEquals("Replaced file should have wf2's address",
        wf2.getAddress(), reread.getAddress());
  }

  @Test
  public void testWriteWalletFileLeavesNoTempFile() throws Exception {
    File dir = tempFolder.newFolder("write-no-temp");
    WalletFile wf = lightWalletFile("password123");
    File destination = new File(dir, "final.json");

    WalletUtils.writeWalletFile(wf, destination);

    File[] tempFiles = dir.listFiles((d, name) -> name.startsWith("keystore-")
        && name.endsWith(".tmp"));
    assertNotNull(tempFiles);
    assertEquals("No temp files should remain", 0, tempFiles.length);
  }

  @Test
  public void testWriteWalletFileCreatesParentDirectories() throws Exception {
    File base = tempFolder.newFolder("write-nested");
    File destination = new File(base, "a/b/c/out.json");
    assertFalse("Parent dir does not exist yet", destination.getParentFile().exists());

    WalletFile wf = lightWalletFile("password123");
    WalletUtils.writeWalletFile(wf, destination);

    assertTrue("Destination written", destination.exists());
  }

  @Test
  public void testWriteWalletFileCleansUpTempOnFailure() throws Exception {
    // Force failure by making the destination a directory — Files.move will fail
    // because the source is a file. The temp file must be cleaned up.
    File dir = tempFolder.newFolder("write-fail");
    File destinationAsDir = new File(dir, "blocking-dir");
    assertTrue("Setup: blocking dir created", destinationAsDir.mkdir());
    // Put a file inside so Files.move with REPLACE_EXISTING fails (non-empty dir).
    assertTrue("Setup: block file", new File(destinationAsDir, "blocker").createNewFile());

    WalletFile wf = lightWalletFile("password123");

    try {
      WalletUtils.writeWalletFile(wf, destinationAsDir);
      fail("Expected IOException because destination is a non-empty directory");
    } catch (IOException expected) {
      // Expected
    }

    File[] tempFiles = dir.listFiles((d, name) -> name.startsWith("keystore-")
        && name.endsWith(".tmp"));
    assertNotNull(tempFiles);
    assertEquals("Temp file must be cleaned up on failure", 0, tempFiles.length);
  }

  // ---------- loadCredentials symlink behavior ----------

  @Test
  public void testLoadCredentialsFollowsSymlinkButWarns() throws Exception {
    Assume.assumeTrue("Symlinks only tested on POSIX",
        !System.getProperty("os.name").toLowerCase().contains("win"));

    File realDir = tempFolder.newFolder("load-symlink-target");
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom(), true);
    String realName = WalletUtils.generateWalletFile("password123", keyPair, realDir, false);
    File realKeystore = new File(realDir, realName);

    File linkDir = tempFolder.newFolder("load-symlink-link");
    File symlink = new File(linkDir, "witness.json");
    Files.createSymbolicLink(symlink.toPath(), realKeystore.toPath());

    // Should NOT throw — Lighthouse-style: follow the symlink, log a warning
    // for the operator. Hard-rejecting would silently break legitimate SR
    // deployments that organize keystores via symlinks.
    Credentials creds =
        WalletUtils.loadCredentials("password123", symlink, true);
    assertNotNull(creds.getAddress());
  }

  @Test
  public void testLoadCredentialsAcceptsRegularFile() throws Exception {
    File dir = tempFolder.newFolder("load-ok");
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom(), true);
    String fileName = WalletUtils.generateWalletFile("password123", keyPair, dir, false);

    Credentials creds =
        WalletUtils.loadCredentials("password123", new File(dir, fileName), true);
    assertNotNull(creds.getAddress());
  }
}
