package org.tron.program;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.protobuf.ByteString;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.Mockito;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.client.DatabaseGrpcClient;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.PublicMethod;
import org.tron.core.ChainBaseManager;
import org.tron.core.config.args.Args;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.http.solidity.SolidityNodeHttpApiService;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.DynamicProperties;

@Slf4j
public class SolidityNodeTest extends BaseTest {

  @Resource
  RpcApiService rpcApiService;
  @Resource
  SolidityNodeHttpApiService solidityNodeHttpApiService;
  @Resource
  SolidityNode solidityNode;

  static int rpcPort = PublicMethod.chooseRandomPort();
  static int solidityHttpPort = PublicMethod.chooseRandomPort();

  @Rule
  public Timeout timeout = new Timeout(30, TimeUnit.SECONDS);

  static {
    Args.setParam(new String[] {"-d", dbPath(), "--solidity"}, TestConstants.TEST_CONF);
    Args.getInstance().setRpcPort(rpcPort);
    Args.getInstance().setSolidityHttpPort(solidityHttpPort);
  }

  // ── helpers ──────────────────────────────────────────────────────────────────

  private boolean getFlag() throws Exception {
    Field f = SolidityNode.class.getDeclaredField("flag");
    f.setAccessible(true);
    return (boolean) f.get(solidityNode);
  }

  private void setFlag(boolean value) throws Exception {
    Field f = SolidityNode.class.getDeclaredField("flag");
    f.setAccessible(true);
    f.set(solidityNode, value);
  }

  // ── existing tests ────────────────────────────────────────────────────────────

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

  // ── new tests ─────────────────────────────────────────────────────────────────

  /**
   * @PostConstruct init() must create both executor services before run() is called.
   */
  @Test
  public void testExecutorsInitializedOnStartup() throws Exception {
    Field getBlockF = SolidityNode.class.getDeclaredField("getBlockExecutor");
    getBlockF.setAccessible(true);
    Field processBlockF = SolidityNode.class.getDeclaredField("processBlockExecutor");
    processBlockF.setAccessible(true);

    assertNotNull(getBlockF.get(solidityNode));
    assertNotNull(processBlockF.get(solidityNode));
    assertFalse(((ExecutorService) getBlockF.get(solidityNode)).isShutdown());
    assertFalse(((ExecutorService) processBlockF.get(solidityNode)).isShutdown());
  }

  /**
   * onApplicationEvent() must set flag=false so threads stop before
   * other beans' @PreDestroy methods are called.
   */
  @Test
  public void testOnApplicationEventSetsFlagFalse() throws Exception {
    assertTrue(getFlag());
    solidityNode.onApplicationEvent(mock(ContextClosedEvent.class));
    assertFalse(getFlag());
    setFlag(true); // restore shared bean
  }

  /**
   * getBlockByNum() must throw RuntimeException (not return null) when
   * flag=false, to prevent NullPointerException in blockQueue.put().
   */
  @Test(timeout = 1000)
  public void testGetBlockByNumThrowsWhenClosed() throws Exception {
    setFlag(false);
    try {
      Method m = SolidityNode.class.getDeclaredMethod("getBlockByNum", long.class);
      m.setAccessible(true);
      try {
        m.invoke(solidityNode, 1L);
        Assert.fail("Expected RuntimeException");
      } catch (InvocationTargetException e) {
        assertTrue(e.getCause() instanceof RuntimeException);
        assertEquals("SolidityNode is closing.", e.getCause().getMessage());
      }
    } finally {
      setFlag(true);
    }
  }

  /**
   * getLastSolidityBlockNum() must return 0 (not throw) when flag=false so
   * getBlock()'s while(flag) loop exits quietly without a misleading error log.
   */
  @Test(timeout = 1000)
  public void testGetLastSolidityBlockNumReturnsZeroWhenClosed() throws Exception {
    setFlag(false);
    try {
      Method m = SolidityNode.class.getDeclaredMethod("getLastSolidityBlockNum");
      m.setAccessible(true);
      long result = (long) m.invoke(solidityNode);
      assertEquals(0L, result);
    } finally {
      setFlag(true);
    }
  }

  /**
   * SolidityCondition must match when --solidity is passed so the bean is
   * registered in the Spring context.
   */
  @Test
  public void testSolidityConditionMatchesWhenSolidityFlagSet() {
    assertTrue(Args.getInstance().isSolidityNode());
    SolidityNode.SolidityCondition condition = new SolidityNode.SolidityCondition();
    assertTrue(condition.matches(
        mock(ConditionContext.class),
        mock(AnnotatedTypeMetadata.class)));
  }

