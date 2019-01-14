package org.tron.common.logsfilter.trigger;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

public class InternalTransactionPojo {
  private String transactionId;
  private byte[] hash;
  private byte[] parentHash;
  /* the amount of trx to transfer (calculated as sun) */
  private long value;
  private long tokenValue;
  private Map<String, Long> tokenInfo =new HashMap<>();

  /* the address of the destination account (for message)
   * In creation transaction the receive address is - 0 */
  private byte[] receiveAddress;

  /* An unlimited size byte array specifying
   * input [data] of the message call or
   * Initialization code for a new contract */
  private byte[] data;
  private long nonce;
  private byte[] transferToAddress;

  /*  Message sender address */
  private byte[] sendAddress;
  @Getter
  private int deep;
  @Getter
  private int index;
  private boolean rejected;
  private String note;
  private byte[] protoEncoded;
}
