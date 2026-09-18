package org.tron.core.services.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.core.db.Manager;
import org.tron.core.db2.core.Chainbase;

/**
 * Pins the cursor's scope without depending on thread-pool scheduling.
 *
 * <p>{@code interceptCall()} is driven on thread A and the returned listener's
 * {@code onHalfClose()} on a different thread B, which is what gRPC's {@code SerializingExecutor}
 * is free to do. An implementation that scoped the cursor around {@code interceptCall} instead of
 * {@code onHalfClose} then fails here by construction rather than by luck.
 */
public class CursorInterceptorScopeTest {

  private ExecutorService threadA;
  private ExecutorService threadB;

  private Manager manager;
  private Chainbase.Cursor cursorDuringHandler;
  private Chainbase.Cursor cursorAfterCall;
  private Chainbase.Cursor current;

  @Before
  public void setUp() {
    threadA = Executors.newSingleThreadExecutor(r -> new Thread(r, "cursor-thread-A"));
    threadB = Executors.newSingleThreadExecutor(r -> new Thread(r, "cursor-thread-B"));

    // a Manager whose cursor state is observable, standing in for the ThreadLocal in Chainbase
    current = Chainbase.Cursor.HEAD;
    manager = mock(Manager.class);
    doAnswer(inv -> current = inv.getArgument(0))
        .when(manager).setCursor(any(Chainbase.Cursor.class));
    doAnswer(inv -> current = Chainbase.Cursor.HEAD).when(manager).resetCursor();
  }

  @After
  public void tearDown() throws Exception {
    threadA.shutdownNow();
    threadB.shutdownNow();
    threadA.awaitTermination(5, TimeUnit.SECONDS);
    threadB.awaitTermination(5, TimeUnit.SECONDS);
  }

  @Test
  public void testHandlerSeesTheCursorWhenInterceptCallRanOnAnotherThread() throws Exception {
    ServerCall.Listener<Object> listener = startCallOnThreadA(false);

    // the handler runs from onHalfClose, on a different thread than interceptCall
    runOn(threadB, () -> {
      listener.onHalfClose();
      return null;
    });

    Assert.assertEquals("handler must observe the SOLIDITY cursor",
        Chainbase.Cursor.SOLIDITY, cursorDuringHandler);
    Assert.assertEquals("cursor must be back at HEAD once the handler returns",
        Chainbase.Cursor.HEAD, cursorAfterCall);
  }

  @Test
  public void testCursorIsRestoredOnThreadBWhenTheHandlerThrows() throws Exception {
    ServerCall.Listener<Object> listener = startCallOnThreadA(true);

    try {
      runOn(threadB, () -> {
        listener.onHalfClose();
        return null;
      });
      Assert.fail("expected the handler failure to propagate");
    } catch (Exception expected) {
      // what matters is the cursor state below
    }

    Assert.assertEquals("a throwing handler must still leave the cursor at HEAD",
        Chainbase.Cursor.HEAD, current);
  }

  /** Runs interceptCall on thread A and returns the listener, with a handler that records state. */
  private ServerCall.Listener<Object> startCallOnThreadA(boolean handlerThrows) throws Exception {
    SolidityCursorInterceptor interceptor = new SolidityCursorInterceptor();
    Field dbManager = CursorServerInterceptor.class.getDeclaredField("dbManager");
    dbManager.setAccessible(true);
    dbManager.set(interceptor, manager);

    @SuppressWarnings("unchecked")
    ServerCall<Object, Object> call = mock(ServerCall.class);
    @SuppressWarnings("unchecked")
    MethodDescriptor<Object, Object> descriptor = mock(MethodDescriptor.class);
    doAnswer(inv -> descriptor).when(call).getMethodDescriptor();

    ServerCallHandler<Object, Object> handler = (c, h) -> new ServerCall.Listener<Object>() {
      @Override
      public void onHalfClose() {
        cursorDuringHandler = current;
        if (handlerThrows) {
          throw new IllegalStateException("boom");
        }
      }
    };

    return runOn(threadA, () -> {
      ServerCall.Listener<Object> l = interceptor.interceptCall(call, new Metadata(), handler);
      cursorAfterCall = current;
      return l;
    });
  }

  private static <T> T runOn(ExecutorService executor, Callable<T> task) throws Exception {
    try {
      return executor.submit(task).get(5, TimeUnit.SECONDS);
    } catch (java.util.concurrent.ExecutionException e) {
      if (e.getCause() instanceof Exception) {
        throw (Exception) e.getCause();
      }
      throw e;
    }
  }
}
