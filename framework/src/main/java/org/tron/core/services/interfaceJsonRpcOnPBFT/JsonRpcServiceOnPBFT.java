package org.tron.core.services.interfaceJsonRpcOnPBFT;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.application.HttpService;
import org.tron.core.config.args.Args;

@Component
@Slf4j(topic = "API")
public class JsonRpcServiceOnPBFT extends HttpService {

  @Autowired
  private JsonRpcOnPBFTServlet jsonRpcOnPBFTServlet;

  public JsonRpcServiceOnPBFT() {
    port = Args.getInstance().getJsonRpcHttpPBFTPort();
    enable = isFullNode() && Args.getInstance().isJsonRpcHttpPBFTNodeEnable();
    contextPath = "/";
  }

  @Override
  public void addServlet(ServletContextHandler context) {
    context.addServlet(new ServletHolder(jsonRpcOnPBFTServlet), "/jsonrpc");
  }
}
