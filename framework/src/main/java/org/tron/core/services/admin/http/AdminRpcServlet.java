package org.tron.core.services.admin.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.googlecode.jsonrpc4j.ErrorResolver.JsonError;
import com.googlecode.jsonrpc4j.JsonRpcInterceptor;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.ProxyUtil;
import io.prometheus.client.Histogram;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.Metrics;
import org.tron.core.services.admin.AdminJsonRpc;
import org.tron.core.services.jsonrpc.JsonRpcErrorResolver;
import org.tron.core.services.jsonrpc.JsonRpcMapper;
import org.tron.core.services.jsonrpc.JsonRpcMediaType;

/**
 * Serves the {@link AdminJsonRpc} API at {@code POST /admin} through jsonrpc4j.
 *
 * <p>This endpoint is intended for trusted node operators. Deployments must restrict access to
 * loopback or a controlled management network. This low-frequency management endpoint intentionally
 * bypasses public API rate limiting and JSON-RPC response-size limits.
 * Only individual request objects are supported; batch requests are rejected.
 * HTTP request-size limits are enforced by
 * {@link org.tron.common.application.HttpService}; JSON parser limits come from
 * {@link JsonRpcMapper}.
 */
@Component
@Slf4j(topic = "API")
public class AdminRpcServlet extends HttpServlet {

  private static final long serialVersionUID = 0L;
  private static final ObjectMapper OBJECT_MAPPER = JsonRpcMapper.create();

  private JsonRpcServer rpcServer = null;
  private VirtualHostValidator virtualHostValidator =
      new VirtualHostValidator(Collections.emptyList());

  @Autowired
  private AdminJsonRpc adminJsonRpc;

  @Autowired
  private JsonRpcInterceptor interceptor;

  /**
   * Initializes the HTTP dispatcher from the Admin API and snapshots the virtual-host policy.
   */
  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    // Expose the annotated Admin interface through the same proxy mechanism as public JSON-RPC.
    Object compositeService = ProxyUtil.createCompositeServiceProxy(cl,
        new Object[] {adminJsonRpc},
        new Class[] {AdminJsonRpc.class},
        true);

    // Keep parser constraints and annotation-based error mapping consistent with the IPC transport.
    rpcServer = new JsonRpcServer(OBJECT_MAPPER, compositeService);
    rpcServer.setErrorResolver(JsonRpcErrorResolver.INSTANCE);

    rpcServer.setShouldLogInvocationErrors(false);
    if (CommonParameter.getInstance().isMetricsPrometheusEnable()) {
      rpcServer.setInterceptorList(Collections.singletonList(interceptor));
    }
    virtualHostValidator = new VirtualHostValidator(
        CommonParameter.getInstance().getAdminHttpVirtualHosts());
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.setContentType("application/json; charset=utf-8");
    Histogram.Timer requestTimer = Metrics.histogramStartTimer(
        MetricKeys.Histogram.HTTP_SERVICE_LATENCY, req.getContextPath() + req.getServletPath());
    try {
      super.service(req, resp);
    } finally {
      Metrics.histogramObserve(requestTimer);
    }
  }

  /**
   * Validates HTTP headers and JSON input before dispatching through jsonrpc4j.
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // Check the requested hostname before dispatch to guard against DNS rebinding.
    if (!virtualHostValidator.isAllowedHost(req.getHeader("Host"))) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid Host header");
      return;
    }
    // Require JSON media types so browser form and text/plain submissions are rejected.
    if (!JsonRpcMediaType.isSupported(req.getContentType())) {
      resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
      resp.setContentLength(0);
      return;
    }
    byte[] body = ByteStreams.toByteArray(req.getInputStream());
    JsonNode request;
    try {
      request = OBJECT_MAPPER.reader()
          .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(body);
      if (request == null || request.isMissingNode()) {
        writeErrorResponse(resp, JsonError.PARSE_ERROR.code, JsonError.PARSE_ERROR.message);
        return;
      }
    } catch (JsonProcessingException e) {
      writeErrorResponse(resp, JsonError.PARSE_ERROR.code, JsonError.PARSE_ERROR.message);
      return;
    }
    if (request.isArray()) {
      writeErrorResponse(resp, JsonError.INVALID_REQUEST.code,
          AdminJsonRpc.BATCH_NOT_SUPPORTED_MESSAGE);
      return;
    }
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    rpcServer.handleRequest(new ByteArrayInputStream(body), output);
    writeResponse(resp, output.toByteArray());
  }

  private void writeErrorResponse(HttpServletResponse resp, int code, String message)
      throws IOException {
    byte[] body = OBJECT_MAPPER.writeValueAsBytes(
        JsonRpcMapper.createErrorResponse(code, message, null));
    writeResponse(resp, body);
  }

  private void writeResponse(HttpServletResponse resp, byte[] body) throws IOException {
    // JSON-RPC result codes belong in the response body. HTTP validation still uses 403/415.
    resp.setContentType("application/json-rpc");
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentLength(body.length);
    resp.getOutputStream().write(body);
    resp.getOutputStream().flush();
  }
}
