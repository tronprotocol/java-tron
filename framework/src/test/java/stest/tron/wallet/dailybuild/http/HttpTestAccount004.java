package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
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
public class HttpTestAccount004 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] setAccountIdAddress = ecKey1.getAddress();
  String setAccountIdKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  Long amount = 10000000L;
  String accountId;
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
  @Test(enabled = true, description = "Set account by http")
  public void test1setAccountId() {
    response = HttpMethed.sendCoin(httpnode, fromAddress, setAccountIdAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    response = HttpMethed
        .setAccountId(httpnode, setAccountIdAddress, System.currentTimeMillis() + "id", false,
            setAccountIdKey);
    Assert.assertFalse(HttpMethed.verificationResult(response));

    //Set account id.
    accountId = System.currentTimeMillis() + "id";
    response = HttpMethed
        .setAccountId(httpnode, setAccountIdAddress, accountId, true, setAccountIdKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get account by id via http")
  public void test2getAccountId() {
    response = HttpMethed.getAccountById(httpnode, accountId, true);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.get("account_id"), accountId);
    Assert.assertTrue(responseContent.size() >= 10);

    response = HttpMethed.getAccountById(httpnode, accountId, false);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() <= 1);


  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get account by id via http")
  public void test3getAccountIdFromSolidity() {
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethed.getAccountByIdFromSolidity(httpSoliditynode, accountId, true);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.get("account_id"), accountId);
    Assert.assertTrue(responseContent.size() >= 10);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get account by id via PBFT http")
  public void test4getAccountIdFromPbft() {
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethed.getAccountByIdFromPbft(httpPbftNode, accountId, true);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.get("account_id"), accountId);
    Assert.assertTrue(responseContent.size() >= 10);
  }


  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.freedResource(httpnode, setAccountIdAddress, fromAddress, setAccountIdKey);
    HttpMethed.disConnect();
  }
}