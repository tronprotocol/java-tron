package org.tron.core.services.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;

/** Validation shared by the public and Admin JSON-RPC request entry points. */
public final class JsonRpcRequestValidator {

  private static final String INVALID_REQUEST = "Invalid Request";
  public static final String INVALID_NUMERIC_ID_MESSAGE =
      INVALID_REQUEST + ": numeric id must be an integer literal within the signed 64-bit range";

  private JsonRpcRequestValidator() {
  }

  /** Returns the existing public JSON-RPC error message, or null for a valid request envelope. */
  public static String invalidRequestMessage(JsonNode request) {
    if (request != null && request.isObject() && hasInvalidVersion(request)) {
      return INVALID_REQUEST + ": jsonrpc must be \"2.0\"";
    }
    if (request != null && request.isObject() && hasInvalidNumericId(request)) {
      return INVALID_NUMERIC_ID_MESSAGE;
    }
    // An explicit null ID is invalid: jsonrpc4j treats it as a notification and drops the result.
    // Explicit null params retain the public endpoint's existing dispatch behavior.
    if (request == null || !request.isObject()
        || (request.has("id") && !isValidResponseId(request.get("id")))
        || (request.hasNonNull("params") && !request.get("params").isContainerNode())
        || hasInvalidMethod(request)) {
      return INVALID_REQUEST;
    }
    return null;
  }

  /** Invalid and null request IDs are represented as JSON null in error responses. */
  public static boolean isValidResponseId(JsonNode id) {
    return id != null && (id.isTextual() || id.isNumber());
  }

  /** jsonrpc4j parses IDs beyond the long range with asInt() and fractional IDs with asDouble(). */
  private static boolean hasInvalidNumericId(JsonNode request) {
    JsonNode id = request.get("id");
    return id != null && id.isNumber() && !(id.isIntegralNumber() && id.canConvertToLong());
  }

  private static boolean hasInvalidVersion(JsonNode request) {
    JsonNode version = request.get("jsonrpc");
    return version == null || !version.isTextual() || !"2.0".equals(version.asText());
  }

  private static boolean hasInvalidMethod(JsonNode request) {
    JsonNode method = request.get("method");
    return method == null || !method.isTextual() || method.asText().isEmpty();
  }
}
