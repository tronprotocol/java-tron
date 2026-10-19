package org.tron.common.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Internal;
import com.google.protobuf.Message;
import io.grpc.BindableService;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.Compressor;
import io.grpc.CompressorRegistry;
import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.ProtoMethodDescriptorSupplier;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;
import io.prometheus.client.CollectorRegistry;
import java.io.ByteArrayInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.tron.api.DatabaseGrpc;
import org.tron.api.GrpcAPI;
import org.tron.api.MonitorGrpc;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.MetricKeys;
import org.tron.core.actuator.TransactionFactory;
import org.tron.core.admission.AdmissionCheckedHandler;
import org.tron.core.admission.GuardedRequestMarshaller;
import org.tron.core.admission.TransactionAdmissionException;
import org.tron.core.admission.TransactionAdmissionException.Reason;
import org.tron.core.services.ratelimiter.RpcApiAccessInterceptor;
import org.tron.core.utils.TransactionRegister;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.ShieldContract.ShieldedTransferContract;

public class RpcServiceAdmissionTest {

  private static final int MAX_MESSAGE = 4 * 1024 * 1024;
  private static final int BUDGET = 1000;
  private static final long DEADLINE_SECONDS = 10;
  // raw_data { contract { parameter { type_url: C3 28 } } }
  private static final byte[] INVALID_UTF8_TYPE_URL =
      {0x0a, 0x08, 0x5a, 0x06, 0x12, 0x04, 0x0a, 0x02, (byte) 0xc3, 0x28};

  private static final AtomicInteger HANDLER_CALLS = new AtomicInteger();
  private static final AtomicInteger NOW_BLOCK_CALLS = new AtomicInteger();
  private static final AtomicInteger STARTED = new AtomicInteger();
  private static final AtomicInteger FINISHED = new AtomicInteger();
  private static final List<Status.Code> CLOSED_WITH = new CopyOnWriteArrayList<>();
  private static final List<LogRecord> GRPC_SEVERE = new CopyOnWriteArrayList<>();

  private static Server server;
  private static ManagedChannel channel;
  private static Handler severeCapture;
  private static List<String> originalDisabledApiList;

  @Rule
  public Timeout timeout = new Timeout(60, TimeUnit.SECONDS);

  @BeforeClass
  public static void startServer() throws Exception {
    CommonParameter parameter = CommonParameter.getInstance();
    originalDisabledApiList = parameter.disabledApiList;
    parameter.disabledApiList = Collections.emptyList();
    severeCapture = new Handler() {
      @Override
      public void publish(LogRecord record) {
        // Only the logger that reports exceptions from a call, not leftovers of other tests.
        if (record.getLevel().intValue() >= Level.SEVERE.intValue()
            && "io.grpc.internal.SerializingExecutor".equals(record.getLoggerName())) {
          GRPC_SEVERE.add(record);
        }
      }

      @Override
      public void flush() {
      }

      @Override
      public void close() {
      }
    };
    Logger.getLogger("io.grpc").addHandler(severeCapture);

    WalletGrpc.WalletImplBase wallet = new WalletGrpc.WalletImplBase() {
      @Override
      public void broadcastTransaction(Transaction request,
          StreamObserver<GrpcAPI.Return> responseObserver) {
        HANDLER_CALLS.incrementAndGet();
        responseObserver.onNext(GrpcAPI.Return.newBuilder().setResult(true).build());
        responseObserver.onCompleted();
      }

      @Override
      public void getNowBlock(GrpcAPI.EmptyMessage request,
          StreamObserver<Block> responseObserver) {
        NOW_BLOCK_CALLS.incrementAndGet();
        responseObserver.onNext(Block.getDefaultInstance());
        responseObserver.onCompleted();
      }
    };
    // Real Netty transport bound to loopback only: the in-process transport skips the
    // deframer and decompression.
    server = NettyServerBuilder.forAddress(new InetSocketAddress("127.0.0.1", 0))
        .maxInboundMessageSize(MAX_MESSAGE)
        .addService(RpcService.guardRequestMethods(wallet.bindService(), MAX_MESSAGE, BUDGET))
        .intercept(new RpcApiAccessInterceptor())
        .intercept(new PermitLikeInterceptor())
        .build().start();
    channel = NettyChannelBuilder.forAddress("127.0.0.1", server.getPort()).usePlaintext()
        .build();
  }

  @AfterClass
  public static void stopServer() throws Exception {
    try {
      if (channel != null) {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      }
      if (server != null) {
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      }
    } finally {
      if (severeCapture != null) {
        Logger.getLogger("io.grpc").removeHandler(severeCapture);
      }
      CommonParameter.getInstance().disabledApiList = originalDisabledApiList;
    }
  }

