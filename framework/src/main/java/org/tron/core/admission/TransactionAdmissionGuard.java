package org.tron.core.admission;

import com.google.protobuf.Any;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Internal;
import com.google.protobuf.WireFormat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.tron.core.actuator.TransactionFactory;
import org.tron.core.admission.TransactionAdmissionException.Reason;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.ShieldContract.ShieldedTransferContract;

/**
 * Counts every field occurrence of an externally submitted {@code Transaction}, including the
 * contract payload that {@code TransactionCapsule.getOwner} unpacks, and rejects it before parsing
 * once the budget is exceeded.
 *
 * <p>Never use it for block, database or P2P parsing: a valid block must not fail a local,
 * non-consensus limit.
 */
public final class TransactionAdmissionGuard {

  // About 12.7 times the largest count in mainnet history (3,926, a contract deployment).
  public static final int MAX_TX_OCCURRENCES = 50_000;

  static final int MAX_DEPTH = 32;

  private static final Descriptor TRANSACTION = Transaction.getDescriptor();
  private static final Descriptor CONTRACT = Transaction.Contract.getDescriptor();
  private static final Descriptor ANY = Any.getDescriptor();

  private static final int CONTRACT_TYPE_TAG = makeTag(
      Transaction.Contract.TYPE_FIELD_NUMBER, WireFormat.WIRETYPE_VARINT);
  private static final int CONTRACT_PARAMETER_TAG = makeTag(
      Transaction.Contract.PARAMETER_FIELD_NUMBER, WireFormat.WIRETYPE_LENGTH_DELIMITED);
  private static final int ANY_TYPE_URL_TAG = makeTag(
      Any.TYPE_URL_FIELD_NUMBER, WireFormat.WIRETYPE_LENGTH_DELIMITED);
  private static final int ANY_VALUE_TAG = makeTag(
      Any.VALUE_FIELD_NUMBER, WireFormat.WIRETYPE_LENGTH_DELIMITED);

  private static final ConcurrentMap<Descriptor, ChildTable> CHILDREN = new ConcurrentHashMap<>();
  private static final ConcurrentMap<ContractType, Payload> PAYLOADS = new ConcurrentHashMap<>();

  private TransactionAdmissionGuard() {
  }

  // WireFormat.makeTag is not public.
  private static int makeTag(int fieldNumber, int wireType) {
    return (fieldNumber << 3) | wireType;
  }

  public static void checkTransaction(byte[] data, int maxOccurrences) {
    check(data, TRANSACTION, maxOccurrences);
  }

  public static long countTransaction(byte[] data) {
    return count(data, TRANSACTION);
  }

  static void check(byte[] data, Descriptor root, int maxOccurrences) {
    if (maxOccurrences < 0) {
      throw new IllegalArgumentException("maxOccurrences must not be negative");
    }
    run(data, root, new Budget(maxOccurrences));
  }

  static long count(byte[] data, Descriptor root) {
    Budget budget = new Budget(Long.MAX_VALUE);
    run(data, root, budget);
    return budget.used;
  }

  private static void run(byte[] data, Descriptor root, Budget budget) {
    if (data == null) {
      throw new TransactionAdmissionException(Reason.MALFORMED, "null payload");
    }
    try {
      scan(data, CodedInputStream.newInstance(data), root, null, budget, 1);
    } catch (IOException e) {
      throw new TransactionAdmissionException(Reason.MALFORMED, e.getMessage());
    }
  }

