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
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Constant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol;

public class CreateWitnessServletTest extends BaseTest {

  @Resource
  private CreateWitnessServlet createWitnessServlet;

  static {
    Args.setParam(
            new String[]{
                "--output-directory", dbPath(),
            }, Constant.TEST_CONF
    );
  }

  private static WitnessCapsule witnessCapsule;
  private static AccountCapsule accountCapsule;

  @Before
  public void init() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    ByteString address = ByteString.copyFrom(ecKey.getAddress());

    accountCapsule =
            new AccountCapsule(Protocol.Account
                    .newBuilder()
                    .setAddress(address).build());
    accountCapsule.setBalance(10000000L);
    dbManager.getAccountStore().put(accountCapsule
             .getAddress().toByteArray(), accountCapsule);
  }

  @Test
  public void testCreateWitness() {
    chainBaseManager.getDynamicPropertiesStore()
            .saveAccountUpgradeCost(1L);
    String hexAddress =  ByteArray
            .toHexString(accountCapsule.getAddress().toByteArray());
    String jsonParam = "{\"owner_address\":\""
            + hexAddress  + "\","
            + " \"url\": \"00757064617"
            + "4654e616d6531353330363038383733343633\"}";
    MockHttpServletRequest request = createRequest(HttpPost.METHOD_NAME);
    request.setContentType("application/json");
    request.setContent(jsonParam.getBytes(UTF_8));

    MockHttpServletResponse response = new MockHttpServletResponse();
    createWitnessServlet.doPost(request, response);
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


