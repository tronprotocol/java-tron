package org.tron.core.services.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.NettyServerBuilder;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.tron.api.DatabaseGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.interfaceOnPBFT.RpcApiServiceOnPBFT;
import org.tron.core.services.interfaceOnSolidity.RpcApiServiceOnSolidity;

/**
 * Guards that each cursor gRPC service registers the shared read services <em>through</em> its
 * cursor interceptor. Nothing else catches a dropped interceptor: the services would still be
 * served and every response would still look well-formed, only resolved against HEAD instead of the
 * solidified or PBFT snapshot. This is the gRPC counterpart of CursorFilterInstallationTest.
 *
 * <p>Registering without the interceptor binds the {@code addService(BindableService)} overload
 * rather than the {@code addService(ServerServiceDefinition)} one, so the two are distinguishable
 * here. What the interceptor does once attached is pinned by CursorInterceptorScopeTest and
 * CursorInterceptorServerTest.
 */
public class CursorInterceptorWiringTest {

  private static final Set<String> SHARED_READ_SERVICES = new HashSet<>(
      Arrays.asList(DatabaseGrpc.SERVICE_NAME, WalletSolidityGrpc.SERVICE_NAME));

  @Test
  public void testSolidityServiceRegistersBothReadServicesThroughTheCursor() throws Exception {
    Assert.assertEquals(SHARED_READ_SERVICES,
        interceptedServices(RpcApiServiceOnSolidity.class, new SolidityCursorInterceptor()));
  }

  @Test
  public void testPbftServiceRegistersBothReadServicesThroughTheCursor() throws Exception {
    Assert.assertEquals(SHARED_READ_SERVICES,
        interceptedServices(RpcApiServiceOnPBFT.class, new PbftCursorInterceptor()));
  }

  /**
   * Runs the service's real addService against a mock builder and returns the names of the services
   * it registered as intercepted definitions, failing if any was registered unintercepted.
   */
  private static Set<String> interceptedServices(Class<?> serviceClass,
      CursorServerInterceptor interceptor) throws Exception {
    RpcApiService rpcApiService = mock(RpcApiService.class);
    given(rpcApiService.getDatabaseApi()).willReturn(RpcApiService.DatabaseApi.class
        .getDeclaredConstructor(RpcApiService.class).newInstance(rpcApiService));
    given(rpcApiService.getWalletSolidityApi()).willReturn(RpcApiService.WalletSolidityApi.class
        .getDeclaredConstructor(RpcApiService.class).newInstance(rpcApiService));

    Object service = mock(serviceClass, withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
    inject(serviceClass, service, rpcApiService);
    inject(serviceClass, service, interceptor);

    NettyServerBuilder builder = mock(NettyServerBuilder.class);
    Method addService = serviceClass.getDeclaredMethod("addService", NettyServerBuilder.class);
    addService.setAccessible(true);
    addService.invoke(service, builder);

    verify(builder, never()).addService(any(BindableService.class));
    ArgumentCaptor<ServerServiceDefinition> registered =
        ArgumentCaptor.forClass(ServerServiceDefinition.class);
    verify(builder, times(2)).addService(registered.capture());

    Set<String> names = new HashSet<>();
    for (ServerServiceDefinition definition : registered.getAllValues()) {
      names.add(definition.getServiceDescriptor().getName());
    }
    return names;
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
}
