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
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.invocation.InvocationOnMock;
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
 *
 * <p>Collaborators fail in two ways. An unchecked exception reaches the handlers' generic catch
 * blocks. The checked exceptions a collaborator method declares reach the typed ones, such as the
 * {@code MaintenanceUnavailableException} catch in {@code getPaginatedNowWitnessList}; a method can
 * declare several, so the checked sweep runs once for each declaration position. A handler that
 * lets the failure escape is checked as well: what it recorded before throwing may not contain a
 * second terminal event either.
 *
 * <p>Nothing here is a singleton: each sweep builds its own service and mocks, so no {@code @After}
 * reset is needed.
 */
public class RpcApiServiceErrorPathTest {

  /** Minimum handlers that must actually fail, so the sweep cannot silently cover nothing. */
  private static final int MIN_EXERCISED = 20;

  /** Minimum handlers whose typed catch must run in each checked sweep, for the same reason. */
  private static final int MIN_EXERCISED_CHECKED = 10;

  /** Collaborators the handlers call; the exceptions their methods declare drive the sweeps. */
  private static final List<Class<?>> COLLABORATORS = Arrays.asList(
      Wallet.class, TransactionUtil.class, NodeInfoService.class, MetricsApiService.class);

  @Test
  public void testWalletApiTerminatesTheCallOnce() throws Exception {
    assertSingleTerminalEvent(WalletApi.class, Failure.unchecked(), MIN_EXERCISED);
  }

  @Test
  public void testWalletSolidityApiTerminatesTheCallOnce() throws Exception {
    assertSingleTerminalEvent(WalletSolidityApi.class, Failure.unchecked(), MIN_EXERCISED);
  }

  @Test
  public void testWalletApiTerminatesTheCallOnceOnCheckedExceptions() throws Exception {
    for (int position = 0; position < declarationPositions(); position++) {
      assertSingleTerminalEvent(WalletApi.class, Failure.checked(position),
          MIN_EXERCISED_CHECKED);
    }
  }

  @Test
  public void testWalletSolidityApiTerminatesTheCallOnceOnCheckedExceptions() throws Exception {
    for (int position = 0; position < declarationPositions(); position++) {
      assertSingleTerminalEvent(WalletSolidityApi.class, Failure.checked(position),
          MIN_EXERCISED_CHECKED);
    }
  }

  /**
   * Drives every unary handler of the given service class with collaborators that fail as
   * {@code failure} says, and asserts none of them terminates the call more than once.
   */
  private static void assertSingleTerminalEvent(Class<?> apiClass, Failure failure,
      int minExercised) throws Exception {
    RpcApiService service = mock(RpcApiService.class,
        withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
    injectFailingCollaborators(service, failure);
    Object api = apiClass.getDeclaredConstructor(RpcApiService.class).newInstance(service);

    int exercised = 0;
    for (Method method : apiClass.getDeclaredMethods()) {
      if (!isUnaryHandler(method)) {
        continue;
      }
      Message request = (Message) method.getParameterTypes()[0]
          .getMethod("getDefaultInstance").invoke(null);
      TerminalRecorder recorder = new TerminalRecorder();
      failure.threwChecked = false;
      String escaped = "";
      try {
        method.invoke(api, request, recorder);
      } catch (InvocationTargetException e) {
        escaped = " before letting " + e.getCause() + " escape";
      }
      Assert.assertTrue(
          method.getName() + " terminated the call " + recorder.events.size() + " times "
              + recorder.events + escaped + " " + failure + "; onError must be followed by return",
          recorder.events.size() <= 1);
      if (!recorder.events.isEmpty() && failure.reachedTheIntendedCatch()) {
        exercised++;
      }
    }
    Assert.assertTrue(
        apiClass.getSimpleName() + " exercised only " + exercised + " handlers " + failure
            + ", expected at least " + minExercised
            + " — the sweep is no longer reaching the handler bodies",
        exercised >= minExercised);
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
   * Replaces the service's collaborators with mocks that fail on every call, so each handler takes
   * its own error path, and binds a real {@code WalletApi} for the solidity handlers to delegate
   * to.
   */
  private static void injectFailingCollaborators(RpcApiService service, Failure failure)
      throws Exception {
    set(service, "wallet", mock(Wallet.class, withSettings().defaultAnswer(failure)));
    set(service, "transactionUtil",
        mock(TransactionUtil.class, withSettings().defaultAnswer(failure)));
    set(service, "nodeInfoService",
        mock(NodeInfoService.class, withSettings().defaultAnswer(failure)));
    set(service, "metricsApiService",
        mock(MetricsApiService.class, withSettings().defaultAnswer(failure)));
    set(service, "walletApi",
        WalletApi.class.getDeclaredConstructor(RpcApiService.class).newInstance(service));
  }

  private static void set(RpcApiService service, String name, Object value) throws Exception {
    Field field = RpcApiService.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(service, value);
  }

  /** Most checked exceptions any collaborator method declares, i.e. the checked sweeps to run. */
  private static int declarationPositions() {
    int positions = 0;
    for (Class<?> collaborator : COLLABORATORS) {
      for (Method method : collaborator.getMethods()) {
        int declared = checkedExceptions(method).size();
        positions = declared > positions ? declared : positions;
      }
    }
    Assert.assertTrue("no collaborator method declares a checked exception", positions > 0);
    return positions;
  }

  private static List<Class<?>> checkedExceptions(Method method) {
    List<Class<?>> checked = new ArrayList<>();
    for (Class<?> type : method.getExceptionTypes()) {
      if (Exception.class.isAssignableFrom(type)
          && !RuntimeException.class.isAssignableFrom(type)) {
        checked.add(type);
      }
    }
    return checked;
  }

  /** Builds the exception through its message or no-arg constructor; null if it has neither. */
  private static Exception newException(Class<?> type) {
    try {
      return (Exception) type.getConstructor(String.class).newInstance("collaborator unavailable");
    } catch (ReflectiveOperationException noMessageConstructor) {
      try {
        return (Exception) type.getConstructor().newInstance();
      } catch (ReflectiveOperationException noDefaultConstructor) {
        return null;
      }
    }
  }

  /**
   * How every collaborator call fails in one sweep: with an unchecked exception, or with the
   * checked exception the called method declares at a given position (its last one when it
   * declares fewer).
   */
  private static final class Failure implements Answer<Object> {

    /** Declaration position to throw, or -1 for an unchecked exception. */
    private final int position;
    /** Whether the current handler's collaborator call threw a checked exception. */
    private boolean threwChecked;

    private Failure(int position) {
      this.position = position;
    }

    static Failure unchecked() {
      return new Failure(-1);
    }

    static Failure checked(int position) {
      return new Failure(position);
    }

    /** A checked sweep only counts handlers whose collaborator really threw a checked exception. */
    boolean reachedTheIntendedCatch() {
      return position < 0 || threwChecked;
    }

    @Override
    public Object answer(InvocationOnMock invocation) throws Throwable {
      if (position >= 0) {
        List<Class<?>> declared = checkedExceptions(invocation.getMethod());
        if (!declared.isEmpty()) {
          Exception checked = newException(
              declared.get(position < declared.size() ? position : declared.size() - 1));
          if (checked != null) {
            threwChecked = true;
            throw checked;
          }
        }
      }
      throw new RuntimeException("collaborator unavailable");
    }

    @Override
    public String toString() {
      return position < 0 ? "(unchecked failures)"
          : "(checked failures, declaration position " + position + ")";
    }
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
