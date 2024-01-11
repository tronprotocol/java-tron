package org.tron.core.services.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.tron.common.utils.client.utils.HttpMethed.createRequest;

import javax.annotation.Resource;
import org.apache.http.client.methods.HttpPost;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;

public class GetTransactionInfoByBlockNumServletTest extends BaseTest {

  @Resource
  private GetTransactionInfoByBlockNumServlet getTransactionInfoByBlockNumServlet;

  static {
    Args.setParam(
            new String[]{
                "--output-directory", dbPath(),
            }, Constant.TEST_CONF
    );
  }

  @Test
  public void testGetTransactionInfoByBlockNum() {
    String jsonParam = "{\"num\" : 100}";
    MockHttpServletRequest request = createRequest(HttpPost.METHOD_NAME);
    request.setContentType("application/json");
    request.setContent(jsonParam.getBytes(UTF_8));
    MockHttpServletResponse response = new MockHttpServletResponse();

    getTransactionInfoByBlockNumServlet.doPost(request, response);
    Assert.assertEquals(200, response.getStatus());
  }
}
