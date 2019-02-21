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
public class HttpTestAccount002 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Get account by http")
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
  @Test(enabled = false, description = "Get accountNet by http")
  public void getAccountNet() {
    response = HttpMethed.getAccountNet(httpnode, fromAddress);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    for (String str : responseContent.keySet()) {
      if (str.equals("freeNetLimit")) {
        Assert.assertEquals(responseContent.get(str), 5000);
      }
      if (str.equals("TotalNetLimit")) {
        Assert.assertEquals(responseContent.get(str), 43200000000L);
      }
    }
    Assert.assertTrue(responseContent.size() >= 3);
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Get accountResource by http")
  public void getAccountResource() {
    response = HttpMethed.getAccountReource(httpnode, fromAddress);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    responseContent.get("TotalEnergyLimit");
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
