package org.tron.p2p.exception;

import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.exception.P2pException.TypeEnum;

public class P2pExceptionTest {

  @Test
  public void constructorsCarryTypeMessageAndCause() {
    P2pException withMsg = new P2pException(TypeEnum.BAD_MESSAGE, "broken");
    Assert.assertEquals(TypeEnum.BAD_MESSAGE, withMsg.getType());
    Assert.assertEquals("broken", withMsg.getMessage());

    Throwable cause = new IllegalStateException("root");
    P2pException withCause = new P2pException(TypeEnum.PARSE_MESSAGE_FAILED, cause);
    Assert.assertEquals(TypeEnum.PARSE_MESSAGE_FAILED, withCause.getType());
    Assert.assertSame(cause, withCause.getCause());

    P2pException both = new P2pException(TypeEnum.BIG_MESSAGE, "too big", cause);
    Assert.assertEquals("too big", both.getMessage());
    Assert.assertSame(cause, both.getCause());
  }

  @Test
  public void typeValuesAreDistinctAndDescribed() {
    java.util.Set<Integer> values = new java.util.HashSet<>();
    for (TypeEnum type : TypeEnum.values()) {
      Assert.assertTrue("duplicate value for " + type, values.add(type.getValue()));
      Assert.assertNotNull(type.getDesc());
      Assert.assertFalse(type.getDesc().isEmpty());
      Assert.assertEquals(type.getValue() + ", " + type.getDesc(), type.toString());
    }
    Assert.assertEquals(TypeEnum.values().length, values.size());
  }
}
