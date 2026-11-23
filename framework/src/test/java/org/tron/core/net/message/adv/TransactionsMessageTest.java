package org.tron.core.net.message.adv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.tron.core.config.Parameter.NetConstants.MAX_TRX_FETCH_PER_PEER;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.WireFormat;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.common.overlay.message.Message;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TronMessageFactory;
import org.tron.p2p.base.Parameter;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transactions;

public class TransactionsMessageTest {

  @Test
  public void transactionCountBoundaryIsAccepted() throws Exception {
    try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS)) {
      messages.when(Message::isFilter).thenReturn(false);
      for (int count : new int[] {0, 1, MAX_TRX_FETCH_PER_PEER - 1, MAX_TRX_FETCH_PER_PEER}) {
        TransactionsMessage message = new TransactionsMessage(emptyTransactions(count));
        assertEquals(count, message.getTransactions().getTransactionsCount());
      }
    }
  }

  @Test
  public void excessiveTransactionsAreRejectedBeforeParsingWithEitherFilterSetting() {
    byte[] data = withType(emptyTransactions(MAX_TRX_FETCH_PER_PEER + 1));
    for (boolean filter : new boolean[] {false, true}) {
      try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS);
          MockedStatic<Transactions> transactions = mockStatic(Transactions.class,
              CALLS_REAL_METHODS)) {
        messages.when(Message::isFilter).thenReturn(filter);
        transactions.when(() -> Transactions.parseFrom(any(CodedInputStream.class)))
            .thenThrow(new AssertionError("protobuf parsing must not be reached"));
        P2pException exception = assertThrows(P2pException.class,
            () -> TronMessageFactory.create(data));
        assertEquals(TypeEnum.BAD_MESSAGE, exception.getType());
        assertEquals("transactions count over limit", exception.getMessage());
      }
    }
  }

  @Test
  public void transactionTagsInsideBytesAreNotCounted() throws Exception {
    Transaction transaction = Transaction.newBuilder()
        .addSignature(ByteString.copyFrom(emptyTransactions(MAX_TRX_FETCH_PER_PEER + 1)))
        .build();
    Transactions expected = Transactions.newBuilder().addTransactions(transaction).build();
    try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS)) {
      messages.when(Message::isFilter).thenReturn(false);
      assertEquals(expected, new TransactionsMessage(expected.toByteArray()).getTransactions());
    }
  }

  @Test
  public void transactionTagsInsideUnknownGroupsAreNotCounted() throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    CodedOutputStream output = CodedOutputStream.newInstance(bytes);
    output.writeTag(1000, WireFormat.WIRETYPE_START_GROUP);
    output.writeRawBytes(emptyTransactions(MAX_TRX_FETCH_PER_PEER + 1));
    output.writeTag(1000, WireFormat.WIRETYPE_END_GROUP);
    output.flush();
    try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS)) {
      messages.when(Message::isFilter).thenReturn(false);
      Transactions parsed = new TransactionsMessage(bytes.toByteArray()).getTransactions();
      assertEquals(0, parsed.getTransactionsCount());
      assertEquals(MAX_TRX_FETCH_PER_PEER + 1,
          parsed.getUnknownFields().getField(1000).getGroupList().get(0)
              .getField(Transactions.TRANSACTIONS_FIELD_NUMBER).getLengthDelimitedList().size());
    }
  }

  @Test
  public void transactionFieldWithWrongWireTypeIsNotCounted() throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    CodedOutputStream output = CodedOutputStream.newInstance(bytes);
    for (int i = 0; i <= MAX_TRX_FETCH_PER_PEER; i++) {
      output.writeUInt32(Transactions.TRANSACTIONS_FIELD_NUMBER, 0);
    }
    output.flush();
    try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS)) {
      messages.when(Message::isFilter).thenReturn(false);
      Transactions parsed = new TransactionsMessage(bytes.toByteArray()).getTransactions();
      assertEquals(0, parsed.getTransactionsCount());
      assertEquals(MAX_TRX_FETCH_PER_PEER + 1,
          parsed.getUnknownFields().getField(Transactions.TRANSACTIONS_FIELD_NUMBER)
              .getVarintList().size());
    }
  }

  @Test
  public void largeSingleTransactionDoesNotGetAnAdditionalSizeLimit() throws Exception {
    Transaction transaction = Transaction.newBuilder()
        .addSignature(ByteString.copyFrom(new byte[2_100_000])).build();
    Transactions expected = Transactions.newBuilder().addTransactions(transaction).build();
    byte[] data = expected.toByteArray();
    assertTrue(data.length > BlockMessage.MAX_BLOCK_SIZE);
    assertTrue(data.length + 1 < Parameter.MAX_MESSAGE_LENGTH);
    try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS)) {
      messages.when(Message::isFilter).thenReturn(false);
      assertEquals(expected, new TransactionsMessage(data).getTransactions());
    }
  }

  @Test
  public void trailingEndGroupIsRejectedWithEitherFilterSetting() {
    byte[] canonical = emptyTransactions(1);
    for (boolean filter : new boolean[] {false, true}) {
      try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS)) {
        messages.when(Message::isFilter).thenReturn(filter);
        for (byte[] suffix : new byte[][] {{0x0c}, {0x14}, {0x0c, 0x08, 0x01}}) {
          byte[] malformed = Arrays.copyOf(canonical, canonical.length + suffix.length);
          System.arraycopy(suffix, 0, malformed, canonical.length, suffix.length);
          assertThrows(InvalidProtocolBufferException.class,
              () -> new TransactionsMessage(malformed));
        }
      }
    }
  }

  @Test
  public void malformedLengthsStillReportParseFailure() {
    for (byte[] data : new byte[][] {{0x0a, 0x01}, {0x0a, (byte) 0x80},
        {0x0a, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x0f}}) {
      P2pException exception = assertThrows(P2pException.class,
          () -> TronMessageFactory.create(withType(data)));
      assertEquals(TypeEnum.PARSE_MESSAGE_FAILED, exception.getType());
    }
  }

  @Test
  public void outgoingConstructorRetainsItsExistingBehavior() {
    TransactionsMessage message = new TransactionsMessage(
        Collections.nCopies(MAX_TRX_FETCH_PER_PEER + 1, Transaction.getDefaultInstance()));
    assertEquals(MAX_TRX_FETCH_PER_PEER + 1, message.getTransactions().getTransactionsCount());
  }

  private static byte[] emptyTransactions(int count) {
    byte[] data = new byte[count * 2];
    for (int i = 0; i < data.length; i += 2) {
      data[i] = 0x0a;
    }
    return data;
  }

  private static byte[] withType(byte[] data) {
    byte[] message = new byte[data.length + 1];
    message[0] = MessageTypes.TRXS.asByte();
    System.arraycopy(data, 0, message, 1, data.length);
    return message;
  }
}
