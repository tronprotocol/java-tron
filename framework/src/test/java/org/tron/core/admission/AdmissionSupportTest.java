package org.tron.core.admission;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.ProtoUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.Test;
import org.tron.core.admission.TransactionAdmissionException.Reason;
import org.tron.protos.Protocol.Transaction;

public class AdmissionSupportTest {

  // ---------------------------------------------------------------- readBounded

  @Test
  public void readBoundedReturnsExactlyTheBytesRead() throws IOException {
    byte[] data = new byte[1000];
    data[999] = 7;
    assertArrayEquals(data, GuardedTransactionMarshaller.readBounded(
        new HintStream(data, 0), 1000));                         // no hint
    assertArrayEquals(data, GuardedTransactionMarshaller.readBounded(
        new HintStream(data, 1000), 1000));                      // exact hint
    assertArrayEquals(data, GuardedTransactionMarshaller.readBounded(
        new HintStream(data, 1), 1000));                         // decompressing stream
    assertArrayEquals(data, GuardedTransactionMarshaller.readBounded(
        new HintStream(data, Integer.MAX_VALUE), 4096));         // huge hint, short content
    assertArrayEquals(data, GuardedTransactionMarshaller.readBounded(
        new HintStream(data, 10), Integer.MAX_VALUE));           // no overflow at max int
    assertArrayEquals(new byte[0], GuardedTransactionMarshaller.readBounded(
        new HintStream(new byte[0], 0), 0));
  }

  @Test
  public void readBoundedRejectsMoreThanMax() throws IOException {
    byte[] data = new byte[1001];
    assertNull(GuardedTransactionMarshaller.readBounded(new HintStream(data, 0), 1000));
    assertNull(GuardedTransactionMarshaller.readBounded(new HintStream(data, 1001), 1000));
    assertNull(GuardedTransactionMarshaller.readBounded(new HintStream(new byte[1], 0), 0));
  }

  // ---------------------------------------------------------------- marshaller

  @Test
  public void marshallerReturnsTheSentinelBeforeTheDelegateParses() throws Exception {
    CountingMarshaller delegate = new CountingMarshaller();
    GuardedTransactionMarshaller marshaller = new GuardedTransactionMarshaller(delegate, 4096, 10);

    byte[] flood = new byte[2 * 11];
    for (int i = 0; i < flood.length; i += 2) {
      flood[i] = 0x2a;
    }
    assertNotNull(GuardedTransactionMarshaller.rejectionReason(
        marshaller.parse(new ByteArrayInputStream(flood))));                 // over budget
    assertNotNull(GuardedTransactionMarshaller.rejectionReason(
        marshaller.parse(new ByteArrayInputStream(new byte[4097]))));        // over max size
    assertNotNull(GuardedTransactionMarshaller.rejectionReason(
        marshaller.parse(new ByteArrayInputStream(new byte[] {0x0b, 0x0c}))));  // malformed
    assertEquals(0, delegate.parsed.get());

    Transaction tx = Transaction.newBuilder().addSignature(
        com.google.protobuf.ByteString.copyFrom(new byte[65])).build();
    Transaction parsed = marshaller.parse(new ByteArrayInputStream(tx.toByteArray()));
    assertEquals(tx, parsed);
    assertNull(GuardedTransactionMarshaller.rejectionReason(parsed));
    assertEquals(1, delegate.parsed.get());
  }

  @Test
  public void sentinelIsNeverAParsedOrDefaultTransaction() throws Exception {
    assertNull(GuardedTransactionMarshaller.rejectionReason(Transaction.getDefaultInstance()));
    assertNull(GuardedTransactionMarshaller.rejectionReason(Transaction.parseFrom(new byte[0])));
    assertNull(GuardedTransactionMarshaller.rejectionReason(Transaction.newBuilder().build()));
    CountingMarshaller delegate = new CountingMarshaller();
    GuardedTransactionMarshaller marshaller = new GuardedTransactionMarshaller(delegate, 4096, 10);
    assertNull(GuardedTransactionMarshaller.rejectionReason(
        marshaller.parse(new ByteArrayInputStream(new byte[0]))));
  }

  @Test
  public void eachRejectionMapsToItsReason() throws Exception {
    CountingMarshaller delegate = new CountingMarshaller();
    GuardedTransactionMarshaller marshaller = new GuardedTransactionMarshaller(delegate, 4096, 10);
    byte[] flood = new byte[2 * 11];
    for (int i = 0; i < flood.length; i += 2) {
      flood[i] = 0x2a;
    }
    assertEquals(Reason.OVER_BUDGET, reason(marshaller, new ByteArrayInputStream(flood)));
    assertEquals(Reason.TOO_LARGE, reason(marshaller, new ByteArrayInputStream(new byte[4097])));
    assertEquals(Reason.MALFORMED,
        reason(marshaller, new ByteArrayInputStream(new byte[] {0x0b, 0x0c})));
    assertEquals(0, delegate.parsed.get());
    assertNull(GuardedTransactionMarshaller.rejectionReason(Transaction.getDefaultInstance()));
  }

  @Test
  public void decodingErrorOfTheDelegateIsAMalformedRejection() {
    CountingMarshaller delegate = new CountingMarshaller();
    GuardedTransactionMarshaller marshaller = new GuardedTransactionMarshaller(delegate, 4096, 10);
    // Well-formed wire within budget, but Any.type_url is not valid UTF-8.
    assertEquals(Reason.MALFORMED,
        reason(marshaller, new ByteArrayInputStream(INVALID_UTF8_TYPE_URL)));
    assertEquals(1, delegate.parsed.get());
  }

