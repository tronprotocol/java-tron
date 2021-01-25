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
public class HttpShieldTrc20Token004 extends ZenTrc20Base {

  JSONArray shieldedReceives = new JSONArray();
  String txid;
  JSONArray shieldSpends = new JSONArray();
  private String httpnode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(0);
  private String anotherHttpnode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(1);
  private String httpSolidityNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private String httpPbftNode = Configuration.getByPath("testng.conf")
          .getStringList("httpnode.ip.list").get(4);
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
  private Long account2Receive2V2Amount = publicFromAmount + account2Receive1V2Amount
      - account1Receive2V2Amount;

  /**
   * constructor.
   */
  @BeforeClass(enabled = true, description = "Prepare for transfer without ask")
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
    HttpMethed.waitToProduceOneBlock(httpnode);
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


  @Test(enabled = true, description = "Transfer type with 1V1 without ask by http")
  public void test01TransferTypeWith1V1WithoutAskByHttp() {
    account1IvkNoteTxs = scanShieldTrc20NoteByIvk(httpnode, shieldAccountInfo1);
    shieldSpends.clear();
    shieldSpends = createAndSetShieldedSpends(httpnode, shieldSpends, account1IvkNoteTxs
        .getJSONObject(0));
    HttpMethed.waitToProduceOneBlock(httpnode);
    shieldedReceives.clear();
    shieldedReceives = getHttpShieldedReceivesJsonArray(shieldedReceives, publicFromAmount,
        shieldAccountInfo2.getString("payment_address"), getRcm(httpnode));
    response = createShieldContractParametersWithoutAskForTransfer(httpnode, shieldAccountInfo1,
        shieldSpends, shieldedReceives);
    JSONObject shieldedTrc20Parameters = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(shieldedTrc20Parameters);
    JSONObject spendAuthSig = createSpendAuthSig(httpnode, shieldAccountInfo1,
        shieldedTrc20Parameters.getString("message_hash"), account1IvkNoteTxs
            .getJSONObject(0).getJSONObject("note").getString("rcm"));
    HttpMethed.printJsonContent(spendAuthSig);
    JSONArray spendAuthSigArray = new JSONArray();
    spendAuthSigArray.add(spendAuthSig);

    response = getTriggerInputForShieldedTrc20Contract(httpnode, shieldedTrc20Parameters,
        spendAuthSigArray);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    txid = HttpMethed.triggerContractGetTxidWithVisibleTrue(httpnode,anotherHttpnode,
        zenTrc20TokenOwnerAddressString, shieldAddress, transfer, responseContent
            .getString("value"), maxFeeLimit, 0L, 0, 0L,
        zenTrc20TokenOwnerKey);
    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getTransactionInfoById(httpnode, txid, true);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getJSONObject("receipt")
        .getLong("energy_usage_total") > 300000L);
    Assert.assertEquals(responseContent.getString("contract_address"), shieldAddress);
    Assert.assertEquals(responseContent.getJSONObject("receipt").getString("result"), "SUCCESS");