  // ---------------------------------------------------------------- wrapping

  @Test
  public void everyMethodIsWrapped() {
    ServerServiceDefinition original = new WalletGrpc.WalletImplBase() { }.bindService();
    ServerServiceDefinition guarded =
        RpcService.guardRequestMethods(original, MAX_MESSAGE, BUDGET);

    for (ServerMethodDefinition<?, ?> method : guarded.getMethods()) {
      MethodDescriptor<?, ?> descriptor = method.getMethodDescriptor();
      String name = descriptor.getFullMethodName();
      ServerMethodDefinition<?, ?> before = original.getMethod(name);
      assertSame(name, before.getMethodDescriptor().getResponseMarshaller(),
          descriptor.getResponseMarshaller());
      assertSame(name, before.getMethodDescriptor().getSchemaDescriptor(),
          descriptor.getSchemaDescriptor());
      assertTrue(name, descriptor.getRequestMarshaller() instanceof GuardedRequestMarshaller);
      assertTrue(name, method.getServerCallHandler() instanceof AdmissionCheckedHandler);
      assertSame(name, before.getServerCallHandler(),
          ((AdmissionCheckedHandler<?, ?>) method.getServerCallHandler()).getDelegate());
    }
    assertEquals(original.getMethods().size(), guarded.getMethods().size());
    // The rpc count of service Wallet in api.proto; update it with the proto.
    assertEquals(147, guarded.getMethods().size());
    assertSame(original.getServiceDescriptor().getSchemaDescriptor(),
        guarded.getServiceDescriptor().getSchemaDescriptor());
  }

  @Test
  public void everyMethodOfTheServiceTypesIsGuarded() {
    BindableService[] services = {
        new WalletGrpc.WalletImplBase() { },
        new WalletSolidityGrpc.WalletSolidityImplBase() { },
        new DatabaseGrpc.DatabaseImplBase() { },
        new MonitorGrpc.MonitorImplBase() { }
    };
    int total = 0;
    for (BindableService service : services) {
      ServerServiceDefinition definition = service.bindService();
      assertEquals(definition.getMethods().size(), assertAllMethodsGuarded(
          RpcService.guardRequestMethods(definition, 64 * 1024, BUDGET), BUDGET, 64 * 1024));
      total += definition.getMethods().size();
    }
    // Wallet 147 + WalletSolidity 47 + Database 4 + Monitor 1 in api.proto.
    assertEquals(199, total);
  }

  /**
   * Shared with the production registration tests. Every method must have a proto schema, be
   * wrapped with a {@link GuardedRequestMarshaller} whose scan root is the schema's request type
   * (so schema and request marshaller agree), be handled by the matching
   * {@link AdmissionCheckedHandler}, and apply exactly {@code budget} and
   * {@code maxMessageSize}. Returns the number of methods checked.
   */
  @SuppressWarnings("unchecked")
  public static int assertAllMethodsGuarded(ServerServiceDefinition definition, int budget,
      int maxMessageSize) {
    int guarded = 0;
    for (ServerMethodDefinition<?, ?> method : definition.getMethods()) {
      MethodDescriptor<?, ?> descriptor = method.getMethodDescriptor();
      String name = descriptor.getFullMethodName();
      Object schema = descriptor.getSchemaDescriptor();
      assertTrue(name + " has no proto schema", schema instanceof ProtoMethodDescriptorSupplier);
      Descriptor input = ((ProtoMethodDescriptorSupplier) schema).getMethodDescriptor()
          .getInputType();
      MethodDescriptor.Marshaller<?> request = descriptor.getRequestMarshaller();
      assertTrue(name + " is not guarded with a sentinel-returning marshaller: " + request,
          request instanceof GuardedRequestMarshaller);
      GuardedRequestMarshaller<Object> marshaller = (GuardedRequestMarshaller<Object>) request;
      assertEquals(name + " schema and request marshaller disagree", input,
          marshaller.getRoot());
      assertEquals(name, input,
          ((Message) marshaller.getMessagePrototype()).getDescriptorForType());
      assertTrue(name, method.getServerCallHandler() instanceof AdmissionCheckedHandler);
      assertSame(name, marshaller,
          ((AdmissionCheckedHandler<?, ?>) method.getServerCallHandler()).getMarshaller());
      assertLimits(name, marshaller, budget, maxMessageSize);
      guarded++;
    }
    return guarded;
  }

