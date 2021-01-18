package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class HttpTestBlock001 {

  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);
  private String httpSoliditynode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private String httpPbftNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(4);
  private Integer currentBlockNum;
  private JSONObject blockContent;
  private JSONObject blockContentWithVisibleTrue;
  private String blockId;


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get now block by http")
  public void get01NowBlock() {
    response = HttpMethed.getNowBlock(httpnode);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    blockContent = responseContent;
    blockId = responseContent.get("blockID").toString();
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() >= 2);
    responseContent = HttpMethed.parseStringContent(responseContent.get("block_header").toString());
    Assert.assertTrue(responseContent.size() >= 2);
    Assert.assertFalse(responseContent.get("witness_signature").toString().isEmpty());
    HttpMethed.printJsonContent(responseContent);
    responseContent = HttpMethed.parseStringContent(responseContent.get("raw_data").toString());
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(Integer.parseInt(responseContent.get("number").toString()) > 0);
    currentBlockNum = Integer.parseInt(responseContent.get("number").toString());
    Assert.assertTrue(Long.parseLong(responseContent.get("timestamp").toString()) > 1550724114000L);
    Assert.assertFalse(responseContent.get("witness_address").toString().isEmpty());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get now block from solidity by http")
  public void get02NowBlockFromSolidity() {
    response = HttpMethed.getNowBlockFromSolidity(httpSoliditynode);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    blockContent = responseContent;
    blockId = responseContent.get("blockID").toString();
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() >= 2);
    responseContent = HttpMethed.parseStringContent(responseContent.get("block_header").toString());
    Assert.assertTrue(responseContent.size() >= 2);
    Assert.assertFalse(responseContent.get("witness_signature").toString().isEmpty());
    HttpMethed.printJsonContent(responseContent);
    responseContent = HttpMethed.parseStringContent(responseContent.get("raw_data").toString());
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(Integer.parseInt(responseContent.get("number").toString()) > 0);
    currentBlockNum = Integer.parseInt(responseContent.get("number").toString());
    Assert.assertTrue(Long.parseLong(responseContent.get("timestamp").toString()) > 1550724114000L);
    Assert.assertFalse(responseContent.get("witness_address").toString().isEmpty());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get now block from pbft by http")
  public void get03NowBlockFromPbft() {
    response = HttpMethed.getNowBlockFromPbft(httpPbftNode);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    blockContent = responseContent;
    blockId = responseContent.get("blockID").toString();
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() >= 2);
    responseContent = HttpMethed.parseStringContent(responseContent.get("block_header").toString());
    Assert.assertTrue(responseContent.size() >= 2);
    Assert.assertFalse(responseContent.get("witness_signature").toString().isEmpty());
    HttpMethed.printJsonContent(responseContent);
    responseContent = HttpMethed.parseStringContent(responseContent.get("raw_data").toString());
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(Integer.parseInt(responseContent.get("number").toString()) > 0);
    currentBlockNum = Integer.parseInt(responseContent.get("number").toString());
    Assert.assertTrue(Long.parseLong(responseContent.get("timestamp").toString()) > 1550724114000L);
    Assert.assertFalse(responseContent.get("witness_address").toString().isEmpty());
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get block by num by http")
  public void get04BlockByNum() {
    response = HttpMethed.getBlockByNum(httpnode, currentBlockNum);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    Assert.assertEquals(responseContent, blockContent);

    //visible=true
    response = HttpMethed.getBlockByNum(httpnode, currentBlockNum, true);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    Assert.assertEquals(responseContent.getString("blockID"),
        blockContent.getString("blockID"));

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get block by num from solidity by http")
  public void get05BlockByNumFromSolidity() {
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethed.getBlockByNumFromSolidity(httpSoliditynode, currentBlockNum);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    Assert.assertEquals(responseContent, blockContent);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get block by num from PBFT by http")
  public void get06BlockByNumFromPbft() {
    response = HttpMethed.getBlockByNumFromPbft(httpPbftNode, currentBlockNum);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    Assert.assertEquals(responseContent, blockContent);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetBlockByLimitNext by http")
  public void get07BlockByLimitNext() {
    response = HttpMethed.getBlockByLimitNext(httpnode, currentBlockNum - 10, currentBlockNum);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    logger.info(responseContent.get("block").toString());
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("block").toString());
    Assert.assertEquals(jsonArray.size(), 10);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetBlockByLastNum by http")
  public void get08BlockByLastNum() {
    response = HttpMethed.getBlockByLastNum(httpnode, 8);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    logger.info(responseContent.get("block").toString());
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("block").toString());
    Assert.assertEquals(jsonArray.size(), 8);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetBlockById by http")
  public void get09BlockById() {
    response = HttpMethed.getBlockById(httpnode, blockId);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(blockId, responseContent.get("blockID").toString());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetBlockById by Solidity http")
  public void get10BlockByIdFromSolidity() {
    response = HttpMethed.getBlockByIdFromSolidity(httpSoliditynode, blockId);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(blockId, responseContent.get("blockID").toString());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetBlockById by PBFT http")
  public void get11BlockByIdFromPbft() {
    response = HttpMethed.getBlockByIdFromPbft(httpPbftNode, blockId);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(blockId, responseContent.get("blockID").toString());
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "List nodes by http")
  public void get12ListNodes() {
    response = HttpMethed.listNodes(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "get next maintenance time by http")
  public void get13NextMaintaenanceTime() {
    response = HttpMethed.getNextmaintenanceTime(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertFalse(responseContent.get("num").toString().isEmpty());
    Assert.assertTrue(responseContent.getLong("num") >= System.currentTimeMillis());
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "get chain parameter by http")
  public void get14ChainParameter() {
    response = HttpMethed.getChainParameter(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("chainParameter").toString());
    Assert.assertTrue(jsonArray.size() >= 26);
    Boolean exsistDelegated = false;
    for (int i = 0; i < jsonArray.size(); i++) {
      if (jsonArray.getJSONObject(i).getString("key").equals("getAllowDelegateResource")) {
        exsistDelegated = true;
        Assert.assertTrue(jsonArray.getJSONObject(i).getString("value").equals("1"));
      }
    }
    Assert.assertTrue(exsistDelegated);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "get Node Info by http")
  public void get15NodeInfo() {
    response = HttpMethed.getNodeInfo(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertFalse(responseContent.get("configNodeInfo").toString().isEmpty());
    Assert.assertTrue(responseContent.getString("configNodeInfo").contains("\"dbVersion\":2"));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction count by blocknum from solidity by http")
  public void get16TransactionCountByBlocknumFromSolidity() {
    response = HttpMethed
        .getTransactionCountByBlocknumFromSolidity(httpSoliditynode, currentBlockNum);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() == 1);
    Assert.assertTrue(Integer.parseInt(responseContent.get("count").toString()) >= 0);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction count by blocknum from PBFT by http")
  public void get17TransactionCountByBlocknumFromPbft() {
    response = HttpMethed.getTransactionCountByBlocknumFromPbft(httpPbftNode, currentBlockNum);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() == 1);
    Assert.assertTrue(Integer.parseInt(responseContent.get("count").toString()) >= 0);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetBlockByLimitNext by Solidity http")
  public void get18BlockByLimitNextFromSolidity() {
    response = HttpMethed
        .getBlockByLimitNextFromSolidity(httpSoliditynode, currentBlockNum - 10, currentBlockNum);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    logger.info(responseContent.get("block").toString());
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("block").toString());
    Assert.assertEquals(jsonArray.size(), 10);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetBlockByLimitNext by PBFT http")
  public void get19BlockByLimitNextFromPbft() {
    response = HttpMethed
        .getBlockByLimitNextFromPbft(httpPbftNode, currentBlockNum - 10, currentBlockNum);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    logger.info(responseContent.get("block").toString());
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("block").toString());
    Assert.assertEquals(jsonArray.size(), 10);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetBlockByLastNum by solidity http")
  public void get20BlockByLastNumFromSolidity() {
    response = HttpMethed.getBlockByLastNumFromSolidity(httpSoliditynode, 8);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    logger.info(responseContent.get("block").toString());
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("block").toString());
    Assert.assertEquals(jsonArray.size(), 8);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetBlockByLastNum by PBFT http")
  public void get21BlockByLastNumFromPbft() {
    response = HttpMethed.getBlockByLastNumFromPbft(httpPbftNode, 8);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    logger.info(responseContent.get("block").toString());
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("block").toString());
    Assert.assertEquals(jsonArray.size(), 8);
  }


  /**
   * constructor.
   */
  @Test(enabled = false, description = "Get block by num by http")
  public void get16TestResponse() {
    Integer times = 1000;
    String testKey002 = "7400E3D0727F8A61041A8E8BF86599FE5597CE19DE451E59AED07D60967A5E25";
    byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
    Long duration = HttpMethed.getBlockByNumForResponse(httpnode, 4942435, times);
    /*    Long duration = HttpMethed.getAccountForResponse(httpnode, fromAddress, times);*/
    /*    Long duration = HttpMethed.getTransactionByIdForResponse(httpnode,
      "a265fc457551fd9cfa55daec0550268b1a2da54018cc700f1559454836de411c", times);*/
    logger.info("Total duration  : " + duration);
    logger.info("Average duration: " + duration / times);
  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }
}