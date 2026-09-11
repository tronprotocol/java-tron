package org.tron.core.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TronLogShutdownHookTest {

  private boolean originalShutDown;
  private Thread runner;
  private final AtomicReference<Throwable> failure = new AtomicReference<>();

  @Before
  public void saveShutDownFlag() {
    originalShutDown = TronLogShutdownHook.shutDown;
  }

  @After
  public void restoreShutDownFlag() throws InterruptedException {
    try {
      if (runner != null) {
        runner.interrupt();
        runner.join(5000);
        assertFalse("Shutdown hook worker leaked", runner.isAlive());
      }
      assertNull("Shutdown hook worker failed", failure.get());
    } finally {
      TronLogShutdownHook.shutDown = originalShutDown;
    }
  }

  @Test(timeout = 5000)
  public void returnsImmediatelyWhenAlreadyShutDown() {
    TronLogShutdownHook.shutDown = true;
    new TronLogShutdownHook().run();
  }

  @Test(timeout = 10_000)
  public void wakesUpWhenShutDownFlagFlips() throws InterruptedException {
    TronLogShutdownHook.shutDown = false;
    CountDownLatch waiting = new CountDownLatch(1);
    TronLogShutdownHook hook = observedHook(waiting);
    startWorker(hook);
    assertTrue("Hook did not enter its wait loop", waiting.await(5, TimeUnit.SECONDS));
    TronLogShutdownHook.shutDown = true;
    runner.join(5000);
    assertFalse("Hook did not exit after flag flipped", runner.isAlive());
  }

  @Test(timeout = 10_000)
  public void preservesInterruptStatusWhenInterrupted() throws InterruptedException {
    TronLogShutdownHook.shutDown = false;
    CountDownLatch waiting = new CountDownLatch(1);
    TronLogShutdownHook hook = observedHook(waiting);
    AtomicBoolean interruptedAfterRun = new AtomicBoolean();
    startWorker(() -> {
      hook.run();
      interruptedAfterRun.set(Thread.currentThread().isInterrupted());
    });
    assertTrue("Hook did not enter its wait loop", waiting.await(5, TimeUnit.SECONDS));
    runner.interrupt();
    runner.join(5000);
    assertFalse("Hook did not exit after interrupt", runner.isAlive());
    assertTrue("Hook did not preserve interrupt status", interruptedAfterRun.get());
  }

  private TronLogShutdownHook observedHook(CountDownLatch waiting) {
    return new TronLogShutdownHook() {
      @Override
      public void addInfo(String message) {
        waiting.countDown();
      }
    };
  }

  private void startWorker(Runnable task) {
    runner = new Thread(task, "shutdown-hook-test");
    runner.setDaemon(true);
    runner.setUncaughtExceptionHandler((thread, error) -> failure.set(error));
    runner.start();
  }
}
