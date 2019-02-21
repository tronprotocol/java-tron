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
public class HttpTestBlock001 {
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
  @Test(enabled = true, description = "Get now block by http")
  public void getNowBlock() {
    response = HttpMethed.getNowBlock(httpnode);
    try {
      entity = response.getEntity();
    } catch (Exception e) {
      e.printStackTrace();
    }
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(),200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() >= 2);
    responseContent = HttpMethed.parseStringContent(responseContent.get("block_header").toString());
    Assert.assertTrue(responseContent.size() >= 2);
    Assert.assertFalse(responseContent.get("witness_signature").toString().isEmpty());
    HttpMethed.printJsonContent(responseContent);
    responseContent = HttpMethed.parseStringContent(responseContent.get("raw_data").toString());
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(Integer.parseInt(responseContent.get("number").toString()) > 0);
    Assert.assertTrue(Long.parseLong(responseContent.get("timestamp").toString()) > 1550724114000L);
    Assert.assertFalse(responseContent.get("witness_address").toString().isEmpty());
  }

}



