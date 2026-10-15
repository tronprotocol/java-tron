package org.tron.common.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
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
import io.grpc.stub.StreamObserver;
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
import org.tron.api.ZksnarkGrpcAPI.ZksnarkRequest;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.admission.AdmissionCheckedHandler;
import org.tron.core.admission.GuardedTransactionMarshaller;
import org.tron.core.services.ratelimiter.RpcApiAccessInterceptor;
import org.tron.protos.Protocol.Transaction;

public class RpcServiceAdmissionTest {

  private static final int MAX_MESSAGE = 4 * 1024 * 1024;
  private static final int BUDGET = 1000;
  private static final long DEADLINE_SECONDS = 10;
  // raw_data { contract { parameter { type_url: C3 28 } } }
  private static final byte[] INVALID_UTF8_TYPE_URL =
      {0x0a, 0x08, 0x5a, 0x06, 0x12, 0x04, 0x0a, 0x02, (byte) 0xc3, 0x28};

  private static final AtomicInteger HANDLER_CALLS = new AtomicInteger();
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
    };
    // Real Netty transport bound to loopback only: the in-process transport skips the
    // deframer and decompression.
    server = NettyServerBuilder.forAddress(new InetSocketAddress("127.0.0.1", 0))
        .maxInboundMessageSize(MAX_MESSAGE)
        .addService(RpcService.guardTransactionMethods(wallet.bindService(), MAX_MESSAGE, BUDGET))
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
  public void onlyTransactionRequestMethodsAreWrapped() {
    ServerServiceDefinition original = new WalletGrpc.WalletImplBase() { }.bindService();
    ServerServiceDefinition guarded =
        RpcService.guardTransactionMethods(original, MAX_MESSAGE, BUDGET);

    Set<String> wrapped = new HashSet<>();
    for (ServerMethodDefinition<?, ?> method : guarded.getMethods()) {
      MethodDescriptor<?, ?> descriptor = method.getMethodDescriptor();
      ServerMethodDefinition<?, ?> before = original.getMethod(descriptor.getFullMethodName());
      assertSame(before.getMethodDescriptor().getResponseMarshaller(),
          descriptor.getResponseMarshaller());
      if (descriptor.getRequestMarshaller() instanceof GuardedTransactionMarshaller) {
        wrapped.add(descriptor.getBareMethodName());
        assertTrue(method.getServerCallHandler() instanceof AdmissionCheckedHandler);
        assertSame(before.getServerCallHandler(),
            ((AdmissionCheckedHandler<?>) method.getServerCallHandler()).getDelegate());
      } else {
        assertSame(before.getMethodDescriptor(), descriptor);
        assertSame(before.getServerCallHandler(), method.getServerCallHandler());
      }
    }
    assertEquals(new HashSet<>(Arrays.asList("BroadcastTransaction",
        "GetTransactionSignWeight", "GetTransactionApprovedList", "GetShieldTransactionHash",
        "CreateCommonTransaction")), wrapped);
    assertEquals(original.getMethods().size(), guarded.getMethods().size());
    assertSame(original.getServiceDescriptor().getSchemaDescriptor(),
        guarded.getServiceDescriptor().getSchemaDescriptor());
  }

  @Test
  public void everyTransactionRequestMethodOfTheServiceTypesIsGuarded() {
    BindableService[] services = {
        new WalletGrpc.WalletImplBase() { },
        new WalletSolidityGrpc.WalletSolidityImplBase() { },
        new DatabaseGrpc.DatabaseImplBase() { },
        new MonitorGrpc.MonitorImplBase() { }
    };
    for (BindableService service : services) {
      assertAllTransactionMethodsGuarded(
          RpcService.guardTransactionMethods(service.bindService(), MAX_MESSAGE, BUDGET),
          BUDGET, MAX_MESSAGE);
    }
  }

  /**
   * Shared with the production registration tests. A method is classified by its proto schema,
   * so a request that only nests a Transaction, or a method without a schema, fails here instead
   * of being skipped. Each guarded method must apply exactly {@code budget} and
   * {@code maxMessageSize}.
   */
  @SuppressWarnings("unchecked")
  public static int assertAllTransactionMethodsGuarded(ServerServiceDefinition definition,
      int budget, int maxMessageSize) {
    int guarded = 0;
    for (ServerMethodDefinition<?, ?> method : definition.getMethods()) {
      MethodDescriptor<?, ?> descriptor = method.getMethodDescriptor();
      String name = descriptor.getFullMethodName();
      Object schema = descriptor.getSchemaDescriptor();
      assertTrue(name + " has no proto schema", schema instanceof ProtoMethodDescriptorSupplier);
      Descriptor input = ((ProtoMethodDescriptorSupplier) schema).getMethodDescriptor()
          .getInputType();
      MethodDescriptor.Marshaller<?> request = descriptor.getRequestMarshaller();
      if (input.equals(Transaction.getDescriptor())) {
        assertTrue(name, request instanceof GuardedTransactionMarshaller);
        assertTrue(name, method.getServerCallHandler() instanceof AdmissionCheckedHandler);
        assertLimits(name, (MethodDescriptor.Marshaller<Transaction>) request, budget,
            maxMessageSize);
        guarded++;
      } else if (request instanceof GuardedTransactionMarshaller) {
        fail(name + " is guarded although its request is not a Transaction");
      } else if (reachesTransaction(input, new HashSet<>())) {
        fail(name + " nests a Transaction in its request, which the admission guard does not "
            + "cover");
      }
    }
    return guarded;
  }

  private static void assertLimits(String name, MethodDescriptor.Marshaller<Transaction> request,
      int budget, int maxMessageSize) {
    // A sentinel is an empty Transaction, a parsed request keeps its ret entries or signature.
    assertEquals(name, budget,
        request.parse(new ByteArrayInputStream(retFlood(budget))).getRetCount());
    assertEquals(name, 0,
        request.parse(new ByteArrayInputStream(retFlood(budget + 1))).getRetCount());
    assertEquals(name, 1, request.parse(signatureOfSize(maxMessageSize)).getSignatureCount());
    assertEquals(name, 0, request.parse(signatureOfSize(maxMessageSize + 1)).getSignatureCount());
  }

  /** A Transaction of exactly {@code size} bytes holding one signature. */
  private static InputStream signatureOfSize(int size) {
    int content = size - 2;
    while (1 + CodedOutputStream.computeUInt32SizeNoTag(content) + content > size) {
      content--;
    }
    byte[] bytes = Transaction.newBuilder().addSignature(ByteString.copyFrom(new byte[content]))
        .build().toByteArray();
    assertEquals(size, bytes.length);
    return new ByteArrayInputStream(bytes);
  }

  private static boolean reachesTransaction(Descriptor type, Set<String> seen) {
    if (type.equals(Transaction.getDescriptor())) {
      return true;
    }
    if (!seen.add(type.getFullName())) {
      return false;
    }
    for (FieldDescriptor field : type.getFields()) {
      if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE
          && reachesTransaction(field.getMessageType(), seen)) {
        return true;
      }
    }
    return false;
  }

  @Test
  public void requestsNestingATransactionAreDetected() {
    assertTrue(reachesTransaction(ZksnarkRequest.getDescriptor(), new HashSet<>()));
    assertFalse(reachesTransaction(GrpcAPI.BytesMessage.getDescriptor(), new HashSet<>()));
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
  public void laterMessagesOnARejectedUnaryCallAreIgnored() throws Exception {
    Transaction flood = Transaction.parseFrom(retFlood(BUDGET + 1));
    int before = HANDLER_CALLS.get();
    // A unary call that carries a rejected message followed by a valid one.
    assertEquals(Status.Code.INVALID_ARGUMENT,
        rawUnaryCall(Arrays.asList(flood, legitimate())).getCode());
    // A valid message followed by a rejected one.
    assertEquals(Status.Code.INVALID_ARGUMENT,
        rawUnaryCall(Arrays.asList(legitimate(), flood)).getCode());
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
    // Uncompressed: rejected by the deframer before the marshaller.
    assertCode(Status.Code.RESOURCE_EXHAUSTED, () -> stub().broadcastTransaction(big));
    // Compressed: the decompressed size check fires while the marshaller reads. This path is
    // unchanged grpc behaviour (UNKNOWN plus a SEVERE log), which also shows that the SEVERE
    // capture used above works.
    assertCode(Status.Code.UNKNOWN, () -> stub().withCompression("gzip").broadcastTransaction(big));
    assertEquals(before, HANDLER_CALLS.get());
    awaitTrue(() -> GRPC_SEVERE.size() > severeBefore);
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
    assertEquals(1, assertAllTransactionMethodsGuarded(
        RpcService.guardTransactionMethods(definition, MAX_MESSAGE, BUDGET), BUDGET, MAX_MESSAGE));
  }

  @Test
  public void theSchemaDecidesWhenThereIsOne() {
    MethodDescriptor<Transaction, GrpcAPI.Return> broadcast =
        WalletGrpc.getBroadcastTransactionMethod();
    MethodDescriptor<GrpcAPI.BytesMessage, Transaction> byId =
        WalletGrpc.getGetTransactionByIdMethod();
    MethodDescriptor.Marshaller<Transaction> transaction =
        ProtoUtils.marshaller(Transaction.getDefaultInstance());
    MethodDescriptor.Marshaller<GrpcAPI.BytesMessage> bytes =
        ProtoUtils.marshaller(GrpcAPI.BytesMessage.getDefaultInstance());

    assertTrue(RpcService.isTransactionRequest(broadcast));
    assertFalse(RpcService.isTransactionRequest(byId));
    // The schema wins over a conflicting request prototype.
    assertFalse(RpcService.isTransactionRequest(
        byId.toBuilder(transaction, byId.getResponseMarshaller()).build()));
    assertTrue(RpcService.isTransactionRequest(
        broadcast.toBuilder(bytes, broadcast.getResponseMarshaller()).build()));
    // Without a schema the request prototype decides.
    assertTrue(RpcService.isTransactionRequest(
        broadcast.toBuilder().setSchemaDescriptor(null).build()));
    assertFalse(RpcService.isTransactionRequest(
        byId.toBuilder().setSchemaDescriptor(null).build()));
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
    MethodDescriptor<Transaction, GrpcAPI.Return> generated =
        WalletGrpc.getBroadcastTransactionMethod();
    MethodDescriptor<byte[], GrpcAPI.Return> method =
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
