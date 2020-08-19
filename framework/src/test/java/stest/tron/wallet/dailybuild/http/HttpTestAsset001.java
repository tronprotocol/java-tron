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
public class HttpTestAsset001 {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static String name = "testAssetIssue002_" + now;
  private static String assetIssueId;
  private static String updateDescription = "Description_update_" + now;
  private static String updateUrl = "Url_update_" + now;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] assetAddress = ecKey1.getAddress();
  String assetKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] participateAddress = ecKey2.getAddress();
  String participateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  Long amount = 2048000000L;

  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf").getString("defaultParameter.assetUrl");
  private JSONObject responseContent;
  private JSONObject getAssetIssueByIdContent;
  private JSONObject getAssetIssueByNameContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(1);
  private String httpSoliditynode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private String httpPbftNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(4);


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Create asset issue by http")
  public void test01CreateAssetIssue() {
    response = HttpMethed.sendCoin(httpnode, fromAddress, assetAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed
        .sendCoin(httpnode, fromAddress, participateAddress, 10000000L, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    //Create an asset issue
    response = HttpMethed.assetIssue(httpnode, assetAddress, name, name, totalSupply, 1, 1,
        System.currentTimeMillis() + 5000, System.currentTimeMillis() + 50000000, 2, 3, description,
        url, 1000L, 1000L, assetKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getAccount(httpnode, assetAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    assetIssueId = responseContent.getString("asset_issued_ID");
    logger.info(assetIssueId);
    Assert.assertTrue(Integer.parseInt(assetIssueId) > 1000000);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetAssetIssueById by http")
  public void test02GetAssetIssueById() {
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getAssetIssueById(httpnode, assetIssueId);
    getAssetIssueByIdContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getAssetIssueByIdContent);
    Assert.assertTrue(totalSupply == getAssetIssueByIdContent.getLong("total_supply"));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetAssetIssueById from solidity by http")
  public void test03GetAssetIssueByIdFromSolidity() {
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethed.getAssetIssueByIdFromSolidity(httpSoliditynode, assetIssueId);
    getAssetIssueByIdContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getAssetIssueByIdContent);
    Assert.assertTrue(totalSupply == getAssetIssueByIdContent.getLong("total_supply"));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetAssetIssueById from PBFT by http")
  public void test04GetAssetIssueByIdFromPbft() {
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethed.getAssetIssueByIdFromPbft(httpPbftNode, assetIssueId);
    getAssetIssueByIdContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getAssetIssueByIdContent);
    Assert.assertTrue(totalSupply == getAssetIssueByIdContent.getLong("total_supply"));
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetAssetIssueByName by http")
  public void test05GetAssetIssueByName() {
    response = HttpMethed.getAssetIssueByName(httpnode, name);
    getAssetIssueByNameContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getAssetIssueByNameContent);
    Assert.assertTrue(totalSupply == getAssetIssueByNameContent.getLong("total_supply"));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetAssetIssueByName from solidity by http")
  public void test06GetAssetIssueByNameFromSolidity() {
    response = HttpMethed.getAssetIssueByNameFromSolidity(httpSoliditynode, name);
    getAssetIssueByNameContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getAssetIssueByNameContent);
    Assert.assertTrue(totalSupply == getAssetIssueByNameContent.getLong("total_supply"));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetAssetIssueByName from PBFT by http")
  public void test07GetAssetIssueByNameFromPbft() {
    response = HttpMethed.getAssetIssueByNameFromPbft(httpPbftNode, name);
    getAssetIssueByNameContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getAssetIssueByNameContent);
    Assert.assertTrue(totalSupply == getAssetIssueByNameContent.getLong("total_supply"));
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "TransferAsset by http")
  public void test08TransferAsset() {
    logger.info("Transfer asset.");
    response = HttpMethed
        .transferAsset(httpnode, assetAddress, participateAddress, assetIssueId, 100L, assetKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getAccount(httpnode, participateAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(!responseContent.getString("assetV2").isEmpty());
    //logger.info(responseContent.get("assetV2").toString());

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Participate asset issue by http")
  public void test09ParticipateAssetIssue() {
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed
        .participateAssetIssue(httpnode, assetAddress, participateAddress, assetIssueId, 1000L,
            participateKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getAccount(httpnode, participateAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Update asset issue by http")
  public void test10UpdateAssetIssue() {
    response = HttpMethed
        .updateAssetIssue(httpnode, assetAddress, updateDescription, updateUrl, 290L, 390L,
            assetKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getAssetIssueById(httpnode, assetIssueId);
    getAssetIssueByIdContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getAssetIssueByIdContent);

    Assert.assertTrue(getAssetIssueByIdContent.getLong("public_free_asset_net_limit") == 390L);
    Assert.assertTrue(getAssetIssueByIdContent.getLong("free_asset_net_limit") == 290L);
    Assert.assertTrue(getAssetIssueByIdContent.getString("description")
        .equalsIgnoreCase(HttpMethed.str2hex(updateDescription)));
    Assert.assertTrue(
        getAssetIssueByIdContent.getString("url").equalsIgnoreCase(HttpMethed.str2hex(updateUrl)));
  }


  /**
   * * constructor. *
   */
  @Test(enabled = true, description = "Get asset issue list by http")
  public void test11GetAssetissueList() {

    response = HttpMethed.getAssetissueList(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("assetIssue"));
    Assert.assertTrue(jsonArray.size() >= 1);
  }


  /**
   * * constructor. *
   */
  @Test(enabled = true, description = "Get asset issue list from solidity by http")
  public void test12GetAssetissueListFromSolidity() {
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethed.getAssetIssueListFromSolidity(httpSoliditynode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("assetIssue"));
    Assert.assertTrue(jsonArray.size() >= 1);
  }

  /**
   * * constructor. *
   */
  @Test(enabled = true, description = "Get asset issue list from PBFT by http")
  public void test13GetAssetissueListFromPbft() {
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethed.getAssetIssueListFromPbft(httpPbftNode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("assetIssue"));
    Assert.assertTrue(jsonArray.size() >= 1);
  }


  /**
   * * constructor. *
   */
  @Test(enabled = true, description = "Get paginated asset issue list by http")
  public void test14GetPaginatedAssetissueList() {
    response = HttpMethed.getPaginatedAssetissueList(httpnode, 0, 1);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("assetIssue"));
    Assert.assertTrue(jsonArray.size() == 1);
  }


  /**
   * * constructor. *
   */
  @Test(enabled = true, description = "Get paginated asset issue list from solidity by http")
  public void test15GetPaginatedAssetissueListFromSolidity() {
    response = HttpMethed.getPaginatedAssetissueListFromSolidity(httpSoliditynode, 0, 1);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("assetIssue"));
    Assert.assertTrue(jsonArray.size() == 1);
  }


  /**
   * * constructor. *
   */
  @Test(enabled = true, description = "Get paginated asset issue list from PBFT by http")
  public void test16GetPaginatedAssetissueListFromPbft() {
    response = HttpMethed.getPaginatedAssetissueListFromPbft(httpPbftNode, 0, 1);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("assetIssue"));
    Assert.assertTrue(jsonArray.size() == 1);
  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.freedResource(httpnode, assetAddress, fromAddress, assetKey);
    HttpMethed.freedResource(httpnode, participateAddress, fromAddress, participateKey);
    HttpMethed.disConnect();
  }
}