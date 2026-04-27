package org.tron.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Drop-in replacement for {@code com.alibaba.fastjson.JSON}.
 *
 * @deprecated Compatibility shim from the fastjson removal. New code should use
 *     Jackson directly ({@link com.fasterxml.jackson.databind.ObjectMapper},
 *     {@link com.fasterxml.jackson.databind.JsonNode}) instead of this helper.
 */
@Deprecated
public final class JSON {

  public static final ObjectMapper MAPPER = JsonMapper.builder()
      // Fastjson Feature.AllowUnQuotedFieldNames (default ON)
      .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
      // Fastjson Feature.AllowSingleQuotes (default ON)
      .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
      // Fastjson tolerates trailing commas (e.g. {"a":1,}) by default
      .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
      // Fastjson accepts NaN/Infinity as valid tokens
      .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
      // Fastjson accepts leading plus sign for numbers (e.g. +123)
      .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
      // Fastjson accepts leading decimal point for numbers (e.g. .5)
      .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
      // Fastjson accepts trailing decimal point for numbers (e.g. 5.)
      .enable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS)
      // Fastjson accepts leading zeros for numbers (e.g. 007)
      .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
      // Fastjson accepts unescaped control chars in strings (e.g. raw tab/newline)
      .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
      // Fastjson accepts backslash-escaping any character (e.g. \q → q)
      .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
      // Fastjson accepts Java-style comments (// and /* */)
      .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
      // Fastjson Feature.UseBigDecimal (default ON)
      // https://github.com/alibaba/fastjson/wiki/deserialize_disable_bigdecimal_cn
      .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
      // Fastjson Feature.IgnoreNotMatch (default ON) — unknown fields silently ignored
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      // Fastjson serializes empty beans as "{}" without error
      .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
      // Fastjson omits null-valued fields by default (WriteMapNullValue is OFF by default)
      // https://github.com/alibaba/fastjson/wiki/WriteNull_cn
      .serializationInclusion(JsonInclude.Include.NON_NULL)
      // Fastjson uses WriteDateUseDateFormat (string) not timestamps by default
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      // Fastjson smart-match: field names are matched ignoring case/underscores by default
      // (DisableFieldSmartMatch is OFF by default → smart match ON)
      .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
      .build();

  private JSON() {
  }

  /**
   * Returns {@code true} when {@code text} is null, blank, or a
   * case-insensitive {@code "null"} literal — mirroring Fastjson's lenient
   * treatment of these inputs as JSON {@code null}.
   */
  static boolean isNullLiteral(String text) {
    if (text == null) {
      return true;
    }
    String trimmed = text.trim();
    return trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed);
  }

  public static JSONObject parseObject(String text) {
    if (isNullLiteral(text)) {
      return null;
    }
    try {
      JsonNode node = MAPPER.readTree(text);
      if (node == null || node.isNull()) {
        return null;
      }
      if (!node.isObject()) {
        throw new JSONException("can not cast to JSONObject.");
      }
      return new JSONObject((ObjectNode) node);
    } catch (JSONException e) {
      throw e;
    } catch (Exception e) {
      throw new JSONException(e.getMessage(), e);
    }
  }

  public static <T> T parseObject(String text, Class<T> clazz) {
    if (isNullLiteral(text)) {
      return null;
    }
    if (clazz == JSONObject.class) {
      return clazz.cast(parseObject(text));
    }
    if (clazz == JSONArray.class) {
      return clazz.cast(parseArray(text));
    }
    try {
      return MAPPER.readValue(text, clazz);
    } catch (Exception e) {
      throw new JSONException(e.getMessage(), e);
    }
  }

  public static JsonNode parse(String text) {
    if (isNullLiteral(text)) {
      return null;
    }
    try {
      JsonNode node = MAPPER.readTree(text);
      if (node == null || node.isNull()) {
        return null;
      }
      return node;
    } catch (Exception e) {
      throw new JSONException(e.getMessage(), e);
    }
  }

  static JSONArray parseArray(String text) {
    return JSONArray.parseArray(text);
  }

  public static String toJSONString(Object obj) {
    return toJSONString(obj, false);
  }

  public static String toJSONString(Object obj, boolean pretty) {
    if (obj == null) {
      return "null";
    }
    try {
      if (obj instanceof JSONObject) {
        return pretty ? MAPPER.writerWithDefaultPrettyPrinter()
            .writeValueAsString(((JSONObject) obj).unwrap())
            : MAPPER.writeValueAsString(((JSONObject) obj).unwrap());
      }
      if (obj instanceof JSONArray) {
        return pretty ? MAPPER.writerWithDefaultPrettyPrinter()
            .writeValueAsString(((JSONArray) obj).unwrap())
            : MAPPER.writeValueAsString(((JSONArray) obj).unwrap());
      }
      return pretty ? MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj)
          : MAPPER.writeValueAsString(obj);
    } catch (Exception e) {
      throw new JSONException(e.getMessage(), e);
    }
  }
}
