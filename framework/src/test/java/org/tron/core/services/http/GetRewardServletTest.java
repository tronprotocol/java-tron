package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;

import java.io.UnsupportedEncodingException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class GetRewardServletTest {

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @Before
  public void setUp() {
    request = new MockHttpServletRequest();
    request.setMethod("POST");
    request.setContentType("application/x-www-form-urlencoded");
    request.setCharacterEncoding("UTF-8");

    response = new MockHttpServletResponse();
  }

  @Test
  public void getRewardTest() {
    request.addParameter("address", "");
    GetRewardServlet getRewardServlet = new GetRewardServlet();
    getRewardServlet.doPost(request, response);
    try {
      String contentAsString = response.getContentAsString();
      JSONObject result = JSONObject.parseObject(contentAsString);
      int reward = (int)result.get("reward");
      Assert.assertEquals(0, reward);
      String content = (String) result.get("Error");
      Assert.assertNull(content);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }
}
