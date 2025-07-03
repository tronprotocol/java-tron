package org.tron.core.services.jsonrpc;

import com.googlecode.jsonrpc4j.HttpStatusCodeProvider;
import com.googlecode.jsonrpc4j.JsonRpcInterceptor;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.ProxyUtil;
import java.io.IOException;
import java.util.Collections;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.Wallet;
import org.tron.core.db.Manager;
import org.tron.core.services.NodeInfoService;
import org.tron.core.services.http.RateLimiterServlet;

@Component
@Slf4j(topic = "API")
public class JsonRpcServlet extends RateLimiterServlet {

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
    rpcServer.handle(req, resp);
  }
}