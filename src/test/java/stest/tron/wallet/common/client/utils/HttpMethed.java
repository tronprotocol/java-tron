package stest.tron.wallet.common.client.utils;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import java.nio.charset.Charset;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.tron.common.utils.ByteArray;


public class HttpMethed {
  static  HttpClient httpClient = new DefaultHttpClient();
  static HttpPost httppost;
  static HttpResponse response;

  /**
   * constructor.
   */
  public static HttpResponse getAccount(String httpNode, byte[] queryAddress) {
    httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 2000);
    httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 2000);
    try {
      String getAccountUrl = "http://" + httpNode + "/wallet/getaccount";
      httppost = new HttpPost(getAccountUrl);
      httppost.setHeader("Content-type", "application/json; charset=utf-8");
      httppost.setHeader("Connection", "Close");
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("address", ByteArray.toHexString(queryAddress));
      StringEntity entity = new StringEntity(userBaseObj2.toString(), Charset.forName("UTF-8"));
      entity.setContentEncoding("UTF-8");
      entity.setContentType("application/json");
      httppost.setEntity(entity);
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

}
