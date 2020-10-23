package stest.tron.wallet.dailybuild.http;

import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.tron.core.config.Parameter.ChainConstant.FROZEN_PERIOD;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class HttpTestQueryReward001 {

  private final String foundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);
  private final String foundationAddressString = Base58.encode58Check(PublicMethed
      .getFinalAddress(foundationKey));
  private final String witnessKey = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress = PublicMethed.getFinalAddress(witnessKey);
  private final String witnessAddressString = Base58.encode58Check(PublicMethed
      .getFinalAddress(witnessKey));
  Integer cycle = 0;
  private JSONObject responseContent;
  private JSONObject accountReward;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list")
      .get(0);

  /**
   * constructor.
   */
  @BeforeSuite(enabled = false)
  public void beforeSuite() {
    HttpMethed.sendCoin(httpnode, foundationAddress, witnessAddress, 50000000L, foundationKey);
    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.freezeBalance(httpnode, witnessAddress, 30000000L, 3, 0, witnessKey);
    HttpMethed.waitToProduceOneBlock(httpnode);
    JsonArray voteKeys = new JsonArray();
    JsonObject voteElement = new JsonObject();
    voteElement.addProperty("vote_address", Base58.encode58Check(PublicMethed
        .getFinalAddress(witnessKey)));
    voteElement.addProperty("vote_count", 20);
    voteKeys.add(voteElement);
    HttpMethed.voteWitnessAccount(httpnode, Base58.encode58Check(PublicMethed
        .getFinalAddress(witnessKey)), voteKeys, witnessKey);

  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Get account reward")
  public void test01GetAccountReward() {
    response = HttpMethed.getCurrentCycle(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    cycle = responseContent.getInteger("cycle");
    while (cycle < 2) {
      response = HttpMethed.getCurrentCycle(httpnode);
      responseContent = HttpMethed.parseResponseContent(response);
      cycle = responseContent.getInteger("cycle");
      HttpMethed.waitToProduceOneBlock(httpnode);
      HttpMethed.waitToProduceOneBlock(httpnode);
    }

    response = HttpMethed
        .getAccountRewardByCycle(httpnode, witnessAddressString,
            0, cycle + 1);
    accountReward = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(accountReward);
    Assert.assertTrue(accountReward.getLong("totalReward") > 0);
    Assert.assertEquals(accountReward.getLong(witnessAddressString),
        accountReward.getLong("totalReward"));

  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Get llast unwithdraw account reward")
  public void test02GetLastUnwithdrawAccountReward() {
    response = HttpMethed
        .getAccountLastUnwithdrawReward(httpnode, witnessAddressString);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getLong("totalReward") > 0);
    Assert.assertEquals(responseContent.getLong(witnessAddressString),
        responseContent.getLong("totalReward"));
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Get SR profit by cycle")
  public void test03GetSrProfitByCycle() throws Exception {
    response = HttpMethed
        .getSrProfitByCycle(httpnode, witnessAddressString, 0, cycle + 2);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Long total = responseContent.getLong("total");
    Long produceBlock = responseContent.getLong("produceBlock");
    Long vote = responseContent.getLong("vote");
    Assert.assertTrue(total == produceBlock + vote);
    Assert.assertTrue(total > 0);
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Get SR dividends by cycle")
  public void test04GetSrDividendsByCycle() throws Exception {
    response = HttpMethed
        .getSrDividendsByCycle(httpnode, witnessAddressString, 0, cycle + 2);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Long total = responseContent.getLong("total");
    Long produceBlock = responseContent.getLong("produceBlock");
    Long vote = responseContent.getLong("vote");
    Assert.assertTrue(total == produceBlock + vote);
    Assert.assertTrue(total > 0 && produceBlock > 0 && vote > 0);
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Get now SR annualized rate")
  public void test05GetNowSrAnnualizedRate() throws Exception {
    response = HttpMethed.getNowSrAnnualizedRate(httpnode, witnessAddressString);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getDouble("annualizedRateOfReturn") > 0);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }
}
