package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol.Transaction;

@Slf4j
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

  public TransactionCapsule getTransactionCapsule() {
    return new TransactionCapsule(getTransaction());
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
