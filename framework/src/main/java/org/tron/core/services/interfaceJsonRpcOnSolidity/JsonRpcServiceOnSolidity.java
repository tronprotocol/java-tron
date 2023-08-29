package org.tron.core.services.interfaceJsonRpcOnSolidity;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.application.HttpService;
import org.tron.common.parameter.CommonParameter;

@Component
@Slf4j(topic = "API")
public class JsonRpcServiceOnSolidity extends HttpService {

  @Autowired
  private JsonRpcOnSolidityServlet jsonRpcOnSolidityServlet;

  @Override
  public void init() {
  }

  @Override
  public void init(CommonParameter args) {
    port = CommonParameter.getInstance().getJsonRpcHttpSolidityPort();
  }

  @Override
  public void start() {
    try {
      apiServer = new Server(port);
      ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
      context.setContextPath("/");
      apiServer.setHandler(context);

      context.addServlet(new ServletHolder(jsonRpcOnSolidityServlet), "/jsonrpc");

      int maxHttpConnectNumber = CommonParameter.getInstance().getMaxHttpConnectNumber();
      if (maxHttpConnectNumber > 0) {
        apiServer.addBean(new ConnectionLimit(maxHttpConnectNumber, apiServer));
      }
      super.start();

    } catch (Exception e) {
      logger.debug("IOException: {}", e.getMessage());
    }
  }
}
