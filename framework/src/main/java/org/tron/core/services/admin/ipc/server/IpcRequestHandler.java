package org.tron.core.services.admin.ipc.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
 * Per-request buffers are local so one handler can serve concurrent client connections.
 * Stream and socket ownership remains with {@link IpcService}.
 */
@Slf4j(topic = "API")
final class IpcRequestHandler {

  private static final ObjectMapper OBJECT_MAPPER = JsonRpcMapper.create();

  private final JsonRpcServer jsonRpcServer;
  @Getter(AccessLevel.PACKAGE)
  private final int maxRequestSize;

  IpcRequestHandler(AdminJsonRpc adminJsonRpc, int maxRequestSize) {
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
      return buildInternalErrorResponse(jsonRequest);
    }
  }

  private String buildInternalErrorResponse(String jsonRequest) {
    JsonNode requestId = NullNode.getInstance();
    try {
      JsonNode request = OBJECT_MAPPER.readTree(jsonRequest);
      if (request != null && request.has("id")) {
        requestId = request.get("id");
      }
    } catch (IOException e) {
      logger.debug("Unable to read request id from invalid IPC request");
    }

    ObjectNode error = OBJECT_MAPPER.createObjectNode();
    error.put("code", -32603);
    error.put("message", "Internal error");
    ObjectNode response = OBJECT_MAPPER.createObjectNode();
    response.put("jsonrpc", "2.0");
    response.set("error", error);
    response.set("id", requestId);
    try {
      return OBJECT_MAPPER.writeValueAsString(response);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to serialize IPC error response", e);
    }
  }

  static final class RequestTooLargeException extends IOException {

    private static final long serialVersionUID = 1L;
  }
}
