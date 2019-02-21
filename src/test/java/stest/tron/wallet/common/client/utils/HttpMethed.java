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
import org.tron.common.utils.ByteArray;
import stest.tron.wallet.common.client.Configuration;

@Slf4j
public class HttpMethed {
  static  HttpClient httpClient = new DefaultHttpClient();
  static HttpPost httppost;
  static HttpResponse response;
  static Integer connectionTimeout = Configuration.getByPath("testng.conf")
      .getInt("defaultParameter.httpConnectionTimeout");
  static Integer soTimeout = Configuration.getByPath("testng.conf")
      .getInt("defaultParameter.httpSoTimeout");

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


}
