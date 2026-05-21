package org.tron.core.jsonrpc;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db2.core.Chainbase.Cursor;
import org.tron.core.exception.jsonrpc.JsonRpcExceedLimitException;
import org.tron.core.services.NodeInfoService;
import org.tron.core.services.jsonrpc.TronJsonRpc.FilterRequest;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl.RequestSource;
import org.tron.core.services.jsonrpc.filters.LogFilterAndResult;
import org.tron.core.services.jsonrpc.types.BuildArguments;
import org.tron.protos.Protocol;

@Slf4j
public class WalletCursorTest extends BaseTest {

  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_ACCOUNT_NAME = "first";
  @Resource
  private Wallet wallet;
  @Resource
  private NodeInfoService nodeInfoService;
  private static boolean init;

  static {
    Args.setParam(new String[] {"--output-directory", dbPath()}, TestConstants.TEST_CONF);

    OWNER_ADDRESS =
        Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
  }

  @Before
  public void init() {
    if (init) {
      return;
    }
    AccountCapsule accountCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(OWNER_ADDRESS_ACCOUNT_NAME),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            Protocol.AccountType.Normal,
            10000_000_000L);
    dbManager.getAccountStore().put(accountCapsule.getAddress().toByteArray(), accountCapsule);
    init = true;
  }

  @Test
  public void testSource() {
    TronJsonRpcImpl tronJsonRpc = new TronJsonRpcImpl(nodeInfoService, wallet);
    tronJsonRpc.setManager(dbManager);

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

    TronJsonRpcImpl tronJsonRpc = new TronJsonRpcImpl(nodeInfoService, wallet);
    tronJsonRpc.setManager(dbManager);
    try {
      tronJsonRpc.buildTransaction(buildArguments);
      tronJsonRpc.close();
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

    TronJsonRpcImpl tronJsonRpc = new TronJsonRpcImpl(nodeInfoService, wallet);
    tronJsonRpc.setManager(dbManager);
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

    TronJsonRpcImpl tronJsonRpc = new TronJsonRpcImpl(nodeInfoService, wallet);
    tronJsonRpc.setManager(dbManager);

    try {
      tronJsonRpc.buildTransaction(buildArguments);
      tronJsonRpc.close();
    } catch (Exception e) {
      Assert.fail();
    }
  }

  /**
   * When the active filter count reaches the configured cap (node.jsonrpc.maxLogFilterNum),
   * eth_newFilter must throw JsonRpcExceedLimitException instead of growing without bound.
   */
  @Test
  public void testNewFilter_exceedsCapThrowsException() throws Exception {
    int cap = 5;
    int saved = Args.getInstance().getJsonRpcMaxLogFilterNum();
    Args.getInstance().setJsonRpcMaxLogFilterNum(cap);
    FilterRequest fr = new FilterRequest();
    TronJsonRpcImpl tronJsonRpc = new TronJsonRpcImpl(nodeInfoService, wallet);
    tronJsonRpc.setManager(dbManager);
    Map<String, LogFilterAndResult> map = tronJsonRpc.getEventFilter2ResultFull();
    List<String> addedKeys = new ArrayList<>();

    try {
      for (int i = 0; i < cap; i++) {
        String key = "walletcursor-cap-test-" + i;
        map.put(key, new LogFilterAndResult(fr, 0L, null));
        addedKeys.add(key);
      }
      Assert.assertEquals(cap, addedKeys.size());

      try {
        tronJsonRpc.newFilter(fr);
        Assert.fail("Expected JsonRpcExceedLimitException when filter count reaches cap");
      } catch (JsonRpcExceedLimitException e) {
        Assert.assertTrue(e.getMessage().contains(String.valueOf(cap)));
      }
    } finally {
      tronJsonRpc.close();
      Args.getInstance().setJsonRpcMaxLogFilterNum(saved);
    }
  }

}