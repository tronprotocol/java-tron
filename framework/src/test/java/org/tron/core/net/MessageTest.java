package org.tron.core.net;

import com.google.protobuf.ByteString;
import com.google.protobuf.UnknownFieldSet;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.common.overlay.message.Message;
import org.tron.core.exception.P2pException;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.adv.FetchInvDataMessage;
import org.tron.core.net.message.adv.InventoryMessage;
import org.tron.core.net.message.adv.TransactionsMessage;
import org.tron.core.net.message.base.DisconnectMessage;
import org.tron.core.net.message.handshake.HelloMessage;
import org.tron.core.net.message.sync.BlockInventoryMessage;
import org.tron.core.net.message.sync.ChainInventoryMessage;
import org.tron.core.net.message.sync.SyncBlockChainMessage;
import org.tron.core.net.service.statistics.MessageStatistics;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Inventory.InventoryType;
import org.tron.protos.Protocol.ReasonCode;

public class MessageTest {

  // A field number not used by any message under test, so it always parses as an unknown field.
  private static final int UNKNOWN_FIELD_NUMBER = 1000;
  private static final int PADDING_SIZE = 8192;

  private DisconnectMessage disconnectMessage;

  /**
   * Adds unknown padding to transient control messages to verify that decoding drops it.
   */
  private static byte[] withUnknown(com.google.protobuf.Message msg) {
    UnknownFieldSet unknown = UnknownFieldSet.newBuilder()
        .addField(UNKNOWN_FIELD_NUMBER, UnknownFieldSet.Field.newBuilder()
            .addLengthDelimited(ByteString.copyFrom(new byte[PADDING_SIZE]))
            .build())
        .build();
    byte[] padded = msg.toBuilder().setUnknownFields(unknown).build().toByteArray();
    // Sanity: the padded bytes really are larger than the clean message.
    Assert.assertTrue(padded.length > msg.toByteArray().length + PADDING_SIZE - 16);
    return padded;
  }

  @Test
  public void inventoryDiscardsUnknownFields() throws Exception {
    Protocol.Inventory inv = Protocol.Inventory.newBuilder()
        .setType(InventoryType.TRX)
        .addIds(ByteString.copyFrom(new byte[32]))
        .build();
    InventoryMessage msg = new InventoryMessage(withUnknown(inv));
    Assert.assertTrue(msg.getInventory().getUnknownFields().asMap().isEmpty());
    Assert.assertEquals(inv, msg.getInventory());
    Assert.assertEquals(1, msg.getHashList().size());
  }

  @Test
  public void fetchInvDataDiscardsUnknownFields() throws Exception {
    Protocol.Inventory inv = Protocol.Inventory.newBuilder()
        .setType(InventoryType.BLOCK)
        .addIds(ByteString.copyFrom(new byte[32]))
        .build();
    FetchInvDataMessage msg = new FetchInvDataMessage(withUnknown(inv));
    Assert.assertTrue(msg.getInventory().getUnknownFields().asMap().isEmpty());
    Assert.assertEquals(inv, msg.getInventory());
  }

  @Test
  public void helloMessageDiscardsUnknownFields() throws Exception {
    Protocol.HelloMessage.BlockId blockId = Protocol.HelloMessage.BlockId.newBuilder()
        .setHash(ByteString.copyFrom(new byte[32]))
        .build();
    Protocol.HelloMessage hello = Protocol.HelloMessage.newBuilder()
        .setVersion(1)
        .setTimestamp(123456789L)
        .setGenesisBlockId(blockId)
        .setSolidBlockId(blockId)
        .setHeadBlockId(blockId)
        .build();
    HelloMessage msg = new HelloMessage(withUnknown(hello));
    Assert.assertTrue(msg.getHelloMessage().getUnknownFields().asMap().isEmpty());
    Assert.assertEquals(1, msg.getVersion());
    Assert.assertEquals(123456789L, msg.getTimestamp());
    assertCleanHelloBytes(hello, msg);
  }

