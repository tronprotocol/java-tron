package org.tron.core.services.admin;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Utf8;
import com.google.common.primitives.Bytes;
import com.googlecode.jsonrpc4j.ErrorResolver.JsonError;
import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.googlecode.jsonrpc4j.JsonRpcInterceptor;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.services.jsonrpc.JsonRpcErrorResolver;
import org.tron.core.services.jsonrpc.JsonRpcMapper;
import org.tron.core.services.jsonrpc.JsonRpcRequestValidator;

/**
 * Validates and dispatches individual Admin JSON-RPC requests for both HTTP and IPC.
 * Accepted IDs retain their JSON value. Only requests without an ID member are notifications,
 * including when invocation fails.
 */
@Slf4j(topic = "API")
public final class AdminJsonRpcRequestHandler extends JsonRpcBasicServer {

  private static final ObjectMapper OBJECT_MAPPER = JsonRpcMapper.create();

  public AdminJsonRpcRequestHandler(AdminJsonRpc adminJsonRpc) {
    super(OBJECT_MAPPER, adminJsonRpc, AdminJsonRpc.class);
    setErrorResolver(JsonRpcErrorResolver.INSTANCE);
    setShouldLogInvocationErrors(false);
  }

  /** Parses the complete input before dispatch, returning an empty body for valid notifications. */
  public byte[] handleRequest(byte[] body) throws IOException {
    // Jackson accepts overlong and surrogate UTF-8 sequences and treats NUL bytes as a sign of
    // UTF-16 or UTF-32, so check the bytes first.
    if (!Utf8.isWellFormed(body) || Bytes.contains(body, (byte) 0)) {
      return OBJECT_MAPPER.writeValueAsBytes(errorResponse(JsonError.PARSE_ERROR, null));
    }
    JsonNode request;
    try {
      request = OBJECT_MAPPER.reader()
          .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(body);
    } catch (IOException e) {
      return OBJECT_MAPPER.writeValueAsBytes(errorResponse(JsonError.PARSE_ERROR, null));
    }

    JsonNode response = check(request);
    if (response == null) {
      response = dispatch((ObjectNode) request, request.get("id"));
    }
    return response == null ? new byte[0] : OBJECT_MAPPER.writeValueAsBytes(response);
  }

  /** Returns an error response for invalid input, or null when the request can be dispatched. */
  private JsonNode check(JsonNode request) {
    if (request == null || request.isMissingNode()) {
      return errorResponse(JsonError.PARSE_ERROR, null);
    }
    if (request.isArray()) {
      return JsonRpcMapper.createErrorResponse(
          JsonError.INVALID_REQUEST.code, AdminJsonRpc.BATCH_NOT_SUPPORTED_MESSAGE, null);
    }
    String invalidRequest = JsonRpcRequestValidator.invalidRequestMessage(request);
    if (invalidRequest != null) {
      JsonNode id = request.get("id");
      return JsonRpcMapper.createErrorResponse(JsonError.INVALID_REQUEST.code, invalidRequest,
          JsonRpcRequestValidator.isValidResponseId(id) ? id : null);
    }
    return null;
  }

  private JsonNode dispatch(ObjectNode request, JsonNode id) {
    boolean notification = !request.has("id");
    try {
      for (JsonRpcInterceptor interceptor : getInterceptorList()) {
        interceptor.preHandleJson(request);
      }
      JsonNode response;
      try {
        response = super.handleJsonNodeRequest(request).getResponse();
      } catch (MismatchedInputException e) {
        // jsonrpc4j propagates input conversion failures before invoking the method.
        response = errorResponse(JsonError.METHOD_PARAMS_INVALID, id);
      }
      if (notification) {
        return null;
      }
      ((ObjectNode) response).set("id", id);
      return response;
    } catch (Exception e) {
      logger.debug("Failed to dispatch Admin request");
      return notification ? null : errorResponse(JsonRpcErrorResolver.INTERNAL_ERROR, id);
    }
  }

  private JsonNode errorResponse(JsonError error, JsonNode id) {
    return JsonRpcMapper.createErrorResponse(error.code, error.message, id);
  }
}
