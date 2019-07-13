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
import org.tron.core.config.args.Args;
import org.tron.core.config.args.RateLimiterInitialization;
import org.tron.core.services.ratelimiter.RateLimiterContainer;
import org.tron.core.services.ratelimiter.adapter.GlobalPreemptibleAdapter;
import org.tron.core.services.ratelimiter.adapter.IPQPSRateLimiterAdapter;
import org.tron.core.services.ratelimiter.adapter.IPreemptibleRateLimiter;
import org.tron.core.services.ratelimiter.adapter.IRateLimiter;
import org.tron.core.services.ratelimiter.adapter.QpsRateLimiterAdapter;
import org.tron.core.services.ratelimiter.RuntimeData;

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

    if (item == null) {
      return;
    }

    String cName = item.getStrategy();
    String params = item.getParams();

    if ("".equals(cName)) {
      return;
    }

    Object obj;

    // init the http api rate limiter.
    try {
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

    } catch (Exception e) {
      logger.warn("the ratelimiter adaptor {} is undefined.", cName);
    }
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    IRateLimiter rateLimiter = container.get(KEY_PREFIX_HTTP, getClass().getSimpleName());

    boolean acquireResource = true;
    //todo: add default ratelimiter
    if (rateLimiter != null) {
      acquireResource = rateLimiter.acquire(new RuntimeData(req));
    }

    if (acquireResource) {
      super.service(req, resp);
    } else {
      resp.getWriter()
          .println(Util.printErrorMsg(new IllegalAccessException("lack of computing resources")));
    }

    if (rateLimiter != null) {
      if (rateLimiter instanceof IPreemptibleRateLimiter && acquireResource) {
        ((IPreemptibleRateLimiter) rateLimiter).release();
      }
    }
  }
}