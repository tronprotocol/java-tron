package org.tron.common.backup;

import io.netty.channel.Channel;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.Assert;
import org.junit.function.ThrowingRunnable;
import org.tron.common.backup.socket.BackupServer;
import org.tron.common.utils.ReflectUtils;

/**
 * Shared reflection/await/cleanup helpers for backup tests. Assertion messages and timeout
 * parameters mirror the helpers they replace, so failure output is unchanged.
 */
public final class BackupTestUtils {

  private BackupTestUtils() {
  }

  public static Channel getChannel(BackupServer server) {
    try {
      return (Channel) ReflectUtils.getFieldObject(server, "channel");
    } catch (Exception e) {
      throw new AssertionError("cannot inspect backup channel", e);
    }
  }

  public static void awaitCondition(String description, BooleanSupplier condition)
      throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
    while (System.nanoTime() < deadline) {
      if (condition.getAsBoolean()) {
        return;
      }
      Thread.sleep(20);
    }
    Assert.fail("timed out waiting for " + description);
  }

  public static void assertExecutorsTerminated(BackupManager manager, BackupServer server)
      throws Exception {
    if (manager == null || server == null) {
      return;
    }
    ExecutorService managerExecutor =
        (ExecutorService) ReflectUtils.getFieldObject(manager, "executorService");
    Assert.assertTrue("backup manager executor must terminate", managerExecutor.isTerminated());
    ExecutorService serverExecutor =
        (ExecutorService) ReflectUtils.getFieldObject(server, "executor");
    if (serverExecutor != null) {
      Assert.assertTrue("backup server executor must terminate", serverExecutor.isTerminated());
    }
  }

  public static void runQuietly(List<Throwable> errors, ThrowingRunnable step) {
    try {
      step.run();
    } catch (Throwable t) {
      errors.add(t);
    }
  }

  public static void throwIfAnyError(List<Throwable> errors) {
    if (!errors.isEmpty()) {
      AssertionError failure = new AssertionError("backup test cleanup failed");
      errors.forEach(failure::addSuppressed);
      throw failure;
    }
  }
}
