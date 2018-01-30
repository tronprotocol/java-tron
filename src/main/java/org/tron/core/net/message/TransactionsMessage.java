package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import org.tron.protos.Protocal.Items;
import org.tron.protos.Protocal.Transaction;

public class TransactionsMessage extends Message {

  private List<Transaction> trxs = new ArrayList<Transaction>();

  public TransactionsMessage(List<Transaction> trxs) {
    this.trxs = trxs;
    unpacked = true;
  }

  public TransactionsMessage(byte[] packed) {
    super(packed);
  }

  public TransactionsMessage() {
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
  public MessageTypes getType() {
    return MessageTypes.TRXS;
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
