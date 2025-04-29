package org.tron.core.services.interfaceJsonRpcOnSolidity;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.application.HttpService;
import org.tron.core.config.args.Args;

@Component
@Slf4j(topic = "API")
public class JsonRpcServiceOnSolidity extends HttpService {

  @Autowired
  private JsonRpcOnSolidityServlet jsonRpcOnSolidityServlet;

  public JsonRpcServiceOnSolidity() {
    port = Args.getInstance().getJsonRpcHttpSolidityPort();
    enable = isFullNode() && Args.getInstance().isJsonRpcHttpSolidityNodeEnable();
    contextPath = "/";
  }

  @Override
  public void addServlet(ServletContextHandler context) {
    context.addServlet(new ServletHolder(jsonRpcOnSolidityServlet), "/jsonrpc");
  }
}
