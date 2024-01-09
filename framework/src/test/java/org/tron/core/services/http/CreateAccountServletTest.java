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


public class CreateAccountServletTest extends BaseTest {

  static {
    Args.setParam(
            new String[]{
                "--output-directory", dbPath(),
            }, Constant.TEST_CONF
    );
  }

  @Resource
  private CreateAccountServlet createAccountServlet;

  @Test
  public void testCreate() {
    String jsonParam = "{"
            + "\"owner_address\": \"41d1e7a6bc354106cb410e65ff8b181c600ff14292\","
            + "\"account_address\": \"41e552f6487585c2b58bc2c9bb4492bc1f17132cd0\""
            + "}";
    MockHttpServletRequest request = createRequest(HttpPost.METHOD_NAME);
    request.setContentType("application/json");
    request.setContent(jsonParam.getBytes(UTF_8));

    MockHttpServletResponse response = new MockHttpServletResponse();
    createAccountServlet.doPost(request, response);

    Assert.assertEquals(200, response.getStatus());
  }
}
