package org.tron.core.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.NettyServerBuilder;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.reflections.Reflections;
import org.tron.common.application.RpcService;
import org.tron.common.application.RpcServiceAdmissionTest;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.admission.TransactionAdmissionGuard;
import org.tron.core.services.filter.PbftCursorInterceptor;
import org.tron.core.services.filter.SolidityCursorInterceptor;
import org.tron.core.services.interfaceOnPBFT.RpcApiServiceOnPBFT;
import org.tron.core.services.interfaceOnSolidity.RpcApiServiceOnSolidity;

/**
 * Captures what the production RpcService subclasses actually register, for every branch of
 * their addService, and checks that every method is guarded and that no service is registered
 * unwrapped.
 */
public class RpcServiceRegistrationAdmissionTest {

  private static final int MAX_MESSAGE_SIZE = 256 * 1024;
  // Methods per service in api.proto: Database 4, Wallet 147, WalletSolidity 47, Monitor 1.

  private boolean solidityNode;
  private boolean nodeMetricsEnable;
  private int maxMessageSize;

  @Before
  public void saveFlags() {
    CommonParameter parameter = CommonParameter.getInstance();
    solidityNode = parameter.isSolidityNode();
    nodeMetricsEnable = parameter.nodeMetricsEnable;
    maxMessageSize = parameter.maxMessageSize;
    parameter.maxMessageSize = MAX_MESSAGE_SIZE;
  }

  @After
  public void restoreFlags() {
    CommonParameter parameter = CommonParameter.getInstance();
    parameter.setSolidityNode(solidityNode);
    parameter.nodeMetricsEnable = nodeMetricsEnable;
    parameter.maxMessageSize = maxMessageSize;
  }

  @Test
  public void fullNodeRegistersEveryMethodGuarded() throws Exception {
    CommonParameter.getInstance().setSolidityNode(false);
    CommonParameter.getInstance().nodeMetricsEnable = true;
    List<ServerServiceDefinition> registered = register(new RpcApiService());
    assertEquals(names(registered).toString(), 3, registered.size());   // database, wallet, monitor
    assertEquals(4 + 147 + 1, guardedMethods(registered));
  }

  @Test
  public void solidityNodeBranchRegistersEverythingThroughTheGuard() throws Exception {
    CommonParameter.getInstance().setSolidityNode(true);
    CommonParameter.getInstance().nodeMetricsEnable = true;
    List<ServerServiceDefinition> registered = register(new RpcApiService());
    // database, walletSolidity, monitor
    assertEquals(names(registered).toString(), 3, registered.size());
    assertEquals(4 + 47 + 1, guardedMethods(registered));
  }

  @Test
  public void solidityAndPbftServicesRegisterEverythingThroughTheGuard() throws Exception {
    for (RpcService service : Arrays.asList(
        inject(new RpcApiServiceOnSolidity(), new SolidityCursorInterceptor()),
        inject(new RpcApiServiceOnPBFT(), new PbftCursorInterceptor()))) {
      List<ServerServiceDefinition> registered = register(service);
      assertEquals(names(registered).toString(), 2, registered.size());   // database, wallet
      assertEquals(4 + 47, guardedMethods(registered));
    }
  }

  @Test
  public void everyProductionRpcServiceIsCoveredByThisTest() {
    URL testClasses = getClass().getProtectionDomain().getCodeSource().getLocation();
    Set<String> found = new TreeSet<>();
    for (Class<? extends RpcService> type :
        new Reflections("org.tron").getSubTypesOf(RpcService.class)) {
      if (!Modifier.isAbstract(type.getModifiers())
          && !testClasses.equals(type.getProtectionDomain().getCodeSource().getLocation())) {
        found.add(type.getName());
      }
    }
    // A new subclass must get a registration test above before it is added here.
    assertEquals(new TreeSet<>(Arrays.asList(RpcApiService.class.getName(),
        RpcApiServiceOnSolidity.class.getName(), RpcApiServiceOnPBFT.class.getName())), found);
  }

  private static RpcService inject(RpcService service, Object interceptor) throws Exception {
    RpcApiService rpcApiService = new RpcApiService();
    for (Field field : service.getClass().getDeclaredFields()) {
      for (Object value : Arrays.asList(rpcApiService, interceptor)) {
        if (field.getType().isInstance(value)) {
          field.setAccessible(true);
          field.set(service, value);
        }
      }
    }
    return service;
  }

  private static List<ServerServiceDefinition> register(RpcService service) throws Exception {
    NettyServerBuilder builder = mock(NettyServerBuilder.class);
    Method addService = service.getClass().getDeclaredMethod("addService",
        NettyServerBuilder.class);
    addService.setAccessible(true);
    addService.invoke(service, builder);
    // Every registration must go through guardRequestMethods, which yields a definition.
    verify(builder, never()).addService(any(BindableService.class));
    ArgumentCaptor<ServerServiceDefinition> captor =
        ArgumentCaptor.forClass(ServerServiceDefinition.class);
    verify(builder, atLeastOnce()).addService(captor.capture());
    List<ServerServiceDefinition> registered = captor.getAllValues();
    guardedMethods(registered);
    return registered;
  }

  private static int guardedMethods(List<ServerServiceDefinition> definitions) {
    int guarded = 0;
    for (ServerServiceDefinition definition : definitions) {
      guarded += RpcServiceAdmissionTest.assertAllMethodsGuarded(definition,
          TransactionAdmissionGuard.MAX_TX_OCCURRENCES, MAX_MESSAGE_SIZE);
    }
    return guarded;
  }

  private static Set<String> names(List<ServerServiceDefinition> definitions) {
    Set<String> names = new HashSet<>();
    for (ServerServiceDefinition definition : definitions) {
      names.add(definition.getServiceDescriptor().getName());
    }
    return names;
  }
}
