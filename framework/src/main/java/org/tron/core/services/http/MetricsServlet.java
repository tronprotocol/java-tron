package org.tron.core.services.http;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.metrics.MetricsApiService;
import org.tron.core.metrics.MetricsInfo;
import org.tron.json.JSON;

@Component
@Slf4j(topic = "API")
public class MetricsServlet extends RateLimiterServlet {

  private static final AtomicBoolean deprecatedWarned = new AtomicBoolean(false);

  @Autowired
  private MetricsApiService metricsApiService;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    if (deprecatedWarned.compareAndSet(false, true)) {
      logger.warn("HTTP /monitor/getstatsinfo is deprecated and will be removed in a "
          + "future major release; migrate to the prometheus metrics endpoint");
    }
    try {
      MetricsInfo metricsInfo = metricsApiService.getMetricsInfo();

      if (metricsInfo != null) {
        response.getWriter().println(JSON.toJSONString(metricsInfo, true));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