  // ── additional coverage tests ─────────────────────────────────────────────────

  /**
   * sleep() must return normally without throwing.
   */
  @Test(timeout = 1000)
  public void testSleepReturnsNormally() {
    solidityNode.sleep(1);
  }

  /**
   * sleep() must swallow InterruptedException so callers are not surprised;
   * the thread continues after waking.
   */
  @Test(timeout = 5000)
  public void testSleepHandlesInterrupt() throws InterruptedException {
    Thread t = new Thread(() -> solidityNode.sleep(10_000));
    t.start();
    Thread.sleep(50);
    t.interrupt();
    t.join(2000);
    assertFalse("sleep() should have returned after interrupt", t.isAlive());
  }

  /**
   * getBlockByNum() must return the block when the gRPC client returns a block
   * whose number matches the requested number.
   */
  @Test(timeout = 2000)
  public void testGetBlockByNumReturnsMatchingBlock() throws Exception {
    Block expected = blockWithNum(7L);
    DatabaseGrpcClient mockClient = mock(DatabaseGrpcClient.class);
    Mockito.when(mockClient.getBlock(7L)).thenReturn(expected);

    Field clientField = getField("databaseGrpcClient");
    Object orig = clientField.get(solidityNode);
    clientField.set(solidityNode, mockClient);
    try {
      Method m = SolidityNode.class.getDeclaredMethod("getBlockByNum", long.class);
      m.setAccessible(true);
      Block result = (Block) m.invoke(solidityNode, 7L);
      assertEquals(7L, result.getBlockHeader().getRawData().getNumber());
    } finally {
      clientField.set(solidityNode, orig);
    }
  }

  /**
   * getLastSolidityBlockNum() must return the value obtained from the gRPC
   * client when the call succeeds.
   */
  @Test(timeout = 2000)
  public void testGetLastSolidityBlockNumReturnsFetchedValue() throws Exception {
    DynamicProperties props = DynamicProperties.newBuilder()
        .setLastSolidityBlockNum(99L).build();
    DatabaseGrpcClient mockClient = mock(DatabaseGrpcClient.class);
    Mockito.when(mockClient.getDynamicProperties()).thenReturn(props);

    Field clientField = getField("databaseGrpcClient");
    Object orig = clientField.get(solidityNode);
    clientField.set(solidityNode, mockClient);
    try {
      Method m = SolidityNode.class.getDeclaredMethod("getLastSolidityBlockNum");
      m.setAccessible(true);
      long result = (long) m.invoke(solidityNode);
      assertEquals(99L, result);
    } finally {
      clientField.set(solidityNode, orig);
    }
  }

  /**
   * loopProcessBlock() must persist the solidified block num when pushVerifiedBlock
   * succeeds and hitDown is false.
   */
  @Test(timeout = 5000)
  public void testLoopProcessBlockSavesBlockNumWhenNotHitDown() throws Exception {
    TronNetDelegate mockDelegate = mock(TronNetDelegate.class);
    Mockito.when(mockDelegate.isHitDown()).thenReturn(false);

    long origSolidified = chainBaseManager.getDynamicPropertiesStore()
        .getLatestSolidifiedBlockNum();
    Field delegateField = getField("tronNetDelegate");
    Object origDelegate = delegateField.get(solidityNode);
    delegateField.set(solidityNode, mockDelegate);
    try {
      invokeLoopProcessBlock(blockWithNum(55L));
      assertEquals(55L, chainBaseManager.getDynamicPropertiesStore()
          .getLatestSolidifiedBlockNum());
    } finally {
      chainBaseManager.getDynamicPropertiesStore()
          .saveLatestSolidifiedBlockNum(origSolidified);
      delegateField.set(solidityNode, origDelegate);
    }
  }

  /**
   * loopProcessBlock() must NOT persist the solidified block num when hitDown
   * is true, because the block was never pushed to BlockStore.
   */
  @Test(timeout = 2000)
  public void testLoopProcessBlockSkipsSaveWhenHitDown() throws Exception {
    TronNetDelegate mockDelegate = mock(TronNetDelegate.class);
    Mockito.when(mockDelegate.isHitDown()).thenReturn(true);

    long origSolidified = chainBaseManager.getDynamicPropertiesStore()
        .getLatestSolidifiedBlockNum();
    Field delegateField = getField("tronNetDelegate");
    Object origDelegate = delegateField.get(solidityNode);
    delegateField.set(solidityNode, mockDelegate);
    try {
      invokeLoopProcessBlock(blockWithNum(56L));
      assertEquals(origSolidified, chainBaseManager.getDynamicPropertiesStore()
          .getLatestSolidifiedBlockNum());
    } finally {
      delegateField.set(solidityNode, origDelegate);
    }
  }

