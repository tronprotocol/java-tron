package org.tron.core.services.admin.ipc.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.Getter;

/** A console-ready JSON-RPC response, parsed without initializing node logging. */
@Getter(AccessLevel.PACKAGE)
final class IpcResponse {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final String formatted;
  private final boolean successful;

  private IpcResponse(String formatted, boolean successful) {
    this.formatted = formatted;
    this.successful = successful;
  }

  /**
   * Validates a JSON-RPC response and formats it for the interactive console.
   *
   * <p>A response must declare version 2.0, contain an ID and exactly one of result or error.
   * A result field, including a null value, marks a valid response as successful. Text results
   * are unquoted and other values are formatted as JSON. Invalid responses use a fixed message.
   */
  static IpcResponse parse(String response) {
    return parse(response, null);
  }

  /** Also verifies the numeric request ID when called for single-command execution. */
  static IpcResponse parse(String response, Integer expectedId) {
    try {
      JsonNode root = OBJECT_MAPPER.reader()
          .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(response);
      if (!isValidEnvelope(root)) {
        return invalidResponse();
      }
      JsonNode id = root.get("id");
      if (expectedId != null && (!id.isIntegralNumber() || !id.canConvertToInt()
          || id.intValue() != expectedId)) {
        return new IpcResponse("IPC response ID does not match request.", false);
      }
      if (root.has("error")) {
        return parseError(root.get("error"));
      }
      // only output column "result" of jsonrpc and ignore other columns
      return new IpcResponse(formatJsonValue(root.get("result")), true);
    } catch (JsonProcessingException e) {
      return invalidResponse();
    }
  }

  private static boolean isValidEnvelope(JsonNode root) {
    if (root == null || !root.isObject()
        || !"2.0".equals(root.path("jsonrpc").textValue())
        || !root.has("id") || root.has("result") == root.has("error")) {
      return false;
    }
    JsonNode id = root.get("id");
    return id.isTextual() || id.isNumber() || id.isNull();
  }

  private static IpcResponse parseError(JsonNode error) {
    if (!error.isObject() || !error.path("code").isIntegralNumber()
        || !error.path("message").isTextual()) {
      return invalidResponse();
    }
    return new IpcResponse("Error " + error.get("code").asText()
        + ": " + error.get("message").textValue(), false);
  }

  private static IpcResponse invalidResponse() {
    return new IpcResponse("Invalid IPC response.", false);
  }

  private static String formatJsonValue(JsonNode value) throws JsonProcessingException {
    if (value == null || value.isNull()) {
      return "null";
    }
    if (value.isTextual()) {
      return value.asText();
    }
    return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
  }
}
