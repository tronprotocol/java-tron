package org.tron.core.admission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerMethodDefinition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.ProtoUtils;
import io.prometheus.client.CollectorRegistry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.api.ZksnarkGrpcAPI.ZksnarkRequest;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.MetricKeys;
import org.tron.core.admission.TransactionAdmissionException.Reason;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;

public class GuardedRequestMarshallerTest {

  private static final int MAX = 4096;
  private static final int BUDGET = 10;
  private static final ServerCallHandler<Object, Object> HANDLER =
      (call, headers) -> new ServerCall.Listener<Object>() { };

  private boolean metricsEnabled;

  @Before
  public void enableMetrics() {
    metricsEnabled = CommonParameter.getInstance().metricsPrometheusEnable;
    CommonParameter.getInstance().metricsPrometheusEnable = true;
  }

  @After
  public void restoreMetrics() {
    CommonParameter.getInstance().metricsPrometheusEnable = metricsEnabled;
  }

  @Test
  public void rootAndSentinelsFollowTheRequestType() {
    for (Message prototype : new Message[] {GrpcAPI.EmptyMessage.getDefaultInstance(),
        GrpcAPI.BytesMessage.getDefaultInstance(), Transaction.getDefaultInstance()}) {
      GuardedRequestMarshaller<Message> marshaller = guarded(proto(prototype), prototype);
      assertEquals(prototype.getDescriptorForType(), marshaller.getRoot());
      assertSame(prototype, marshaller.getMessagePrototype());
      assertSame(prototype.getClass(), marshaller.getMessageClass());
      Map<Object, Reason> seen = new IdentityHashMap<>();
      for (Reason reason : Reason.values()) {
        Message sentinel = marshaller.rejected(reason);
        assertNotSame(prototype, sentinel);
        assertEquals(prototype.getClass(), sentinel.getClass());
        assertEquals(reason, marshaller.rejectionReason(sentinel));
        assertNull(seen.put(sentinel, reason));
      }
      // An empty request parses to the shared default instance, which is never a sentinel.
      Message empty = marshaller.parse(new ByteArrayInputStream(new byte[0]));
      assertNull(marshaller.rejectionReason(empty));
      assertNull(marshaller.rejectionReason(prototype));
    }
  }

  @Test
  public void unknownFieldsOfAnyRequestTypeCount() {
    GuardedRequestMarshaller<Message> marshaller =
        guarded(proto(GrpcAPI.EmptyMessage.getDefaultInstance()),
            GrpcAPI.EmptyMessage.getDefaultInstance());
    assertNull(marshaller.rejectionReason(
        marshaller.parse(new ByteArrayInputStream(distinctUnknownFields(BUDGET)))));
    assertEquals(Reason.OVER_BUDGET, marshaller.rejectionReason(
        marshaller.parse(new ByteArrayInputStream(distinctUnknownFields(BUDGET + 1)))));
    assertEquals(TransactionAdmissionException.REQUEST_REJECTION_MESSAGE,
        marshaller.rejectionMessage());
    assertEquals(TransactionAdmissionException.REJECTION_MESSAGE,
        guarded(proto(Transaction.getDefaultInstance()), Transaction.getDefaultInstance())
            .rejectionMessage());
  }

