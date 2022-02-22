package stest.tron.wallet.dailybuild.zentrc20token;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.ZenTrc20Base;

@Slf4j
public class HttpShieldTrc20Token003 extends ZenTrc20Base {

  JSONArray shieldedReceives = new JSONArray();
  String txid;
  JSONArray shieldSpends = new JSONArray();
  private String httpnode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(0);
  private String anotherHttpnode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(1);
  private String httpSolidityNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private JSONObject responseContent;
  private HttpResponse response;
  private JSONObject shieldAccountInfo1;
  private JSONObject shieldAccountInfo2;
  private JSONArray account1IvkNoteTxs = new JSONArray();
  private JSONArray account2IvkNoteTxs = new JSONArray();
  private JSONArray account1OvkNoteTxs = new JSONArray();
  private JSONArray account2OvkNoteTxs = new JSONArray();
  private Long publicFromAmount = getRandomLongAmount();
  private Long account1Receive1V2Amount = 10L;
  private Long account2Receive1V2Amount = publicFromAmount - account1Receive1V2Amount;
  private Long account1Receive2V2Amount = 13L;
  private Long account2Receive2V2Amount = publicFromAmount
      + account2Receive1V2Amount - account1Receive2V2Amount;

  /**
   * constructor.
   */
  @BeforeClass(enabled = true, description = "Prepare for transfer")
  public void prepareForTransfer() {
    //Create two shield account
    response = getNewShieldedAddress(httpnode);
    shieldAccountInfo1 = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(shieldAccountInfo1);

    response = getNewShieldedAddress(httpnode);
    shieldAccountInfo2 = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(shieldAccountInfo2);
    //Send two mint to shield account 1
    shieldedReceives.clear();
    shieldedReceives = getHttpShieldedReceivesJsonArray(shieldedReceives, publicFromAmount,
        shieldAccountInfo1.getString("payment_address"), getRcm(httpnode));
    response = createShieldContractParameters(httpnode, publicFromAmount, shieldAccountInfo1,
        shieldedReceives);
    responseContent = HttpMethed.parseResponseContent(response);
    //HttpMethed.printJsonContent(responseContent);

    txid = HttpMethed.triggerContractGetTxidWithVisibleTrue(httpnode,anotherHttpnode,
        zenTrc20TokenOwnerAddressString, shieldAddress, mint, responseContent
            .getString("trigger_contract_input"), maxFeeLimit, 0L, 0, 0L,
        zenTrc20TokenOwnerKey);
    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);

    shieldedReceives.clear();
    shieldedReceives = getHttpShieldedReceivesJsonArray(shieldedReceives, publicFromAmount,
        shieldAccountInfo1.getString("payment_address"), getRcm(httpnode));
    response = createShieldContractParameters(httpnode, publicFromAmount, shieldAccountInfo1,
        shieldedReceives);
    responseContent = HttpMethed.parseResponseContent(response);
    //HttpMethed.printJsonContent(responseContent);

