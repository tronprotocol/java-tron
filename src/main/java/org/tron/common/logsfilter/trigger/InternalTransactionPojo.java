package org.tron.common.logsfilter.trigger;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.runtime.vm.program.InternalTransaction;

public class InternalTransactionPojo {

  @Getter
  private String transactionId;
  @Getter
  private byte[] hash;
  @Getter
  private byte[] parentHash;
  @Getter
  /* the amount of trx to transfer (calculated as sun) */
  private long value;
  @Getter
  private Map<String, Long> tokenInfo = new HashMap<>();

  /* the address of the destination account (for message)
   * In creation transaction the receive address is - 0 */
  @Getter
  private byte[] receiveAddress;

  /* An unlimited size byte array specifying
   * input [data] of the message call or
   * Initialization code for a new contract */
  @Getter
  private byte[] data;
  @Getter
  private long nonce;
  @Getter
  private byte[] transferToAddress;

  /*  Message sender address */
  @Getter
  private byte[] sendAddress;
  @Getter
  private int deep;
  @Getter
  private int index;
  @Getter
  private boolean rejected;
  @Getter
  private String note;
  @Getter
  private byte[] protoEncoded;

  public InternalTransactionPojo(InternalTransaction internalTransaction) {
    this.transactionId = Hex.toHexString(internalTransaction.getHash());
    this.hash = internalTransaction.getHash();
    this.value = internalTransaction.getValue();
    this.tokenInfo = internalTransaction.getTokenInfo();
    this.sendAddress = internalTransaction.getSender();
    this.receiveAddress = internalTransaction.getReceiveAddress();
    this.parentHash = internalTransaction.getParentHash();
    this.data = internalTransaction.getData();
    this.nonce = internalTransaction.getNonce();
    this.deep = internalTransaction.getDeep();
    this.index = internalTransaction.getIndex();
    this.rejected = internalTransaction.isRejected();
    this.note = internalTransaction.getNote();
    this.protoEncoded = internalTransaction.getEncoded();
  }

  @Override
  public String toString() {
    return super.toString();
  }
}
