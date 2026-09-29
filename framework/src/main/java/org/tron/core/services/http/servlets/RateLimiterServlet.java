package org.tron.core.services.http.servlets;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import io.prometheus.client.Histogram;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.BadMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.common.parameter.RateLimiterInitialization;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.MetricLabels;
import org.tron.common.prometheus.Metrics;
import org.tron.core.config.args.Args;
import org.tron.core.exception.TronError;
import org.tron.core.services.WalletOnCursor;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Surface;
import org.tron.core.services.interfaceOnPBFT.WalletOnPBFT;
import org.tron.core.services.ratelimiter.GlobalRateLimiter;
import org.tron.core.services.ratelimiter.RateLimiterContainer;
import org.tron.core.services.ratelimiter.RuntimeData;
import org.tron.core.services.ratelimiter.adapter.DefaultBaseQqsAdapter;
import org.tron.core.services.ratelimiter.adapter.GlobalPreemptibleAdapter;
import org.tron.core.services.ratelimiter.adapter.IPQPSRateLimiterAdapter;
import org.tron.core.services.ratelimiter.adapter.IPreemptibleRateLimiter;
import org.tron.core.services.ratelimiter.adapter.IRateLimiter;
import org.tron.core.services.ratelimiter.adapter.QpsRateLimiterAdapter;
import org.tron.core.services.ratelimiter.strategy.QpsStrategy;

@Slf4j
public abstract class RateLimiterServlet extends HttpServlet {
  private static final String KEY_PREFIX_HTTP = "http_";

  /** Servlet-context attribute holding the {@link Surface} an http service's context serves. */
  public static final String SURFACE_ATTRIBUTE = Surface.class.getName();

  /**
   * Endpoints whose rate limiter is named {@code <stem>SolidityServlet} on the SOLIDITY_NODE
   * surface; every other endpoint uses its class simple name there.
   */
  private static final Set<String> SOLIDITY_NODE_OWN_NAMES = ImmutableSet.of(
      "GetTransactionByIdServlet", "GetTransactionInfoByIdServlet");

  static final Map<String, Class<? extends IRateLimiter>> ALLOWED_ADAPTERS;
  static final String DEFAULT_ADAPTER_NAME = DefaultBaseQqsAdapter.class.getSimpleName();

  static {
    List<Class<? extends IRateLimiter>> adapters = Arrays.asList(
        GlobalPreemptibleAdapter.class,
        QpsRateLimiterAdapter.class,
        IPQPSRateLimiterAdapter.class,
        DefaultBaseQqsAdapter.class);
    Map<String, Class<? extends IRateLimiter>> m = new HashMap<>();
    for (Class<? extends IRateLimiter> c : adapters) {
      m.put(c.getSimpleName(), c);
    }
    ALLOWED_ADAPTERS = Collections.unmodifiableMap(m);
  }

  @Autowired
  private RateLimiterContainer container;

  @Autowired
  private WalletOnPBFT walletOnPBFT;

  /**
   * Name of the rate limiter, which is also its {@code rate.limiter.http} component, for the
   * endpoint {@code servlet} serves on {@code surface}: the class simple name on FULL,
   * {@code <stem>OnSolidityServlet} on SOLIDITY and {@code <stem>OnPBFTServlet} on PBFT. Each
   * surface therefore keeps its own quota, and per-surface configurations keep applying.
   */
  public static String limiterName(Class<?> servlet, Surface surface) {
    String name = servlet.getSimpleName();
    String stem = name.endsWith("Servlet")
        ? name.substring(0, name.length() - "Servlet".length()) : name;
    switch (surface) {
      case SOLIDITY:
        return stem + "OnSolidityServlet";
      case PBFT:
        return stem + "OnPBFTServlet";
      case SOLIDITY_NODE:
        return SOLIDITY_NODE_OWN_NAMES.contains(name) ? stem + "SolidityServlet" : name;
      default:
        return name;
    }
  }

  /**
   * Builds one limiter per name this servlet is served under: its class simple name, which also
   * covers contexts that declare no surface, and one per surface its {@link HttpApi} declares.
   */
  @PostConstruct
  private void addRateContainer() {
    Set<String> names = new LinkedHashSet<>();
    names.add(getClass().getSimpleName());
    HttpApi api = getClass().getDeclaredAnnotation(HttpApi.class);
    if (api != null) {
      for (Surface surface : api.surfaces()) {
        names.add(limiterName(getClass(), surface));
      }
    }
    for (String name : names) {
      addRateLimiter(name);
    }
  }

