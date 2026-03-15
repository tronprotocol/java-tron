package org.tron.core.services.http;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.tron.common.TestEnv.withDbEngineOverride;

import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.common.TestEnv;
import org.tron.core.config.args.Args;

public class GetAccountByIdServletTest extends BaseTest {

  static {
    Args.setParam(withDbEngineOverride(
                "--output-directory", dbPath()
            ), TestEnv.TEST_CONF
    );
  }

  @Resource
  private GetAccountByIdServlet getAccountByIdServlet;

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
  public void testGetAccountById() {
    String jsonParam = "{\"account_id\": \"6161616162626262\"}";
    MockHttpServletRequest request = createRequest("application/json");
    request.setContent(jsonParam.getBytes());
    MockHttpServletResponse response = new MockHttpServletResponse();

    getAccountByIdServlet.doPost(request, response);
    Assert.assertEquals(200, response.getStatus());
  }
}
