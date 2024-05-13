package org.tron.core.services.jsonrpc;

import java.util.EnumSet;
import javax.servlet.DispatcherType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.application.HttpService;
import org.tron.core.config.args.Args;
import org.tron.core.services.filter.HttpInterceptor;

@Component
@Slf4j(topic = "API")
public class FullNodeJsonRpcHttpService extends HttpService {

  @Autowired
  private JsonRpcServlet jsonRpcServlet;

  public FullNodeJsonRpcHttpService() {
    port = Args.getInstance().getJsonRpcHttpFullNodePort();
    enable = isFullNode() && Args.getInstance().isJsonRpcHttpFullNodeEnable();
    contextPath = "/";
  }

  @Override
  protected void addServlet(ServletContextHandler context) {
    context.addServlet(new ServletHolder(jsonRpcServlet), "/jsonrpc");
  }

  @Override
  protected void addFilter(ServletContextHandler context) {
    // filter
    ServletHandler handler = new ServletHandler();
    FilterHolder fh = handler
        .addFilterWithMapping(HttpInterceptor.class, "/*",
            EnumSet.of(DispatcherType.REQUEST));
    context.addFilter(fh, "/*", EnumSet.of(DispatcherType.REQUEST));
  }
}
