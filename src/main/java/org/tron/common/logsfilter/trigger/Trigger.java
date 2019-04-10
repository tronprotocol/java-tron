package org.tron.common.logsfilter.trigger;

import lombok.Getter;
import lombok.Setter;

public class Trigger {

  @Getter
  @Setter
  protected long timeStamp;

  @Getter
  @Setter
  private String triggerName;

  public static final int BLOCK_TRIGGER = 0;
  public static final int TRANSACTION_TRIGGER = 1;
  public static final int CONTRACTLOG_TRIGGER = 2;
  public static final int CONTRACTEVENT_TRIGGER = 3;

  public static final String BLOCK_TRIGGER_NAME = "blockTrigger";
  public static final String TRANSACTION_TRIGGER_NAME = "transactionTrigger";
  public static final String CONTRACTLOG_TRIGGER_NAME = "contractLogTrigger";
  public static final String CONTRACTEVENT_TRIGGER_NAME = "contractEventTrigger";
}
