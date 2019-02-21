package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Assert;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class HttpTestAccount001 {
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private HttpClient httpClient = new DefaultHttpClient();
  private JSONObject responseContent;
  private HttpResponse response;
  private HttpEntity entity;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get account by http")
  public void getAccount() {
    response = HttpMethed.getAccount(httpnode,fromAddress);
    try {
      entity = response.getEntity();
    } catch (Exception e) {
      e.printStackTrace();
    }
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(),200);
    responseContent = HttpMethed.parseResponseContent(response);

    for (String str:responseContent.keySet()) {
      logger.info(str + ":" + responseContent.get(str));
    }
    logger.info("contents are" + responseContent.keySet());
    Assert.assertTrue(responseContent.size() > 3);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get accountNet by http")
  public void getAccountNet() {
    response = HttpMethed.getAccountNet(httpnode,fromAddress);
    try {
      entity = response.getEntity();
    } catch (Exception e) {
      e.printStackTrace();
    }
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(),200);
    responseContent = HttpMethed.parseResponseContent(response);

    for (String str:responseContent.keySet()) {
      if (str.equals("freeNetLimit")) {
        Assert.assertEquals(responseContent.get(str),5000);
      }
      if (str.equals("TotalNetLimit")) {
        Assert.assertEquals(responseContent.get(str),43200000000L);
      }
      logger.info(str + ":" + responseContent.get(str));
    }
    Assert.assertTrue(responseContent.size() >= 4);
  }



}


