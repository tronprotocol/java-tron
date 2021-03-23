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
public class HttpShieldTrc20Token005 extends ZenTrc20Base {

  JSONArray shieldedReceives = new JSONArray();
  String txid;
  private String httpnode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(0);
  private String anotherHttpnode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(1);
  private String httpSolidityNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private JSONObject responseContent;
  private HttpResponse response;
  private JSONObject shieldAccountInfo;
  private JSONObject shieldReceiverAccountInfo;
  private JSONArray noteTxs;
  private Long publicFromAmount = getRandomLongAmount();

  /**
   * constructor.
   */

  @BeforeClass(enabled = true, description = "Get new shield account  by http")
  public void createTwoNote() {
    response = getNewShieldedAddress(httpnode);
    shieldAccountInfo = HttpMethed.parseResponseContent(response);
    shieldedReceives = getHttpShieldedReceivesJsonArray(shieldedReceives, publicFromAmount,
        shieldAccountInfo.getString("payment_address"), getRcm((httpnode)));
    response = createShieldContractParameters(httpnode, publicFromAmount, shieldAccountInfo,
        shieldedReceives);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    txid = HttpMethed.triggerContractGetTxidWithVisibleTrue(httpnode,anotherHttpnode,
        zenTrc20TokenOwnerAddressString, shieldAddress, mint, responseContent
            .getString("trigger_contract_input"), maxFeeLimit, 0L, 0, 0L,
        zenTrc20TokenOwnerKey);
    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getTransactionInfoById(httpnode, txid, true);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    shieldedReceives.clear();
    shieldedReceives = getHttpShieldedReceivesJsonArray(shieldedReceives, publicFromAmount,
        shieldAccountInfo.getString("payment_address"), getRcm(httpnode));
    response = createShieldContractParameters(httpnode, publicFromAmount, shieldAccountInfo,
        shieldedReceives);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    txid = HttpMethed.triggerContractGetTxidWithVisibleTrue(httpnode,anotherHttpnode,
        zenTrc20TokenOwnerAddressString, shieldAddress, mint, responseContent
            .getString("trigger_contract_input"), maxFeeLimit, 0L, 0, 0L,
        zenTrc20TokenOwnerKey);

    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getTransactionInfoById(httpnode, txid, true);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    noteTxs = scanShieldTrc20NoteByIvk(httpnode, shieldAccountInfo);
  }

  @Test(enabled = true, description = "Shield trc20 burn to one T and one S by http")
  public void test01ShiledTrc20BurnToOnePublicAndOneShieldByHttp() {
    response = getNewShieldedAddress(httpnode);
    shieldReceiverAccountInfo = HttpMethed.parseResponseContent(response);

    JSONArray shieldSpends = new JSONArray();
    shieldSpends = createAndSetShieldedSpends(httpnode, shieldSpends, noteTxs.getJSONObject(0));

    logger.info(shieldSpends.toJSONString());

    Long toShieldAmount = 9L;
    Long toPublicAmount = publicFromAmount - toShieldAmount;
    shieldedReceives.clear();
    shieldedReceives = getHttpShieldedReceivesJsonArray(shieldedReceives, toShieldAmount,
        shieldReceiverAccountInfo.getString("payment_address"), getRcm(httpnode));

    response = createShieldContractParametersForBurn(httpnode, shieldAccountInfo, shieldSpends,
        zenTrc20TokenOwnerAddressString, toPublicAmount, shieldedReceives);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    txid = HttpMethed.triggerContractGetTxidWithVisibleTrue(httpnode,anotherHttpnode,
        zenTrc20TokenOwnerAddressString, shieldAddress, burn, responseContent
            .getString("trigger_contract_input"), maxFeeLimit, 0L, 0, 0L,
        zenTrc20TokenOwnerKey);

    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getTransactionInfoById(httpnode, txid, true);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getJSONObject("receipt")
        .getLong("energy_usage_total") > 150000L);
    Assert.assertEquals(responseContent.getString("contract_address"), shieldAddress);
    Assert.assertEquals(responseContent.getJSONObject("receipt").getString("result"),
        "SUCCESS");

