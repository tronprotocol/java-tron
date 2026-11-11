package org.tron.core.services.admin.ipc.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import org.tron.core.services.admin.ipc.client.ActivePeerOutputFormatter.OutputFormat;

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
  static IpcResponse parse(String response, Map<Integer, OutputFormat> pendingOutputFormats) {
    return parse(response, null, pendingOutputFormats);
  }

  /** Also verifies the numeric request ID without changing JSON-RPC result semantics. */
  static IpcResponse parse(String response, Integer expectedId,
      Map<Integer, OutputFormat> pendingOutputFormats) {
    return parse(response, expectedId, pendingOutputFormats, false);
  }

  private static IpcResponse parse(String response, Integer expectedId,
      Map<Integer, OutputFormat> pendingOutputFormats, boolean checkBusinessResult) {
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
      OutputFormat outputFormat = id.isIntegralNumber() && id.canConvertToInt()
          ? pendingOutputFormats.remove(id.intValue()) : OutputFormat.JSON;
      if (root.has("error")) {
        return parseError(root.get("error"));
      }
      JsonNode result = root.get("result");
      if (checkBusinessResult && result.isObject() && result.path("success").isBoolean()
          && !result.get("success").booleanValue()) {
        JsonNode message = result.path("errorMessage");
        return new IpcResponse(message.isTextual() && !message.textValue().trim().isEmpty()
            ? message.textValue() : "Admin operation failed.", false);
      }
      if (outputFormat == OutputFormat.TEXT) {
        String table = new ActivePeerOutputFormatter().formatText(result);
        if (table != null) {
          return new IpcResponse(table, true);
        }
      }
      // only output column "result" of jsonrpc and ignore other columns
      return new IpcResponse(formatJsonValue(result), true);
    } catch (JsonProcessingException e) {
      return invalidResponse();
    }
  }

  /** Formats a single-command response, treating a business rejection as a command failure. */
  static IpcResponse parseForExec(String response, int expectedId,
      Map<Integer, OutputFormat> pendingOutputFormats) {
    return parse(response, expectedId, pendingOutputFormats, true);
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
