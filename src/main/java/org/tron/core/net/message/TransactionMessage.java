package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.protos.Protocal.Transaction;

public class TransactionMessage extends Message {

  private Transaction trx;

  public TransactionMessage(byte[] packed) {
    super(packed);
  }

  public TransactionMessage(Transaction trx) {
    this.trx = trx;
    unpacked = true;
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.TRX;
  }

  @Override
  public String toString() {
    return null;
  }

  @Override
  public byte[] getData() {
    if (data == null) {
      pack();
    }
    return data;
  }

  public Transaction getTransaction() {
    unPack();
    return trx;
  }

  private synchronized void unPack() {
    if (unpacked) {
      return;
    }

    try {
      this.trx = Transaction.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }

    unpacked = true;
  }

  private void pack() {
    this.data = this.trx.toByteArray();
  }

}
