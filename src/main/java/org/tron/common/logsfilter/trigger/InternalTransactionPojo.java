package org.tron.common.logsfilter.trigger;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.runtime.vm.program.InternalTransaction;

public class InternalTransactionPojo {

  @Getter
  private String hash;
  @Getter
  /* the amount of trx to transfer (calculated as sun) */
  private long callValue;
  @Getter
  private Map<String, Long> tokenInfo = new HashMap<>();

  /* the address of the destination account (for message)
   * In creation transaction the receive address is - 0 */
  @Getter
  private String transferTo_address;

  /* An unlimited size byte array specifying
   * input [data] of the message call or
   * Initialization code for a new contract */
  @Getter
  private String data;

  /*  Message sender address */
  @Getter
  private String caller_address;
  @Getter
  private boolean rejected;
  @Getter
  private String note;

  public InternalTransactionPojo(InternalTransaction internalTransaction) {
    this.hash = Hex.toHexString(internalTransaction.getHash());
    this.callValue = internalTransaction.getValue();
    this.tokenInfo = internalTransaction.getTokenInfo();

    this.caller_address = Hex.toHexString(internalTransaction.getSender());
    this.transferTo_address = Hex.toHexString(internalTransaction.getReceiveAddress());
    this.data = Hex.toHexString(internalTransaction.getData());
    this.rejected = internalTransaction.isRejected();
    this.note = internalTransaction.getNote();
  }

  @Override
  public String toString() {
    return super.toString();
  }
}
