package org.tron.core.services.admin.ipc.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.AccessLevel;
import lombok.Getter;
import org.tron.core.services.admin.AdminJsonRpc;
import org.tron.core.services.admin.AdminJsonRpcRequestHandler;

/**
 * Reads bounded IPC request lines and dispatches Admin JSON-RPC calls.
 * Only individual request objects are supported; batch requests are rejected.
 * Per-request buffers are local so one handler can serve concurrent client connections.
 * Stream and socket ownership remains with {@link IpcService}.
 */
final class IpcRequestHandler {

  private final AdminJsonRpcRequestHandler requestHandler;
  @Getter(AccessLevel.PACKAGE)
  private final long maxRequestSize;

  IpcRequestHandler(AdminJsonRpc adminJsonRpc, long maxRequestSize) {
    this.maxRequestSize = maxRequestSize;
    requestHandler = new AdminJsonRpcRequestHandler(adminJsonRpc);
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

  String handleCommand(String jsonRequest) throws IOException {
    return new String(requestHandler.handleRequest(jsonRequest.getBytes(StandardCharsets.UTF_8)),
        StandardCharsets.UTF_8);
  }

  static final class RequestTooLargeException extends IOException {

    private static final long serialVersionUID = 1L;
  }
}
