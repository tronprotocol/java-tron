package org.tron.core.services.stop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.capsule.BlockCapsule;

public class BlockStopOverlapTest extends ConditionallyStopTest {

  private static final long HEIGHT = 5;

  private final AtomicReference<Throwable> waitingError = new AtomicReference<>();

  @Override
  protected void initParameter(CommonParameter parameter) {
    parameter.setShutdownBlockHeight(HEIGHT);
    parameter.getStorage().setMaxFlushCount(1);
  }

  @Override
  protected void check() {
    assertEquals(HEIGHT, persistedHeight());
  }

  @Test(timeout = 30_000)
  public void blockWaitingForTheLockStopsAtTheTargetHeight() throws Exception {
    BlockCapsule first = nextBlockBeforeTargetIsPersisted();
    BlockCapsule second = blockAfter(first);
    Thread waiting;

    synchronized (tronNetDelegate.getBlockLock()) {
      waiting = startWaitingForTheLock(second);
      tronNetDelegate.processBlock(first, false);
      assertEquals(HEIGHT, persistedHeight());
    }
    waiting.join();

    assertNull(waitingError.get());
    assertTrue(tronNetDelegate.isHitDown());
    assertStoppedAfter(first);
  }

  @Test(timeout = 30_000)
  public void blockWaitingForTheLockIsDroppedAfterTheStopSignal() throws Exception {
    BlockCapsule first = nextBlockBeforeTargetIsPersisted();
    BlockCapsule second = blockAfter(first);
    Thread waiting;

    synchronized (tronNetDelegate.getBlockLock()) {
      waiting = startWaitingForTheLock(second);
      tronNetDelegate.processBlock(first, false);
      tronNetDelegate.processBlock(second, false);
      assertTrue(tronNetDelegate.isHitDown());
    }
    waiting.join();

    assertNull(waitingError.get());
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

  private Thread startWaitingForTheLock(BlockCapsule block) throws InterruptedException {
    Thread thread = new Thread(() -> {
      try {
        tronNetDelegate.processBlock(block, false);
      } catch (Throwable t) {
        waitingError.set(t);
      }
    }, "waiting-block");
    thread.start();
    while (thread.getState() != Thread.State.BLOCKED) {
      assertTrue(thread.isAlive());
      Thread.sleep(1);
    }
    return thread;
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
