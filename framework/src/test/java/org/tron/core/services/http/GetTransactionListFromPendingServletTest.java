package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.tron.common.utils.client.utils.HttpMethed.createRequest;

import javax.annotation.Resource;

import org.apache.http.client.methods.HttpGet;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;


public class GetTransactionListFromPendingServletTest extends BaseTest {

  @Resource
  private GetTransactionListFromPendingServlet getTransactionListFromPendingServlet;

  static {
    Args.setParam(
            new String[]{
                "--output-directory", dbPath(),
            }, Constant.TEST_CONF
    );
  }

  @Test
  public void testGet() {
    MockHttpServletRequest request = createRequest(HttpGet.METHOD_NAME);
    MockHttpServletResponse response = new MockHttpServletResponse();
    getTransactionListFromPendingServlet.doPost(request, response);
    assertEquals(200, response.getStatus());
  }

}
