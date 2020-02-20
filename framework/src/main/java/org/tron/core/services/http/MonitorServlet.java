package org.tron.core.services.http;

import com.alibaba.fastjson.JSON;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.entity.NodeInfo;
import org.tron.core.services.monitor.MonitorInfo;
import org.tron.core.services.monitor.MonitorService;
import org.tron.protos.Protocol;

@Component
@Slf4j(topic = "API")
public class MonitorServlet extends RateLimiterServlet {

  @Autowired
  MonitorService monitorService;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
        MonitorInfo monitorInfo = monitorService.getMonitorInfo();

    //   Protocol.MonitorInfo monitorInfo = monitorService.getMonitorInfo();

      if (monitorInfo != null) {
         response.getWriter().println(JSON.toJSONString(monitorInfo,true));
      //   response.getWriter().println(JsonFormat.printToString(monitorInfo, true));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
