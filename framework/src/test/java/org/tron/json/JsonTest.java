package org.tron.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for Jackson {@code JsonReadFeature} compatibility with Fastjson 1.x.
 */
public class JsonTest {

  @Test
  public void testUnquotedFieldNames() {
    assertEquals(1, JSON.parseObject("{a:1}").getIntValue("a"));
  }

  @Test
  public void testSingleQuotes() {
    assertEquals(1,  JSON.parseObject("{'a':'1'}").getIntValue("a"));
  }

  @Test
  public void testTrailingComma() {
    JSONArray array = JSON.parseArray("[1,2,{\"a\":1,},]");
    assertEquals(3, array.size());
    assertEquals(2, array.get(1));
    assertEquals(1, ((JSONObject) array.get(2)).getIntValue("a"));
  }

  @Test
  public void testNonNumericNumbers() {
    JSONObject json = JSON.parseObject("{a:NaN, b:Infinity, c:-Infinity}");
    assertNotNull(json);
    double val = ((Number) json.get("a")).doubleValue();
    assertTrue(Double.isNaN(val)); // Fastjson is null, but jackson parses as NaN
    val = ((Number) json.get("b")).doubleValue();
    assertTrue(Double.isInfinite(val) && val > 0); // Fastjson will throw an error
    val = ((Number) json.get("c")).doubleValue();
    assertTrue(Double.isInfinite(val) && val < 0); // Fastjson will throw an error
  }

  @Test
  public void testLeadingNumbers() {
    JSONObject o = JSON.parseObject("{'a':+1,b:-2,c:.3,d:-.4,e:+.5,f:+6.,h:007}");
    assertNotNull(o);
    assertEquals(1, o.getIntValue("a"));
    assertEquals(-2, o.getIntValue("b"));
    assertEquals("0.3", o.getBigDecimal("c").toPlainString()); // Fastjson will throw an error
    assertEquals("-0.4", o.getBigDecimal("d").toPlainString());
    assertEquals("0.5", o.getBigDecimal("e").toPlainString());
    assertEquals(6, o.getIntValue("f"));
    assertEquals(7, o.getIntValue("h"));
  }

  @Test
  public void testUnescapedControlChars() {
    JSONObject obj = JSON.parseObject("{'a':'line1\n\tline2'}");
    assertNotNull(obj);
    assertEquals("line1\n\tline2", obj.getString("a"));
    obj = JSON.parseObject("{\"a\":\"\u0001\"}");
    assertNotNull(obj);
    assertEquals("\u0001", obj.getString("a"));
  }

  @Test
  public void testBackslashEscapeAnyChar() {
    JSONObject obj = JSON.parseObject("{\"a\":\"\\q\"}"); // Fastjson will throw an error
    assertNotNull(obj);
    assertEquals("q", obj.getString("a"));
  }

  @Test
  public void testComment() {
    JSONObject obj = JSON.parseObject("{\"a\":1} \n\t // this is a comment");
    assertNotNull(obj);
    assertEquals(1, obj.getIntValue("a"));
    obj = JSON.parseObject("{/* comment */\"a\":1}");
    assertNotNull(obj);
    assertEquals(1, obj.getIntValue("a"));
  }


  @Test
  public void testParseNull() {
    assertNull(JSON.parseObject(null));
    assertNull(JSON.parseObject(""));
    assertNull(JSON.parseObject("   "));
    assertNull(JSON.parseObject("\n\t"));
    assertNull(JSON.parseObject("null"));
    assertNull(JSON.parseObject("NULL"));
  }

  @Test
  public void testThrows() {
    assertThrows(JSONException.class, () -> JSON.parseObject("{a:abc}"));
    assertThrows(JSONException.class, () -> JSON.parseObject("{a:TRUE}"));
    assertThrows(JSONException.class, () -> JSON.parseObject("{a:FALSE}"));
    assertThrows(JSONException.class, () -> JSON.parseObject("[1,,3]"));
    // NOTE: Fastjson 1.x treats unquoted NULL as null, but jackson throws an error
    assertThrows(JSONException.class, () -> JSON.parseObject("{a:NULL}"));
  }

}
