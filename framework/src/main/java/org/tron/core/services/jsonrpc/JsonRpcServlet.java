package org.tron.core.services.jsonrpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.googlecode.jsonrpc4j.ErrorResolver.JsonError;
import com.googlecode.jsonrpc4j.JsonRpcInterceptor;
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.ProxyUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.services.filter.BufferedResponseWrapper;
import org.tron.core.services.http.servlets.RateLimiterServlet;

@Component
@Slf4j(topic = "API")
public class JsonRpcServlet extends RateLimiterServlet {

  private static final ObjectMapper MAPPER = JsonRpcMapper.create();
  private static final ObjectMapper OUTBOUND_MAPPER = new ObjectMapper();

  private enum JsonRpcError {
    PARSE_ERROR(-32700),
    INVALID_REQUEST(-32600),
    INTERNAL_ERROR(-32603),
    EXCEED_LIMIT(-32005),
    RESPONSE_TOO_LARGE(-32003);

    private final int code;

    JsonRpcError(int code) {
      this.code = code;
    }
  }

  private JsonRpcServer rpcServer = null;
  private Set<String> methodNames = Collections.emptySet();

  @Autowired
  private TronJsonRpc tronJsonRpc;

  @Autowired
  private JsonRpcInterceptor interceptor;

  @VisibleForTesting
  void setRpcServer(JsonRpcServer rpcServer, Class<?> serviceInterface) {
    this.rpcServer = rpcServer;
    this.methodNames = collectMethodNames(serviceInterface);
  }

  private static Set<String> collectMethodNames(Class<?> serviceInterface) {
    Set<String> names = new HashSet<>();
    for (Method method : serviceInterface.getMethods()) {
      JsonRpcMethod annotation = method.getAnnotation(JsonRpcMethod.class);
      if (annotation == null || !annotation.required()) {
        names.add(method.getName());
      }
      if (annotation != null) {
        names.add(annotation.value());
      }
    }
    return Collections.unmodifiableSet(names);
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    Object compositeService = ProxyUtil.createCompositeServiceProxy(
        cl,
        new Object[] {tronJsonRpc},
        new Class[] {TronJsonRpc.class},
        true);

    JsonRpcServer server = new JsonRpcServer(compositeService);
    server.setErrorResolver(JsonRpcErrorResolver.INSTANCE);

    server.setShouldLogInvocationErrors(false);
    if (CommonParameter.getInstance().isMetricsPrometheusEnable()) {
      server.setInterceptorList(Collections.singletonList(interceptor));
    }
    setRpcServer(server, TronJsonRpc.class);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    try {
      doPostInternal(req, resp);
    } catch (Error fatal) {
      JsonRpcHttpResponse.commitBareInternalServerError(resp);
      throw fatal;
    }
  }

  private void doPostInternal(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    CommonParameter parameter = CommonParameter.getInstance();

    // Transport IOException from readBody propagates as HTTP 500 (genuine IO failure).
    byte[] body = readBody(req.getInputStream());
    JsonNode rootNode;
    try {
      rootNode = MAPPER.readTree(body);
      if (rootNode == null || rootNode.isMissingNode()) {
        writeJsonRpcError(resp, JsonRpcError.PARSE_ERROR, "JSON parse error", null, false);
        return;
      }
    } catch (JsonProcessingException e) {
      if (e instanceof StreamConstraintsException) {
        writeJsonRpcError(resp, JsonRpcError.PARSE_ERROR, e.getMessage(), null, false);
      } else {
        writeJsonRpcError(resp, JsonRpcError.PARSE_ERROR, "JSON parse error", null, false);
      }
      return;
    }

    if (!rootNode.isObject() && !rootNode.isArray()) {
      writeJsonRpcError(resp, JsonRpcError.INVALID_REQUEST,
          JsonRpcRequestValidator.invalidRequestMessage(rootNode), null, false);
      return;
    }

    boolean isBatch = rootNode.isArray();
    if (isBatch && rootNode.isEmpty()) {
      writeJsonRpcError(resp, JsonRpcError.INVALID_REQUEST,
          JsonRpcRequestValidator.invalidRequestMessage(rootNode), null, false);
      return;
    }
    if (!isBatch) {
      String invalidRequest = JsonRpcRequestValidator.invalidRequestMessage(rootNode);
      if (invalidRequest != null) {
        writeJsonRpcError(resp, JsonRpcError.INVALID_REQUEST, invalidRequest,
            rootNode.get("id"), false);
        return;
      }
    }
    int batchSize = parameter.getJsonRpcMaxBatchSize();
    if (isBatch && batchSize > 0 && rootNode.size() > batchSize) {
      writeJsonRpcError(resp, JsonRpcError.EXCEED_LIMIT,
          "Batch size " + rootNode.size() + " exceeds the limit of " + batchSize, null, true);
      return;
    }

    int maxResponseSize = parameter.getJsonRpcMaxResponseSize();
    if (isBatch) {
      handleBatch(resp, rootNode, maxResponseSize);
    } else {
      handleSingle(resp, rootNode, body, maxResponseSize);
    }
  }

