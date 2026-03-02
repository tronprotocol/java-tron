package org.tron.core.services.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.tron.common.utils.client.utils.HttpMethed.createRequest;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;

import java.io.UnsupportedEncodingException;
import javax.annotation.Resource;

import org.apache.http.client.methods.HttpPost;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.config.args.Args;
import org.tron.protos.contract.SmartContractOuterClass;

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

  private static final String SMART_CONTRACT_NAME = "smart_contract_test";
  private static String CONTRACT_ADDRESS = "A0B4750E2CD76E19DCA331BF5D089B71C3C2798548";
  private static String OWNER_ADDRESS;
  private static final long SOURCE_ENERGY_LIMIT = 10L;



  private SmartContractOuterClass.SmartContract.Builder createContract(
          String contractAddress, String contractName) {
    OWNER_ADDRESS =
            "A099357684BC659F5166046B56C95A0E99F1265CBD";
    SmartContractOuterClass.SmartContract.Builder builder =
            SmartContractOuterClass.SmartContract.newBuilder();
    builder.setName(contractName);
    builder.setOriginAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
    builder.setContractAddress(ByteString.copyFrom(ByteArray.fromHexString(contractAddress)));
    builder.setOriginEnergyLimit(SOURCE_ENERGY_LIMIT);
    return builder;
  }

  @Test
  public void testClearABI() {
    chainBaseManager.getDynamicPropertiesStore()
            .saveAllowTvmConstantinople(1);
    SmartContractOuterClass.SmartContract.Builder contract =
            createContract(CONTRACT_ADDRESS, SMART_CONTRACT_NAME);
    chainBaseManager.getContractStore().put(
            ByteArray.fromHexString(CONTRACT_ADDRESS),
            new ContractCapsule(contract.build()));

    String jsonParam = "{"
            + "    \"owner_address\": \"A099357684BC659F5166046B56C95A0E99F1265CBD\","
            + "    \"contract_address\": \"A0B4750E2CD76E19DCA331BF5D089B71C3C2798548\""
            + "}";
    MockHttpServletRequest request = createRequest(HttpPost.METHOD_NAME);
    request.setContentType("application/json");
    request.setContent(jsonParam.getBytes(UTF_8));

    MockHttpServletResponse response = new MockHttpServletResponse();
    clearABIServlet.doPost(request, response);
    Assert.assertEquals(200, response.getStatus());
    try {
      String contentAsString = response.getContentAsString();
      JSONObject result = JSONObject.parseObject(contentAsString);
      assertTrue(result.containsKey("raw_data"));
      assertTrue(result.containsKey("txID"));
    } catch (UnsupportedEncodingException e) {
      fail(e.getMessage());
    }

  }

}
