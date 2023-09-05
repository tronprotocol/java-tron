package org.tron.core.services.http;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.tron.common.utils.client.utils.HttpMethed.createRequest;

import com.alibaba.fastjson.JSONObject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.annotation.Resource;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;

public class GetEnergyPricesServletTest extends BaseTest {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Resource
  private GetEnergyPricesServlet getEnergyPricesServlet;

  @BeforeClass
  public static void init() throws IOException {
    Args.setParam(new String[]{"-d", temporaryFolder.newFolder().toString()}, Constant.TEST_CONF);
  }

  @Test
  public void testGet() {
    MockHttpServletRequest request = createRequest(HttpGet.METHOD_NAME);
    MockHttpServletResponse response = new MockHttpServletResponse();
    getEnergyPricesServlet.doPost(request, response);
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
      getEnergyPricesServlet.doPost(request, response);
      String contentAsString = response.getContentAsString();
      JSONObject result = JSONObject.parseObject(contentAsString);
      assertTrue(result.containsKey("prices"));
    } catch (UnsupportedEncodingException e) {
      fail(e.getMessage());
    }
  }
}
