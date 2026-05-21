package org.tron.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.StreamReadConstraints;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.junit.Test;
import org.tron.core.Constant;

/**
 * Tests for Jackson {@code JsonReadFeature} compatibility with Fastjson 1.x.
 */
public class JsonTest {

  @Test
  public void testUnquotedFieldNames() {
    assertEquals(1, JSON.parseObject("{a:1}").getIntValue("a"));
  }

  @Test
  public void testDupFieldNames() {
    assertEquals(2, JSON.parseObject("{a:1, a:2 }").getIntValue("a"));
    assertEquals(1, JSON.parseObject("{a:2, a:1 }").getIntValue("a"));
  }

  @Test
  public void testSingleQuotes() {
    assertEquals(1,  JSON.parseObject("{'a':'1'}").getIntValue("a"));
  }

  @Test
  public void testTrailingComma() {
    assertEquals(1, JSON.parseObject("{\"a\":1,}").getIntValue("a"));
    assertEquals(2, JSON.parseArray("[1,2,]").size());
    assertThrows(JSONException.class, () -> JSON.parseObject("{c:'NULL',,,,,,}"));
    assertThrows(JSONException.class, () -> JSON.parseObject("[1,,2]"));
  }

  @Test
  public void testNonNumericNumbers() {
    assertThrows(JSONException.class, () -> JSON.parseObject("{\"a\":NaN}"));
    assertThrows(JSONException.class, () -> JSON.parseArray("[1, NaN, 2]"));
    assertThrows(JSONException.class, () -> JSON.parseObject("{outer:{inner:NaN}}"));
    assertThrows(JSONException.class, () -> JSON.parseObject("{b:Infinity}"));
    assertThrows(JSONException.class, () -> JSON.parseObject("{c:-Infinity}"));
    assertThrows(JSONException.class, () -> JSON.parseArray("[Infinity]"));
  }

  @Test
  public void testLeadingNumbers() {
    JSONObject o = JSON.parseObject("{'a':+1,b:-2,c:.3,d:-.4,e:+.5,f:+6.,h:007}");
    assertNotNull(o);
    assertEquals(1, o.getIntValue("a"));
    assertEquals(-2, o.getIntValue("b"));
    assertEquals("0.3", o.getBigDecimal("c").toPlainString());
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
    assertThrows(JSONException.class, () -> JSON.parseObject("{\"a\":\"\\q\"}"));
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
    assertThrows(JSONException.class, () -> JSON.parseObject("NULL"));
  }

  @Test
  public void testThrows() {
    assertThrows(JSONException.class, () -> JSON.parseObject("{a:abc}"));
    assertThrows(JSONException.class, () -> JSON.parseObject("{a:TRUE}"));
    assertThrows(JSONException.class, () -> JSON.parseObject("{a:FALSE}"));
    assertThrows(JSONException.class, () -> JSON.parseObject("[1,,3]"));
    // valid JSON but wrong shape — exercises the single-arg JSONException constructor
    assertThrows(JSONException.class, () -> JSON.parseObject("[1,2,3]"));
  }

  @Test
  public void testUppercaseNullRejected() {
    assertThrows(JSONException.class, () -> JSON.parseObject("{\"a\":NULL,\"b\":1}"));
    assertThrows(JSONException.class, () -> JSON.parseArray("[NULL, null]"));
    assertThrows(JSONException.class, () -> JSON.parse("NULL"));
    // String value containing the substring "NULL" must be preserved verbatim.
    JSONObject q = JSON.parseObject("{\"k\":\"NULL\"}");
    assertEquals("NULL", q.getString("k"));
  }

  @Test
  public void testParseHelpers() {
    assertNotNull(JSON.parse("{\"a\":1}"));
    assertEquals(1, JSON.parse("{\"a\":1}").get("a").intValue());
    assertNull(JSON.parse(null));
    assertNull(JSON.parse("null"));

    // JSONObject.parseObject delegate
    assertEquals(1, JSONObject.parseObject("{\"a\":1}").getIntValue("a"));

    // JSONArray.parseArray null inputs
    assertNull(JSONArray.parseArray(null));
    assertNull(JSONArray.parseArray("null"));
  }

