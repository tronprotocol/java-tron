package org.tron.common.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.TestConstants;
import org.tron.core.ChainBaseManager;
import org.tron.core.admission.TransactionAdmissionException;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.services.filter.LiteFnQueryGrpcInterceptor;
import org.tron.core.services.filter.SolidityCursorInterceptor;
import org.tron.core.services.ratelimiter.PrometheusInterceptor;
import org.tron.core.services.ratelimiter.RateLimiterContainer;
import org.tron.core.services.ratelimiter.RateLimiterInterceptor;
import org.tron.core.services.ratelimiter.RpcApiAccessInterceptor;
import org.tron.core.services.ratelimiter.adapter.GlobalPreemptibleAdapter;
import org.tron.protos.Protocol.Account;

/**
 * Rejections through the interceptors that production registers: the rate limiter with a
 * preemptible permit, access control, the lite fullnode filter and the SOLIDITY cursor.
 */
public class RpcServiceInterceptorChainAdmissionTest {

  private static final int MAX_MESSAGE = 4 * 1024 * 1024;
  private static final int BUDGET = 100;
  private static final String GET_ACCOUNT = "protocol.WalletSolidity/GetAccount";
  private static final MethodDescriptor<byte[], byte[]> RAW_GET_ACCOUNT =
      MethodDescriptor.<byte[], byte[]>newBuilder()
          .setType(MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(GET_ACCOUNT)
          .setRequestMarshaller(new RawMarshaller())
          .setResponseMarshaller(new RawMarshaller())
          .build();

  private static final AtomicInteger SERVICE_CALLS = new AtomicInteger();

  private static Server server;
  private static ManagedChannel channel;
  private static Manager manager;
  private static GlobalPreemptibleAdapter permits;

  @Rule
  public Timeout timeout = new Timeout(60, TimeUnit.SECONDS);

  @BeforeClass
  public static void startServer() throws Exception {
    Args.setParam(new String[0], TestConstants.TEST_CONF);

    RateLimiterContainer container = new RateLimiterContainer();
    permits = new GlobalPreemptibleAdapter("permit=1");
    container.add("rpc_", GET_ACCOUNT, permits);
    RateLimiterInterceptor rateLimiter = new RateLimiterInterceptor();
    ReflectionTestUtils.setField(rateLimiter, "container", container);
    LiteFnQueryGrpcInterceptor liteFn = new LiteFnQueryGrpcInterceptor();
    ChainBaseManager chainBaseManager = mock(ChainBaseManager.class);
    when(chainBaseManager.isLiteNode()).thenReturn(false);
    ReflectionTestUtils.setField(liteFn, "chainBaseManager", chainBaseManager);
    manager = mock(Manager.class);
    SolidityCursorInterceptor cursor = new SolidityCursorInterceptor();
    ReflectionTestUtils.setField(cursor, "dbManager", manager);

    RpcService service = new RpcService() {
      @Override
      protected void addService(NettyServerBuilder serverBuilder) {
      }
    };
    ReflectionTestUtils.setField(service, "rateLimiterInterceptor", rateLimiter);
    ReflectionTestUtils.setField(service, "apiAccessInterceptor", new RpcApiAccessInterceptor());
    ReflectionTestUtils.setField(service, "liteFnQueryGrpcInterceptor", liteFn);
    ReflectionTestUtils.setField(service, "prometheusInterceptor", new PrometheusInterceptor());

    WalletSolidityGrpc.WalletSolidityImplBase walletSolidity =
        new WalletSolidityGrpc.WalletSolidityImplBase() {
          @Override
          public void getAccount(Account request, StreamObserver<Account> responseObserver) {
            SERVICE_CALLS.incrementAndGet();
            responseObserver.onNext(request);
            responseObserver.onCompleted();
          }
        };
    // As RpcApiServiceOnSolidity registers it.
    NettyServerBuilder builder = NettyServerBuilder
        .forAddress(new InetSocketAddress("127.0.0.1", 0))
        .maxInboundMessageSize(MAX_MESSAGE)
        .addService(RpcService.guardRequestMethods(
            ServerInterceptors.intercept(walletSolidity.bindService(), cursor),
            MAX_MESSAGE, BUDGET));
    service.addInterceptor(builder);
    server = builder.build().start();
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
      Args.clearParam();
    }
  }

  @Test
  public void rejectionsReleaseThePermitAndNeverSetTheCursor() throws Exception {
    byte[] overBudget = unknownFields(BUDGET + 1);
    // With one permit, a permit kept by a rejected call fails the next call.
    for (int i = 0; i < 3; i++) {
      try {
        ClientCalls.blockingUnaryCall(channel, RAW_GET_ACCOUNT,
            CallOptions.DEFAULT.withDeadlineAfter(10, TimeUnit.SECONDS), overBudget);
        fail("expected a rejection");
      } catch (StatusRuntimeException e) {
        assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
        assertEquals(TransactionAdmissionException.REQUEST_REJECTION_MESSAGE,
            e.getStatus().getDescription());
      }
    }
    assertEquals(0, SERVICE_CALLS.get());
    verify(manager, never()).setCursor(any());

    Account account = Account.newBuilder()
        .setAddress(ByteString.copyFrom(new byte[21])).build();
    byte[] reply = ClientCalls.blockingUnaryCall(channel, RAW_GET_ACCOUNT,
        CallOptions.DEFAULT.withDeadlineAfter(10, TimeUnit.SECONDS), account.toByteArray());
    assertEquals(account, Account.parseFrom(reply));
    assertEquals(1, SERVICE_CALLS.get());

    // The permit comes back in onComplete, which runs after onHalfClose has reset the cursor.
    long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
    boolean acquired = false;
    while (!acquired && System.nanoTime() < end) {
      acquired = permits.tryAcquire(null);
      if (!acquired) {
        Thread.sleep(10);
      }
    }
    assertTrue("permit leaked", acquired);
    permits.release();
    verify(manager, times(1)).setCursor(any());
    verify(manager, times(1)).resetCursor();
  }

  private static byte[] unknownFields(int count) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    CodedOutputStream out = CodedOutputStream.newInstance(bytes);
    for (int i = 0; i < count; i++) {
      out.writeUInt64(1000 + i, 1);
    }
    out.flush();
    return bytes.toByteArray();
  }

  private static final class RawMarshaller implements MethodDescriptor.Marshaller<byte[]> {

    @Override
    public InputStream stream(byte[] value) {
      return new ByteArrayInputStream(value);
    }

    @Override
    public byte[] parse(InputStream stream) {
      try {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = stream.read(buffer)) > 0) {
          out.write(buffer, 0, read);
        }
        return out.toByteArray();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
