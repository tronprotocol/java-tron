package org.tron.core.admission;

import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.zip.ZipException;
import org.tron.common.math.StrictMathWrapper;
import org.tron.core.admission.TransactionAdmissionException.Reason;
import org.tron.protos.Protocol.Transaction;

/**
 * Reads the request with a size bound and runs {@link TransactionAdmissionGuard} before the
 * original marshaller parses the same bytes. A rejection returns a per-{@link Reason} sentinel
 * instead of throwing, because grpc logs every marshaller exception at SEVERE; the
 * {@link AdmissionCheckedHandler} closes the call. Interceptors see a sentinel as an empty
 * {@code Transaction}.
 */
public final class GuardedTransactionMarshaller
    implements MethodDescriptor.PrototypeMarshaller<Transaction> {

  static final int MAX_INITIAL_BUFFER = 1 << 20;
  private static final int DEFAULT_INITIAL_BUFFER = 8 * 1024;

  // Compared by identity; never the default instance.
  private static final Map<Reason, Transaction> REJECTED = new EnumMap<>(Reason.class);

  static {
    for (Reason reason : Reason.values()) {
      REJECTED.put(reason, Transaction.newBuilder().build());
    }
  }

  private final MethodDescriptor.Marshaller<Transaction> delegate;
  private final int maxMessageSize;
  private final int maxOccurrences;

  public GuardedTransactionMarshaller(MethodDescriptor.Marshaller<Transaction> delegate,
      int maxMessageSize, int maxOccurrences) {
    this.delegate = delegate;
    this.maxMessageSize = maxMessageSize;
    this.maxOccurrences = maxOccurrences;
  }

  @Override
  public InputStream stream(Transaction value) {
    return delegate.stream(value);
  }

  @Override
  public Transaction parse(InputStream stream) {
    byte[] bytes;
    try {
      bytes = readBounded(stream, maxMessageSize);
    } catch (ZipException | EOFException e) {
      return REJECTED.get(Reason.MALFORMED);
    } catch (IOException e) {
      throw Status.INTERNAL.withDescription("failed to read request").withCause(e)
          .asRuntimeException();
    }
    if (bytes == null) {
      return REJECTED.get(Reason.TOO_LARGE);
    }
    try {
      TransactionAdmissionGuard.checkTransaction(bytes, maxOccurrences);
    } catch (TransactionAdmissionException e) {
      return REJECTED.get(e.getReason());
    }
    try {
      return delegate.parse(new ByteArrayInputStream(bytes));
    } catch (StatusRuntimeException e) {
      if (e.getCause() instanceof InvalidProtocolBufferException) {
        return REJECTED.get(Reason.MALFORMED);
      }
      throw e;
    }
  }

  static Reason rejectionReason(Transaction request) {
    for (Map.Entry<Reason, Transaction> entry : REJECTED.entrySet()) {
      if (entry.getValue() == request) {
        return entry.getKey();
      }
    }
    return null;
  }

  @Override
  public Transaction getMessagePrototype() {
    return Transaction.getDefaultInstance();
  }

  @Override
  public Class<Transaction> getMessageClass() {
    return Transaction.class;
  }

  /** Returns the whole stream, or {@code null} once more than {@code max} bytes are seen. */
  static byte[] readBounded(InputStream stream, int max) throws IOException {
    if (max < 0) {
      throw new IllegalArgumentException("max must not be negative");
    }
    long limit = (long) max + 1;
    int hint = stream.available();
    // A decompressing stream reports 1 until it reaches the end, which says nothing about size.
    long initial = hint > 1 ? hint : DEFAULT_INITIAL_BUFFER;
    initial = StrictMathWrapper.min(initial, StrictMathWrapper.min(limit, MAX_INITIAL_BUFFER));
    byte[] buffer = new byte[(int) StrictMathWrapper.max(initial, 1)];
    int size = 0;
    while (true) {
      if (size == buffer.length) {
        // Probe for end of stream first, so an exact size hint never costs a copy.
        int next = stream.read();
        if (next < 0) {
          break;
        }
        long grown = StrictMathWrapper.min(
            StrictMathWrapper.min((long) buffer.length * 2, limit), Integer.MAX_VALUE - 8);
        if (grown <= buffer.length) {
          return null;   // cannot hold more than this; the message is over the bound
        }
        buffer = Arrays.copyOf(buffer, (int) grown);
        buffer[size++] = (byte) next;
        if (size > max) {
          return null;
        }
        continue;
      }
      int read = stream.read(buffer, size, buffer.length - size);
      if (read < 0) {
        break;
      }
      size += read;
      if (size > max) {
        return null;
      }
    }
    return size == buffer.length ? buffer : Arrays.copyOf(buffer, size);
  }
}
