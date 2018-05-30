package org.tron.core.net.message;

import java.util.List;

import org.tron.core.exception.P2pException;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;

public class TransactionsMessage extends TronMessage {

  private Protocol.Transactions transactions;

  public TransactionsMessage(List<Transaction> trxs) {
    Protocol.Transactions.Builder builder = Protocol.Transactions.newBuilder();
    trxs.forEach(trx -> builder.addTransactions(trx));
    this.transactions = builder.build();
    this.type = MessageTypes.TRXS.asByte();
    this.data = this.transactions.toByteArray();
  }

  public TransactionsMessage(byte[] data) throws Exception {
    this.type = MessageTypes.TRXS.asByte();
    this.data = data;
    this.transactions = Protocol.Transactions.parseFrom(data);
  }

  public Protocol.Transactions getTransactions() {
    return transactions;
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString()).append("trx size: ")
        .append(this.transactions.getTransactionsList().size()).toString();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

}