  private void handleSingle(HttpServletResponse resp, JsonNode rootNode, byte[] body,
      int maxResponseSize) throws IOException {
    BufferedResponseWrapper bufferedResp = new BufferedResponseWrapper(
        resp, maxResponseSize);

    try {
      // JsonRpcServer.handle catches Throwable and would swallow fatal errors rethrown by the
      // resolver. Use the lower-level entry point so single and batch requests share a boundary.
      dispatch(rootNode, body, bufferedResp.getOutputStream());
    } catch (RuntimeException | IOException | Error e) {
      rethrowIfFatal(e);
      logger.error("RPC execution failed", e);
      if (!rootNode.has("id")) {
        resp.setContentType("application/json-rpc");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentLength(0);
        return;
      }
      writeJsonRpcError(resp, JsonRpcError.INTERNAL_ERROR, "Internal error",
          rootNode.get("id"), false);
      return;
    }

    bufferedResp.setContentType("application/json-rpc");
    bufferedResp.setStatus(HttpServletResponse.SC_OK);
    bufferedResp.commitToResponse();
    if (bufferedResp.isOverflow()) {
      writeJsonRpcError(resp, JsonRpcError.RESPONSE_TOO_LARGE,
          "Response exceeds the limit of " + maxResponseSize + " bytes",
          rootNode.get("id"), false);
    }
  }

  private void handleBatch(HttpServletResponse resp, JsonNode rootNode, int maxResponseSize)
      throws IOException {

    ArrayNode batchResult = MAPPER.createArrayNode();
    int accumulatedSize = 2; // "[]"
    boolean overflow = false;
    BatchFailureLog failureLog = new BatchFailureLog();

    for (int i = 0; i < rootNode.size(); i++) {
      JsonNode subRequest = rootNode.get(i);

      String invalidRequest = JsonRpcRequestValidator.invalidRequestMessage(subRequest);
      if (invalidRequest != null) {
        ObjectNode errNode = buildErrorNode(JsonRpcError.INVALID_REQUEST, invalidRequest,
            subRequest.get("id"));
        if (!overflow) {
          byte[] errBytes = MAPPER.writeValueAsBytes(errNode);
          int addition = errBytes.length + (!batchResult.isEmpty() ? 1 : 0);
          if (maxResponseSize > 0 && accumulatedSize + addition > maxResponseSize) {
            overflow = true;
          } else {
            accumulatedSize += addition;
          }
        }
        batchResult.add(errNode);
        continue;
      }

      if (overflow) {
        if (subRequest.has("id")) {
          // Notifications (no "id") do not get a response even on overflow.
          batchResult.add(buildErrorNode(JsonRpcError.RESPONSE_TOO_LARGE,
              "Response exceeds the limit of " + maxResponseSize + " bytes",
              subRequest.get("id")));
        }
        continue;
      }

      byte[] responseBytes = executeBatchRequest(subRequest, i, failureLog);
      if (responseBytes.length == 0) {
        continue; // notification — no response
      }

      // comma(,) separator between array elements
      int addition = responseBytes.length + (!batchResult.isEmpty() ? 1 : 0);
      // Reject bytes beyond the remaining budget before parsing, even if they are malformed.
      if (maxResponseSize > 0 && accumulatedSize + addition > maxResponseSize) {
        overflow = true;
        batchResult.add(buildErrorNode(JsonRpcError.RESPONSE_TOO_LARGE,
            "Response exceeds the limit of " + maxResponseSize + " bytes",
            subRequest.get("id")));
        continue;
      }
      JsonNode responseNode;
      try {
        responseNode = OUTBOUND_MAPPER.readTree(responseBytes);
      } catch (IOException e) {
        logger.debug("RPC response parsing failed for batch sub-request {}", i, e);
        responseBytes = internalErrorResponse(subRequest.get("id"));
        responseNode = MAPPER.readTree(responseBytes);
        // Charge only the replacement, not the discarded malformed response bytes.
        addition = responseBytes.length + (!batchResult.isEmpty() ? 1 : 0);
        // A replacement error can be larger than the malformed response it replaces.
        if (maxResponseSize > 0 && accumulatedSize + addition > maxResponseSize) {
          overflow = true;
          batchResult.add(buildErrorNode(JsonRpcError.RESPONSE_TOO_LARGE,
              "Response exceeds the limit of " + maxResponseSize + " bytes",
              subRequest.get("id")));
          continue;
        }
      }
      accumulatedSize += addition;
      batchResult.add(responseNode);
    }

    // JSON-RPC 2.0 §6: MUST NOT return an empty Array when there are no response objects.
    if (batchResult.isEmpty()) {
      resp.setContentType("application/json-rpc");
      resp.setStatus(HttpServletResponse.SC_OK);
      resp.setContentLength(0);
      return;
    }

    byte[] finalBytes = MAPPER.writeValueAsBytes(batchResult);
    resp.setContentType("application/json-rpc");
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentLength(finalBytes.length);
    resp.getOutputStream().write(finalBytes);
    resp.getOutputStream().flush();
  }

