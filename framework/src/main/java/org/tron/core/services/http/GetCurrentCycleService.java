package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.db.Manager;

@Component
@Slf4j(topic = "API")
public class GetCurrentCycleService extends RateLimiterServlet {
  @Autowired
  private Manager manager;
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      int value = 0;
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      JSONObject jsonObject = JSONObject.parseObject(input);
      long startTimeStamp = Util
          .getJsonLongValue(jsonObject, "timeStamp", true);

      long cycle = manager.getDelegationService().getCycleFromTimeStamp(startTimeStamp);
      response.getWriter().println("{\"cycle\": " + cycle + "}");
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    doGet(request, response);
  }
}