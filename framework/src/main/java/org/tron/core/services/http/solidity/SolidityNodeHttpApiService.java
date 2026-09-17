package org.tron.core.services.http.solidity;

import java.util.EnumSet;
import javax.servlet.DispatcherType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.tron.common.application.HttpService;
import org.tron.core.config.args.Args;
import org.tron.core.services.filter.HttpApiAccessFilter;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApiRegistry;
import org.tron.core.services.http.servlets.GetNodeInfoServlet;

@Component
@Slf4j(topic = "API")
public class SolidityNodeHttpApiService extends HttpService {

  @Autowired
  private HttpApiAccessFilter httpApiAccessFilter;

  @Autowired
  private ApplicationContext appContext;

  public SolidityNodeHttpApiService() {
    port = Args.getInstance().getSolidityHttpPort();
    enable = !isFullNode() && Args.getInstance().isSolidityNodeHttpEnable();
    contextPath = "/";
    maxRequestSize = Args.getInstance().getHttpMaxMessageSize();
  }

  @Override
  protected void addServlet(ServletContextHandler context) {
    addServletsFromRegistry(context);
  }

  /**
   * Registry-driven registration: mounts every endpoint the registry declares for the
   * SOLIDITY_NODE surface under the /walletsolidity prefix, resolving servlet beans from the
   * application context; getnodeinfo is additionally reachable under the fullnode prefix.
   */
  protected void addServletsFromRegistry(ServletContextHandler context) {
    for (HttpApiRegistry.Entry def : HttpApiRegistry.forSurface(HttpApi.Surface.SOLIDITY_NODE)) {
      context.addServlet(new ServletHolder(appContext.getBean(def.getServlet())),
          "/walletsolidity/" + def.getSuffix());
    }
    // surface-specific alias kept for compatibility: /wallet/getnodeinfo answers on this port
    context.addServlet(new ServletHolder(appContext.getBean(GetNodeInfoServlet.class)),
        "/wallet/getnodeinfo");
  }

  @Override
  protected void addFilter(ServletContextHandler context) {
    // http access filter
    context.addFilter(new FilterHolder(httpApiAccessFilter), "/walletsolidity/*",
        EnumSet.allOf(DispatcherType.class));
    context.getServletHandler().getFilterMappings()[0]
        .setPathSpecs(new String[] {"/walletsolidity/*",
            "/wallet/getnodeinfo"});
  }
}
