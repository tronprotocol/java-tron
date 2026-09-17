package org.tron.core.services.filter;

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.core.db.Manager;
import org.tron.core.db2.core.Chainbase;

/**
 * Switches the current thread's read cursor around the synchronous handler callback and restores it
 * afterwards, so the handler reads from the snapshot its subclass selects (HEAD / SOLIDITY / PBFT);
 * the services behind it never touch the cursor.
 *
 * <p>Two invariants it relies on:
 * <ul>
 * <li>The bracket wraps {@code onHalfClose()} — where gRPC runs the unary handler inline — not
 * {@code interceptCall}, which may land on another pool thread; the cursor is a
 * {@link ThreadLocal}, so setting it elsewhere fails silently and the port serves HEAD.
 * <li>Handlers must be synchronous: the cursor is reset when {@code onHalfClose} returns, so a read
 * deferred to another thread would read HEAD.
 * </ul>
 *
 * <p>The {@code finally} reset is mandatory: the fixed thread pool is reused, so a leftover cursor
 * leaks into the next call on that thread.
 */
public abstract class CursorServerInterceptor implements ServerInterceptor {

  @Autowired
  protected Manager dbManager;

  /** Snapshot every call on this server reads from; set by the subclass. */
  protected Chainbase.Cursor cursor;

  @Override
  public <Q, A> ServerCall.Listener<Q> interceptCall(
      ServerCall<Q, A> call, Metadata headers, ServerCallHandler<Q, A> next) {
    return new SimpleForwardingServerCallListener<Q>(next.startCall(call, headers)) {
      @Override
      public void onHalfClose() {
        try {
          dbManager.setCursor(cursor);
          super.onHalfClose();
        } finally {
          dbManager.resetCursor();
        }
      }
    };
  }
}
