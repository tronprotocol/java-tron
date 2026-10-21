package org.web3j.utils;

import org.junit.Assert;
import org.junit.Test;

public class AssertionsTest {

  @Test
  public void satisfiedPreconditionIsSilent() {
    Assertions.verifyPrecondition(true, "should not be thrown");
  }

  @Test
  public void failedPreconditionCarriesTheMessage() {
    try {
      Assertions.verifyPrecondition(false, "boom");
      Assert.fail("expected a RuntimeException");
    } catch (RuntimeException e) {
      Assert.assertEquals("boom", e.getMessage());
    }
  }
}
