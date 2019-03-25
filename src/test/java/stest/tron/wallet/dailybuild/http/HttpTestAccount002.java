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
public class HttpTestAccount002 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(1);
  private String httpSoliditynode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] freezeBalanceAddress = ecKey1.getAddress();
  String freezeBalanceKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiverResourceAddress = ecKey2.getAddress();
  String receiverResourceKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  Long berforeBalance;
  Long afterBalance;

  Long amount = 10000000L;
  Long frozenBalance = 2000000L;

  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalance for bandwidth by http")
  public void test01FreezebalanceForBandwidth() {
    PublicMethed.printAddress(freezeBalanceKey);
    //Send trx to test account
    response = HttpMethed.sendCoin(httpnode, fromAddress, freezeBalanceAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);

    //Freeze balance
    response = HttpMethed.freezeBalance(httpnode, freezeBalanceAddress, frozenBalance, 0,
        0, freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(berforeBalance - afterBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalance for bandwidth by http")
  public void test02UnFreezebalanceForBandwidth() {
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);

    //UnFreeze balance for bandwidth
    response = HttpMethed.unFreezeBalance(httpnode, freezeBalanceAddress, 0, freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(afterBalance - berforeBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalance for energy by http")
  public void test03FreezebalanceForEnergy() {
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);

    //Freeze balance for energy
    response = HttpMethed.freezeBalance(httpnode, freezeBalanceAddress, frozenBalance, 0,
        1, freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(berforeBalance - afterBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalance for energy by http")
  public void test04UnFreezebalanceForEnergy() {

    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    HttpMethed.waitToProduceOneBlock(httpnode);
    //UnFreeze balance for energy
    response = HttpMethed.unFreezeBalance(httpnode, freezeBalanceAddress, 1, freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(afterBalance - berforeBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalance with bandwidth for others by http")
  public void test05FreezebalanceOfBandwidthForOthers() {
    response = HttpMethed
        .sendCoin(httpnode, fromAddress, receiverResourceAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);

    //Freeze balance with bandwidth for others
    response = HttpMethed.freezeBalance(httpnode, freezeBalanceAddress, frozenBalance, 0,
        0, receiverResourceAddress, freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(berforeBalance - afterBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get Delegated Resource by http")
  public void test06GetDelegatedResource() {
    response = HttpMethed.getDelegatedResource(
        httpnode, freezeBalanceAddress, receiverResourceAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("delegatedResource").toString());
    Assert.assertTrue(jsonArray.size() >= 1);
    Assert.assertEquals(jsonArray.getJSONObject(0).getString("from"),
        ByteArray.toHexString(freezeBalanceAddress));
    Assert.assertEquals(jsonArray.getJSONObject(0).getString("to"),
        ByteArray.toHexString(receiverResourceAddress));
    Assert.assertEquals(jsonArray.getJSONObject(0).getLong("frozen_balance_for_bandwidth"),
        frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get Delegated Resource from solidity by http")
  public void test07GetDelegatedResourceFromSolidity() {
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethed.getDelegatedResourceFromSolidity(
        httpSoliditynode, freezeBalanceAddress, receiverResourceAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("delegatedResource").toString());
    Assert.assertTrue(jsonArray.size() >= 1);
    Assert.assertEquals(jsonArray.getJSONObject(0).getString("from"),
        ByteArray.toHexString(freezeBalanceAddress));
    Assert.assertEquals(jsonArray.getJSONObject(0).getString("to"),
        ByteArray.toHexString(receiverResourceAddress));
    Assert.assertEquals(jsonArray.getJSONObject(0).getLong("frozen_balance_for_bandwidth"),
        frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get Delegated Resource Account Index by http")
  public void test08GetDelegatedResourceAccountIndex() {
    response = HttpMethed.getDelegatedResourceAccountIndex(httpnode, freezeBalanceAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertFalse(responseContent.get("toAccounts").toString().isEmpty());
    String toAddress = responseContent.getJSONArray("toAccounts").get(0).toString();
    Assert.assertEquals(toAddress, ByteArray.toHexString(receiverResourceAddress));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get Delegated Resource Account Index from solidity by http")
  public void test09GetDelegatedResourceAccountIndexFromSolidity() {
    response = HttpMethed.getDelegatedResourceAccountIndexFromSolidity(httpSoliditynode,
        freezeBalanceAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertFalse(responseContent.get("toAccounts").toString().isEmpty());
    String toAddress = responseContent.getJSONArray("toAccounts").get(0).toString();
    Assert.assertEquals(toAddress, ByteArray.toHexString(receiverResourceAddress));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalance with bandwidth for others by http")
  public void test10UnFreezebalanceOfBandwidthForOthers() {
    HttpMethed.waitToProduceOneBlock(httpnode);
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);

    //UnFreeze balance with bandwidth for others
    response = HttpMethed.unFreezeBalance(httpnode, freezeBalanceAddress, 0,
        receiverResourceAddress, freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(afterBalance - berforeBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalance with energy for others by http")
  public void test11FreezebalanceOfEnergyForOthers() {
    response = HttpMethed
        .sendCoin(httpnode, fromAddress, receiverResourceAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);

    //Freeze balance with energy for others
    response = HttpMethed.freezeBalance(httpnode, freezeBalanceAddress, frozenBalance, 0,
        1, receiverResourceAddress, freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(berforeBalance - afterBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalance with energy for others by http")
  public void test12UnFreezebalanceOfEnergyForOthers() {
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);

    //UnFreeze balance with energy for others
    response = HttpMethed.unFreezeBalance(httpnode, freezeBalanceAddress, 1,
        receiverResourceAddress, freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(afterBalance - berforeBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }
}
