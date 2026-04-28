package org.tron.program;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.TestConstants;
import org.tron.core.config.args.Args;

/**
 * Verifies the deprecated --keystore-factory CLI.
 */
public class KeystoreFactoryDeprecationTest {

  private PrintStream originalOut;
  private PrintStream originalErr;
  private InputStream originalIn;

  @Before
  public void setup() {
    originalOut = System.out;
    originalErr = System.err;
    originalIn = System.in;
    Args.setParam(new String[] {}, TestConstants.TEST_CONF);
  }

  @After
  public void teardown() throws Exception {
    System.setOut(originalOut);
    System.setErr(originalErr);
    System.setIn(originalIn);
    Args.clearParam();
    // Clean up Wallet dir
    File wallet = new File("Wallet");
    if (wallet.exists()) {
      if (wallet.isDirectory() && wallet.listFiles() != null) {
        for (File f : wallet.listFiles()) {
          f.delete();
        }
      }
      wallet.delete();
    }
  }

  @Test(timeout = 10000)
  public void testDeprecationWarningPrinted() throws Exception {
    ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    System.setErr(new PrintStream(errContent));
    System.setIn(new ByteArrayInputStream("exit\n".getBytes()));

    KeystoreFactory.start();

    String errOutput = errContent.toString("UTF-8");
    assertTrue("Should contain deprecation warning",
        errOutput.contains("--keystore-factory is deprecated"));
    assertTrue("Should point to Toolkit.jar",
        errOutput.contains("Toolkit.jar keystore"));
  }

  @Test(timeout = 10000)
  public void testHelpCommand() throws Exception {
    ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));
    System.setIn(new ByteArrayInputStream("help\nexit\n".getBytes()));

    KeystoreFactory.start();

    String out = outContent.toString("UTF-8");
    assertTrue("Should show legacy commands", out.contains("GenKeystore"));
    assertTrue("Should show ImportPrivateKey", out.contains("ImportPrivateKey"));
  }

  @Test(timeout = 10000)
  public void testInvalidCommand() throws Exception {
    ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));
    System.setIn(new ByteArrayInputStream("badcommand\nexit\n".getBytes()));

    KeystoreFactory.start();

    String out = outContent.toString("UTF-8");
    assertTrue("Should report invalid cmd",
        out.contains("Invalid cmd: badcommand"));
  }

  @Test(timeout = 10000)
  public void testEmptyLineSkipped() throws Exception {
    ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));
    System.setIn(new ByteArrayInputStream("\n\nexit\n".getBytes()));

    KeystoreFactory.start();

    String out = outContent.toString("UTF-8");
    assertTrue("Should exit cleanly", out.contains("Exit"));
  }

  @Test(timeout = 10000)
  public void testQuitCommand() throws Exception {
    ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));
    System.setIn(new ByteArrayInputStream("quit\n".getBytes()));

    KeystoreFactory.start();

    String out = outContent.toString("UTF-8");
    assertTrue("Quit should terminate", out.contains("Exit"));
  }

  @Test(timeout = 10000)
  public void testGenKeystoreTriggersError() throws Exception {
    // genkeystore reads password via a nested Scanner, which conflicts
    // with the outer Scanner and throws "No line found". The error is
    // caught and logged, and the REPL continues.
    ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));
    System.setIn(new ByteArrayInputStream("genkeystore\nexit\n".getBytes()));

    KeystoreFactory.start();

    String out = outContent.toString("UTF-8");
    assertTrue("genKeystore should prompt for password",
        out.contains("Please input password"));
    assertTrue("REPL should continue to exit", out.contains("Exit"));
  }

  @Test(timeout = 10000)
  public void testImportPrivateKeyTriggersPrompt() throws Exception {
    // importprivatekey reads via nested Scanner — same limitation as above,
    // but we at least hit the dispatch logic.
    ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));
    System.setIn(new ByteArrayInputStream("importprivatekey\nexit\n".getBytes()));

    KeystoreFactory.start();

    String out = outContent.toString("UTF-8");
    assertTrue("importprivatekey should prompt for key",
        out.contains("Please input private key"));
  }
}