  @Test
  public void corruptOrTruncatedCompressedInputIsAMalformedRejection() throws Exception {
    CountingMarshaller delegate = new CountingMarshaller();
    GuardedTransactionMarshaller marshaller = new GuardedTransactionMarshaller(delegate, 4096, 10);
    byte[] corrupt = {0x1f, (byte) 0x8b, 8, 0, 0, 0, 0, 0, 0, (byte) 0xff, (byte) 0xff, 0, 0};
    assertEquals(Reason.MALFORMED,
        reason(marshaller, new GZIPInputStream(new ByteArrayInputStream(corrupt))));
    ByteArrayOutputStream gzip = new ByteArrayOutputStream();
    try (GZIPOutputStream out = new GZIPOutputStream(gzip)) {
      out.write(new byte[1000]);
    }
    byte[] truncated = Arrays.copyOf(gzip.toByteArray(), gzip.size() / 2);
    assertEquals(Reason.MALFORMED,
        reason(marshaller, new GZIPInputStream(new ByteArrayInputStream(truncated))));
    assertEquals(0, delegate.parsed.get());
  }

  @Test
  public void otherReadFailuresAreThrownWithTheirCause() {
    CountingMarshaller delegate = new CountingMarshaller();
    GuardedTransactionMarshaller marshaller = new GuardedTransactionMarshaller(delegate, 4096, 10);
    IOException failure = new IOException("read failed");
    InputStream failsOnAvailable = new InputStream() {
      @Override
      public int read() {
        return -1;
      }

      @Override
      public int available() throws IOException {
        throw failure;
      }
    };
    InputStream failsAfterSomeBytes = new InputStream() {
      private int left = 3;

      @Override
      public int read() throws IOException {
        if (left-- > 0) {
          return 0x08;
        }
        throw failure;
      }
    };
    for (InputStream stream : Arrays.asList(failsOnAvailable, failsAfterSomeBytes)) {
      try {
        marshaller.parse(stream);
        fail("expected a read failure");
      } catch (StatusRuntimeException e) {
        assertEquals(Status.Code.INTERNAL, e.getStatus().getCode());
        assertSame(failure, e.getCause());
      }
    }
    assertEquals(0, delegate.parsed.get());
  }

  @Test
  public void unexpectedDelegateFailuresAreNotTurnedIntoRejections() {
    RuntimeException bug = new IllegalStateException("bug");
    StatusRuntimeException other = Status.INTERNAL.withCause(new IOException("x"))
        .asRuntimeException();
    for (RuntimeException failure : Arrays.asList(bug, other)) {
      MethodDescriptor.Marshaller<Transaction> failing =
          new MethodDescriptor.Marshaller<Transaction>() {
            @Override
            public InputStream stream(Transaction value) {
              throw new UnsupportedOperationException();
            }

            @Override
            public Transaction parse(InputStream stream) {
              throw failure;
            }
          };
      GuardedTransactionMarshaller marshaller =
          new GuardedTransactionMarshaller(failing, 4096, 10);
      try {
        marshaller.parse(new ByteArrayInputStream(new byte[0]));
        fail("expected the delegate failure");
      } catch (RuntimeException e) {
        assertSame(failure, e);
      }
    }
  }

  @Test
  public void plainDelegateMarshallerIsSupported() {
    MethodDescriptor.Marshaller<Transaction> plain =
        new MethodDescriptor.Marshaller<Transaction>() {
      @Override
      public InputStream stream(Transaction value) {
        return new ByteArrayInputStream(value.toByteArray());
      }

      @Override
      public Transaction parse(InputStream stream) {
        try {
          return Transaction.parseFrom(stream);
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
      }
    };
    GuardedTransactionMarshaller marshaller = new GuardedTransactionMarshaller(plain, 4096, 10);
    Transaction tx = Transaction.newBuilder().addSignature(
        com.google.protobuf.ByteString.copyFrom(new byte[65])).build();
    assertEquals(tx, marshaller.parse(new ByteArrayInputStream(tx.toByteArray())));
    assertSame(Transaction.getDefaultInstance(), marshaller.getMessagePrototype());
  }

  // raw_data { contract { parameter { type_url: C3 28 } } }
  static final byte[] INVALID_UTF8_TYPE_URL =
      {0x0a, 0x08, 0x5a, 0x06, 0x12, 0x04, 0x0a, 0x02, (byte) 0xc3, 0x28};

  private static Reason reason(GuardedTransactionMarshaller marshaller, InputStream stream) {
    return GuardedTransactionMarshaller.rejectionReason(marshaller.parse(stream));
  }

  private static final class CountingMarshaller
      implements MethodDescriptor.PrototypeMarshaller<Transaction> {

    private final MethodDescriptor.PrototypeMarshaller<Transaction> real =
        (MethodDescriptor.PrototypeMarshaller<Transaction>)
            ProtoUtils.marshaller(Transaction.getDefaultInstance());
    private final AtomicInteger parsed = new AtomicInteger();

    @Override
    public InputStream stream(Transaction value) {
      return real.stream(value);
    }

    @Override
    public Transaction parse(InputStream stream) {
      parsed.incrementAndGet();
      return real.parse(stream);
    }

    @Override
    public Transaction getMessagePrototype() {
      return real.getMessagePrototype();
    }

    @Override
    public Class<Transaction> getMessageClass() {
      return real.getMessageClass();
    }
  }

  /** Stream whose available() returns a fixed hint regardless of the real content. */
  private static final class HintStream extends InputStream {

    private final ByteArrayInputStream in;
    private final int hint;

    HintStream(byte[] data, int hint) {
      this.in = new ByteArrayInputStream(data);
      this.hint = hint;
    }

    @Override
    public int read() {
      return in.read();
    }

    @Override
    public int read(byte[] b, int off, int len) {
      return in.read(b, off, len);
    }

    @Override
    public int available() {
      return hint;
    }
  }
}
