package org.tron.core.admission;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.grpc.KnownLength;
import io.grpc.MethodDescriptor;
import io.grpc.ServerMethodDefinition;
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
import org.tron.core.admission.ProtobufAdmissionException.Reason;
import org.tron.protos.Protocol.Transaction;

/**
 * Reads the request with a size bound and runs {@link ProtobufAdmissionGuard}, rooted at the
 * type the original marshaller parses into, before that marshaller parses the same bytes. A
 * rejection returns a per-{@link Reason} sentinel instead of throwing, because grpc logs every
 * marshaller exception at SEVERE; the {@link AdmissionCheckedHandler} closes the call.
 * Interceptors see a sentinel as an empty request.
 */
public final class GuardedRequestMarshaller<T> implements MethodDescriptor.PrototypeMarshaller<T> {

  static final int MAX_INITIAL_BUFFER = 1 << 20;
  private static final int DEFAULT_INITIAL_BUFFER = 8 * 1024;

  private static final Reason[] REASONS = Reason.values();

  private final MethodDescriptor.Marshaller<T> delegate;
  private final Descriptor root;
  private final T prototype;
  // Compared by identity; never the default instance, which an empty request parses to.
  private final Map<Reason, T> rejected = new EnumMap<>(Reason.class);
  private final int maxMessageSize;
  private final int maxOccurrences;

  @SuppressWarnings("unchecked")
  GuardedRequestMarshaller(MethodDescriptor.Marshaller<T> delegate, Message prototype,
      int maxMessageSize, int maxOccurrences) {
    this.delegate = delegate;
    this.root = prototype.getDescriptorForType();
    this.prototype = (T) prototype.getDefaultInstanceForType();
    for (Reason reason : Reason.values()) {
      rejected.put(reason, (T) prototype.newBuilderForType().build());
    }
    this.maxMessageSize = maxMessageSize;
    this.maxOccurrences = maxOccurrences;
  }

  /**
   * Wraps {@code method}. The scan root is the type its request marshaller parses into, taken
   * once here from the marshaller's protobuf prototype and never from a real request.
   *
   * @throws IllegalStateException if the request marshaller gives no protobuf prototype or is
   *     already guarded
   */
  public static <ReqT, RespT> ServerMethodDefinition<ReqT, RespT> guard(
      ServerMethodDefinition<ReqT, RespT> method, int maxMessageSize, int maxOccurrences) {
    MethodDescriptor<ReqT, RespT> descriptor = method.getMethodDescriptor();
    if (descriptor.getRequestMarshaller() instanceof GuardedRequestMarshaller) {
      throw new IllegalStateException(
          "request already guarded: " + descriptor.getFullMethodName());
    }
    GuardedRequestMarshaller<ReqT> guarded = new GuardedRequestMarshaller<>(
        descriptor.getRequestMarshaller(), prototypeOf(descriptor), maxMessageSize,
        maxOccurrences);
    return ServerMethodDefinition.create(
        descriptor.toBuilder(guarded, descriptor.getResponseMarshaller()).build(),
        new AdmissionCheckedHandler<>(method.getServerCallHandler(), guarded));
  }

  private static Message prototypeOf(MethodDescriptor<?, ?> descriptor) {
    MethodDescriptor.Marshaller<?> marshaller = descriptor.getRequestMarshaller();
    Object prototype = marshaller instanceof MethodDescriptor.PrototypeMarshaller
        ? ((MethodDescriptor.PrototypeMarshaller<?>) marshaller).getMessagePrototype()
        : null;
    if (!(prototype instanceof Message)) {
      throw new IllegalStateException(
          "no protobuf request prototype: " + descriptor.getFullMethodName());
    }
    return (Message) prototype;
  }

  @Override
  public InputStream stream(T value) {
    return delegate.stream(value);
  }

  @Override
  public T parse(InputStream stream) {
    try {
      return parseChecked(stream);
    } catch (ProtobufAdmissionException e) {
      return rejected.get(e.getReason());
    }
  }

  public Reason rejectionReason(T request) {
    // Called for every accepted request; iterating the map would allocate entries.
    for (Reason reason : REASONS) {
      if (rejected.get(reason) == request) {
        return reason;
      }
    }
    return null;
  }

  T rejected(Reason reason) {
    return rejected.get(reason);
  }

  public Descriptor getRoot() {
    return root;
  }

  String rejectionMessage() {
    return Transaction.getDescriptor().equals(root)
        ? ProtobufAdmissionException.REJECTION_MESSAGE
        : ProtobufAdmissionException.REQUEST_REJECTION_MESSAGE;
  }

  @Override
  public T getMessagePrototype() {
    return prototype;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Class<T> getMessageClass() {
    return (Class<T>) prototype.getClass();
  }

  /** Throws {@link ProtobufAdmissionException} for a rejection. */
  private T parseChecked(InputStream stream) {
    byte[] bytes;
    try {
      bytes = readBounded(stream, maxMessageSize);
    } catch (ZipException | EOFException e) {
      throw new ProtobufAdmissionException(Reason.MALFORMED, null);
    } catch (StatusRuntimeException e) {
      // The decompressing stream of grpc enforces the inbound size limit while it is read.
      if (e.getStatus().getCode() == Status.Code.RESOURCE_EXHAUSTED) {
        throw new ProtobufAdmissionException(Reason.TOO_LARGE, null);
      }
      throw e;
    } catch (IOException e) {
      throw Status.INTERNAL.withDescription("failed to read request").withCause(e)
          .asRuntimeException();
    }
    if (bytes == null) {
      throw new ProtobufAdmissionException(Reason.TOO_LARGE, null);
    }
    ProtobufAdmissionGuard.check(bytes, root, maxOccurrences);
    try {
      return delegate.parse(new KnownLengthInputStream(bytes));
    } catch (StatusRuntimeException e) {
      if (e.getCause() instanceof InvalidProtocolBufferException) {
        throw new ProtobufAdmissionException(Reason.MALFORMED, null);
      }
      throw e;
    }
  }

  /** Returns the whole stream, or {@code null} once more than {@code max} bytes are seen. */
  static byte[] readBounded(InputStream stream, int max) throws IOException {
    if (max < 0) {
      throw new IllegalArgumentException("max must not be negative");
    }
    long limit = (long) max + 1;
    int hint = stream.available();
    long initial;
    if (stream instanceof KnownLength) {
      // The exact remaining size, so one allocation is enough.
      initial = StrictMathWrapper.min(StrictMathWrapper.max(hint, 0), limit);
    } else {
      // A decompressing stream reports 1 until it reaches the end, which says nothing about size.
      initial = hint > 1 ? hint : DEFAULT_INITIAL_BUFFER;
      initial = StrictMathWrapper.min(initial, StrictMathWrapper.min(limit, MAX_INITIAL_BUFFER));
    }
    byte[] buffer = new byte[(int) initial];
    int size = 0;
    while (true) {
      if (size == buffer.length) {
        // Probe for end of stream first, so an exact size hint never costs a copy.
        int next = stream.read();
        if (next < 0) {
          break;
        }
        long grown = StrictMathWrapper.min(
            StrictMathWrapper.min(StrictMathWrapper.max((long) buffer.length * 2,
                DEFAULT_INITIAL_BUFFER), limit), Integer.MAX_VALUE - 8);
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

  // Lets the delegate read the bytes into an array instead of parsing a stream.
  private static final class KnownLengthInputStream extends ByteArrayInputStream
      implements KnownLength {

    KnownLengthInputStream(byte[] bytes) {
      super(bytes);
    }
  }
}
