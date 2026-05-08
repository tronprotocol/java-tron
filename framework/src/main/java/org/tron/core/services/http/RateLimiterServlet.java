package org.tron.core.services.http;

import com.google.common.base.Strings;
import io.prometheus.client.Histogram;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.common.parameter.RateLimiterInitialization;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.MetricLabels;
import org.tron.common.prometheus.Metrics;
import org.tron.core.config.args.Args;
import org.tron.core.exception.TronError;
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

  @PostConstruct
  private void addRateContainer() {
    final String name = getClass().getSimpleName();
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
    
    RuntimeData runtimeData = new RuntimeData(req);
    GlobalRateLimiter.acquire(runtimeData);

    IRateLimiter rateLimiter = container.get(KEY_PREFIX_HTTP, getClass().getSimpleName());

    boolean acquireResource = true;

    if (rateLimiter != null) {
      acquireResource = rateLimiter.acquire(runtimeData);
    }
    String contextPath = req.getContextPath();
    String url = Strings.isNullOrEmpty(req.getServletPath())
        ? MetricLabels.UNDEFINED : contextPath + req.getServletPath();
    try {
      resp.setContentType("application/json; charset=utf-8");

      if (acquireResource) {
        Histogram.Timer requestTimer = Metrics.histogramStartTimer(
            MetricKeys.Histogram.HTTP_SERVICE_LATENCY, url);
        super.service(req, resp);
        Metrics.histogramObserve(requestTimer);
      } else {
        resp.getWriter()
            .println(Util.printErrorMsg(new IllegalAccessException("lack of computing resources")));
      }
    } catch (ServletException | IOException e) {
      throw e;
    } catch (Exception unexpected) {
      logger.error("Http Api {}, Method:{}. Error：", url, req.getMethod(), unexpected);
    } finally {
      if (rateLimiter instanceof IPreemptibleRateLimiter && acquireResource) {
        ((IPreemptibleRateLimiter) rateLimiter).release();
      }
    }
  }
}