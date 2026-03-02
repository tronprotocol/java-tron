package org.tron.core.services.http;

import static org.junit.Assert.assertTrue;
import static org.tron.common.utils.client.utils.HttpMethed.createRequest;

import com.alibaba.fastjson.JSONObject;

import java.io.UnsupportedEncodingException;
import javax.annotation.Resource;

import org.apache.http.client.methods.HttpPost;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;

public class GetBlockByNumServletTest extends BaseTest {

  @Resource
  private GetBlockByNumServlet getBlockByNumServlet;

  static {
    Args.setParam(
          new String[]{
              "--output-directory", dbPath(),
          }, Constant.TEST_CONF
    );
  }

  @Test
  public void testGetBlockByNum() {
    String jsonParam = "{\"number\": 1}";
    MockHttpServletRequest request = createRequest(HttpPost.METHOD_NAME);
    request.setContentType("application/json");
    request.setContent(jsonParam.getBytes());
    MockHttpServletResponse response = new MockHttpServletResponse();

    try {
      getBlockByNumServlet.doPost(request, response);
      String contentAsString = response.getContentAsString();
      JSONObject result = JSONObject.parseObject(contentAsString);
      assertTrue(result.containsKey("blockID"));
      assertTrue(result.containsKey("transactions"));
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testGet() {
    String jsonParam = "{\"number\": 1}";
    MockHttpServletRequest request = createRequest("application/json");
    request.setContent(jsonParam.getBytes());
    MockHttpServletResponse response = new MockHttpServletResponse();

    try {
      getBlockByNumServlet.doPost(request, response);
      String contentAsString = response.getContentAsString();
      JSONObject result = JSONObject.parseObject(contentAsString);
      assertTrue(result.containsKey("blockID"));
      assertTrue(result.containsKey("transactions"));
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }

}
