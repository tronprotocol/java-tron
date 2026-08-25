package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.security.InvalidParameterException;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.tron.json.JSONObject;

public class JsonLongValueValidationTest {

  private static JSONObject value(String value) {
    return JSONObject.parseObject("{\"id\":\"" + value + "\"}");
  }

  /** The same value submitted unquoted, which reaches the node as a number rather than a string. */
  private static JSONObject unquoted(String value) {
    return JSONObject.parseObject("{\"id\":" + value + "}");
  }

  @Test
  public void stringLengthBoundaryIsEnforcedBeforeDecimalConversion() {
    assertEquals(0L, Util.getJsonLongValue(value(StringUtils.repeat('0', 64)), "id", true));

    String oversized = StringUtils.repeat('9', 65);
    InvalidParameterException exception = assertThrows(InvalidParameterException.class,
        () -> Util.getJsonLongValue(value(oversized), "id", true));
    assertTrue(exception.getMessage().contains("id"));
    assertFalse(exception.getMessage().contains(oversized));
  }

  /** An unquoted token reaches this check as an already-normalized Number. */
  @Test
  public void unquotedNumberNormalizedTextLengthBoundaryIsEnforced() {
    // json forbids leading zeros, so the accepted case is shown by the failure it reaches
    // instead: at 64 characters the bound is passed and the exact conversion is what rejects it
    assertThrows(ArithmeticException.class,
        () -> Util.getJsonLongValue(unquoted(StringUtils.repeat('9', 64)), "id", true));

    String oversized = StringUtils.repeat('9', 65);
    InvalidParameterException exception = assertThrows(InvalidParameterException.class,
        () -> Util.getJsonLongValue(unquoted(oversized), "id", true));
    assertTrue(exception.getMessage().contains("id"));
    assertFalse(exception.getMessage().contains(oversized));
  }

  @Test
  public void exactLongRepresentationsRemainCompatible() {
    assertEquals(Long.MIN_VALUE,
        Util.getJsonLongValue(value(Long.toString(Long.MIN_VALUE)), "id", true));
    assertEquals(Long.MAX_VALUE,
        Util.getJsonLongValue(value(Long.toString(Long.MAX_VALUE)), "id", true));
    assertEquals(0L, Util.getJsonLongValue(value("0"), "id", true));
    assertEquals(1L, Util.getJsonLongValue(value("1.0"), "id", true));
    assertEquals(100L, Util.getJsonLongValue(value("1e2"), "id", true));
  }

  @Test
  public void existingExactConversionFailuresRemainInEffect() {
    assertThrows(ArithmeticException.class,
        () -> Util.getJsonLongValue(value("1.5"), "id", true));
    assertThrows(ArithmeticException.class,
        () -> Util.getJsonLongValue(value("9223372036854775808"), "id", true));
  }

  /**
   * A container is not a number. It used to skip the length bound altogether and reach the
   * conversion as its full serialized form, bounded only by the shim's 65535-character fallback.
   */
  @Test
  public void nonNumericTypesAreRejectedBeforeConversion() {
    String digits = StringUtils.repeat('9', 128);
    String[] rawJsonValues = {"[" + digits + "]", "{\"a\":" + digits + "}", "true"};
    for (String rawJsonValue : rawJsonValues) {
      JSONObject jsonObject = JSONObject.parseObject("{\"id\":" + rawJsonValue + "}");
      InvalidParameterException exception = assertThrows(InvalidParameterException.class,
          () -> Util.getJsonLongValue(jsonObject, "id", true));
      assertTrue(exception.getMessage().contains("id"));
      assertFalse(exception.getMessage().contains(digits));
    }
  }

  /** An absent optional key must stay distinguishable from one the node refuses. */
  @Test
  public void optionalMissingValueStillDefaultsToZero() {
    assertEquals(0L, Util.getJsonLongValue(new JSONObject(), "id", false));
  }

  @Test
  public void requiredMissingValueStillFails() {
    assertThrows(InvalidParameterException.class,
        () -> Util.getJsonLongValue(new JSONObject(), "id", true));
  }
}
