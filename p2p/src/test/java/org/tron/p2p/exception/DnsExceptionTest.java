package org.tron.p2p.exception;

import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.exception.DnsException.TypeEnum;

public class DnsExceptionTest {

  @Test
  public void messageConstructorPrefixesTheTypeDescription() {
    DnsException e = new DnsException(TypeEnum.NO_ROOT_FOUND, "nile.trondisco.net");
    Assert.assertEquals(TypeEnum.NO_ROOT_FOUND, e.getType());
    Assert.assertEquals(TypeEnum.NO_ROOT_FOUND.getDesc() + ", nile.trondisco.net",
        e.getMessage());
  }

  @Test
  public void causeConstructorsCarryTheCause() {
    Throwable cause = new IllegalArgumentException("root");
    DnsException fromCause = new DnsException(TypeEnum.INVALID_SIGNATURE, cause);
    Assert.assertSame(cause, fromCause.getCause());
    Assert.assertEquals(TypeEnum.INVALID_SIGNATURE, fromCause.getType());

    DnsException both = new DnsException(TypeEnum.INVALID_ROOT, "bad proto", cause);
    Assert.assertEquals("bad proto", both.getMessage());
    Assert.assertSame(cause, both.getCause());
  }

  @Test
  public void typeValuesAreDistinctAndDescribed() {
    java.util.Set<Integer> values = new java.util.HashSet<>();
    for (TypeEnum type : TypeEnum.values()) {
      Assert.assertTrue("duplicate value for " + type, values.add(type.getValue()));
      Assert.assertNotNull(type.getDesc());
      Assert.assertFalse(type.getDesc().isEmpty());
      // Note DnsException uses "-" as the separator while P2pException uses ", ".
      Assert.assertEquals(type.getValue() + "-" + type.getDesc(), type.toString());
    }
  }
}
