package org.tron.core.services.jsonrpc;

import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.ProxyUtil;
import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;
import org.tron.core.services.NodeInfoService;
import org.tron.core.services.http.RateLimiterServlet;

@Component
@Slf4j(topic = "API")
public class JsonRpcServlet extends RateLimiterServlet {

  private static final long serialVersionUID = 12341234345L;
  private JsonRpcServer rpcServer = null;
  private TestServiceImpl testServiceImpl;

  @Autowired
  private NodeInfoService nodeInfoService;
  @Autowired
  private Wallet wallet;

  // @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    testServiceImpl = new TestServiceImpl(nodeInfoService, wallet);
    Object compositeService = ProxyUtil.createCompositeServiceProxy(
        this.getClass().getClassLoader(),
        new Object[] {testServiceImpl},
        new Class[] {TestService.class},
        true);

    rpcServer = new JsonRpcServer(compositeService);
  }

  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    rpcServer.handle(req, resp);
  }
}