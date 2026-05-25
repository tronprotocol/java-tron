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

  // ── gRPC / HTTP service integration ──────────────────────────────────────────

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

  // ── lifecycle ─────────────────────────────────────────────────────────────────

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

  // ── sleep() ───────────────────────────────────────────────────────────────────

  /**
   * sleep() must:
   * - return normally without throwing on a plain call,
   * - exit early when the thread is interrupted,
   * - restore the interrupt flag so callers can observe it immediately.
   */
  @Test(timeout = 5000)
  public void testSleep() throws InterruptedException {
    // Normal: returns without throwing.
    solidityNode.sleep(1);

    // Interrupt: exits early + restores flag.
    boolean[] flagAfterSleep = {false};
    Thread t = new Thread(() -> {
      solidityNode.sleep(10_000);
      flagAfterSleep[0] = Thread.currentThread().isInterrupted();
    });
    t.start();
    Thread.sleep(50);
    t.interrupt();
    t.join(2000);
    assertFalse("sleep() must return after interrupt", t.isAlive());
    assertTrue("sleep() must restore the interrupt flag", flagAfterSleep[0]);
  }

  // ── getBlockByNum() ───────────────────────────────────────────────────────────

  /**
   * getBlockByNum() normal-path and transient-error recovery:
   * - happy path: returns the block when the gRPC response number matches,
   * - null response: warns and retries on the next iteration,
   * - RPC exception: logs, sleeps, and succeeds on the second attempt.
   */
  @Test(timeout = 6000)
  public void testGetBlockByNum() throws Exception {
    Method m = SolidityNode.class.getDeclaredMethod("getBlockByNum", long.class);
    m.setAccessible(true);
    Field clientField = getField("databaseGrpcClient");
    Object orig = clientField.get(solidityNode);
    try {
      DatabaseGrpcClient mockClient = mock(DatabaseGrpcClient.class);
      clientField.set(solidityNode, mockClient);

      // Happy path: matching block returned directly.
      Mockito.when(mockClient.getBlock(7L)).thenReturn(blockWithNum(7L));
      Block result = (Block) m.invoke(solidityNode, 7L);
      assertEquals(7L, result.getBlockHeader().getRawData().getNumber());

      // Null response: warn + retry, succeed on second call.
      Mockito.when(mockClient.getBlock(5L))
          .thenReturn(null)
          .thenReturn(blockWithNum(5L));
      result = (Block) m.invoke(solidityNode, 5L);
      assertEquals(5L, result.getBlockHeader().getRawData().getNumber());
      Mockito.verify(mockClient, Mockito.times(2)).getBlock(5L);

      // RPC exception: log + retry, succeed on second call.
      Mockito.when(mockClient.getBlock(8L))
          .thenThrow(new RuntimeException("rpc error"))
          .thenReturn(blockWithNum(8L));
      result = (Block) m.invoke(solidityNode, 8L);
      assertEquals(8L, result.getBlockHeader().getRawData().getNumber());
    } finally {
      clientField.set(solidityNode, orig);
    }
  }

  /**
   * getBlockByNum() shutdown paths: must throw RuntimeException (not return
   * null) in two cases so callers can detect closure cleanly:
   * - flag=false before the loop starts (immediate exit),
   * - wrong block number returned and flag races to false during the retry sleep.
   */
  @Test(timeout = 5000)
  public void testGetBlockByNumWhenClosed() throws Exception {
    Method m = SolidityNode.class.getDeclaredMethod("getBlockByNum", long.class);
    m.setAccessible(true);

    // flag=false: while condition exits immediately.
    setFlag(false);
    try {
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

    // Wrong block number returned: flag goes false → loop exits → throws.
    DatabaseGrpcClient mockClient = mock(DatabaseGrpcClient.class);
    Mockito.when(mockClient.getBlock(9L)).thenAnswer(inv -> {
      setFlag(false);
      return blockWithNum(999L);
    });
    Field clientField = getField("databaseGrpcClient");
    Object orig = clientField.get(solidityNode);
    clientField.set(solidityNode, mockClient);
    try {
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

  // ── getLastSolidityBlockNum() ─────────────────────────────────────────────────

  /**
   * getLastSolidityBlockNum() normal-path and retry:
   * - happy path: returns the value from getDynamicProperties(),
   * - RPC exception: logs, sleeps, and returns the value on the second attempt.
   */
  @Test(timeout = 4000)
  public void testGetLastSolidityBlockNum() throws Exception {
    Method m = SolidityNode.class.getDeclaredMethod("getLastSolidityBlockNum");
    m.setAccessible(true);
    Field clientField = getField("databaseGrpcClient");
    Object orig = clientField.get(solidityNode);
    try {
      DatabaseGrpcClient mockClient = mock(DatabaseGrpcClient.class);
      clientField.set(solidityNode, mockClient);

      // Happy path.
      Mockito.when(mockClient.getDynamicProperties())
          .thenReturn(DynamicProperties.newBuilder().setLastSolidityBlockNum(99L).build());
      assertEquals(99L, (long) m.invoke(solidityNode));

      // RPC exception: retry, return value on second attempt.
      Mockito.when(mockClient.getDynamicProperties())
          .thenThrow(new RuntimeException("rpc error"))
          .thenReturn(DynamicProperties.newBuilder().setLastSolidityBlockNum(50L).build());
      assertEquals(50L, (long) m.invoke(solidityNode));
    } finally {
      clientField.set(solidityNode, orig);
    }
  }

  /**
   * getLastSolidityBlockNum() shutdown paths: must return 0 without looping in
   * two cases:
   * - flag=false before the loop starts (while condition fails),
   * - exception thrown after flag races to false during the gRPC call.
   */
  @Test(timeout = 3000)
  public void testGetLastSolidityBlockNumWhenClosed() throws Exception {
    Method m = SolidityNode.class.getDeclaredMethod("getLastSolidityBlockNum");
    m.setAccessible(true);

    // flag=false: while condition exits immediately, returns 0.
    setFlag(false);
    try {
      assertEquals(0L, (long) m.invoke(solidityNode));
    } finally {
      setFlag(true);
    }

    // Exception while flag races to false: !flag guard returns 0 with INFO.
    DatabaseGrpcClient mockClient = mock(DatabaseGrpcClient.class);
    Mockito.when(mockClient.getDynamicProperties()).thenAnswer(inv -> {
      setFlag(false);
      throw new RuntimeException("channel closed during shutdown");
    });
    Field clientField = getField("databaseGrpcClient");
    Object orig = clientField.get(solidityNode);
    clientField.set(solidityNode, mockClient);
    try {
      assertEquals(0L, (long) m.invoke(solidityNode));
    } finally {
      setFlag(true);
      clientField.set(solidityNode, orig);
    }
  }

  // ── loopProcessBlock() ────────────────────────────────────────────────────────

  /**
   * loopProcessBlock() behaviour across three scenarios:
   * - hitDown=false: solidified block num is persisted after a successful push,
   * - hitDown=true: solidified block num is NOT updated (block not in store),
   * - push throws on first attempt: retries after sleep and succeeds on second.
   */
  @Test(timeout = 6000)
  public void testLoopProcessBlock() throws Exception {
    long origSolidified = chainBaseManager.getDynamicPropertiesStore()
        .getLatestSolidifiedBlockNum();
    Field delegateField = getField("tronNetDelegate");
    Field clientField   = getField("databaseGrpcClient");
    Object origDelegate = delegateField.get(solidityNode);
    Object origClient   = clientField.get(solidityNode);
    try {
      // hitDown=false: solidified block num must be saved.
      TronNetDelegate notHitDown = mock(TronNetDelegate.class);
      Mockito.when(notHitDown.isHitDown()).thenReturn(false);
      delegateField.set(solidityNode, notHitDown);
      invokeLoopProcessBlock(blockWithNum(55L));
      assertEquals(55L, chainBaseManager.getDynamicPropertiesStore()
          .getLatestSolidifiedBlockNum());

      // hitDown=true: solidified block num must NOT change.
      TronNetDelegate hitDown = mock(TronNetDelegate.class);
      Mockito.when(hitDown.isHitDown()).thenReturn(true);
      delegateField.set(solidityNode, hitDown);
      invokeLoopProcessBlock(blockWithNum(56L));
      assertEquals(55L, chainBaseManager.getDynamicPropertiesStore()
          .getLatestSolidifiedBlockNum()); // unchanged

      // Exception on first push: sleep, re-fetch, succeed on second push.
      TronNetDelegate retryDelegate = mock(TronNetDelegate.class);
      Mockito.when(retryDelegate.isHitDown()).thenReturn(false);
      Mockito.doThrow(new RuntimeException("push failed"))
          .doNothing()
          .when(retryDelegate).pushVerifiedBlock(Mockito.any());
      DatabaseGrpcClient mockClient = mock(DatabaseGrpcClient.class);
      Mockito.when(mockClient.getBlock(33L)).thenReturn(blockWithNum(33L));
      delegateField.set(solidityNode, retryDelegate);
      clientField.set(solidityNode, mockClient);
      invokeLoopProcessBlock(blockWithNum(33L));
      assertEquals(33L, chainBaseManager.getDynamicPropertiesStore()
          .getLatestSolidifiedBlockNum());
    } finally {
      chainBaseManager.getDynamicPropertiesStore()
          .saveLatestSolidifiedBlockNum(origSolidified);
      delegateField.set(solidityNode, origDelegate);
      clientField.set(solidityNode, origClient);
    }
  }

  // ── getBlock() ────────────────────────────────────────────────────────────────

  /**
   * getBlock() must fetch a block via gRPC, place it in blockQueue, then exit
   * when flag becomes false after the first successful fetch.
   */
  @Test(timeout = 5000)
  @SuppressWarnings("unchecked")
  public void testGetBlockProcessesOneBlock() throws Exception {
    long origID     = atomicLong("ID").get();
    long origRemote = atomicLong("remoteBlockNum").get();

    atomicLong("ID").set(0L);
    atomicLong("remoteBlockNum").set(2L);

    DatabaseGrpcClient mockClient = mock(DatabaseGrpcClient.class);
    Mockito.when(mockClient.getBlock(1L)).thenAnswer(inv -> {
      setFlag(false);
      return blockWithNum(1L);
    });

    TronNetDelegate mockDelegate = mock(TronNetDelegate.class);
    Mockito.when(mockDelegate.isHitDown()).thenReturn(false);

    Field clientField   = getField("databaseGrpcClient");
    Field delegateField = getField("tronNetDelegate");
    Object origClient   = clientField.get(solidityNode);
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
      Block peeked = queue.peek();
      Assert.assertNotNull("blockQueue must contain the fetched block", peeked);
      assertEquals(1L, peeked.getBlockHeader().getRawData().getNumber());
    } finally {
      setFlag(true);
      queue.clear();
      atomicLong("ID").set(origID);
      atomicLong("remoteBlockNum").set(origRemote);
      clientField.set(solidityNode, origClient);
      delegateField.set(solidityNode, origDelegate);
    }
  }

  /**
   * getBlock() shutdown paths:
   * - interrupted in blockQueue.put() by shutdownNow(): must exit cleanly with
   *   INFO (root cause of the original "reason: null" ERROR bug),
   * - exception thrown while flag is already false: must exit cleanly with INFO
   *   instead of logging ERROR and retrying.
   */
  @Test(timeout = 8000)
  @SuppressWarnings("unchecked")
  public void testGetBlockShutdownPaths() throws Exception {
    long origID     = atomicLong("ID").get();
    long origRemote = atomicLong("remoteBlockNum").get();
    Field clientField   = getField("databaseGrpcClient");
    Field delegateField = getField("tronNetDelegate");
    Object origClient   = clientField.get(solidityNode);
    Object origDelegate = delegateField.get(solidityNode);

    LinkedBlockingDeque<Block> queue =
        (LinkedBlockingDeque<Block>) getField("blockQueue").get(solidityNode);
    try {
      // ── Part 1: interrupt during blockQueue.put() ──────────────────────────
      // Fill the queue to capacity so the next put() call blocks.
      for (int i = 0; i < 100; i++) {
        queue.offer(blockWithNum(i));
      }
      assertEquals(100, queue.size());

      atomicLong("ID").set(0L);
      atomicLong("remoteBlockNum").set(10L);

      DatabaseGrpcClient putClient = mock(DatabaseGrpcClient.class);
      Mockito.when(putClient.getBlock(1L)).thenReturn(blockWithNum(1L));
      TronNetDelegate mockDelegate = mock(TronNetDelegate.class);
      Mockito.when(mockDelegate.isHitDown()).thenReturn(false);
      clientField.set(solidityNode, putClient);
      delegateField.set(solidityNode, mockDelegate);

      Method getBlockM = SolidityNode.class.getDeclaredMethod("getBlock");
      getBlockM.setAccessible(true);
      Thread t = new Thread(() -> {
        try {
          getBlockM.invoke(solidityNode);
        } catch (Exception e) {
          Thread.currentThread().interrupt();
        }
      });
      t.start();
      Thread.sleep(200); // let the thread block inside blockQueue.put()
      t.interrupt();     // simulate ExecutorService.shutdownNow()
      t.join(4000);
      assertFalse("getBlock must exit cleanly when interrupted during put()", t.isAlive());
      queue.clear();
      setFlag(true);

      // ── Part 2: exception while flag is false ──────────────────────────────
      atomicLong("ID").set(0L);
      atomicLong("remoteBlockNum").set(10L);

      DatabaseGrpcClient closingClient = mock(DatabaseGrpcClient.class);
      Mockito.when(closingClient.getBlock(1L)).thenAnswer(inv -> {
        setFlag(false); // shutdown races with this gRPC call
        throw new RuntimeException("channel closed during shutdown");
      });
      clientField.set(solidityNode, closingClient);
      delegateField.set(solidityNode, mockDelegate);

      // Must return without throwing and without infinite retry.
      getBlockM.invoke(solidityNode);
    } finally {
      setFlag(true);
      queue.clear();
      atomicLong("ID").set(origID);
      atomicLong("remoteBlockNum").set(origRemote);
      clientField.set(solidityNode, origClient);
      delegateField.set(solidityNode, origDelegate);
    }
  }

  // ── processSolidityBlock() ────────────────────────────────────────────────────

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