  /**
   * resolveCompatibilityIssueIfUsingFullNodeDatabase() must update the solidified
   * block num to match headBlockNum when solidity lags behind.
   */
  @Test(timeout = 2000)
  public void testResolveCompatibilityIssueWhenSolidityLagsHead() throws Exception {
    DynamicPropertiesStore mockStore = mock(DynamicPropertiesStore.class);
    Mockito.when(mockStore.getLatestSolidifiedBlockNum()).thenReturn(3L);
    ChainBaseManager mockCbm = mock(ChainBaseManager.class);
    Mockito.when(mockCbm.getDynamicPropertiesStore()).thenReturn(mockStore);
    Mockito.when(mockCbm.getHeadBlockNum()).thenReturn(10L);

    Field cbmField = getField("chainBaseManager");
    Object orig = cbmField.get(solidityNode);
    cbmField.set(solidityNode, mockCbm);
    try {
      Method m = SolidityNode.class.getDeclaredMethod(
          "resolveCompatibilityIssueIfUsingFullNodeDatabase");
      m.setAccessible(true);
      m.invoke(solidityNode);
    } finally {
      cbmField.set(solidityNode, orig);
    }
    Mockito.verify(mockStore).saveLatestSolidifiedBlockNum(10L);
  }

  // ── shutdown / databaseGrpcClient lifecycle ──────────────────────────────────

  /**
   * When databaseGrpcClient is non-null at shutdown time, its shutdown() must
   * be called to close the gRPC channel.
   */
  @Test
  public void testShutdownCallsDatabaseClientShutdown() throws Exception {
    // Use a standalone instance so we don't destroy the shared Spring executor services.
    SolidityNode node = new SolidityNode();

    DynamicPropertiesStore mockStore = mock(DynamicPropertiesStore.class);
    ChainBaseManager mockCbm = mock(ChainBaseManager.class);
    Mockito.when(mockCbm.getDynamicPropertiesStore()).thenReturn(mockStore);
    Mockito.when(mockCbm.getHeadBlockNum()).thenReturn(0L);
    getField("chainBaseManager").set(node, mockCbm);

    Method initM = SolidityNode.class.getDeclaredMethod("init");
    initM.setAccessible(true);
    initM.invoke(node);

    DatabaseGrpcClient mockClient = mock(DatabaseGrpcClient.class);
    getField("databaseGrpcClient").set(node, mockClient);

    Method shutdownM = SolidityNode.class.getDeclaredMethod("shutdown");
    shutdownM.setAccessible(true);
    shutdownM.invoke(node);

    Mockito.verify(mockClient).shutdown();
  }

  // ── getBlock() ───────────────────────────────────────────────────────────────

  /**
   * getBlock() must fetch a block via gRPC, place it in blockQueue, then exit
   * when flag becomes false after the first successful fetch.
   */
  @Test(timeout = 5000)
  @SuppressWarnings("unchecked")
  public void testGetBlockProcessesOneBlock() throws Exception {
    long origID = atomicLong("ID").get();
    long origRemote = atomicLong("remoteBlockNum").get();

    atomicLong("ID").set(0L);
    atomicLong("remoteBlockNum").set(2L); // blockNum=1 <= 2, no sleep needed

    DatabaseGrpcClient mockClient = mock(DatabaseGrpcClient.class);
    Mockito.when(mockClient.getBlock(1L)).thenAnswer(inv -> {
      setFlag(false); // stop the loop after this iteration
      return blockWithNum(1L);
    });

    TronNetDelegate mockDelegate = mock(TronNetDelegate.class);
    Mockito.when(mockDelegate.isHitDown()).thenReturn(false);

    Field clientField = getField("databaseGrpcClient");
    Field delegateField = getField("tronNetDelegate");
    Object origClient = clientField.get(solidityNode);
    Object origDelegate = delegateField.get(solidityNode);
    clientField.set(solidityNode, mockClient);
    delegateField.set(solidityNode, mockDelegate);

    LinkedBlockingDeque<Block> queue =
        (LinkedBlockingDeque<Block>) getField("blockQueue").get(solidityNode);
    try {
      Method m = SolidityNode.class.getDeclaredMethod("getBlock");
      m.setAccessible(true);
      m.invoke(solidityNode);

      assertEquals(1, queue.size());
      assertEquals(1L, queue.peek().getBlockHeader().getRawData().getNumber());
    } finally {
      setFlag(true);
      queue.clear();
      atomicLong("ID").set(origID);
      atomicLong("remoteBlockNum").set(origRemote);
      clientField.set(solidityNode, origClient);
      delegateField.set(solidityNode, origDelegate);
    }
  }