  private void addRateLimiter(String name) {
    RateLimiterInitialization.HttpRateLimiterItem item = Args.getInstance()
        .getRateLimiterInitialization().getHttpMap().get(name);

    String cName;
    String params;
    if (item == null) {
      cName = DEFAULT_ADAPTER_NAME;
      params = QpsStrategy.DEFAULT_QPS_PARAM;
    } else {
      cName = item.getStrategy();
      params = item.getParams();
    }

    try {
      container.add(KEY_PREFIX_HTTP, name, buildAdapter(cName, params, name));
    } catch (Exception e) {
      throw rateLimiterInitError(cName, params, name, e);
    }
  }

  static IRateLimiter buildAdapter(String cName, String params, String name) {
    Class<? extends IRateLimiter> c = ALLOWED_ADAPTERS.get(cName);
    if (c == null) {
      throw rateLimiterInitError(cName, params, name,
          new IllegalArgumentException("unknown rate limiter adapter; allowed="
              + ALLOWED_ADAPTERS.keySet()));
    }
    try {
      return c.getConstructor(String.class).newInstance(params);
    } catch (Exception e) {
      throw rateLimiterInitError(cName, params, name, e);
    }
  }

  private static TronError rateLimiterInitError(String strategy, String params, String servlet,
      Exception e) {
    return new TronError("failure to add the rate limiter strategy. servlet = " + servlet
        + ", strategy name = " + strategy + ", params = \"" + params + "\".",
            e, TronError.ErrCode.RATE_LIMITER_INIT);
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    Surface surface = surfaceOf(req);
    RuntimeData runtimeData = new RuntimeData(req);
    IRateLimiter rateLimiter = container.get(KEY_PREFIX_HTTP, limiterName(getClass(), surface));

    // Check per-endpoint first to avoid consuming global IP/QPS quota for requests
    // that would be rejected by the per-endpoint limiter anyway. acquirePermit()
    // chooses blocking or non-blocking semantics based on rate.limiter.apiNonBlocking.
    boolean perEndpointAcquired = rateLimiter == null || rateLimiter.acquirePermit(runtimeData);
    boolean acquireResource = perEndpointAcquired && GlobalRateLimiter.acquirePermit(runtimeData);

    String contextPath = req.getContextPath();
    String url = Strings.isNullOrEmpty(req.getServletPath())
        ? MetricLabels.UNDEFINED : contextPath + req.getServletPath();
    // int64_as_string is honored only on GET requests (URL query). POST is intentionally
    // unsupported because reading the body here would consume request.getReader() and
    // break downstream servlets that read it themselves.
    if ("GET".equalsIgnoreCase(req.getMethod())) {
      JsonFormat.setInt64AsString(Util.getInt64AsString(req));
    }
    try {
      resp.setContentType("application/json; charset=utf-8");

      if (acquireResource) {
        Histogram.Timer requestTimer = Metrics.histogramStartTimer(
            MetricKeys.Histogram.HTTP_SERVICE_LATENCY, url);
        serviceOnSurface(surface, req, resp);
        Metrics.histogramObserve(requestTimer);
      } else {
        Util.writeAuditedError(Util.RATE_LIMITER_ERROR_MSG, resp);
      }
    } catch (ServletException | IOException | BadMessageException e) {
      throw e;
    } catch (Exception unexpected) {
      logger.error("Http Api {}, Method:{}. Error：", url, req.getMethod(), unexpected);
    } finally {
      // CRITICAL: this clear pairs with the setInt64AsString call above. Removing it
      // will leak int64_as_string state across requests on reused Tomcat threads,
      // producing intermittent quoted/unquoted output that is very hard to debug.
      JsonFormat.clearInt64AsString();
      // Release whenever the per-endpoint permit was acquired (covers both the normal
      // completion path and the case where GlobalRateLimiter rejected the request).
      if (rateLimiter instanceof IPreemptibleRateLimiter && perEndpointAcquired) {
        ((IPreemptibleRateLimiter) rateLimiter).release();
      }
    }
  }

  /**
   * Runs the endpoint. On PBFT the cursor is selected here, after both limiters admitted the
   * request: a PBFT cursor is an offset from the live head, so selecting it before an admission
   * that blocks would let the reads pass the PBFT-finalized block while the head advances.
   */
  private void serviceOnSurface(Surface surface, HttpServletRequest req,
      HttpServletResponse resp) throws ServletException, IOException {
    if (surface != Surface.PBFT) {
      super.service(req, resp);
      return;
    }
    try (WalletOnCursor.CursorScope ignored = walletOnPBFT.selectCursor()) {
      super.service(req, resp);
    }
  }

  private static Surface surfaceOf(HttpServletRequest req) {
    ServletContext context = req.getServletContext();
    Object surface = context == null ? null : context.getAttribute(SURFACE_ATTRIBUTE);
    return surface instanceof Surface ? (Surface) surface : Surface.FULL;
  }
}
