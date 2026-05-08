package org.tron.core.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TronLogShutdownHookTest {

  private boolean originalShutDown;

  @Before
  public void saveShutDownFlag() {
    originalShutDown = TronLogShutdownHook.shutDown;
  }

  @After
  public void restoreShutDownFlag() {
    TronLogShutdownHook.shutDown = originalShutDown;
  }

  @Test(timeout = 5_000)
  public void returnsImmediatelyWhenAlreadyShutDown() {
    TronLogShutdownHook.shutDown = true;

    TronLogShutdownHook hook = new TronLogShutdownHook();
    long startNs = System.nanoTime();
    hook.run();
    long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
    assertTrue("hook should exit fast when shutDown==true, elapsed=" + elapsedMs + "ms",
        elapsedMs < 2_000);
  }

  @Test(timeout = 10_000)
  public void wakesUpWhenShutDownFlagFlips() throws InterruptedException {
    TronLogShutdownHook.shutDown = false;

    TronLogShutdownHook hook = new TronLogShutdownHook();
    Thread runner = new Thread(hook, "shutdown-hook-test-runner");
    runner.setDaemon(true);
    runner.start();

    Thread.sleep(300);
    long flipNs = System.nanoTime();
    TronLogShutdownHook.shutDown = true;

    runner.join(5_000);
    long elapsedAfterFlipMs =
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - flipNs);

    assertFalse("runner should have exited after flag flipped, still alive",
        runner.isAlive());
    // The loop sleeps in 100 ms slices, so it should wake up well inside one
    // slice's worth of jitter. 1 s is comfortable even on slow CI.
    assertTrue("hook should return shortly after flag flip, elapsed="
        + elapsedAfterFlipMs + "ms", elapsedAfterFlipMs < 1_000);
  }

  @Test(timeout = 10_000)
  public void preservesInterruptStatusWhenInterrupted() throws InterruptedException {
    TronLogShutdownHook.shutDown = false;

    TronLogShutdownHook hook = new TronLogShutdownHook();
    AtomicBoolean interruptedAfterRun = new AtomicBoolean(false);
    Thread runner = new Thread(() -> {
      hook.run();
      interruptedAfterRun.set(Thread.currentThread().isInterrupted());
    }, "shutdown-hook-test-interrupt");
    runner.setDaemon(true);
    runner.start();

    Thread.sleep(200);
    runner.interrupt();

    runner.join(5_000);
    assertFalse("runner should have exited after interrupt", runner.isAlive());
    assertTrue("run() must re-assert interrupt status after catching "
        + "InterruptedException", interruptedAfterRun.get());
  }
}
