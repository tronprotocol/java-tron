package org.tron.core.services.interfaceJsonRpcOnSolidity;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.application.Service;
import org.tron.common.parameter.CommonParameter;

@Component
@Slf4j(topic = "API")
public class JsonRpcOnSolidityService implements Service {

  private int port = CommonParameter.getInstance().getSolidityJsonRpcHttpPort();

  private Server server;

  @Autowired
  private JsonRpcOnSolidityServlet jsonRpcOnSolidityServlet;

  @Override
  public void init() {
  }

  @Override
  public void init(CommonParameter args) {
  }

  @Override
  public void start() {
    try {
      server = new Server(port);
      ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
      context.setContextPath("/");
      server.setHandler(context);

      context.addServlet(new ServletHolder(jsonRpcOnSolidityServlet), "/jsonrpc");

      // filter
      // ServletHandler handler = new ServletHandler();
      // FilterHolder fh = handler
      //     .addFilterWithMapping((Class<? extends Filter>) HttpInterceptor.class, "/*",
      //         EnumSet.of(DispatcherType.REQUEST));
      // context.addFilter(fh, "/*", EnumSet.of(DispatcherType.REQUEST));

      server.start();

    } catch (Exception e) {
      logger.debug("IOException: {}", e.getMessage());
    }
  }

  @Override
  public void stop() {
    try {
      server.stop();
    } catch (Exception e) {
      logger.debug("IOException: {}", e.getMessage());
    }
  }
}
