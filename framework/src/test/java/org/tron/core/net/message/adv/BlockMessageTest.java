package org.tron.core.net.message.adv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.WireFormat;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.tron.common.overlay.message.Message;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TronMessageFactory;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.Transaction;

public class BlockMessageTest {

  private static final int MAX_TRANSACTION_COUNT =
      BlockMessage.MAX_BLOCK_SIZE / Constant.MIN_TRANSACTION_SIZE;

  private static Block sampleBlock() {
    return Block.newBuilder().setBlockHeader(BlockHeader.newBuilder()
        .setRawData(BlockHeader.raw.newBuilder().setNumber(100).setTimestamp(123456789L))
        .setWitnessSignature(ByteString.copyFrom(new byte[] {0x0c})))
        .build();
  }

  @Test
  public void trailingEndGroupIsRejectedWithEitherFilterSetting() {
    byte[] canonical = sampleBlock().toByteArray();
    for (boolean filter : new boolean[] {false, true}) {
      try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS)) {
        messages.when(Message::isFilter).thenReturn(filter);
        // Also reject an END_GROUP followed by more bytes, not just a final 0x0c byte.
        for (byte[] suffix : new byte[][] {{0x0c}, {0x0c, 0x08, 0x01}}) {
          byte[] malformed = Arrays.copyOf(canonical, canonical.length + suffix.length);
          System.arraycopy(suffix, 0, malformed, canonical.length, suffix.length);
          assertThrows(InvalidProtocolBufferException.class, () -> new BlockMessage(malformed));
        }
      }
    }
  }

  @Test
  public void endGroupByteInsideABytesFieldIsAccepted() throws Exception {
    Block block = sampleBlock();
    byte[] data = block.toByteArray();
    assertEquals(0x0c, data[data.length - 1]);
    for (boolean filter : new boolean[] {false, true}) {
      try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS)) {
        messages.when(Message::isFilter).thenReturn(filter);
        assertEquals(block, new BlockMessage(data).getBlockCapsule().getInstance());
      }
    }
  }

  @Test
  public void rawSizeBoundaryIsAcceptedWithEitherFilterSetting() throws Exception {
    for (int size : new int[] {BlockMessage.MAX_BLOCK_SIZE - 1, BlockMessage.MAX_BLOCK_SIZE}) {
      // The two tags and two three-byte lengths add eight bytes at these sizes.
      Block block = Block.newBuilder().setBlockHeader(BlockHeader.newBuilder()
          .setWitnessSignature(ByteString.copyFrom(new byte[size - 8]))).build();
      byte[] data = block.toByteArray();
      assertEquals(size, data.length);
      for (boolean filter : new boolean[] {false, true}) {
        try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS)) {
          messages.when(Message::isFilter).thenReturn(filter);
          assertEquals(block, new BlockMessage(data).getBlockCapsule().getInstance());
        }
      }
    }
  }

  @Test
  public void oversizedRawPayloadIsRejectedBeforeCapsuleConstruction() {
    byte[] data = new byte[BlockMessage.MAX_BLOCK_SIZE + 1];
    for (boolean filter : new boolean[] {false, true}) {
      try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS);
          MockedConstruction<BlockCapsule> capsules = mockConstruction(BlockCapsule.class)) {
        messages.when(Message::isFilter).thenReturn(filter);
        P2pException exception = assertThrows(P2pException.class,
            () -> TronMessageFactory.create(withType(data)));
        assertEquals(TypeEnum.BAD_MESSAGE, exception.getType());
        assertEquals("block size over limit", exception.getMessage());
        assertTrue(capsules.constructed().isEmpty());
      }
    }
  }

  @Test
  public void oversizedNoncanonicalPayloadIsRejectedDespiteSmallParsedSize() throws Exception {
    byte[] data = new byte[BlockMessage.MAX_BLOCK_SIZE + 2];
    for (int i = 0; i < data.length; i += 2) {
      // Repeated empty occurrences of the singular block_header merge into one header.
      data[i] = 0x12;
    }
    assertEquals(2, Block.parseFrom(data).getSerializedSize());
    try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS)) {
      messages.when(Message::isFilter).thenReturn(false);
      P2pException exception = assertThrows(P2pException.class, () -> new BlockMessage(data));
      assertEquals(TypeEnum.BAD_MESSAGE, exception.getType());
      assertEquals("block size over limit", exception.getMessage());
    }
  }

  @Test
  public void transactionCountBoundaryIsAccepted() throws Exception {
    try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS)) {
      messages.when(Message::isFilter).thenReturn(false);
      for (int count : new int[] {0, MAX_TRANSACTION_COUNT - 1, MAX_TRANSACTION_COUNT}) {
        BlockMessage message = new BlockMessage(emptyTransactions(count));
        assertEquals(count, message.getBlockCapsule().getInstance().getTransactionsCount());
        assertEquals(count, message.getBlockCapsule().getTransactions().size());
      }
    }
  }

  @Test
  public void excessiveTransactionsAreRejectedBeforeCapsuleConstruction() {
    byte[] data = emptyTransactions(MAX_TRANSACTION_COUNT + 1);
    for (boolean filter : new boolean[] {false, true}) {
      try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS);
          MockedConstruction<BlockCapsule> capsules = mockConstruction(BlockCapsule.class)) {
        messages.when(Message::isFilter).thenReturn(filter);
        P2pException exception = assertThrows(P2pException.class,
            () -> TronMessageFactory.create(withType(data)));
        assertEquals(TypeEnum.BAD_MESSAGE, exception.getType());
        assertEquals("block transaction count over limit", exception.getMessage());
        assertTrue(capsules.constructed().isEmpty());
      }
    }
  }

  @Test
  public void transactionTagsInsideBytesAreNotCounted() throws Exception {
    Transaction transaction = Transaction.newBuilder()
        .addSignature(ByteString.copyFrom(emptyTransactions(MAX_TRANSACTION_COUNT + 1)))
        .build();
    Block block = sampleBlock().toBuilder().addTransactions(transaction).build();
    try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS)) {
      messages.when(Message::isFilter).thenReturn(false);
      assertEquals(block, new BlockMessage(block.toByteArray()).getBlockCapsule().getInstance());
    }
  }

  @Test
  public void transactionTagsInsideUnknownGroupsAreNotCounted() throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    CodedOutputStream output = CodedOutputStream.newInstance(bytes);
    output.writeTag(1000, WireFormat.WIRETYPE_START_GROUP);
    output.writeRawBytes(emptyTransactions(MAX_TRANSACTION_COUNT + 1));
    output.writeTag(1000, WireFormat.WIRETYPE_END_GROUP);
    output.flush();
    try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS)) {
      messages.when(Message::isFilter).thenReturn(false);
      Block block = new BlockMessage(bytes.toByteArray()).getBlockCapsule().getInstance();
      assertEquals(0, block.getTransactionsCount());
      assertEquals(MAX_TRANSACTION_COUNT + 1,
          block.getUnknownFields().getField(1000).getGroupList().get(0)
              .getField(Block.TRANSACTIONS_FIELD_NUMBER).getLengthDelimitedList().size());
    }
  }

  @Test
  public void transactionFieldWithWrongWireTypeIsNotCounted() throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    CodedOutputStream output = CodedOutputStream.newInstance(bytes);
    for (int i = 0; i <= MAX_TRANSACTION_COUNT; i++) {
      output.writeUInt32(Block.TRANSACTIONS_FIELD_NUMBER, 0);
    }
    output.flush();
    try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS)) {
      messages.when(Message::isFilter).thenReturn(false);
      Block block = new BlockMessage(bytes.toByteArray()).getBlockCapsule().getInstance();
      assertEquals(0, block.getTransactionsCount());
      assertEquals(MAX_TRANSACTION_COUNT + 1,
          block.getUnknownFields().getField(Block.TRANSACTIONS_FIELD_NUMBER)
              .getVarintList().size());
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

  private static byte[] emptyTransactions(int count) {
    byte[] data = new byte[count * 2];
    for (int i = 0; i < data.length; i += 2) {
      data[i] = 0x0a;
    }
    return data;
  }

  private static byte[] withType(byte[] data) {
    byte[] message = new byte[data.length + 1];
    message[0] = MessageTypes.BLOCK.asByte();
    System.arraycopy(data, 0, message, 1, data.length);
    return message;
  }

}