  @Test
  public void testToJSONString() {
    assertEquals("null", JSON.toJSONString(null));
    assertEquals("{\"a\":1}", JSON.toJSONString(new JSONObject().put("a", 1)));
    assertEquals("[1,2]", JSON.toJSONString(JSON.parseArray("[1,2]")));
    assertEquals("\"hi\"", JSON.toJSONString("hi"));
    assertEquals("0", JSON.toJSONString(new Date(0)));

    // pretty variant differs from compact for containers (exercises the pretty branch)
    JSONObject obj = new JSONObject().put("a", 1);
    assertNotEquals(JSON.toJSONString(obj, false), JSON.toJSONString(obj, true));
    JSONArray arr = JSON.parseArray("[1,2]");
    assertNotEquals(JSON.toJSONString(arr, false), JSON.toJSONString(arr, true));
    // pretty for primitive types is just the JSON literal
    assertEquals("\"hi\"", JSON.toJSONString("hi", true));
  }

  @Test
  public void testJsonObjectGetters() {
    JSONObject o = JSON.parseObject(
        "{'b':true,'i':42,'l':9000000000,'s':'hi','nested':{'k':'v'},'arr':[1,2]}");
    assertNotNull(o);

    assertEquals(Boolean.TRUE, o.getBoolean("b"));
    assertEquals(Integer.valueOf(42), o.getInteger("i"));
    assertEquals(42L, o.getLongValue("i"));
    assertEquals(Long.valueOf(9_000_000_000L), o.getLong("l"));
    assertEquals("hi", o.getString("s"));
    assertEquals("v", o.getJSONObject("nested").getString("k"));
    assertEquals(2, o.getJSONArray("arr").size());

    // getString on container serializes to JSON
    assertTrue(o.getString("nested").contains("\"k\""));
    assertTrue(o.getString("arr").contains("1"));

    // missing keys → null / 0 for primitive accessors
    assertNull(o.getString("missing"));
    assertNull(o.getJSONObject("missing"));
    assertNull(o.getJSONArray("missing"));
    assertNull(o.getBoolean("missing"));
    assertNull(o.getInteger("missing"));
    assertNull(o.getLong("missing"));
    assertNull(o.getBigDecimal("missing"));
    assertEquals(0, o.getIntValue("missing"));
    assertEquals(0L, o.getLongValue("missing"));

    // getObject(key, Class) — JSONObject/JSONArray short-circuits + POJO via Jackson
    assertEquals("v", o.getObject("nested", JSONObject.class).getString("k"));
    assertEquals(2, o.getObject("arr", JSONArray.class).size());
    assertNull(o.getObject("missing", Pojo.class));

    Pojo nested = JSON.parseObject("{\"obj\":{\"name\":\"x\"}}").getObject("obj", Pojo.class);
    assertEquals("x", nested.name);

    // Fastjson compat: getJSONObject / getJSONArray auto-parse stringified JSON
    JSONObject autoParse = JSON.parseObject(
        "{'jsonStr':'{\"k\":\"v\"}','arrStr':'[1,2,3]'}");
    assertEquals("v", autoParse.getJSONObject("jsonStr").getString("k"));
    assertEquals(3, autoParse.getJSONArray("arrStr").size());
  }

  @Test
  public void testJsonObjectPutAndEquality() {
    JSONObject o = new JSONObject();
    o.put("s", "str");
    o.put("b", Boolean.TRUE);
    o.put("i", Integer.valueOf(1));
    o.put("l", Long.valueOf(2L));
    o.put("nested", new JSONObject().put("k", "v"));
    o.put("arr", new JSONArray().add(new JSONObject().put("x", 1)));
    o.put("o_json", (Object) new JSONObject().put("k2", "v2"));
    o.put("o_str", (Object) "fallthrough");
    o.put("list", Arrays.asList("raw", null, new JSONObject().put("a", 1),
        new JSONArray()));

    assertEquals(9, o.size());
    assertEquals(9, o.keySet().size());
    assertTrue(o.containsKey("s"));

    // null put removes the key for every typed overload
    o.put("s", (String) null);
    o.put("b", (Boolean) null);
    o.put("i", (Integer) null);
    o.put("l", (Long) null);
    o.put("nested", (JSONObject) null);
    o.put("arr", (JSONArray) null);
    o.put("o_json", (Object) null);
    o.put("list", (List<?>) null);
    assertFalse(o.containsKey("s"));
    assertEquals(1, o.size()); // only "o_str" remains

    // remove returns the converted value
    o.put("k", 7);
    assertEquals(7, o.remove("k"));
    assertNull(o.remove("nonexistent"));

    // unwrap, toString, toJSONString
    assertNotNull(o.unwrap());
    assertEquals(o.toString(), o.toJSONString());

    // equals / hashCode round-trip
    JSONObject copy = JSON.parseObject(o.toString());
    assertEquals(o, copy);
    assertEquals(o.hashCode(), copy.hashCode());
    assertNotEquals(o, "not a json");
    assertNotEquals(o, null);
    assertEquals(o, o);
  }

