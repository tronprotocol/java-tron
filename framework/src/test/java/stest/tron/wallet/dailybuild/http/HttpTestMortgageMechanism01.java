package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
import java.math.BigInteger;
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
public class HttpTestMortgageMechanism01 {

  private static final long now = System.currentTimeMillis();
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String witnessKey = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress = PublicMethed.getFinalAddress(witnessKey);
  private final String witnessKey2 = Configuration.getByPath("testng.conf")
      .getString("witness.key2");
  private final byte[] witnessAddress2 = PublicMethed.getFinalAddress(witnessKey2);
  Long amount = 2048000000L;
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf").getString("defaultParameter.assetUrl");
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(1);
  private String httpSoliditynode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private String httpPbftNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(4);
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetBrokerage by http")
  public void test01GetBrokerage() {
    response = HttpMethed.getBrokerage(httpnode, witnessAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals("20", responseContent.getString("brokerage"));

    response = HttpMethed.getBrokerageOnVisible(httpnode, witnessAddress2, "true");
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals("20", responseContent.getString("brokerage"));

    response = HttpMethed.getBrokerageOnVisible(httpnode, fromAddress, "false");
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals("20", responseContent.getString("brokerage"));
  }

  @Test(enabled = true, description = "GetBrokerage from solidity by http")
  public void test02GetBrokerageFromSolidity() {
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethed.getBrokerageFromSolidity(httpSoliditynode, witnessAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals("20", responseContent.getString("brokerage"));

    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethed
        .getBrokerageFromSolidityOnVisible(httpSoliditynode, witnessAddress2, "true");
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals("20", responseContent.getString("brokerage"));

    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethed.getBrokerageFromSolidityOnVisible(httpSoliditynode, fromAddress, "false");
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals("20", responseContent.getString("brokerage"));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetBrokerage from PBFT by http")
  public void test03GetBrokerageFromPbft() {
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethed.getBrokerageFromPbft(httpPbftNode, witnessAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals("20", responseContent.getString("brokerage"));
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "UpdateBrokerage by http")
  public void test04UpdateBrokerage() {
    response = HttpMethed.sendCoin(httpnode, fromAddress, witnessAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    //update brokerage
    response = HttpMethed.updateBrokerage(httpnode, witnessAddress, 11L, witnessKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    response = HttpMethed.sendCoin(httpnode, fromAddress, witnessAddress2, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    //update brokerage onvisible true
    response = HttpMethed
        .updateBrokerageOnVisible(httpnode, witnessAddress2, 24L, witnessKey2, "true");
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    //update brokerage onvisible false
    response = HttpMethed
        .updateBrokerageOnVisible(httpnode, witnessAddress, 88L, witnessKey, "false");
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    //update brokerage onvisible false for notwitness
    response = HttpMethed.sendCoin(httpnode, fromAddress, dev001Address, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    response = HttpMethed.updateBrokerageOnVisible(httpnode, dev001Address, 78L, dev001Key, "true");
    Assert.assertFalse(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetReward by http")
  public void test05GetReward() {
    response = HttpMethed.getReward(httpnode, witnessAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(
        (new BigInteger(responseContent.getString("reward")).compareTo(new BigInteger("0"))) == 1);

    response = HttpMethed.getRewardOnVisible(httpnode, witnessAddress2, "true");
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(
        (new BigInteger(responseContent.getString("reward")).compareTo(new BigInteger("0"))) == 1);

    response = HttpMethed.getRewardOnVisible(httpnode, witnessAddress, "false");
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(
        (new BigInteger(responseContent.getString("reward")).compareTo(new BigInteger("0"))) == 1);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetReward from solidity by http")
  public void test06GetRewardFromSolidity() {
    response = HttpMethed.getRewardFromSolidity(httpSoliditynode, witnessAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(
        (new BigInteger(responseContent.getString("reward")).compareTo(new BigInteger("0"))) == 1);

    response = HttpMethed.getRewardFromSolidityOnVisible(httpSoliditynode, witnessAddress, "true");
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(
        (new BigInteger(responseContent.getString("reward")).compareTo(new BigInteger("0"))) == 1);

    response = HttpMethed.getRewardFromSolidityOnVisible(httpSoliditynode, witnessAddress, "false");
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(
        (new BigInteger(responseContent.getString("reward")).compareTo(new BigInteger("0"))) == 1);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetReward from PBFT by http")
  public void test07GetRewardFromPbft() {
    response = HttpMethed.getRewardFromPbft(httpPbftNode, witnessAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(
        (new BigInteger(responseContent.getString("reward")).compareTo(new BigInteger("0")) == 0)
            || (new BigInteger(responseContent.getString("reward")).compareTo(new BigInteger("0")))
            == 1);
  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    //update brokerage
    HttpMethed.freedResource(httpnode, witnessAddress, fromAddress, witnessKey);
    HttpMethed.disConnect();
  }
}