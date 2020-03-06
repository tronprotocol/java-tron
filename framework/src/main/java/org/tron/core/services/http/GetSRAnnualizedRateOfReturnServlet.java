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
      double annualizedRateOfReturn=0;
      byte[] address = Util.getAddress(request);
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      JSONObject jsonObject = JSONObject.parseObject(input);
      long startTimeStamp = Util
          .getJsonLongValue(jsonObject, "startTimeStamp", true);
      long endTimeStamp = Util.getJsonLongValue(jsonObject, "endTimeStamp", true);

      int rewardOfVoteEachBlock = 160;
      int rewardOfBlockEachBlock = 16;
      int srNumber = 27;
      int blockNumberEachDay = 28792;
      double totalVote;
      double srVote;
      double ratio;
      if (startTimeStamp < endTimeStamp && address != null) {
        srVote = wallet.queryVoteNumber(address, startTimeStamp, endTimeStamp);
        totalVote = wallet.queryTotalVoteNumber(startTimeStamp, endTimeStamp);
        ratio = wallet.querySrRatio(address, startTimeStamp, endTimeStamp);
        if (totalVote < srVote || totalVote <= 0 || srVote <= 0 || ratio > 1 || ratio < 0) {
          throw new Exception("bad parameters");
        }
        annualizedRateOfReturn=(rewardOfBlockEachBlock/srNumber/srVote+rewardOfVoteEachBlock/totalVote)*blockNumberEachDay*ratio*365;
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
