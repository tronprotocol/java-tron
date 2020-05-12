package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;

@Component
@Slf4j(topic = "API")
public class GetSRPayByTimeStampServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      long value = 0;
      byte[] address = Util.getAddress(request);
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      JSONObject jsonObject = JSONObject.parseObject(input);
      long startTimeStamp = Util
          .getJsonLongValue(jsonObject, "startTimeStamp", true);
      long endTimeStamp = Util.getJsonLongValue(jsonObject, "endTimeStamp", true);
      if (startTimeStamp < endTimeStamp && address != null) {
        value = wallet
            .queryPayByTimeStamp(address, startTimeStamp, endTimeStamp);

      }
      response.getWriter().println("{\"reward\": " + value + "}");
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
    try {
      long value = 0;
      PostParams params = PostParams.getPostParams(request);
      Account.Builder build = Account.newBuilder();
      JsonFormat.merge(params.getParams(), build, params.isVisible());
      JSONObject jsonObject = JSONObject.parseObject(params.getParams());
      long startTimeStamp = jsonObject.getLong("startTimeStamp");
      long endTimeStamp = jsonObject.getLong("endTimeStamp");
      byte[] address = build.getAddress().toByteArray();
      if (startTimeStamp < endTimeStamp && address != null) {
        value = wallet
            .queryPayByTimeStamp(address, startTimeStamp, endTimeStamp);
      }
      response.getWriter().println("{\"reward\": " + value + "}");
    } catch (Exception e) {
      logger.error("", e);
      try {
        response.getWriter().println(Util.printErrorMsg(e));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
    }
  }
}
