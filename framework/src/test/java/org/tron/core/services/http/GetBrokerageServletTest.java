package org.tron.core.services.http;

import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.TestConstants;
import org.tron.core.config.args.Args;
import org.tron.json.JSONObject;

public class GetBrokerageServletTest extends AddressQueryServletTestBase {

  @Resource
  private GetBrokerageServlet getBrokerageServlet;

  static {
    Args.setParam(
            new String[]{
                "--output-directory", dbPath(),
            }, TestConstants.TEST_CONF
    );
  }

  @Override
  protected void invokeGet(MockHttpServletRequest request, MockHttpServletResponse response) {
    getBrokerageServlet.doGet(request, response);
  }

  @Override
  protected void invokePost(MockHttpServletRequest request, MockHttpServletResponse response) {
    getBrokerageServlet.doPost(request, response);
  }

  @Override
  protected String valueKey() {
    return "brokerage";
  }

  /** An address the delegation store holds nothing for still answers the default brokerage. */
  @Override
  protected int valueWithoutState() {
    return 20;
  }

  @Test
  public void getBrokerageValueByJsonTest() throws Exception {
    MockHttpServletRequest request = jsonRequest("TGSzEq4t7oMTRcn1VxDghRu5r5bWAE5D1W");

    JSONObject result = JSONObject.parseObject(invoke(request));
    Assert.assertEquals(20, (int) result.get("brokerage"));
  }

  @Test
  public void getBrokerageValueTest() throws Exception {
    MockHttpServletRequest request = formRequest("TGSzEq4t7oMTRcn1VxDghRu5r5bWAE5D1W");

    JSONObject result = JSONObject.parseObject(invoke(request));
    Assert.assertEquals(20, (int) result.get("brokerage"));
  }
}
