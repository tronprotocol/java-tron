package org.tron.core.services.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.tron.common.utils.client.utils.HttpMethed.createRequest;

import javax.annotation.Resource;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;

public class GetBlockByIdServletTest extends BaseTest {

  @Resource
  private GetBlockByIdServlet getBlockByIdServlet;

  static {
    Args.setParam(
          new String[]{
              "--output-directory", dbPath(),
          }, Constant.TEST_CONF
    );
  }

  @Test
  public void testGetBlockById() {
    String jsonParam = "{\"value\": "
            + "\"0000000002951a2f65db6725c2d0583f1ab9bdb1520eeedece99d9c98f3\"}";
    MockHttpServletRequest request = createRequest(HttpPost.METHOD_NAME);
    request.setContentType("application/json");
    request.setContent(jsonParam.getBytes(UTF_8));
    MockHttpServletResponse response = new MockHttpServletResponse();
    getBlockByIdServlet.doPost(request, response);
    Assert.assertEquals(200, response.getStatus());
  }

  @Test
  public void testGet() {
    MockHttpServletRequest request = createRequest(HttpGet.METHOD_NAME);
    request.addParameter("value", "0000000002951a2f65db6725c2d0583f1ab9bdb1520eeedece99d9c98f3");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getBlockByIdServlet.doGet(request, response);
    Assert.assertEquals(200, response.getStatus());
  }
}
