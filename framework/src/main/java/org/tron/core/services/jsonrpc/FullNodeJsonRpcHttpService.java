package org.tron.core.services.jsonrpc;

import java.util.EnumSet;
import javax.servlet.DispatcherType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.application.HttpService;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.services.filter.HttpInterceptor;

@Component
@Slf4j(topic = "API")
public class FullNodeJsonRpcHttpService extends HttpService {

  @Autowired
  private JsonRpcServlet jsonRpcServlet;

  @Override
  public void init() {
  }

  @Override
  public void init(CommonParameter args) {
    port = CommonParameter.getInstance().getJsonRpcHttpFullNodePort();
  }

  @Override
  public void start() {
    try {
      apiServer = new Server(port);
      ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
      context.setContextPath("/");
      apiServer.setHandler(context);

      context.addServlet(new ServletHolder(jsonRpcServlet), "/jsonrpc");

      int maxHttpConnectNumber = CommonParameter.getInstance().getMaxHttpConnectNumber();
      if (maxHttpConnectNumber > 0) {
        apiServer.addBean(new ConnectionLimit(maxHttpConnectNumber, apiServer));
      }

      // filter
      ServletHandler handler = new ServletHandler();
      FilterHolder fh = handler
          .addFilterWithMapping(HttpInterceptor.class, "/*",
              EnumSet.of(DispatcherType.REQUEST));
      context.addFilter(fh, "/*", EnumSet.of(DispatcherType.REQUEST));

      super.start();

    } catch (Exception e) {
      logger.debug("IOException: {}", e.getMessage());
    }
  }
}
