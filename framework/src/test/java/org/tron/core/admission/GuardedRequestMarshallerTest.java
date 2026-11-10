package org.tron.core.admission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.IdentityHashMap;
import java.util.Map;
import org.junit.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.api.ZksnarkGrpcAPI.ZksnarkRequest;
import org.tron.core.admission.ProtobufAdmissionException.Reason;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;

public class GuardedRequestMarshallerTest {

  private static final int MAX = 4096;
  private static final int BUDGET = 10;
  private static final ServerCallHandler<Object, Object> HANDLER =
      (call, headers) -> new ServerCall.Listener<Object>() { };

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
        Message sentinel = marshaller.rejectionSentinel(reason);
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
    assertEquals(ProtobufAdmissionException.REQUEST_REJECTION_MESSAGE,
        marshaller.rejectionMessage());
    assertEquals(ProtobufAdmissionException.REJECTION_MESSAGE,
        guarded(proto(Transaction.getDefaultInstance()), Transaction.getDefaultInstance())
            .rejectionMessage());
  }

  @Test
  public void wrappingUsesTheMarshallerPrototype() {
    ServerMethodDefinition<GrpcAPI.EmptyMessage, Object> method =
        method(proto(GrpcAPI.EmptyMessage.getDefaultInstance()),
            GrpcAPI.EmptyMessage.getDefaultInstance());
    ServerMethodDefinition<GrpcAPI.EmptyMessage, Object> wrapped =
        GuardedRequestMarshaller.guard(method, MAX, BUDGET);
    MethodDescriptor.Marshaller<GrpcAPI.EmptyMessage> request =
        wrapped.getMethodDescriptor().getRequestMarshaller();
    assertTrue(request instanceof GuardedRequestMarshaller);
    assertTrue(wrapped.getServerCallHandler() instanceof AdmissionCheckedHandler);
    assertSame(request,
        ((AdmissionCheckedHandler<?, ?>) wrapped.getServerCallHandler()).getMarshaller());
    assertSame(method.getServerCallHandler(),
        ((AdmissionCheckedHandler<?, ?>) wrapped.getServerCallHandler()).getDelegate());
    assertEquals(GrpcAPI.EmptyMessage.getDescriptor(),
        ((GuardedRequestMarshaller<?>) request).getRoot());
  }

  @Test
  public void wrappingFailsWithoutAProtobufPrototype() {
    assertWrappingFails(plain(GrpcAPI.EmptyMessage.getDefaultInstance()));
    assertWrappingFails(prototypeMarshaller(null));
    assertWrappingFails(prototypeMarshaller("not a message"));
  }

  @Test
  public void wrappingFailsOnAnAlreadyGuardedRequest() {
    ServerMethodDefinition<GrpcAPI.EmptyMessage, Object> method =
        method(proto(GrpcAPI.EmptyMessage.getDefaultInstance()),
            GrpcAPI.EmptyMessage.getDefaultInstance());
    String name = method.getMethodDescriptor().getFullMethodName();
    ServerMethodDefinition<GrpcAPI.EmptyMessage, Object> guarded =
        GuardedRequestMarshaller.guard(method, MAX, BUDGET);

    assertAlreadyGuarded(guarded, name);
    // An interceptor keeps the request marshaller, so a guard inside it is still found.
    assertAlreadyGuarded(intercepted(guarded), name);

    ServerMethodDefinition<GrpcAPI.EmptyMessage, Object> interceptedFirst = intercepted(method);
    ServerMethodDefinition<GrpcAPI.EmptyMessage, Object> wrapped =
        GuardedRequestMarshaller.guard(interceptedFirst, MAX, BUDGET);
    assertSame(interceptedFirst.getServerCallHandler(),
        ((AdmissionCheckedHandler<?, ?>) wrapped.getServerCallHandler()).getDelegate());
  }

  private static <T> void assertAlreadyGuarded(ServerMethodDefinition<T, Object> method,
      String name) {
    try {
      GuardedRequestMarshaller.guard(method, MAX, BUDGET);
      fail("expected a wiring failure for " + name);
    } catch (IllegalStateException e) {
      assertEquals("request already guarded: " + name, e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> ServerMethodDefinition<T, Object> intercepted(
      ServerMethodDefinition<T, Object> method) {
    String name = method.getMethodDescriptor().getFullMethodName();
    ServerServiceDefinition service = ServerInterceptors.intercept(
        ServerServiceDefinition.builder(MethodDescriptor.extractFullServiceName(name))
            .addMethod(method).build(),
        new ServerInterceptor() {
          @Override
          public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
              ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
            return next.startCall(call, headers);
          }
        });
    ServerMethodDefinition<T, Object> result =
        (ServerMethodDefinition<T, Object>) service.getMethod(name);
    assertSame(method.getMethodDescriptor().getRequestMarshaller(),
        result.getMethodDescriptor().getRequestMarshaller());
    assertNotSame(method.getServerCallHandler(), result.getServerCallHandler());
    return result;
  }

  private static <T> void assertWrappingFails(MethodDescriptor.Marshaller<T> request) {
    ServerMethodDefinition<T, Object> method =
        method(request, GrpcAPI.EmptyMessage.getDefaultInstance());
    String name = method.getMethodDescriptor().getFullMethodName();
    try {
      GuardedRequestMarshaller.guard(method, MAX, BUDGET);
      fail("expected a wiring failure for " + name);
    } catch (IllegalStateException e) {
      assertEquals("no protobuf request prototype: " + name, e.getMessage());
    }
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

    // A Transaction delegate: scanned as the Transaction it becomes.
    ServerMethodDefinition<Transaction, Object> transactionMethod = GuardedRequestMarshaller.guard(
        method(proto(Transaction.getDefaultInstance()), GrpcAPI.BytesMessage.getDefaultInstance()),
        MAX, BUDGET);
    GuardedRequestMarshaller<Transaction> asTransaction =
        (GuardedRequestMarshaller<Transaction>)
            transactionMethod.getMethodDescriptor().getRequestMarshaller();
    assertEquals(Transaction.getDescriptor(), asTransaction.getRoot());
    assertEquals(Reason.OVER_BUDGET,
        asTransaction.rejectionReason(asTransaction.parse(new ByteArrayInputStream(data))));

    // A BytesMessage delegate: one bytes field, nothing is materialized.
    ServerMethodDefinition<GrpcAPI.BytesMessage, Object> bytesMethod =
        GuardedRequestMarshaller.guard(method(proto(GrpcAPI.BytesMessage.getDefaultInstance()),
            Transaction.getDefaultInstance()), MAX, BUDGET);
    GuardedRequestMarshaller<GrpcAPI.BytesMessage> asBytes =
        (GuardedRequestMarshaller<GrpcAPI.BytesMessage>)
            bytesMethod.getMethodDescriptor().getRequestMarshaller();
    assertEquals(GrpcAPI.BytesMessage.getDescriptor(), asBytes.getRoot());
    GrpcAPI.BytesMessage parsed = asBytes.parse(new ByteArrayInputStream(data));
    assertNull(asBytes.rejectionReason(parsed));
    assertEquals(data.length - 2, parsed.getValue().size());
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
    assertTrue(ProtobufAdmissionGuard.count(request.toBuilder().setTransaction(
        tx.toBuilder().setRawData(tx.getRawData().toBuilder().clearContract())).build()
        .toByteArray(), ZksnarkRequest.getDescriptor()) < BUDGET);
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

  private static <T> MethodDescriptor.Marshaller<T> prototypeMarshaller(T prototype) {
    return new MethodDescriptor.PrototypeMarshaller<T>() {
      @Override
      public T getMessagePrototype() {
        return prototype;
      }

      @Override
      @SuppressWarnings("unchecked")
      public Class<T> getMessageClass() {
        return (Class<T>) Object.class;
      }

      @Override
      public InputStream stream(T value) {
        throw new UnsupportedOperationException();
      }

      @Override
      public T parse(InputStream stream) {
        throw new UnsupportedOperationException();
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

}
