package org.tron.core.exception;

import org.junit.Assert;
import org.junit.Test;

public class ZksnarkExceptionTest {

  @Test
  public void testNoArgConstructor() {
    ZksnarkException e = new ZksnarkException();
    Assert.assertNull(e.getMessage());
    Assert.assertNull(e.getCause());
  }

  @Test
  public void testMessageConstructor() {
    ZksnarkException e = new ZksnarkException("boom");
    Assert.assertEquals("boom", e.getMessage());
    Assert.assertNull(e.getCause());
  }

  @Test
  public void testMessageAndCauseConstructor() {
    Throwable cause = new ArithmeticException("overflow");
    ZksnarkException e = new ZksnarkException("wrapped", cause);
    Assert.assertEquals("wrapped", e.getMessage());
    Assert.assertSame(cause, e.getCause());
  }
}
