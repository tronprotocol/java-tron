package org.tron.core.services.interfaceOnSolidity;

import java.util.EnumSet;
import javax.servlet.DispatcherType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.tron.common.application.HttpService;
import org.tron.core.config.args.Args;
import org.tron.core.services.filter.HttpApiAccessFilter;
import org.tron.core.services.filter.LiteFnQueryHttpFilter;
import org.tron.core.services.filter.SolidityCursorFilter;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApiRegistry;
import org.tron.core.services.http.servlets.GetNodeInfoServlet;

@Slf4j(topic = "API")
public class HttpApiOnSolidityService extends HttpService {

  @Autowired
  private LiteFnQueryHttpFilter liteFnQueryHttpFilter;

  @Autowired
  private HttpApiAccessFilter httpApiAccessFilter;

  @Autowired
  private SolidityCursorFilter solidityCursorFilter;

  @Autowired
  private ApplicationContext appContext;

  public HttpApiOnSolidityService() {
    port = Args.getInstance().getSolidityHttpPort();
    enable = isFullNode() && Args.getInstance().isSolidityNodeHttpEnable();
    contextPath = "/";
    maxRequestSize = Args.getInstance().getHttpMaxMessageSize();
  }

  @Override
  protected void addServlet(ServletContextHandler context) {
    addServletsFromRegistry(context);
  }

  /**
   * Registry-driven registration: mounts every endpoint the registry declares for the SOLIDITY
   * surface under the /walletsolidity prefix, resolving servlet beans from the application
   * context; getnodeinfo is additionally reachable under the fullnode prefix on this port.
   */
  protected void addServletsFromRegistry(ServletContextHandler context) {
    for (HttpApiRegistry.Entry def : HttpApiRegistry.forSurface(HttpApi.Surface.SOLIDITY)) {
      context.addServlet(new ServletHolder(appContext.getBean(def.getServlet())),
          "/walletsolidity/" + def.getSuffix());
    }
    // surface-specific alias kept for compatibility: /wallet/getnodeinfo answers on this port
    context.addServlet(new ServletHolder(appContext.getBean(GetNodeInfoServlet.class)),
        "/wallet/getnodeinfo");
  }

  @Override
  protected void addFilter(ServletContextHandler context) {
    // filters the specified APIs
    // when node is lite fullnode and openHistoryQueryWhenLiteFN is false
    context.addFilter(new FilterHolder(liteFnQueryHttpFilter), "/*",
        EnumSet.allOf(DispatcherType.class));

    // api access filter
    context.addFilter(new FilterHolder(httpApiAccessFilter), "/walletsolidity/*",
        EnumSet.allOf(DispatcherType.class));
    context.getServletHandler().getFilterMappings()[1]
        .setPathSpecs(new String[] {"/walletsolidity/*",
            "/wallet/getnodeinfo"});

    // every request on this port reads the SOLIDITY state view
    context.addFilter(new FilterHolder(solidityCursorFilter), "/*",
        EnumSet.allOf(DispatcherType.class));
  }
}
