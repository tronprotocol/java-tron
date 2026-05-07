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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
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
      // Fastjson also accepts repeated/arbitrary commas like {"a":1,,,,} and
      // [1,,2], which Jackson does not support with this feature.
      .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
      // Fastjson accepts NaN as null but rejects Infinity by default.
      // Jackson enables both with this feature, so every parse path must normalize
      // NaN and reject +/-Infinity after reading.
      .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
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
   * Fastjson 1.x parity: replace bare {@code NULL} tokens with {@code null}
   * in-place so Jackson's strict lowercase-only literal parser accepts them.
   * Skips contents of single- or double-quoted strings (with backslash-escape
   * support) and uses an identifier-aware boundary so unquoted field names
   * like {@code NULL_KEY} are left intact.
   */
  static String coerceUppercaseNull(String text) {
    StringBuilder out = new StringBuilder(text.length());
    int i = 0;
    int n = text.length();
    while (i < n) {
      char c = text.charAt(i);
      if (c == '"' || c == '\'') {
        char quote = c;
        out.append(c);
        i++;
        while (i < n) {
          char ch = text.charAt(i);
          out.append(ch);
          i++;
          if (ch == '\\' && i < n) {
            out.append(text.charAt(i));
            i++;
          } else if (ch == quote) {
            break;
          }
        }
        continue;
      }
      if (c == 'N' && i + 4 <= n
          && text.charAt(i + 1) == 'U'
          && text.charAt(i + 2) == 'L'
          && text.charAt(i + 3) == 'L'
          && (i == 0 || !isIdentChar(text.charAt(i - 1)))
          && (i + 4 == n || !isIdentChar(text.charAt(i + 4)))) {
        out.append("null");
        i += 4;
        continue;
      }
      out.append(c);
      i++;
    }
    return out.toString();
  }

  private static boolean isIdentChar(char c) {
    return Character.isLetterOrDigit(c) || c == '_' || c == '$';
  }

  /**
   * Fast pre-check for Fastjson 1.x non-numeric coercion. Because
   * {@code USE_BIG_DECIMAL_FOR_FLOATS} is on, the only way a {@code Double}
   * {@code NaN} / {@code Infinity} can land in the tree is via the literal
   * tokens {@code NaN} / {@code Infinity} in the source text — large numeric
   * literals go to BigDecimal/BigInteger without overflow. So a substring
   * absence proves the tree has no offending nodes, and the O(n) walk in
   * {@link #coerceNonNumeric} can be skipped on the common case.
   */
  static boolean mayContainNonNumeric(String text) {
    return text != null && (text.contains("Infinity") || text.contains("NaN"));
  }

  /**
   * Fastjson 1.x non-numeric-number parity: silently coerce {@code NaN} to JSON
   * {@code null}, reject {@code Infinity} / {@code -Infinity} with a
   * {@link JSONException} ({@code "syntax error, Infinity"} /
   * {@code "syntax error, -Infinity"}). Walks containers in-place.
   */
  static JsonNode coerceNonNumeric(JsonNode node) {
    if (node == null || node.isNull()) {
      return node;
    }
    if (node.isFloatingPointNumber()) {
      double v = node.doubleValue();
      if (Double.isInfinite(v)) {
        throw new JSONException("syntax error, " + (v > 0 ? "Infinity" : "-Infinity"));
      }
      if (Double.isNaN(v)) {
        return NullNode.getInstance();
      }
      return node;
    }
    if (node.isObject()) {
      ObjectNode obj = (ObjectNode) node;
      List<String> keys = new ArrayList<>();
      obj.fieldNames().forEachRemaining(keys::add);
      for (String k : keys) {
        JsonNode child = obj.get(k);
        JsonNode replacement = coerceNonNumeric(child);
        if (replacement != child) {
          obj.set(k, replacement);
        }
      }
    } else if (node.isArray()) {
      ArrayNode arr = (ArrayNode) node;
      for (int i = 0; i < arr.size(); i++) {
        JsonNode child = arr.get(i);
        JsonNode replacement = coerceNonNumeric(child);
        if (replacement != child) {
          arr.set(i, replacement);
        }
      }
    }
    return node;
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
    String input = text.indexOf("NULL") >= 0 ? coerceUppercaseNull(text) : text;
    try {
      JsonNode node = MAPPER.readTree(input);
      if (mayContainNonNumeric(input)) {
        node = coerceNonNumeric(node);
      }
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
    String input = text.indexOf("NULL") >= 0 ? coerceUppercaseNull(text) : text;
    try {
      JsonNode node = MAPPER.readTree(input);
      if (mayContainNonNumeric(input)) {
        node = coerceNonNumeric(node);
      }
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
