package org.tron.core.admission;

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.admission.TransactionAdmissionException.Reason;

/**
 * Closes the call with {@code INVALID_ARGUMENT}, or {@code RESOURCE_EXHAUSTED} for a request over
 * the size limit, when {@link GuardedRequestMarshaller} hands over a rejection sentinel; the
 * service method is never invoked.
 */
@Slf4j(topic = "rpc")
public final class AdmissionCheckedHandler<ReqT, RespT> implements ServerCallHandler<ReqT, RespT> {

  private final ServerCallHandler<ReqT, RespT> delegate;
  private final GuardedRequestMarshaller<ReqT> marshaller;

  AdmissionCheckedHandler(ServerCallHandler<ReqT, RespT> delegate,
      GuardedRequestMarshaller<ReqT> marshaller) {
    this.delegate = delegate;
    this.marshaller = marshaller;
  }

  public ServerCallHandler<ReqT, RespT> getDelegate() {
    return delegate;
  }

  public GuardedRequestMarshaller<ReqT> getMarshaller() {
    return marshaller;
  }

  @Override
  public ServerCall.Listener<ReqT> startCall(ServerCall<ReqT, RespT> call, Metadata headers) {
    ServerCall.Listener<ReqT> next = delegate.startCall(call, headers);
    return new SimpleForwardingServerCallListener<ReqT>(next) {
      // Listener callbacks of one call are serialized by grpc.
      private boolean rejected;

      @Override
      public void onMessage(ReqT message) {
        if (rejected) {
          return;
        }
        Reason reason = marshaller.rejectionReason(message);
        if (reason != null) {
          rejected = true;
          String method = call.getMethodDescriptor().getFullMethodName();
          if (logger.isDebugEnabled()) {
            logger.debug("gRPC {} from {} rejected by admission check: {}", method,
                call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR), reason);
          }
          GuardedRequestMarshaller.countRejection(method, reason);
          // The same status grpc gives an uncompressed message over the size limit.
          Status status = reason == Reason.TOO_LARGE
              ? Status.RESOURCE_EXHAUSTED : Status.INVALID_ARGUMENT;
          call.close(status.withDescription(marshaller.rejectionMessage()), new Metadata());
          return;
        }
        super.onMessage(message);
      }

      @Override
      public void onHalfClose() {
        if (!rejected) {
          super.onHalfClose();
        }
      }

      @Override
      public void onReady() {
        if (!rejected) {
          super.onReady();
        }
      }
    };
  }
}
