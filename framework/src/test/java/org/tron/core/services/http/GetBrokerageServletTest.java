package org.tron.core.services.http;

import java.io.UnsupportedEncodingException;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.core.config.args.Args;
import org.tron.json.JSONObject;

public class GetBrokerageServletTest extends BaseTest {

  @Resource
  private  GetBrokerageServlet getBrokerageServlet;

  static {
    Args.setParam(
            new String[]{
                "--output-directory", dbPath(),
            }, TestConstants.TEST_CONF
    );
  }

  public MockHttpServletRequest createRequest(String contentType) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("POST");
    request.setContentType(contentType);
    request.setCharacterEncoding("UTF-8");
    return request;
  }

  @Test
  public void getBrokerageValueByJsonTest() {
    int expect = 20;
    String jsonParam = "{\"address\": \"TGSzEq4t7oMTRcn1VxDghRu5r5bWAE5D1W\"}";
    MockHttpServletRequest request = createRequest("application/json");
    request.setContent(jsonParam.getBytes());
    MockHttpServletResponse response = new MockHttpServletResponse();
    getBrokerageServlet.doPost(request, response);
    try {
      String contentAsString = response.getContentAsString();
      JSONObject result = JSONObject.parseObject(contentAsString);
      int brokerage = (int)result.get("brokerage");
      Assert.assertEquals(expect, brokerage);
    } catch (UnsupportedEncodingException e) {
      Assert.fail(e.getMessage());
    }
  }


  @Test
  public void getBrokerageByJsonUTF8Test() {
    int expect = 20;
    String jsonParam = "{\"address\": \"TGSzEq4t7oMTRcn1VxDghRu5r5bWAE5D1W\"}";
    MockHttpServletRequest request = createRequest("application/json; charset=utf-8");
    request.setContent(jsonParam.getBytes());
    MockHttpServletResponse response = new MockHttpServletResponse();
    getBrokerageServlet.doPost(request, response);
    try {
      String contentAsString = response.getContentAsString();
      JSONObject result = JSONObject.parseObject(contentAsString);
      int brokerage = (int)result.get("brokerage");
      Assert.assertEquals(expect, brokerage);
    } catch (UnsupportedEncodingException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void getBrokerageValueTest() {
    int expect = 20;
    MockHttpServletRequest request = createRequest("application/x-www-form-urlencoded");
    request.addParameter("address", "TGSzEq4t7oMTRcn1VxDghRu5r5bWAE5D1W");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getBrokerageServlet.doPost(request, response);
    try {
      String contentAsString = response.getContentAsString();
      JSONObject result = JSONObject.parseObject(contentAsString);
      int brokerage = (int)result.get("brokerage");
      Assert.assertEquals(expect, brokerage);
    } catch (UnsupportedEncodingException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void getByBlankParamTest() {
    int expect = 0;
    MockHttpServletRequest request = createRequest("application/x-www-form-urlencoded");
    request.addParameter("address", "");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getBrokerageServlet.doPost(request, response);
    try {
      String contentAsString = response.getContentAsString();
      JSONObject result = JSONObject.parseObject(contentAsString);
      int brokerage = (int)result.get("brokerage");
      Assert.assertEquals(expect, brokerage);
      String content = (String) result.get("Error");
      Assert.assertNull(content);
    } catch (UnsupportedEncodingException e) {
      Assert.fail(e.getMessage());
    }
  }
}