  private static void assertCleanHelloBytes(Protocol.HelloMessage hello, HelloMessage msg) {
    Assert.assertEquals(hello, msg.getHelloMessage());
    byte[] clean = hello.toByteArray();
    Assert.assertArrayEquals(clean, msg.getData());
    byte[] expectedSendBytes = new byte[clean.length + 1];
    expectedSendBytes[0] = MessageTypes.P2P_HELLO.asByte();
    System.arraycopy(clean, 0, expectedSendBytes, 1, clean.length);
    Assert.assertArrayEquals(expectedSendBytes, msg.getSendBytes());
  }

  @Test
  public void chainInventoryDiscardsUnknownFields() throws Exception {
    Protocol.ChainInventory chainInv = Protocol.ChainInventory.newBuilder()
        .addIds(Protocol.ChainInventory.BlockId.newBuilder()
            .setHash(ByteString.copyFrom(new byte[32])).setNumber(1).build())
        .setRemainNum(0)
        .build();
    ChainInventoryMessage msg = new ChainInventoryMessage(withUnknown(chainInv));
    Protocol.ChainInventory parsed =
        (Protocol.ChainInventory) ReflectionTestUtils.getField(msg, "chainInventory");
    Assert.assertNotNull(parsed);
    Assert.assertTrue(parsed.getUnknownFields().asMap().isEmpty());
    Assert.assertEquals(chainInv, parsed);
    Assert.assertEquals(1, msg.getBlockIds().size());
    Assert.assertEquals(Long.valueOf(0L), msg.getRemainNum());
  }

  @Test
  public void disconnectMessageDiscardsUnknownFields() throws Exception {
    Protocol.DisconnectMessage disconnect = Protocol.DisconnectMessage.newBuilder()
        .setReason(ReasonCode.TOO_MANY_PEERS)
        .build();
    DisconnectMessage msg = new DisconnectMessage(withUnknown(disconnect));
    Protocol.DisconnectMessage parsed =
        (Protocol.DisconnectMessage) ReflectionTestUtils.getField(msg, "disconnectMessage");
    Assert.assertNotNull(parsed);
    Assert.assertTrue(parsed.getUnknownFields().asMap().isEmpty());
    Assert.assertEquals(disconnect, parsed);
    Assert.assertEquals(ReasonCode.TOO_MANY_PEERS, msg.getReason());
  }

  @Test
  public void blockInventoryDiscardsUnknownFields() throws Exception {
    Protocol.BlockInventory inventory = Protocol.BlockInventory.newBuilder()
        .addIds(Protocol.BlockInventory.BlockId.newBuilder()
            .setHash(ByteString.copyFrom(new byte[32])).setNumber(1).build())
        .setType(Protocol.BlockInventory.Type.ADVTISE)
        .build();
    BlockInventoryMessage msg = new BlockInventoryMessage(withUnknown(inventory));
    Protocol.BlockInventory parsed =
        (Protocol.BlockInventory) ReflectionTestUtils.getField(msg, "blockInventory");
    Assert.assertNotNull(parsed);
    Assert.assertTrue(parsed.getUnknownFields().asMap().isEmpty());
    Assert.assertEquals(inventory, parsed);
    Assert.assertEquals(1, msg.getBlockIds().size());
  }

  @Test
  public void syncBlockChainDiscardsUnknownFields() throws Exception {
    Protocol.BlockInventory inventory = Protocol.BlockInventory.newBuilder()
        .addIds(Protocol.BlockInventory.BlockId.newBuilder()
            .setHash(ByteString.copyFrom(new byte[32])).setNumber(1).build())
        .setType(Protocol.BlockInventory.Type.SYNC)
        .build();
    SyncBlockChainMessage msg = new SyncBlockChainMessage(withUnknown(inventory));
    Protocol.BlockInventory parsed =
        (Protocol.BlockInventory) ReflectionTestUtils.getField(msg, "blockInventory");
    Assert.assertNotNull(parsed);
    Assert.assertTrue(parsed.getUnknownFields().asMap().isEmpty());
    Assert.assertEquals(inventory, parsed);
    Assert.assertEquals(1, msg.getBlockIds().size());
  }

