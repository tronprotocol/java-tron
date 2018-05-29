package org.tron.core.net.message;

import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.BadItemException;
import org.tron.protos.Protocol.Transaction;

public class TransactionMessage extends TronMessage {

  private TransactionCapsule transactionCapsule;

  public TransactionMessage(byte[] data) throws BadItemException {
    this.transactionCapsule = new TransactionCapsule(data);
    this.data = data;
    this.type = MessageTypes.TRX.asByte();
  }

  public TransactionMessage(Transaction trx) {
    this.transactionCapsule = new TransactionCapsule(trx);
    this.type = MessageTypes.TRX.asByte();
    this.data = trx.toByteArray();
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString())
        .append("messageId: ").append(super.getMessageId()).toString();
  }

  @Override
  public Sha256Hash getMessageId() {
    return this.transactionCapsule.getTransactionId();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  public TransactionCapsule getTransactionCapsule() {
    return this.transactionCapsule;
  }
}