  private static void assertLimits(String name, GuardedRequestMarshaller<Object> request,
      int budget, int maxMessageSize) {
    assertNull(name, request.rejectionReason(
        request.parse(new ByteArrayInputStream(distinctUnknownFields(budget)))));
    assertEquals(name, Reason.OVER_BUDGET, request.rejectionReason(
        request.parse(new ByteArrayInputStream(distinctUnknownFields(budget + 1)))));
    assertNull(name, request.rejectionReason(request.parse(unknownFieldOfSize(maxMessageSize))));
    assertEquals(name, Reason.TOO_LARGE,
        request.rejectionReason(request.parse(unknownFieldOfSize(maxMessageSize + 1))));
  }

  /** {@code count} varint fields, each with a field number no request type uses. */
  private static byte[] distinctUnknownFields(int count) {
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    for (int i = 0; i < count; i++) {
      CodedOutputStream coded = CodedOutputStream.newInstance(out, 16);
      try {
        coded.writeUInt64(1000 + i, 0);
        coded.flush();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    return out.toByteArray();
  }

  /** Exactly {@code size} bytes: one length-delimited field 999, unknown to every request. */
  private static InputStream unknownFieldOfSize(int size) {
    int content = size - 2;
    while (CodedOutputStream.computeTagSize(999) + CodedOutputStream.computeUInt32SizeNoTag(
        content) + content > size) {
      content--;
    }
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    CodedOutputStream coded = CodedOutputStream.newInstance(out);
    try {
      coded.writeByteArray(999, new byte[content]);
      coded.flush();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    assertEquals(size, out.size());
    return new ByteArrayInputStream(out.toByteArray());
  }

  // ---------------------------------------------------------------- real Netty transport

  @Test
  public void legitimateRequestsSucceedPlainAndGzip() {
    int before = HANDLER_CALLS.get();
    assertTrue(stub().broadcastTransaction(legitimate()).getResult());
    assertTrue(stub().withCompression("gzip").broadcastTransaction(legitimate()).getResult());
    assertEquals(before + 2, HANDLER_CALLS.get());
  }

  @Test
  public void overBudgetRequestsAreRejectedWithInvalidArgumentAndNoSevereLog()
      throws Exception {
    Transaction flood = Transaction.parseFrom(retFlood(BUDGET + 1));
    int before = HANDLER_CALLS.get();
    int severeBefore = GRPC_SEVERE.size();
    CLOSED_WITH.clear();

    assertCode(Status.Code.INVALID_ARGUMENT, () -> stub().broadcastTransaction(flood));
    assertCode(Status.Code.INVALID_ARGUMENT,
        () -> stub().withCompression("gzip").broadcastTransaction(flood));

    assertEquals(before, HANDLER_CALLS.get());
    // Interceptors see the call close with the same status the client gets.
    assertEquals(Arrays.asList(Status.Code.INVALID_ARGUMENT, Status.Code.INVALID_ARGUMENT),
        CLOSED_WITH);
    // Permit-style interceptors are released for every rejected call.
    awaitTrue(() -> STARTED.get() == FINISHED.get());
    // No exception left the marshaller, so grpc logged nothing. Give its executor time to log.
    Thread.sleep(500);
    assertEquals(severeBefore, GRPC_SEVERE.size());
  }

  @Test
  public void anyMethodRejectsOverBudgetRequestsAndCountsEachRejectionOnce() throws Exception {
    CommonParameter parameter = CommonParameter.getInstance();
    boolean metrics = parameter.metricsPrometheusEnable;
    parameter.metricsPrometheusEnable = true;
    try {
      MethodDescriptor<GrpcAPI.EmptyMessage, Block> nowBlock = WalletGrpc.getGetNowBlockMethod();
      String name = nowBlock.getFullMethodName();
      double before = rejections(name, Reason.OVER_BUDGET);
      double transactionBefore = rejections(
          WalletGrpc.getBroadcastTransactionMethod().getFullMethodName(), Reason.OVER_BUDGET);
      int calls = NOW_BLOCK_CALLS.get();
      int severeBefore = GRPC_SEVERE.size();
      for (String compression : Arrays.asList(null, "gzip")) {
        Status status = rawCall(channel, nowBlock, distinctUnknownFields(BUDGET + 1),
            compression);
        assertEquals(Status.Code.INVALID_ARGUMENT, status.getCode());
        assertEquals(TransactionAdmissionException.REQUEST_REJECTION_MESSAGE,
            status.getDescription());
      }
      assertEquals(before + 2, rejections(name, Reason.OVER_BUDGET), 0);
      assertEquals(calls, NOW_BLOCK_CALLS.get());
      // The Transaction methods keep their message.
      Status transaction = rawBytesCall(channel, retFlood(BUDGET + 1), null);
      assertEquals(Status.Code.INVALID_ARGUMENT, transaction.getCode());
      assertEquals(TransactionAdmissionException.REJECTION_MESSAGE, transaction.getDescription());
      assertEquals(transactionBefore + 1, rejections(
          WalletGrpc.getBroadcastTransactionMethod().getFullMethodName(), Reason.OVER_BUDGET), 0);

      // Within the budget, and empty, the request reaches the service method.
      for (String compression : Arrays.asList(null, "gzip")) {
        assertEquals(Status.Code.OK,
            rawCall(channel, nowBlock, distinctUnknownFields(BUDGET), compression).getCode());
        assertEquals(Status.Code.OK, rawCall(channel, nowBlock, new byte[0], compression)
            .getCode());
      }
      assertEquals(calls + 4, NOW_BLOCK_CALLS.get());
      assertEquals(before + 2, rejections(name, Reason.OVER_BUDGET), 0);
      awaitTrue(() -> STARTED.get() == FINISHED.get());
      Thread.sleep(500);
      assertEquals(severeBefore, GRPC_SEVERE.size());
    } finally {
      parameter.metricsPrometheusEnable = metrics;
    }
  }

  @Test
  public void withoutAPrototypeARejectionReachesTheClientAsUnknownAndIsCountedOnce()
      throws Exception {
    CommonParameter parameter = CommonParameter.getInstance();
    boolean metrics = parameter.metricsPrometheusEnable;
    parameter.metricsPrometheusEnable = true;
    try {
      rejectionWithoutAPrototype();
    } finally {
      parameter.metricsPrometheusEnable = metrics;
    }
  }

  private void rejectionWithoutAPrototype() throws Exception {
    MethodDescriptor<GrpcAPI.EmptyMessage, Block> generated = WalletGrpc.getGetNowBlockMethod();
    AtomicInteger parsed = new AtomicInteger();
    AtomicInteger served = new AtomicInteger();
    // Not a PrototypeMarshaller, and it cannot parse empty bytes, so no instance is available
    // when wrapping and only the schema type is known.
    MethodDescriptor.Marshaller<GrpcAPI.EmptyMessage> noPrototype =
        new MethodDescriptor.Marshaller<GrpcAPI.EmptyMessage>() {
          @Override
          public InputStream stream(GrpcAPI.EmptyMessage value) {
            return generated.getRequestMarshaller().stream(value);
          }

          @Override
          public GrpcAPI.EmptyMessage parse(InputStream stream) {
            parsed.incrementAndGet();
            try {
              if (stream.available() == 0) {
                throw new IllegalStateException("no instance");
              }
            } catch (IOException e) {
              throw new IllegalStateException(e);
            }
            return generated.getRequestMarshaller().parse(stream);
          }
        };
    MethodDescriptor<GrpcAPI.EmptyMessage, Block> method =
        generated.toBuilder(noPrototype, generated.getResponseMarshaller()).build();
    ServerCallHandler<GrpcAPI.EmptyMessage, Block> handler =
        ServerCalls.asyncUnaryCall((request, observer) -> {
          served.incrementAndGet();
          observer.onNext(Block.getDefaultInstance());
          observer.onCompleted();
        });
    ServerServiceDefinition guarded = RpcService.guardRequestMethods(
        ServerServiceDefinition.builder(ServiceDescriptor.newBuilder(WalletGrpc.SERVICE_NAME)
            .addMethod(method).build()).addMethod(method, handler).build(),
        MAX_MESSAGE, BUDGET);
    String name = method.getFullMethodName();
    ServerMethodDefinition<?, ?> wrapped = guarded.getMethod(name);
    assertFalse(wrapped.getMethodDescriptor().getRequestMarshaller()
        instanceof MethodDescriptor.PrototypeMarshaller);
    assertSame(handler, wrapped.getServerCallHandler());
    Server plainServer = NettyServerBuilder.forAddress(new InetSocketAddress("127.0.0.1", 0))
        .maxInboundMessageSize(MAX_MESSAGE).addService(guarded)
        .intercept(new PermitLikeInterceptor()).build().start();
    ManagedChannel plainChannel = NettyChannelBuilder
        .forAddress("127.0.0.1", plainServer.getPort()).usePlaintext().build();
    try {
      parsed.set(0);   // the empty-bytes attempt when wrapping
      double before = rejections(name, Reason.OVER_BUDGET);
      int severeBefore = GRPC_SEVERE.size();

      Status status = rawCall(plainChannel, method, distinctUnknownFields(BUDGET + 1), null);
      assertEquals(Status.Code.UNKNOWN, status.getCode());
      assertEquals(0, parsed.get());
      assertEquals(0, served.get());
      assertEquals(before + 1, rejections(name, Reason.OVER_BUDGET), 0);
      awaitTrue(() -> STARTED.get() == FINISHED.get());
      awaitTrue(() -> GRPC_SEVERE.size() > severeBefore);

      assertEquals(Status.Code.OK,
          rawCall(plainChannel, method, distinctUnknownFields(BUDGET), null).getCode());
      assertEquals(1, parsed.get());
      assertEquals(1, served.get());
      assertEquals(before + 1, rejections(name, Reason.OVER_BUDGET), 0);
      awaitTrue(() -> STARTED.get() == FINISHED.get());
    } finally {
      plainChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      plainServer.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  public void laterMessagesOnARejectedUnaryCallAreIgnored() throws Exception {
    Transaction flood = Transaction.parseFrom(retFlood(BUDGET + 1));
    int before = HANDLER_CALLS.get();
    // A unary call that carries a rejected message followed by a valid one.
    assertEquals(Status.Code.INVALID_ARGUMENT,
        rawUnaryCall(Arrays.asList(flood, legitimate())).getCode());
    // A valid message followed by a rejected one.
    assertEquals(Status.Code.INVALID_ARGUMENT,
        rawUnaryCall(Arrays.asList(legitimate(), flood)).getCode());
    // Two rejected messages: one rejection, counted once.
    CommonParameter parameter = CommonParameter.getInstance();
    boolean metrics = parameter.metricsPrometheusEnable;
    parameter.metricsPrometheusEnable = true;
    try {
      String name = WalletGrpc.getBroadcastTransactionMethod().getFullMethodName();
      double rejected = rejections(name, Reason.OVER_BUDGET);
      assertEquals(Status.Code.INVALID_ARGUMENT,
          rawUnaryCall(Arrays.asList(flood, flood)).getCode());
      assertEquals(rejected + 1, rejections(name, Reason.OVER_BUDGET), 0);
    } finally {
      parameter.metricsPrometheusEnable = metrics;
    }
    assertEquals(before, HANDLER_CALLS.get());
    awaitTrue(() -> STARTED.get() == FINISHED.get());
  }

  @Test
  public void cancelledCallsReleaseInterceptorState() throws Exception {
    int startedBefore = STARTED.get();
    int handlerCallsBefore = HANDLER_CALLS.get();
    ClientCall<Transaction, GrpcAPI.Return> call = channel.newCall(
        WalletGrpc.getBroadcastTransactionMethod(),
        CallOptions.DEFAULT.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS));
    CompletableFuture<Status> closed = new CompletableFuture<>();
    call.start(new ClientCall.Listener<GrpcAPI.Return>() {
      @Override
      public void onClose(Status status, Metadata trailers) {
        closed.complete(status);
      }
    }, new Metadata());
    call.request(1);
    // Unary headers are only flushed with the message. Without a half-close the server keeps
    // waiting, so the cancel is what ends the call.
    call.sendMessage(legitimate());
    // Wait until the server has started the call, so its release is actually observed and
    // nothing of this call is still in flight when the next test starts.
    awaitTrue(() -> STARTED.get() > startedBefore);
    call.cancel("client gives up", null);
    closed.get(DEADLINE_SECONDS, TimeUnit.SECONDS);
    awaitTrue(() -> STARTED.get() == FINISHED.get());
    assertEquals(handlerCallsBefore, HANDLER_CALLS.get());
  }

  @Test
  public void accessInterceptorStillAppliesToGuardedMethods() {
    CommonParameter parameter = CommonParameter.getInstance();
    List<String> previous = parameter.disabledApiList;
    parameter.disabledApiList = Collections.singletonList("broadcasttransaction");
    try {
      int before = HANDLER_CALLS.get();
      assertCode(Status.Code.UNAVAILABLE, () -> stub().broadcastTransaction(legitimate()));
      assertEquals(before, HANDLER_CALLS.get());
    } finally {
      parameter.disabledApiList = previous;
    }
  }

  @Test
  public void messageSizeLimitStillApplies() throws Exception {
    Transaction big = Transaction.newBuilder()
        .addSignature(ByteString.copyFrom(new byte[MAX_MESSAGE + 1])).build();
    int before = HANDLER_CALLS.get();
    int severeBefore = GRPC_SEVERE.size();
    CommonParameter parameter = CommonParameter.getInstance();
    boolean metrics = parameter.metricsPrometheusEnable;
    parameter.metricsPrometheusEnable = true;
    try {
      String name = WalletGrpc.getBroadcastTransactionMethod().getFullMethodName();
      double tooLarge = rejections(name, Reason.TOO_LARGE);
      // Uncompressed: rejected by the deframer before the marshaller, not counted here.
      assertCode(Status.Code.RESOURCE_EXHAUSTED, () -> stub().broadcastTransaction(big));
      assertEquals(tooLarge, rejections(name, Reason.TOO_LARGE), 0);
      // Compressed: the decompressed size check of grpc fires while the marshaller reads; it is
      // turned into the same status without an error log.
      assertCode(Status.Code.RESOURCE_EXHAUSTED,
          () -> stub().withCompression("gzip").broadcastTransaction(big));
      assertEquals(tooLarge + 1, rejections(name, Reason.TOO_LARGE), 0);
    } finally {
      parameter.metricsPrometheusEnable = metrics;
    }
    assertEquals(before, HANDLER_CALLS.get());
    awaitTrue(() -> STARTED.get() == FINISHED.get());
    Thread.sleep(500);
    assertEquals(severeBefore, GRPC_SEVERE.size());
  }

  @Test
  public void malformedRequestsAreRejectedWithInvalidArgumentAndNoSevereLog() throws Exception {
    CompressorRegistry registry = CompressorRegistry.newEmptyInstance();
    registry.register(new CorruptGzip());
    ManagedChannel corruptChannel = NettyChannelBuilder.forAddress("127.0.0.1", server.getPort())
        .usePlaintext().compressorRegistry(registry).build();
    try {
      int before = HANDLER_CALLS.get();
      int severeBefore = GRPC_SEVERE.size();
      CLOSED_WITH.clear();
      // Passes the guard, then fails protobuf decoding of the original marshaller.
      assertEquals(Status.Code.INVALID_ARGUMENT,
          rawBytesCall(channel, INVALID_UTF8_TYPE_URL, null).getCode());
      assertEquals(Status.Code.INVALID_ARGUMENT,
          rawBytesCall(channel, INVALID_UTF8_TYPE_URL, "gzip").getCode());
      // Valid gzip header, invalid deflate data.
      assertEquals(Status.Code.INVALID_ARGUMENT,
          rawBytesCall(corruptChannel, legitimate().toByteArray(), "gzip").getCode());

      assertEquals(before, HANDLER_CALLS.get());
      assertEquals(Arrays.asList(Status.Code.INVALID_ARGUMENT, Status.Code.INVALID_ARGUMENT,
          Status.Code.INVALID_ARGUMENT), CLOSED_WITH);
      awaitTrue(() -> STARTED.get() == FINISHED.get());
      Thread.sleep(500);
      assertEquals(severeBefore, GRPC_SEVERE.size());
      assertTrue(stub().broadcastTransaction(legitimate()).getResult());
    } finally {
      corruptChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  public void transactionMethodWithANonPrototypeMarshallerIsStillGuarded() {
    MethodDescriptor<Transaction, GrpcAPI.Return> generated =
        WalletGrpc.getBroadcastTransactionMethod();
    MethodDescriptor.Marshaller<Transaction> plain =
        new MethodDescriptor.Marshaller<Transaction>() {
      @Override
      public InputStream stream(Transaction value) {
        return generated.getRequestMarshaller().stream(value);
      }

      @Override
      public Transaction parse(InputStream stream) {
        return generated.getRequestMarshaller().parse(stream);
      }
    };
    MethodDescriptor<Transaction, GrpcAPI.Return> method =
        generated.toBuilder(plain, generated.getResponseMarshaller()).build();
    ServerServiceDefinition definition = ServerServiceDefinition.builder(
        ServiceDescriptor.newBuilder(WalletGrpc.SERVICE_NAME).addMethod(method).build())
        .addMethod(method, (call, headers) -> new ServerCall.Listener<Transaction>() { })
        .build();
    assertEquals(1, assertAllMethodsGuarded(
        RpcService.guardRequestMethods(definition, MAX_MESSAGE, BUDGET), BUDGET, MAX_MESSAGE));
  }

  /**
   * The guard counts a packed repeated scalar field once and only unpacks the Any of
   * Transaction.Contract, so no request type, including contract payloads, may have a packable
   * repeated field or another Any.
   */
  @Test
  public void requestTypesHaveNoOtherPackedFieldOrAny() {
    TransactionRegister.registerActuator();
    Set<String> packable = new TreeSet<>();
    Set<String> any = new TreeSet<>();
    Set<String> seen = new HashSet<>();
    for (BindableService service : new BindableService[] {
        new WalletGrpc.WalletImplBase() { }, new WalletSolidityGrpc.WalletSolidityImplBase() { },
        new DatabaseGrpc.DatabaseImplBase() { }, new MonitorGrpc.MonitorImplBase() { }}) {
      for (ServerMethodDefinition<?, ?> method : service.bindService().getMethods()) {
        walk(((ProtoMethodDescriptorSupplier) method.getMethodDescriptor().getSchemaDescriptor())
            .getMethodDescriptor().getInputType(), seen, packable, any);
      }
    }
    for (Transaction.Contract.ContractType type : Transaction.Contract.ContractType.values()) {
      Class<? extends GeneratedMessageV3> payload =
          type == Transaction.Contract.ContractType.ShieldedTransferContract
              ? ShieldedTransferContract.class : TransactionFactory.getContract(type);
      if (payload != null) {
        walk(Internal.getDefaultInstance(payload).getDescriptorForType(), seen, packable, any);
      }
    }
    assertEquals(Collections.emptySet(), packable);
    assertEquals(Collections.singleton("protocol.Transaction.Contract.parameter"), any);
  }

  private static void walk(Descriptor type, Set<String> seen, Set<String> packable,
      Set<String> any) {
    if (!seen.add(type.getFullName())) {
      return;
    }
    for (FieldDescriptor field : type.getFields()) {
      if (field.isRepeated() && field.isPackable()) {
        packable.add(field.getFullName());
      }
      if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
        if (field.getMessageType().equals(com.google.protobuf.Any.getDescriptor())) {
          any.add(field.getFullName());
        } else {
          walk(field.getMessageType(), seen, packable, any);
        }
      }
    }
  }

  @Test
  public void theSchemaDecidesWhetherToGuardWhenThereIsOne() {
    MethodDescriptor<Transaction, GrpcAPI.Return> broadcast =
        WalletGrpc.getBroadcastTransactionMethod();
    MethodDescriptor<GrpcAPI.BytesMessage, Transaction> byId =
        WalletGrpc.getGetTransactionByIdMethod();
    MethodDescriptor.Marshaller<Transaction> transaction =
        ProtoUtils.marshaller(Transaction.getDefaultInstance());
    MethodDescriptor.Marshaller<GrpcAPI.BytesMessage> bytes =
        ProtoUtils.marshaller(GrpcAPI.BytesMessage.getDefaultInstance());

    assertEquals(Transaction.getDescriptor(), RpcService.requestType(broadcast));
    assertEquals(GrpcAPI.BytesMessage.getDescriptor(), RpcService.requestType(byId));
    // The schema wins over a conflicting request prototype for this decision.
    assertEquals(GrpcAPI.BytesMessage.getDescriptor(), RpcService.requestType(
        byId.toBuilder(transaction, byId.getResponseMarshaller()).build()));
    assertEquals(Transaction.getDescriptor(), RpcService.requestType(
        broadcast.toBuilder(bytes, broadcast.getResponseMarshaller()).build()));
    // Without a schema the request prototype decides.
    assertEquals(Transaction.getDescriptor(), RpcService.requestType(
        broadcast.toBuilder().setSchemaDescriptor(null).build()));
    assertEquals(GrpcAPI.BytesMessage.getDescriptor(), RpcService.requestType(
        byId.toBuilder().setSchemaDescriptor(null).build()));
  }

  @Test
  public void aMethodOfUnknownRequestTypeIsKeptAndFailsTheRegistrationCheck() {
    MethodDescriptor<byte[], GrpcAPI.Return> unknown = MethodDescriptor.<byte[], GrpcAPI.Return>
        newBuilder().setType(MethodDescriptor.MethodType.UNARY)
        .setFullMethodName(MethodDescriptor.generateFullMethodName("test.Unknown", "Call"))
        .setRequestMarshaller(RAW)
        .setResponseMarshaller(ProtoUtils.marshaller(GrpcAPI.Return.getDefaultInstance()))
        .build();
    ServerCallHandler<byte[], GrpcAPI.Return> handler =
        (call, headers) -> new ServerCall.Listener<byte[]>() { };
    ServerServiceDefinition definition = ServerServiceDefinition.builder(
        ServiceDescriptor.newBuilder("test.Unknown").addMethod(unknown).build())
        .addMethod(unknown, handler).build();
    assertNull(RpcService.requestType(unknown));
    ServerServiceDefinition guarded = RpcService.guardRequestMethods(definition, MAX_MESSAGE,
        BUDGET);
    ServerMethodDefinition<?, ?> kept = guarded.getMethod(unknown.getFullMethodName());
    assertSame(unknown, kept.getMethodDescriptor());
    assertSame(handler, kept.getServerCallHandler());
    try {
      assertAllMethodsGuarded(guarded, BUDGET, MAX_MESSAGE);
      fail("an unguarded method must fail the registration check");
    } catch (AssertionError expected) {
      assertTrue(expected.getMessage(), expected.getMessage().contains("test.Unknown/Call"));
    }
  }

  // ---------------------------------------------------------------- helpers

  private static WalletGrpc.WalletBlockingStub stub() {
    return WalletGrpc.newBlockingStub(channel)
        .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS);
  }

  private static Transaction legitimate() {
    return Transaction.newBuilder().addSignature(ByteString.copyFrom(new byte[65])).build();
  }

  private static Status rawUnaryCall(List<Transaction> messages) throws Exception {
    ClientCall<Transaction, GrpcAPI.Return> call = channel.newCall(
        WalletGrpc.getBroadcastTransactionMethod(),
        CallOptions.DEFAULT.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS));
    CompletableFuture<Status> closed = new CompletableFuture<>();
    call.start(new ClientCall.Listener<GrpcAPI.Return>() {
      @Override
      public void onClose(Status status, Metadata trailers) {
        closed.complete(status);
      }
    }, new Metadata());
    call.request(1);
    for (Transaction message : messages) {
      call.sendMessage(message);
    }
    call.halfClose();
    return closed.get(DEADLINE_SECONDS, TimeUnit.SECONDS);
  }

  private static final MethodDescriptor.Marshaller<byte[]> RAW =
      new MethodDescriptor.Marshaller<byte[]>() {
        @Override
        public InputStream stream(byte[] value) {
          return new ByteArrayInputStream(value);
        }

        @Override
        public byte[] parse(InputStream stream) {
          throw new UnsupportedOperationException();
        }
      };

  private static Status rawBytesCall(ManagedChannel target, byte[] request, String compression) {
    return rawCall(target, WalletGrpc.getBroadcastTransactionMethod(), request, compression);
  }

  private static <RespT> Status rawCall(ManagedChannel target,
      MethodDescriptor<?, RespT> generated, byte[] request, String compression) {
    MethodDescriptor<byte[], RespT> method =
        generated.toBuilder(RAW, generated.getResponseMarshaller()).build();
    CallOptions options = CallOptions.DEFAULT
        .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS).withCompression(compression);
    try {
      ClientCalls.blockingUnaryCall(target, method, options, request);
      return Status.OK;
    } catch (StatusRuntimeException e) {
      return e.getStatus();
    }
  }

  /** Writes a valid gzip header followed by deflate data with an invalid block type. */
  private static final class CorruptGzip implements Compressor {

    @Override
    public String getMessageEncoding() {
      return "gzip";
    }

    @Override
    public OutputStream compress(OutputStream os) {
      return new FilterOutputStream(os) {
        @Override
        public void write(int b) {
        }

        @Override
        public void write(byte[] b, int off, int len) {
        }

        @Override
        public void close() throws IOException {
          out.write(new byte[] {0x1f, (byte) 0x8b, 8, 0, 0, 0, 0, 0, 0, (byte) 0xff,
              (byte) 0xff, (byte) 0xff, 0, 0});
          super.close();
        }
      };
    }
  }

  private static void assertCode(Status.Code expected, Runnable call) {
    try {
      call.run();
      fail("expected " + expected);
    } catch (StatusRuntimeException e) {
      assertEquals(expected, e.getStatus().getCode());
    }
  }

  private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(DEADLINE_SECONDS);
    while (!condition.getAsBoolean()) {
      if (System.nanoTime() > deadline) {
        fail("condition not reached in time");
      }
      Thread.sleep(10);
    }
  }

  private static double rejections(String method, Reason reason) {
    Double value = CollectorRegistry.defaultRegistry.getSampleValue(
        MetricKeys.Counter.GRPC_ADMISSION_REJECT + "_total", new String[] {"method", "reason"},
        new String[] {method, reason.name()});
    return value == null ? 0 : value;
  }

  private static byte[] retFlood(int entries) {
    byte[] out = new byte[entries * 2];
    for (int i = 0; i < out.length; i += 2) {
      out[i] = 0x2a;
    }
    return out;
  }

  /**
   * Mirrors how the rate limiter holds a permit from interceptCall until the call completes or
   * is cancelled, and how the Prometheus interceptor observes the close status.
   */
  private static final class PermitLikeInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
        Metadata headers, ServerCallHandler<ReqT, RespT> next) {
      STARTED.incrementAndGet();
      ServerCall<ReqT, RespT> recording = new SimpleForwardingServerCall<ReqT, RespT>(call) {
        @Override
        public void close(Status status, Metadata trailers) {
          CLOSED_WITH.add(status.getCode());
          super.close(status, trailers);
        }
      };
      return new SimpleForwardingServerCallListener<ReqT>(next.startCall(recording, headers)) {
        @Override
        public void onComplete() {
          FINISHED.incrementAndGet();
          super.onComplete();
        }

        @Override
        public void onCancel() {
          FINISHED.incrementAndGet();
          super.onCancel();
        }
      };
    }
  }
}
