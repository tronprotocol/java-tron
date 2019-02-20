package stest.tron.wallet.http;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Assert;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
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
  @BeforeClass
  public void beforeClass() {
  }

  /**
   * constructor.
   */
  @Test
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
      System.out.println(str + ":" + responseContent.get(str));
    }
    System.out.println(responseContent.keySet());
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
  }
}