  @Test
  public void testJsonArrayOps() {
    JSONArray arr = new JSONArray();
    arr.add(new JSONObject().put("k", "v"));
    arr.add(null); // becomes a JSON null node

    assertEquals(2, arr.size());
    assertEquals("v", arr.getJSONObject(0).getString("k"));
    assertNull(arr.getJSONObject(1));
    assertTrue(arr.getString(0).contains("\"k\""));
    assertNull(arr.getString(1));

    // iterator covers JSONObject + null branches
    int count = 0;
    for (Object e : arr) {
      if (count == 0) {
        assertTrue(e instanceof JSONObject);
      } else {
        assertNull(e);
      }
      count++;
    }
    assertEquals(2, count);

    // toString / toJSONString equivalence + unwrap
    assertEquals(arr.toString(), arr.toJSONString());
    assertNotNull(arr.unwrap());
    assertEquals(2, arr.unwrap().size());

    // equals / hashCode round-trip
    JSONArray copy = JSON.parseArray(arr.toString());
    assertEquals(arr, copy);
    assertEquals(arr.hashCode(), copy.hashCode());
    assertNotEquals("not array", arr);
    assertEquals(arr, arr);

    // Fastjson compat: getJSONObject auto-parses stringified JSON elements
    JSONArray stringified = JSON.parseArray("[\"{\\\"k\\\":\\\"v\\\"}\"]");
    assertEquals("v", stringified.getJSONObject(0).getString("k"));
  }

