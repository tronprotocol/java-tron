package org.tron.core.services.http;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.Assert.assertTrue;

import com.alibaba.fastjson.JSONObject;

import java.io.UnsupportedEncodingException;
import javax.annotation.Resource;
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

  public MockHttpServletRequest createRequest(String contentType) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("POST");
    if (isNotEmpty(contentType)) {
      request.setContentType(contentType);
    }
    request.setCharacterEncoding("UTF-8");
    return request;
  }

  @Test
  public void testGetBlockByNum() {
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
