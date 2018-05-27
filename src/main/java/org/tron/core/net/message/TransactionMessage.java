package org.tron.core.net.message;

import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;

public class TransactionMessage extends TronMessage {

  private Transaction trx;
  private TransactionCapsule transactionCapsule;

  public TransactionMessage(byte[] data) throws Exception {
    this.type = MessageTypes.TRX.asByte();
    this.data = data;
    this.trx = Protocol.Transaction.parseFrom(data);
    this.transactionCapsule = new TransactionCapsule(this.trx);
  }

  public TransactionMessage(Transaction trx) {
    this.trx = trx;
    this.transactionCapsule = new TransactionCapsule(this.trx);
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

  public Transaction getTransaction() {
    return trx;
  }

  public TransactionCapsule getTransactionCapsule() {
    return this.transactionCapsule;
  }

}
