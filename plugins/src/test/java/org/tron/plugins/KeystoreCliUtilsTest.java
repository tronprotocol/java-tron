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
import java.util.Locale;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.keystore.WalletFile;

public class KeystoreCliUtilsTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

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

  private static WalletFile.Crypto supportedCrypto() {
    WalletFile.Crypto crypto = new WalletFile.Crypto();
    crypto.setCipher("aes-128-ctr");
    crypto.setKdf("scrypt");
    return crypto;
  }

  @Test
  public void testIsValidKeystoreFileValid() {
    WalletFile wf = new WalletFile();
    wf.setAddress("TAddr");
    wf.setVersion(3);
    wf.setCrypto(supportedCrypto());
    assertTrue(KeystoreCliUtils.isValidKeystoreFile(wf));
  }

  @Test
  public void testIsValidKeystoreFileNullAddress() {
    WalletFile wf = new WalletFile();
    wf.setVersion(3);
    wf.setCrypto(supportedCrypto());
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
    wf.setCrypto(supportedCrypto());
    assertFalse(KeystoreCliUtils.isValidKeystoreFile(wf));
  }

  @Test
  public void testIsValidKeystoreFileRejectsEmptyCryptoStub() {
    // {"address":"T...","version":3,"crypto":{}} — passes the old checks
    // but Wallet.validate would later reject it. Discovery should skip it.
    WalletFile wf = new WalletFile();
    wf.setAddress("TAddr");
    wf.setVersion(3);
    wf.setCrypto(new WalletFile.Crypto());
    assertFalse(KeystoreCliUtils.isValidKeystoreFile(wf));
  }

  @Test
  public void testIsValidKeystoreFileRejectsUnsupportedCipher() {
    WalletFile wf = new WalletFile();
    wf.setAddress("TAddr");
    wf.setVersion(3);
    WalletFile.Crypto crypto = new WalletFile.Crypto();
    crypto.setCipher("des");
    crypto.setKdf("scrypt");
    wf.setCrypto(crypto);
    assertFalse(KeystoreCliUtils.isValidKeystoreFile(wf));
  }

  @Test
  public void testIsValidKeystoreFileRejectsUnsupportedKdf() {
    WalletFile wf = new WalletFile();
    wf.setAddress("TAddr");
    wf.setVersion(3);
    WalletFile.Crypto crypto = new WalletFile.Crypto();
    crypto.setCipher("aes-128-ctr");
    crypto.setKdf("bcrypt");
    wf.setCrypto(crypto);
    assertFalse(KeystoreCliUtils.isValidKeystoreFile(wf));
  }

  @Test
  public void testIsValidKeystoreFileAcceptsPbkdf2Kdf() {
    // pbkdf2 is the other supported KDF (used by some Ethereum keystores).
    WalletFile wf = new WalletFile();
    wf.setAddress("TAddr");
    wf.setVersion(3);
    WalletFile.Crypto crypto = new WalletFile.Crypto();
    crypto.setCipher("aes-128-ctr");
    crypto.setKdf("pbkdf2");
    wf.setCrypto(crypto);
    assertTrue(KeystoreCliUtils.isValidKeystoreFile(wf));
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
  public void testReadRegularFileSuccess() throws Exception {
    File f = tempFolder.newFile("regular.txt");
    Files.write(f.toPath(), "hello".getBytes(StandardCharsets.UTF_8));
    StringWriter err = new StringWriter();

    byte[] bytes = KeystoreCliUtils.readRegularFile(f, 1024, "File",
        new PrintWriter(err));
    assertNotNull(bytes);
    assertEquals("hello", new String(bytes, StandardCharsets.UTF_8));
  }

  @Test
  public void testReadRegularFileMissing() throws Exception {
    File f = new File(tempFolder.getRoot(), "does-not-exist");
    StringWriter err = new StringWriter();

    byte[] bytes = KeystoreCliUtils.readRegularFile(f, 1024, "Password file",
        new PrintWriter(err));
    assertNull(bytes);
    assertTrue("Expected 'not found' error, got: " + err.toString(),
        err.toString().contains("Password file not found"));
  }

  @Test
  public void testReadRegularFileTooLarge() throws Exception {
    File f = tempFolder.newFile("big.txt");
    byte[] big = new byte[2048];
    java.util.Arrays.fill(big, (byte) 'a');
    Files.write(f.toPath(), big);
    StringWriter err = new StringWriter();

    byte[] bytes = KeystoreCliUtils.readRegularFile(f, 1024, "Password file",
        new PrintWriter(err));
    assertNull(bytes);
    assertTrue("Expected 'too large', got: " + err.toString(),
        err.toString().contains("too large"));
  }

  @Test
  public void testReadRegularFileRefusesSymlink() throws Exception {
    org.junit.Assume.assumeTrue("Symlinks only tested on POSIX",
        !System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win"));

    File target = tempFolder.newFile("real-target.txt");
    Files.write(target.toPath(), "secret content".getBytes(StandardCharsets.UTF_8));
    File link = new File(tempFolder.getRoot(), "symlink.txt");
    Files.createSymbolicLink(link.toPath(), target.toPath());

    StringWriter err = new StringWriter();
    byte[] bytes = KeystoreCliUtils.readRegularFile(link, 1024, "File",
        new PrintWriter(err));

    assertNull("Must refuse to read through symlink", bytes);
    assertTrue("Expected symlink-refusal message, got: " + err.toString(),
        err.toString().contains("Refusing to follow symbolic link"));
  }

  @Test
  public void testReadRegularFileRefusesDirectory() throws Exception {
    File dir = tempFolder.newFolder("a-dir");
    StringWriter err = new StringWriter();

    byte[] bytes = KeystoreCliUtils.readRegularFile(dir, 1024, "File",
        new PrintWriter(err));
    assertNull(bytes);
    assertTrue("Expected not-regular-file error, got: " + err.toString(),
        err.toString().contains("Not a regular file"));
  }

  @Test
  public void testReadRegularFileEmptyFile() throws Exception {
    File f = tempFolder.newFile("empty.txt");
    StringWriter err = new StringWriter();

    byte[] bytes = KeystoreCliUtils.readRegularFile(f, 1024, "File",
        new PrintWriter(err));
    assertNotNull(bytes);
    assertEquals(0, bytes.length);
  }
}
