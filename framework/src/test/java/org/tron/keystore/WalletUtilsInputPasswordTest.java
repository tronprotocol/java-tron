package org.tron.keystore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Verifies that {@link WalletUtils#inputPassword()} preserves the full
 * password including internal whitespace on the non-TTY (stdin) path,
 * and that {@link WalletUtils#stripPasswordLine(String)} handles all
 * edge cases correctly.
 *
 * <p>Previously the non-TTY path applied {@code trim() + split("\\s+")[0]}
 * which silently truncated passphrases like "correct horse battery staple"
 * to "correct" when piped via stdin. This test locks in the fix.
 */
public class WalletUtilsInputPasswordTest {

  private InputStream originalIn;

  @Before
  public void saveStdin() {
    originalIn = System.in;
    // Clear the cached Scanner so each test binds to its own System.in
    WalletUtils.resetSharedStdinScanner();
  }

  @After
  public void restoreStdin() {
    System.setIn(originalIn);
    WalletUtils.resetSharedStdinScanner();
  }

  // ---------- inputPassword() behavioral tests ----------

  @Test(timeout = 5000)
  public void testInputPasswordPreservesInternalWhitespace() {
    System.setIn(new ByteArrayInputStream(
        "correct horse battery staple\n".getBytes(StandardCharsets.UTF_8)));

    String pw = WalletUtils.inputPassword();

    assertEquals("Password with internal whitespace must be preserved intact",
        "correct horse battery staple", pw);
  }

  @Test(timeout = 5000)
  public void testInputPasswordPreservesTabs() {
    System.setIn(new ByteArrayInputStream(
        "pass\tw0rd\n".getBytes(StandardCharsets.UTF_8)));

    String pw = WalletUtils.inputPassword();

    assertEquals("Internal tabs must be preserved", "pass\tw0rd", pw);
  }

  @Test(timeout = 5000)
  public void testInputPasswordStripsTrailingCr() {
    // Windows line endings
    System.setIn(new ByteArrayInputStream(
        "password123\r\n".getBytes(StandardCharsets.UTF_8)));

    String pw = WalletUtils.inputPassword();

    assertEquals("Trailing \\r must be stripped", "password123", pw);
  }

  @Test(timeout = 5000)
  public void testInputPasswordStripsBom() {
    System.setIn(new ByteArrayInputStream(
        "\uFEFFpassword123\n".getBytes(StandardCharsets.UTF_8)));

    String pw = WalletUtils.inputPassword();

    assertEquals("UTF-8 BOM must be stripped from the start", "password123", pw);
  }

  @Test(timeout = 5000)
  public void testInputPasswordPreservesLeadingAndTrailingSpaces() {
    // The legacy bug also called trim(); post-fix, spaces at the edges
    // are part of the password. Callers that want to trim should do so
    // themselves with full knowledge.
    System.setIn(new ByteArrayInputStream(
        "  with spaces  \n".getBytes(StandardCharsets.UTF_8)));

    String pw = WalletUtils.inputPassword();

    assertEquals("Leading and trailing spaces are part of the password",
        "  with spaces  ", pw);
  }

  @Test(timeout = 10000)
  public void testInputPassword2TwicePipedPreservesInternalWhitespace() {
    // M1: verifies the double-read path (inputPassword2Twice → inputPassword()
    // called twice) works correctly when both lines arrive on the same
    // piped stdin. Guards against regressions from Scanner lifecycle issues
    // where a newly-constructed Scanner could miss bytes buffered by an
    // earlier Scanner on the same InputStream.
    System.setIn(new ByteArrayInputStream(
        ("correct horse battery staple\n"
            + "correct horse battery staple\n").getBytes(StandardCharsets.UTF_8)));

    String pw = WalletUtils.inputPassword2Twice();

    assertEquals("Full passphrase must survive the double-read path",
        "correct horse battery staple", pw);
  }

  // ---------- stripPasswordLine() direct unit tests (M3) ----------

  @Test
  public void testStripPasswordLineNull() {
    assertNull(WalletUtils.stripPasswordLine(null));
  }

  @Test
  public void testStripPasswordLineEmpty() {
    assertEquals("", WalletUtils.stripPasswordLine(""));
  }

  @Test
  public void testStripPasswordLineOnlyBom() {
    assertEquals("", WalletUtils.stripPasswordLine("\uFEFF"));
  }

  @Test
  public void testStripPasswordLineOnlyLineTerminators() {
    assertEquals("", WalletUtils.stripPasswordLine("\r\n\r\n"));
  }

  @Test
  public void testStripPasswordLineBomThenTerminator() {
    assertEquals("", WalletUtils.stripPasswordLine("\uFEFF\r\n"));
  }

  @Test
  public void testStripPasswordLineBomAndInternalWhitespace() {
    assertEquals("with spaces",
        WalletUtils.stripPasswordLine("\uFEFFwith spaces\r\n"));
  }

  @Test
  public void testStripPasswordLineNoChange() {
    assertEquals("password", WalletUtils.stripPasswordLine("password"));
  }

  @Test
  public void testStripPasswordLineTrailingLf() {
    assertEquals("password", WalletUtils.stripPasswordLine("password\n"));
  }

  @Test
  public void testStripPasswordLineTrailingCr() {
    assertEquals("password", WalletUtils.stripPasswordLine("password\r"));
  }

  @Test
  public void testStripPasswordLineMultipleTrailing() {
    assertEquals("password", WalletUtils.stripPasswordLine("password\r\n\r\n"));
  }
}
