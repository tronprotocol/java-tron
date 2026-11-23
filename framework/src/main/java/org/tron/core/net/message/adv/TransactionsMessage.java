package org.tron.core.net.message.adv;

import static org.tron.core.config.Parameter.NetConstants.MAX_TRX_FETCH_PER_PEER;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;
import java.io.IOException;
import java.util.List;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TronMessage;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;

public class TransactionsMessage extends TronMessage {

  private static final int TRANSACTIONS_TAG = (Protocol.Transactions.TRANSACTIONS_FIELD_NUMBER << 3)
      | WireFormat.WIRETYPE_LENGTH_DELIMITED;

  private Protocol.Transactions transactions;

  public TransactionsMessage(List<Transaction> trxs) {
    Protocol.Transactions.Builder builder = Protocol.Transactions.newBuilder();
    trxs.forEach(trx -> builder.addTransactions(trx));
    this.transactions = builder.build();
    this.type = MessageTypes.TRXS.asByte();
    this.data = this.transactions.toByteArray();
  }

  public TransactionsMessage(byte[] data) throws Exception {
    super(data);
    precheck(data);
    this.type = MessageTypes.TRXS.asByte();
    this.transactions = Protocol.Transactions.parseFrom(getCodedInputStream(data));
    if (isFilter()) {
      compareBytes(data, transactions.toByteArray());
      TransactionCapsule.validContractProto(transactions.getTransactionsList());
    }
  }

  private void precheck(byte[] data) throws IOException, P2pException {
    CodedInputStream input = CodedInputStream.newInstance(data);
    int transactionCount = 0;
    int tag;
    while ((tag = input.readTag()) != 0) {
      // Our TRX fetch requests contain at most this many entries; replies may split the batch.
      if (tag == TRANSACTIONS_TAG && ++transactionCount > MAX_TRX_FETCH_PER_PEER) {
        throw new P2pException(TypeEnum.BAD_MESSAGE, "transactions count over limit");
      }
      // Count only top-level entries without parsing transaction contents.
      if (!input.skipField(tag)) {
        break;
      }
    }
    input.checkLastTagWas(0);
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
