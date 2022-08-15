package stest.tron.wallet.dailybuild.http;

import static org.hamcrest.core.StringContains.containsString;

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
public class HttpTestClearAbiContract001 {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static String name = "testAssetIssue002_" + now;
  private static String assetIssueId;
  private static String contractName;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] assetOwnerAddress = ecKey2.getAddress();
  String assetOwnerKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  String contractAddress;
  String abi;
  Long amount = 2048000000L;
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf").getString("defaultParameter.assetUrl");
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);
  private String httpSoliditynode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private String httpPbftnode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(4);

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Deploy smart contract by http")
  public void test1DeployContract() {
    PublicMethed.printAddress(assetOwnerKey);
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.sendCoin(httpnode, fromAddress, assetOwnerAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    response = HttpMethed.getAccount(httpnode, assetOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    String filePath = "src/test/resources/soliditycode/TriggerConstant003.sol";
    contractName = "testConstantContract";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    logger.info("abi:" + abi);
    logger.info("code:" + code);

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
  @Test(enabled = true, description = "Get contract by http")
  public void test2GetContract() {
    response = HttpMethed.getContract(httpnode, contractAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.getString("consume_user_resource_percent"), "100");
    Assert.assertEquals(responseContent.getString("contract_address"), contractAddress);
    Assert.assertEquals(responseContent.getString("origin_address"),
        ByteArray.toHexString(assetOwnerAddress));
    Assert.assertThat(responseContent.getString("abi"), containsString("testView"));

    Assert.assertEquals(responseContent.getString("origin_energy_limit"), "11111111111111");
    Assert.assertEquals(responseContent.getString("name"), contractName);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Trigger contract by http")
  public void test3TriggerConstantContract() {

    HttpResponse httpResponse = HttpMethed
        .triggerConstantContract(httpnode, assetOwnerAddress, contractAddress, "testView()", "");

    responseContent = HttpMethed.parseResponseContent(httpResponse);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.getString("result"), "{\"result\":true}");
    Assert.assertEquals(responseContent.getString("constant_result"),
        "[\"0000000000000000000000000000000000000000000000000000000000000001\"]");

    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode,httpSoliditynode);
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode,httpSoliditynode);
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode,httpSoliditynode);
    httpResponse = HttpMethed.triggerConstantContractFromSolidity(httpSoliditynode,
        assetOwnerAddress, contractAddress, "testView()", "");

    responseContent = HttpMethed.parseResponseContent(httpResponse);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.getString("result"), "{\"result\":true}");
    Assert.assertEquals(responseContent.getString("constant_result"),
            "[\"0000000000000000000000000000000000000000000000000000000000000001\"]");

    httpResponse = HttpMethed.triggerConstantContractFromPbft(httpPbftnode, assetOwnerAddress,
        contractAddress, "testView()", "");

    responseContent = HttpMethed.parseResponseContent(httpResponse);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.getString("result"), "{\"result\":true}");
    Assert.assertEquals(responseContent.getString("constant_result"),
            "[\"0000000000000000000000000000000000000000000000000000000000000001\"]");
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Trigger contract by http")
  public void test4ClearAbiContract() {

    HttpResponse httpResponse = HttpMethed
        .clearABiGetTxid(httpnode, assetOwnerAddress, contractAddress, assetOwnerKey);

    responseContent = HttpMethed.parseResponseContent(httpResponse);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.getString("result"), "true");

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get contract by http")
  public void test5GetContract() {
    response = HttpMethed.getContract(httpnode, contractAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.getString("consume_user_resource_percent"), "100");
    Assert.assertEquals(responseContent.getString("contract_address"), contractAddress);
    Assert.assertEquals(responseContent.getString("origin_address"),
        ByteArray.toHexString(assetOwnerAddress));
    Assert.assertEquals(responseContent.getString("abi"), "{}");
    Assert.assertEquals(responseContent.getString("origin_energy_limit"), "11111111111111");
    Assert.assertEquals(responseContent.getString("name"), contractName);
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
