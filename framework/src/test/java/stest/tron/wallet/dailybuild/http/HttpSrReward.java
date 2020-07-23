package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class HttpSrReward {

  private final String foundationAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddressByte = PublicMethed.getFinalAddress(foundationAccountKey);
  private final String srKey = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final String srAddress = PublicMethed.getAddressString(srKey);
  Long totalReward = 0L;
  Integer cycle = 0;
  Long amount = 50000000L;
  Long frozenAmount = amount / 2;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  String voteAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private final String voteAccountAddress = PublicMethed.getAddressString(voteAccountKey);
  private final byte[] voteAccountAddressByte = PublicMethed.getFinalAddress(voteAccountKey);
  JsonArray voteKeys = new JsonArray();
  JsonObject voteElement = new JsonObject();
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get now current cycles")
  public void test01GetnowCurrentCycles() {
    Integer retryTime = 100;
    while (cycle < 3 && retryTime-- > 0) {
      response = HttpMethed.getCurrentCycle(httpnode);
      Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
      responseContent = HttpMethed.parseResponseContent(response);
      HttpMethed.printJsonContent(responseContent);
      cycle = responseContent.getInteger("cycle");
      HttpMethed.waitToProduceOneBlock(httpnode);
    }
    Assert.assertTrue(cycle >= 3);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get account reward by cycles")
  public void test02GetAccountReward() throws Exception {
    response = HttpMethed.sendCoin(httpnode, foundationAddressByte, voteAccountAddressByte, amount,
        foundationAccountKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    //Freeze balance
    response = HttpMethed
        .freezeBalance(httpnode, voteAccountAddressByte, frozenAmount, 0,
            0, voteAccountKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    voteElement.addProperty("vote_address", srAddress);
    voteElement.addProperty("vote_count", frozenAmount / 1000000L);
    voteKeys.add(voteElement);
    response = HttpMethed
        .voteWitnessAccount(httpnode, voteAccountAddress, voteKeys, voteAccountKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    Integer retryTime = 50;
    while (totalReward == 0 && retryTime-- > 0) {
      HttpMethed.waitToProduceOneBlock(httpnode);
      response = HttpMethed
          .getAccountRewardByCycle(httpnode, voteAccountAddress, cycle, cycle + 3);
      responseContent = HttpMethed.parseResponseContent(response);
      HttpMethed.printJsonContent(responseContent);
      totalReward = responseContent.getLong("totalReward");
    }
    Assert.assertTrue(totalReward > 0);
    System.out.println("totalReward:" + totalReward);
    System.out.println("responseContent.getLong(srAddress):" + responseContent.getLong(srAddress));
    //Assert.assertTrue(totalReward == responseContent.getLong(srAddress));
    Assert.assertEquals(totalReward, responseContent.getLong(srAddress));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get account last unwithdraw reward")
  public void test03GetAccountLastUnwithdrawReward() throws Exception {
    response = HttpMethed
        .getAccountLastUnwithdrawReward(httpnode, voteAccountAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Long unWithdrawReward = responseContent.getLong("totalReward");
    logger.info("unWithdrawReward:" + unWithdrawReward);
    Assert.assertTrue(unWithdrawReward >= totalReward);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get SR profit by cycle")
  public void test04GetSrProfitByCycle() throws Exception {
    response = HttpMethed
        .getSrProfitByCycle(httpnode, srAddress, cycle - 1, cycle + 2);
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
  @Test(enabled = true, description = "Get SR dividends by cycle")
  public void test05GetSrDividendsByCycle() throws Exception {
    response = HttpMethed
        .getSrDividendsByCycle(httpnode, srAddress, cycle - 1, cycle + 2);
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
  @Test(enabled = true, description = "Get now SR annualized rate")
  public void test06GetNowSrAnnualizedRate() throws Exception {
    response = HttpMethed.getNowSrAnnualizedRate(httpnode, srAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getDouble("annualizedRateOfReturn") > 0);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.unFreezeBalance(httpnode, voteAccountAddressByte, 0, voteAccountKey);
    HttpMethed.disConnect();
  }
}