package org.tron.core.services.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import javax.annotation.Resource;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;

public class ListNodesServletTest extends BaseTest {

  @Resource
  private ListNodesServlet listNodesServlet;

  static {
    dbPath = "db_GetNowBlockServlet_test";
    Args.setParam(
        new String[]{
            "--output-directory", dbPath,
        }, Constant.TEST_CONF
    );
  }

  public MockHttpServletRequest createRequest(String contentType) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("POST");
    request.setContentType(contentType);
    request.setCharacterEncoding("UTF-8");
    return request;
  }

  @Test
  public void testListNodesByJson() {
    String jsonParam = "{\"visible\": true}";
    MockHttpServletRequest request = createRequest("application/json");
    request.setContent(jsonParam.getBytes());
    MockHttpServletResponse response = new MockHttpServletResponse();
    listNodesServlet.doPost(request, response);
    try {
      String contentAsString = response.getContentAsString();
      assertNotNull(contentAsString);
    } catch (UnsupportedEncodingException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testListNodesValue() {
    MockHttpServletRequest request = createRequest("application/x-www-form-urlencoded");
    try {
      String params = "visible=true";
      request.setContent(params.getBytes(UTF_8));
      MockHttpServletResponse response = new MockHttpServletResponse();
      listNodesServlet.doPost(request, response);
      String contentAsString = response.getContentAsString();
      assertNotNull(contentAsString);
    } catch (UnsupportedEncodingException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testListNodesEmptyParam() {
    MockHttpServletRequest request = createRequest("application/x-www-form-urlencoded");
    String params = "visible=";
    request.setContent(params.getBytes(UTF_8));
    MockHttpServletResponse response = new MockHttpServletResponse();
    listNodesServlet.doPost(request, response);
    try {
      String contentAsString = response.getContentAsString();
      assertNotNull(contentAsString);
    } catch (UnsupportedEncodingException e) {
      fail(e.getMessage());
    }
  }

}
