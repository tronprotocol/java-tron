package org.tron.core.services.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.googlecode.jsonrpc4j.ErrorResolver.JsonError;
import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.googlecode.jsonrpc4j.JsonRpcInterceptor;
import java.io.IOException;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.services.jsonrpc.JsonRpcErrorResolver;
import org.tron.core.services.jsonrpc.JsonRpcMapper;

/**
 * Validates and dispatches individual Admin JSON-RPC requests for both HTTP and IPC.
 * Numeric IDs must be within the signed 64-bit range; accepted IDs retain their JSON value.
 * Only requests without an ID member are notifications, including when invocation fails.
 */
@Slf4j(topic = "API")
public final class AdminJsonRpcRequestHandler extends JsonRpcBasicServer {

  private static final ObjectMapper OBJECT_MAPPER = JsonRpcMapper.create();
  private static final BigDecimal MIN_ID = BigDecimal.valueOf(Long.MIN_VALUE);
  private static final BigDecimal MAX_ID = BigDecimal.valueOf(Long.MAX_VALUE);

  public AdminJsonRpcRequestHandler(AdminJsonRpc adminJsonRpc) {
    super(OBJECT_MAPPER, adminJsonRpc, AdminJsonRpc.class);
    setErrorResolver(JsonRpcErrorResolver.INSTANCE);
    setShouldLogInvocationErrors(false);
  }

  /** Parses the complete input before dispatch, returning an empty body for valid notifications. */
  public byte[] handleRequest(byte[] body) throws IOException {
    JsonNode request;
    try {
      request = OBJECT_MAPPER.reader()
          .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(body);
    } catch (JsonProcessingException e) {
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
    JsonNode id = request.get("id");
    if (!isValidId(id)) {
      return errorResponse(JsonError.INVALID_REQUEST, null);
    }
    if (!isValidEnvelope(request)) {
      return errorResponse(JsonError.INVALID_REQUEST, id);
    }
    return null;
  }

  private boolean isValidEnvelope(JsonNode request) {
    JsonNode version = request.path("jsonrpc");
    JsonNode params = request.get("params");
    return request.isObject() && version.isTextual() && "2.0".equals(version.textValue())
        && request.path("method").isTextual()
        && (params == null || params.isArray() || params.isObject());
  }

  private boolean isValidId(JsonNode id) {
    if (id == null || id.isNull() || id.isTextual()) {
      return true;
    }
    return id.isNumber() && id.decimalValue().compareTo(MIN_ID) >= 0
        && id.decimalValue().compareTo(MAX_ID) <= 0;
  }

  private JsonNode dispatch(ObjectNode request, JsonNode id) {
    boolean notification = !request.has("id");
    try {
      for (JsonRpcInterceptor interceptor : getInterceptorList()) {
        interceptor.preHandleJson(request);
      }
      // jsonrpc4j treats null IDs as notifications. Use a request-local ID during invocation,
      // then restore the original value directly on the response without parsing it again.
      if (id != null && id.isNull()) {
        request.put("id", 0);
      }
      JsonNode response = super.handleJsonNodeRequest(request).getResponse();
      if (notification) {
        return null;
      }
      ((ObjectNode) response).set("id", id);
      return response;
    } catch (Exception e) {
      logger.debug("Failed to dispatch Admin request");
      return notification ? null : JsonRpcMapper.createErrorResponse(-32603, "Internal error", id);
    } finally {
      if (id != null && id.isNull()) {
        request.set("id", id);
      }
    }
  }

  private JsonNode errorResponse(JsonError error, JsonNode id) {
    return JsonRpcMapper.createErrorResponse(error.code, error.message, id);
  }
}
