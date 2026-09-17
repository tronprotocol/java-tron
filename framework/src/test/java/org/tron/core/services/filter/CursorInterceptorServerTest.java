package org.tron.core.services.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.api.DatabaseGrpc;
import org.tron.api.DatabaseGrpc.DatabaseImplBase;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.core.db.Manager;
import org.tron.core.db2.core.Chainbase;
import org.tron.protos.Protocol.Block;

/**
 * Drives the production interceptor through a real gRPC server, which is the only place the
 * assumption it rests on can be checked: that gRPC runs the handler inline from
 * {@code onHalfClose}, on the same thread. The cursor is a {@link ThreadLocal}, so if that stops
 * holding the cursor never reaches the read path and the port serves HEAD data with no error —
 * responses stay well-formed, so nothing else notices.
 *
 * <p>CursorInterceptorScopeTest covers the interceptor's own logic on a synthetic harness; this is
 * the end-to-end half.
 */
public class CursorInterceptorServerTest {

  private ExecutorService executor;

  @Before
  public void setUp() {
    // a fixed thread pool mirrors the production server configuration
    executor = Executors.newFixedThreadPool(2, r -> {
      Thread thread = new Thread(r);
      thread.setName("cursor-rpc-executor-" + thread.getId());
      return thread;
    });
  }

  @After
  public void tearDown() throws Exception {
    if (executor != null) {
      executor.shutdown();
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    }
  }

  @Test
  public void testCursorIsSetAndRestoredOnTheHandlerThread() throws Exception {
    final List<String> setOn = new CopyOnWriteArrayList<>();
    final List<String> resetOn = new CopyOnWriteArrayList<>();
    final String[] handlerOn = new String[1];

    Manager manager = mock(Manager.class);
    doAnswer(inv -> setOn.add(Thread.currentThread().getName()))
        .when(manager).setCursor(any(Chainbase.Cursor.class));
    doAnswer(inv -> resetOn.add(Thread.currentThread().getName()))
        .when(manager).resetCursor();

    SolidityCursorInterceptor interceptor = new SolidityCursorInterceptor();
    Field dbManager = CursorServerInterceptor.class.getDeclaredField("dbManager");
    dbManager.setAccessible(true);
    dbManager.set(interceptor, manager);

    int port = freePort();
    Server server = ServerBuilder.forPort(port)
        .executor(executor)
        .addService(new DatabaseImplBase() {
          @Override
          public void getNowBlock(EmptyMessage request, StreamObserver<Block> observer) {
            handlerOn[0] = Thread.currentThread().getName();
            observer.onNext(Block.getDefaultInstance());
            observer.onCompleted();
          }
        })
        .intercept(interceptor)
        .build()
        .start();

    ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", port)
        .usePlaintext().directExecutor().build();
    try {
      DatabaseGrpc.newBlockingStub(channel).getNowBlock(EmptyMessage.getDefaultInstance());

      Assert.assertEquals("cursor must be set exactly once per call", 1, setOn.size());
      Assert.assertEquals("cursor must be restored exactly once per call", 1, resetOn.size());
      Assert.assertNotNull("handler did not run", handlerOn[0]);
      // the ThreadLocal cursor only reaches the read path if it is set on the handler's thread
      Assert.assertEquals("cursor was set on a thread other than the handler's",
          handlerOn[0], setOn.get(0));
      Assert.assertEquals("cursor was restored on a thread other than the handler's",
          handlerOn[0], resetOn.get(0));
      verify(manager).setCursor(Chainbase.Cursor.SOLIDITY);
    } finally {
      channel.shutdownNow();
      server.shutdownNow();
      server.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  private static int freePort() throws Exception {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }
}
