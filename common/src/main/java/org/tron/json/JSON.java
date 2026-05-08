package org.tron.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.tron.common.parameter.CommonParameter;

/**
 * Drop-in replacement for {@code com.alibaba.fastjson.JSON}.
 *
 * @deprecated Compatibility shim from the fastjson removal. New code should use
 *     Jackson directly ({@link com.fasterxml.jackson.databind.ObjectMapper},
 *     {@link com.fasterxml.jackson.databind.JsonNode}) instead of this helper.
 */
@Deprecated
public final class JSON {

  // Initialization-order invariant: this class must NOT be loaded before
  // Args.setParam() completes. The factory's StreamReadConstraints are a
  // one-shot snapshot of CommonParameter at class-init time. If JSON is
  // touched too early — e.g. a stray reference in startup code or in a static
  // initializer that runs before Args — the snapshot captures CommonParameter's
  // hardcoded defaults (100 / 100_000) and any user override of
  // node.http.maxNestingDepth / maxTokenCount is silently ignored.
  // Current production startup (FullNode.main) calls Args.setParam first and
  // no path in that call chain references this class, so the invariant holds.
  static final ObjectMapper MAPPER = JsonMapper.builder(buildFactory())
      // Fastjson Feature.AllowUnQuotedFieldNames (default ON)
      .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
      // Fastjson Feature.AllowSingleQuotes (default ON)
      .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
      // Partial compatibility with Fastjson Feature.AllowArbitraryCommas:
      // this only covers a single trailing comma like {"a":1,} or [1,2,].
      // Repeated/arbitrary commas like {"a":1,,,,} and [1,,2] remain rejected.
      .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
      // Fastjson accepts a leading plus sign for numbers (for example +123, +0.5)
      .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
      // Partial compatibility for Fastjson's asymmetric decimal behavior:
      // Fastjson accepts +.5 but rejects .5 by default. Jackson cannot model only
      // the signed form, so enabling this also accepts .5.
      .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
      // Fastjson accepts a trailing decimal point for numbers (for example 5.)
      .enable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS)
      // Fastjson accepts leading zeros for numbers (for example 007)
      .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
      // Fastjson accepts unescaped control chars in strings (for example raw tab/newline)
      .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
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
      .build();

  private static JsonFactory buildFactory() {
    CommonParameter p = CommonParameter.getInstance();
    return JsonFactory.builder().streamReadConstraints(StreamReadConstraints.builder()
            .maxNestingDepth(p.getMaxNestingDepth()).maxTokenCount(p.getMaxTokenCount())
            .build()).build();
  }

  private JSON() {
  }

  /**
   * Returns {@code true} when {@code text} is null, blank, or a
   * lowercase {@code "null"} literal.
   */
  static boolean isNullLiteral(String text) {
    if (text == null) {
      return true;
    }
    String trimmed = text.trim();
    return trimmed.isEmpty() || "null".equals(trimmed);
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
    } catch (JSONException e) {
      throw e;
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
