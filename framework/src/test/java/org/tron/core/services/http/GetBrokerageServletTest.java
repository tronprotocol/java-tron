package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;

import java.io.UnsupportedEncodingException;
import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;

public class GetBrokerageServletTest extends BaseTest {

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @Resource
  private  GetBrokerageServlet getBrokerageServlet;


  static {
    dbPath = "db_GetBrokerageServlet_test";
    Args.setParam(
            new String[]{
                "--output-directory", dbPath,
            }, Constant.TEST_CONF
    );
  }

  @Before
  public void setUp() {
    request = new MockHttpServletRequest();
    request.setMethod("POST");
    request.setContentType("application/x-www-form-urlencoded");
    request.setCharacterEncoding("UTF-8");

    response = new MockHttpServletResponse();
  }

  @Test
  public void getBrokerageValueTest() {
    int expect = 20;
    request.addParameter("address", "27VZHn9PFZwNh7o2EporxmLkpe157iWZVkh");
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
  public void getBrokerageTest() {
    int expect = 0;
    request.addParameter("address", "");
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
