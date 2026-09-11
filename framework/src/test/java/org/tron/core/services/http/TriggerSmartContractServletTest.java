package org.tron.core.services.http;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.PublicMethod;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.json.JSONObject;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass;

@Slf4j
public class TriggerSmartContractServletTest extends BaseTest {
  private static String httpNode;
  private static final byte[] ownerAddr = Hex.decode("410000000000000000000000000000000000000000");
  private static final byte[] contractAddr = Hex.decode(
      "41000000000000000000000000000000000000dEaD");

  @BeforeClass
  public static void init() throws Exception {
    Args.setParam(
        new String[]{"--output-directory", dbPath(), "--debug"}, TestConstants.TEST_CONF);
    Args.getInstance().needSyncCheck = false;
    Args.getInstance().setFullNodeHttpEnable(true);
    Args.getInstance().setFullNodeHttpPort(PublicMethod.chooseRandomPort());
    Args.getInstance().setP2pDisable(true);
    httpNode = String.format("%s:%d", "127.0.0.1",
        Args.getInstance().getFullNodeHttpPort());
  }

  @Before
  public void before() {
    // start services
    appT.startup();

    // create contract for testing
    Repository rootRepository = RepositoryImpl.createRoot(StoreFactory.getInstance());
    rootRepository.createAccount(contractAddr, Protocol.AccountType.Contract);
    rootRepository.createContract(contractAddr, new ContractCapsule(
        SmartContractOuterClass.SmartContract.newBuilder().build()));
    rootRepository.saveCode(contractAddr, Hex.decode(
        "608060405260043610601c5760003560e01c8063f8a8fd6d146021575b600080fd5b60276029565b00"
            + "5b3373ffffffffffffffffffffffffffffffffffffffff166108fc34908115029060405160006040518"
            + "0830381858888f19350505050158015606e573d6000803e3d6000fd5b5056fea2646970667358221220"
            + "45fe2c565cf16b27bb8cbafbe251a850a0bb5cd8806a186dbda12d57685ced6f64736f6c63430008120"
            + "033"));
    rootRepository.commit();
  }


  @Test
  public void testNormalCall() throws IOException {
    JsonObject parameter = new JsonObject();
    parameter.addProperty("owner_address", ByteArray.toHexString(ownerAddr));
    parameter.addProperty("contract_address", ByteArray.toHexString(contractAddr));
    parameter.addProperty("function_selector", "test()");
    RequestConfig timeouts = RequestConfig.custom().setConnectTimeout(5000)
        .setConnectionRequestTimeout(5000).setSocketTimeout(10000).build();
    try (CloseableHttpClient client = HttpClients.custom()
        .setDefaultRequestConfig(timeouts).build()) {
      for (String method : new String[]{"triggersmartcontract", "triggerconstantcontract",
          "estimateenergy"}) {
        HttpPost request = new HttpPost("http://" + httpNode + "/wallet/" + method);
        request.setEntity(new StringEntity(parameter.toString(), ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = client.execute(request)) {
          Assert.assertEquals(method, 200, response.getStatusLine().getStatusCode());
          String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
          Assert.assertNotNull(method, JSONObject.parseObject(body));
        }
      }
    }
  }
}
