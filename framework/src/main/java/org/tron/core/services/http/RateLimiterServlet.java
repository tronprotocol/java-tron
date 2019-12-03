package org.tron.core.services.http;

import java.io.IOException;
import java.lang.reflect.Constructor;
import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.common.parameter.RateLimiterInitialization;
import org.tron.core.config.args.Args;
import org.tron.core.services.ratelimiter.RateLimiterContainer;
import org.tron.core.services.ratelimiter.RuntimeData;
import org.tron.core.services.ratelimiter.adapter.DefaultBaseQqsAdapter;
import org.tron.core.services.ratelimiter.adapter.GlobalPreemptibleAdapter;
import org.tron.core.services.ratelimiter.adapter.IPQPSRateLimiterAdapter;
import org.tron.core.services.ratelimiter.adapter.IPreemptibleRateLimiter;
import org.tron.core.services.ratelimiter.adapter.IRateLimiter;
import org.tron.core.services.ratelimiter.adapter.QpsRateLimiterAdapter;

@Slf4j
public abstract class RateLimiterServlet extends HttpServlet {

  private static final String KEY_PREFIX_HTTP = "http_";
  private static final String ADAPTER_PREFIX = "org.tron.core.services.ratelimiter.adapter.";

  @Autowired
  private RateLimiterContainer container;

  @PostConstruct
  private void addRateContainer() {

    RateLimiterInitialization.HttpRateLimiterItem item = Args.getInstance()
        .getRateLimiterInitialization().getHttpMap().get(getClass().getSimpleName());

    boolean success = false;

    if (item != null) {
      String cName = "";
      String params = "";
      Object obj;
      try {
        cName = item.getStrategy();
        params = item.getParams();

        // add the specific rate limiter strategy of servlet.
        Class<?> c = Class.forName(ADAPTER_PREFIX + cName);
        Constructor constructor;
        if (c == GlobalPreemptibleAdapter.class || c == QpsRateLimiterAdapter.class
            || c == IPQPSRateLimiterAdapter.class) {
          constructor = c.getConstructor(String.class);
          obj = constructor.newInstance(params);
          container.add(KEY_PREFIX_HTTP, getClass().getSimpleName(), (IRateLimiter) obj);

        } else {
          constructor = c.getConstructor();
          obj = constructor.newInstance();
          container.add(KEY_PREFIX_HTTP, getClass().getSimpleName(), (IRateLimiter) obj);
        }
        success = true;
      } catch (Exception e) {
        logger.warn(
            "failure to add the rate limiter strategy. servlet = {}, "
                + "strategy name = {}, params = \"{}\".",
            getClass().getSimpleName(), cName, params);
      }
    }

    if (!success) {
      // if the specific rate limiter strategy of servlet is not defined or fail to add,
      // then add a default Strategy.
      try {
        IRateLimiter rateLimiter = new DefaultBaseQqsAdapter("qps=1000");
        container.add(KEY_PREFIX_HTTP, getClass().getSimpleName(), rateLimiter);
      } catch (Exception e) {
        logger.warn(
            "failure to add the default rate limiter strategy. servlet = {}.",
            getClass().getSimpleName());
      }
    }

  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    IRateLimiter rateLimiter = container.get(KEY_PREFIX_HTTP, getClass().getSimpleName());

    boolean acquireResource = true;

    if (rateLimiter != null) {
      acquireResource = rateLimiter.acquire(new RuntimeData(req));
    }

    try {
      if (acquireResource) {
        super.service(req, resp);
      } else {
        resp.getWriter()
            .println(Util.printErrorMsg(new IllegalAccessException("lack of computing resources")));
      }
    } catch (ServletException | IOException e) {
      throw e;
    } catch (Exception unexpected) {
      logger.error("Http Api Error: {}", unexpected.getMessage());
    } finally {
      if (rateLimiter instanceof IPreemptibleRateLimiter && acquireResource) {
        ((IPreemptibleRateLimiter) rateLimiter).release();
      }
    }
  }
}