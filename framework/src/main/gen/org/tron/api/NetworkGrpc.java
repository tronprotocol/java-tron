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
 * the api of tron's network such as node list.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.9.0)",
    comments = "Source: api/api.proto")
public final class NetworkGrpc {

  private NetworkGrpc() {}

  public static final String SERVICE_NAME = "protocol.Network";

  // Static method descriptors that strictly reflect the proto.

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static NetworkStub newStub(io.grpc.Channel channel) {
    return new NetworkStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static NetworkBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new NetworkBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static NetworkFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new NetworkFutureStub(channel);
  }

  /**
   * <pre>
   * the api of tron's network such as node list.
   * </pre>
   */
  public static abstract class NetworkImplBase implements io.grpc.BindableService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .build();
    }
  }

  /**
   * <pre>
   * the api of tron's network such as node list.
   * </pre>
   */
  public static final class NetworkStub extends io.grpc.stub.AbstractStub<NetworkStub> {
    private NetworkStub(io.grpc.Channel channel) {
      super(channel);
    }

    private NetworkStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NetworkStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new NetworkStub(channel, callOptions);
    }
  }

  /**
   * <pre>
   * the api of tron's network such as node list.
   * </pre>
   */
  public static final class NetworkBlockingStub extends io.grpc.stub.AbstractStub<NetworkBlockingStub> {
    private NetworkBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private NetworkBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NetworkBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new NetworkBlockingStub(channel, callOptions);
    }
  }

  /**
   * <pre>
   * the api of tron's network such as node list.
   * </pre>
   */
  public static final class NetworkFutureStub extends io.grpc.stub.AbstractStub<NetworkFutureStub> {
    private NetworkFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private NetworkFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NetworkFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new NetworkFutureStub(channel, callOptions);
    }
  }


  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final NetworkImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(NetworkImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
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

  private static abstract class NetworkBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    NetworkBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.tron.api.GrpcAPI.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Network");
    }
  }

  private static final class NetworkFileDescriptorSupplier
      extends NetworkBaseDescriptorSupplier {
    NetworkFileDescriptorSupplier() {}
  }

  private static final class NetworkMethodDescriptorSupplier
      extends NetworkBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    NetworkMethodDescriptorSupplier(String methodName) {
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
      synchronized (NetworkGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new NetworkFileDescriptorSupplier())
              .build();
        }
      }
    }
    return result;
  }
}