  private static void scan(byte[] data, CodedInputStream in, Descriptor descriptor,
      Effective effective, Budget budget, int depth) throws IOException {
    if (depth > MAX_DEPTH) {
      throw new TransactionAdmissionException(Reason.TOO_DEEP, "depth " + depth);
    }
    int tag;
    while ((tag = in.readTag()) != 0) {
      budget.consume();
      if (isGroup(tag)) {
        throw new TransactionAdmissionException(Reason.MALFORMED, "group");
      }
      Descriptor child = null;
      if (WireFormat.getTagWireType(tag) == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
        if (descriptor == ANY && tag == ANY_VALUE_TAG) {
          // Only the value the merged Any ends up with is unpacked.
          if (effective != null && in.getTotalBytesRead() == effective.valueTagEnd) {
            child = effective.payload;
          }
        } else {
          child = children(descriptor).get(WireFormat.getTagFieldNumber(tag));
        }
      }
      if (child == null) {
        if (!in.skipField(tag)) {
          throw new TransactionAdmissionException(Reason.MALFORMED, "unexpected end group");
        }
        continue;
      }
      int length = in.readRawVarint32();
      int start = in.getTotalBytesRead();
      int oldLimit = in.pushLimit(length);
      try {
        Effective next = null;
        if (child == CONTRACT) {
          next = resolveContract(data, start, length, budget);
        } else if (descriptor == CONTRACT) {
          next = effective;
        }
        scan(data, in, child, next, budget, depth + 1);
      } finally {
        in.popLimit(oldLimit);
      }
    }
  }

  /**
   * Finds the contract type and the {@code Any} value the merged {@code Contract} ends up with:
   * the last occurrence of each, across all {@code parameter} occurrences. The caller must have
   * pushed the limit for this slice.
   */
  private static Effective resolveContract(byte[] data, int start, int length, Budget budget)
      throws IOException {
    CodedInputStream in = CodedInputStream.newInstance(data, start, length);
    // Local count only: the second pass consumes the real budget for the same occurrences.
    long left = budget.remaining();
    int type = 0;
    int typeUrlOffset = -1;
    int typeUrlLength = 0;
    int valueTagEnd = -1;
    int tag;
    while ((tag = in.readTag()) != 0) {
      left = consumeLocal(left);
      if (isGroup(tag)) {
        throw new TransactionAdmissionException(Reason.MALFORMED, "group");
      }
      if (tag == CONTRACT_TYPE_TAG) {
        type = in.readEnum();
      } else if (tag == CONTRACT_PARAMETER_TAG) {
        int parameterLength = in.readRawVarint32();
        int oldLimit = in.pushLimit(parameterLength);
        try {
          int parameterTag;
          while ((parameterTag = in.readTag()) != 0) {
            left = consumeLocal(left);
            if (isGroup(parameterTag)) {
              throw new TransactionAdmissionException(Reason.MALFORMED, "group");
            }
            if (parameterTag == ANY_TYPE_URL_TAG) {
              int urlLength = in.readRawVarint32();
              typeUrlOffset = start + in.getTotalBytesRead();
              typeUrlLength = urlLength;
              in.skipRawBytes(urlLength);
            } else if (parameterTag == ANY_VALUE_TAG) {
              valueTagEnd = start + in.getTotalBytesRead();
              in.skipField(parameterTag);
            } else if (!in.skipField(parameterTag)) {
              throw new TransactionAdmissionException(Reason.MALFORMED, "unexpected end group");
            }
          }
        } finally {
          in.popLimit(oldLimit);
        }
      } else if (!in.skipField(tag)) {
        throw new TransactionAdmissionException(Reason.MALFORMED, "unexpected end group");
      }
    }
    Payload payload = payload(ContractType.forNumber(type));
    if (payload == null || typeUrlOffset < 0
        || !typeNameMatches(data, typeUrlOffset, typeUrlLength, payload.fullName)) {
      return new Effective(null, valueTagEnd);
    }
    return new Effective(payload.descriptor, valueTagEnd);
  }

  private static long consumeLocal(long left) {
    if (left <= 0) {
      throw new TransactionAdmissionException(Reason.OVER_BUDGET, null);
    }
    return left - 1;
  }