    txid = HttpMethed.triggerContractGetTxidWithVisibleTrue(httpnode,anotherHttpnode,
        zenTrc20TokenOwnerAddressString, shieldAddress, mint, responseContent
            .getString("trigger_contract_input"), maxFeeLimit, 0L, 0, 0L,
        zenTrc20TokenOwnerKey);
    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);

  }


  @Test(enabled = true, description = "Transfer type with 1V1 by http")
  public void test01TransferTypeWith1V1ByHttp() {
    account1IvkNoteTxs = scanShieldTrc20NoteByIvk(httpnode, shieldAccountInfo1);
    shieldSpends.clear();
    shieldSpends = createAndSetShieldedSpends(httpnode, shieldSpends, account1IvkNoteTxs
        .getJSONObject(0));
    shieldedReceives.clear();
    shieldedReceives = getHttpShieldedReceivesJsonArray(shieldedReceives, publicFromAmount,
        shieldAccountInfo2.getString("payment_address"), getRcm(httpnode));
    response = createShieldContractParametersForTransfer(httpnode, shieldAccountInfo1,
        shieldSpends, shieldedReceives);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    txid = HttpMethed.triggerContractGetTxidWithVisibleTrue(httpnode,anotherHttpnode,
        zenTrc20TokenOwnerAddressString, shieldAddress, transfer, responseContent
            .getString("trigger_contract_input"), maxFeeLimit, 0L, 0, 0L,
        zenTrc20TokenOwnerKey);
    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getTransactionInfoById(httpnode, txid, true);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getJSONObject("receipt")
        .getLong("energy_usage_total") > 300000L);
    Assert.assertEquals(responseContent.getString("contract_address"), shieldAddress);
    Assert.assertEquals(responseContent.getJSONObject("receipt").getString("result"),
        "SUCCESS");

    account1OvkNoteTxs = scanShieldTrc20NoteByOvk(httpnode, shieldAccountInfo1);
    logger.info(account1OvkNoteTxs.toJSONString());
    Assert.assertEquals(account1OvkNoteTxs.size(), 1);
  }

  @Test(enabled = true, description = "Transfer type with 1V2 by http")
  public void test02TransferTypeWith1V2ByHttp() {
    account1IvkNoteTxs = scanShieldTrc20NoteByIvk(httpnode, shieldAccountInfo1);

    Assert.assertTrue(isShieldedTrc20ContractNoteSpent(httpnode, shieldAccountInfo1,
        account1IvkNoteTxs.getJSONObject(0)));
    Assert.assertFalse(isShieldedTrc20ContractNoteSpent(httpnode, shieldAccountInfo1,
        account1IvkNoteTxs.getJSONObject(1)));
    shieldSpends.clear();
    shieldSpends = createAndSetShieldedSpends(httpnode, shieldSpends, account1IvkNoteTxs
        .getJSONObject(1));

    shieldedReceives.clear();
    shieldedReceives = getHttpShieldedReceivesJsonArray(shieldedReceives, account1Receive1V2Amount,
        shieldAccountInfo1.getString("payment_address"), getRcm(httpnode));
    shieldedReceives = getHttpShieldedReceivesJsonArray(shieldedReceives, account2Receive1V2Amount,
        shieldAccountInfo2.getString("payment_address"), getRcm(httpnode));
    response = createShieldContractParametersForTransfer(httpnode, shieldAccountInfo1, shieldSpends,
        shieldedReceives);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.containsKey("trigger_contract_input"));

    txid = HttpMethed.triggerContractGetTxidWithVisibleTrue(httpnode,anotherHttpnode,
        zenTrc20TokenOwnerAddressString, shieldAddress, transfer, responseContent
            .getString("trigger_contract_input"), maxFeeLimit, 0L, 0, 0L,
        zenTrc20TokenOwnerKey);
    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getTransactionInfoById(httpnode, txid, true);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getJSONObject("receipt")
        .getLong("energy_usage_total") > 300000L);
    Assert.assertEquals(responseContent.getString("contract_address"), shieldAddress);
    Assert.assertEquals(responseContent.getJSONObject("receipt").getString("result"),
        "SUCCESS");

    account1OvkNoteTxs = scanShieldTrc20NoteByOvk(httpnode, shieldAccountInfo1);
    logger.info(account1OvkNoteTxs.toJSONString());
    Assert.assertEquals(account1OvkNoteTxs.size(), 3);
  }


  @Test(enabled = true, description = "Transfer type with 2V2 by http")
  public void test03TransferTypeWith2V2ByHttp() {
    account2IvkNoteTxs = scanShieldTrc20NoteByIvk(httpnode, shieldAccountInfo2);

    Assert.assertFalse(isShieldedTrc20ContractNoteSpent(httpnode, shieldAccountInfo2,
        account2IvkNoteTxs.getJSONObject(0)));
    Assert.assertFalse(isShieldedTrc20ContractNoteSpent(httpnode, shieldAccountInfo2,
        account2IvkNoteTxs.getJSONObject(1)));
    shieldSpends.clear();
    shieldSpends = createAndSetShieldedSpends(httpnode, shieldSpends, account2IvkNoteTxs
        .getJSONObject(0));
    shieldSpends = createAndSetShieldedSpends(httpnode, shieldSpends, account2IvkNoteTxs
        .getJSONObject(1));
    shieldedReceives.clear();
    shieldedReceives = getHttpShieldedReceivesJsonArray(shieldedReceives, account1Receive2V2Amount,
        shieldAccountInfo1.getString("payment_address"), getRcm(httpnode));
    shieldedReceives = getHttpShieldedReceivesJsonArray(shieldedReceives, account2Receive2V2Amount,
        shieldAccountInfo2.getString("payment_address"), getRcm(httpnode));
    response = createShieldContractParametersForTransfer(httpnode, shieldAccountInfo2, shieldSpends,
        shieldedReceives);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.containsKey("trigger_contract_input"));

    txid = HttpMethed.triggerContractGetTxidWithVisibleTrue(httpnode,anotherHttpnode,
        zenTrc20TokenOwnerAddressString, shieldAddress, transfer, responseContent
            .getString("trigger_contract_input"), maxFeeLimit, 0L, 0, 0L,
        zenTrc20TokenOwnerKey);
    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getTransactionInfoById(httpnode, txid, true);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getJSONObject("receipt")
        .getLong("energy_usage_total") > 300000L);
    Assert.assertEquals(responseContent.getString("contract_address"), shieldAddress);
    Assert.assertEquals(responseContent.getJSONObject("receipt").getString("result"),
        "SUCCESS");

    account2OvkNoteTxs = scanShieldTrc20NoteByOvk(httpnode, shieldAccountInfo2);
    logger.info(account1OvkNoteTxs.toJSONString());
    Assert.assertEquals(account2OvkNoteTxs.size(), 2);

    Assert.assertTrue(isShieldedTrc20ContractNoteSpent(httpnode, shieldAccountInfo2,
        account2IvkNoteTxs.getJSONObject(0)));
    Assert.assertTrue(isShieldedTrc20ContractNoteSpent(httpnode, shieldAccountInfo2,
        account2IvkNoteTxs.getJSONObject(1)));

  }

  @Test(enabled = true, description = "Transfer type with 2V1 by http")
  public void test04TransferTypeWith2V1ByHttp() {
    account1IvkNoteTxs = scanShieldTrc20NoteByIvk(httpnode, shieldAccountInfo1);

    Assert.assertFalse(isShieldedTrc20ContractNoteSpent(httpnode, shieldAccountInfo1,
        account1IvkNoteTxs.getJSONObject(2)));
    Assert.assertFalse(isShieldedTrc20ContractNoteSpent(httpnode, shieldAccountInfo1,
        account1IvkNoteTxs.getJSONObject(3)));
    shieldSpends.clear();
    shieldSpends = createAndSetShieldedSpends(httpnode, shieldSpends, account1IvkNoteTxs
        .getJSONObject(2));
    shieldSpends = createAndSetShieldedSpends(httpnode, shieldSpends, account1IvkNoteTxs
        .getJSONObject(3));
    Long account1Receive2V1Amount = account1IvkNoteTxs.getJSONObject(2)
        .getJSONObject("note").getLong("value")
        + account1IvkNoteTxs.getJSONObject(3).getJSONObject("note").getLong("value");
    shieldedReceives.clear();
    shieldedReceives = getHttpShieldedReceivesJsonArray(shieldedReceives, account1Receive2V1Amount,
        shieldAccountInfo1.getString("payment_address"), getRcm(httpnode));
    response = createShieldContractParametersForTransfer(httpnode, shieldAccountInfo1, shieldSpends,
        shieldedReceives);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.containsKey("trigger_contract_input"));

    txid = HttpMethed.triggerContractGetTxidWithVisibleTrue(httpnode,anotherHttpnode,
        zenTrc20TokenOwnerAddressString, shieldAddress, transfer, responseContent
            .getString("trigger_contract_input"), maxFeeLimit, 0L, 0, 0L,
        zenTrc20TokenOwnerKey);

    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getTransactionInfoById(httpnode, txid, true);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getJSONObject("receipt")
        .getLong("energy_usage_total") > 300000L);
    Assert.assertEquals(responseContent.getString("contract_address"), shieldAddress);
    Assert.assertEquals(responseContent.getJSONObject("receipt").getString("result"),
        "SUCCESS");

    account1OvkNoteTxs = scanShieldTrc20NoteByOvk(httpnode, shieldAccountInfo1);
    logger.info(account1OvkNoteTxs.toJSONString());
    Assert.assertEquals(account1OvkNoteTxs.size(), 4);

    account1IvkNoteTxs = scanShieldTrc20NoteByIvk(httpnode, shieldAccountInfo1);
    Assert.assertTrue(isShieldedTrc20ContractNoteSpent(httpnode, shieldAccountInfo1,
        account1IvkNoteTxs.getJSONObject(2)));
    Assert.assertTrue(isShieldedTrc20ContractNoteSpent(httpnode, shieldAccountInfo1,
        account1IvkNoteTxs.getJSONObject(3)));

  }


  @Test(enabled = true, description = "Query is shielded trc20 contract note spent on "
      + "solidity by http")
  public void test05QueryIsShieldedTrc20ContractNoteSpentByHttp() {
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    Assert.assertTrue(isShieldedTrc20ContractNoteSpentOnSolidity(httpSolidityNode,
        shieldAccountInfo1, account1IvkNoteTxs.getJSONObject(2)));
    Assert.assertTrue(isShieldedTrc20ContractNoteSpentOnSolidity(httpSolidityNode,
        shieldAccountInfo1, account1IvkNoteTxs.getJSONObject(3)));
  }


  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
  }
}