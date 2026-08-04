package org.tron.core.services.jsonrpc;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
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
import org.junit.Test;
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
  public void testUnmappedExceptionsAreLoggedAtDebugWithCause() throws Exception {
    Method method = getClass().getMethod("unmappedMethod");
    Method otherMethod = getClass().getMethod("otherUnmappedMethod");
    RuntimeException first = new RuntimeException("first-marker");
    RuntimeException second = new RuntimeException("second-marker");
    IllegalStateException third = new IllegalStateException("third-marker");
    RuntimeException fourth = new RuntimeException("fourth-marker");

    List<ILoggingEvent> events;
    try (ApiLogCapture logs = new ApiLogCapture()) {
      resolver.resolveError(first, method, NO_ARGUMENTS);
      resolver.resolveError(second, method, NO_ARGUMENTS);
      resolver.resolveError(third, method, NO_ARGUMENTS);
      resolver.resolveError(fourth, otherMethod, NO_ARGUMENTS);
      events = logs.events();
    }

    Assert.assertEquals(0, countEvents(events, Level.WARN));
    Assert.assertEquals(4, events.size());
    assertDebugWithCause(events.get(0), "test_unmapped", first);
    assertDebugWithCause(events.get(1), "test_unmapped", second);
    assertDebugWithCause(events.get(2), "test_unmapped", third);
    assertDebugWithCause(events.get(3), "test_unmapped_other", fourth);
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

    try (ApiLogCapture logs = new ApiLogCapture()) {
      Error thrown = Assert.assertThrows(Error.class,
          () -> resolver.resolveError(new ExecutionException(fatal), method, NO_ARGUMENTS));

      Assert.assertSame(fatal, thrown);
      Assert.assertTrue("a fatal cause must not be logged", logs.events().isEmpty());
    }
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
    try (ApiLogCapture logs = new ApiLogCapture()) {
      Error thrown = Assert.assertThrows(Error.class,
          () -> resolver.resolveError(fatal, method, NO_ARGUMENTS));
      Assert.assertSame(fatal, thrown);
      Assert.assertTrue("a fatal cause must not be logged", logs.events().isEmpty());
    }
  }

  private static void assertInternalError(JsonError error) {
    Assert.assertNotNull(error);
    Assert.assertEquals(-32603, error.code);
    Assert.assertEquals("Internal error", error.message);
    Assert.assertNull(error.data);
  }

  private static void assertDebugWithCause(ILoggingEvent event, String rpcMethod,
      Throwable cause) {
    Assert.assertEquals(Level.DEBUG, event.getLevel());
    Assert.assertEquals("Unhandled exception in JSON-RPC method " + rpcMethod,
        event.getFormattedMessage());
    IThrowableProxy throwable = event.getThrowableProxy();
    Assert.assertNotNull(throwable);
    Assert.assertEquals(cause.getClass().getName(), throwable.getClassName());
    Assert.assertEquals(cause.getMessage(), throwable.getMessage());
  }

  private static int countEvents(List<ILoggingEvent> events, Level level) {
    int count = 0;
    for (ILoggingEvent event : events) {
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
