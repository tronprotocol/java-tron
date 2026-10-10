package org.tron.core.services;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.stubbing.Answer;
import org.tron.core.Wallet;
import org.tron.core.metrics.MetricsApiService;
import org.tron.core.services.RpcApiService.WalletApi;
import org.tron.core.services.RpcApiService.WalletSolidityApi;
import org.tron.core.utils.TransactionUtil;

/**
 * Pins the one-terminal-event rule on the gRPC error path: a handler that reports a failure through
 * {@code onError} must not fall through to {@code onCompleted}. gRPC rejects the second close with
 * {@code IllegalStateException("call already closed")}, so a handler doing both costs a server-side
 * exception on every failed call while the client sees nothing extra.
 *
 * <p>The rule is checked for every handler rather than for the ones that were fixed, because the
 * shape is trivially reintroduced by copying a neighbouring handler.
 */
public class RpcApiServiceErrorPathTest {

  /** Minimum handlers that must actually fail, so the sweep cannot silently cover nothing. */
  private static final int MIN_EXERCISED = 20;

  @Test
  public void testWalletApiTerminatesTheCallOnce() throws Exception {
    assertSingleTerminalEvent(WalletApi.class);
  }

  @Test
  public void testWalletSolidityApiTerminatesTheCallOnce() throws Exception {
    assertSingleTerminalEvent(WalletSolidityApi.class);
  }

  /**
   * Drives every unary handler of the given service class with collaborators that throw, and
   * asserts none of them terminates the call more than once.
   */
  private static void assertSingleTerminalEvent(Class<?> apiClass) throws Exception {
    RpcApiService service = mock(RpcApiService.class,
        withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
    injectThrowingCollaborators(service);
    Object api = apiClass.getDeclaredConstructor(RpcApiService.class).newInstance(service);

    int exercised = 0;
    for (Method method : apiClass.getDeclaredMethods()) {
      if (!isUnaryHandler(method)) {
        continue;
      }
      Message request = (Message) method.getParameterTypes()[0]
          .getMethod("getDefaultInstance").invoke(null);
      TerminalRecorder recorder = new TerminalRecorder();
      try {
        method.invoke(api, request, recorder);
      } catch (InvocationTargetException e) {
        // a handler that lets the failure escape cannot have closed the call twice
        continue;
      }
      Assert.assertTrue(
          method.getName() + " terminated the call " + recorder.events.size() + " times "
              + recorder.events + "; onError must be followed by return",
          recorder.events.size() <= 1);
      if (!recorder.events.isEmpty()) {
        exercised++;
      }
    }
    Assert.assertTrue(
        apiClass.getSimpleName() + " exercised only " + exercised + " handlers, expected at least "
            + MIN_EXERCISED + " — the sweep is no longer reaching the handler bodies",
        exercised >= MIN_EXERCISED);
  }

  private static boolean isUnaryHandler(Method method) {
    Class<?>[] params = method.getParameterTypes();
    return Modifier.isPublic(method.getModifiers())
        && method.getReturnType() == void.class
        && params.length == 2
        && Message.class.isAssignableFrom(params[0])
        && params[1] == StreamObserver.class;
  }

  /**
   * Replaces the service's collaborators with mocks that throw on every call, so each handler takes
   * its own error path, and binds a real {@code WalletApi} for the solidity handlers to delegate
   * to.
   */
  private static void injectThrowingCollaborators(RpcApiService service) throws Exception {
    Answer<Object> throwing = invocation -> {
      throw new RuntimeException("collaborator unavailable");
    };
    set(service, "wallet", mock(Wallet.class, withSettings().defaultAnswer(throwing)));
    set(service, "transactionUtil",
        mock(TransactionUtil.class, withSettings().defaultAnswer(throwing)));
    set(service, "nodeInfoService",
        mock(NodeInfoService.class, withSettings().defaultAnswer(throwing)));
    set(service, "metricsApiService",
        mock(MetricsApiService.class, withSettings().defaultAnswer(throwing)));
    set(service, "walletApi",
        WalletApi.class.getDeclaredConstructor(RpcApiService.class).newInstance(service));
  }

  private static void set(RpcApiService service, String name, Object value) throws Exception {
    Field field = RpcApiService.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(service, value);
  }

  /** Counts terminal events instead of closing a real call. */
  private static final class TerminalRecorder implements StreamObserver<Object> {

    private final List<String> events = new ArrayList<>();

    @Override
    public void onNext(Object value) {
    }

    @Override
    public void onError(Throwable t) {
      events.add("onError");
    }

    @Override
    public void onCompleted() {
      events.add("onCompleted");
    }
  }
}