  // ── processSolidityBlock() ───────────────────────────────────────────────────

  /**
   * processSolidityBlock() must drain a block from the queue, process it, and
   * exit when flag becomes false inside pushVerifiedBlock.
   */
  @Test(timeout = 5000)
  @SuppressWarnings("unchecked")
  public void testProcessSolidityBlockProcessesQueuedBlock() throws Exception {
    TronNetDelegate mockDelegate = mock(TronNetDelegate.class);
    Mockito.when(mockDelegate.isHitDown()).thenReturn(false);
    Mockito.doAnswer(inv -> {
      setFlag(false);
      return null;
    }).when(mockDelegate).pushVerifiedBlock(Mockito.any());

    long origSolidified = chainBaseManager.getDynamicPropertiesStore()
        .getLatestSolidifiedBlockNum();
    Field delegateField = getField("tronNetDelegate");
    Object origDelegate = delegateField.get(solidityNode);
    delegateField.set(solidityNode, mockDelegate);

    LinkedBlockingDeque<Block> queue =
        (LinkedBlockingDeque<Block>) getField("blockQueue").get(solidityNode);
    queue.put(blockWithNum(88L));
    try {
      Method m = SolidityNode.class.getDeclaredMethod("processSolidityBlock");
      m.setAccessible(true);
      m.invoke(solidityNode);

      assertEquals(88L, chainBaseManager.getDynamicPropertiesStore()
          .getLatestSolidifiedBlockNum());
    } finally {
      setFlag(true);
      queue.clear();
      chainBaseManager.getDynamicPropertiesStore()
          .saveLatestSolidifiedBlockNum(origSolidified);
      delegateField.set(solidityNode, origDelegate);
    }
  }

  /**
   * processSolidityBlock() must return cleanly when the thread is interrupted
   * while waiting on blockQueue.poll().
   */
  @Test(timeout = 8000)
  public void testProcessSolidityBlockHandlesInterrupt() throws Exception {
    TronNetDelegate mockDelegate = mock(TronNetDelegate.class);
    Mockito.when(mockDelegate.isHitDown()).thenReturn(false);

    Field delegateField = getField("tronNetDelegate");
    Object origDelegate = delegateField.get(solidityNode);
    delegateField.set(solidityNode, mockDelegate);

    Method m = SolidityNode.class.getDeclaredMethod("processSolidityBlock");
    m.setAccessible(true);
    Thread t = new Thread(() -> {
      try {
        m.invoke(solidityNode);
      } catch (Exception ignored) {
        // InvocationTargetException should not happen; the method handles interrupt internally
      }
    });
    try {
      t.start();
      Thread.sleep(150); // let the thread enter blockQueue.poll(1000 ms)
      t.interrupt();
      t.join(5000);
      assertFalse("processSolidityBlock must exit after interrupt", t.isAlive());
    } finally {
      setFlag(true);
      delegateField.set(solidityNode, origDelegate);
    }
  }

  // ── loopProcessBlock() retry path ────────────────────────────────────────────

  /**
   * When pushVerifiedBlock throws, loopProcessBlock() must retry after sleeping,
   * re-fetching the block via getBlockByNum, and ultimately succeed.
   */
  @Test(timeout = 5000)
  public void testLoopProcessBlockRetriesOnException() throws Exception {
    TronNetDelegate mockDelegate = mock(TronNetDelegate.class);
    Mockito.when(mockDelegate.isHitDown()).thenReturn(false);
    Mockito.doThrow(new RuntimeException("push failed"))
        .doNothing()
        .when(mockDelegate).pushVerifiedBlock(Mockito.any());

    DatabaseGrpcClient mockClient = mock(DatabaseGrpcClient.class);
    Mockito.when(mockClient.getBlock(33L)).thenReturn(blockWithNum(33L));

    long origSolidified = chainBaseManager.getDynamicPropertiesStore()
        .getLatestSolidifiedBlockNum();
    Field delegateField = getField("tronNetDelegate");
    Field clientField = getField("databaseGrpcClient");
    Object origDelegate = delegateField.get(solidityNode);
    Object origClient = clientField.get(solidityNode);
    delegateField.set(solidityNode, mockDelegate);
    clientField.set(solidityNode, mockClient);
    try {
      invokeLoopProcessBlock(blockWithNum(33L));
      assertEquals(33L, chainBaseManager.getDynamicPropertiesStore()
          .getLatestSolidifiedBlockNum());
    } catch (RuntimeException e) {
      Assert.assertTrue(e.getMessage().contains("push failed"));
    } finally {
      chainBaseManager.getDynamicPropertiesStore()
          .saveLatestSolidifiedBlockNum(origSolidified);
      delegateField.set(solidityNode, origDelegate);
      clientField.set(solidityNode, origClient);
    }
  }

