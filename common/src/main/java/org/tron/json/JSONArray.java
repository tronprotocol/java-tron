package org.tron.json;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Drop-in replacement for {@code com.alibaba.fastjson.JSONArray}.
 *
 * @deprecated Compatibility shim from the fastjson removal. New code should use
 *     Jackson directly ({@link com.fasterxml.jackson.databind.node.ArrayNode})
 *     instead of this helper.
 */
@Deprecated
public class JSONArray implements Iterable<Object> {

  private final ArrayNode node;

  public JSONArray(ArrayNode node) {
    this.node = node;
  }

  public JSONArray() {
    this.node = JSON.MAPPER.createArrayNode();
  }

  public static JSONArray parseArray(String text) {
    if (JSON.isNullLiteral(text)) {
      return null;
    }
    try {
      JsonNode node = JSON.MAPPER.readTree(text);
      if (node == null || node.isNull()) {
        return null;
      }
      if (!node.isArray()) {
        throw new JSONException("Expected JSON array but got: " + node.getNodeType());
      }
      return new JSONArray((ArrayNode) node);
    } catch (JSONException e) {
      throw e;
    } catch (Exception e) {
      throw new JSONException(e.getMessage(), e);
    }
  }

  public int size() {
    return node.size();
  }

  private void rangeCheck(int index) {
    if (index < 0 || index >= node.size()) {
      throw new IndexOutOfBoundsException(
          "Index: " + index + ", Size: " + node.size());
    }
  }

  @VisibleForTesting
  public Object get(int index) {
    rangeCheck(index);
    return JSONObject.convertNode(node.get(index));
  }

  public JSONObject getJSONObject(int index) {
    rangeCheck(index);
    JsonNode child = node.get(index);
    if (child.isNull()) {
      return null;
    }
    if (child.isObject()) {
      return new JSONObject((ObjectNode) child);
    }
    // Fastjson auto-parses stringified JSON objects
    if (child.isTextual()) {
      return JSON.parseObject(child.asText());
    }
    throw new JSONException("Element at index " + index + " is not an object");
  }

  @VisibleForTesting
  public String getString(int index) {
    rangeCheck(index);
    JsonNode child = node.get(index);
    if (child.isNull()) {
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

  // -------------------------------------------------------------------------
  // Mutation helpers
  // -------------------------------------------------------------------------

  public JSONArray add(JSONObject value) {
    node.add(value == null ? node.nullNode() : value.unwrap());
    return this;
  }

  @JsonValue
  public ArrayNode unwrap() {
    return node;
  }

  @Override
  public Iterator<Object> iterator() {
    List<Object> list = new ArrayList<>();
    node.forEach(child -> list.add(JSONObject.convertNode(child)));
    return list.iterator();
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
    if (!(o instanceof JSONArray)) {
      return false;
    }
    return node.equals(((JSONArray) o).node);
  }

  @Override
  public int hashCode() {
    return node.hashCode();
  }
}
