package stest.tron.wallet.onlinestress;

import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.tron.core.config.Parameter.ChainConstant.FROZEN_PERIOD;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.grpc.ManagedChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.HttpMethed;

@Slf4j
public class TestSrRewardOnlineData {

  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String httpnode = "47.245.3.27:8090";
  private JSONObject responseContent;
  private HttpResponse response;
  Integer cycle = 0;
  String srAddress = "TLyqzVGLV1srkB7dToTAEqgDSfPtXRJZYH";

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }


  @Test(enabled = true)
  public void test01GetAccountReward() {
    response = HttpMethed.getCurrentCycle(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    cycle = responseContent.getInteger("cycle");

    response = HttpMethed
        .getAccountRewardByCycle(httpnode, "TWjvFoH2HgkNCsf897tG5BSzx7ZpfkqHPs",
            cycle - 20, cycle + 1);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    Long total = 0L;
    Long totalReward = 0L;
    for (Map.Entry<String, Object> entry : responseContent.entrySet()) {
      if (entry.getKey() == "totalReward") {
        totalReward = Long.valueOf(String.valueOf(entry.getValue()));
        continue;
      }
      total = total + Long.valueOf(String.valueOf(entry.getValue()));
    }

    logger.info("total      :" + total);
    logger.info("totalReward:" + totalReward);
  }


  @Test(enabled = true)
  public void test02GetLastUnwithdrawAccountReward() {
    response = HttpMethed
        .getAccountLastUnwithdrawReward(httpnode, "TWjvFoH2HgkNCsf897tG5BSzx7ZpfkqHPs");
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Long unWithdrawReward = responseContent.getLong("totalReward");
    logger.info("unWithdrawReward:" + unWithdrawReward);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get SR profit by cycle")
  public void test03GetSrProfitByCycle() throws Exception {
    response = HttpMethed
        .getSrProfitByCycle(httpnode, srAddress, 0, cycle + 2);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Long total = responseContent.getLong("total");
    Long produceBlock = responseContent.getLong("produceBlock");
    Long vote = responseContent.getLong("vote");
    Assert.assertTrue(total == produceBlock + vote);

  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get SR dividends by cycle")
  public void test04GetSrDividendsByCycle() throws Exception {
    response = HttpMethed
        .getSrDividendsByCycle(httpnode, srAddress, 0, cycle + 2);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Long total = responseContent.getLong("total");
    Long produceBlock = responseContent.getLong("produceBlock");
    Long vote = responseContent.getLong("vote");
    Assert.assertTrue(total == produceBlock + vote);
    //Assert.assertTrue(total > 0 && produceBlock > 0 && vote > 0);
    //410BE88A918D74D0DFD71DC84BD4ABF036D0562991
    //TB4B1RMhoPeivkj4Hebm6tttHjRY9yQFes
    //{
    //"address": "410BE88A918D74D0DFD71DC84BD4ABF036D0562991",
    //"visible": false,
    //"startCycle": "0",
    // "endCycle": "888"
    //}
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get now SR annualized rate")
  public void test05GetNowSrAnnualizedRate() throws Exception {
    response = HttpMethed.getNowSrAnnualizedRate(httpnode, srAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getDouble("annualizedRateOfReturn") > 0);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get now SR annualized rate")
  public void test06GetNowSrAnnualizedRate() throws Exception {
    response = HttpMethed.listwitnesses(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONArray wintessArray = responseContent.getJSONArray("witnesses");
    List<Long> voteList = new ArrayList<>();
    HashMap<String, Long> witness127 = new HashMap<>();

    for (int i = 0; i < wintessArray.size(); i++) {
      if (wintessArray.getJSONObject(i).containsKey("voteCount")) {
        voteList.add(wintessArray.getJSONObject(i).getLong("voteCount"));
        witness127.put(wintessArray.getJSONObject(i).getString("address"),
            wintessArray.getJSONObject(i).getLong("voteCount"));
      }
    }

    Long totalVote = 0L;
    Long voteFor127 = 0L;

    for (int j = 0; j < voteList.size(); j++) {
      totalVote = totalVote + voteList.get(j);
      if (j < 127) {
        voteFor127 = voteFor127 + voteList.get(j);
      }
    }

    logger.info("totalVote :" + totalVote);
    logger.info("voteFor127:" + voteFor127);

    double blockNumberEachDay = FROZEN_PERIOD / BLOCK_PRODUCED_INTERVAL
        - 2 * (FROZEN_PERIOD / 21600000L);

    List<String> result = new ArrayList<>();

    for (String key : witness127.keySet()) {
      Long srVote = witness127.get(key);
      String witnessAddress = key;
      response = HttpMethed.getNowSrAnnualizedRate(httpnode, witnessAddress);
      responseContent = HttpMethed.parseResponseContent(response);
      double annualizedRateOfReturn = Double.valueOf(responseContent
          .getString("annualizedRateOfReturn"));
      if (annualizedRateOfReturn > 0) {
        result.add(
            witnessAddress + " annualizedRateOfReturn is:"
                + responseContent.getString("annualizedRateOfReturn"));
      }
    }

    for (int i = 0; i < result.size(); i++) {
      logger.info(result.get(i));
    }

    logger.info(result.size() + "");



    /*    double annualizedRateOfReturn = ((double) 16 / (double) 27 / (double)srVote
        + (double)160 / (double)voteFor127) * (double)blockNumberEachDay
        * (double)80 * (double)365;
    logger.info("voteFor127 annu is:" + annualizedRateOfReturn);*/
  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }


}



