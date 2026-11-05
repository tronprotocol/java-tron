package org.tron.core.services.stop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.capsule.BlockCapsule;

public class BlockStopOverlapTest extends ConditionallyStopTest {

  private static final long HEIGHT = 5;
  private static final String EXECUTOR_NAME = "waiting-block";

  private final ExecutorService executor =
      ExecutorServiceManager.newSingleThreadExecutor(EXECUTOR_NAME);

  @Override
  protected void initParameter(CommonParameter parameter) {
    parameter.setShutdownBlockHeight(HEIGHT);
    parameter.getStorage().setMaxFlushCount(1);
  }

  @Override
  protected void beforeDestroy() {
    ExecutorServiceManager.shutdownAndAwaitTermination(executor, EXECUTOR_NAME);
  }

  @Override
  protected void check() {
    assertEquals(HEIGHT, persistedHeight());
  }

  @Test(timeout = 30_000)
  public void blockWaitingForTheLockStopsAtTheTargetHeight() throws Exception {
    BlockCapsule first = nextBlockBeforeTargetIsPersisted();
    BlockCapsule second = blockAfter(first);
    Future<?> waiting;

    synchronized (tronNetDelegate.getBlockLock()) {
      waiting = startWaitingForTheLock(second);
      tronNetDelegate.processBlock(first, false);
      assertEquals(HEIGHT, persistedHeight());
    }
    waiting.get();

    assertTrue(tronNetDelegate.isHitDown());
    assertStoppedAfter(first);
  }

  @Test(timeout = 30_000)
  public void blockWaitingForTheLockIsDroppedAfterTheStopSignal() throws Exception {
    BlockCapsule first = nextBlockBeforeTargetIsPersisted();
    BlockCapsule second = blockAfter(first);
    Future<?> waiting;

    synchronized (tronNetDelegate.getBlockLock()) {
      waiting = startWaitingForTheLock(second);
      tronNetDelegate.processBlock(first, false);
      tronNetDelegate.processBlock(second, false);
      assertTrue(tronNetDelegate.isHitDown());
    }
    waiting.get();

    assertStoppedAfter(first);
  }

  private BlockCapsule nextBlockBeforeTargetIsPersisted() throws Exception {
    while (persistedHeight() < HEIGHT - 1) {
      generateBlock();
    }
    return createTestBlockCapsule(
        chainManager.getNextBlockSlotTime(),
        headHeight() + 1,
        chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString());
  }

  private BlockCapsule blockAfter(BlockCapsule block) {
    return createTestBlockCapsule(block.getTimeStamp() + 3000, block.getNum() + 1,
        block.getBlockId().getByteString());
  }

  private Future<?> startWaitingForTheLock(BlockCapsule block) throws InterruptedException {
    AtomicReference<Thread> worker = new AtomicReference<>();
    Future<?> task = executor.submit(() -> {
      worker.set(Thread.currentThread());
      tronNetDelegate.processBlock(block, false);
      return null;
    });
    while (worker.get() == null || worker.get().getState() != Thread.State.BLOCKED) {
      assertFalse(task.isDone());
      Thread.sleep(1);
    }
    return task;
  }

  private void assertStoppedAfter(BlockCapsule block) {
    assertEquals(HEIGHT, persistedHeight());
    assertEquals(block.getNum(), headHeight());
  }

  private long persistedHeight() {
    return dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumberFromDB();
  }

  private long headHeight() {
    return dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
  }
}