    noteTxs = scanShieldTrc20NoteByOvk(httpnode, shieldAccountInfo);
    logger.info("noteTxs ovk:" + noteTxs);

    Assert.assertEquals(noteTxs.getJSONObject(0).getJSONObject("note")
        .getLong("value"), toShieldAmount);
    Assert.assertEquals(noteTxs.getJSONObject(0).getJSONObject("note")
        .getString("payment_address"), shieldReceiverAccountInfo.getString("payment_address"));

    Assert.assertEquals(noteTxs.getJSONObject(1).getLong("to_amount"), toPublicAmount);
    Assert.assertEquals(noteTxs.getJSONObject(1).getString("transparent_to_address"),
        zenTrc20TokenOwnerAddressString);
    Assert.assertEquals(noteTxs.getJSONObject(1).getString("txid"), txid);
  }


  @Test(enabled = true, description = "Shield trc20 burn without ask to one "
      + "public and one shield by http")
  public void test02ShiledTrc20BurnWithoutAskToOnePublicAndOneShieldByHttp() {
    noteTxs = scanShieldTrc20NoteByIvk(httpnode, shieldAccountInfo);
    JSONArray shieldSpends = new JSONArray();
    shieldSpends = createAndSetShieldedSpends(httpnode, shieldSpends, noteTxs.getJSONObject(1));

    Long toShieldAmount = 8L;
    Long toPublicAmount = publicFromAmount - toShieldAmount;
    shieldedReceives.clear();
    shieldedReceives = getHttpShieldedReceivesJsonArray(shieldedReceives, toShieldAmount,
        shieldReceiverAccountInfo.getString("payment_address"), getRcm(httpnode));

    response = createShieldContractParametersWithoutAskForBurn(httpnode, shieldAccountInfo,
        shieldSpends, zenTrc20TokenOwnerAddressString, toPublicAmount, shieldedReceives);
    JSONObject shieldedTrc20Parameters = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(shieldedTrc20Parameters);
    JSONObject spendAuthSig = createSpendAuthSig(httpnode, shieldAccountInfo,
        shieldedTrc20Parameters.getString("message_hash"), noteTxs.getJSONObject(1)
            .getJSONObject("note").getString("rcm"));
    HttpMethed.printJsonContent(spendAuthSig);
    JSONArray spendAuthSigArray = new JSONArray();
    spendAuthSigArray.add(spendAuthSig);

    response = getTriggerInputForShieldedTrc20BurnContract(httpnode,
        shieldedTrc20Parameters, spendAuthSigArray, toPublicAmount,
        zenTrc20TokenOwnerAddressString);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    txid = HttpMethed.triggerContractGetTxidWithVisibleTrue(httpnode,anotherHttpnode,
        zenTrc20TokenOwnerAddressString, shieldAddress, burn, responseContent
            .getString("value"), maxFeeLimit, 0L, 0, 0L,
        zenTrc20TokenOwnerKey);

    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getTransactionInfoById(httpnode, txid, true);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getJSONObject("receipt")
        .getLong("energy_usage_total") > 150000L);
    Assert.assertEquals(responseContent.getString("contract_address"), shieldAddress);
    Assert.assertEquals(responseContent.getJSONObject("receipt").getString("result"), "SUCCESS");

    noteTxs = scanShieldTrc20NoteByOvk(httpnode, shieldAccountInfo);
    logger.info("noteTxs ovk:" + noteTxs);

    Assert.assertEquals(noteTxs.getJSONObject(2).getJSONObject("note")
        .getLong("value"), toShieldAmount);
    Assert.assertEquals(noteTxs.getJSONObject(2).getJSONObject("note")
        .getString("payment_address"), shieldReceiverAccountInfo.getString("payment_address"));

    Assert.assertEquals(noteTxs.getJSONObject(3).getLong("to_amount"), toPublicAmount);
    Assert.assertEquals(noteTxs.getJSONObject(3).getString("transparent_to_address"),
        zenTrc20TokenOwnerAddressString);
    Assert.assertEquals(noteTxs.getJSONObject(3).getString("txid"), txid);

  }


  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
  }
}