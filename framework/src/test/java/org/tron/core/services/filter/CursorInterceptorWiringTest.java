package org.tron.core.services.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.NettyServerBuilder;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.tron.api.DatabaseGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.core.ChainBaseManager;
import org.tron.core.Wallet;
import org.tron.core.db.Manager;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.interfaceOnPBFT.RpcApiServiceOnPBFT;
import org.tron.core.services.interfaceOnSolidity.RpcApiServiceOnSolidity;

/**
 * Guards that each cursor gRPC service registers the shared read services <em>through</em> its
 * cursor interceptor. Nothing else catches a dropped interceptor: the services would still be
 * served and every response would still look well-formed, only resolved against HEAD instead of the
 * solidified or PBFT snapshot.
 *
 * <p>The production {@code addService} runs against a mock builder, and one unary call is driven
 * through each definition it registers the way gRPC does: {@code startCall}, the request, then
 * {@code onHalfClose}. The handler's first read is recorded alongside the cursor calls, so the
 * port's cursor has to be selected before that read and reset after the handler returns. A
 * registration that bypasses the interceptor, whether through the {@code BindableService}
 * overload, a plain {@code bindService()} definition or a different interceptor, reaches the
 * handler with no cursor selected and fails here. What the interceptor does on a real server is
 * pinned by CursorInterceptorScopeTest and CursorInterceptorServerTest.
 *
 * <p>Nothing here is a singleton: each test builds its own service, interceptor and mocks, so no
 * {@code @After} reset is needed.
 */
public class CursorInterceptorWiringTest {

  /** The unary method driven on each shared read service; both read through a collaborator. */
  private static final Map<String, String> PROBE_METHODS = new HashMap<>();

  static {
    PROBE_METHODS.put(DatabaseGrpc.SERVICE_NAME,
        DatabaseGrpc.getGetNowBlockMethod().getFullMethodName());
    PROBE_METHODS.put(WalletSolidityGrpc.SERVICE_NAME,
        WalletSolidityGrpc.getGetNowBlockMethod().getFullMethodName());
  }

  @Test
  public void testSolidityServiceReadsBothServicesUnderTheSolidityCursor() throws Exception {
    assertEveryServiceReadsUnder(RpcApiServiceOnSolidity.class, new SolidityCursorInterceptor(),
        Chainbase.Cursor.SOLIDITY);
  }

  @Test
  public void testPbftServiceReadsBothServicesUnderThePbftCursor() throws Exception {
    assertEveryServiceReadsUnder(RpcApiServiceOnPBFT.class, new PbftCursorInterceptor(),
        Chainbase.Cursor.PBFT);
  }

  /**
   * Registers the services through the production addService, drives one call through each
   * registered definition, and asserts every handler read ran between the port's setCursor and
   * resetCursor.
   */
  private static void assertEveryServiceReadsUnder(Class<?> serviceClass,
      CursorServerInterceptor interceptor, Chainbase.Cursor cursor) throws Exception {
    List<String> events = new ArrayList<>();
    Manager manager = mock(Manager.class);
    doAnswer(inv -> events.add("setCursor " + inv.getArgument(0)))
        .when(manager).setCursor(any(Chainbase.Cursor.class));
    doAnswer(inv -> events.add("resetCursor")).when(manager).resetCursor();
    setField(CursorServerInterceptor.class, interceptor, "dbManager", manager);

    Map<String, List<String>> eventsByService = new HashMap<>();
    for (ServerServiceDefinition definition
        : register(serviceClass, interceptor, recordRead(events))) {
      String service = definition.getServiceDescriptor().getName();
      String probe = PROBE_METHODS.get(service);
      Assert.assertNotNull(serviceClass.getSimpleName() + " registered " + service, probe);
      events.clear();
      driveUnaryCall(definition.getMethod(probe));
      eventsByService.put(service, new ArrayList<>(events));
    }

    List<String> bracketed = Arrays.asList("setCursor " + cursor, "read", "resetCursor");
    Map<String, List<String>> expected = new HashMap<>();
    for (String service : PROBE_METHODS.keySet()) {
      expected.put(service, bracketed);
    }
    Assert.assertEquals(serviceClass.getSimpleName() + " must serve every shared read service"
        + " under the " + cursor + " cursor", expected, eventsByService);
  }

