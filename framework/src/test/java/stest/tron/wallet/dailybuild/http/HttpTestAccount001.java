package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class HttpTestAccount001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);
  private String httpSoliditynode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private String httpPbftNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(4);

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get account by http")
  public void getAccount() {
    response = HttpMethed.getAccount(httpnode, fromAddress);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() > 3);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get account from solidity by http")
  public void getAccountFromSolidity() {
    response = HttpMethed.getAccountFromSolidity(httpSoliditynode, fromAddress);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() > 3);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get account from PBFT by http")
  public void getAccountFromPbftNode() {
    response = HttpMethed.getAccountFromPbft(httpPbftNode, fromAddress);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() > 3);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get accountNet by http")
  public void getAccountNet() {
    response = HttpMethed.getAccountNet(httpnode, fromAddress);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(Integer.parseInt(responseContent.get("freeNetLimit").toString()), 5000);
    Assert.assertEquals(Long.parseLong(responseContent.get("TotalNetLimit").toString()),
        43200000000L);
    Assert.assertTrue(responseContent.size() >= 2);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get accountResource by http")
  public void getAccountResource() {
    response = HttpMethed.getAccountReource(httpnode, fromAddress);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(
        Long.parseLong(responseContent.get("TotalEnergyLimit").toString()) >= 50000000000L);
    Assert.assertTrue(responseContent.size() >= 3);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }
}