  @Test
  public void aNonPrototypeDelegateGivesItsTypeOnceWhenWrapped() {
    Counting<GrpcAPI.EmptyMessage> delegate =
        new Counting<>(plain(GrpcAPI.EmptyMessage.getDefaultInstance()));
    ServerMethodDefinition<GrpcAPI.EmptyMessage, Object> method = method(delegate,
        GrpcAPI.EmptyMessage.getDefaultInstance());
    ServerMethodDefinition<GrpcAPI.EmptyMessage, Object> wrapped = GuardedRequestMarshaller.guard(
        method, GrpcAPI.EmptyMessage.getDescriptor(), MAX, BUDGET);
    assertEquals(1, delegate.parsed.get());      // the empty-bytes parse when wrapping
    MethodDescriptor.Marshaller<GrpcAPI.EmptyMessage> request =
        wrapped.getMethodDescriptor().getRequestMarshaller();
    assertTrue(request instanceof GuardedRequestMarshaller);
    assertTrue(wrapped.getServerCallHandler() instanceof AdmissionCheckedHandler);
    assertSame(request,
        ((AdmissionCheckedHandler<?, ?>) wrapped.getServerCallHandler()).getMarshaller());
    GuardedRequestMarshaller<GrpcAPI.EmptyMessage> guarded =
        (GuardedRequestMarshaller<GrpcAPI.EmptyMessage>) request;
    assertEquals(GrpcAPI.EmptyMessage.getDescriptor(), guarded.getRoot());
    assertEquals(Reason.OVER_BUDGET, guarded.rejectionReason(
        guarded.parse(new ByteArrayInputStream(distinctUnknownFields(BUDGET + 1)))));
    assertEquals(1, delegate.parsed.get());
    assertNull(guarded.rejectionReason(guarded.parse(new ByteArrayInputStream(new byte[0]))));
    assertEquals(2, delegate.parsed.get());
  }

  @Test
  public void theScanRootIsTheTypeTheDelegateParsesInto() {
    // raw_data { auths x (BUDGET + 1) }: one field 1 of a BytesMessage, BUDGET + 2 occurrences
    // of a Transaction.
    Transaction.raw.Builder raw = Transaction.raw.newBuilder();
    for (int i = 0; i <= BUDGET; i++) {
      raw.addAuths(org.tron.protos.Protocol.authority.getDefaultInstance());
    }
    byte[] data = Transaction.newBuilder().setRawData(raw).build().toByteArray();

    // Schema BytesMessage, delegate Transaction: scanned as the Transaction it becomes.
    ServerMethodDefinition<Transaction, Object> declaredBytes = GuardedRequestMarshaller.guard(
        method(proto(Transaction.getDefaultInstance()), Transaction.getDefaultInstance()),
        GrpcAPI.BytesMessage.getDescriptor(), MAX, BUDGET);
    GuardedRequestMarshaller<Transaction> asTransaction =
        (GuardedRequestMarshaller<Transaction>)
            declaredBytes.getMethodDescriptor().getRequestMarshaller();
    assertEquals(Transaction.getDescriptor(), asTransaction.getRoot());
    assertEquals(Reason.OVER_BUDGET,
        asTransaction.rejectionReason(asTransaction.parse(new ByteArrayInputStream(data))));

    // Schema Transaction, delegate BytesMessage: one bytes field, nothing is materialized.
    ServerMethodDefinition<GrpcAPI.BytesMessage, Object> declaredTransaction =
        GuardedRequestMarshaller.guard(method(proto(GrpcAPI.BytesMessage.getDefaultInstance()),
            GrpcAPI.BytesMessage.getDefaultInstance()), Transaction.getDescriptor(), MAX, BUDGET);
    GuardedRequestMarshaller<GrpcAPI.BytesMessage> asBytes =
        (GuardedRequestMarshaller<GrpcAPI.BytesMessage>)
            declaredTransaction.getMethodDescriptor().getRequestMarshaller();
    assertEquals(GrpcAPI.BytesMessage.getDescriptor(), asBytes.getRoot());
    GrpcAPI.BytesMessage parsed = asBytes.parse(new ByteArrayInputStream(data));
    assertNull(asBytes.rejectionReason(parsed));
    assertEquals(data.length - 2, parsed.getValue().size());
  }

