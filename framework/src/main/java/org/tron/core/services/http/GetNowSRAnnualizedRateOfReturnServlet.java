
package org.tron.core.services.http;

import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.tron.core.config.Parameter.ChainConstant.FROZEN_PERIOD;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.Wallet;

@Component
@Slf4j(topic = "API")
public class GetNowSRAnnualizedRateOfReturnServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      double annualizedRateOfReturn = 0;
      byte[] address = Util.getAddress(request);

      long rewardOfVoteEachBlock = wallet.getRewardOfVoteEachBlock() / 1000000;
      long rewardOfBlockEachBlock = wallet.getRewardOfBlockEachBlock() / 1000000;
      double srNumber = 27;
      long maintenanceTimeInterval = CommonParameter.getInstance().getMaintenanceTimeInterval();
      if (maintenanceTimeInterval == 0) {
        maintenanceTimeInterval = 21600000L;
      }
      double blockNumberEachDay = FROZEN_PERIOD / BLOCK_PRODUCED_INTERVAL
          - 2 * (FROZEN_PERIOD / maintenanceTimeInterval);

      double totalVote;
      double srVote;
      double ratio;
      srVote = wallet.queryNowVoteNumber(address);
      totalVote = wallet.queryNowTotalVoteNumber();
      ratio = wallet.queryNowSrRatio(address);
      //debug
      logger.debug("getRewardOfVoteEachBlock: {}, getRewardOfBlockEachBlock: {}, getSrNumber: {},",
          rewardOfVoteEachBlock,rewardOfBlockEachBlock,srNumber);
      logger.info("totalVoteNow: {}, srVoteNow: {}, ratioNow: {},",
          totalVote,srVote,ratio);

      if (totalVote < srVote || totalVote <= 0 || srVote <= 0 || ratio > 100 || ratio < 0) {
        throw new Exception("bad parameters");
      }
      annualizedRateOfReturn = (rewardOfBlockEachBlock / srNumber / srVote
          + rewardOfVoteEachBlock / totalVote) * blockNumberEachDay * ratio * 365;

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