    account1OvkNoteTxs = scanShieldTrc20NoteByOvk(httpnode, shieldAccountInfo1);
    logger.info(account1OvkNoteTxs.toJSONString());
    Assert.assertEquals(account1OvkNoteTxs.size(), 1);
  }

  @Test(enabled = true, description = "Transfer type with 1V2 without ask by http")
  public void test02TransferTypeWith1V2WithoutAskByHttp() {

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
    response = createShieldContractParametersWithoutAskForTransfer(httpnode, shieldAccountInfo1,
        shieldSpends, shieldedReceives);
    JSONObject shieldedTrc20Parameters = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(shieldedTrc20Parameters);
    JSONObject spendAuthSig1 = createSpendAuthSig(httpnode, shieldAccountInfo1,
        shieldedTrc20Parameters.getString("message_hash"), account1IvkNoteTxs.getJSONObject(1)
            .getJSONObject("note").getString("rcm"));
    HttpMethed.printJsonContent(spendAuthSig1);
    JSONArray spendAuthSigArray = new JSONArray();
    spendAuthSigArray.add(spendAuthSig1);
    //spendAuthSigArray.add(spendAuthSig2);

    response = getTriggerInputForShieldedTrc20Contract(httpnode, shieldedTrc20Parameters,
        spendAuthSigArray);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    txid = HttpMethed.triggerContractGetTxidWithVisibleTrue(httpnode,anotherHttpnode,
        zenTrc20TokenOwnerAddressString, shieldAddress, transfer, responseContent
            .getString("value"), maxFeeLimit, 0L, 0, 0L,
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


  @Test(enabled = true, description = "Transfer type with 2V2 without ask by http")
  public void test03TransferTypeWith2V2WithoutAskByHttp() {
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
    response = createShieldContractParametersWithoutAskForTransfer(httpnode, shieldAccountInfo2,
        shieldSpends, shieldedReceives);
    JSONObject shieldedTrc20Parameters = HttpMethed.parseResponseContent(response);

    JSONObject spendAuthSig1 = createSpendAuthSig(httpnode, shieldAccountInfo2,
        shieldedTrc20Parameters.getString("message_hash"), account2IvkNoteTxs.getJSONObject(0)
            .getJSONObject("note").getString("rcm"));
    HttpMethed.printJsonContent(spendAuthSig1);

    JSONObject spendAuthSig2 = createSpendAuthSig(httpnode, shieldAccountInfo2,
        shieldedTrc20Parameters.getString("message_hash"), account2IvkNoteTxs.getJSONObject(1)
            .getJSONObject("note").getString("rcm"));
    JSONArray spendAuthSigArray = new JSONArray();
    spendAuthSigArray.add(spendAuthSig1);
    spendAuthSigArray.add(spendAuthSig2);

    response = getTriggerInputForShieldedTrc20Contract(httpnode, shieldedTrc20Parameters,
        spendAuthSigArray);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    txid = HttpMethed.triggerContractGetTxidWithVisibleTrue(httpnode,anotherHttpnode,
        zenTrc20TokenOwnerAddressString, shieldAddress, transfer, responseContent
            .getString("value"), maxFeeLimit, 0L, 0, 0L,
        zenTrc20TokenOwnerKey);

    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getTransactionInfoById(httpnode, txid, true);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getJSONObject("receipt")
        .getLong("energy_usage_total") > 300000L);
    Assert.assertEquals(responseContent.getString("contract_address"), shieldAddress);
    Assert.assertEquals(responseContent.getJSONObject("receipt").getString("result"), "SUCCESS");

    account2OvkNoteTxs = scanShieldTrc20NoteByOvk(httpnode, shieldAccountInfo2);
    logger.info(account1OvkNoteTxs.toJSONString());
    Assert.assertEquals(account2OvkNoteTxs.size(), 2);

    Assert.assertTrue(isShieldedTrc20ContractNoteSpent(httpnode, shieldAccountInfo2,
        account2IvkNoteTxs.getJSONObject(0)));
    Assert.assertTrue(isShieldedTrc20ContractNoteSpent(httpnode, shieldAccountInfo2,
        account2IvkNoteTxs.getJSONObject(1)));

  }

  @Test(enabled = true, description = "Transfer type with 2V1 without ask by http")
  public void test04TransferTypeWith2V1WithoutAskByHttp() {
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
    response = createShieldContractParametersWithoutAskForTransfer(httpnode, shieldAccountInfo1,
        shieldSpends, shieldedReceives);
    JSONObject shieldedTrc20Parameters = HttpMethed.parseResponseContent(response);

    JSONObject spendAuthSig1 = createSpendAuthSig(httpnode, shieldAccountInfo1,
        shieldedTrc20Parameters.getString("message_hash"), account1IvkNoteTxs.getJSONObject(2)
            .getJSONObject("note").getString("rcm"));
    HttpMethed.printJsonContent(spendAuthSig1);

    JSONObject spendAuthSig2 = createSpendAuthSig(httpnode, shieldAccountInfo1,
        shieldedTrc20Parameters.getString("message_hash"), account1IvkNoteTxs.getJSONObject(3)
            .getJSONObject("note").getString("rcm"));
    HttpMethed.printJsonContent(spendAuthSig2);

    JSONArray spendAuthSigArray = new JSONArray();
    spendAuthSigArray.add(spendAuthSig1);
    spendAuthSigArray.add(spendAuthSig2);

    response = getTriggerInputForShieldedTrc20Contract(httpnode, shieldedTrc20Parameters,
        spendAuthSigArray);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    txid = HttpMethed.triggerContractGetTxidWithVisibleTrue(httpnode,anotherHttpnode,
        zenTrc20TokenOwnerAddressString, shieldAddress, transfer, responseContent
            .getString("value"), maxFeeLimit, 0L, 0, 0L,
        zenTrc20TokenOwnerKey);
    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getTransactionInfoById(httpnode, txid, true);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getJSONObject("receipt")
        .getLong("energy_usage_total") > 300000L);
    Assert.assertEquals(responseContent.getString("contract_address"), shieldAddress);
    Assert.assertEquals(responseContent.getJSONObject("receipt").getString("result"), "SUCCESS");

    account1OvkNoteTxs = scanShieldTrc20NoteByOvk(httpnode, shieldAccountInfo1);
    logger.info(account1OvkNoteTxs.toJSONString());
    Assert.assertEquals(account1OvkNoteTxs.size(), 4);

    account1IvkNoteTxs = scanShieldTrc20NoteByIvk(httpnode, shieldAccountInfo1);
    Assert.assertTrue(isShieldedTrc20ContractNoteSpent(httpnode, shieldAccountInfo1,
        account1IvkNoteTxs.getJSONObject(2)));
    Assert.assertTrue(isShieldedTrc20ContractNoteSpentOnPbft(httpPbftNode, shieldAccountInfo1,
            account1IvkNoteTxs.getJSONObject(2)));
    Assert.assertTrue(isShieldedTrc20ContractNoteSpent(httpnode, shieldAccountInfo1,
        account1IvkNoteTxs.getJSONObject(3)));
    Assert.assertTrue(isShieldedTrc20ContractNoteSpentOnPbft(httpPbftNode, shieldAccountInfo1,
            account1IvkNoteTxs.getJSONObject(3)));

  }


  @Test(enabled = true, description = "Scan note by ivk and ovk on solidity and pbft by http")
  public void test05ScanNoteByIvkAndOvkOnSOlidityAndPbftByHttp() {
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);

    account1IvkNoteTxs = scanShieldTrc20NoteByIvk(httpnode, shieldAccountInfo1);
    JSONArray account1IvkNoteTxsOnSolidity = scanShieldTrc20NoteByIvkOnSolidity(httpSolidityNode,
        shieldAccountInfo1);
    Assert.assertEquals(account1IvkNoteTxs, account1IvkNoteTxsOnSolidity);
    JSONArray account1IvkNoteTxsOnPbft = scanShieldTrc20NoteByIvkOnPbft(httpPbftNode,
            shieldAccountInfo1);
    Assert.assertEquals(account1IvkNoteTxs, account1IvkNoteTxsOnPbft);

    account1OvkNoteTxs = scanShieldTrc20NoteByOvk(httpnode, shieldAccountInfo1);
    JSONArray account1OvkNoteTxsOnSolidity = scanShieldTrc20NoteByOvkOnSolidity(httpSolidityNode,
        shieldAccountInfo1);
    Assert.assertEquals(account1OvkNoteTxs, account1OvkNoteTxsOnSolidity);
    JSONArray account1OvkNoteTxsOnPbft = scanShieldTrc20NoteByOvkOnPbft(httpPbftNode,
            shieldAccountInfo1);
    Assert.assertEquals(account1OvkNoteTxs, account1OvkNoteTxsOnPbft);

    account2IvkNoteTxs = scanShieldTrc20NoteByIvk(httpnode, shieldAccountInfo2);
    JSONArray account2IvkNoteTxsOnSolidity = scanShieldTrc20NoteByIvkOnSolidity(httpSolidityNode,
        shieldAccountInfo2);
    Assert.assertEquals(account2IvkNoteTxs, account2IvkNoteTxsOnSolidity);
    JSONArray account2IvkNoteTxsOnPbft = scanShieldTrc20NoteByIvkOnPbft(httpPbftNode,
            shieldAccountInfo2);
    Assert.assertEquals(account2IvkNoteTxs, account2IvkNoteTxsOnPbft);

    account2OvkNoteTxs = scanShieldTrc20NoteByOvk(httpnode, shieldAccountInfo2);
    JSONArray account2OvkNoteTxsOnSolidity = scanShieldTrc20NoteByOvkOnSolidity(httpSolidityNode,
        shieldAccountInfo2);
    Assert.assertEquals(account2OvkNoteTxs, account2OvkNoteTxsOnSolidity);
    JSONArray account2OvkNoteTxsOnPbft = scanShieldTrc20NoteByOvkOnPbft(httpPbftNode,
            shieldAccountInfo2);
    Assert.assertEquals(account2OvkNoteTxs, account2OvkNoteTxsOnPbft);

  }


  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
  }
}