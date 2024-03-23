package org.tron.core.services.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.fail;
import static org.tron.common.utils.client.utils.HttpMethed.createRequest;

import com.alibaba.fastjson.JSONObject;
import java.io.UnsupportedEncodingException;
import javax.annotation.Resource;
import org.apache.http.client.methods.HttpPost;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;

public class CreateSpendAuthSigServletTest extends BaseTest {

  static {
    Args.setParam(
            new String[]{
                "--output-directory", dbPath(),
            }, Constant.TEST_CONF
    );
  }

  @Resource
  private CreateSpendAuthSigServlet createSpendAuthSigServlet;

  @Test
  public void testCreateSpendAuthSig() {
    String jsonParam = "{"
            + "    \"ask\": \"e3ebcba1531f6d9158d9c162660c5d7c04dadf77d"
            + "85d7436a9c98b291ff69a09\","
            + "    \"tx_hash\": \"3b78fee6e956f915ffe082284c5f18640edca9"
            + "c57a5f227e5f7d7eb65ad61502\","
            + "    \"alpha\": \"2608999c3a97d005a879ecdaa16fd29ae434fb67"
            + "b177c5e875b0c829e6a1db04\""
            + "}";
    MockHttpServletRequest request = createRequest(HttpPost.METHOD_NAME);
    request.setContentType("application/json");
    request.setContent(jsonParam.getBytes(UTF_8));

    MockHttpServletResponse response = new MockHttpServletResponse();
    createSpendAuthSigServlet.doPost(request, response);
    Assert.assertEquals(200, response.getStatus());
    try {
      String contentAsString = response.getContentAsString();
      JSONObject result = JSONObject.parseObject(contentAsString);
      Assert.assertTrue(result.containsKey("value"));
      String resultValue = (String) result.get("value");
      Assert.assertNotNull(resultValue);
    } catch (UnsupportedEncodingException e) {
      fail(e.getMessage());
    }
  }

}
