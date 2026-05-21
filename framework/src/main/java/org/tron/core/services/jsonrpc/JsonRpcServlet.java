package org.tron.core.services.jsonrpc;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.googlecode.jsonrpc4j.HttpStatusCodeProvider;
import com.googlecode.jsonrpc4j.JsonRpcInterceptor;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.ProxyUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.services.filter.BufferedResponseWrapper;
import org.tron.core.services.filter.CachedBodyRequestWrapper;
import org.tron.core.services.http.RateLimiterServlet;

@Component
@Slf4j(topic = "API")
public class JsonRpcServlet extends RateLimiterServlet {

  // Snapshot of node.http.maxNestingDepth / maxTokenCount at class-load time (after Args.setParam).
  private static final ObjectMapper MAPPER = buildMapper();

  private static ObjectMapper buildMapper() {
    CommonParameter p = CommonParameter.getInstance();
    JsonFactory factory = JsonFactory.builder()
        .streamReadConstraints(StreamReadConstraints.builder()
            .maxNestingDepth(p.getMaxNestingDepth())
            .maxTokenCount(p.getMaxTokenCount())
            .build())
        .build();
    return new ObjectMapper(factory);
  }

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

  @Autowired
  private TronJsonRpc tronJsonRpc;

  @Autowired
  private JsonRpcInterceptor interceptor;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    Object compositeService = ProxyUtil.createCompositeServiceProxy(
        cl,
        new Object[] {tronJsonRpc},
        new Class[] {TronJsonRpc.class},
        true);

    rpcServer = new JsonRpcServer(compositeService);
    rpcServer.setErrorResolver(JsonRpcErrorResolver.INSTANCE);

    HttpStatusCodeProvider httpStatusCodeProvider = new HttpStatusCodeProvider() {
      @Override
      public int getHttpStatusCode(int resultCode) {
        return 200;
      }

      @Override
      public Integer getJsonRpcCode(int httpStatusCode) {
        return null;
      }
    };
    rpcServer.setHttpStatusCodeProvider(httpStatusCodeProvider);

    rpcServer.setShouldLogInvocationErrors(false);
    if (CommonParameter.getInstance().isMetricsPrometheusEnable()) {
      rpcServer.setInterceptorList(Collections.singletonList(interceptor));
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
      writeJsonRpcError(resp, JsonRpcError.PARSE_ERROR, "JSON parse error", null, false);
      return;
    }

    if (!rootNode.isObject() && !rootNode.isArray()) {
      writeJsonRpcError(resp, JsonRpcError.INVALID_REQUEST, "Invalid Request", null, false);
      return;
    }

    boolean isBatch = rootNode.isArray();
    if (isBatch && rootNode.isEmpty()) {
      writeJsonRpcError(resp, JsonRpcError.INVALID_REQUEST, "Invalid Request", null, false);
      return;
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
      handleSingle(req, resp, rootNode, body, maxResponseSize);
    }
  }

  private void handleSingle(HttpServletRequest req, HttpServletResponse resp,
      JsonNode rootNode, byte[] body, int maxResponseSize) throws IOException {
    CachedBodyRequestWrapper cachedReq = new CachedBodyRequestWrapper(req, body);
    BufferedResponseWrapper bufferedResp = new BufferedResponseWrapper(
        resp, maxResponseSize);

    try {
      rpcServer.handle(cachedReq, bufferedResp);
    } catch (RuntimeException e) {
      logger.error("RPC execution failed", e);
      writeJsonRpcError(resp, JsonRpcError.INTERNAL_ERROR, "Internal error",
          rootNode.get("id"), false);
      return;
    }

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

    for (int i = 0; i < rootNode.size(); i++) {
      JsonNode subRequest = rootNode.get(i);

      if (overflow) {
        if (!subRequest.isObject()) {
          batchResult.add(buildErrorNode(JsonRpcError.INVALID_REQUEST, "Invalid Request", null));
        } else if (subRequest.has("id")) {
          // Notifications (no "id") do not get a response even on overflow.
          batchResult.add(buildErrorNode(JsonRpcError.RESPONSE_TOO_LARGE,
              "Response exceeds the limit of " + maxResponseSize + " bytes",
              subRequest.get("id")));
        }
        continue;
      }

      if (!subRequest.isObject()) {
        ObjectNode errNode = buildErrorNode(JsonRpcError.INVALID_REQUEST, "Invalid Request", null);
        byte[] errBytes = MAPPER.writeValueAsBytes(errNode);
        int addition = errBytes.length + (!batchResult.isEmpty() ? 1 : 0);
        if (maxResponseSize > 0 && accumulatedSize + addition > maxResponseSize) {
          overflow = true;
        } else {
          accumulatedSize += addition;
        }
        batchResult.add(errNode);
        continue;
      }

      byte[] subBody;
      try {
        subBody = MAPPER.writeValueAsBytes(subRequest);
      } catch (JsonProcessingException e) {
        writeJsonRpcError(resp, JsonRpcError.INTERNAL_ERROR, "Internal error", null, true);
        return;
      }

      ByteArrayOutputStream subOutput = new ByteArrayOutputStream();
      try {
        rpcServer.handleRequest(new ByteArrayInputStream(subBody), subOutput);
      } catch (RuntimeException e) {
        logger.error("RPC execution failed for batch sub-request {}", i, e);
        writeJsonRpcError(resp, JsonRpcError.INTERNAL_ERROR, "Internal error", null, true);
        return;
      }

      byte[] responseBytes = subOutput.toByteArray();
      if (responseBytes.length == 0) {
        continue; // notification — no response
      }

      // comma(,) separator between array elements
      int addition = responseBytes.length + (!batchResult.isEmpty() ? 1 : 0);
      if (maxResponseSize > 0 && accumulatedSize + addition > maxResponseSize) {
        overflow = true;
        batchResult.add(buildErrorNode(JsonRpcError.RESPONSE_TOO_LARGE,
            "Response exceeds the limit of " + maxResponseSize + " bytes",
            subRequest.get("id")));
        continue;
      }
      accumulatedSize += addition;

      JsonNode responseNode;
      try {
        responseNode = MAPPER.readTree(responseBytes);
      } catch (IOException e) {
        writeJsonRpcError(resp, JsonRpcError.INTERNAL_ERROR, "Internal error", null, true);
        return;
      }
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
    if (id != null && !id.isNull() && !id.isMissingNode()) {
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
