package org.tron.common.backup;

import io.netty.channel.Channel;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.tron.common.TestConstants;
import org.tron.common.backup.socket.BackupServer;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.PublicMethod;
import org.tron.core.config.args.Args;


public class BackupServerTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public Timeout globalTimeout = Timeout.seconds(60);
  private BackupServer backupServer;
  private BackupManager backupManager;
  private boolean backupServerClosed;

  @Before
  public void setUp() throws Exception {
    Args.setParam(new String[]{"-d", temporaryFolder.newFolder().toString()},
        TestConstants.TEST_CONF);
    CommonParameter.getInstance().setBackupPort(PublicMethod.chooseRandomPort());
    List<String> members = new ArrayList<>();
    members.add("127.0.0.2");
    CommonParameter.getInstance().setBackupMembers(members);
    backupManager = new BackupManager();
    backupManager.init();
    backupServer = new BackupServer(backupManager);
  }

  @After
  public void tearDown() throws Exception {
    Throwable failure = null;
    try {
      if (!backupServerClosed && backupServer != null) {
        backupServer.close();
      }
    } catch (Throwable t) {
      failure = t;
    } finally {
      try {
        if (backupManager != null) {
          backupManager.stop();
        }
        assertExecutorsTerminated();
      } catch (Throwable t) {
        if (failure == null) {
          failure = t;
        } else {
          failure.addSuppressed(t);
        }
      } finally {
        Args.clearParam();
      }
    }
    if (failure != null) {
      throw new AssertionError("backup test cleanup failed", failure);
    }
  }

  @Test(timeout = 60_000)
  public void test() throws Exception {
    backupServer.initServer();
    awaitCondition("backup channel to become active", () -> getChannel() != null
        && getChannel().isActive());
    Channel channel = getChannel();
    Assert.assertTrue("backup channel must be active after startup", channel.isActive());

    backupServer.close();
    backupServerClosed = true;

    Assert.assertFalse("backup channel must close", channel.isOpen());
    assertExecutorsTerminated();
  }

  private Channel getChannel() {
    try {
      return getField(backupServer, "channel");
    } catch (Exception e) {
      throw new AssertionError("cannot inspect backup channel", e);
    }
  }

  private void assertExecutorsTerminated() throws Exception {
    if (backupManager == null || backupServer == null) {
      return;
    }
    ExecutorService managerExecutor = getField(backupManager, "executorService");
    Assert.assertTrue("backup manager executor must terminate", managerExecutor.isTerminated());
    ExecutorService serverExecutor = getField(backupServer, "executor");
    if (serverExecutor != null) {
      Assert.assertTrue("backup server executor must terminate", serverExecutor.isTerminated());
    }
  }

  private void awaitCondition(String description, BooleanSupplier condition) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
    while (System.nanoTime() < deadline) {
      if (condition.getAsBoolean()) {
        return;
      }
      Thread.sleep(20);
    }
    Assert.fail("timed out waiting for " + description);
  }

  @SuppressWarnings("unchecked")
  private <T> T getField(Object target, String name) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return (T) field.get(target);
  }
}
