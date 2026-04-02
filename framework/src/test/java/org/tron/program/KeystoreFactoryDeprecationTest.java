package org.tron.program;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import org.junit.Test;

/**
 * Verifies that --keystore-factory prints deprecation warning to stderr.
 */
public class KeystoreFactoryDeprecationTest {

  @Test(timeout = 10000)
  public void testDeprecationWarningPrinted() throws Exception {
    PrintStream originalErr = System.err;
    InputStream originalIn = System.in;
    ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    System.setErr(new PrintStream(errContent));
    System.setIn(new java.io.ByteArrayInputStream("exit\n".getBytes()));
    try {
      KeystoreFactory.start();
    } finally {
      System.setErr(originalErr);
      System.setIn(originalIn);
    }

    String errOutput = errContent.toString("UTF-8");
    assertTrue("Should contain deprecation warning",
        errOutput.contains("--keystore-factory is deprecated"));
    assertTrue("Should point to Toolkit.jar",
        errOutput.contains("Toolkit.jar keystore"));
  }
}