  @Test
  public void withoutAnInstanceTheSchemaTypeIsCheckedAndARejectionIsThrownAndCounted() {
    Counting<GrpcAPI.EmptyMessage> delegate = new Counting<>(
        new MethodDescriptor.Marshaller<GrpcAPI.EmptyMessage>() {
          @Override
          public InputStream stream(GrpcAPI.EmptyMessage value) {
            return new ByteArrayInputStream(value.toByteArray());
          }

          @Override
          public GrpcAPI.EmptyMessage parse(InputStream stream) {
            try {
              if (stream.available() == 0) {
                throw new IllegalStateException("no empty requests");
              }
              return GrpcAPI.EmptyMessage.parseFrom(stream);
            } catch (IOException e) {
              throw new IllegalStateException(e);
            }
          }
        });
    ServerMethodDefinition<GrpcAPI.EmptyMessage, Object> method = method(delegate,
        GrpcAPI.EmptyMessage.getDefaultInstance());
    String name = method.getMethodDescriptor().getFullMethodName();
    double before = rejections(name, Reason.OVER_BUDGET);
    ServerMethodDefinition<GrpcAPI.EmptyMessage, Object> wrapped = GuardedRequestMarshaller.guard(
        method, GrpcAPI.EmptyMessage.getDescriptor(), MAX, BUDGET);
    // Failing to get an instance while wrapping is not a rejection.
    assertEquals(before, rejections(name, Reason.OVER_BUDGET), 0);
    MethodDescriptor.Marshaller<GrpcAPI.EmptyMessage> request =
        wrapped.getMethodDescriptor().getRequestMarshaller();
    assertTrue(request instanceof GuardedRequestMarshaller.Plain);
    assertFalse(request instanceof MethodDescriptor.PrototypeMarshaller);
    assertSame(method.getServerCallHandler(), wrapped.getServerCallHandler());
    assertEquals(GrpcAPI.EmptyMessage.getDescriptor(),
        ((GuardedRequestMarshaller.Plain<?>) request).getRoot());
    delegate.parsed.set(0);
    try {
      request.parse(new ByteArrayInputStream(distinctUnknownFields(BUDGET + 1)));
      fail("expected a rejection");
    } catch (StatusRuntimeException e) {
      assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
      assertEquals(TransactionAdmissionException.REQUEST_REJECTION_MESSAGE,
          e.getStatus().getDescription());
    }
    assertEquals(0, delegate.parsed.get());
    assertEquals(before + 1, rejections(name, Reason.OVER_BUDGET), 0);
    assertNotNull(request.parse(new ByteArrayInputStream(distinctUnknownFields(BUDGET))));
    assertEquals(1, delegate.parsed.get());
    assertEquals(before + 1, rejections(name, Reason.OVER_BUDGET), 0);
  }

  @Test
  public void aTransactionNestedInAnotherRequestIsCountedWithItsContract() {
    // The contract payload is reached through Any.value, which the schema sees as bytes.
    CreateSmartContract.Builder contract = CreateSmartContract.newBuilder();
    SmartContract.ABI.Builder abi = SmartContract.ABI.newBuilder();
    for (int i = 0; i < BUDGET; i++) {
      abi.addEntrys(SmartContract.ABI.Entry.getDefaultInstance());
    }
    contract.setNewContract(SmartContract.newBuilder().setAbi(abi));
    Transaction tx = Transaction.newBuilder().setRawData(Transaction.raw.newBuilder()
        .addContract(Transaction.Contract.newBuilder()
            .setType(Transaction.Contract.ContractType.CreateSmartContract)
            .setParameter(Any.pack(contract.build())))).build();
    ZksnarkRequest request = ZksnarkRequest.newBuilder().setTransaction(tx).build();
    GuardedRequestMarshaller<Message> marshaller =
        guarded(proto(ZksnarkRequest.getDefaultInstance()), ZksnarkRequest.getDefaultInstance());
    assertEquals(Reason.OVER_BUDGET, marshaller.rejectionReason(
        marshaller.parse(new ByteArrayInputStream(request.toByteArray()))));
    // Without the contract payload the same request is far below the budget.
    assertTrue(TransactionAdmissionGuard.count(request.toBuilder().setTransaction(
        tx.toBuilder().setRawData(tx.getRawData().toBuilder().clearContract())).build()
        .toByteArray(), ZksnarkRequest.getDescriptor()) < BUDGET);
  }

