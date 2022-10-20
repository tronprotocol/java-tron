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
import org.tron.core.db2.core.Chainbase.Cursor;
import org.tron.core.services.NodeInfoService;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl.RequestSource;
import org.tron.core.services.jsonrpc.types.BuildArguments;
import org.tron.protos.Protocol;

@Slf4j
public class WalletCursorTest {
  private static String dbPath = "output_wallet_cursor_test";
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_ACCOUNT_NAME = "first";

  private static TronApplicationContext context;
  private static Manager dbManager;
  private static Wallet wallet;
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
    dbManager = context.getBean(Manager.class);
    wallet = context.getBean(Wallet.class);

    AccountCapsule accountCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(OWNER_ADDRESS_ACCOUNT_NAME),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            Protocol.AccountType.Normal,
            10000_000_000L);
    dbManager.getAccountStore().put(accountCapsule.getAddress().toByteArray(), accountCapsule);
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
  public void testSource() {
    TronJsonRpcImpl tronJsonRpc = new TronJsonRpcImpl(nodeInfoService, wallet, dbManager);

    Assert.assertEquals(Cursor.HEAD, wallet.getCursor());
    Assert.assertEquals(RequestSource.FULLNODE, tronJsonRpc.getSource());

    dbManager.setCursor(Cursor.HEAD);
    Assert.assertEquals(Cursor.HEAD, wallet.getCursor());
    Assert.assertEquals(RequestSource.FULLNODE, tronJsonRpc.getSource());
    dbManager.resetCursor();

    dbManager.setCursor(Cursor.SOLIDITY);
    Assert.assertEquals(Cursor.SOLIDITY, wallet.getCursor());
    Assert.assertEquals(RequestSource.SOLIDITY, tronJsonRpc.getSource());
    dbManager.resetCursor();

    dbManager.setCursor(Cursor.PBFT);
    Assert.assertEquals(Cursor.PBFT, wallet.getCursor());
    Assert.assertEquals(RequestSource.PBFT, tronJsonRpc.getSource());
    dbManager.resetCursor();
  }

  @Test
  public void testDisableInSolidity() {
    BuildArguments buildArguments = new BuildArguments();
    buildArguments.setFrom("0xabd4b9367799eaa3197fecb144eb71de1e049abc");
    buildArguments.setTo("0x548794500882809695a8a687866e76d4271a1abc");
    buildArguments.setTokenId(1000016L);
    buildArguments.setTokenValue(20L);

    dbManager.setCursor(Cursor.SOLIDITY);

    TronJsonRpcImpl tronJsonRpc = new TronJsonRpcImpl(nodeInfoService, wallet, dbManager);
    try {
      tronJsonRpc.buildTransaction(buildArguments);
    } catch (Exception e) {
      Assert.assertEquals("the method buildTransaction does not exist/is not available in "
          + "SOLIDITY", e.getMessage());
    }

    dbManager.resetCursor();
  }

  @Test
  public void testDisableInPBFT() {
    BuildArguments buildArguments = new BuildArguments();
    buildArguments.setFrom("0xabd4b9367799eaa3197fecb144eb71de1e049abc");
    buildArguments.setTo("0x548794500882809695a8a687866e76d4271a1abc");
    buildArguments.setTokenId(1000016L);
    buildArguments.setTokenValue(20L);

    dbManager.setCursor(Cursor.PBFT);

    TronJsonRpcImpl tronJsonRpc = new TronJsonRpcImpl(nodeInfoService, wallet, dbManager);
    try {
      tronJsonRpc.buildTransaction(buildArguments);
    } catch (Exception e) {
      Assert.assertEquals("the method buildTransaction does not exist/is not available in "
          + "PBFT", e.getMessage());
    }

    String method = "test";
    try {
      tronJsonRpc.disableInPBFT(method);
    } catch (Exception e) {
      String expMsg = String.format("the method %s does not exist/is not available in PBFT",
          method);
      Assert.assertEquals(expMsg, e.getMessage());
    }

    dbManager.resetCursor();
  }

  @Test
  public void testEnableInFullNode() {
    BuildArguments buildArguments = new BuildArguments();
    buildArguments.setFrom("0xabd4b9367799eaa3197fecb144eb71de1e049abc");
    buildArguments.setTo("0x548794500882809695a8a687866e76d4271a1abc");
    buildArguments.setValue("0x1f4");

    TronJsonRpcImpl tronJsonRpc = new TronJsonRpcImpl(nodeInfoService, wallet, dbManager);

    try {
      tronJsonRpc.buildTransaction(buildArguments);
    } catch (Exception e) {
      Assert.fail();
    }
  }

}