package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
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
      long startCycle = jsonObject.getLong("startCycle");
      long endCycle = jsonObject.getLong("endCycle");
      long currentCycle = wallet.queryCurrentCycle();;
      long rewardOfVoteEachBlock = wallet.getRewardOfVoteEachBlock() / 1000000;
      long rewardOfBlockEachBlock = wallet.getRewardOfBlockEachBlock() / 1000000;
      double srNumber = 27;
      double blockNumberEachDay = wallet.getBlockNumberEachDay();
      double totalVote;
      double srVote;
      double ratio;
      if (startCycle <= endCycle && address != null) {
        if (endCycle > currentCycle) {
          endCycle = currentCycle;
        }
        srVote = wallet.queryVoteNumber(address, startCycle, endCycle);
        totalVote = wallet.queryTotalVoteNumber(startCycle, endCycle);
        ratio = wallet.querySrRatio(address, startCycle, endCycle);
        annualizedRateOfReturn = wallet.getAnnualizedRateOfReturn(rewardOfBlockEachBlock,
            blockNumberEachDay, srNumber,srVote,totalVote,rewardOfVoteEachBlock,ratio);
      }
      response.getWriter().println("{\"annualizedRateOfReturn\": " + annualizedRateOfReturn + "}");
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      Account.Builder build = Account.newBuilder();
      JsonFormat.merge(params.getParams(), build, params.isVisible());
      JSONObject jsonObject = JSONObject.parseObject(params.getParams());
      double annualizedRateOfReturn = 0;
      byte[] address = build.getAddress().toByteArray();
      long startCycle = jsonObject.getLong("startCycle");
      long endCycle = jsonObject.getLong("endCycle");
      long currentCycle = wallet.queryCurrentCycle();;
      long rewardOfVoteEachBlock = wallet.getRewardOfVoteEachBlock() / 1000000;
      long rewardOfBlockEachBlock = wallet.getRewardOfBlockEachBlock() / 1000000;
      double srNumber = 27;
      double blockNumberEachDay = wallet.getBlockNumberEachDay();
      double totalVote;
      double srVote;
      double ratio;

      if (startCycle <= endCycle && address != null) {
        if (endCycle > currentCycle) {
          endCycle = currentCycle;
        }
        srVote = wallet.queryVoteNumber(address, startCycle, endCycle);
        totalVote = wallet.queryTotalVoteNumber(startCycle, endCycle);
        ratio = wallet.querySrRatio(address, startCycle, endCycle);
        annualizedRateOfReturn = wallet.getAnnualizedRateOfReturn(rewardOfBlockEachBlock,
            blockNumberEachDay, srNumber,srVote,totalVote,rewardOfVoteEachBlock,ratio);
      }
      response.getWriter().println("{\"annualizedRateOfReturn\": " + annualizedRateOfReturn + "}");
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
