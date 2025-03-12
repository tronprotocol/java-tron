package org.tron.core.services.http;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.client.utils.HttpMethed;
import org.tron.core.Constant;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.RepositoryImpl;
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
        new String[]{"--output-directory", dbPath(), "--debug"}, Constant.TEST_CONF);
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
  public void testNormalCall() {
    HttpMethed.waitToProduceOneBlock(httpNode);
    JsonObject parameter = new JsonObject();
    parameter.addProperty("owner_address", ByteArray.toHexString(ownerAddr));
    parameter.addProperty("contract_address", ByteArray.toHexString(contractAddr));
    parameter.addProperty("function_selector", "test()");
    HttpResponse triggersmartcontract1 = invokeToLocal("triggersmartcontract", parameter);
    HttpResponse triggersmartcontract2 = invokeToLocal("triggerconstantcontract", parameter);
    HttpResponse triggersmartcontract3 = invokeToLocal("estimateenergy", parameter);
    Assert.assertNotNull(triggersmartcontract1);
    Assert.assertNotNull(triggersmartcontract2);
    Assert.assertNotNull(triggersmartcontract3);
  }

  public static HttpResponse invokeToLocal(
      String method, JsonObject parameter) {
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/" + method;
      return HttpMethed.createConnect(requestUrl, parameter);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
