package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
import java.util.HashMap;
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
public class HttpTestConstantContract001 {

  private static String contractName;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] assetOwnerAddress = ecKey2.getAddress();
  String assetOwnerKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  String contractAddress;
  Long amount = 2048000000L;
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Deploy constant contract by http")
  public void test1DeployConstantContract() {
    PublicMethed.printAddress(assetOwnerKey);
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.sendCoin(httpnode, fromAddress, assetOwnerAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    String filePath = "src/test/resources/soliditycode/constantContract001.sol";
    contractName = "testConstantContract";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    String txid = HttpMethed
        .deployContractGetTxid(httpnode, contractName, abi, code, 1000000L, 1000000000L, 100,
            11111111111111L, 0L, 0, 0L, assetOwnerAddress, assetOwnerKey);

    HttpMethed.waitToProduceOneBlock(httpnode);
    logger.info(txid);
    response = HttpMethed.getTransactionById(httpnode, txid);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(!responseContent.getString("contract_address").isEmpty());
    contractAddress = responseContent.getString("contract_address");

    response = HttpMethed.getTransactionInfoById(httpnode, txid);
    responseContent = HttpMethed.parseResponseContent(response);
    String receiptString = responseContent.getString("receipt");
    Assert
        .assertEquals(HttpMethed.parseStringContent(receiptString).getString("result"), "SUCCESS");
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get constant contract by http")
  public void test2GetConstantContract() {
    response = HttpMethed.getContract(httpnode, contractAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.getString("consume_user_resource_percent"), "100");
    Assert.assertEquals(responseContent.getString("contract_address"), contractAddress);
    Assert.assertEquals(responseContent.getString("origin_address"),
        ByteArray.toHexString(assetOwnerAddress));
    Assert.assertEquals(responseContent.getString("origin_energy_limit"), "11111111111111");
    Assert.assertEquals(responseContent.getString("name"), contractName);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Trigger constant contract without parameterString by http")
  public void test3TriggerConstantContract() {
    String param1 =
        "000000000000000000000000000000000000000000000000000000000000000" + Integer.toHexString(3);
    String param2 =
        "00000000000000000000000000000000000000000000000000000000000000" + Integer.toHexString(30);
    logger.info(param1);
    logger.info(param2);
    String param = param1 + param2;
    logger.info(ByteArray.toHexString(assetOwnerAddress));
    response = HttpMethed.triggerConstantContract(httpnode, assetOwnerAddress, contractAddress,
        "testPure(uint256,uint256)", param, 1000000000L, assetOwnerKey);
    HttpMethed.waitToProduceOneBlock(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(!responseContent.getString("transaction").isEmpty());
    JSONObject transactionObject = HttpMethed
        .parseStringContent(responseContent.getString("transaction"));
    Assert.assertTrue(!transactionObject.getString("raw_data").isEmpty());
    Assert.assertTrue(!transactionObject.getString("raw_data_hex").isEmpty());
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.freedResource(httpnode, assetOwnerAddress, fromAddress, assetOwnerKey);
    HttpMethed.disConnect();
  }
}