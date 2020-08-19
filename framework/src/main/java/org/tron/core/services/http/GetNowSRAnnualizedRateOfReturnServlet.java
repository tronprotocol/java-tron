package org.tron.core.services.http;

import static org.tron.core.config.Parameter.ChainConstant.MAX_ACTIVE_WITNESS_NUM;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.DecodeUtil;
import org.tron.core.Wallet;
import org.tron.core.exception.AddressNotFound;
import org.tron.core.exception.InvalidAddress;

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
      if (!DecodeUtil.addressValid(address)) {
        throw new InvalidAddress("Invalid address!");
      }
      long rewardOfVoteEachBlock = wallet.checkStandbyWitness(address)
          ? wallet.getRewardOfVoteEachBlock() / 1000000 : 0;
      long rewardOfBlockEachBlock = wallet.checkAddress(address)
          ? wallet.getRewardOfBlockEachBlock() / 1000000 : 0;
      double srNumber = MAX_ACTIVE_WITNESS_NUM;
      double blockNumberEachDay = wallet.getBlockNumberEachDay();
      double totalVote;
      double srVote;
      double ratio;
      if (!wallet.existAddress(address)) {
        throw new AddressNotFound("address not found!");
      }
      srVote = wallet.queryNowVoteNumber(address);
      totalVote = wallet.queryNowTotalVoteNumber();
      ratio = wallet.queryNowSrRatio(address);
      annualizedRateOfReturn = wallet.getAnnualizedRateOfReturn(rewardOfBlockEachBlock,
          blockNumberEachDay, srNumber, srVote, totalVote, rewardOfVoteEachBlock, ratio);
      response.getWriter().println("{\"annualizedRateOfReturn\": " + annualizedRateOfReturn + "}");
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      byte[] address = Util.getAddress(request);
      if (!DecodeUtil.addressValid(address)) {
        throw new InvalidAddress("Invalid address!");
      }
      double annualizedRateOfReturn = 0;
      long rewardOfVoteEachBlock = wallet.checkStandbyWitness(address)
          ? wallet.getRewardOfVoteEachBlock() / 1000000 : 0;
      long rewardOfBlockEachBlock = wallet.checkAddress(address)
          ? wallet.getRewardOfBlockEachBlock() / 1000000 : 0;
      double srNumber = MAX_ACTIVE_WITNESS_NUM;
      double blockNumberEachDay = wallet.getBlockNumberEachDay();
      double totalVote;
      double srVote;
      double ratio;
      if (!wallet.existAddress(address)) {
        throw new AddressNotFound("address not found!");
      }
      srVote = wallet.queryNowVoteNumber(address);
      totalVote = wallet.queryNowTotalVoteNumber();
      ratio = wallet.queryNowSrRatio(address);
      annualizedRateOfReturn = wallet.getAnnualizedRateOfReturn(rewardOfBlockEachBlock,
          blockNumberEachDay, srNumber, srVote, totalVote, rewardOfVoteEachBlock, ratio);
      response.getWriter().println("{\"annualizedRateOfReturn\": " + annualizedRateOfReturn + "}");
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
