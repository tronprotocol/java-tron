package org.tron.core.services.http;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.tron.common.utils.client.utils.HttpMethed.createRequest;

import com.alibaba.fastjson.JSONObject;
import java.io.UnsupportedEncodingException;
import javax.annotation.Resource;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;

public class GetMemoFeePricesServletTest extends BaseTest {

  @Resource
  private GetMemoFeePricesServlet getMemoFeePricesServlet;

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d",dbPath()}, Constant.TEST_CONF);
  }

  @Test
  public void testGet() {
    MockHttpServletRequest request = createRequest(HttpGet.METHOD_NAME);
    MockHttpServletResponse response = new MockHttpServletResponse();
    getMemoFeePricesServlet.doPost(request, response);
    try {
      String contentAsString = response.getContentAsString();
      JSONObject result = JSONObject.parseObject(contentAsString);
      assertTrue(result.containsKey("prices"));
    } catch (UnsupportedEncodingException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testPost() {
    MockHttpServletRequest request = createRequest(HttpPost.METHOD_NAME);
    try {
      MockHttpServletResponse response = new MockHttpServletResponse();
      getMemoFeePricesServlet.doPost(request, response);
      String contentAsString = response.getContentAsString();
      JSONObject result = JSONObject.parseObject(contentAsString);
      assertTrue(result.containsKey("prices"));
    } catch (UnsupportedEncodingException e) {
      fail(e.getMessage());
    }
  }
}
