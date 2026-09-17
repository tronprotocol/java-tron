package org.tron.core.services.http;

import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.tron.common.application.HttpService;
import org.tron.core.config.args.Args;
import org.tron.core.services.filter.HttpApiAccessFilter;
import org.tron.core.services.filter.HttpInterceptor;
import org.tron.core.services.filter.LiteFnQueryHttpFilter;
import org.tron.core.services.http.servlets.GetNodeInfoServlet;
import org.tron.core.services.http.servlets.ListNodesServlet;
import org.tron.core.services.http.servlets.MetricsServlet;

@Component("fullNodeHttpApiService")
@Slf4j(topic = "API")
public class FullNodeHttpApiService extends HttpService {

  @Autowired
  private LiteFnQueryHttpFilter liteFnQueryHttpFilter;
  @Autowired
  private HttpApiAccessFilter httpApiAccessFilter;

  @Autowired
  private ApplicationContext appContext;

  public FullNodeHttpApiService() {
    port = Args.getInstance().getFullNodeHttpPort();
    enable = isFullNode() && Args.getInstance().isFullNodeHttpEnable();
    contextPath = "/";
    maxRequestSize = Args.getInstance().getHttpMaxMessageSize();
  }

  @Override
  protected void addServlet(ServletContextHandler context) {
    addServletsFromRegistry(context);
  }

  /**
   * Registry-driven registration: mounts every endpoint the registry declares for the FULL
   * surface under the /wallet prefix, resolving servlet beans from the application context.
   * The three root-mounted endpoints (net / monitor) are not under /wallet and are mounted
   * explicitly; their paths must stay listed in the access-filter pathSpecs in
   * {@link #addFilter}.
   */
  protected void addServletsFromRegistry(ServletContextHandler context) {
    for (HttpApiRegistry.Entry def : HttpApiRegistry.forSurface(HttpApi.Surface.FULL)) {
      context.addServlet(new ServletHolder(appContext.getBean(def.getServlet())),
          "/wallet/" + def.getSuffix());
    }
    // root-mounted endpoints, not under the /wallet prefix
    context.addServlet(new ServletHolder(appContext.getBean(ListNodesServlet.class)),
        "/net/listnodes");
    context.addServlet(new ServletHolder(appContext.getBean(MetricsServlet.class)),
        "/monitor/getstatsinfo");
    context.addServlet(new ServletHolder(appContext.getBean(GetNodeInfoServlet.class)),
        "/monitor/getnodeinfo");
  }

  @Override
  protected void addFilter(ServletContextHandler context) {
    // filters the specified APIs
    // when node is lite fullnode and openHistoryQueryWhenLiteFN is false
    context.addFilter(new FilterHolder(liteFnQueryHttpFilter), "/*",
        EnumSet.allOf(DispatcherType.class));

    // http access filter, it should have higher priority than HttpInterceptor
    context.addFilter(new FilterHolder(httpApiAccessFilter), "/*",
        EnumSet.allOf(DispatcherType.class));
    // note: if the pathSpec of servlet is not started with wallet, it should be included here
    context.getServletHandler().getFilterMappings()[1]
        .setPathSpecs(new String[] {"/wallet/*",
            "/net/listnodes",
            "/monitor/getstatsinfo",
            "/monitor/getnodeinfo"});

    // metrics filter
    ServletHandler handler = new ServletHandler();
    FilterHolder fh = handler
        .addFilterWithMapping((Class<? extends Filter>) HttpInterceptor.class, "/*",
            EnumSet.of(DispatcherType.REQUEST));
    context.addFilter(fh, "/*", EnumSet.of(DispatcherType.REQUEST));
  }
}