  @Test
  public void testTypeUtilsCoercion() {
    // null inputs across all coercers
    assertNull(TypeUtils.castToBoolean(null));
    assertNull(TypeUtils.castToInt(null));
    assertNull(TypeUtils.castToLong(null));
    assertNull(TypeUtils.castToBigDecimal(null));

    // direct passthrough (Boolean / Number / BigDecimal / BigInteger)
    assertEquals(Boolean.TRUE, TypeUtils.castToBoolean(Boolean.TRUE));
    assertEquals(Boolean.TRUE, TypeUtils.castToBoolean(Integer.valueOf(1)));
    assertEquals(Boolean.FALSE, TypeUtils.castToBoolean(Integer.valueOf(0)));
    assertEquals(Boolean.TRUE, TypeUtils.castToBoolean(new BigDecimal("1")));
    assertEquals(Integer.valueOf(7), TypeUtils.castToInt(Integer.valueOf(7)));
    assertEquals(Integer.valueOf(7), TypeUtils.castToInt(new BigDecimal("7")));
    assertEquals(Integer.valueOf(7), TypeUtils.castToInt(Long.valueOf(7L))); // Number branch
    assertEquals(Long.valueOf(7L), TypeUtils.castToLong(Long.valueOf(7L)));
    assertEquals(Long.valueOf(7L), TypeUtils.castToLong(new BigDecimal("7")));
    assertEquals(Long.valueOf(7L), TypeUtils.castToLong(Integer.valueOf(7))); // Number branch

    // string-null literals → null
    assertNull(TypeUtils.castToBoolean(""));
    assertNull(TypeUtils.castToBoolean("null"));
    assertNull(TypeUtils.castToBoolean("NULL"));
    assertNull(TypeUtils.castToInt(""));
    assertNull(TypeUtils.castToInt("null"));
    assertNull(TypeUtils.castToInt("NULL"));
    assertNull(TypeUtils.castToLong(""));
    assertNull(TypeUtils.castToLong("null"));
    assertNull(TypeUtils.castToLong("NULL"));
    assertNull(TypeUtils.castToBigDecimal(""));
    assertNull(TypeUtils.castToBigDecimal("null"));

    // boolean string parsing — covers all token branches
    assertEquals(Boolean.TRUE, TypeUtils.castToBoolean("true"));
    assertEquals(Boolean.TRUE, TypeUtils.castToBoolean("TRUE"));
    assertEquals(Boolean.TRUE, TypeUtils.castToBoolean("1"));
    assertEquals(Boolean.FALSE, TypeUtils.castToBoolean("false"));
    assertEquals(Boolean.FALSE, TypeUtils.castToBoolean("FALSE"));
    assertEquals(Boolean.FALSE, TypeUtils.castToBoolean("0"));
    assertEquals(Boolean.TRUE, TypeUtils.castToBoolean("Y"));
    assertEquals(Boolean.TRUE, TypeUtils.castToBoolean("T"));
    assertEquals(Boolean.FALSE, TypeUtils.castToBoolean("F"));
    assertEquals(Boolean.FALSE, TypeUtils.castToBoolean("N"));

    // numeric string — comma stripping + trailing-zero stripping (Fastjson compat)
    assertEquals(Integer.valueOf(1000), TypeUtils.castToInt("1,000"));
    assertEquals(Integer.valueOf(1), TypeUtils.castToInt("1.0"));
    assertEquals(Long.valueOf(2_000L), TypeUtils.castToLong("2,000"));
    assertEquals(Long.valueOf(9_000_000_000L), TypeUtils.castToLong("9000000000"));

    // Boolean → numeric
    assertEquals(Integer.valueOf(1), TypeUtils.castToInt(Boolean.TRUE));
    assertEquals(Integer.valueOf(0), TypeUtils.castToInt(Boolean.FALSE));
    assertEquals(Long.valueOf(1L), TypeUtils.castToLong(Boolean.TRUE));
    assertEquals(Long.valueOf(0L), TypeUtils.castToLong(Boolean.FALSE));

    // BigDecimal: NaN / Infinity → null; BigInteger conversion; string with comma
    assertEquals(new BigDecimal("3.14"),
        TypeUtils.castToBigDecimal(new BigDecimal("3.14")));
    assertEquals(new BigDecimal(BigInteger.TEN),
        TypeUtils.castToBigDecimal(BigInteger.TEN));
    assertNull(TypeUtils.castToBigDecimal(Float.NaN));
    assertNull(TypeUtils.castToBigDecimal(Float.POSITIVE_INFINITY));
    assertNull(TypeUtils.castToBigDecimal(Double.NaN));
    assertNull(TypeUtils.castToBigDecimal(Double.NEGATIVE_INFINITY));
    assertEquals(new BigDecimal("1000.5"), TypeUtils.castToBigDecimal("1,000.5"));
    assertEquals(new BigDecimal("123"), TypeUtils.castToBigDecimal(Integer.valueOf(123)));

    // intValue / longValue helpers — null + normal scale
    assertEquals(0, TypeUtils.intValue(null));
    assertEquals(0L, TypeUtils.longValue(null));
    assertEquals(7, TypeUtils.intValue(new BigDecimal("7")));
    assertEquals(7L, TypeUtils.longValue(new BigDecimal("7")));
  }

  @Test
  public void testJsonMapperHasConfiguredConstraints() {
    StreamReadConstraints sr = JSON.MAPPER.getFactory().streamReadConstraints();
    assertEquals(Constant.MAX_NESTING_DEPTH, sr.getMaxNestingDepth());
    assertEquals((long) Constant.MAX_TOKEN_COUNT, sr.getMaxTokenCount());
  }

  @Test
  public void testParseObjectRejectsOverDepth() {
    StringBuilder open = new StringBuilder();
    StringBuilder close = new StringBuilder();
    for (int i = 0; i < 120; i++) {
      open.append("{\"a\":");
      close.append("}");
    }
    String deep = open + "1" + close;
    JSONException e = assertThrows(JSONException.class, () -> JSON.parseObject(deep));
    assertTrue(e.getMessage().toLowerCase(Locale.ROOT).contains("depth"));
  }

  @Test
  public void testParseArrayRejectsOverTokenCount() {
    StringBuilder sb = new StringBuilder("[0");
    for (int i = 1; i < 100_500; i++) {
      sb.append(",0");
    }
    sb.append(']');
    JSONException e = assertThrows(JSONException.class, () -> JSON.parseArray(sb.toString()));
    assertTrue(e.getMessage().toLowerCase(Locale.ROOT).contains("token"));
  }

  public static class Pojo {
    public String name;
  }
}
