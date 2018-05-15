package org.tron.core.net.message;

import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;

public class TransactionMessage extends TronMessage {

  private Transaction trx;

  public TransactionMessage(byte[] data) throws Exception {
    this.type = MessageTypes.TRX.asByte();
    this.data = data;
    this.trx = Protocol.Transaction.parseFrom(data);
  }

  public TransactionMessage(Transaction trx) {
    this.trx = trx;
    this.type = MessageTypes.TRX.asByte();
    this.data = trx.toByteArray();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  public Transaction getTransaction() {
    return trx;
  }

  public TransactionCapsule getTransactionCapsule() {
    return new TransactionCapsule(getTransaction());
  }
  
}