  /**
   * Runs the service's real addService against a mock builder and returns the definitions it
   * registered. A service added through the {@code BindableService} overload is not among them, so
   * the count check below fails for it.
   */
  private static List<ServerServiceDefinition> register(Class<?> serviceClass,
      CursorServerInterceptor interceptor, Answer<Object> collaborator) throws Exception {
    RpcApiService rpcApiService = mock(RpcApiService.class);
    setField(RpcApiService.class, rpcApiService, "wallet",
        mock(Wallet.class, withSettings().defaultAnswer(collaborator)));
    setField(RpcApiService.class, rpcApiService, "chainBaseManager",
        mock(ChainBaseManager.class, withSettings().defaultAnswer(collaborator)));
    setField(RpcApiService.class, rpcApiService, "walletApi",
        newInner(RpcApiService.WalletApi.class, rpcApiService));
    given(rpcApiService.getDatabaseApi())
        .willReturn(newInner(RpcApiService.DatabaseApi.class, rpcApiService));
    given(rpcApiService.getWalletSolidityApi())
        .willReturn(newInner(RpcApiService.WalletSolidityApi.class, rpcApiService));

    Object service = mock(serviceClass, withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
    inject(serviceClass, service, rpcApiService);
    inject(serviceClass, service, interceptor);

    NettyServerBuilder builder = mock(NettyServerBuilder.class);
    Method addService = serviceClass.getDeclaredMethod("addService", NettyServerBuilder.class);
    addService.setAccessible(true);
    addService.invoke(service, builder);

    ArgumentCaptor<ServerServiceDefinition> registered =
        ArgumentCaptor.forClass(ServerServiceDefinition.class);
    verify(builder, times(2)).addService(registered.capture());
    return registered.getAllValues();
  }

  /** Drives one unary call through the registered handler, the way gRPC delivers it. */
  @SuppressWarnings("unchecked")
  private static <Q, A> void driveUnaryCall(ServerMethodDefinition<Q, A> method) {
    ServerCall<Q, A> call = mock(ServerCall.class);
    given(call.getMethodDescriptor()).willReturn(method.getMethodDescriptor());
    ServerCall.Listener<Q> listener =
        method.getServerCallHandler().startCall(call, new Metadata());
    listener.onMessage(((MethodDescriptor.PrototypeMarshaller<Q>)
        method.getMethodDescriptor().getRequestMarshaller()).getMessagePrototype());
    try {
      listener.onHalfClose();
    } catch (StopRead expected) {
      // the recording collaborator ends the handler at its first read
    }
  }

  /** Records the handler's first read, then ends the handler there. */
  private static Answer<Object> recordRead(List<String> events) {
    return invocation -> {
      if (invocation.getMethod().getDeclaringClass() == Object.class) {
        return Answers.RETURNS_DEFAULTS.answer(invocation);
      }
      events.add("read");
      throw new StopRead();
    };
  }

  private static <T> T newInner(Class<T> innerClass, RpcApiService outer) throws Exception {
    return innerClass.getDeclaredConstructor(RpcApiService.class).newInstance(outer);
  }

  private static void setField(Class<?> owner, Object target, String name, Object value)
      throws Exception {
    Field field = owner.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  /** Sets the one declared field the value fits; the two injected types are unrelated. */
  private static void inject(Class<?> serviceClass, Object service, Object value) throws Exception {
    for (Field field : serviceClass.getDeclaredFields()) {
      if (field.getType().isInstance(value)) {
        field.setAccessible(true);
        field.set(service, value);
        return;
      }
    }
    Assert.fail(serviceClass.getSimpleName() + " has no field for " + value.getClass().getName());
  }

  /** Thrown by the recording collaborator to stop a handler after its first read. */
  private static final class StopRead extends RuntimeException {

    StopRead() {
      super("stop the handler after its first read");
    }
  }
}
