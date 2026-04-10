package org.tron.p2p.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DnsExceptionTest {

  // --- Constructor with TypeEnum and message ---

  @Test
  public void testConstructorWithTypeAndMessage() {
    DnsException ex = new DnsException(
        DnsException.TypeEnum.NO_ROOT_FOUND, "detail");
    assertTrue(ex.getMessage().contains("no valid root found"));
    assertTrue(ex.getMessage().contains("detail"));
    assertEquals(DnsException.TypeEnum.NO_ROOT_FOUND, ex.getType());
  }

  // --- Constructor with TypeEnum and Throwable ---

  @Test
  public void testConstructorWithTypeAndThrowable() {
    RuntimeException cause = new RuntimeException("root cause");
    DnsException ex = new DnsException(
        DnsException.TypeEnum.HASH_MISS_MATCH, cause);
    assertSame(cause, ex.getCause());
    assertEquals(DnsException.TypeEnum.HASH_MISS_MATCH, ex.getType());
  }

  // --- Constructor with TypeEnum, message, and Throwable ---

  @Test
  public void testConstructorWithTypeMessageAndThrowable() {
    RuntimeException cause = new RuntimeException("root");
    DnsException ex = new DnsException(
        DnsException.TypeEnum.UNKNOWN_ENTRY, "extra info", cause);
    assertEquals("extra info", ex.getMessage());
    assertSame(cause, ex.getCause());
    assertEquals(DnsException.TypeEnum.UNKNOWN_ENTRY, ex.getType());
  }

  // --- TypeEnum coverage ---

  @Test
  public void testAllTypeEnumValues() {
    DnsException.TypeEnum[] values = DnsException.TypeEnum.values();
    // There are 16 enum constants (0 through 15)
    assertEquals(16, values.length);
  }

  @Test
  public void testTypeEnumGetValue() {
    assertEquals(Integer.valueOf(0), DnsException.TypeEnum.LOOK_UP_ROOT_FAILED.getValue());
    assertEquals(Integer.valueOf(7), DnsException.TypeEnum.NO_PUBLIC_KEY.getValue());
    assertEquals(Integer.valueOf(15), DnsException.TypeEnum.OTHER_ERROR.getValue());
  }

  @Test
  public void testTypeEnumGetDesc() {
    assertEquals("look up root failed",
        DnsException.TypeEnum.LOOK_UP_ROOT_FAILED.getDesc());
    assertEquals("invalid public key",
        DnsException.TypeEnum.BAD_PUBLIC_KEY.getDesc());
    assertEquals("other error",
        DnsException.TypeEnum.OTHER_ERROR.getDesc());
  }

  @Test
  public void testTypeEnumToString() {
    assertEquals("0-look up root failed",
        DnsException.TypeEnum.LOOK_UP_ROOT_FAILED.toString());
    assertEquals("11-invalid base64 signature",
        DnsException.TypeEnum.INVALID_SIGNATURE.toString());
    assertEquals("15-other error",
        DnsException.TypeEnum.OTHER_ERROR.toString());
  }

  @Test
  public void testTypeEnumValueOf() {
    assertEquals(DnsException.TypeEnum.NO_ROOT_FOUND,
        DnsException.TypeEnum.valueOf("NO_ROOT_FOUND"));
    assertEquals(DnsException.TypeEnum.DEPLOY_DOMAIN_FAILED,
        DnsException.TypeEnum.valueOf("DEPLOY_DOMAIN_FAILED"));
  }

  @Test
  public void testAllEnumGettersAndToString() {
    // Exercise getValue(), getDesc(), toString() on every enum to maximize coverage
    for (DnsException.TypeEnum t : DnsException.TypeEnum.values()) {
      assertNotNull(t.getValue());
      assertNotNull(t.getDesc());
      String str = t.toString();
      assertTrue(str.contains("-"));
      assertTrue(str.contains(t.getDesc()));
    }
  }
}
