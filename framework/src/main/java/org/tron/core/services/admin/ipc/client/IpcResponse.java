package org.tron.core.services.admin.ipc.client;

import com.fasterxml.jackson.core.JsonProcessingException;
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
   * Parses a JSON-RPC response into console text and a success flag.
   *
   * <p>A non-null error takes precedence over any result; otherwise, a result field, including
   * a null value, marks the response as successful. Text results are unquoted and other values
   * are formatted as JSON. Responses without a result are unsuccessful; empty or malformed
   * input is preserved verbatim.
   */
  static IpcResponse parse(String response) {
    try {
      JsonNode root = OBJECT_MAPPER.readTree(response);
      if (root == null || root.isMissingNode()) {
        return new IpcResponse(response, false);
      }
      JsonNode error = root.get("error");
      if (error != null && !error.isNull()) {
        String code = error.has("code") ? " " + error.get("code").asText() : "";
        String message = error.has("message") ? error.get("message").asText() : "Unknown error";
        return new IpcResponse("Error" + code + ": " + message, false);
      }
      if (root.has("result")) {
        return new IpcResponse(formatJsonValue(root.get("result")), true);
      }
      return new IpcResponse(formatJsonValue(root), false);
    } catch (JsonProcessingException e) {
      return new IpcResponse(response, false);
    }
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
