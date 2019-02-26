package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
public class HttpTestAccount003 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);
  private final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witness1Address = PublicMethed.getFinalAddress(witnessKey001);
  private final String witnessKey002 = Configuration.getByPath("testng.conf")
      .getString("witness.key2");
  private final byte[] witness2Address = PublicMethed.getFinalAddress(witnessKey002);


  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] updateAccountAddress = ecKey1.getAddress();
  String updateAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  Long amount = 50000000L;
  private static String updateAccountName = "updateAccount_"
      + Long.toString(System.currentTimeMillis());
  private static String updateUrl = "http://www.update.url" + Long.toString(System.currentTimeMillis());

  JsonArray voteKeys = new JsonArray();
  JsonObject voteElement = new JsonObject();



  /**
   * constructor.
   */
  @Test(enabled = true, description = "Update account by http")
  public void test1UpdateAccount() {
    response = HttpMethed.sendCoin(httpnode, fromAddress, updateAccountAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));

    HttpMethed.waitToProduceOneBlock(httpnode);

    response = HttpMethed.updateAccount(httpnode, updateAccountAddress,updateAccountName,
        updateAccountKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));


    response = HttpMethed.getAccount(httpnode, updateAccountAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getString("account_name")
        .equalsIgnoreCase(HttpMethed.str2hex(updateAccountName)));

    Assert.assertFalse(responseContent.getString("active_permission").isEmpty());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Vote witness account by http")
  public void test2VoteWitnessAccount() {
    //Freeze balance
    response = HttpMethed.freezeBalance(httpnode,updateAccountAddress,40000000L,0,
        0,updateAccountKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));

    voteElement.addProperty("vote_address",ByteArray.toHexString(witness1Address));
    voteElement.addProperty("vote_count",11);
    voteKeys.add(voteElement);

    voteElement.remove("vote_address");
    voteElement.remove("vote_count");
    voteElement.addProperty("vote_address",ByteArray.toHexString(witness2Address));
    voteElement.addProperty("vote_count",12);
    voteKeys.add(voteElement);

    //voteKeys.getAsString();

    response = HttpMethed.voteWitnessAccount(httpnode,updateAccountAddress,voteKeys,updateAccountKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));

    response = HttpMethed.getAccount(httpnode, updateAccountAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(!responseContent.getString("votes").isEmpty());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "List witnesses by http")
  public void test3ListWitness() {
    response = HttpMethed.listwitnesses(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("witnesses"));
    Assert.assertTrue(jsonArray.size() >= 2);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Update witness by http")
  public void test4UpdateWitness() {
    response = HttpMethed.updateWitness(httpnode,witness1Address,updateUrl,witnessKey001);
    Assert.assertTrue(HttpMethed.verificationResult(response));

    HttpMethed.waitToProduceOneBlock(httpnode);

    response = HttpMethed.listwitnesses(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    logger.info("result is " + responseContent.getString("witnesses").indexOf(updateUrl));
  }





  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }
}
