package org.tron.program;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.InOrder;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.application.Application;
import org.tron.common.client.DatabaseGrpcClient;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.utils.PublicMethod;
import org.tron.core.config.args.Args;
import org.tron.core.exception.TronError;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.http.solidity.SolidityNodeHttpApiService;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.DynamicProperties;

@Slf4j
public class SolidityNodeTest extends BaseTest {

  @Resource
  RpcApiService rpcApiService;
  @Resource
  SolidityNodeHttpApiService solidityNodeHttpApiService;
  static int rpcPort = PublicMethod.chooseRandomPort();
  static int solidityHttpPort = PublicMethod.chooseRandomPort();

  @Rule
  public Timeout timeout = new Timeout(30, TimeUnit.SECONDS);

  static {
    Args.setParam(new String[] {"-d", dbPath(), "--solidity"}, TestConstants.TEST_CONF);
    Args.getInstance().setRpcPort(rpcPort);
    Args.getInstance().setSolidityHttpPort(solidityHttpPort);
  }

  @Test
  public void testSolidityArgs() {
    Assert.assertNotNull(Args.getInstance().getTrustNodeAddr());
    Assert.assertTrue(Args.getInstance().isSolidityNode());
    String trustNodeAddr = Args.getInstance().getTrustNodeAddr();
    Args.getInstance().setTrustNodeAddr(null);
    TronError thrown = assertThrows(TronError.class,
        SolidityNode::start);
    assertEquals(TronError.ErrCode.SOLID_NODE_INIT, thrown.getErrCode());
    Args.getInstance().setTrustNodeAddr(trustNodeAddr);
  }

  @Test
  public void testSolidityGrpcCall() {
    rpcApiService.start();
    DatabaseGrpcClient databaseGrpcClient = null;
    String address = Args.getInstance().getTrustNodeAddr().split(":")[0] + ":" + rpcPort;
    try {
      databaseGrpcClient = new DatabaseGrpcClient(address);
    } catch (Exception e) {
      logger.error("Failed to create database grpc client {}", address);
    }

    Assert.assertNotNull(databaseGrpcClient);
    DynamicProperties dynamicProperties = databaseGrpcClient.getDynamicProperties();
    Assert.assertNotNull(dynamicProperties);

    Block genesisBlock = databaseGrpcClient.getBlock(0);
    Assert.assertNotNull(genesisBlock);
    Assert.assertFalse(genesisBlock.getTransactionsList().isEmpty());
    Block invalidBlock = databaseGrpcClient.getBlock(-1);
    Assert.assertNotNull(invalidBlock);
    try {
      databaseGrpcClient = new DatabaseGrpcClient(address, -1);
    } catch (Exception e) {
      logger.error("Failed to create database grpc client {}", address);
    }
    databaseGrpcClient.shutdown();
    rpcApiService.stop();
  }

  @Test
  public void testSolidityNodeHttpApiService() {
    solidityNodeHttpApiService.start();
    // start again
    solidityNodeHttpApiService.start();
    solidityNodeHttpApiService.stop();
    Assert.assertTrue(true);
  }

  @Test
  public void testAwaitShutdownAlwaysStopsNode() {
    Application app = mock(Application.class);
    SolidityNode node = mock(SolidityNode.class);

    SolidityNode.awaitShutdown(app, node);

    InOrder inOrder = inOrder(app, node);
    inOrder.verify(app).blockUntilShutdown();
    inOrder.verify(node).shutdown();
  }

  @Test
  public void testAwaitShutdownStopsNodeWhenBlockedCallFails() {
    Application app = mock(Application.class);
    SolidityNode node = mock(SolidityNode.class);
    RuntimeException expected = new RuntimeException("boom");
    doThrow(expected).when(app).blockUntilShutdown();

    RuntimeException thrown = assertThrows(RuntimeException.class,
        () -> SolidityNode.awaitShutdown(app, node));
    assertSame(expected, thrown);

    InOrder inOrder = inOrder(app, node);
    inOrder.verify(app).blockUntilShutdown();
    inOrder.verify(node).shutdown();
  }

  @Test
  public void testShutdownSetsFlagAndShutsDownExecutors() throws Exception {
    SolidityNode node = mock(SolidityNode.class);
    doCallRealMethod().when(node).shutdown();

    ExecutorService es1 = ExecutorServiceManager.newSingleThreadExecutor("test-solid-get");
    ExecutorService es2 = ExecutorServiceManager.newSingleThreadExecutor("test-solid-process");

    Field flagField = SolidityNode.class.getDeclaredField("flag");
    flagField.setAccessible(true);
    flagField.set(node, true);

    Field getBlockEsField = SolidityNode.class.getDeclaredField("getBlockEs");
    getBlockEsField.setAccessible(true);
    getBlockEsField.set(node, es1);

    Field processBlockEsField = SolidityNode.class.getDeclaredField("processBlockEs");
    processBlockEsField.setAccessible(true);
    processBlockEsField.set(node, es2);

    node.shutdown();

    Assert.assertFalse((boolean) flagField.get(node));
    Assert.assertTrue(es1.isShutdown());
    Assert.assertTrue(es2.isShutdown());
  }

  @Test
  public void testRunInitializesNamedExecutors() throws Exception {
    rpcApiService.start();
    String originalAddr = Args.getInstance().getTrustNodeAddr();
    Args.getInstance().setTrustNodeAddr("127.0.0.1:" + rpcPort);
    try {
      SolidityNode node = new SolidityNode(dbManager);

      Field flagField = SolidityNode.class.getDeclaredField("flag");
      flagField.setAccessible(true);
      flagField.set(node, false);

      Method runMethod = SolidityNode.class.getDeclaredMethod("run");
      runMethod.setAccessible(true);
      runMethod.invoke(node);

      Field getBlockEsField = SolidityNode.class.getDeclaredField("getBlockEs");
      getBlockEsField.setAccessible(true);
      Field processBlockEsField = SolidityNode.class.getDeclaredField("processBlockEs");
      processBlockEsField.setAccessible(true);

      ExecutorService getBlockEs = (ExecutorService) getBlockEsField.get(node);
      ExecutorService processBlockEs = (ExecutorService) processBlockEsField.get(node);

      Assert.assertNotNull(getBlockEs);
      Assert.assertNotNull(processBlockEs);

      ExecutorServiceManager.shutdownAndAwaitTermination(getBlockEs, "test-solid-get");
      ExecutorServiceManager.shutdownAndAwaitTermination(processBlockEs, "test-solid-process");
    } finally {
      Args.getInstance().setTrustNodeAddr(originalAddr);
      rpcApiService.stop();
    }
  }
}
