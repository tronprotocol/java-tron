package stest.tron.wallet.common.client.utils;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import java.nio.charset.Charset;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.tron.common.utils.ByteArray;
import stest.tron.wallet.common.client.Configuration;

@Slf4j
public class HttpMethed {
  static HttpClient httpClient = new DefaultHttpClient();
  static HttpPost httppost;
  static HttpResponse response;
  static Integer connectionTimeout = Configuration.getByPath("testng.conf")
      .getInt("defaultParameter.httpConnectionTimeout");
  static Integer soTimeout = Configuration.getByPath("testng.conf")
      .getInt("defaultParameter.httpSoTimeout");
  static String transactionString;
  static String transactionSignString;
  static JSONObject responseContent;


  /**
   * constructor.
   */
  public static HttpResponse sendCoin(String httpNode, byte[] fromAddress, byte[] toAddress, 
      Long amount, String fromKey) {
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/createtransaction";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("to_address", ByteArray.toHexString(toAddress));
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(fromAddress));
      userBaseObj2.addProperty("amount", amount);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode,transactionString,fromKey);
      response = broadcastTransaction(httpNode,transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static HttpResponse exchangeCreate(String httpNode, byte[] ownerAddress,
      String firstTokenId, Long firstTokenBalance,
      String secondTokenId,Long secondTokenBalance,String fromKey) {
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/exchangecreate";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("first_token_id", str2hex(firstTokenId));
      userBaseObj2.addProperty("first_token_balance", firstTokenBalance);
      userBaseObj2.addProperty("second_token_id", str2hex(secondTokenId));
      userBaseObj2.addProperty("second_token_balance", secondTokenBalance);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode,transactionString,fromKey);
      response = broadcastTransaction(httpNode,transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static HttpResponse exchangeInject(String httpNode, byte[] ownerAddress,
      Integer exchangeId, String tokenId,Long quant,String fromKey) {
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/exchangeinject";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("exchange_id", exchangeId);
      userBaseObj2.addProperty("token_id", str2hex(tokenId));
      userBaseObj2.addProperty("quant", quant);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode,transactionString,fromKey);
      response = broadcastTransaction(httpNode,transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static HttpResponse exchangeWithdraw(String httpNode, byte[] ownerAddress,
      Integer exchangeId, String tokenId,Long quant,String fromKey) {
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/exchangewithdraw";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("exchange_id", exchangeId);
      userBaseObj2.addProperty("token_id", str2hex(tokenId));
      userBaseObj2.addProperty("quant", quant);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode,transactionString,fromKey);
      response = broadcastTransaction(httpNode,transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static HttpResponse exchangeTransaction(String httpNode, byte[] ownerAddress,
      Integer exchangeId, String tokenId,Long quant,Long expected, String fromKey) {
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/exchangetransaction";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("exchange_id", exchangeId);
      userBaseObj2.addProperty("token_id", str2hex(tokenId));
      userBaseObj2.addProperty("quant", quant);
      userBaseObj2.addProperty("expected", expected);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode,transactionString,fromKey);
      response = broadcastTransaction(httpNode,transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }



  /**
   * constructor.
   */
  public static HttpResponse assetIssue(String httpNode, byte[] ownerAddress, String name,
      String abbr, Long totalSupply,Integer trxNum, Integer num,Long startTime, Long endTime,
      Integer voteScore, Integer precision, String description, String url,Long freeAssetNetLimit,
      Long publicFreeAssetNetLimit,String fromKey) {
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/createassetissue";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("name", str2hex(name));
      userBaseObj2.addProperty("abbr", str2hex(abbr));
      userBaseObj2.addProperty("total_supply", totalSupply);
      userBaseObj2.addProperty("trx_num", trxNum);
      userBaseObj2.addProperty("num", num);
      userBaseObj2.addProperty("precision", precision);
      userBaseObj2.addProperty("start_time", startTime);
      userBaseObj2.addProperty("end_time", endTime);
      userBaseObj2.addProperty("vote_score", voteScore);
      userBaseObj2.addProperty("description", str2hex(description));
      userBaseObj2.addProperty("url", str2hex(url));
      userBaseObj2.addProperty("free_asset_net_limit", freeAssetNetLimit);
      userBaseObj2.addProperty("public_free_asset_net_limit", publicFreeAssetNetLimit);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode,transactionString,fromKey);
      response = broadcastTransaction(httpNode,transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static HttpResponse transferAsset(String httpNode, byte[] ownerAddress,
      byte[] toAddress, String assetIssueById, Long amount, String fromKey) {
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/transferasset";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("to_address", ByteArray.toHexString(toAddress));
      userBaseObj2.addProperty("asset_name", str2hex(assetIssueById));
      userBaseObj2.addProperty("amount", amount);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode,transactionString,fromKey);
      response = broadcastTransaction(httpNode,transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static HttpResponse participateAssetIssue(String httpNode, byte[] toAddress,
      byte[] ownerAddress, String assetIssueById, Long amount, String fromKey) {
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/participateassetissue";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("to_address", ByteArray.toHexString(toAddress));
      userBaseObj2.addProperty("asset_name", str2hex(assetIssueById));
      userBaseObj2.addProperty("amount", amount);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      logger.info(transactionString);
      transactionSignString = gettransactionsign(httpNode,transactionString,fromKey);
      logger.info(transactionSignString);
      response = broadcastTransaction(httpNode,transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static Boolean verificationResult(HttpResponse response) {
    if (response.getStatusLine().getStatusCode() != 200) {
      return false;
    }
    Assert.assertEquals(response.getStatusLine().getStatusCode(),200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    return Boolean.valueOf(responseContent.getString("result")).booleanValue();
  }

  /**
   * constructor.
   */
  public static HttpResponse freezeBalance(String httpNode, byte[] ownerAddress,
      Long frozenBalance, Integer frozenDuration,Integer resourceCode, String fromKey) {
    return freezeBalance(httpNode,ownerAddress,frozenBalance,frozenDuration,resourceCode,
        null,fromKey);
  }

  /**
   * constructor.
   */
  public static HttpResponse freezeBalance(String httpNode, byte[] ownerAddress,
      Long frozenBalance, Integer frozenDuration,Integer resourceCode,byte[] receiverAddress,
      String fromKey) {
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/freezebalance";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("frozen_balance", frozenBalance);
      userBaseObj2.addProperty("frozen_duration", frozenDuration);
      if (resourceCode == 0) {
        userBaseObj2.addProperty("resource", "BANDWIDTH");
      }
      if (resourceCode == 1) {
        userBaseObj2.addProperty("resource", "ENERGY");
      }
      if (receiverAddress != null) {
        userBaseObj2.addProperty("receiver_address", ByteArray.toHexString(receiverAddress));
      }
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode,transactionString,fromKey);
      response = broadcastTransaction(httpNode,transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static HttpResponse unFreezeBalance(String httpNode, byte[] ownerAddress,
      Integer resourceCode,String fromKey) {
    return unFreezeBalance(httpNode,ownerAddress,resourceCode,null,fromKey);
  }

  /**
   * constructor.
   */
  public static HttpResponse unFreezeBalance(String httpNode, byte[] ownerAddress,
      Integer resourceCode,byte[] receiverAddress,String fromKey) {
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/unfreezebalance";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      if (resourceCode == 0) {
        userBaseObj2.addProperty("resource", "BANDWIDTH");
      }
      if (resourceCode == 1) {
        userBaseObj2.addProperty("resource", "ENERGY");
      }
      if (receiverAddress != null) {
        userBaseObj2.addProperty("receiver_address", ByteArray.toHexString(receiverAddress));
      }
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode,transactionString,fromKey);
      response = broadcastTransaction(httpNode,transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static String gettransactionsign(String httpNode,String transactionString,
      String privateKey) {
    try {
      String requestUrl = "http://" + httpNode + "/wallet/gettransactionsign";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("transaction", transactionString);
      userBaseObj2.addProperty("privateKey", privateKey);
      response = createConnect(requestUrl, userBaseObj2);
      transactionSignString = EntityUtils.toString(response.getEntity());
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return transactionSignString;
  }

  /**
   * constructor.
   */
  public static HttpResponse broadcastTransaction(String httpNode,String transactionSignString) {
    try {
      String requestUrl = "http://" + httpNode + "/wallet/broadcasttransaction";
      httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,
          connectionTimeout);
      httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, soTimeout);
      httppost = new HttpPost(requestUrl);
      httppost.setHeader("Content-type", "application/json; charset=utf-8");
      httppost.setHeader("Connection", "Close");
      if (transactionSignString != null) {
        StringEntity entity = new StringEntity(transactionSignString, Charset.forName("UTF-8"));
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        httppost.setEntity(entity);
      }
      response = httpClient.execute(httppost);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    httppost.releaseConnection();
    return response;
  }

  /**
   * constructor.
   */
  public static HttpResponse getAccount(String httpNode, byte[] queryAddress) {
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getaccount";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("address", ByteArray.toHexString(queryAddress));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static HttpResponse listExchanges(String httpNode) {
    try {
      String requestUrl = "http://" + httpNode + "/wallet/listexchanges";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static HttpResponse getExchangeById(String httpNode, Integer exchangeId) {
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getexchangebyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("id", exchangeId);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }


  /**
   * constructor.
   */
  public static HttpResponse getAssetIssueById(String httpNode, String assetIssueId) {
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getassetissuebyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", assetIssueId);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static HttpResponse getAssetIssueByName(String httpNode, String name) {
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getassetissuebyname";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", str2hex(name));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }


  /**
   * constructor.
   */
  public static Long getBalance(String httpNode, byte[] queryAddress) {
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getaccount";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("address", ByteArray.toHexString(queryAddress));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    responseContent = HttpMethed.parseResponseContent(response);
    //HttpMethed.printJsonContent(responseContent);
    //httppost.releaseConnection();
    return Long.parseLong(responseContent.get("balance").toString());
  }


  /**
   * constructor.
   */
  public static HttpResponse getAccountNet(String httpNode, byte[] queryAddress) {
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getaccountnet";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("address", ByteArray.toHexString(queryAddress));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static HttpResponse getAccountReource(String httpNode, byte[] queryAddress) {
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getaccountresource";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("address", ByteArray.toHexString(queryAddress));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static HttpResponse getNowBlock(String httpNode) {
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getnowblock";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static void waitToProduceOneBlock(String httpNode) {
    response = HttpMethed.getNowBlock(httpNode);
    responseContent = HttpMethed.parseResponseContent(response);
    responseContent = HttpMethed.parseStringContent(responseContent.get("block_header").toString());
    responseContent = HttpMethed.parseStringContent(responseContent.get("raw_data").toString());
    Integer currentBlockNum = Integer.parseInt(responseContent.get("number").toString());
    Integer nextBlockNum = 0;
    Integer times = 0;
    while (nextBlockNum <= currentBlockNum && times++ <= 3) {
      response = HttpMethed.getNowBlock(httpNode);
      responseContent = HttpMethed.parseResponseContent(response);
      responseContent = HttpMethed.parseStringContent(responseContent.get("block_header")
          .toString());
      responseContent = HttpMethed.parseStringContent(responseContent.get("raw_data").toString());
      nextBlockNum = Integer.parseInt(responseContent.get("number").toString());
      try {
        Thread.sleep(3500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }


  /**
   * constructor.
   */
  public static HttpResponse getBlockByNum(String httpNode,Integer blockNUm) {
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getblockbynum";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("num", blockNUm);
      response = createConnect(requestUrl,userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static HttpResponse getBlockByLimitNext(String httpNode,Integer startNum,Integer endNum) {
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getblockbylimitnext";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("startNum", startNum);
      userBaseObj2.addProperty("endNum", endNum);
      response = createConnect(requestUrl,userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static HttpResponse getBlockByLastNum(String httpNode,Integer num) {
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getblockbylatestnum";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("num", num);
      response = createConnect(requestUrl,userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static HttpResponse getBlockById(String httpNode,String blockId) {
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getblockbyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", blockId);
      response = createConnect(requestUrl,userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }


  /**
   * constructor.
   */
  public  static HttpResponse createConnect(String url) {
    return createConnect(url,null);
  }

  /**
   * constructor.
   */
  public  static HttpResponse createConnect(String url, JsonObject requestBody) {
    try {
      httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,
          connectionTimeout);
      httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, soTimeout);
      httppost = new HttpPost(url);
      httppost.setHeader("Content-type", "application/json; charset=utf-8");
      httppost.setHeader("Connection", "Close");
      if (requestBody != null) {
        StringEntity entity = new StringEntity(requestBody.toString(), Charset.forName("UTF-8"));
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        httppost.setEntity(entity);
      }
      response = httpClient.execute(httppost);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }


  /**
   * constructor.
   */
  public  static void disConnect() {
    httppost.releaseConnection();
  }

  /**
   * constructor.
   */
  public static JSONObject parseResponseContent(HttpResponse response) {
    try {
      String result = EntityUtils.toString(response.getEntity());
      JSONObject obj = JSONObject.parseObject(result);
      return obj;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * constructor.
   */
  public static JSONObject parseStringContent(String content) {
    try {
      JSONObject obj = JSONObject.parseObject(content);
      return obj;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * constructor.
   */
  public static void printJsonContent(JSONObject responseContent) {
    logger.info("----------------------------Print JSON Start---------------------------");
    for (String str : responseContent.keySet()) {
      logger.info(str + ":" + responseContent.get(str));
    }
    logger.info("JSON content size are: " + responseContent.size());
    logger.info("----------------------------Print JSON End-----------------------------");
  }

  /**
   * constructor.
   */
  public static String str2hex(String str) {
    char[] chars = "0123456789ABCDEF".toCharArray();
    StringBuilder sb = new StringBuilder("");
    byte[] bs = str.getBytes();
    int bit;
    for (int i = 0; i < bs.length; i++) {
      bit = (bs[i] & 0x0f0) >> 4;
      sb.append(chars[bit]);
      bit = bs[i] & 0x0f;
      sb.append(chars[bit]);
      // sb.append(' ');
    }
    return sb.toString().trim();
  }
}
