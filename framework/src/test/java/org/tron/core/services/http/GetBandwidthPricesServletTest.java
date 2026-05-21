package org.tron.core.services.http;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.tron.common.utils.client.utils.HttpMethed.createRequest;

import java.io.UnsupportedEncodingException;
import javax.annotation.Resource;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.core.config.args.Args;
import org.tron.json.JSONObject;

public class GetBandwidthPricesServletTest extends BaseTest {

  @Resource
  private GetBandwidthPricesServlet getBandwidthPricesServlet;

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath()}, TestConstants.TEST_CONF);
  }

  @Test
  public void testGet() {
    MockHttpServletRequest request = createRequest(HttpGet.METHOD_NAME);
    MockHttpServletResponse response = new MockHttpServletResponse();
    getBandwidthPricesServlet.doPost(request, response);
    try {
      String contentAsString = response.getContentAsString();
      JSONObject result = JSONObject.parseObject(contentAsString);
      assertTrue(result.containsKey("prices"));
    } catch (UnsupportedEncodingException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testPost() {
    MockHttpServletRequest request = createRequest(HttpPost.METHOD_NAME);
    try {
      MockHttpServletResponse response = new MockHttpServletResponse();
      getBandwidthPricesServlet.doPost(request, response);
      String contentAsString = response.getContentAsString();
      JSONObject result = JSONObject.parseObject(contentAsString);
      assertTrue(result.containsKey("prices"));
    } catch (UnsupportedEncodingException e) {
      fail(e.getMessage());
    }
  }
}
