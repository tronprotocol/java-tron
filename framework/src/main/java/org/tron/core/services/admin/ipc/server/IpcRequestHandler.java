package org.tron.core.services.admin.ipc.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.ErrorResolver.JsonError;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.services.admin.AdminJsonRpc;
import org.tron.core.services.jsonrpc.JsonRpcErrorResolver;
import org.tron.core.services.jsonrpc.JsonRpcMapper;

/**
 * Reads bounded IPC request lines and dispatches Admin JSON-RPC calls.
 * Only individual request objects are supported; batch requests are rejected.
 * Per-request buffers are local so one handler can serve concurrent client connections.
 * Stream and socket ownership remains with {@link IpcService}.
 */
@Slf4j(topic = "API")
final class IpcRequestHandler {

  private static final ObjectMapper OBJECT_MAPPER = JsonRpcMapper.create();

  private final JsonRpcServer jsonRpcServer;
  @Getter(AccessLevel.PACKAGE)
  private final long maxRequestSize;

  IpcRequestHandler(AdminJsonRpc adminJsonRpc, long maxRequestSize) {
    this.maxRequestSize = maxRequestSize;
    jsonRpcServer = new JsonRpcServer(OBJECT_MAPPER, adminJsonRpc, AdminJsonRpc.class);
    jsonRpcServer.setErrorResolver(JsonRpcErrorResolver.INSTANCE);
    jsonRpcServer.setShouldLogInvocationErrors(false);
  }

  String readRequest(InputStream input) throws IOException {
    ByteArrayOutputStream request = new ByteArrayOutputStream();
    int value;
    while ((value = input.read()) != -1) {
      if (value == '\n') {
        break;
      }
      if (request.size() >= maxRequestSize) {
        throw new RequestTooLargeException();
      }
      request.write(value);
    }
    if (value == -1 && request.size() == 0) {
      return null;
    }
    byte[] bytes = request.toByteArray();
    int length = bytes.length;
    if (length > 0 && bytes[length - 1] == '\r') {
      length--;
    }
    return new String(bytes, 0, length, StandardCharsets.UTF_8);
  }

  String handleCommand(String jsonRequest) {
    JsonNode request;
    try {
      request = OBJECT_MAPPER.reader()
          .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(jsonRequest);
      if (request == null || request.isMissingNode()) {
        return buildErrorResponse(JsonError.PARSE_ERROR.code, JsonError.PARSE_ERROR.message, null);
      }
    } catch (JsonProcessingException e) {
      return buildErrorResponse(JsonError.PARSE_ERROR.code, JsonError.PARSE_ERROR.message, null);
    }
    if (request.isArray()) {
      return buildErrorResponse(
          JsonError.INVALID_REQUEST.code, AdminJsonRpc.BATCH_NOT_SUPPORTED_MESSAGE, null);
    }
    ByteArrayInputStream input =
        new ByteArrayInputStream(jsonRequest.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    try {
      jsonRpcServer.handleRequest(input, output);
      if (output.size() == 0) {
        return "";
      }
      JsonNode response = OBJECT_MAPPER.readTree(output.toByteArray());
      return response == null ? "" : OBJECT_MAPPER.writeValueAsString(response);
    } catch (Exception e) {
      logger.debug("Failed to dispatch IPC request");
      return buildErrorResponse(-32603, "Internal error", request.get("id"));
    }
  }

  private String buildErrorResponse(int code, String message, JsonNode requestId) {
    try {
      return OBJECT_MAPPER.writeValueAsString(
          JsonRpcMapper.createErrorResponse(code, message, requestId));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to serialize IPC error response", e);
    }
  }

  static final class RequestTooLargeException extends IOException {

    private static final long serialVersionUID = 1L;
  }
}
