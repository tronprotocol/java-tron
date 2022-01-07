package org.tron.core.jsonrpc;

import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.services.NodeInfoService;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;
import org.tron.protos.Protocol;


@Slf4j
public class JsonrpcServiceTest {
  private static String dbPath = "output_jsonrpc_service_test";
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_ACCOUNT_NAME = "first";

  private static TronJsonRpcImpl tronJsonRpc;
  private static TronApplicationContext context;
  private static NodeInfoService nodeInfoService;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);

    OWNER_ADDRESS =
        Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    nodeInfoService = context.getBean("nodeInfoService", NodeInfoService.class);
  }

  @BeforeClass
  public static void init() {
    Manager dbManager = context.getBean(Manager.class);
    Wallet wallet = context.getBean(Wallet.class);

    AccountCapsule accountCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(OWNER_ADDRESS_ACCOUNT_NAME),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            Protocol.AccountType.Normal,
            10000_000_000L);
    dbManager.getAccountStore().put(accountCapsule.getAddress().toByteArray(), accountCapsule);

    tronJsonRpc = new TronJsonRpcImpl(nodeInfoService, wallet, dbManager);
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  @Test
  public void testWeb3Sha3() {
    String result = "";
    try {
      result = tronJsonRpc.web3Sha3("0x1122334455667788");
    } catch (Exception e) {
      Assert.fail();
    }

    Assert.assertEquals("0x1360118a9c9fd897720cf4e26de80683f402dd7c28e000aa98ea51b85c60161c",
        result);
  }

}