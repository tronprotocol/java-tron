package org.tron.core.services.admin.http;

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
import org.tron.core.config.args.InetUtil;
import org.tron.core.services.filter.HttpInterceptor;

@Component
@Slf4j(topic = "API")
public class AdminRpcHttpService extends HttpService {

  @Autowired
  private AdminRpcServlet adminRpcServlet;

  public AdminRpcHttpService() {
    enable = isFullNode() && Args.getInstance().isAdminHttpEnable();
    listenAddress = Args.getInstance().getAdminHttpListenAddress();
    port = Args.getInstance().getAdminHttpListenPort();
    contextPath = "/";
  }

  @Override
  public void innerStart() throws Exception {
    if (enable && !InetUtil.isLoopbackAddress(listenAddress)) {
      logger.warn("Admin HTTP is enabled on {} and may be accessible remotely. "
          + "Restrict access to trusted networks.", listenAddress);
    }
    super.innerStart();
  }

  @Override
  protected void addServlet(ServletContextHandler context) {
    context.addServlet(new ServletHolder(adminRpcServlet), "/admin");
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
