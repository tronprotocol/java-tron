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
public class HttpTestSmartContract001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] assetOwnerAddress = ecKey2.getAddress();
  String assetOwnerKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] assetReceiverAddress = ecKey3.getAddress();
  String assetReceiverKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());


  String contractAddress;

  Long amount = 2048000000L;

  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  private static final long now = System.currentTimeMillis();
  private static String name = "testAssetIssue002_" + Long.toString(now);
  private static final long totalSupply = now;
  private static String assetIssueId;
  private static String contractName;


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Deploy smart contract by http")
  public void test1DeployContract() {
    PublicMethed.printAddress(assetOwnerKey);
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.sendCoin(httpnode, fromAddress, assetOwnerAddress, amount, testKey002);
    response = HttpMethed.sendCoin(httpnode, fromAddress, assetReceiverAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    //Create an asset issue
    response = HttpMethed.freezeBalance(httpnode,assetOwnerAddress,100000000L,3,1,assetOwnerKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    response = HttpMethed.assetIssue(httpnode, assetOwnerAddress, name, name, totalSupply, 1, 1,
        System.currentTimeMillis() + 5000, System.currentTimeMillis() + 50000000,
        2, 3, description, url, 1000L, 1000L, assetOwnerKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));

    HttpMethed.waitToProduceOneBlock(httpnode);

    response = HttpMethed.getAccount(httpnode, assetOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    assetIssueId = responseContent.getString("asset_issued_ID");

    contractName = "transferTokenContract";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_ContractTrcToken001_transferTokenContract");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_ContractTrcToken001_transferTokenContract");


    long tokenValue = 100000;
    long callValue = 5000;

    //This deploy is test too large call_token_value will made the witness node cpu 100%
    /*response = HttpMethed.deployContractGetTxidWithTooBigLong(httpnode,
    contractName, abi, code, 1000000L,1000000000L, 100, 11111111111111L,
        callValue, Integer.parseInt(assetIssueId), tokenValue, assetOwnerAddress, assetOwnerKey);
    responseContent = HttpMethed.parseResponseContent(response);
    Assert.assertTrue(responseContent.getString("Error").contains("Overflow"));*/




    String txid = HttpMethed.deployContractGetTxid(httpnode, contractName, abi, code, 1000000L,
        1000000000L, 100, 11111111111111L,
        callValue, Integer.parseInt(assetIssueId), tokenValue, assetOwnerAddress, assetOwnerKey);

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
    Assert.assertEquals(responseContent.getString("call_value"), "5000");
    Assert.assertEquals(responseContent.getString("origin_energy_limit"), "11111111111111");
    Assert.assertEquals(responseContent.getString("name"), contractName);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Trigger contract by http")
  public void test3TriggerContract() {

    String hexReceiverAddress = ByteArray.toHexString(assetReceiverAddress);
    String addressParam = "000000000000000000000000" + hexReceiverAddress.substring(2);//[0,3)

    String tokenIdParam = "00000000000000000000000000000000000000000000000000000000000"
        + Integer.toHexString(Integer.parseInt(assetIssueId));

    String tokenValueParam = "0000000000000000000000000000000000000000000000000000000000000001";
    logger.info(addressParam);
    logger.info(tokenIdParam);
    logger.info(tokenValueParam);
    final Long beforeBalance = HttpMethed.getBalance(httpnode,assetOwnerAddress);
    String param = addressParam + tokenIdParam + tokenValueParam;
    Long callValue = 10L;
    String txid = HttpMethed.triggerContractGetTxid(httpnode, assetOwnerAddress, contractAddress,
        "TransferTokenTo(address,trcToken,uint256)",
        param, 1000000000L, callValue, Integer.parseInt(assetIssueId), 20L, assetOwnerKey);

    HttpMethed.waitToProduceOneBlock(httpnode);
    //String txid = "49a30653d6e648da1e9a104b051b1b55c185fcaa0c2885405ae1d2fb258e3b3c";
    logger.info(txid);
    response = HttpMethed.getTransactionById(httpnode, txid);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(txid, responseContent.getString("txID"));
    Assert.assertTrue(!responseContent.getString("raw_data").isEmpty());
    Assert.assertTrue(!responseContent.getString("raw_data_hex").isEmpty());
    Long afterBalance = HttpMethed.getBalance(httpnode,assetOwnerAddress);
    logger.info("beforeBalance: " + beforeBalance);
    logger.info("afterBalance: " + afterBalance);
    Assert.assertTrue(beforeBalance - afterBalance == callValue);



    response = HttpMethed.getTransactionInfoById(httpnode, txid);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    String receiptString = responseContent.getString("receipt");
    Assert
        .assertEquals(HttpMethed.parseStringContent(receiptString).getString("result"), "SUCCESS");
    Assert.assertTrue(HttpMethed.parseStringContent(receiptString).getLong("energy_usage") > 0);
    Assert.assertTrue(responseContent.getLong("blockNumber") > 0);

    response = HttpMethed.getAccount(httpnode, assetReceiverAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(!responseContent.getString("assetV2").isEmpty());
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "UpdateSetting contract by http")
  public void test4UpdateSetting() {

    //assetOwnerAddress, assetOwnerKey
    response = HttpMethed
        .updateSetting(httpnode, assetOwnerAddress, contractAddress, 75, assetOwnerKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    response = HttpMethed.getContract(httpnode, contractAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.getString("consume_user_resource_percent"), "75");
    Assert.assertEquals(responseContent.getString("contract_address"), contractAddress);
    Assert.assertEquals(responseContent.getString("origin_address"),
        ByteArray.toHexString(assetOwnerAddress));
    Assert.assertEquals(responseContent.getString("call_value"), "5000");
    Assert.assertEquals(responseContent.getString("origin_energy_limit"), "11111111111111");
    Assert.assertEquals(responseContent.getString("name"), contractName);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "UpdateEnergyLimit contract by http")
  public void test5UpdateEnergyLimit() {

    //assetOwnerAddress, assetOwnerKey
    response = HttpMethed
        .updateEnergyLimit(httpnode, assetOwnerAddress, contractAddress, 1234567, assetOwnerKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    response = HttpMethed.getContract(httpnode, contractAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.getString("consume_user_resource_percent"), "75");
    Assert.assertEquals(responseContent.getString("contract_address"), contractAddress);
    Assert.assertEquals(responseContent.getString("origin_address"),
        ByteArray.toHexString(assetOwnerAddress));
    Assert.assertEquals(responseContent.getString("call_value"), "5000");
    Assert.assertEquals(responseContent.getString("origin_energy_limit"), "1234567");
    Assert.assertEquals(responseContent.getString("name"), contractName);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }
}
