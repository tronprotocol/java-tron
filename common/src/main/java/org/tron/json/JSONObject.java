package org.tron.json;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Drop-in replacement for {@code com.alibaba.fastjson.JSONObject}.
 *
 * <p>Note: {@code put(key, null)} removes the key instead of storing a JSON
 * {@code null}. This matches Fastjson's default serialization output
 * ({@code WriteMapNullValue=OFF} omits null fields), but differs in
 * {@link #containsKey(String)} / {@link #size()} after a null put. To emit an
 * explicit {@code "key":null}, pass a Jackson {@code NullNode} via
 * {@link #put(String, Object)}.
 *
 * @deprecated Compatibility shim from the fastjson removal. New code should use
 *     Jackson directly ({@link com.fasterxml.jackson.databind.node.ObjectNode})
 *     instead of this helper.
 */
@Deprecated
public class JSONObject {

  private final ObjectNode node;

  public JSONObject(ObjectNode node) {
    this.node = node;
  }

  public JSONObject() {
    this.node = JSON.MAPPER.createObjectNode();
  }

  public static JSONObject parseObject(String text) {
    return JSON.parseObject(text);
  }

  public boolean containsKey(String key) {
    return node.has(key);
  }

  @VisibleForTesting
  public int size() {
    return node.size();
  }

  @VisibleForTesting
  public Set<String> keySet() {
    Set<String> keys = new LinkedHashSet<>(node.size());
    Iterator<String> names = node.fieldNames();
    while (names.hasNext()) {
      keys.add(names.next());
    }
    return keys;
  }

  public String getString(String key) {
    JsonNode child = node.get(key);
    if (child == null || child.isNull()) {
      return null;
    }
    if (child.isContainerNode()) {
      try {
        return JSON.MAPPER.writeValueAsString(child);
      } catch (Exception e) {
        throw new JSONException("Serialization failed: " + e.getMessage(), e);
      }
    }
    return child.asText(null);
  }

  public Boolean getBoolean(String key) {
    return TypeUtils.castToBoolean(get(key));
  }

  public Integer getInteger(String key) {
    return TypeUtils.castToInt(get(key));
  }

  @VisibleForTesting
  public Long getLong(String key) {
    return TypeUtils.castToLong(get(key));
  }

  @VisibleForTesting
  public long getLongValue(String key) {
    Long value = TypeUtils.castToLong(get(key));
    return value == null ? 0L : value;
  }

  @VisibleForTesting
  public int getIntValue(String key) {
    Integer value = TypeUtils.castToInt(get(key));
    return value == null ? 0 : value;
  }

  public BigDecimal getBigDecimal(String key) {
    return TypeUtils.castToBigDecimal(get(key));
  }


  public Object get(String key) {
    return convertNode(node.get(key));
  }

  static Object convertNode(JsonNode child) {
    if (child == null || child.isNull() || child.isMissingNode()) {
      return null;
    }
    if (child.isObject()) {
      return new JSONObject((ObjectNode) child);
    }
    if (child.isArray()) {
      return new JSONArray((ArrayNode) child);
    }
    if (child.isTextual()) {
      return child.asText();
    }
    if (child.isInt()) {
      return child.intValue();
    }
    if (child.isShort()) {
      return child.shortValue();
    }
    if (child.isLong()) {
      return child.longValue();
    }
    if (child.isBigInteger()) {
      return child.bigIntegerValue();
    }
    if (child.isBigDecimal()) {
      return child.decimalValue();
    }
    if (child.isDouble() || child.isFloat()) {
      return child.doubleValue();
    }
    if (child.isBoolean()) {
      return child.booleanValue();
    }
    return child.asText();
  }

  public JSONObject getJSONObject(String key) {
    JsonNode child = node.get(key);
    if (child == null || child.isNull()) {
      return null;
    }
    if (child.isObject()) {
      return new JSONObject((ObjectNode) child);
    }
    // Fastjson auto-parses stringified JSON objects
    if (child.isTextual()) {
      return JSON.parseObject(child.asText());
    }
    throw new JSONException("Field '" + key + "' is not an object");
  }

  public JSONArray getJSONArray(String key) {
    JsonNode child = node.get(key);
    if (child == null || child.isNull()) {
      return null;
    }
    if (child.isArray()) {
      return new JSONArray((ArrayNode) child);
    }
    if (child.isTextual()) {
      return JSON.parseArray(child.asText());
    }
    throw new JSONException("Field '" + key + "' is not an array");
  }

  @VisibleForTesting
  public <T> T getObject(String key, Class<T> clazz) {
    JsonNode child = node.get(key);
    if (child == null || child.isNull()) {
      return null;
    }
    try {
      if (clazz == JSONObject.class) {
        if (!child.isObject()) {
          throw new JSONException(
              "Field '" + key + "' is " + child.getNodeType() + ", cannot convert to JSONObject");
        }
        return clazz.cast(new JSONObject((ObjectNode) child));
      }
      if (clazz == JSONArray.class) {
        if (!child.isArray()) {
          throw new JSONException(
              "Field '" + key + "' is " + child.getNodeType() + ", cannot convert to JSONArray");
        }
        return clazz.cast(new JSONArray((ArrayNode) child));
      }
      return JSON.MAPPER.treeToValue(child, clazz);
    } catch (JSONException e) {
      throw e;
    } catch (Exception e) {
      throw new JSONException(
          "Failed to convert field '" + key + "' to " + clazz.getSimpleName(), e);
    }
  }

  public JSONObject put(String key, String value) {
    if (value == null) {
      node.remove(key);
    } else {
      node.put(key, value);
    }
    return this;
  }

  public JSONObject put(String key, Boolean value) {
    if (value == null) {
      node.remove(key);
    } else {
      node.put(key, value);
    }
    return this;
  }

  public JSONObject put(String key, Integer value) {
    if (value == null) {
      node.remove(key);
    } else {
      node.put(key, value);
    }
    return this;
  }

  public JSONObject put(String key, Long value) {
    if (value == null) {
      node.remove(key);
    } else {
      node.put(key, value);
    }
    return this;
  }

  public JSONObject put(String key, JSONObject value) {
    if (value == null) {
      node.remove(key);
    } else {
      node.set(key, value.unwrap());
    }
    return this;
  }

  public JSONObject put(String key, JSONArray value) {
    if (value == null) {
      node.remove(key);
    } else {
      node.set(key, value.unwrap());
    }
    return this;
  }

  public JSONObject put(String key, Object value) {
    if (value == null) {
      node.remove(key);
      return this;
    }
    if (value instanceof JSONObject) {
      return put(key, (JSONObject) value);
    }
    if (value instanceof JSONArray) {
      return put(key, (JSONArray) value);
    }
    if (value instanceof JsonNode) {
      node.set(key, (JsonNode) value);
      return this;
    }
    node.set(key, JSON.MAPPER.valueToTree(value));
    return this;
  }

  public JSONObject put(String key, List<?> value) {
    if (value == null) {
      node.remove(key);
      return this;
    }
    ArrayNode arr = JSON.MAPPER.createArrayNode();
    for (Object v : value) {
      if (v == null) {
        arr.addNull();
      } else if (v instanceof JSONObject) {
        arr.add(((JSONObject) v).unwrap());
      } else if (v instanceof JSONArray) {
        arr.add(((JSONArray) v).unwrap());
      } else if (v instanceof JsonNode) {
        arr.add((JsonNode) v);
      } else {
        arr.add(JSON.MAPPER.valueToTree(v));
      }
    }
    node.set(key, arr);
    return this;
  }

  public Object remove(String key) {
    JsonNode removed = node.remove(key);
    return convertNode(removed);
  }

  @JsonValue
  public ObjectNode unwrap() {
    return node;
  }

  @Override
  public String toString() {
    try {
      return JSON.MAPPER.writeValueAsString(node);
    } catch (Exception e) {
      throw new JSONException("Serialization failed: " + e.getMessage(), e);
    }
  }

  public String toJSONString() {
    return toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof JSONObject)) {
      return false;
    }
    return node.equals(((JSONObject) o).node);
  }

  @Override
  public int hashCode() {
    return node.hashCode();
  }
}
