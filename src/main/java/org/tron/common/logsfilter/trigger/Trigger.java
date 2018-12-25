package org.tron.common.logsfilter.trigger;

import lombok.Getter;
import lombok.Setter;

public class Trigger {
  @Getter
  @Setter
  protected long timeStamp;

  public static final int BLOCK_TRIGGER = 0;
  public static final int TRANSACTION_TRIGGER = 1;
  public static final int CONTRACTLOG_TRIGGER = 2;
  public static final int CONTRACTEVENT_TRIGGER = 3;
}
