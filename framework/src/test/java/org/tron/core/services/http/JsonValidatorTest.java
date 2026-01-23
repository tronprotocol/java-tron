package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class JsonValidatorTest {
  @Test
  public void testParseAndGetVisibleTrue() throws Exception {
    String json = "{\"visible\":true,\"other\":\"value\"}";
    assertTrue(JsonValidator.parseAndGetVisible(json));
  }

  @Test
  public void testParseAndGetVisibleFalse() throws Exception {
    String json = "{\"visible\":false}";
    assertFalse(JsonValidator.parseAndGetVisible(json));
  }

  @Test
  public void testParseAndGetVisibleStringTrue() throws Exception {
    String json = "{\"visible\":\"true\"}";
    assertTrue(JsonValidator.parseAndGetVisible(json));
  }

  @Test
  public void testParseAndGetVisibleNotPresent() throws Exception {
    String json = "{\"other\":\"value\"}";
    assertFalse(JsonValidator.parseAndGetVisible(json));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseAndGetVisibleDeepNesting() throws Exception {
    StringBuilder json = new StringBuilder("{\"visible\":true,\"x\":");
    for (int i = 0; i < 150; i++) {
      json.append("{\"x\":");
    }
    json.append("1");
    for (int i = 0; i < 150; i++) {
      json.append("}");
    }
    json.append("}");

    // Should throw due to depth constraint
    JsonValidator.parseAndGetVisible(json.toString());
  }

  @Test
  public void testParseAndGetString() throws Exception {
    String json = "{\"contractType\":\"TransferContract\"}";
    String result = JsonValidator.parseAndGetString(json, "contractType");
    assertEquals("TransferContract", result);
  }

  @Test
  public void testParseAndGetStringNotPresent() throws Exception {
    String json = "{\"other\":\"value\"}";
    String result = JsonValidator.parseAndGetString(json, "missing");
    assertNull(result);
  }

  @Test(expected = Exception.class)
  public void testParseAndGetVisibleMalformedJson() throws Exception {
    String json = "{invalid json}";
    JsonValidator.parseAndGetVisible(json);
  }
}
