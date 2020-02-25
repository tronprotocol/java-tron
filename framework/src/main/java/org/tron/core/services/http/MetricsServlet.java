package org.tron.core.services.http;

import com.alibaba.fastjson.JSON;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.metrics.MetricsInfo;
import org.tron.core.metrics.MetricsService;

@Component
@Slf4j(topic = "API")
public class MetricsServlet extends RateLimiterServlet {

  @Autowired
  MetricsService metricsService;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      MetricsInfo metricsInfo = metricsService.getMetricsInfo();

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
