package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;

import java.io.UnsupportedEncodingException;
import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;

public class GetBrokerageServletTest extends BaseTest {

  @Resource
  private  GetBrokerageServlet getBrokerageServlet;

  static {
    Args.setParam(
            new String[]{
                "--output-directory", dbPath(),
            }, Constant.TEST_CONF
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
    String jsonParam = "{\"address\": \"27VZHn9PFZwNh7o2EporxmLkpe157iWZVkh\"}";
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
    String jsonParam = "{\"address\": \"27VZHn9PFZwNh7o2EporxmLkpe157iWZVkh\"}";
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
    request.addParameter("address", "27VZHn9PFZwNh7o2EporxmLkpe157iWZVkh");
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
