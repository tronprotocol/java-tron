package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.protos.Protocol.Items;
import org.tron.protos.Protocol.Transaction;

@Slf4j(topic = "core.net")
public class TransactionsMessage extends TronMessage {

  private List<Transaction> trxs = new ArrayList<Transaction>();

  public TransactionsMessage(List<Transaction> trxs) {
    this.trxs = trxs;
    unpacked = true;
    this.type = MessageTypes.TRXS.asByte();
  }

  public TransactionsMessage(byte[] packed) {
    super(packed);
    this.type = MessageTypes.TRXS.asByte();
  }

  public TransactionsMessage() {
    this.type = MessageTypes.TRXS.asByte();
  }

  @Override
  public byte[] getData() {
    if (data == null) {
      pack();
    }
    return data;
  }

  @Override
  public String toString() {
    return null;
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }

  public List<Transaction> getTransactions() {
    unPack();
    return trxs;
  }

  private void pack() {
    Items.Builder itemsBuilder = Items.newBuilder();
    itemsBuilder.setType(Items.ItemType.TRX);
    itemsBuilder.addAllTransactions(this.trxs);
    this.data = itemsBuilder.build().toByteArray();
  }

  private synchronized void unPack() {
    if (unpacked) {
      return;
    }
    try {
      Items items = Items.parseFrom(data);
      if (items.getType() == Items.ItemType.TRX) {
        trxs = items.getTransactionsList();
      }
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }

    unpacked = true;
  }
}