  @Test
  public void test1() {
    DisconnectMessageTest disconnectMessageTest = new DisconnectMessageTest();
    try {
      disconnectMessage = new DisconnectMessage(disconnectMessageTest.toByteArray());
    } catch (Exception e) {
      Assert.assertTrue(e instanceof P2pException);
    }
  }

  @Test
  public void testMessageStatistics() {
    MessageStatistics messageStatistics = new MessageStatistics();
    Message message1 = new Message(MessageTypes.P2P_HELLO.asByte(), null) {
      @Override
      public Class<?> getAnswerMessage() {
        return null;
      }
    };
    Message message2 = new Message(MessageTypes.P2P_PING.asByte(), null) {
      @Override
      public Class<?> getAnswerMessage() {
        return null;
      }
    };
    Message message3 = new Message(MessageTypes.P2P_PONG.asByte(), null) {
      @Override
      public Class<?> getAnswerMessage() {
        return null;
      }
    };
    Message message4 = new Message(MessageTypes.P2P_DISCONNECT.asByte(), null) {
      @Override
      public Class<?> getAnswerMessage() {
        return null;
      }
    };
    Message message5 = new Message(MessageTypes.SYNC_BLOCK_CHAIN.asByte(), null) {
      @Override
      public Class<?> getAnswerMessage() {
        return null;
      }
    };
    Message message6 = new Message(MessageTypes.BLOCK_CHAIN_INVENTORY.asByte(), null) {
      @Override
      public Class<?> getAnswerMessage() {
        return null;
      }
    };
    Message message7 = new Message(MessageTypes.TRX.asByte(), null) {
      @Override
      public Class<?> getAnswerMessage() {
        return null;
      }
    };
    Message message8 = new Message(MessageTypes.BLOCK.asByte(), null) {
      @Override
      public Class<?> getAnswerMessage() {
        return null;
      }
    };
    InventoryMessage message9 = new InventoryMessage(new ArrayList<>(), InventoryType.TRX);
    FetchInvDataMessage message10 = new FetchInvDataMessage(new ArrayList<>(), InventoryType.TRX);
    TransactionsMessage message11 = new TransactionsMessage(new ArrayList<>());

    messageStatistics.addTcpInMessage(message1);
    messageStatistics.addTcpOutMessage(message1);
    messageStatistics.addTcpInMessage(message2);
    messageStatistics.addTcpOutMessage(message2);
    messageStatistics.addTcpInMessage(message3);
    messageStatistics.addTcpOutMessage(message3);
    messageStatistics.addTcpInMessage(message4);
    messageStatistics.addTcpOutMessage(message4);
    messageStatistics.addTcpInMessage(message5);
    messageStatistics.addTcpOutMessage(message5);
    try {
      Thread.sleep(2000);// so that gap > 1 in MessageCount.update method
    } catch (InterruptedException e) {
      //ignore
    }
    messageStatistics.addTcpInMessage(message6);
    messageStatistics.addTcpOutMessage(message6);
    messageStatistics.addTcpInMessage(message7);
    messageStatistics.addTcpOutMessage(message7);
    messageStatistics.addTcpInMessage(message8);
    messageStatistics.addTcpOutMessage(message8);
    messageStatistics.addTcpInMessage(message9);
    messageStatistics.addTcpOutMessage(message9);
    messageStatistics.addTcpInMessage(message10);
    messageStatistics.addTcpOutMessage(message10);
    messageStatistics.addTcpInMessage(message11);
    messageStatistics.addTcpOutMessage(message11);

    Assert.assertEquals(11, messageStatistics.tronInMessage.getTotalCount());
    Assert.assertEquals(11, messageStatistics.tronOutMessage.getTotalCount());
  }

}
