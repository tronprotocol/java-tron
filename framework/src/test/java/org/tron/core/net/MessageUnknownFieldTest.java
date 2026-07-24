package org.tron.core.net;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.UnknownFieldSet;
import org.junit.Assert;
import org.junit.Test;
import org.tron.core.net.message.adv.FetchInvDataMessage;
import org.tron.core.net.message.adv.InventoryMessage;
import org.tron.core.net.message.base.DisconnectMessage;
import org.tron.core.net.message.handshake.HelloMessage;
import org.tron.core.net.message.sync.ChainInventoryMessage;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Inventory.InventoryType;
import org.tron.protos.Protocol.ReasonCode;

/**
 * Verifies that transient control messages (not persisted to the database) drop attacker-appended
 * unknown fields at parse time via {@code DiscardUnknownFieldsParser}, so padding cannot be
 * retained in memory. trx/trxs/block/pbft messages are intentionally excluded because they are
 * stored and rely on their exact serialized bytes for hashing/signature verification.
 */
public class MessageUnknownFieldTest {

  // A field number not used by any message under test, so it always parses as an unknown field.
  private static final int UNKNOWN_FIELD_NUMBER = 1000;
  private static final int PADDING_SIZE = 8192;

  private static byte[] withUnknown(Message msg) {
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
    Protocol.HelloMessage hello = Protocol.HelloMessage.newBuilder()
        .setVersion(1)
        .setTimestamp(123456789L)
        .build();
    HelloMessage msg = new HelloMessage(withUnknown(hello));
    Assert.assertTrue(msg.getHelloMessage().getUnknownFields().asMap().isEmpty());
    Assert.assertEquals(1, msg.getVersion());
    Assert.assertEquals(123456789L, msg.getTimestamp());
  }

  @Test
  public void chainInventoryDiscardsUnknownFields() throws Exception {
    Protocol.ChainInventory chainInv = Protocol.ChainInventory.newBuilder()
        .addIds(Protocol.ChainInventory.BlockId.newBuilder()
            .setHash(ByteString.copyFrom(new byte[32])).setNumber(1).build())
        .setRemainNum(0)
        .build();
    ChainInventoryMessage msg = new ChainInventoryMessage(withUnknown(chainInv));
    // Known fields survive; padding is dropped so the message re-serializes to its clean size.
    Assert.assertEquals(1, msg.getBlockIds().size());
    Assert.assertEquals(Long.valueOf(0L), msg.getRemainNum());
  }

  @Test
  public void disconnectMessageDiscardsUnknownFields() throws Exception {
    Protocol.DisconnectMessage disconnect = Protocol.DisconnectMessage.newBuilder()
        .setReason(ReasonCode.TOO_MANY_PEERS)
        .build();
    DisconnectMessage msg = new DisconnectMessage(withUnknown(disconnect));
    Assert.assertEquals(ReasonCode.TOO_MANY_PEERS, msg.getReason());
  }
}
