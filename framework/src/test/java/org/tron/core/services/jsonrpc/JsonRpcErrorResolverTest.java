package org.tron.core.services.jsonrpc;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.googlecode.jsonrpc4j.ErrorResolver.JsonError;
import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.tron.core.exception.TronError;
import org.tron.core.exception.jsonrpc.JsonRpcException;
import org.tron.core.exception.jsonrpc.JsonRpcInternalException;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidRequestException;

public class JsonRpcErrorResolverTest {

  private static final List<JsonNode> NO_ARGUMENTS = Collections.emptyList();

  private final JsonRpcErrorResolver resolver = JsonRpcErrorResolver.INSTANCE;

  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidRequestException.class, code = -32600, data = "{}"),
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
      @JsonRpcError(exception = JsonRpcInternalException.class, code = -32000, data = "{}"),
      @JsonRpcError(exception = ExecutionException.class, code = -32000, data = "{}"),
      @JsonRpcError(exception = JsonRpcException.class, code = -1)
  })
  public void dummyMethod() {
  }

  @JsonRpcErrors({
      @JsonRpcError(exception = IllegalArgumentException.class, code = -32602,
          message = "annotation message"),
      @JsonRpcError(exception = NullPointerException.class, code = -32602),
      @JsonRpcError(exception = IllegalStateException.class, code = -32600),
      @JsonRpcError(exception = UnsupportedOperationException.class, code = -32601),
      @JsonRpcError(exception = RuntimeException.class, code = -32000)
  })
  public void messageMethod() {
  }

  @JsonRpcMethod("test_unmapped")
  public void unmappedMethod() {
  }

  @JsonRpcMethod("test_unmapped_other")
  public void otherUnmappedMethod() {
  }

  @Before
  public void clearSeenFailures() {
    JsonRpcErrorResolver.clearSeenFailuresForTest();
  }

  @Test
  public void testMappedErrorsPreserveCodeAndDataPriority() throws Exception {
    String message = "JsonRpcInvalidRequestException";
    JsonRpcException exception = new JsonRpcInvalidRequestException(message);
    Method method = getClass().getMethod("dummyMethod");

    JsonError error = resolver.resolveError(exception, method, NO_ARGUMENTS);
    Assert.assertNotNull(error);
    Assert.assertEquals(-32600, error.code);
    Assert.assertEquals(message, error.message);
    Assert.assertEquals("{}", error.data);

    message = "JsonRpcInternalException";
    String data = "JsonRpcInternalException data";
    exception = new JsonRpcInternalException(message, data);
    error = resolver.resolveError(exception, method, NO_ARGUMENTS);

    Assert.assertNotNull(error);
    Assert.assertEquals(-32000, error.code);
    Assert.assertEquals(message, error.message);
    Assert.assertEquals(data, error.data);

    exception = new JsonRpcInternalException(message, null);
    error = resolver.resolveError(exception, method, NO_ARGUMENTS);

    Assert.assertNotNull(error);
    Assert.assertEquals(-32000, error.code);
    Assert.assertEquals(message, error.message);
    Assert.assertEquals("{}", error.data);

    message = "JsonRpcException";
    exception = new JsonRpcException(message, null);
    error = resolver.resolveError(exception, method, NO_ARGUMENTS);

    Assert.assertNotNull(error);
    Assert.assertEquals(-1, error.code);
    Assert.assertEquals(message, error.message);
    Assert.assertNull(error.data);
  }

  @Test
  public void testUnmappedExceptionsUseSanitizedInternalError() throws Exception {
    Method method = getClass().getMethod("unmappedMethod");
    JsonError error = resolver.resolveError(
        new RuntimeException("sensitive-marker"), method, NO_ARGUMENTS);

    assertInternalError(error);
  }

  @Test
  public void testUnmappedExceptionLoggingIsBoundedByMethodAndType() throws Exception {
    Logger apiLogger = (Logger) LoggerFactory.getLogger("API");
    Level originalLevel = apiLogger.getLevel();
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    apiLogger.addAppender(appender);
    apiLogger.setLevel(Level.DEBUG);

    try {
      Method method = getClass().getMethod("unmappedMethod");
      resolver.resolveError(new RuntimeException("first-sensitive-marker"), method, NO_ARGUMENTS);
      resolver.resolveError(new RuntimeException("second-sensitive-marker"), method, NO_ARGUMENTS);
      resolver.resolveError(new IllegalStateException("third-sensitive-marker"), method,
          NO_ARGUMENTS);
      resolver.resolveError(new RuntimeException("fourth-sensitive-marker"),
          getClass().getMethod("otherUnmappedMethod"), NO_ARGUMENTS);

      Assert.assertEquals(3, countEvents(appender, Level.WARN));
      Assert.assertEquals(1, countEvents(appender, Level.DEBUG));

      ILoggingEvent firstWarning = findEvent(appender, Level.WARN, "test_unmapped");
      Assert.assertNotNull(firstWarning);
      IThrowableProxy throwable = firstWarning.getThrowableProxy();
      Assert.assertNotNull(throwable);
      Assert.assertEquals(RuntimeException.class.getName(), throwable.getClassName());
      Assert.assertEquals("first-sensitive-marker", throwable.getMessage());

      ILoggingEvent repeated = findEvent(appender, Level.DEBUG, "test_unmapped");
      Assert.assertNotNull(repeated);
      Assert.assertNull(repeated.getThrowableProxy());
      Assert.assertFalse(repeated.getFormattedMessage().contains("second-sensitive-marker"));
    } finally {
      apiLogger.setLevel(originalLevel);
      apiLogger.detachAppender(appender);
      appender.stop();
    }
  }

  @Test
  public void testNullMethodUsesSanitizedInternalError() {
    JsonError error = resolver.resolveError(
        new RuntimeException("sensitive-marker"), null, NO_ARGUMENTS);

    assertInternalError(error);
  }

  @Test
  public void testMappedMessagePriorityAndDefaults() throws Exception {
    Method method = getClass().getMethod("messageMethod");

    JsonError error = resolver.resolveError(
        new IllegalArgumentException("exception message"), method, NO_ARGUMENTS);
    Assert.assertEquals("annotation message", error.message);

    error = resolver.resolveError(
        new RuntimeException("filter not found"), method, NO_ARGUMENTS);
    Assert.assertEquals("filter not found", error.message);

    error = resolver.resolveError(new NullPointerException(), method, NO_ARGUMENTS);
    Assert.assertEquals("Invalid params", error.message);

    error = resolver.resolveError(new IllegalStateException("   "), method, NO_ARGUMENTS);
    Assert.assertEquals("Invalid Request", error.message);

    error = resolver.resolveError(new UnsupportedOperationException(), method, NO_ARGUMENTS);
    Assert.assertEquals("Method not found", error.message);

    error = resolver.resolveError(new RuntimeException(), method, NO_ARGUMENTS);
    Assert.assertEquals("Internal error", error.message);
  }

  @Test
  public void testNonFatalErrorUsesSanitizedInternalError() throws Exception {
    Method method = getClass().getMethod("unmappedMethod");
    JsonError error = resolver.resolveError(new AssertionError("sensitive-marker"),
        method, NO_ARGUMENTS);

    assertInternalError(error);
  }

  @Test
  public void testFatalErrorsPropagate() throws Exception {
    Method method = getClass().getMethod("unmappedMethod");

    assertFatalPropagates(new StackOverflowError("fatal-marker"), method);
    assertFatalPropagates(new ThreadDeath(), method);
    assertFatalPropagates(new LinkageError("fatal-marker"), method);
    assertFatalPropagates(
        new TronError("fatal-marker", TronError.ErrCode.API_SERVER_INIT), method);
  }

  @Test
  public void testWrappedFatalErrorPropagatesActualCause() throws Exception {
    Method method = getClass().getMethod("dummyMethod");
    StackOverflowError fatal = new StackOverflowError("fatal-marker");

    Error thrown = Assert.assertThrows(Error.class,
        () -> resolver.resolveError(new ExecutionException(fatal), method, NO_ARGUMENTS));

    Assert.assertSame(fatal, thrown);
  }

  @Test(timeout = 5000)
  public void testCyclicCauseChainTerminates() throws Exception {
    Method method = getClass().getMethod("unmappedMethod");
    CyclicException first = new CyclicException("first");
    CyclicException second = new CyclicException("second");
    first.setNext(second);
    second.setNext(first);

    JsonError error = resolver.resolveError(first, method, NO_ARGUMENTS);

    assertInternalError(error);
  }

  @Test
  public void testFirstOccurrenceIsTrackedPerOperationAndExceptionType() {
    Assert.assertTrue(JsonRpcErrorResolver.firstOccurrence("op-a", new IllegalStateException()));
    Assert.assertFalse(JsonRpcErrorResolver.firstOccurrence("op-a", new IllegalStateException()));
    Assert.assertTrue(JsonRpcErrorResolver.firstOccurrence("op-a", new NullPointerException()));
    Assert.assertTrue(JsonRpcErrorResolver.firstOccurrence("op-b", new IllegalStateException()));

    JsonRpcErrorResolver.clearSeenFailuresForTest();

    Assert.assertTrue(JsonRpcErrorResolver.firstOccurrence("op-a", new IllegalStateException()));
  }

  @Test(timeout = 5000)
  public void testFindFatalCauseWithNull() {
    Assert.assertNull(JsonRpcErrorResolver.findFatalCause(null));
  }

  @Test(timeout = 5000)
  public void testFindFatalCauseWithOrdinaryChain() {
    Throwable chain = new Exception("outer", new Exception("middle", new Exception("inner")));

    Assert.assertNull(JsonRpcErrorResolver.findFatalCause(chain));
  }

  @Test(timeout = 5000)
  public void testFindFatalCauseAtEndOfChain() {
    StackOverflowError fatal = new StackOverflowError("fatal-marker");
    Throwable chain = new Exception("outer", new Exception("middle", fatal));

    Assert.assertSame(fatal, JsonRpcErrorResolver.findFatalCause(chain));
  }

  @Test(timeout = 5000)
  public void testFindFatalCauseWithWrappedTronError() {
    TronError fatal = new TronError("fatal-marker", TronError.ErrCode.API_SERVER_INIT);

    Assert.assertSame(fatal, JsonRpcErrorResolver.findFatalCause(new Exception("outer", fatal)));
  }

  @Test(timeout = 5000)
  public void testFindFatalCauseWithOrdinaryCycle() {
    Exception first = new Exception("first");
    Exception second = new Exception("second");
    first.initCause(second);
    second.initCause(first);

    Assert.assertNull(JsonRpcErrorResolver.findFatalCause(first));
  }

  @Test(timeout = 5000)
  public void testFindFatalCauseInsideCycleAfterPrefix() {
    StackOverflowError fatal = new StackOverflowError("fatal-marker");
    Throwable[] chain = new Throwable[12];
    for (int i = 0; i < chain.length; i++) {
      chain[i] = i == 8 ? fatal : new Exception("cause-" + i);
    }
    for (int i = 0; i < chain.length - 1; i++) {
      chain[i].initCause(chain[i + 1]);
    }
    chain[11].initCause(chain[6]);

    Assert.assertSame(fatal, JsonRpcErrorResolver.findFatalCause(chain[0]));
  }

  @Test(timeout = 5000)
  public void testFindFatalCauseBeyondWrapperDepthLimit() {
    StackOverflowError fatal = new StackOverflowError("fatal-marker");
    Throwable chain = fatal;
    for (int i = 0; i < 64; i++) {
      chain = new Exception("cause-" + i, chain);
    }

    Assert.assertSame(fatal, JsonRpcErrorResolver.findFatalCause(chain));
  }

  private void assertFatalPropagates(Error fatal, Method method) {
    Error thrown = Assert.assertThrows(Error.class,
        () -> resolver.resolveError(fatal, method, NO_ARGUMENTS));
    Assert.assertSame(fatal, thrown);
  }

  private static void assertInternalError(JsonError error) {
    Assert.assertNotNull(error);
    Assert.assertEquals(-32603, error.code);
    Assert.assertEquals("Internal error", error.message);
    Assert.assertNull(error.data);
  }

  private static ILoggingEvent findEvent(ListAppender<ILoggingEvent> appender, Level level,
      String marker) {
    for (ILoggingEvent event : appender.list) {
      if (event.getLevel() == level && event.getFormattedMessage().contains(marker)) {
        return event;
      }
    }
    return null;
  }

  private static int countEvents(ListAppender<ILoggingEvent> appender, Level level) {
    int count = 0;
    for (ILoggingEvent event : appender.list) {
      if (event.getLevel() == level) {
        count++;
      }
    }
    return count;
  }

  private static class CyclicException extends RuntimeException {

    private Throwable next;

    CyclicException(String message) {
      super(message, null);
    }

    void setNext(Throwable next) {
      this.next = next;
    }

    @Override
    public synchronized Throwable getCause() {
      return next;
    }
  }

}