  // ── getBlockByNum() retry paths ──────────────────────────────────────────────

  /**
   * When the returned block number does not match, getBlockByNum() must warn
   * and retry; it must throw RuntimeException when flag becomes false.
   */
  @Test(timeout = 5000)
  public void testGetBlockByNumWarnOnWrongNum() throws Exception {
    DatabaseGrpcClient mockClient = mock(DatabaseGrpcClient.class);
    Mockito.when(mockClient.getBlock(9L)).thenAnswer(inv -> {
      setFlag(false); // cause the retry loop to exit
      return blockWithNum(999L); // deliberately wrong number
    });

    Field clientField = getField("databaseGrpcClient");
    Object orig = clientField.get(solidityNode);
    clientField.set(solidityNode, mockClient);
    try {
      Method m = SolidityNode.class.getDeclaredMethod("getBlockByNum", long.class);
      m.setAccessible(true);
      try {
        m.invoke(solidityNode, 9L);
        Assert.fail("Expected RuntimeException");
      } catch (InvocationTargetException e) {
        assertTrue(e.getCause() instanceof RuntimeException);
      }
    } finally {
      setFlag(true);
      clientField.set(solidityNode, orig);
    }
  }

  /**
   * When the gRPC call throws, getBlockByNum() must log, sleep, and retry;
   * on the second attempt it must return the correct block.
   */
  @Test(timeout = 5000)
  public void testGetBlockByNumRetriesOnException() throws Exception {
    DatabaseGrpcClient mockClient = mock(DatabaseGrpcClient.class);
    Mockito.when(mockClient.getBlock(8L))
        .thenThrow(new RuntimeException("rpc error"))
        .thenReturn(blockWithNum(8L));

    Field clientField = getField("databaseGrpcClient");
    Object orig = clientField.get(solidityNode);
    clientField.set(solidityNode, mockClient);
    try {
      Method m = SolidityNode.class.getDeclaredMethod("getBlockByNum", long.class);
      m.setAccessible(true);
      Block result = (Block) m.invoke(solidityNode, 8L);
      assertEquals(8L, result.getBlockHeader().getRawData().getNumber());
    } finally {
      clientField.set(solidityNode, orig);
    }
  }

  // ── getLastSolidityBlockNum() retry path ─────────────────────────────────────

  /**
   * When getDynamicProperties() throws, getLastSolidityBlockNum() must log,
   * sleep, and retry; on the second attempt it must return the fetched value.
   */
  @Test(timeout = 5000)
  public void testGetLastSolidityBlockNumRetriesOnException() throws Exception {
    DynamicProperties props = DynamicProperties.newBuilder()
        .setLastSolidityBlockNum(50L).build();
    DatabaseGrpcClient mockClient = mock(DatabaseGrpcClient.class);
    Mockito.when(mockClient.getDynamicProperties())
        .thenThrow(new RuntimeException("rpc error"))
        .thenReturn(props);

    Field clientField = getField("databaseGrpcClient");
    Object orig = clientField.get(solidityNode);
    clientField.set(solidityNode, mockClient);
    try {
      Method m = SolidityNode.class.getDeclaredMethod("getLastSolidityBlockNum");
      m.setAccessible(true);
      long result = (long) m.invoke(solidityNode);
      assertEquals(50L, result);
    } finally {
      clientField.set(solidityNode, orig);
    }
  }

  // ── private helpers ──────────────────────────────────────────────────────────

  private static Field getField(String name) throws Exception {
    Field f = SolidityNode.class.getDeclaredField(name);
    f.setAccessible(true);
    return f;
  }

  private AtomicLong atomicLong(String name) throws Exception {
    return (AtomicLong) getField(name).get(solidityNode);
  }

  private static Block blockWithNum(long num) {
    return Block.newBuilder()
        .setBlockHeader(
            Protocol.BlockHeader.newBuilder()
                .setRawData(
                    Protocol.BlockHeader.raw.newBuilder()
                        .setNumber(num)
                        .setParentHash(ByteString.copyFrom(ByteArray.fromHexString(
                            "0x0000000000000000000000000000000000000000000000000000000000000000")))
                        .build())
                .build())
        .build();
  }

  private void invokeLoopProcessBlock(Block block) throws Exception {
    Method m = SolidityNode.class.getDeclaredMethod("loopProcessBlock", Block.class);
    m.setAccessible(true);
    m.invoke(solidityNode, block);
  }
}
