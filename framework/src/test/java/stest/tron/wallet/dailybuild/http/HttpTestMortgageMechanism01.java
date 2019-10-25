package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class HttpTestMortgageMechanism01 {

  private static final long now = System.currentTimeMillis();
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String witnessKey = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress = PublicMethed.getFinalAddress(witnessKey);
  Long amount = 2048000000L;
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(1);
  private String httpSoliditynode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetBrokerage by http")
  public void test01GetBrokerage() {
    response = HttpMethed.getBrokerage(httpnode, witnessAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals("20", responseContent.getString("brokerage"));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetBrokerage from solidity by http")
  public void test02GetBrokerageFromSolidity() {
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethed.getBrokerageFromSolidity(httpSoliditynode, witnessAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals("20", responseContent.getString("brokerage"));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UpdateBrokerage by http")
  public void test03UpdateBrokerage() {
    response = HttpMethed.sendCoin(httpnode, fromAddress, witnessAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    //update brokerage
    response = HttpMethed.updateBrokerage(httpnode, witnessAddress, 30L, witnessKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetReward by http")
  public void test04GetReward() {
    response = HttpMethed.getReward(httpnode, witnessAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue((
        new BigInteger(responseContent.getString("reward")).compareTo(new BigInteger("0")) == 0)
        || (new BigInteger(responseContent.getString("reward"))
        .compareTo(new BigInteger("0"))) == 1);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetReward from solidity by http")
  public void test05GetRewardFromSolidity() {
    response = HttpMethed.getRewardFromSolidity(httpSoliditynode, witnessAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue((
        new BigInteger(responseContent.getString("reward")).compareTo(new BigInteger("0")) == 0)
        || (new BigInteger(responseContent.getString("reward"))
        .compareTo(new BigInteger("0"))) == 1);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.freedResource(httpnode, witnessAddress, fromAddress, witnessKey);
    HttpMethed.disConnect();
  }
}
