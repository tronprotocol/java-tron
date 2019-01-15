package org.tron.common.logsfilter.trigger;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.runtime.vm.program.InternalTransaction;
import org.tron.core.Wallet;

public class InternalTransactionPojo {

  @Getter
  private String hash;
  @Getter
  private String parentHash;
  @Getter
  /* the amount of trx to transfer (calculated as sun) */
  private long value;
  @Getter
  private Map<String, Long> tokenInfo = new HashMap<>();

  /* the address of the destination account (for message)
   * In creation transaction the receive address is - 0 */
  @Getter
  private String receiveAddress;

  /* An unlimited size byte array specifying
   * input [data] of the message call or
   * Initialization code for a new contract */
  @Getter
  private String data;
  @Getter
  private long nonce;
  @Getter
  private String transferToAddress;

  /*  Message sender address */
  @Getter
  private String sendAddress;
  @Getter
  private int deep;
  @Getter
  private int index;
  @Getter
  private boolean rejected;
  @Getter
  private String note;

  public InternalTransactionPojo(InternalTransaction internalTransaction) {
    this.hash = Hex.toHexString(internalTransaction.getHash());
    this.value = internalTransaction.getValue();
    this.tokenInfo = internalTransaction.getTokenInfo();
    this.sendAddress = Wallet.encode58Check(internalTransaction.getSender());
    this.receiveAddress = Wallet.encode58Check(internalTransaction.getReceiveAddress());
    this.parentHash = Hex.toHexString(internalTransaction.getParentHash());
    this.data = Hex.toHexString(internalTransaction.getData());
    this.nonce = internalTransaction.getNonce();
    this.deep = internalTransaction.getDeep();
    this.index = internalTransaction.getIndex();
    this.rejected = internalTransaction.isRejected();
    this.note = internalTransaction.getNote();
  }

  @Override
  public String toString() {
    return super.toString();
  }
}
