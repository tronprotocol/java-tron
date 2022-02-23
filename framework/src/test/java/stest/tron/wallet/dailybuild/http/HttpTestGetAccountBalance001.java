package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class HttpTestGetAccountBalance001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(0);
  private String httpPbftNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(4);
  private String httpSolidityNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] assetOwnerAddress = ecKey2.getAddress();
  String assetOwnerKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] randomAddress = ecKey3.getAddress();
  Long amount = 2048000000L;
  String txid;
  Integer sendcoinBlockNumber;
  String sendcoinBlockHash;
  Integer deployContractBlockNumber;
  String deployContractBlockHash;
  Long fee;

  /**
   * constructor.
   */
  @BeforeClass(enabled = true, description = "Deploy smart contract by http")
  public void test01DeployContractForTest() {
    HttpMethed.waitToProduceOneBlock(httpnode);
    PublicMethed.printAddress(assetOwnerKey);
    txid = HttpMethed.sendCoin(httpnode, fromAddress, assetOwnerAddress, amount, "", testKey002);
    HttpMethed.waitToProduceOneBlock(httpnode);
    txid = HttpMethed.sendCoin(httpnode, assetOwnerAddress, randomAddress,
        amount / 1000000L, "", assetOwnerKey);
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getTransactionInfoById(httpnode, txid);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    sendcoinBlockNumber = responseContent.getInteger("blockNumber");
    Assert.assertTrue(sendcoinBlockNumber > 0);

    response = HttpMethed.getBlockByNum(httpnode, sendcoinBlockNumber);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    sendcoinBlockHash = responseContent.getString("blockID");

    String contractName = "transferTokenContract";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_ContractTrcToken001_transferTokenContract");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_ContractTrcToken001_transferTokenContract");
    txid = HttpMethed
        .deployContractGetTxid(httpnode, contractName, abi, code, 1000000L, 1000000000L, 100,
            11111111111111L, 0L, 0, 0L, assetOwnerAddress, assetOwnerKey);

    HttpMethed.waitToProduceOneBlock(httpnode);
    logger.info(txid);

    response = HttpMethed.getTransactionInfoById(httpnode, txid);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    fee = responseContent.getLong("fee");
    deployContractBlockNumber = responseContent.getInteger("blockNumber");
    String receiptString = responseContent.getString("receipt");
    Assert
        .assertEquals(HttpMethed.parseStringContent(receiptString).getString("result"), "SUCCESS");

    response = HttpMethed.getBlockByNum(httpnode, deployContractBlockNumber);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    deployContractBlockHash = responseContent.getString("blockID");
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get account balance by http")
  public void test01GetAccountBalance() {
    response = HttpMethed.getAccountBalance(httpnode, assetOwnerAddress,
        sendcoinBlockNumber, sendcoinBlockHash);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() >= 2);
    final Long beforeBalance = responseContent.getLong("balance");

    response = HttpMethed.getAccountBalance(httpnode, assetOwnerAddress,
        deployContractBlockNumber, deployContractBlockHash);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() >= 2);
    Long afterBalance = responseContent.getLong("balance");

    Assert.assertTrue(beforeBalance - afterBalance == fee);


    response = HttpMethed.getAccountBalance(httpnode, assetOwnerAddress,
        deployContractBlockNumber, deployContractBlockHash);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() >= 2);


  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get block balance by http")
  public void test02GetBlockBalance() {
    response = HttpMethed.getBlockBalance(httpnode,
        sendcoinBlockNumber, sendcoinBlockHash);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() >= 2);
    Assert.assertEquals(sendcoinBlockNumber, responseContent.getJSONObject("block_identifier")
        .getInteger("number"));
    JSONObject transactionObject = responseContent.getJSONArray("transaction_balance_trace")
        .getJSONObject(0);
    Assert.assertEquals(transactionObject.getString("type"), "TransferContract");
    Assert.assertTrue(Math.abs(transactionObject.getJSONArray("operation")
        .getJSONObject(0).getLong("amount")) == 100000L);
    Assert.assertTrue(Math.abs(transactionObject.getJSONArray("operation")
        .getJSONObject(1).getLong("amount")) == amount / 1000000L);
    Assert.assertTrue(Math.abs(transactionObject.getJSONArray("operation")
        .getJSONObject(2).getLong("amount")) == amount / 1000000L);

    response = HttpMethed.getBlockBalance(httpnode,
        deployContractBlockNumber, deployContractBlockHash);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() >= 2);

    transactionObject = responseContent.getJSONArray("transaction_balance_trace").getJSONObject(0);
    Assert.assertEquals(transactionObject.getString("transaction_identifier"), txid);
    Assert.assertEquals(transactionObject.getString("type"), "CreateSmartContract");
    Assert.assertTrue(transactionObject.getJSONArray("operation")
        .getJSONObject(0).getLong("amount") == -fee);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get burn trx by http")
  public void test03GetBurnTrx() {

    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] assetOwnerAddress = ecKey2.getAddress();
    String assetOwnerKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    HttpMethed.sendCoin(httpnode, fromAddress, assetOwnerAddress, amount, "", testKey002);
    HttpMethed.waitToProduceOneBlock(httpnode);
    final Long beforeBurnTrxAmount = HttpMethed.getBurnTrx(httpnode);
    ECKey ecKey3 = new ECKey(Utils.getRandom());
    byte[] receiverAddress = ecKey3.getAddress();

    HttpMethed.sendCoin(httpnode, assetOwnerAddress, receiverAddress, amount - 103000L,
            "", assetOwnerKey);
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    Long afterBurnTrxAmount = HttpMethed.getBurnTrx(httpnode);
    logger.info(afterBurnTrxAmount + "  :   " + beforeBurnTrxAmount);
    Assert.assertTrue(afterBurnTrxAmount - beforeBurnTrxAmount == 100000L);

    Assert.assertEquals(afterBurnTrxAmount, HttpMethed.getBurnTrxFromSolidity(httpSolidityNode));
    Assert.assertEquals(afterBurnTrxAmount, HttpMethed.getBurnTrxFromPbft(httpPbftNode));
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Get receipt root by http")
  public void test04GetReceiptRootByHttp() {
    response = HttpMethed.getBlockByNum(httpnode,sendcoinBlockNumber);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    String receiptsRoot = responseContent.getJSONObject("block_header").getJSONObject("raw_data")
        .getString("receiptsRoot");
    Assert.assertNotEquals(receiptsRoot,
        "0000000000000000000000000000000000000000000000000000000000000000");
    Assert.assertFalse(receiptsRoot.isEmpty());

  }


  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }
}