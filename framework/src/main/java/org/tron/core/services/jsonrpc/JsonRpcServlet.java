package org.tron.core.services.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.googlecode.jsonrpc4j.HttpStatusCodeProvider;
import com.googlecode.jsonrpc4j.JsonRpcInterceptor;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.ProxyUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.exception.jsonrpc.JsonRpcResponseTooLargeException;
import org.tron.core.services.filter.BufferedResponseWrapper;
import org.tron.core.services.filter.CachedBodyRequestWrapper;
import org.tron.core.services.http.RateLimiterServlet;

@Component
@Slf4j(topic = "API")
public class JsonRpcServlet extends RateLimiterServlet {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final ExecutorService RPC_EXECUTOR = Executors.newCachedThreadPool(
      new ThreadFactoryBuilder().setNameFormat("jsonrpc-timeout-%d").setDaemon(true).build());

  enum JsonRpcError {
    EXCEED_LIMIT(-32005),
    RESPONSE_TOO_LARGE(-32003),
    TIMEOUT(-32002);

    final int code;

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

    // Read request body so we can inspect and replay it
    byte[] body = readBody(req.getInputStream());

    // Check batch request array length
    JsonNode rootNode = MAPPER.readTree(body);
    if (rootNode.isArray() && rootNode.size() > parameter.getJsonRpcMaxBatchSize()) {
      writeJsonRpcError(resp, JsonRpcError.EXCEED_LIMIT,
          "Batch size " + rootNode.size() + " exceeds the limit of "
              + parameter.getJsonRpcMaxBatchSize(), null);
      return;
    }

    // Buffer the response; limit is enforced eagerly during writes to bound memory usage
    int maxResponseSize = parameter.getJsonRpcMaxResponseSize();
    CachedBodyRequestWrapper cachedReq = new CachedBodyRequestWrapper(req, body);
    BufferedResponseWrapper bufferedResp = new BufferedResponseWrapper(resp, maxResponseSize);

    int timeoutSec = parameter.getJsonRpcMaxRequestTimeout();
    Future<?> future = RPC_EXECUTOR.submit(() -> {
      try {
        rpcServer.handle(cachedReq, bufferedResp);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    try {
      future.get(timeoutSec, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      future.cancel(true);
      JsonNode idNode = (!rootNode.isArray()) ? rootNode.get("id") : null;
      writeJsonRpcError(resp, JsonRpcError.TIMEOUT, "Request timeout after " + timeoutSec + "s",
          idNode);
      return;
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException
          && cause.getCause() instanceof JsonRpcResponseTooLargeException) {
        JsonNode idNode = (!rootNode.isArray()) ? rootNode.get("id") : null;
        writeJsonRpcError(resp, JsonRpcError.RESPONSE_TOO_LARGE, cause.getCause().getMessage(),
            idNode);
        return;
      }
      throw new IOException("RPC execution failed", cause);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("RPC interrupted", e);
    }

    byte[] responseBytes = bufferedResp.toByteArray();
    resp.setContentLength(responseBytes.length);
    resp.getOutputStream().write(responseBytes);
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

  private void writeJsonRpcError(HttpServletResponse resp, JsonRpcError error, String message,
      JsonNode id) throws IOException {
    String idStr = (id != null && !id.isNull() && !id.isMissingNode()) ? id.toString() : "null";
    String body = "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":" + error.code
        + ",\"message\":\"" + message + "\"},\"id\":" + idStr + "}";
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    resp.setContentType("application/json");
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentLength(bytes.length);
    resp.getOutputStream().write(bytes);
    resp.getOutputStream().flush();
  }
}
