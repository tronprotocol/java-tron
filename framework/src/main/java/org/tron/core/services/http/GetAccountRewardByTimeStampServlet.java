package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;


@Component
@Slf4j(topic = "API")
public class GetAccountRewardByTimeStampServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      byte[] address = Util.getAddress(request);
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      JSONObject jsonObject = JSONObject.parseObject(input);
      long startTimeStamp = Util
          .getJsonLongValue(jsonObject, "startTimeStamp", true);
      long endTimeStamp = Util.getJsonLongValue(jsonObject, "endTimeStamp", true);
      if (startTimeStamp < endTimeStamp && address != null) {
        HashMap<String, Long> value = wallet
            .computeRewardByTimeStamp(address, startTimeStamp, endTimeStamp);
        response.getWriter().println(Util.printRewardMapToJSON(value));
      } else {
        response.getWriter().println("{}");
      }

    } catch (Exception e) {
      logger.error("", e);
      try {
        response.getWriter().println(Util.printErrorMsg(e));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
    }
  }
  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    doGet(request, response);
  }
}