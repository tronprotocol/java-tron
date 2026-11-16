package org.tron.core.net.message.adv;

import com.google.protobuf.CodedInputStream;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TronMessage;
import org.tron.protos.Protocol.Transaction;

public class TransactionMessage extends TronMessage {

  private TransactionCapsule transactionCapsule;

  public TransactionMessage(byte[] data) throws Exception {
    super(data);
    CodedInputStream input = getCodedInputStream(data);
    this.transactionCapsule = new TransactionCapsule(input);
    input.checkLastTagWas(0);
    this.type = MessageTypes.TRX.asByte();
    if (Message.isFilter()) {
      compareBytes(data, transactionCapsule.getInstance().toByteArray());
      transactionCapsule
          .validContractProto(transactionCapsule.getInstance().getRawData().getContract(0));
    }
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
