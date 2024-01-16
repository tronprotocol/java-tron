package org.tron.core.services.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.fail;
import static org.tron.common.utils.client.utils.HttpMethed.createRequest;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;

import java.io.UnsupportedEncodingException;
import javax.annotation.Resource;

import org.apache.http.client.methods.HttpPost;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol;


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

  @Before
  public void init() {
    AccountCapsule accountCapsule = new AccountCapsule(
            ByteString.copyFrom(ByteArray
                    .fromHexString("A099357684BC659F5166046B56C95A0E99F1265CD1")),
            ByteString.copyFromUtf8("owner"),
            Protocol.AccountType.forNumber(1));

    chainBaseManager.getAccountStore().put(accountCapsule.createDbKey(),
            accountCapsule);
  }

  @Test
  public void testCreate() {
    String jsonParam = "{"
            + "\"owner_address\": \"A099357684BC659F5166046B56C95A0E99F1265CD1\","
            + "\"account_address\": \"A0B4750E2CD76E19DCA331BF5D089B71C3C2798541\""
            + "}";
    MockHttpServletRequest request = createRequest(HttpPost.METHOD_NAME);
    request.setContentType("application/json");
    request.setContent(jsonParam.getBytes(UTF_8));

    MockHttpServletResponse response = new MockHttpServletResponse();
    createAccountServlet.doPost(request, response);

    Assert.assertEquals(200, response.getStatus());
    try {
      String contentAsString = response.getContentAsString();
      JSONObject result = JSONObject.parseObject(contentAsString);
      Assert.assertTrue(result.containsKey("raw_data"));
      Assert.assertTrue(result.containsKey("txID"));
    } catch (UnsupportedEncodingException e) {
      fail(e.getMessage());
    }
  }
}
