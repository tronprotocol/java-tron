package org.tron.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.keystore.WalletFile;

public class KeystoreCliUtilsTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testStripLineEndingsNoChange() {
    assertEquals("password", KeystoreCliUtils.stripLineEndings("password"));
  }

  @Test
  public void testStripLineEndingsTrailingLf() {
    assertEquals("password", KeystoreCliUtils.stripLineEndings("password\n"));
  }

  @Test
  public void testStripLineEndingsTrailingCrLf() {
    assertEquals("password", KeystoreCliUtils.stripLineEndings("password\r\n"));
  }

  @Test
  public void testStripLineEndingsTrailingCr() {
    assertEquals("password", KeystoreCliUtils.stripLineEndings("password\r"));
  }

  @Test
  public void testStripLineEndingsMultipleTrailing() {
    assertEquals("password", KeystoreCliUtils.stripLineEndings("password\r\n\r\n"));
  }

  @Test
  public void testStripLineEndingsBom() {
    assertEquals("password", KeystoreCliUtils.stripLineEndings("\uFEFFpassword"));
  }

  @Test
  public void testStripLineEndingsBomAndTrailing() {
    assertEquals("password",
        KeystoreCliUtils.stripLineEndings("\uFEFFpassword\r\n"));
  }

  @Test
  public void testStripLineEndingsEmpty() {
    assertEquals("", KeystoreCliUtils.stripLineEndings(""));
  }

  @Test
  public void testStripLineEndingsOnlyLineEndings() {
    assertEquals("", KeystoreCliUtils.stripLineEndings("\r\n\r\n"));
  }

  @Test
  public void testJsonMapEven() {
    Map<String, String> m = KeystoreCliUtils.jsonMap("a", "1", "b", "2");
    assertEquals(2, m.size());
    assertEquals("1", m.get("a"));
    assertEquals("2", m.get("b"));
  }

  @Test
  public void testJsonMapPreservesOrder() {
    Map<String, String> m = KeystoreCliUtils.jsonMap(
        "z", "1", "a", "2", "m", "3");
    String[] keys = m.keySet().toArray(new String[0]);
    assertEquals("z", keys[0]);
    assertEquals("a", keys[1]);
    assertEquals("m", keys[2]);
  }

  @Test
  public void testJsonMapEmpty() {
    Map<String, String> m = KeystoreCliUtils.jsonMap();
    assertTrue(m.isEmpty());
  }

  @Test
  public void testIsValidKeystoreFileValid() {
    WalletFile wf = new WalletFile();
    wf.setAddress("TAddr");
    wf.setVersion(3);
    wf.setCrypto(new WalletFile.Crypto());
    assertTrue(KeystoreCliUtils.isValidKeystoreFile(wf));
  }

  @Test
  public void testIsValidKeystoreFileNullAddress() {
    WalletFile wf = new WalletFile();
    wf.setVersion(3);
    wf.setCrypto(new WalletFile.Crypto());
    assertFalse(KeystoreCliUtils.isValidKeystoreFile(wf));
  }

  @Test
  public void testIsValidKeystoreFileNullCrypto() {
    WalletFile wf = new WalletFile();
    wf.setAddress("TAddr");
    wf.setVersion(3);
    assertFalse(KeystoreCliUtils.isValidKeystoreFile(wf));
  }

  @Test
  public void testIsValidKeystoreFileWrongVersion() {
    WalletFile wf = new WalletFile();
    wf.setAddress("TAddr");
    wf.setVersion(2);
    wf.setCrypto(new WalletFile.Crypto());
    assertFalse(KeystoreCliUtils.isValidKeystoreFile(wf));
  }

  @Test
  public void testCheckFileExistsNull() {
    StringWriter err = new StringWriter();
    assertTrue(KeystoreCliUtils.checkFileExists(null, "Label",
        new PrintWriter(err)));
    assertEquals("", err.toString());
  }

  @Test
  public void testCheckFileExistsMissing() {
    StringWriter err = new StringWriter();
    File missing = new File("/tmp/nonexistent-cli-utils-test-file");
    assertFalse(KeystoreCliUtils.checkFileExists(missing, "Key file",
        new PrintWriter(err)));
    assertTrue(err.toString().contains("Key file not found"));
  }

  @Test
  public void testCheckFileExistsPresent() throws Exception {
    StringWriter err = new StringWriter();
    File f = tempFolder.newFile("present.txt");
    assertTrue(KeystoreCliUtils.checkFileExists(f, "Key file",
        new PrintWriter(err)));
  }

  @Test
  public void testReadPasswordFromFile() throws Exception {
    File pwFile = tempFolder.newFile("pw.txt");
    Files.write(pwFile.toPath(), "goodpassword".getBytes(StandardCharsets.UTF_8));
    StringWriter err = new StringWriter();
    String pw = KeystoreCliUtils.readPassword(pwFile, new PrintWriter(err));
    assertEquals("goodpassword", pw);
  }

  @Test
  public void testReadPasswordFromFileWithLineEndings() throws Exception {
    File pwFile = tempFolder.newFile("pw-crlf.txt");
    Files.write(pwFile.toPath(), "goodpassword\r\n".getBytes(StandardCharsets.UTF_8));
    StringWriter err = new StringWriter();
    String pw = KeystoreCliUtils.readPassword(pwFile, new PrintWriter(err));
    assertEquals("goodpassword", pw);
  }

  @Test
  public void testReadPasswordFromFileWithBom() throws Exception {
    File pwFile = tempFolder.newFile("pw-bom.txt");
    Files.write(pwFile.toPath(),
        "\uFEFFgoodpassword".getBytes(StandardCharsets.UTF_8));
    StringWriter err = new StringWriter();
    String pw = KeystoreCliUtils.readPassword(pwFile, new PrintWriter(err));
    assertEquals("goodpassword", pw);
  }

  @Test
  public void testReadPasswordFileTooLarge() throws Exception {
    File pwFile = tempFolder.newFile("pw-big.txt");
    byte[] big = new byte[1025];
    java.util.Arrays.fill(big, (byte) 'a');
    Files.write(pwFile.toPath(), big);
    StringWriter err = new StringWriter();
    String pw = KeystoreCliUtils.readPassword(pwFile, new PrintWriter(err));
    assertNull(pw);
    assertTrue(err.toString().contains("too large"));
  }

  @Test
  public void testReadPasswordFileShort() throws Exception {
    File pwFile = tempFolder.newFile("pw-short.txt");
    Files.write(pwFile.toPath(), "abc".getBytes(StandardCharsets.UTF_8));
    StringWriter err = new StringWriter();
    String pw = KeystoreCliUtils.readPassword(pwFile, new PrintWriter(err));
    assertNull(pw);
    assertTrue(err.toString().contains("at least 6"));
  }

  @Test
  public void testReadPasswordFileNotFound() throws Exception {
    StringWriter err = new StringWriter();
    String pw = KeystoreCliUtils.readPassword(
        new File("/tmp/nonexistent-pw-direct-test.txt"), new PrintWriter(err));
    assertNull(pw);
    assertTrue(err.toString().contains("Password file not found"));
  }

  @Test
  public void testEnsureDirectoryCreatesNested() throws Exception {
    File dir = new File(tempFolder.getRoot(), "a/b/c");
    assertFalse(dir.exists());
    KeystoreCliUtils.ensureDirectory(dir);
    assertTrue(dir.exists());
    assertTrue(dir.isDirectory());
  }

  @Test
  public void testEnsureDirectoryExisting() throws Exception {
    File dir = tempFolder.newFolder("existing");
    KeystoreCliUtils.ensureDirectory(dir);
    assertTrue(dir.isDirectory());
  }

  @Test(expected = java.io.IOException.class)
  public void testEnsureDirectoryPathIsFile() throws Exception {
    File f = tempFolder.newFile("not-a-dir");
    KeystoreCliUtils.ensureDirectory(f);
  }

  @Test
  public void testPrintJsonValidOutput() {
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    KeystoreCliUtils.printJson(new PrintWriter(out), new PrintWriter(err),
        KeystoreCliUtils.jsonMap("address", "TAddr", "file", "file.json"));
    String s = out.toString().trim();
    assertTrue(s.contains("\"address\":\"TAddr\""));
    assertTrue(s.contains("\"file\":\"file.json\""));
  }

  @Test
  public void testPrintSecurityTipsIncludesAddressAndFile() {
    StringWriter out = new StringWriter();
    KeystoreCliUtils.printSecurityTips(new PrintWriter(out),
        "TMyAddress", "/path/to/keystore.json");
    String s = out.toString();
    assertTrue(s.contains("TMyAddress"));
    assertTrue(s.contains("/path/to/keystore.json"));
    assertTrue(s.contains("NEVER share"));
    assertTrue(s.contains("BACKUP"));
    assertTrue(s.contains("REMEMBER"));
  }

  @Test
  public void testAtomicMove() throws Exception {
    File src = tempFolder.newFile("src.txt");
    Files.write(src.toPath(), "hello".getBytes(StandardCharsets.UTF_8));
    File target = new File(tempFolder.getRoot(), "target.txt");

    KeystoreCliUtils.atomicMove(src, target);
    assertFalse(src.exists());
    assertTrue(target.exists());
    assertEquals("hello",
        new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8));
  }

  @Test
  public void testAtomicMoveReplacesExisting() throws Exception {
    File src = tempFolder.newFile("src2.txt");
    Files.write(src.toPath(), "new".getBytes(StandardCharsets.UTF_8));
    File target = tempFolder.newFile("target2.txt");
    Files.write(target.toPath(), "old".getBytes(StandardCharsets.UTF_8));

    KeystoreCliUtils.atomicMove(src, target);
    assertEquals("new",
        new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8));
  }

  @Test
  public void testGenerateKeystoreFileFullScrypt() throws Exception {
    File dir = tempFolder.newFolder("gen-full");
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    StringWriter err = new StringWriter();

    String fileName = KeystoreCliUtils.generateKeystoreFile(
        "password123", keyPair, dir, true, new PrintWriter(err));

    assertNotNull(fileName);
    assertTrue(fileName.endsWith(".json"));
    File file = new File(dir, fileName);
    assertTrue(file.exists());
  }

  @Test
  public void testGenerateKeystoreFileLightScrypt() throws Exception {
    File dir = tempFolder.newFolder("gen-light");
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    StringWriter err = new StringWriter();

    String fileName = KeystoreCliUtils.generateKeystoreFile(
        "password123", keyPair, dir, false, new PrintWriter(err));

    assertNotNull(fileName);
    File file = new File(dir, fileName);
    assertTrue(file.exists());
  }

  @Test
  public void testGenerateKeystoreFileLeavesNoTempFile() throws Exception {
    File dir = tempFolder.newFolder("gen-notemp");
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true);
    StringWriter err = new StringWriter();

    KeystoreCliUtils.generateKeystoreFile(
        "password123", keyPair, dir, false, new PrintWriter(err));

    File[] tempFiles = dir.listFiles((d, name) -> name.startsWith("keystore-")
        && name.endsWith(".tmp"));
    assertNotNull(tempFiles);
    assertEquals("No temp files should remain after generation", 0, tempFiles.length);
  }

  @Test
  public void testSetOwnerOnly() throws Exception {
    String os = System.getProperty("os.name").toLowerCase();
    org.junit.Assume.assumeTrue("POSIX permissions test", !os.contains("win"));

    File f = tempFolder.newFile("perm-test.txt");
    StringWriter err = new StringWriter();
    KeystoreCliUtils.setOwnerOnly(f, new PrintWriter(err));

    java.util.Set<java.nio.file.attribute.PosixFilePermission> perms =
        Files.getPosixFilePermissions(f.toPath());
    assertEquals(java.util.EnumSet.of(
        java.nio.file.attribute.PosixFilePermission.OWNER_READ,
        java.nio.file.attribute.PosixFilePermission.OWNER_WRITE),
        perms);
  }
}
