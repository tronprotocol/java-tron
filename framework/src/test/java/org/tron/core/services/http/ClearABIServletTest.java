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

public class ClearABIServletTest extends BaseTest {

  static {
    Args.setParam(
            new String[]{
                "--output-directory", dbPath(),
            }, Constant.TEST_CONF
    );
  }

  @Resource
  private ClearABIServlet clearABIServlet;

  @Test
  public void testClear() {
    String jsonParam = "{\n"
            + "    \"owner_address\": \"41a7d8a35b260395c14aa456297662092ba3b76fc0\",\n"
            + "    \"contract_address\": \"417bcb781f4743afaacf9f9528f3ea903b3782339f\"\n"
            + "}";
    MockHttpServletRequest request = createRequest(HttpPost.METHOD_NAME);
    request.setContentType("application/json");
    request.setContent(jsonParam.getBytes(UTF_8));

    MockHttpServletResponse response = new MockHttpServletResponse();
    clearABIServlet.doPost(request, response);
    Assert.assertEquals(200, response.getStatus());
  }

}
