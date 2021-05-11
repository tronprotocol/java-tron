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
public class HttpTestTransactionPending001 {

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
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] receiverAddress = ecKey1.getAddress();
  String receiverKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  String txid;
  JSONObject transaction;

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction pending size by http")
  public void test01GetTransactionPendingSize() {
    int pendingSize = 0;
    int retryTimes = 50;

    while (pendingSize == 0 && retryTimes-- > 0) {
      HttpMethed.sendCoin(httpnode,fromAddress,receiverAddress,1L,testKey002);
      if (retryTimes % 5 == 0) {
        pendingSize = HttpMethed.getTransactionPendingSize(httpnode);
      }
    }

    Assert.assertNotEquals(pendingSize,0);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get pending transaction list by http")
  public void test02GetPendingTransactionList() {
    int transactionSize = 0;
    int retryTimes = 50;

    while (transactionSize == 0 && retryTimes-- > 0) {
      HttpMethed.sendCoin(httpnode,fromAddress,receiverAddress,1L,testKey002);
      if (retryTimes % 5 == 0) {
        response = HttpMethed.getTransactionListFromPending(httpnode);
        responseContent = HttpMethed.parseResponseContent(response);
        transactionSize = responseContent.getJSONArray("txId").size();
      }
    }
    Assert.assertNotEquals(transactionSize,0);

    txid = responseContent.getJSONArray("txId").getString(0);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction from pending by http")
  public void test03GetPendingTransactionList() {
    response = HttpMethed.getTransactionFromPending(httpnode,txid);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    Assert.assertEquals(txid,responseContent.getString("txID"));
    Assert.assertNotEquals(null,responseContent);
  }




  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }
}