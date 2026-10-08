package org.web3j.exceptions;

import org.junit.Assert;
import org.junit.Test;

public class MessageExceptionsTest {

  @Test
  public void decodingExceptionCarriesMessageAndCause() {
    MessageDecodingException plain = new MessageDecodingException("bad input");
    Assert.assertEquals("bad input", plain.getMessage());
    Assert.assertNull(plain.getCause());

    Throwable cause = new IllegalStateException("root");
    MessageDecodingException wrapped = new MessageDecodingException("bad input", cause);
    Assert.assertEquals("bad input", wrapped.getMessage());
    Assert.assertSame(cause, wrapped.getCause());
  }

  @Test
  public void encodingExceptionCarriesMessageAndCause() {
    MessageEncodingException plain = new MessageEncodingException("bad value");
    Assert.assertEquals("bad value", plain.getMessage());

    Throwable cause = new IllegalArgumentException("root");
    MessageEncodingException wrapped = new MessageEncodingException("bad value", cause);
    Assert.assertSame(cause, wrapped.getCause());
  }
}
