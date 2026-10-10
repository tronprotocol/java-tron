package org.tron.core.services.jsonrpc.filters;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import java.util.BitSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.tron.core.services.jsonrpc.ApiLogCapture;
import org.tron.core.services.jsonrpc.TronJsonRpc.FilterRequest;
import org.tron.core.store.SectionBloomStore;

public class LogBlockQueryFailureTest {

  private static final long CURRENT_MAX_BLOCK_NUM = 100L;
  private static final String SENSITIVE_MARKER = "worker-sensitive-marker";

  @Test
  public void testExecutionFailureLogsCauseAndRethrowsWrapper() throws Exception {
    NullPointerException cause = new NullPointerException(SENSITIVE_MARKER);
    ExecutionException failure = new ExecutionException(cause);
    LogBlockQuery query = newQuery(futureThrowing(failure));

    try (ApiLogCapture logs = new ApiLogCapture()) {
      ExecutionException thrown = Assert.assertThrows(ExecutionException.class,
          query::getPossibleBlock);

      Assert.assertSame(failure, thrown);
      ILoggingEvent event = findThrowableEvent(logs, NullPointerException.class,
          SENSITIVE_MARKER);
      Assert.assertNotNull(event);
      Assert.assertEquals(Level.DEBUG, event.getLevel());
      Assert.assertEquals(1, logs.events().size());
      Assert.assertEquals(0, countEvents(logs, Level.WARN));
    }
  }

  @Test
  public void testWrappedFatalIsRethrownWithoutFailureLog() throws Exception {
    OutOfMemoryError fatal = new OutOfMemoryError("fatal-marker");
    LogBlockQuery query = newQuery(futureThrowing(new ExecutionException(fatal)));

    try (ApiLogCapture logs = new ApiLogCapture()) {
      Assert.assertSame(fatal,
          Assert.assertThrows(OutOfMemoryError.class, query::getPossibleBlock));
      Assert.assertTrue("a fatal cause must not be logged", logs.events().isEmpty());
    }
  }

  @Test(timeout = 10000)
  public void testFatalFromRealExecutorIsRethrownWithoutFailureLog() throws Exception {
    // A real FutureTask records the Error thrown by the task and exposes it wrapped in an
    // ExecutionException from Future.get(); this is the path the fatal check has to cover.
    OutOfMemoryError fatal = new OutOfMemoryError("fatal-marker");
    SectionBloomStore store = mock(SectionBloomStore.class);
    when(store.get(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenThrow(fatal);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    LogBlockQuery query = new LogBlockQuery(newWrapper(), store, CURRENT_MAX_BLOCK_NUM,
        executor);

    try (ApiLogCapture logs = new ApiLogCapture()) {
      Assert.assertSame(fatal,
          Assert.assertThrows(OutOfMemoryError.class, query::getPossibleBlock));
      Assert.assertTrue("a fatal cause must not be logged", logs.events().isEmpty());
    } finally {
      executor.shutdownNow();
      Assert.assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  @Test
  public void testInterruptionRestoresFlagAndRethrows() throws Exception {
    Thread.interrupted();
    InterruptedException failure = new InterruptedException();
    LogBlockQuery query = newQuery(futureThrowing(failure));

    try (ApiLogCapture logs = new ApiLogCapture()) {
      InterruptedException thrown = Assert.assertThrows(InterruptedException.class,
          query::getPossibleBlock);

      Assert.assertSame(failure, thrown);
      Assert.assertTrue(Thread.currentThread().isInterrupted());
      ILoggingEvent event = findThrowableEvent(logs, InterruptedException.class, null);
      Assert.assertNotNull(event);
      Assert.assertEquals(Level.DEBUG, event.getLevel());
      Assert.assertEquals(1, logs.events().size());
      Assert.assertEquals(0, countEvents(logs, Level.WARN));
    } finally {
      Thread.interrupted();
    }
  }

  private static LogBlockQuery newQuery(Future<BitSet> future) throws Exception {
    ExecutorService executor = mock(ExecutorService.class);
    when(executor.submit(ArgumentMatchers.<Callable<BitSet>>any())).thenReturn(future);
    SectionBloomStore store = mock(SectionBloomStore.class);
    return new LogBlockQuery(newWrapper(), store, CURRENT_MAX_BLOCK_NUM, executor);
  }

  private static LogFilterWrapper newWrapper() throws Exception {
    return new LogFilterWrapper(
        new FilterRequest("0x0", "0x1",
            "0x1111111111111111111111111111111111111111", null, null),
        CURRENT_MAX_BLOCK_NUM, null, false);
  }

  @SuppressWarnings("unchecked")
  private static Future<BitSet> futureThrowing(Exception failure) throws Exception {
    Future<BitSet> future = mock(Future.class);
    when(future.get()).thenThrow(failure);
    return future;
  }

  private static ILoggingEvent findThrowableEvent(ApiLogCapture logs,
      Class<? extends Throwable> type, String message) {
    for (ILoggingEvent event : logs.events()) {
      IThrowableProxy throwable = event.getThrowableProxy();
      if (throwable != null && type.getName().equals(throwable.getClassName())
          && (message == null || message.equals(throwable.getMessage()))) {
        return event;
      }
    }
    return null;
  }

  private static int countEvents(ApiLogCapture logs, Level level) {
    int count = 0;
    for (ILoggingEvent event : logs.events()) {
      if (event.getLevel() == level) {
        count++;
      }
    }
    return count;
  }
}
