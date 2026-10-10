package org.tron.core.admission;

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.admission.TransactionAdmissionException.Reason;
import org.tron.protos.Protocol.Transaction;

/**
 * Closes the call with {@code INVALID_ARGUMENT} when {@link GuardedTransactionMarshaller} hands
 * over a rejection sentinel; the service method is never invoked.
 */
@Slf4j(topic = "rpc")
public final class AdmissionCheckedHandler<RespT> implements ServerCallHandler<Transaction, RespT> {

  private final ServerCallHandler<Transaction, RespT> delegate;

  public AdmissionCheckedHandler(ServerCallHandler<Transaction, RespT> delegate) {
    this.delegate = delegate;
  }

  public ServerCallHandler<Transaction, RespT> getDelegate() {
    return delegate;
  }

  @Override
  public ServerCall.Listener<Transaction> startCall(ServerCall<Transaction, RespT> call,
      Metadata headers) {
    ServerCall.Listener<Transaction> next = delegate.startCall(call, headers);
    return new SimpleForwardingServerCallListener<Transaction>(next) {
      // Listener callbacks of one call are serialized by grpc.
      private boolean rejected;

      @Override
      public void onMessage(Transaction message) {
        if (rejected) {
          return;
        }
        Reason reason = GuardedTransactionMarshaller.rejectionReason(message);
        if (reason != null) {
          rejected = true;
          if (logger.isDebugEnabled()) {
            logger.debug("gRPC {} from {} rejected by admission check: {}",
                call.getMethodDescriptor().getFullMethodName(),
                call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR), reason);
          }
          call.close(Status.INVALID_ARGUMENT
              .withDescription(TransactionAdmissionException.REJECTION_MESSAGE), new Metadata());
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
