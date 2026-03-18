package org.tron.core.services.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.HttpStatusCodeProvider;
import com.googlecode.jsonrpc4j.JsonRpcInterceptor;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.ProxyUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

  private static final ObjectMapper MAPPER = new ObjectMapper();

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
      writeJsonRpcError(resp,
          "Batch size " + rootNode.size() + " exceeds the limit of "
              + parameter.getJsonRpcMaxBatchSize(), null);
      return;
    }

    // Buffer the response to check its size before committing
    BufferedResponseWrapper bufferedResp = new BufferedResponseWrapper(resp);
    rpcServer.handle(new CachedBodyRequestWrapper(req, body), bufferedResp);

    byte[] responseBytes = bufferedResp.toByteArray();
    logger.info("responseBytes: {}", responseBytes.length);
    if (responseBytes.length > parameter.getJsonRpcMaxResponseSize()) {
      JsonNode idNode = (!rootNode.isArray()) ? rootNode.get("id") : null;
      writeJsonRpcError(resp,
          "Response byte size " + responseBytes.length + " exceeds the limit of "
              + parameter.getJsonRpcMaxResponseSize(), idNode);
      return;
    }

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

  private void writeJsonRpcError(HttpServletResponse resp, String message, JsonNode id)
      throws IOException {
    String idStr = (id != null && !id.isNull() && !id.isMissingNode()) ? id.toString() : "null";
    String body = "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":" + -32005
        + ",\"message\":\"" + message + "\"},\"id\":" + idStr + "}";
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    resp.setContentType("application/json");
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentLength(bytes.length);
    resp.getOutputStream().write(bytes);
    resp.getOutputStream().flush();
  }
}
