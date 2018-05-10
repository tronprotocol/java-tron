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

  public TransactionsMessage(byte[] data) throws Exception{
    try {
      this.type = MessageTypes.TRXS.asByte();
      this.data = data;
      this.transactions = Protocol.Transactions.parseFrom(data);
    }catch (Exception e){
      throw new P2pException(P2pException.TypeEnum.PARSE_MESSAGE_FAILED);
    }
  }

  public Protocol.Transactions getTransactions() {
    return transactions;
  }

  @Override
  public byte[] getData() {
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
}
