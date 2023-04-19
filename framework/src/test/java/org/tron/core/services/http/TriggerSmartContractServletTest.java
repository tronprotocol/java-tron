package org.tron.core.services.http;

import com.google.gson.JsonObject;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.tron.common.BaseTest;
import org.tron.common.application.Application;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethed;

@Slf4j
public class TriggerSmartContractServletTest extends BaseTest {
  private static final String httpNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list")
      .get(0);
  private static final byte[] ownerAddr = Hex.decode("410000000000000000000000000000000000000000");
  private static final byte[] contractAddr = Hex.decode(
      "41000000000000000000000000000000000000dEaD");

  @Resource
  private FullNodeHttpApiService httpApiService;
  @Resource
  private Application appT;

  @BeforeClass
  public static void init() throws Exception {
    dbPath = "output_" + TriggerSmartContractServletTest.class.getName();
    Args.setParam(
        new String[]{"--output-directory", dbPath, "--debug", "--witness"}, Constant.TEST_CONF);
    Args.getInstance().needSyncCheck = false;

    // build app context
    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    beanFactory.setAllowCircularReferences(false);
  }

  @Before
  public void before() {
    appT.addService(httpApiService);

    // start services
    appT.initServices(Args.getInstance());
    appT.startServices();
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
    invokeToLocal("triggersmartcontract", parameter);
    invokeToLocal("triggerconstantcontract", parameter);
    invokeToLocal("estimateenergy", parameter);
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
