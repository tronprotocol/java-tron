package org.tron.core.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.Empty;
import com.typesafe.config.ConfigFactory;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ClientCalls;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tron.api.DatabaseGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.core.config.args.Args;

public class RpcApiServiceLegacyWalletExtensionTest {

  private static final String LEGACY_SERVICE = "protocol.WalletExtension";
  private static final String[] LEGACY_METHODS = {
      "GetTransactionsFromThis", "GetTransactionsFromThis2",
      "GetTransactionsToThis", "GetTransactionsToThis2"};

  private Server server;
  private ManagedChannel channel;

  @Before
  public void setUp() throws IOException {
    Args.applyConfigParams(ConfigFactory.parseString("node { walletExtensionApi = true }")
        .withFallback(ConfigFactory.defaultReference()));
    Args.getInstance().setSolidityNode(true);

    NettyServerBuilder builder = NettyServerBuilder.forPort(0);
    new RpcApiService().addService(builder);
    server = builder.build().start();
    channel = ManagedChannelBuilder.forAddress("127.0.0.1", server.getPort())
        .usePlaintext()
        .build();
  }

  @After
  public void tearDown() throws InterruptedException {
    try {
      if (channel != null) {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      }
      if (server != null) {
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      }
    } finally {
      Args.clearParam();
    }
  }

  @Test
  public void testSolidityNodeDoesNotRegisterWalletExtension() {
    List<String> services = server.getServices().stream()
        .map(s -> s.getServiceDescriptor().getName())
        .collect(Collectors.toList());
    assertTrue(services.contains(WalletSolidityGrpc.SERVICE_NAME));
    assertTrue(services.contains(DatabaseGrpc.SERVICE_NAME));
    assertFalse(services.contains(LEGACY_SERVICE));
  }

  @Test
  public void testLegacyMethodsReturnUnimplemented() {
    for (String name : LEGACY_METHODS) {
      MethodDescriptor<Empty, Empty> method = MethodDescriptor.<Empty, Empty>newBuilder()
          .setType(MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(MethodDescriptor.generateFullMethodName(LEGACY_SERVICE, name))
          .setRequestMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
          .setResponseMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
          .build();
      StatusRuntimeException e = assertThrows(StatusRuntimeException.class,
          () -> ClientCalls.blockingUnaryCall(channel, method,
              CallOptions.DEFAULT.withDeadlineAfter(5, TimeUnit.SECONDS),
              Empty.getDefaultInstance()));
      assertEquals(name, Status.Code.UNIMPLEMENTED, e.getStatus().getCode());
    }
  }
}