  private byte[] executeBatchRequest(JsonNode subRequest, int index, BatchFailureLog failureLog)
      throws IOException {
    byte[] subBody;
    try {
      subBody = MAPPER.writeValueAsBytes(subRequest);
    } catch (JsonProcessingException e) {
      logger.debug("RPC request serialization failed for batch sub-request {}", index, e);
      return internalErrorResponse(subRequest.get("id"));
    }

    ByteArrayOutputStream subOutput = new ByteArrayOutputStream();
    try {
      dispatch(subRequest, subBody, subOutput);
    } catch (RuntimeException | IOException | Error e) {
      rethrowIfFatal(e);
      failureLog.record(index, e);
      return internalErrorResponse(subRequest.get("id"));
    }
    return subOutput.toByteArray();
  }

  private void dispatch(JsonNode request, byte[] body, OutputStream output) throws IOException {
    if (methodNames.contains(request.get("method").asText())) {
      rpcServer.handleRequest(new ByteArrayInputStream(body), output);
      return;
    }
    ObjectNode error = JsonRpcMapper.createErrorResponse(JsonError.METHOD_NOT_FOUND.code,
        JsonError.METHOD_NOT_FOUND.message, request.get("id"));
    output.write(MAPPER.writeValueAsBytes(error));
    output.write('\n');
  }

  private static class BatchFailureLog {
    private boolean logged;

    private void record(int index, Throwable failure) {
      if (!logged) {
        logged = true;
        logger.error("RPC execution failed for batch sub-request {}", index, failure);
      } else {
        logger.debug("RPC execution failed for batch sub-request {} ({})",
            index, failure.getClass().getName());
      }
    }
  }

  private byte[] internalErrorResponse(JsonNode id) throws JsonProcessingException {
    return MAPPER.writeValueAsBytes(
        buildErrorNode(JsonRpcError.INTERNAL_ERROR, "Internal error", id));
  }

  private static void rethrowIfFatal(Throwable exception) {
    Error fatal = JsonRpcErrorResolver.findFatalCause(exception);
    if (fatal != null) {
      throw fatal;
    }
  }

  private byte[] readBody(InputStream in) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] tmp = new byte[4096];
    int n;
    while ((n = in.read(tmp)) != -1) {
      buffer.write(tmp, 0, n);
    }
    return buffer.toByteArray();
  }

  private ObjectNode buildErrorNode(JsonRpcError error, String message, JsonNode id) {
    ObjectNode errorObj = MAPPER.createObjectNode();
    errorObj.put("jsonrpc", "2.0");
    ObjectNode errNode = errorObj.putObject("error");
    errNode.put("code", error.code);
    errNode.put("message", message);
    if (JsonRpcRequestValidator.isValidResponseId(id)) {
      errorObj.set("id", id);
    } else {
      errorObj.putNull("id");
    }
    return errorObj;
  }

  private void writeJsonRpcError(HttpServletResponse resp, JsonRpcError error, String message,
      JsonNode id, boolean isBatch) throws IOException {
    ObjectNode errorObj = buildErrorNode(error, message, id);
    byte[] bytes;
    if (isBatch) {
      ArrayNode arr = MAPPER.createArrayNode();
      arr.add(errorObj);
      bytes = MAPPER.writeValueAsBytes(arr);
    } else {
      bytes = MAPPER.writeValueAsBytes(errorObj);
    }
    resp.setContentType("application/json-rpc");
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentLength(bytes.length);
    resp.getOutputStream().write(bytes);
    resp.getOutputStream().flush();
  }
}
