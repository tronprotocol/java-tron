package org.tron.core.services.interfaceOnPBFT;

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
import org.tron.core.services.filter.PbftCursorFilter;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApiRegistry;

@Slf4j(topic = "API")
public class HttpApiOnPBFTService extends HttpService {

  @Autowired
  private LiteFnQueryHttpFilter liteFnQueryHttpFilter;
  @Autowired
  private HttpApiAccessFilter httpApiAccessFilter;
  @Autowired
  private PbftCursorFilter pbftCursorFilter;
  @Autowired
  private ApplicationContext appContext;

  public HttpApiOnPBFTService() {
    port = Args.getInstance().getPBFTHttpPort();
    enable = isFullNode() && Args.getInstance().isPBFTHttpEnable();
    contextPath = "/walletpbft";
    maxRequestSize = Args.getInstance().getHttpMaxMessageSize();
  }

  @Override
  protected void addServlet(ServletContextHandler context) {
    addServletsFromRegistry(context);
  }

  /**
   * Registry-driven registration: mounts every endpoint the registry declares for the PBFT
   * surface, resolving servlet beans from the application context.
   */
  protected void addServletsFromRegistry(ServletContextHandler context) {
    for (HttpApiRegistry.Entry def : HttpApiRegistry.forSurface(HttpApi.Surface.PBFT)) {
      context.addServlet(new ServletHolder(appContext.getBean(def.getServlet())),
          "/" + def.getSuffix());
    }
  }

  @Override
  protected void addFilter(ServletContextHandler context) {
    // filters the specified APIs
    // when node is lite fullnode and openHistoryQueryWhenLiteFN is false
    context.addFilter(new FilterHolder(liteFnQueryHttpFilter), "/*",
        EnumSet.allOf(DispatcherType.class));

    // api access filter
    context.addFilter(new FilterHolder(httpApiAccessFilter), "/*",
        EnumSet.allOf(DispatcherType.class));

    // every request on this port reads the PBFT state view
    context.addFilter(new FilterHolder(pbftCursorFilter), "/*",
        EnumSet.allOf(DispatcherType.class));
  }
}
