package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONArray;
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
public class HttpTestSendCoin001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] receiverAddress = ecKey1.getAddress();
  String receiverKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  Long amount = 1000L;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(1);
  private String httpSoliditynode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private String httpPbftNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(4);
  private JSONObject responseContent;
  private HttpResponse response;

  /**
   * constructor.
   */
  @Test(enabled = true, description = "SendCoin by http")
  public void test1SendCoin() {
    response = HttpMethed.sendCoin(httpnode, fromAddress, receiverAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    Assert.assertEquals(HttpMethed.getBalance(httpnode, receiverAddress), amount);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction by id from solidity by http")
  public void test2GetTransactionByIdFromSolidity() {
    String txid = HttpMethed
        .sendCoinGetTxid(httpnode, fromAddress, receiverAddress, amount, testKey002);
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);

    response = HttpMethed.getTransactionByIdFromSolidity(httpSoliditynode, txid);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    String retString = responseContent.getString("ret");
    JSONArray array = JSONArray.parseArray(retString);
    Assert.assertEquals(
        HttpMethed.parseStringContent(array.get(0).toString()).getString("contractRet"), "SUCCESS");
    Assert.assertTrue(responseContent.size() > 4);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction by id from PBFT by http")
  public void test3GetTransactionByIdFromPbft() {
    String txid = HttpMethed
        .sendCoinGetTxid(httpnode, fromAddress, receiverAddress, amount, testKey002);
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);

    response = HttpMethed.getTransactionByIdFromPbft(httpPbftNode, txid);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    String retString = responseContent.getString("ret");
    JSONArray array = JSONArray.parseArray(retString);
    Assert.assertEquals(
        HttpMethed.parseStringContent(array.get(0).toString()).getString("contractRet"), "SUCCESS");
    Assert.assertTrue(responseContent.size() > 4);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction info by id from solidity by http")
  public void test4GetTransactionInfoByIdFromSolidity() {
    String txid = HttpMethed
        .sendCoinGetTxid(httpnode, fromAddress, receiverAddress, amount, testKey002);
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethed.getTransactionInfoByIdFromSolidity(httpSoliditynode, txid);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() > 4);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction info by id from PBFT by http")
  public void test5GetTransactionInfoByIdFromPbft() {
    String txid = HttpMethed
        .sendCoinGetTxid(httpnode, fromAddress, receiverAddress, amount, testKey002);
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethed.getTransactionInfoByIdFromPbft(httpPbftNode, txid);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() > 4);
  }


  /**
   * constructor.
   */
  @Test(enabled = false, description = "Get transactions from this from solidity by http")
  public void test4GetTransactionsFromThisFromSolidity() {
    response = HttpMethed
        .getTransactionsFromThisFromSolidity(httpSoliditynode, fromAddress, 0, 100);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONObject transactionObject = HttpMethed.parseStringContent(
        JSONArray.parseArray(responseContent.getString("transaction")).get(0).toString());
    String retString = transactionObject.getString("ret");
    JSONArray array = JSONArray.parseArray(retString);
    Assert.assertEquals(
        HttpMethed.parseStringContent(array.get(0).toString()).getString("contractRet"), "SUCCESS");
    Assert.assertTrue(responseContent.size() == 1);
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Get transactions to this from solidity by http")
  public void test5GetTransactionsToThisFromSolidity() {
    response = HttpMethed
        .getTransactionsFromThisFromSolidity(httpSoliditynode, fromAddress, 0, 100);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONObject transactionObject = HttpMethed.parseStringContent(
        JSONArray.parseArray(responseContent.getString("transaction")).get(0).toString());
    String retString = transactionObject.getString("ret");
    JSONArray array = JSONArray.parseArray(retString);
    Assert.assertEquals(
        HttpMethed.parseStringContent(array.get(0).toString()).getString("contractRet"), "SUCCESS");
    Assert.assertTrue(responseContent.size() == 1);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.freedResource(httpnode, receiverAddress, fromAddress, receiverKey);
    HttpMethed.disConnect();
  }

}
