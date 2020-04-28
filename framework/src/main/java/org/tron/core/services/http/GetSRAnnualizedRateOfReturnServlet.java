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

@Component
@Slf4j(topic = "API")
public class GetSRAnnualizedRateOfReturnServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      double annualizedRateOfReturn = 0;
      byte[] address = Util.getAddress(request);
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      JSONObject jsonObject = JSONObject.parseObject(input);
      long startTimeStamp = Util
          .getJsonLongValue(jsonObject, "startTimeStamp", true);
      long endTimeStamp = Util.getJsonLongValue(jsonObject, "endTimeStamp", true);


      long rewardOfVoteEachBlock = wallet.getRewardOfVoteEachBlock() / 1000000;
      long rewardOfBlockEachBlock = wallet.getRewardOfBlockEachBlock() / 1000000;
      int srNumber = 27;
      int blockNumberEachDay = 28792;

      double totalVote;
      double srVote;
      double ratio;
      if (startTimeStamp < endTimeStamp && address != null) {
        srVote = wallet.queryVoteNumber(address, startTimeStamp, endTimeStamp);
        totalVote = wallet.queryTotalVoteNumber(startTimeStamp, endTimeStamp);
        ratio = wallet.querySrRatio(address, startTimeStamp, endTimeStamp);
        if (totalVote < srVote || totalVote <= 0 || srVote <= 0 || ratio > 100 || ratio < 0) {
          throw new Exception("bad parameters");
        }
        //debug
        logger.info("getRewardOfVoteEachBlock: {}, getRewardOfBlockEachBlock: {}, getSrNumber: {},",
            rewardOfVoteEachBlock,rewardOfBlockEachBlock,srNumber);
        logger.info("totalVote: {}, srVote: {}, ratio: {},",
            totalVote,srVote,ratio);
        annualizedRateOfReturn = (rewardOfBlockEachBlock / srNumber / srVote
            + rewardOfVoteEachBlock / totalVote) * blockNumberEachDay * ratio * 365;
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

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    doGet(request, response);
  }
}
