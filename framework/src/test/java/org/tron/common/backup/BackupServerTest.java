package org.tron.common.backup;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;

import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import java.io.IOException;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.common.TestConstants;
import org.tron.common.backup.socket.BackupServer;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.Args;


public class BackupServerTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public Timeout globalTimeout = Timeout.seconds(60);
  private BackupManager backupManager;
  private BackupServer backupServer;

  @Before
  public void setUp() throws Exception {
    Args.setParam(new String[]{"-d", temporaryFolder.newFolder().toString()},
        TestConstants.TEST_CONF);
    CommonParameter.getInstance().setBackupPort(freeUdpPort());
    List<String> members = new ArrayList<>();
    members.add("127.0.0.2");
    CommonParameter.getInstance().setBackupMembers(members);
    backupManager = new BackupManager();
    backupManager.init();
    backupServer = new BackupServer(backupManager);
  }

  @After
  public void tearDown() {
    backupServer.close();
    Args.clearParam();
  }

  @Test
  public void closeAfterStarted() throws Exception {
    backupServer.initServer();
    assertTrue("server did not bind in time", waitUntil(() -> {
      Channel channel = serverChannel();
      return channel != null && channel.isActive();
    }, 30));

    Channel channel = serverChannel();
    ExecutorService executor =
        (ExecutorService) ReflectionTestUtils.getField(backupServer, "executor");
    backupServer.close();

    Assert.assertNotNull(channel);
    assertFalse("server channel is still open", channel.isOpen());
    assertTrue("event loop group did not terminate", eventLoopGroup().isTerminated());
    Assert.assertNotNull(executor);
    assertTrue("server executor did not terminate",
        executor.awaitTermination(5, TimeUnit.SECONDS));
  }

  @Test
  public void closeWhileBindIsPending() throws Exception {
    ExecutorService executor = ExecutorServiceManager.newSingleThreadExecutor("BackupServer");
    CountDownLatch parked = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    FutureTask<Void> closed = new FutureTask<>(backupServer::close, null);
    Thread closer = new Thread(closed, "backup-test-closer");
    try {
      try (MockedStatic<ExecutorServiceManager> factory =
          mockStatic(ExecutorServiceManager.class)) {
        factory.when(() -> ExecutorServiceManager.newSingleThreadExecutor("BackupServer"))
            .thenAnswer(invocation -> {
              // The group exists, but the server task has not been submitted yet.
              eventLoopGroup().next().execute(() -> {
                parked.countDown();
                try {
                  release.await();
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
              });
              assertTrue("event loop did not park", parked.await(5, TimeUnit.SECONDS));
              return executor;
            });
        backupServer.initServer();
      }

      NioEventLoopGroup group = eventLoopGroup();
      SingleThreadEventExecutor loop = (SingleThreadEventExecutor) group.next();
      assertTrue("bind registration did not queue", waitUntil(() -> loop.pendingTasks() > 0, 5));
      closer.start();
      assertTrue("close did not shut down the group", waitUntil(group::isShuttingDown, 5));
      // Let bind proceed only after close() has requested shutdown.
      release.countDown();
      closed.get(15, TimeUnit.SECONDS);
      assertTrue("server executor did not terminate",
          executor.awaitTermination(5, TimeUnit.SECONDS));
      assertTrue("event loop group did not terminate", group.isTerminated());
    } finally {
      // Release the event loop even when a preparation or shutdown assertion fails.
      release.countDown();
      closer.interrupt();
      executor.shutdownNow();
      NioEventLoopGroup group = eventLoopGroup();
      if (group != null) {
        assertTrue("event loop cleanup failed",
            group.shutdownGracefully().awaitUninterruptibly(10, TimeUnit.SECONDS));
      }
      closer.join(10_000);
      assertTrue("executor cleanup failed", executor.awaitTermination(10, TimeUnit.SECONDS));
    }
  }

  private Channel serverChannel() {
    Object handler = ReflectionTestUtils.getField(backupManager, "messageHandler");
    return handler == null ? null : (Channel) ReflectionTestUtils.getField(handler, "channel");
  }

  private NioEventLoopGroup eventLoopGroup() {
    return (NioEventLoopGroup) ReflectionTestUtils.getField(backupServer, "group");
  }

  private boolean waitUntil(BooleanSupplier condition, long timeoutSeconds)
      throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
    while (!condition.getAsBoolean()) {
      if (System.nanoTime() - deadline >= 0) {
        return false;
      }
      TimeUnit.MILLISECONDS.sleep(10);
    }
    return true;
  }

  private int freeUdpPort() throws IOException {
    try (DatagramSocket socket = new DatagramSocket(0)) {
      return socket.getLocalPort();
    }
  }
}
