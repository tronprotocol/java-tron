package org.tron.common.logsfilter.capsule;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.logsfilter.trigger.LogPojo;

public class LogsFilterCapsule extends FilterTriggerCapsule {

  @Getter
  @Setter
  private List<LogPojo> logList;

  public LogsFilterCapsule() {

  }

  @Override
  public void processFilterTrigger() {
  }
}