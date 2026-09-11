package org.tron.common.backup;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import io.netty.channel.Channel;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.common.TestConstants;
import org.tron.common.backup.socket.BackupServer;
import org.tron.common.backup.socket.MessageHandler;
import org.tron.common.utils.PublicMethod;
import org.tron.core.config.args.Args;

public class BackupServerLifecycleTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    Args.setParam(new String[]{"-d", temporaryFolder.newFolder().toString()},
        TestConstants.TEST_CONF);
    Args.getInstance().setBackupPort(PublicMethod.chooseRandomPort());
    Args.getInstance().setBackupMembers(Collections.singletonList("127.0.0.2"));
  }

  @After
  public void tearDown() {
    Args.clearParam();
  }

  @Test(timeout = 30_000)
  public void testCloseDuringBind() throws Exception {
    BackupManager manager = mock(BackupManager.class);
    CountDownLatch bindStarted = new CountDownLatch(1);
    CountDownLatch allowBind = new CountDownLatch(1);
    CountDownLatch closeWaiting = new CountDownLatch(1);
    AtomicReference<Channel> pendingChannel = new AtomicReference<>();
    doAnswer(invocation -> {
      MessageHandler handler = invocation.getArgument(0);
      pendingChannel.set((Channel) ReflectionTestUtils.getField(handler, "channel"));
      bindStarted.countDown();
      if (!allowBind.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timed out waiting to finish bind");
      }
      return null;
    }).when(manager).setMessageHandler(any(MessageHandler.class));

    BackupServer server = new BackupServer(manager);
    ExecutorService closer = Executors.newSingleThreadExecutor();
    ExecutorService worker = null;
    try {
      server.initServer();
      worker = (ExecutorService) ReflectionTestUtils.getField(server, "executor");
      Assert.assertTrue("Bind did not start", bindStarted.await(10, TimeUnit.SECONDS));
      ExecutorService delegate = worker;
      ExecutorService observedWorker = mock(ExecutorService.class, delegatesTo(delegate));
      doAnswer(invocation -> {
        delegate.shutdown();
        closeWaiting.countDown();
        return null;
      }).when(observedWorker).shutdown();
      ReflectionTestUtils.setField(server, "executor", observedWorker);

      Future<?> close = closer.submit(server::close);
      // Ensure close() has checked the still-null channel before completing bind.
      Assert.assertTrue("Close did not reach executor shutdown",
          closeWaiting.await(5, TimeUnit.SECONDS));
      allowBind.countDown();
      close.get(5, TimeUnit.SECONDS);
      Assert.assertFalse("The late-bound channel must be closed", pendingChannel.get().isOpen());
      Assert.assertTrue("The server worker must terminate", worker.isTerminated());
    } finally {
      // Also release resources when running this regression against the broken implementation.
      allowBind.countDown();
      Channel channel = pendingChannel.get();
      if (channel != null) {
        channel.close().awaitUninterruptibly(5, TimeUnit.SECONDS);
      }
      if (worker != null) {
        worker.shutdown();
        if (!worker.awaitTermination(5, TimeUnit.SECONDS)) {
          worker.shutdownNow();
        }
      }
      closer.shutdownNow();
      closer.awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
