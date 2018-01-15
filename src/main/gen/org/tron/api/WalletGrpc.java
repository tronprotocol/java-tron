package org.tron.api;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 * <pre>
 * just a example
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.9.0)",
    comments = "Source: api/api.proto")
public final class WalletGrpc {

  private WalletGrpc() {}

  public static final String SERVICE_NAME = "proto.Wallet";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetBalanceMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.Balance,
      org.tron.api.GrpcAPI.Balance> METHOD_GET_BALANCE = getGetBalanceMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.Balance,
      org.tron.api.GrpcAPI.Balance> getGetBalanceMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.Balance,
      org.tron.api.GrpcAPI.Balance> getGetBalanceMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.Balance, org.tron.api.GrpcAPI.Balance> getGetBalanceMethod;
    if ((getGetBalanceMethod = WalletGrpc.getGetBalanceMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetBalanceMethod = WalletGrpc.getGetBalanceMethod) == null) {
          WalletGrpc.getGetBalanceMethod = getGetBalanceMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.Balance, org.tron.api.GrpcAPI.Balance>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.Wallet", "GetBalance"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.Balance.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.Balance.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetBalance"))
                  .build();
          }
        }
     }
     return getGetBalanceMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static WalletStub newStub(io.grpc.Channel channel) {
    return new WalletStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static WalletBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new WalletBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static WalletFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new WalletFutureStub(channel);
  }

  /**
   * <pre>
   * just a example
   * </pre>
   */
  public static abstract class WalletImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * get account balance
     * </pre>
     */
    public void getBalance(org.tron.api.GrpcAPI.Balance request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.Balance> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBalanceMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGetBalanceMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.Balance,
                org.tron.api.GrpcAPI.Balance>(
                  this, METHODID_GET_BALANCE)))
          .build();
    }
  }

  /**
   * <pre>
   * just a example
   * </pre>
   */
  public static final class WalletStub extends io.grpc.stub.AbstractStub<WalletStub> {
    private WalletStub(io.grpc.Channel channel) {
      super(channel);
    }

    private WalletStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WalletStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new WalletStub(channel, callOptions);
    }

    /**
     * <pre>
     * get account balance
     * </pre>
     */
    public void getBalance(org.tron.api.GrpcAPI.Balance request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.Balance> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBalanceMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * just a example
   * </pre>
   */
  public static final class WalletBlockingStub extends io.grpc.stub.AbstractStub<WalletBlockingStub> {
    private WalletBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private WalletBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WalletBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new WalletBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * get account balance
     * </pre>
     */
    public org.tron.api.GrpcAPI.Balance getBalance(org.tron.api.GrpcAPI.Balance request) {
      return blockingUnaryCall(
          getChannel(), getGetBalanceMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * just a example
   * </pre>
   */
  public static final class WalletFutureStub extends io.grpc.stub.AbstractStub<WalletFutureStub> {
    private WalletFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private WalletFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WalletFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new WalletFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * get account balance
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.Balance> getBalance(
        org.tron.api.GrpcAPI.Balance request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBalanceMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_BALANCE = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final WalletImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(WalletImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_BALANCE:
          serviceImpl.getBalance((org.tron.api.GrpcAPI.Balance) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.Balance>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class WalletBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    WalletBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.tron.api.GrpcAPI.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Wallet");
    }
  }

  private static final class WalletFileDescriptorSupplier
      extends WalletBaseDescriptorSupplier {
    WalletFileDescriptorSupplier() {}
  }

  private static final class WalletMethodDescriptorSupplier
      extends WalletBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    WalletMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (WalletGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new WalletFileDescriptorSupplier())
              .addMethod(getGetBalanceMethod())
              .build();
        }
      }
    }
    return result;
  }
}
