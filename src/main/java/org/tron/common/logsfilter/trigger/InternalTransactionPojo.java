package org.tron.common.logsfilter.trigger;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class InternalTransactionPojo {

  @Getter
  @Setter
  private String hash;

  @Getter
  @Setter
  /* the amount of trx to transfer (calculated as sun) */
  private long callValue;

  @Getter
  @Setter
  private Map<String, Long> tokenInfo = new HashMap<>();

  /* the address of the destination account (for message)
   * In creation transaction the receive address is - 0 */
  @Getter
  @Setter
  private String transferTo_address;

  /* An unlimited size byte array specifying
   * input [data] of the message call or
   * Initialization code for a new contract */
  @Getter
  @Setter
  private String data;

  /*  Message sender address */
  @Getter
  @Setter
  private String caller_address;

  @Getter
  @Setter
  private boolean rejected;

  @Getter
  @Setter
  private String note;
}
