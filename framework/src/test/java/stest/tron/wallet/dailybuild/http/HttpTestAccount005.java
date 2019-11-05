package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Utils;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class HttpTestAccount005 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] toAddress = ecKey1.getAddress();
  String toAddressKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  Long amount = 1L;
  String sendText = "Decentralize the WEB!";
  private JSONObject responseContent;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Test transfer with notes by http")
  public void test01TransferWithNotes() {
    PublicMethed.printAddress(toAddressKey);
    //Send trx to test account
    String txid = HttpMethed
        .sendCoin(httpnode, fromAddress, toAddress, amount, sendText, testKey002);
    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpResponse response = HttpMethed.getTransactionById(httpnode, txid);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    String rawData = responseContent.getString("raw_data");
    JSONObject rawDataObject = JSON.parseObject(rawData);
    Assert.assertTrue(rawDataObject.containsKey("data"));
    String hexData = rawDataObject.getString("data");
    String recoveredString = new String(ByteUtil.hexToBytes(hexData));
    Assert.assertEquals(sendText, recoveredString);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.freedResource(httpnode, toAddress, fromAddress, toAddressKey);
    HttpMethed.disConnect();
  }
}