  @Test
  public void prototypeOfAcceptsOnlyAProtobufInstance() {
    assertSame(Transaction.getDefaultInstance(),
        GuardedRequestMarshaller.prototypeOf(proto(Transaction.getDefaultInstance())));
    assertEquals(GrpcAPI.EmptyMessage.getDefaultInstance(),
        GuardedRequestMarshaller.prototypeOf(plain(GrpcAPI.EmptyMessage.getDefaultInstance())));
    assertNull(GuardedRequestMarshaller.prototypeOf(new MethodDescriptor.Marshaller<String>() {
      @Override
      public InputStream stream(String value) {
        throw new UnsupportedOperationException();
      }

      @Override
      public String parse(InputStream stream) {
        return "not a message";
      }
    }));
    assertNull(GuardedRequestMarshaller.prototypeOf(new MethodDescriptor.Marshaller<Message>() {
      @Override
      public InputStream stream(Message value) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Message parse(InputStream stream) {
        return null;
      }
    }));
  }

  // ---------------------------------------------------------------- helpers

  @SuppressWarnings("unchecked")
  private static <T extends Message> GuardedRequestMarshaller<T> guarded(
      MethodDescriptor.Marshaller<T> delegate, Message prototype) {
    return new GuardedRequestMarshaller<>(delegate, prototype, MAX, BUDGET);
  }

  @SuppressWarnings("unchecked")
  private static <T extends Message> MethodDescriptor.Marshaller<T> proto(T prototype) {
    return ProtoUtils.marshaller(prototype);
  }

  private static <T extends Message> MethodDescriptor.Marshaller<T> plain(T prototype) {
    return new MethodDescriptor.Marshaller<T>() {
      @Override
      public InputStream stream(T value) {
        return new ByteArrayInputStream(value.toByteArray());
      }

      @Override
      @SuppressWarnings("unchecked")
      public T parse(InputStream stream) {
        try {
          return (T) prototype.getParserForType().parseFrom(stream);
        } catch (InvalidProtocolBufferException e) {
          throw Status.INTERNAL.withCause(e).asRuntimeException();
        }
      }
    };
  }

  @SuppressWarnings("unchecked")
  private static <T> ServerMethodDefinition<T, Object> method(
      MethodDescriptor.Marshaller<T> request, Message prototype) {
    MethodDescriptor<T, Object> descriptor = MethodDescriptor.<T, Object>newBuilder()
        .setType(MethodDescriptor.MethodType.UNARY)
        .setFullMethodName(MethodDescriptor.generateFullMethodName(WalletGrpc.SERVICE_NAME,
            "Test" + prototype.getDescriptorForType().getName() + System.nanoTime()))
        .setRequestMarshaller(request)
        .setResponseMarshaller((MethodDescriptor.Marshaller<Object>) (Object)
            ProtoUtils.marshaller(GrpcAPI.Return.getDefaultInstance()))
        .build();
    return ServerMethodDefinition.create(descriptor,
        (ServerCallHandler<T, Object>) (Object) HANDLER);
  }

  /** {@code count} varint fields, each with its own field number unknown to every type. */
  static byte[] distinctUnknownFields(int count) {
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    for (int i = 0; i < count; i++) {
      long tag = (long) (1000 + i) << 3;
      while ((tag & ~0x7FL) != 0) {
        out.write((int) ((tag & 0x7F) | 0x80));
        tag >>>= 7;
      }
      out.write((int) tag);
      out.write(0);
    }
    return out.toByteArray();
  }

  private static double rejections(String method, Reason reason) {
    Double value = CollectorRegistry.defaultRegistry.getSampleValue(
        MetricKeys.Counter.GRPC_ADMISSION_REJECT + "_total", new String[] {"method", "reason"},
        new String[] {method, reason.name()});
    return value == null ? 0 : value;
  }

  private static final class Counting<T> implements MethodDescriptor.Marshaller<T> {

    private final MethodDescriptor.Marshaller<T> real;
    private final AtomicInteger parsed = new AtomicInteger();

    Counting(MethodDescriptor.Marshaller<T> real) {
      this.real = real;
    }

    @Override
    public InputStream stream(T value) {
      return real.stream(value);
    }

    @Override
    public T parse(InputStream stream) {
      parsed.incrementAndGet();
      return real.parse(stream);
    }
  }
}
