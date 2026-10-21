package org.tron.core.services.admin.ipc.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.Getter;
import org.tron.core.services.admin.AdminJsonRpc;
import org.tron.core.services.admin.AdminJsonRpcRequestHandler;

/**
 * Reads bounded IPC request lines and dispatches Admin JSON-RPC calls.
 * The byte limit excludes LF or CRLF terminators; other CR bytes count toward the limit.
 * Request bytes reach the JSON parser unchanged, so IPC and HTTP parse the same input.
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

  byte[] readRequest(InputStream input) throws IOException {
    ByteArrayOutputStream request = new ByteArrayOutputStream();
    int value;
    while ((value = input.read()) != -1) {
      if (value == '\n') {
        break;
      }
      // Allow one extra CR until the next byte determines whether it belongs to CRLF.
      if (request.size() > maxRequestSize
          || (request.size() == maxRequestSize && value != '\r')) {
        throw new RequestTooLargeException();
      }
      request.write(value);
    }
    if (value == -1 && request.size() == 0) {
      return null;
    }
    byte[] bytes = request.toByteArray();
    int length = bytes.length;
    if (value == '\n' && length > 0 && bytes[length - 1] == '\r') {
      length--;
    }
    if (length > maxRequestSize) {
      throw new RequestTooLargeException();
    }
    return length == bytes.length ? bytes : Arrays.copyOf(bytes, length);
  }

  String handleCommand(byte[] jsonRequest) throws IOException {
    return new String(requestHandler.handleRequest(jsonRequest), StandardCharsets.UTF_8);
  }

  static final class RequestTooLargeException extends IOException {

    private static final long serialVersionUID = 1L;
  }
}