  private static boolean isGroup(int tag) {
    int wireType = WireFormat.getTagWireType(tag);
    return wireType == WireFormat.WIRETYPE_START_GROUP
        || wireType == WireFormat.WIRETYPE_END_GROUP;
  }

  // Mirrors Any.is: the part after the last '/' must equal the full name.
  private static boolean typeNameMatches(byte[] data, int offset, int length, byte[] fullName) {
    int slash = -1;
    for (int i = offset + length - 1; i >= offset; i--) {
      if (data[i] == '/') {
        slash = i;
        break;
      }
    }
    if (slash < 0) {
      return false;
    }
    int nameLength = offset + length - slash - 1;
    if (nameLength != fullName.length) {
      return false;
    }
    for (int i = 0; i < nameLength; i++) {
      if (data[slash + 1 + i] != fullName[i]) {
        return false;
      }
    }
    return true;
  }

  // The type getOwner unpacks. Only hits are cached: actuators may register later.
  private static Payload payload(ContractType type) {
    if (type == null) {
      return null;
    }
    Payload cached = PAYLOADS.get(type);
    if (cached != null) {
      return cached;
    }
    Class<? extends GeneratedMessageV3> clazz = type == ContractType.ShieldedTransferContract
        ? ShieldedTransferContract.class : TransactionFactory.getContract(type);
    if (clazz == null) {
      return null;
    }
    Descriptor descriptor = Internal.getDefaultInstance(clazz).getDescriptorForType();
    Payload payload = new Payload(descriptor);
    PAYLOADS.putIfAbsent(type, payload);
    return payload;
  }

  private static ChildTable children(Descriptor descriptor) {
    ChildTable table = CHILDREN.get(descriptor);
    if (table == null) {
      table = ChildTable.of(descriptor);
      ChildTable previous = CHILDREN.putIfAbsent(descriptor, table);
      if (previous != null) {
        table = previous;
      }
    }
    return table;
  }

  private static final class ChildTable {

    private final int[] numbers;
    private final Descriptor[] types;

    private ChildTable(int[] numbers, Descriptor[] types) {
      this.numbers = numbers;
      this.types = types;
    }

    static ChildTable of(Descriptor descriptor) {
      List<FieldDescriptor> fields = descriptor.getFields();
      int size = 0;
      for (FieldDescriptor field : fields) {
        if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
          size++;
        }
      }
      FieldDescriptor[] messageFields = new FieldDescriptor[size];
      int index = 0;
      for (FieldDescriptor field : fields) {
        if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
          messageFields[index++] = field;
        }
      }
      Arrays.sort(messageFields, (a, b) -> Integer.compare(a.getNumber(), b.getNumber()));
      int[] numbers = new int[size];
      Descriptor[] types = new Descriptor[size];
      for (int i = 0; i < size; i++) {
        numbers[i] = messageFields[i].getNumber();
        types[i] = messageFields[i].getMessageType();
      }
      return new ChildTable(numbers, types);
    }

    Descriptor get(int fieldNumber) {
      int i = Arrays.binarySearch(numbers, fieldNumber);
      return i >= 0 ? types[i] : null;
    }
  }

  private static final class Payload {

    private final Descriptor descriptor;
    private final byte[] fullName;

    private Payload(Descriptor descriptor) {
      this.descriptor = descriptor;
      this.fullName = descriptor.getFullName().getBytes(StandardCharsets.UTF_8);
    }
  }

  private static final class Effective {

    private final Descriptor payload;
    private final int valueTagEnd;

    private Effective(Descriptor payload, int valueTagEnd) {
      this.payload = payload;
      this.valueTagEnd = valueTagEnd;
    }
  }

  private static final class Budget {

    private final long limit;
    private long used;

    private Budget(long limit) {
      this.limit = limit;
    }

    void consume() {
      if (used >= limit) {
        throw new TransactionAdmissionException(Reason.OVER_BUDGET, null);
      }
      used++;
    }

    long remaining() {
      return limit - used;
    }
  }
}
