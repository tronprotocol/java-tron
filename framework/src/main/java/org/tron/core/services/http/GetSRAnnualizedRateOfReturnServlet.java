package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

@Component
@Slf4j(topic = "API")
public class GetSRAnnualizedRateOfReturnServlet extends RateLimiterServlet{

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      long value = 0;
      double annualizedRateOfReturn=-1;
      double voteNumber=0;
      byte[] address = Util.getAddress(request);
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      JSONObject jsonObject = JSONObject.parseObject(input);
      long startTimeStamp = Util
          .getJsonLongValue(jsonObject, "startTimeStamp", true);
      long endTimeStamp = Util.getJsonLongValue(jsonObject, "endTimeStamp", true);
      if (startTimeStamp < endTimeStamp && address != null) {
        //should endTimeStamp-startTimeStamp=24hour
        value = wallet
            .queryPayByTimeStamp(address, startTimeStamp, endTimeStamp);
        voteNumber = wallet.queryVoteNumber(address, startTimeStamp, endTimeStamp);
        annualizedRateOfReturn=value*365*100/voteNumber;
      }
      response.getWriter().println("{\"annualizedRateOfReturn\": " + annualizedRateOfReturn + "}");
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
